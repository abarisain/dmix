package com.namelessdev.mpdroid;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.MPDConnectionStateChangedEvent;
import org.a0z.mpd.event.MPDPlaylistChangedEvent;
import org.a0z.mpd.event.MPDRandomChangedEvent;
import org.a0z.mpd.event.MPDRepeatChangedEvent;
import org.a0z.mpd.event.MPDStateChangedEvent;
import org.a0z.mpd.event.MPDTrackChangedEvent;
import org.a0z.mpd.event.MPDUpdateStateChangedEvent;
import org.a0z.mpd.event.MPDVolumeChangedEvent;
import org.a0z.mpd.event.StatusChangeListener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.namelessdev.mpdroid.MPDAsyncHelper.ConnectionListener;

/**
 * StreamingService is my code which notifies and streams mpd (theorically) I hope I'm doing things right. Really. And say farewell to your
 * battery because I think I am raping it.
 * 
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id: $
 */

public class StreamingService extends Service implements StatusChangeListener, OnPreparedListener, OnCompletionListener,
		OnBufferingUpdateListener, OnErrorListener, OnInfoListener, ConnectionListener {

	public static final int STREAMINGSERVICE_STATUS = 1;
	public static final int STREAMINGSERVICE_PAUSED = 2;
	public static final int STREAMINGSERVICE_STOPPED = 3;
	public static final String CMD_REMOTE = "com.namelessdev.mpdroid.REMOTE_COMMAND";
	public static final String CMD_COMMAND = "COMMAND";
	public static final String CMD_PAUSE = "PAUSE";
	public static final String CMD_STOP = "STOP";
	public static final String CMD_PLAY = "PLAY";
	public static final String CMD_PLAYPAUSE = "PLAYPAUSE";
	public static final String CMD_PREV = "PREV";
	public static final String CMD_NEXT = "NEXT";
	public static final String CMD_DIE = "DIE"; // Just in case
	public static Boolean isServiceRunning = false;

	private MediaPlayer mediaPlayer;
	private AudioManager audioManager;
	private ComponentName remoteControlResponder;
	private Timer timer = new Timer();
	private String streamSource;
	private Boolean buffering;
	private String oldStatus;
	private Boolean isPlaying;
	private Boolean isPaused; // The distinction needs to be made so the service doesn't start whenever it want
	private Boolean needStoppedNotification;
	private Integer lastStartID;

	private static Method registerMediaButtonEventReceiver; // Thanks you google again for this code
	private static Method unregisterMediaButtonEventReceiver;

	private static final int IDLE_DELAY = 60000;

	private Handler delayedStopHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (isPlaying || isPaused || buffering) {
				return;
			}
			die();
		}
	};

	private static void initializeRemoteControlRegistrationMethods() {
		try {
			if (registerMediaButtonEventReceiver == null) {
				registerMediaButtonEventReceiver = AudioManager.class.getMethod("registerMediaButtonEventReceiver",
						new Class[] { ComponentName.class });
			}
			if (unregisterMediaButtonEventReceiver == null) {
				unregisterMediaButtonEventReceiver = AudioManager.class.getMethod("unregisterMediaButtonEventReceiver",
						new Class[] { ComponentName.class });
			}
		} catch (NoSuchMethodException nsme) {
			/* Aww we're not 2.2 */
		}
	}

	private void registerMediaButtonEvent() {
		if (registerMediaButtonEventReceiver == null) {
			return;
		}
		try {
			registerMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void unregisterMediaButtonEvent() {
		if (unregisterMediaButtonEventReceiver == null) {
			return;
		}
		try {
			unregisterMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Boolean getStreamingServiceStatus() {
		return isServiceRunning;
	}

	// And thanks to google ... again
	private PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			MPDApplication app = (MPDApplication) getApplication();
			if (app == null)
				return;
			if (((MPDApplication) app).isStreamingMode() == false) {
				stopSelf();
				return;
			}
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
				if (ringvolume > 0 && isPlaying) {
					isPaused = true;
					pauseStreaming();
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// pause the music while a conversation is in progress
				if (isPlaying == false)
					return;
				isPaused = (isPaused || isPlaying) && (app.isStreamingMode());
				pauseStreaming();
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				// start playing again
				if (isPaused) {
					// resume playback only if music was playing
					// when the call was answered
					resumeStreaming();
				}
			}
		}
	};

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String cmd = intent.getStringExtra(CMD_COMMAND);
			if (cmd.equals(CMD_NEXT)) {
				next();
			} else if (cmd.equals(CMD_PREV)) {
				prev();
			} else if (cmd.equals(CMD_PLAYPAUSE)) {
				if (isPaused == false) {
					pauseStreaming();
				} else {
					resumeStreaming();
				}
			} else if (cmd.equals(CMD_PAUSE)) {
				pauseStreaming();
			} else if (cmd.equals(CMD_STOP)) {
				stop();
			}
		}
	};

	public void onCreate() {
		super.onCreate();
		isServiceRunning = true;
		mediaPlayer = new MediaPlayer();
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		buffering = true;
		oldStatus = "";
		isPlaying = true;
		isPaused = false;
		needStoppedNotification = false; // Maybe I shouldn't try fixing bugs after long days of work
		lastStartID = 0;
		// streaming_enabled = false;
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnInfoListener(this);

		remoteControlResponder = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

		initializeRemoteControlRegistrationMethods();

		registerMediaButtonEvent();

		TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		MPDApplication app = (MPDApplication) getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addConnectionListener(this);
		streamSource = "http://" + app.oMPDAsyncHelper.getConnectionInfoServer() + ":" + app.oMPDAsyncHelper.getConnectionInfoPortStreaming()
				+ "/";
		// showNotification();
		/*
		 * if (!mIsSupposedToBePlaying) { mIsSupposedToBePlaying = true; notifyChange(PLAYSTATE_CHANGED); }
		 */
	}

	@Override
	public void onDestroy() {
		if (needStoppedNotification) {
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(STREAMINGSERVICE_PAUSED);
			RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
			views.setImageViewResource(R.id.icon, R.drawable.icon);
			Notification status = null;
			views.setTextViewText(R.id.trackname, getString(R.string.streamStopped));
			views.setTextViewText(R.id.album, getString(R.string.app_name));
			views.setTextViewText(R.id.artist, "");
			status = new Notification(R.drawable.icon, getString(R.string.streamStopped), System.currentTimeMillis());
			status.contentView = views;
			status.icon = R.drawable.icon;
			status.contentIntent = PendingIntent.getActivity(this, 0, new Intent("com.namelessdev.mpdroid.PLAYBACK_VIEWER")
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(STREAMINGSERVICE_STOPPED, status);
		}
		isServiceRunning = false;
		unregisterMediaButtonEvent();
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) { // Stupid 1.6 compatibility
		onStartCommand(intent, 0, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		lastStartID = startId;
		if (((MPDApplication) getApplication()).isStreamingMode() == false) {
			stopSelfResult(lastStartID);
			return 0;
		}
		if (intent.getAction().equals("com.namelessdev.mpdroid.START_STREAMING")) {
			// streaming_enabled = true;
			resumeStreaming();
		} else if (intent.getAction().equals("com.namelessdev.mpdroid.STOP_STREAMING")) {
			stopStreaming();
		} else if (intent.getAction().equals("com.namelessdev.mpdroid.RESET_STREAMING")) {
			stopStreaming();
			resumeStreaming();
		} else if (intent.getAction().equals("com.namelessdev.mpdroid.DIE")) {
			die();
		} else if (intent.getAction().equals(CMD_REMOTE)) {
			String cmd = intent.getStringExtra(CMD_COMMAND);
			if (cmd.equals(CMD_NEXT)) {
				next();
			} else if (cmd.equals(CMD_PREV)) {
				prev();
			} else if (cmd.equals(CMD_PLAYPAUSE)) {
				if (isPaused == false) {
					pauseStreaming();
				} else {
					resumeStreaming();
				}
			} else if (cmd.equals(CMD_PAUSE)) {
				pauseStreaming();
			} else if (cmd.equals(CMD_STOP)) {
				stop();
			}
		}
		// Toast.makeText(this, "onStartCommand  : "+(intent.getAction() == "com.namelessdev.mpdroid.START_STREAMING"),
		// Toast.LENGTH_SHORT).show();
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void showNotification() {
		MPDApplication app = (MPDApplication) getApplication();
		MPDStatus statusMpd = null;
		try {
			statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
		} catch (MPDServerException e) {
			// Do nothing cause I suck hard at android programming
		}
		if (statusMpd != null && !isPaused) {
			String state = statusMpd.getState();
			if (state != null) {
				if (state == oldStatus)
					return;
				oldStatus = state;
				int songId = statusMpd.getSongPos();
				if (songId >= 0) {
					((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(STREAMINGSERVICE_PAUSED);
					((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(STREAMINGSERVICE_STOPPED);
					stopForeground(true);
					Music actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getMusic(songId);
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
					views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
					Notification status = null;
					if (buffering) {
						views.setTextViewText(R.id.trackname, getString(R.string.buffering));
						views.setTextViewText(R.id.album, actSong.getTitle());
						views.setTextViewText(R.id.artist, actSong.getAlbum() + " - " + actSong.getArtist());
						status = new Notification(R.drawable.icon, getString(R.string.buffering), System.currentTimeMillis());
					} else {
						views.setTextViewText(R.id.trackname, actSong.getTitle());
						views.setTextViewText(R.id.album, actSong.getAlbum());
						views.setTextViewText(R.id.artist, actSong.getArtist());
						status = new Notification(R.drawable.icon, actSong.getTitle() + " - " + actSong.getArtist(), System.currentTimeMillis());
					}

					status.contentView = views;
					status.flags |= Notification.FLAG_ONGOING_EVENT;
					status.icon = R.drawable.icon;
					status.contentIntent = PendingIntent.getActivity(this, 0, new Intent("com.namelessdev.mpdroid.PLAYBACK_VIEWER")
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);

					startForeground(STREAMINGSERVICE_STATUS, status);
				}
			}
		} else if (isPaused) {
			RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
			views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
			Notification status = null;
			views.setTextViewText(R.id.trackname, getString(R.string.streamPaused));
			views.setTextViewText(R.id.album, getString(R.string.streamPauseBattery));
			views.setTextViewText(R.id.artist, "");
			status = new Notification(R.drawable.icon, getString(R.string.streamPaused), System.currentTimeMillis());

			status.contentView = views;
			status.flags |= Notification.FLAG_ONGOING_EVENT;
			status.icon = R.drawable.icon;
			status.contentIntent = PendingIntent.getActivity(this, 0, new Intent("com.namelessdev.mpdroid.PLAYBACK_VIEWER")
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(STREAMINGSERVICE_PAUSED, status);
		}

	}

	public void pauseStreaming() {
		if (isPlaying == false)
			return;
		isPlaying = false;
		isPaused = true;
		buffering = false;
		mediaPlayer.stop(); // So it stops faster
		showNotification();
		MPDApplication app = (MPDApplication) getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		try {
			String state = mpd.getStatus().getState();
			if (state.equals(MPDStatus.MPD_STATE_PLAYING))
				mpd.pause();
		} catch (MPDServerException e) {

		}
	}

	public void resumeStreaming() {
		// just to be sure, we do not want to start when we're not supposed to
		if (((MPDApplication) getApplication()).isStreamingMode() == false)
			return;
		needStoppedNotification = false;
		buffering = true;
		MPDApplication app = (MPDApplication) getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		registerMediaButtonEvent();
		if (isPaused == true) {
			try {
				String state = mpd.getStatus().getState();
				if (state.equals(MPDStatus.MPD_STATE_PAUSED)) {
					mpd.pause();
				}
				isPaused = false;
			} catch (MPDServerException e) {

			}
		}
		if (mediaPlayer == null)
			return;
		try {
			mediaPlayer.reset();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(streamSource);
			mediaPlayer.prepareAsync();
			showNotification();
		} catch (IOException e) {
			// Error ? Notify the user ! (Another day)
			buffering = false; // Obviously if it failed we are not buffering.
			isPlaying = false;
		} catch (IllegalStateException e) {
			// wtf what state ?
			// Toast.makeText(this, "Error IllegalStateException isPlaying : "+mediaPlayer.isPlaying(), Toast.LENGTH_SHORT).show();
			isPlaying = false;
		}
	}

	public void stopStreaming() {
		oldStatus = "";
		if (mediaPlayer == null)
			return;
		mediaPlayer.stop();
		stopForeground(true);
	}

	public void prev() {
		MPDApplication app = (MPDApplication) getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		try {
			mpd.previous();
		} catch (MPDServerException e) {

		}
		stopStreaming();
		resumeStreaming();
	}

	public void next() {
		MPDApplication app = (MPDApplication) getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		try {
			mpd.next();
		} catch (MPDServerException e) {

		}
		stopStreaming();
		resumeStreaming();
	}

	public void stop() {
		MPDApplication app = (MPDApplication) getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		try {
			mpd.stop();
		} catch (MPDServerException e) {

		}
		stopStreaming();
		die();
	}

	public void die() {
		((MPDApplication) getApplication()).setStreamingMode(false);
		// Toast.makeText(this, "MPD Streaming Stopped", Toast.LENGTH_SHORT).show();
		stopSelfResult(lastStartID);
	}

	@Override
	public void trackChanged(MPDTrackChangedEvent event) {
		oldStatus = "";
		showNotification();
	}

	@Override
	public void connectionStateChanged(MPDConnectionStateChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void playlistChanged(MPDPlaylistChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void randomChanged(MPDRandomChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void repeatChanged(MPDRepeatChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateChanged(MPDStateChangedEvent event) {
		// TODO Auto-generated method stub
		// Toast.makeText(this, "stateChanged :", Toast.LENGTH_SHORT).show();
		Message msg = delayedStopHandler.obtainMessage();
		delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		MPDApplication app = (MPDApplication) getApplication();
		MPDStatus statusMpd = null;
		try {
			statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
		} catch (MPDServerException e) {
			// Do nothing cause I suck hard at android programming
		}
		if (statusMpd != null) {
			String state = statusMpd.getState();
			if (state != null) {
				if (state == oldStatus)
					return;
				if (state == MPDStatus.MPD_STATE_PLAYING) {
					isPaused = false;
					resumeStreaming();
					isPlaying = true;
				} else {
					oldStatus = state;
					isPlaying = false;
					stopStreaming();
				}
			}
		}
	}

	@Override
	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub
		// Toast.makeText(this, "updateStateChanged :", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void volumeChanged(MPDVolumeChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// Buffering done
		buffering = false;
		isPlaying = true;
		oldStatus = "";
		showNotification();
		mediaPlayer.start();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// Toast.makeText(this, "Completion", Toast.LENGTH_SHORT).show();
		Message msg = delayedStopHandler.obtainMessage();
		delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY); // Don't suck the battery too much
		MPDApplication app = (MPDApplication) getApplication();
		MPDStatus statusMpd = null;
		try {
			statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
		} catch (MPDServerException e) {
			// Do nothing cause I suck hard at android programming
		}
		if (statusMpd != null) {
			String state = statusMpd.getState();
			if (state != null) {
				if (state == MPDStatus.MPD_STATE_PLAYING) {
					// Resume playing
					// TODO Stop resuming if no 3G. There's no point. Add something that says "ok we're waiting for 3G/wifi !"
					resumeStreaming();
				} else {
					oldStatus = state;
					// Something's happening, like crappy network or MPD just stopped..
					die();
				}
			}
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		// Toast.makeText(this, "Buf update", Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// Toast.makeText(this, "onError", Toast.LENGTH_SHORT).show();
		// mediaPlayer.reset();
		return false;
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// Toast.makeText(this, "onInfo :", Toast.LENGTH_SHORT).show();
		return false;
	}

	@Override
	public void connectionFailed(String message) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Connection Failed !", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void connectionSucceeded(String message) {
		// TODO Auto-generated method stub
		// Toast.makeText(this, "connectionSucceeded :", Toast.LENGTH_SHORT).show();
	}
}
