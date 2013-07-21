package com.namelessdev.mpdroid;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.ConnectionListener;
import com.namelessdev.mpdroid.tools.Tools;

/**
 * StreamingService is my code which notifies and streams MPD (theoretically) I hope I'm doing things right. Really. And say farewell to your
 * battery because I think I am raping it.
 * 
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id: $
 */

public class StreamingService extends Service implements StatusChangeListener, OnPreparedListener, OnCompletionListener,
		OnBufferingUpdateListener, OnErrorListener, OnInfoListener, ConnectionListener, OnAudioFocusChangeListener {

	static final String TAG = "MPDroidStreamingService";
	
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
	public static boolean isServiceRunning = false;

	
    /**
     * Playback state of a RemoteControlClient which is stopped.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_STOPPED            = 1;
    /**
     * Playback state of a RemoteControlClient which is paused.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_PAUSED             = 2;
    /**
     * Playback state of a RemoteControlClient which is playing media.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_PLAYING            = 3;
    /**
     * Playback state of a RemoteControlClient which is fast forwarding in the media
     *    it is currently playing.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_FAST_FORWARDING    = 4;
    /**
     * Playback state of a RemoteControlClient which is fast rewinding in the media
     *    it is currently playing.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_REWINDING          = 5;
    /**
     * Playback state of a RemoteControlClient which is skipping to the next
     *    logical chapter (such as a song in a playlist) in the media it is currently playing.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_SKIPPING_FORWARDS  = 6;
    /**
     * Playback state of a RemoteControlClient which is skipping back to the previous
     *    logical chapter (such as a song in a playlist) in the media it is currently playing.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_SKIPPING_BACKWARDS = 7;
    /**
     * Playback state of a RemoteControlClient which is buffering data to play before it can
     *    start or resume playback.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_BUFFERING          = 8;
    /**
     * Playback state of a RemoteControlClient which cannot perform any playback related
     *    operation because of an internal error. Examples of such situations are no network
     *    connectivity when attempting to stream data from a server, or expired user credentials
     *    when trying to play subscription-based content.
     *
     * @see #setPlaybackState(int)
     */
    public final static int PLAYSTATE_ERROR              = 9;
	
	private MediaPlayer mediaPlayer;
	private AudioManager audioManager;
	private ComponentName remoteControlResponder;
	//private Timer timer = new Timer();
	private String streamSource;
	private Boolean buffering;
	private String oldStatus;
	private boolean isPlaying;
	private boolean isPaused; // The distinction needs to be made so the service doesn't start whenever it want
	private boolean needStoppedNotification;
	private Integer lastStartID;
	//private Integer mediaPlayerError;

	private static Method registerMediaButtonEventReceiver; // Thanks you google again for this code
	private static Method unregisterMediaButtonEventReceiver;
	
	private Object remoteControlClient = null; // No type ... retrocompatibility

	private static final int IDLE_DELAY = 60000;

	@SuppressLint("HandlerLeak")
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
		if (registerMediaButtonEventReceiver == null)
			return;
		
		try {
			registerMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void unregisterMediaButtonEvent() {
		if (unregisterMediaButtonEventReceiver == null)
			return;
		
		try {
			unregisterMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	@TargetApi(14)
	private void registerRemoteControlClient() {
		if(Build.VERSION.SDK_INT > 14) {
			// build the PendingIntent for the remote control client
			Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			mediaButtonIntent.setComponent(remoteControlResponder);
			PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
			// create and register the remote control client
			remoteControlClient = new RemoteControlClient(mediaPendingIntent);
			((RemoteControlClient) remoteControlClient).setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
					RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
					RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
			audioManager.registerRemoteControlClient((RemoteControlClient) remoteControlClient);
		}
	}
	
	@TargetApi(14)
	private void unregisterRemoteControlClient() {
		if(Build.VERSION.SDK_INT > 14 && remoteControlClient != null) {
			audioManager.unregisterRemoteControlClient((RemoteControlClient) remoteControlClient);
		}
	}
	
	@TargetApi(14)
	private void setMusicState(int state) {
		if(Build.VERSION.SDK_INT > 14 && remoteControlClient != null) {
			((RemoteControlClient) remoteControlClient).setPlaybackState(state);
		}
	}
	
	@TargetApi(14)
	private void setMusicCover(Bitmap cover) {
		if (Build.VERSION.SDK_INT > 14 && remoteControlClient != null) {
			((RemoteControlClient) remoteControlClient).editMetadata(false)
					.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, cover).apply();
		}
	}

	@TargetApi(14)
	private void setMusicInfo(Music song) {
		if(Build.VERSION.SDK_INT > 14 && remoteControlClient != null && song != null) {
			MetadataEditor editor = ((RemoteControlClient) remoteControlClient).editMetadata(true);
			//TODO : maybe add cover art here someday
			editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, song.getTime()*1000);
			editor.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, song.getTrack());
			editor.putLong(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER, song.getDisc());
			editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, song.getAlbum());
			editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, song.getAlbumArtist());
			editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, song.getArtist());
			editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, song.getTitle());
			editor.apply();
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
			
			if (!((MPDApplication) app).getApplicationState().streamingMode) {
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
				isPaused = (isPaused || isPlaying) && (app.getApplicationState().streamingMode);
				pauseStreaming();
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				// start playing again
				if (isPaused) {
					// resume play back only if music was playing
					// when the call was answered
					resumeStreaming();
				}
			}
		}
	};
	
	@TargetApi(9)
	public void onCreate() {
		super.onCreate();

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		if (!((MPDApplication) getApplication()).getApplicationState().streamingMode) {
			stopSelf();
			return;
		}

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
		registerRemoteControlClient();
		
		if(audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
			Toast.makeText(this, R.string.audioFocusFailed, Toast.LENGTH_LONG).show();
			stop();
		}

		TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		MPDApplication app = (MPDApplication) getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addConnectionListener(this);
		app.setActivity(this);
		streamSource = "http://" + app.oMPDAsyncHelper.getConnectionSettings().getConnectionStreamingServer() + ":"
				+ app.oMPDAsyncHelper.getConnectionSettings().iPortStreaming + "/"
				+ app.oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming;
		// Log.w(MPDApplication.TAG, streamSource);
	}

	@Override
	public void onDestroy() {
		if (needStoppedNotification) {
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(STREAMINGSERVICE_PAUSED);
			Notification status = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.streamStopped))
				.setSmallIcon(R.drawable.icon)
				.setContentIntent(PendingIntent.getActivity(this, 0,
						new Intent("com.namelessdev.mpdroid.PLAYBACK_VIEWER").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0))
					.build();
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(STREAMINGSERVICE_STOPPED, status);
		}
		isServiceRunning = false;
		setMusicState(PLAYSTATE_STOPPED);
		unregisterMediaButtonEvent();
		unregisterRemoteControlClient();
		if (audioManager != null)
			audioManager.abandonAudioFocus(this);
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		MPDApplication app = (MPDApplication) getApplication();
		app.unsetActivity(this);
		app.getApplicationState().streamingMode = false;
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) { // Stupid 1.6 compatibility
		onStartCommand(intent, 0, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		lastStartID = startId;
		if (!((MPDApplication) getApplication()).getApplicationState().streamingMode) {
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
		showNotification(false);
	}

	public void showNotification(boolean streamingStatusChanged) {
		try {
			MPDApplication app = (MPDApplication) getApplication();
			MPDStatus statusMpd = null;
			try {
				statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
			} catch (MPDServerException e) {
				// Do nothing cause I suck hard at android programming
			}
			// Don't show the notification if paused, except on Jelly bean where it has buttons
			if (statusMpd != null && (!isPaused || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
				String state = statusMpd.getState();
				if (state != null) {
					if (state == oldStatus && !streamingStatusChanged)
						return;
					oldStatus = state;
					int songPos = statusMpd.getSongPos();
					if (songPos >= 0) {
						((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(STREAMINGSERVICE_PAUSED);
						((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(STREAMINGSERVICE_STOPPED);
						stopForeground(true);
						Music actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
						setMusicInfo(actSong);
						RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
						views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
						Notification status = null;
						NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
								.setSmallIcon(Build.VERSION.SDK_INT >= 9 ? R.drawable.icon_bw : R.drawable.icon)
						 .setOngoing(true)
						 .setContentTitle(getString(R.string.streamStopped))
						 .setContentIntent(PendingIntent.getActivity(this, 0,
								new Intent("com.namelessdev.mpdroid.PLAYBACK_VIEWER").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0));
						if(buffering) {
							setMusicState(PLAYSTATE_BUFFERING);
							notificationBuilder.setContentTitle(getString(R.string.buffering));
							notificationBuilder.setContentText(actSong.getTitle() + " - " + actSong.getArtist());
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
								notificationBuilder.addAction(R.drawable.ic_media_stop, getString(R.string.stop), PendingIntent.getService(
										this, 41,
										new Intent(this, StreamingService.class).setAction(CMD_REMOTE).putExtra(CMD_COMMAND, CMD_STOP),
										PendingIntent.FLAG_CANCEL_CURRENT));
							}
						} else {
							setMusicState(isPaused ? PLAYSTATE_PAUSED : PLAYSTATE_PLAYING);
							notificationBuilder.setContentTitle(actSong.getTitle());
							notificationBuilder.setContentText(actSong.getAlbum() + " - " + actSong.getArtist());
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
								notificationBuilder.addAction(R.drawable.ic_appwidget_music_prev, "", PendingIntent.getService(this, 11,
										new Intent(this, StreamingService.class).setAction(CMD_REMOTE).putExtra(CMD_COMMAND, CMD_PREV),
										PendingIntent.FLAG_CANCEL_CURRENT));
								notificationBuilder.addAction(isPaused ? R.drawable.ic_appwidget_music_play
										: R.drawable.ic_appwidget_music_pause, "", PendingIntent.getService(this, 21, new Intent(this,
										StreamingService.class).setAction(CMD_REMOTE).putExtra(CMD_COMMAND, CMD_PLAYPAUSE),
										PendingIntent.FLAG_CANCEL_CURRENT));
								notificationBuilder.addAction(R.drawable.ic_appwidget_music_next, "", PendingIntent.getService(this, 31,
										new Intent(this, StreamingService.class).setAction(CMD_REMOTE).putExtra(CMD_COMMAND, CMD_NEXT),
										PendingIntent.FLAG_CANCEL_CURRENT));
							}
						}

						// Check if we have a sdcard cover cache for this song
						// Maybe find a more efficient way
						final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
						if (settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true)) {
							final CachedCover cache = new CachedCover(app);
							final String[] coverArtPath = cache.getCoverUrl(actSong.getArtist(), actSong.getAlbum(), actSong.getPath(),
									actSong.getFilename());
							if (coverArtPath != null && coverArtPath.length > 0 && coverArtPath[0] != null) {
								notificationBuilder.setLargeIcon(Tools.decodeSampledBitmapFromPath(coverArtPath[0], getResources()
										.getDimensionPixelSize(android.R.dimen.notification_large_icon_width), getResources()
										.getDimensionPixelSize(android.R.dimen.notification_large_icon_height), true));
								setMusicCover(Tools.decodeSampledBitmapFromPath(coverArtPath[0],
										(int) Tools.convertDpToPixel(200, this), (int) Tools.convertDpToPixel(200, this), false));
							} else {
								setMusicCover(null);
							}
						} else {
							setMusicCover(null);
						}

						status = notificationBuilder.build();

						startForeground(STREAMINGSERVICE_STATUS, status);
					}
				}
			}

		} catch (Exception e) {
			// This should not happen anymore, and catching everything is ugly, but crashing because of a notification is pretty stupid IMHO
		}
	}

	public void pauseStreaming() {
		if (isPlaying == false)
			return;
		isPlaying = false;
		isPaused = true;
		buffering = false;
		if (mediaPlayer != null) { // If that stupid thing crashes
			mediaPlayer.stop(); // So it stops faster
		}
		showNotification(true);
	}

	public void resumeStreaming() {
		// just to be sure, we do not want to start when we're not supposed to
		if (!((MPDApplication) getApplication()).getApplicationState().streamingMode)
			return;
		
		needStoppedNotification = false;
		isPaused = false;
		buffering = true;
		// MPDApplication app = (MPDApplication) getApplication();
		// MPD mpd = app.oMPDAsyncHelper.oMPD;
		registerMediaButtonEvent();
		registerRemoteControlClient();
		/*
		 * if (isPaused == true) { try { String state = mpd.getStatus().getState(); if (state.equals(MPDStatus.MPD_STATE_PAUSED)) {
		 * mpd.pause(); } isPaused = false; } catch (MPDServerException e) {
		 * 
		 * } }
		 */
		if (mediaPlayer == null)
			return;
		try {
			mediaPlayer.reset();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(streamSource);
			mediaPlayer.prepareAsync();
			showNotification(true);
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
		/*
		 * MPDApplication app = (MPDApplication) getApplication(); MPD mpd = app.oMPDAsyncHelper.oMPD; try { mpd.stop(); } catch
		 * (MPDServerException e) {
		 * 
		 * }
		 */
		stopStreaming();
		die();
	}

	public void die() {
		((MPDApplication) getApplication()).getApplicationState().streamingMode = false;
		// Toast.makeText(this, "MPD Streaming Stopped", Toast.LENGTH_SHORT).show();
		stopSelfResult(lastStartID);
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
		//mediaPlayerError = what;
		pauseStreaming();
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
		// Toast.makeText(this, "Connection Failed !", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void connectionSucceeded(String message) {
		// TODO Auto-generated method stub
		// Toast.makeText(this, "connectionSucceeded :", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
		oldStatus = "";
		showNotification();
		
	}

	@Override
	public void stateChanged(MPDStatus mpdStatus, String oldState) {
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
	public void repeatChanged(boolean repeating) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void randomChanged(boolean random) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connectionStateChanged(boolean connected, boolean connectionLost) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void libraryStateChanged(boolean updating) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
			mediaPlayer.setVolume(0.2f, 0.2f);
		} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			mediaPlayer.setVolume(1f, 1f);
		} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
			stop();
		}
	}
}
