package org.pmix.ui;

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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

/**
 * StreamingService is my code which notifies and streams mpd (theorically)
 * I hope I'm doing things right. Really. And say farewell to your battery because I think I am raping it.
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id:  $
 */
public class StreamingService extends Service implements StatusChangeListener {
	
	public static final int STREAMINGSERVICE_STATUS = 1;	
	
	private MediaPlayer mediaPlayer;
	private Timer timer = new Timer();
	
    public class LocalBinder extends Binder {
    	StreamingService getService() {
            return StreamingService.this;
        }
    }

	public void onCreate() {
		super.onCreate();
		mediaPlayer = new MediaPlayer();
		MPDApplication app = (MPDApplication)getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
        showNotification();
        /*if (!mIsSupposedToBePlaying) {
            mIsSupposedToBePlaying = true;
            notifyChange(PLAYSTATE_CHANGED);
        }*/
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
				int songId = statusMpd.getSongPos();
				if(songId>=0)
				{
					stopForeground(true);
					Music actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getMusic(songId);
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
			        views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
			        views.setTextViewText(R.id.trackname, actSong.getTitle());
			        views.setTextViewText(R.id.artistalbum, actSong.getArtist()+" - "+actSong.getAlbum());
			        
					Notification status = new Notification(R.drawable.icon,actSong.getTitle()+" - "+actSong.getArtist(), System.currentTimeMillis());
			        status.contentView = views;
			        status.flags |= Notification.FLAG_ONGOING_EVENT;
			        status.icon = R.drawable.icon;
			        status.contentIntent = PendingIntent.getActivity(this, 0,
			                new Intent("org.pmix.ui.PLAYBACK_VIEWER")
			                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
			        startForeground(STREAMINGSERVICE_STATUS, status);
				}
			}
		}

	}
	
	@Override
	public void trackChanged(MPDTrackChangedEvent event) {
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
		
	}
	@Override
	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void volumeChanged(MPDVolumeChangedEvent event) {
		// TODO Auto-generated method stub
		
	}
}