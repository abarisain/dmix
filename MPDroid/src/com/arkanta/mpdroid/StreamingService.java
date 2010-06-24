package com.arkanta.mpdroid;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

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

import com.arkanta.mpdroid.MPDAsyncHelper.ConnectionListener;
import com.arkanta.mpdroid.R.drawable;
import com.arkanta.mpdroid.R.id;
import com.arkanta.mpdroid.R.layout;
import com.arkanta.mpdroid.R.string;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * StreamingService is my code which notifies and streams mpd (theorically)
 * I hope I'm doing things right. Really. And say farewell to your battery because I think I am raping it.
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id:  $
 */
public class StreamingService extends Service
		implements StatusChangeListener, OnPreparedListener, OnCompletionListener,
		OnBufferingUpdateListener, OnErrorListener, OnInfoListener, ConnectionListener {
	
	public static final int STREAMINGSERVICE_STATUS = 1;	
	
	private MediaPlayer mediaPlayer;
	private Timer timer = new Timer();
	private String streamSource;
	private Boolean buffering;
	private String oldStatus;
	//private Boolean streaming_enabled; //So we know if we've been called.
    public class LocalBinder extends Binder {
    	StreamingService getService() {
            return StreamingService.this;
        }
    }

	public void onCreate() {
		super.onCreate();
		mediaPlayer = new MediaPlayer();
		buffering = true;
		oldStatus = "";
		//streaming_enabled = false;
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnInfoListener(this);
		
		MPDApplication app = (MPDApplication)getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addConnectionListener(this);
		streamSource = "http://"+app.oMPDAsyncHelper.getConnectionInfoServer()+":"
							+app.oMPDAsyncHelper.getConnectionInfoPortStreaming()+"/";
		Toast.makeText(this, "Source : "+streamSource+"|", Toast.LENGTH_SHORT).show();
        //showNotification();
        /*if (!mIsSupposedToBePlaying) {
            mIsSupposedToBePlaying = true;
            notifyChange(PLAYSTATE_CHANGED);
        }*/
	}
	
    @Override
    public void onDestroy() {
    	mediaPlayer.stop();
    	mediaPlayer.release();
    	mediaPlayer = null;
    	super.onDestroy();	
    }
	
	@Override
	public void onStart(Intent intent, int startId) { //Stupid 1.6 compatibility
		onStartCommand(intent, 0, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals("com.arkanta.mpdroid.START_STREAMING")) {
			//streaming_enabled = true;
			resumeStreaming();
		} else if (intent.getAction().equals("com.arkanta.mpdroid.STOP_STREAMING")) {
			stopStreaming();
		} else if (intent.getAction().equals("com.arkanta.mpdroid.RESET_STREAMING")) {
			stopStreaming();
			resumeStreaming();
		}
		//Toast.makeText(this, "onStartCommand  : "+(intent.getAction() == "com.arkanta.mpdroid.START_STREAMING"), Toast.LENGTH_SHORT).show();
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
		MPDApplication app = (MPDApplication)getApplication();
		MPDStatus statusMpd = null;
		try {
			statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
		} catch (MPDServerException e) {
			// Do nothing cause I suck hard at android programming
		}
		if(statusMpd!=null)
		{
			String state = statusMpd.getState();
			if(state != null)
			{
				if(state == oldStatus)
					return;
				oldStatus = state;
				int songId = statusMpd.getSongPos();
				if(songId>=0)
				{
					stopForeground(true);
					Music actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getMusic(songId);
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
			        views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
			        Notification status = null;
			        if(buffering) {
				        views.setTextViewText(R.id.trackname, getString(R.string.buffering));
				        views.setTextViewText(R.id.album, actSong.getTitle());
				        views.setTextViewText(R.id.artist, actSong.getAlbum() + " - " + actSong.getArtist());
						status = new Notification(R.drawable.icon,getString(R.string.buffering), System.currentTimeMillis());
			        } else {
				        views.setTextViewText(R.id.trackname, actSong.getTitle());
				        views.setTextViewText(R.id.album, actSong.getAlbum());
				        views.setTextViewText(R.id.artist, actSong.getArtist());
						status = new Notification(R.drawable.icon,actSong.getTitle()+" - "+actSong.getArtist(), System.currentTimeMillis());
			        }
			        
			        status.contentView = views;
			        status.flags |= Notification.FLAG_ONGOING_EVENT;
			        status.icon = R.drawable.icon;
			        status.contentIntent = PendingIntent.getActivity(this, 0,
			                new Intent("com.arkanta.mpdroid.PLAYBACK_VIEWER")
			                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
			        
			        startForeground(STREAMINGSERVICE_STATUS, status);
				}
			}
		}

	}
	
	public void resumeStreaming() {
		buffering = true;
		if (mediaPlayer == null)
			return;
		try {
			mediaPlayer.reset();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(streamSource);
			mediaPlayer.prepare();
			showNotification();
		} catch (IOException e) {
			// Error ? Notify the user ! (Another day)
			buffering = false; //Obviously if it failed we are not buffering.
		} catch (IllegalStateException e) {
			//wtf what state ?
			Toast.makeText(this, "Error IllegalStateException isPlaying : "+mediaPlayer.isPlaying(), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void stopStreaming() {
		oldStatus = "";
		if (mediaPlayer == null)
			return;
		mediaPlayer.stop();
		stopForeground(true);
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
		//Toast.makeText(this, "stateChanged :", Toast.LENGTH_SHORT).show();
		MPDApplication app = (MPDApplication)getApplication();
		MPDStatus statusMpd = null;
		try {
			statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
		} catch (MPDServerException e) {
			// Do nothing cause I suck hard at android programming
		}
		if(statusMpd!=null) {
			String state = statusMpd.getState();
			if(state != null) {
				if(state == oldStatus)
					return;
				if(state == MPDStatus.MPD_STATE_PLAYING) {
					resumeStreaming();	
				} else {
					oldStatus = state;
					stopStreaming();
				}
			}
		}
	}
	@Override
	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub
		//Toast.makeText(this, "updateStateChanged :", Toast.LENGTH_SHORT).show();
	}
	@Override
	public void volumeChanged(MPDVolumeChangedEvent event) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onPrepared(MediaPlayer mp) {
		// Buffering done
		buffering = false;
		oldStatus = "";
		showNotification();
		mediaPlayer.start();
	}
	@Override
	public void onCompletion(MediaPlayer mp) {
		//Toast.makeText(this, "Completion", Toast.LENGTH_SHORT).show();
		MPDApplication app = (MPDApplication)getApplication();
		MPDStatus statusMpd = null;
		try {
			statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
		} catch (MPDServerException e) {
			// Do nothing cause I suck hard at android programming
		}
		if(statusMpd!=null) {
			String state = statusMpd.getState();
			if(state != null) {
				if(state == MPDStatus.MPD_STATE_PLAYING) {
					// Resume playing
					// TODO Stop resuming if no 3G. There's no point. Add something that says "ok we're waiting for 3G/wifi !"
					resumeStreaming();	
				} else {
					oldStatus = state;
					// Something's happening, like crappy network or MPD just stopped..
					stopForeground(true); // Nothing is playing -> no notification. Also system can now kill us.
				}
			}
		}
	}
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Buf update", Toast.LENGTH_SHORT).show();
	}
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		//Toast.makeText(this, "onError", Toast.LENGTH_SHORT).show();
		//mediaPlayer.reset();
		return false;
	}
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		//Toast.makeText(this, "onInfo :", Toast.LENGTH_SHORT).show();
		return false;
	}

	@Override
	public void connectionFailed(String message) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "connectionFailed :", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void connectionSucceeded(String message) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "connectionSucceeded :", Toast.LENGTH_SHORT).show();
	}
}
