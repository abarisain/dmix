package org.pmix.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.a0z.mpd.event.MPDTrackPositionChangedEvent;
import org.a0z.mpd.event.MPDUpdateStateChangedEvent;
import org.a0z.mpd.event.MPDVolumeChangedEvent;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.pmix.ui.CoverAsyncHelper.CoverDownloadListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.ViewSwitcher.ViewFactory;


/**
 * MainMenuActivity is the starting activity of pmix
 * @author Rémi Flament, Stefan Agner
 * @version $Id:  $
 */
public class MainMenuActivity extends Activity implements StatusChangeListener, TrackPositionListener, CoverDownloadListener {

	private Logger myLogger = Logger.global;
	
	public static final String PREFS_NAME = "pmix.properties";

	public static final int PLAYLIST = 1;
	
	public static final int ARTISTS = 2;

	public static final int SETTINGS = 5;
	
	public static final int STREAM = 6;

	private TextView artistNameText;

	private TextView songNameText;

	private TextView albumNameText;


	public static final int ALBUMS = 4;

	public static final int FILES = 3;

	private SeekBar progressBarVolume = null;
	private SeekBar progressBarTrack = null;

	private TextView trackTime = null;

	private CoverAsyncHelper oCoverAsyncHelper = null;
	long lastSongTime = 0;
	long lastElapsedTime = 0;
	
	private ImageSwitcher coverSwitcher;

	private ProgressBar coverSwitcherProgress;

	private static final int VOLUME_STEP = 5;

	private static final int TRACK_STEP = 10;

	private static Toast notification = null;
	
	private StreamingService streamingServiceBound;
	private boolean isStreamServiceBound;
	
	private ButtonEventHandler buttonEventHandler;
	
	private boolean streamingMode;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case SETTINGS:
			break;

		default:
			break;
		}

	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		
		//WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
		
		myLogger.log(Level.INFO, "onCreate");
		MPDApplication app = (MPDApplication)getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addTrackPositionListener(this);
		app.setActivity(this);
		
		//registerReceiver(, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION) );
		registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION) );
		
		
		//oMPDAsyncHelper.addConnectionListener(MPDConnectionHandler.getInstance(this));

		init();
		
	}	
	
	@Override
	protected void onRestart() {
		super.onRestart();
		myLogger.log(Level.INFO, "onRestart");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		myLogger.log(Level.INFO, "onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		/*
		WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		int wifistate = wifi.getWifiState();
		if(wifistate!=wifi.WIFI_STATE_ENABLED && wifistate!=wifi.WIFI_STATE_ENABLING)
		{
			setTitle("No WIFI");
			return;
		}
		while(wifistate!=wifi.WIFI_STATE_ENABLED)
			setTitle("Waiting for WIFI");
		*/

	}

	private void init() {
		setContentView(R.layout.main);
		
		streamingMode = false;
		
		artistNameText = (TextView) findViewById(R.id.artistName);
		albumNameText = (TextView) findViewById(R.id.albumName);
		songNameText = (TextView) findViewById(R.id.songName);

		progressBarTrack = (SeekBar) findViewById(R.id.progress_track);
		progressBarVolume = (SeekBar) findViewById(R.id.progress_volume);

		trackTime = (TextView) findViewById(R.id.trackTime);

		
		coverSwitcher = (ImageSwitcher) findViewById(R.id.albumCover);
		coverSwitcher.setFactory(new ViewFactory() {

			public View makeView() {
				ImageView i = new ImageView(MainMenuActivity.this);

				i.setBackgroundColor(0x00FF0000);
				i.setScaleType(ImageView.ScaleType.FIT_CENTER);
				//i.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

				
				return i;
			}
		});
		coverSwitcherProgress = (ProgressBar) findViewById(R.id.albumCoverProgress); 
		coverSwitcherProgress.setIndeterminate(true);
		coverSwitcherProgress.setVisibility(ProgressBar.INVISIBLE);
		
		oCoverAsyncHelper = new CoverAsyncHelper();
		oCoverAsyncHelper.addCoverDownloadListener(this);
		buttonEventHandler = new ButtonEventHandler();
		ImageButton button = (ImageButton) findViewById(R.id.next);
		button.setOnClickListener(buttonEventHandler);
		
		button = (ImageButton) findViewById(R.id.prev);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) findViewById(R.id.back);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) findViewById(R.id.playpause);
		button.setOnClickListener(buttonEventHandler);
		button.setOnLongClickListener(buttonEventHandler);
		
		button = (ImageButton) findViewById(R.id.forward);
		button.setOnClickListener(buttonEventHandler);
		progressBarVolume.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub

				System.out.println("Vol2:" + progressBarVolume.getProgress());
			}
		});
		progressBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			
			
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromTouch) {
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				MPDApplication app = (MPDApplication)getApplication();
				Runnable async = new Runnable(){
					@SuppressWarnings("unchecked")
					@Override
					public void run() 
					{
						try {
							MPDApplication app = (MPDApplication)getApplication();
							app.oMPDAsyncHelper.oMPD.setVolume((int)progress);
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}
					public int progress;
					public Runnable setProgress(int prg)
					{
						progress =prg;
						return this;
					}
				}.setProgress(seekBar.getProgress());
				
				app.oMPDAsyncHelper.execAsync(async);
				
				/*
				try {
					MPDApplication app = (MPDApplication)getApplication();
					app.oMPDAsyncHelper.oMPD.setVolume(progress);
				} catch (MPDServerException e) {
					e.printStackTrace();
				}
				*/
				
			}
		});
		progressBarTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromTouch) {
				
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {

				MPDApplication app = (MPDApplication)getApplication();
				Runnable async = new Runnable(){
					@SuppressWarnings("unchecked")
					@Override
					public void run() 
					{
						try {
							MPDApplication app = (MPDApplication)getApplication();
							app.oMPDAsyncHelper.oMPD.seek((int)progress);
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}
					public int progress;
					public Runnable setProgress(int prg)
					{
						progress =prg;
						return this;
					}
				}.setProgress(seekBar.getProgress());
				
				app.oMPDAsyncHelper.execAsync(async);
				
				/*
				try {
					MPDApplication app = (MPDApplication)getApplication();
					app.oMPDAsyncHelper.oMPD.seek((int)progress);
				} catch (MPDServerException e) {
					e.printStackTrace();
				}
				*/
				
			}
		});
		
		myLogger.log(Level.INFO, "Initialization succeeded");
	}

	private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

		public void onClick(View v) {
			MPDApplication app = (MPDApplication)getApplication();
			MPD mpd = app.oMPDAsyncHelper.oMPD;
			Intent i = null;
			try {
				switch(v.getId()) {
					case R.id.next:
						mpd.next();
						if(((MPDApplication) getApplication()).isStreamingMode()) {
							i = new Intent(app, StreamingService.class);
							i.setAction("org.pmix.RESET_STREAMING");
							startService(i);
						}
						break;
					case R.id.prev:
						mpd.previous();
						if(((MPDApplication) getApplication()).isStreamingMode()) {
							i = new Intent(app, StreamingService.class);
							i.setAction("org.pmix.RESET_STREAMING");
							startService(i);
						}
						break;
					case R.id.back:
						mpd.seek(lastElapsedTime - TRACK_STEP);
						break;
					case R.id.forward:
						mpd.seek(lastElapsedTime + TRACK_STEP);
						break;
					case R.id.playpause:
						/**
						 * If playing or paused, just toggle state, otherwise start playing.
						 * @author slubman
						 */
						String state = mpd.getStatus().getState();
						if(state.equals(MPDStatus.MPD_STATE_PLAYING)
							|| state.equals(MPDStatus.MPD_STATE_PAUSED)) {
							mpd.pause();
						} else {
							mpd.play();
						}
						break;

				}
			
			} catch (MPDServerException e) {
				myLogger.log(Level.WARNING, e.getMessage());
			}
		}

		public boolean onLongClick(View v) {
			MPDApplication app = (MPDApplication)getApplication();
			MPD mpd = app.oMPDAsyncHelper.oMPD;
			try {
				switch(v.getId()) {
					case R.id.playpause:
						// Implements the ability to stop playing (may be useful for streams)
						mpd.stop();
						Intent i;
						if(((MPDApplication) getApplication()).isStreamingMode()) {
							i = new Intent(app, StreamingService.class);
							i.setAction("org.pmix.STOP_STREAMING");
							startService(i);
						}
						break;
					default:
						return false;
				}
				return true;
			} catch (MPDServerException e) {
				
			}
			return true;
		}
		
		
	}
	
	
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		MPDApplication app = (MPDApplication)getApplication();
		try {
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if(((MPDApplication) getApplication()).isStreamingMode()) {
					return super.onKeyDown(keyCode, event);
				} else {
					progressBarVolume.incrementProgressBy(VOLUME_STEP);
					app.oMPDAsyncHelper.oMPD.adjustVolume(VOLUME_STEP);
					return true;
				}
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if(((MPDApplication) getApplication()).isStreamingMode()) {
					return super.onKeyDown(keyCode, event);
				} else {
					progressBarVolume.incrementProgressBy(-VOLUME_STEP);
					app.oMPDAsyncHelper.oMPD.adjustVolume(-VOLUME_STEP);
					return true;
				}
			case KeyEvent.KEYCODE_DPAD_LEFT:
				app.oMPDAsyncHelper.oMPD.previous();
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				app.oMPDAsyncHelper.oMPD.next();
				return true;
			default:
				return super.onKeyDown(keyCode, event);
			}
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0,ARTISTS, 0, R.string.artists).setIcon(R.drawable.ic_menu_pmix_artists);
		menu.add(0,ALBUMS, 1, R.string.albums).setIcon(R.drawable.ic_menu_pmix_albums);
		menu.add(0,FILES, 2, R.string.files).setIcon(android.R.drawable.ic_menu_agenda);
		menu.add(0,PLAYLIST, 3, R.string.playlist).setIcon(R.drawable.ic_menu_pmix_playlist);
		menu.add(0,STREAM, 4, R.string.stream).setIcon(android.R.drawable.ic_menu_slideshow);
		menu.add(0,SETTINGS, 5, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
		
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent i = null;
		
		switch (item.getItemId()) {

		case ARTISTS:
			i = new Intent(this, ArtistsActivity.class);
			startActivityForResult(i, ARTISTS);
			return true;
		case ALBUMS:
			i = new Intent(this, AlbumsActivity.class);
			startActivityForResult(i, ALBUMS);
			return true;
		case FILES:
			i = new Intent(this, FSActivity.class);
			startActivityForResult(i, FILES);
			return true;
		case SETTINGS:
			i = new Intent(this, SettingsActivity.class);
			startActivityForResult(i, SETTINGS);
			return true;
		case PLAYLIST:
			i = new Intent(this, PlaylistActivity.class);
			startActivityForResult(i, PLAYLIST);
			// TODO juste pour s'y retrouver
			return true;
		case STREAM:
			if(((MPDApplication) getApplication()).isStreamingMode()) { // yeah, yeah getApplication for that may be ugly but ...
				i = new Intent(this, StreamingService.class);
				i.setAction("org.pmix.KILL");
				stopService(i);
				((MPDApplication) getApplication()).setStreamingMode(false);
				Toast.makeText(this, "Streaming OFF", Toast.LENGTH_SHORT).show();
			} else {
				i = new Intent(this, StreamingService.class);
				i.setAction("org.pmix.START_STREAMING");
				startService(i);
				((MPDApplication) getApplication()).setStreamingMode(true);
				Toast.makeText(this, "Streaming ON", Toast.LENGTH_SHORT).show();
			}

			return true;
		default:
			// showAlert("Menu Item Clicked", "Not yet implemented", "ok", null,
			// false, null);
			return true;
		}

	}

	//private MPDPlaylist playlist;
	public void playlistChanged(MPDPlaylistChangedEvent event) {
		try {
			MPDApplication app = (MPDApplication)getApplication();
			app.oMPDAsyncHelper.oMPD.getPlaylist().refresh();
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void randomChanged(MPDRandomChangedEvent event) {
		// TODO Auto-generated method stub

	}

	public void repeatChanged(MPDRepeatChangedEvent event) {
		// TODO Auto-generated method stub

	}

	public void stateChanged(MPDStateChangedEvent event) {
		MPDStatus status = event.getMpdStatus();
		
		String state = status.getState();
		if(state!=null)
		{

			if(state.equals(MPDStatus.MPD_STATE_PLAYING))
			{
				ImageButton button = (ImageButton) findViewById(R.id.playpause);
				button.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
			} else {
				ImageButton button = (ImageButton) findViewById(R.id.playpause);
				button.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
			}
		}
	}
	private String lastArtist = "";
	private String lastAlbum = "";
	public void trackChanged(MPDTrackChangedEvent event) {

		MPDStatus status = event.getMpdStatus();
		if(status!=null)
		{
			String state = status.getState();
			if(state != null)
			{
				int songId = status.getSongPos();
				if(songId>=0)
				{

					MPDApplication app = (MPDApplication)getApplication();
					Music actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getMusic(songId);
					String artist = actSong.getArtist();
					String title = actSong.getTitle();
					String album = actSong.getAlbum();
					artist = artist==null ? "" : artist;
					title = title==null ? "" : title;
					album = album==null ? "" : album;
					artistNameText.setText(artist);
					songNameText.setText(title);
					albumNameText.setText(album);
					progressBarTrack.setMax((int)actSong.getTime());
					if(!lastAlbum.equals(album) || !lastArtist.equals(artist))
					{
						coverSwitcher.setVisibility(ImageSwitcher.INVISIBLE);
						coverSwitcherProgress.setVisibility(ProgressBar.VISIBLE);
						oCoverAsyncHelper.downloadCover(artist, album);
						lastArtist = artist;
						lastAlbum = album;
					}
				}
				else
				{
					artistNameText.setText("");
					songNameText.setText("");
					albumNameText.setText("");
					progressBarTrack.setMax(0);
				}
			}
		}
	}

	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionStateChanged(MPDConnectionStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void volumeChanged(MPDVolumeChangedEvent event) {
		progressBarVolume.setProgress(event.getMpdStatus().getVolume());
	}

	@Override
	protected void onPause() {
		super.onPause();
		myLogger.log(Level.INFO, "onPause");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		myLogger.log(Level.INFO, "onStop");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.unsetActivity(this);
		myLogger.log(Level.INFO, "onDestroy");
	}
	
	public SeekBar getVolumeSeekBar() {
		return progressBarVolume;
	}

	public SeekBar getProgressBarTrack() {
		return progressBarTrack;
	}


	public void trackPositionChanged(MPDTrackPositionChangedEvent event) {
		MPDStatus status = event.getMpdStatus();
		lastElapsedTime = status.getElapsedTime();
		lastSongTime = status.getTotalTime();
		trackTime.setText(timeToString(lastElapsedTime) + " - " + timeToString(lastSongTime));
		progressBarTrack.setProgress((int) status.getElapsedTime());
	}


	private static String timeToString(long seconds) {
		long min = seconds / 60;
		long sec = seconds - min * 60;
		return (min < 10 ? "0" + min : min) + ":" + (sec < 10 ? "0" + sec : sec);
	}

    public static void notifyUser(String message, Context context) {
            if (notification != null) {
                    notification.setText(message);
                    notification.show();
            } else {
                    notification = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                    notification.show();
            }
    }

	public void onCoverDownloaded(Bitmap cover) {
		coverSwitcherProgress.setVisibility(ProgressBar.INVISIBLE);
		coverSwitcher.setImageDrawable(new BitmapDrawable(cover));
		coverSwitcher.setVisibility(ImageSwitcher.VISIBLE);
		
	}

	public void onCoverNotFound() {
		coverSwitcherProgress.setVisibility(ProgressBar.INVISIBLE);
		coverSwitcher.setImageResource(R.drawable.gmpcnocover);
		coverSwitcher.setVisibility(ImageSwitcher.VISIBLE);
		
	}

	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        streamingServiceBound = ((StreamingService.LocalBinder)service).getService();

	        // Tell the user about this for our demo.
	        Toast.makeText((MPDApplication)getApplication(), "Connected to service", Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	    	streamingServiceBound = null;
	        Toast.makeText((MPDApplication)getApplication(), "Service disconnected", Toast.LENGTH_SHORT).show();
	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
	    bindService(new Intent(this, StreamingService.class), mConnection, Context.BIND_AUTO_CREATE);
	    isStreamServiceBound = true;
	}

	void doUnbindService() {
	    if (isStreamServiceBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        isStreamServiceBound = false;
	    }
	}

	
}