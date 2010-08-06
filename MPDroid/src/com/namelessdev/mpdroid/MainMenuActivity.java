package com.namelessdev.mpdroid;

import java.util.Timer;
import java.util.TimerTask;
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.namelessdev.mpdroid.CoverAsyncHelper.CoverDownloadListener;

/**
 * MainMenuActivity is the starting activity of pmix
 * 
 * @author Rémi Flament, Stefan Agner
 * @version $Id: $
 */
public class MainMenuActivity extends Activity implements StatusChangeListener, TrackPositionListener, CoverDownloadListener {

	private Logger myLogger = Logger.global;

	public static final String PREFS_NAME = "mpdroid.properties";

	public static final int PLAYLIST = 1;

	public static final int ARTISTS = 2;

	public static final int SETTINGS = 5;

	public static final int STREAM = 6;

	public static final int LIBRARY = 7;

	public static final int CONNECT = 8;

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

	private static final int ANIMATION_DURATION_MSEC = 1000;

	private static Toast notification = null;

	private StreamingService streamingServiceBound;
	private boolean isStreamServiceBound;

	private ButtonEventHandler buttonEventHandler;

	private boolean streamingMode;
	private boolean connected;

	private Timer volTimer = new Timer();
	private TimerTask volTimerTask = null;

	// Used for detecing sideways flings
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

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
		// WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
		myLogger.log(Level.INFO, "onCreate");
		MPDApplication app = (MPDApplication) getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addTrackPositionListener(this);
		app.setActivity(this);
		// registerReceiver(, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION) );
		registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

		gestureDetector = new GestureDetector(new MyGestureDetector(this));
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
		// oMPDAsyncHelper.addConnectionListener(MPDConnectionHandler.getInstance(this));
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
		myLogger.log(Level.INFO, "onResume");
		// Annoyingly this seams to be run when the app starts the first time to.
		// Just to make sure that we do actually get an update.
		try { 
			updateTrackInfo();
		} catch ( Exception e) {
			e.printStackTrace();
		}

		/*
		 * WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE); int wifistate = wifi.getWifiState();
		 * if(wifistate!=wifi.WIFI_STATE_ENABLED && wifistate!=wifi.WIFI_STATE_ENABLING) { setTitle("No WIFI"); return; }
		 * while(wifistate!=wifi.WIFI_STATE_ENABLED) setTitle("Waiting for WIFI");
		 */

	}

	private void init() {
		setContentView(R.layout.main);

		streamingMode = ((MPDApplication) getApplication()).isStreamingMode();
		connected = ((MPDApplication) getApplication()).oMPDAsyncHelper.oMPD.isConnected();
		artistNameText = (TextView) findViewById(R.id.artistName);
		albumNameText = (TextView) findViewById(R.id.albumName);
		songNameText = (TextView) findViewById(R.id.songName);

		progressBarTrack = (SeekBar) findViewById(R.id.progress_track);
		progressBarVolume = (SeekBar) findViewById(R.id.progress_volume);

		trackTime = (TextView) findViewById(R.id.trackTime);

		Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		fadeIn.setDuration(ANIMATION_DURATION_MSEC);
		Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		fadeOut.setDuration(ANIMATION_DURATION_MSEC);

		coverSwitcher = (ImageSwitcher) findViewById(R.id.albumCover);
		coverSwitcher.setFactory(new ViewFactory() {

			public View makeView() {
				ImageView i = new ImageView(MainMenuActivity.this);

				i.setBackgroundColor(0x00FF0000);
				i.setScaleType(ImageView.ScaleType.FIT_CENTER);
				// i.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

				return i;
			}
		});
		coverSwitcher.setInAnimation(fadeIn);
		coverSwitcher.setOutAnimation(fadeOut);

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
		progressBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				volTimerTask = new TimerTask() {
					public void run() {
						MPDApplication app = (MPDApplication) getApplication();
						try {
							if (lastSentVol != progress.getProgress()) {
								lastSentVol = progress.getProgress();
								app.oMPDAsyncHelper.oMPD.setVolume(lastSentVol);
							}
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}

					int lastSentVol = -1;
					SeekBar progress;

					public TimerTask setProgress(SeekBar prg) {
						progress = prg;
						return this;
					}
				}.setProgress(seekBar);

				volTimer.scheduleAtFixedRate(volTimerTask, 0, 100);
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				volTimerTask.cancel();
				// Afraid this will run syncronious
				volTimerTask.run();

				/*
				 * try { MPDApplication app = (MPDApplication)getApplication(); app.oMPDAsyncHelper.oMPD.setVolume(progress); } catch
				 * (MPDServerException e) { e.printStackTrace(); }
				 */

			}
		});
		progressBarTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onStopTrackingTouch(SeekBar seekBar) {

				MPDApplication app = (MPDApplication) getApplication();
				Runnable async = new Runnable() {
					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							MPDApplication app = (MPDApplication) getApplication();
							app.oMPDAsyncHelper.oMPD.seek((int) progress);
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}

					public int progress;

					public Runnable setProgress(int prg) {
						progress = prg;
						return this;
					}
				}.setProgress(seekBar.getProgress());

				app.oMPDAsyncHelper.execAsync(async);

				/*
				 * try { MPDApplication app = (MPDApplication)getApplication(); app.oMPDAsyncHelper.oMPD.seek((int)progress); } catch
				 * (MPDServerException e) { e.printStackTrace(); }
				 */

			}
		});

		songNameText.setText(getResources().getString(R.string.notConnected));
		myLogger.log(Level.INFO, "Initialization succeeded");
	}

	private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

		public void onClick(View v) {
			MPDApplication app = (MPDApplication) getApplication();
			MPD mpd = app.oMPDAsyncHelper.oMPD;
			Intent i = null;
			try {
				switch (v.getId()) {
				case R.id.next:
					mpd.next();
					if (((MPDApplication) getApplication()).isStreamingMode()) {
						i = new Intent(app, StreamingService.class);
						i.setAction("com.namelessdev.mpdroid.RESET_STREAMING");
						startService(i);
					}
					break;
				case R.id.prev:
					mpd.previous();
					if (((MPDApplication) getApplication()).isStreamingMode()) {
						i = new Intent(app, StreamingService.class);
						i.setAction("com.namelessdev.mpdroid.RESET_STREAMING");
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
					 * 
					 * @author slubman
					 */
					String state = mpd.getStatus().getState();
					if (state.equals(MPDStatus.MPD_STATE_PLAYING) || state.equals(MPDStatus.MPD_STATE_PAUSED)) {
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
			MPDApplication app = (MPDApplication) getApplication();
			MPD mpd = app.oMPDAsyncHelper.oMPD;
			try {
				switch (v.getId()) {
				case R.id.playpause:
					// Implements the ability to stop playing (may be useful for streams)
					mpd.stop();
					Intent i;
					if (((MPDApplication) getApplication()).isStreamingMode()) {
						i = new Intent(app, StreamingService.class);
						i.setAction("com.namelessdev.mpdroid.STOP_STREAMING");
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
		MPDApplication app = (MPDApplication) getApplication();
		try {
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (((MPDApplication) getApplication()).isStreamingMode()) {
					return super.onKeyDown(keyCode, event);
				} else {
					progressBarVolume.incrementProgressBy(VOLUME_STEP);
					app.oMPDAsyncHelper.oMPD.adjustVolume(VOLUME_STEP);
					return true;
				}
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (((MPDApplication) getApplication()).isStreamingMode()) {
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

		menu.add(0, LIBRARY, 1, R.string.libraryTabActivity).setIcon(R.drawable.ic_menu_music_library);
		menu.add(0, PLAYLIST, 3, R.string.playlist).setIcon(R.drawable.ic_menu_pmix_playlist);
		menu.add(0, STREAM, 4, R.string.stream).setIcon(android.R.drawable.ic_menu_upload_you_tube);
		menu.add(0, SETTINGS, 5, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
		return result;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event))
			return true;
		return false;
	}

	// Most of this comes from
	// http://www.codeshogun.com/blog/2009/04/16/how-to-implement-swipe-action-in-android/
	//
	class MyGestureDetector extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 120;
		private static final int SWIPE_MAX_OFF_PATH = 250;
		private static final int SWIPE_THRESHOLD_VELOCITY = 200;
		private Activity context = null;

		public MyGestureDetector(Activity activity) {
			context = activity;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// Next
					Toast.makeText(context, getResources().getString(R.string.next), Toast.LENGTH_SHORT).show();
					MPDApplication app = (MPDApplication) getApplication();
					MPD mpd = app.oMPDAsyncHelper.oMPD;
					mpd.next();
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// Previous
					Toast.makeText(context, getResources().getString(R.string.previous), Toast.LENGTH_SHORT).show();
					MPDApplication app = (MPDApplication) getApplication();
					MPD mpd = app.oMPDAsyncHelper.oMPD;
					mpd.previous();
				}
			} catch (Exception e) {
				// nothing
			}
			return false;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MPDApplication app = (MPDApplication) getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		if (!mpd.isConnected()) {
			if (menu.findItem(CONNECT) == null) {
				menu.findItem(LIBRARY).setEnabled(false);
				menu.findItem(PLAYLIST).setEnabled(false);
				menu.findItem(STREAM).setEnabled(false);
				menu.add(0, CONNECT, 0, R.string.connect);
			}
		} else {
			if (menu.findItem(CONNECT) != null) {
				menu.findItem(LIBRARY).setEnabled(true);
				menu.findItem(PLAYLIST).setEnabled(true);
				menu.findItem(STREAM).setEnabled(true);
				menu.removeItem(CONNECT);
			}
		}
		return super.onPrepareOptionsMenu(menu);
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
		case LIBRARY:
			i = new Intent(this, LibraryTabActivity.class);
			startActivityForResult(i, SETTINGS);
			return true;
		case SETTINGS:
			if (((MPDApplication) getApplication()).oMPDAsyncHelper.oMPD.isMpdConnectionNull()) {
				startActivityForResult(new Intent(this, WifiConnectionSettings.class), SETTINGS);
			} else {
				i = new Intent(this, SettingsActivity.class);
				startActivityForResult(i, SETTINGS);
			}
			return true;
		case PLAYLIST:
			i = new Intent(this, PlaylistActivity.class);
			startActivityForResult(i, PLAYLIST);
			// TODO juste pour s'y retrouver
			return true;
		case CONNECT:
			((MPDApplication) getApplication()).connect();
			return true;
		case STREAM:
			if (((MPDApplication) getApplication()).isStreamingMode()) { // yeah, yeah getApplication for that may be ugly but ...
				i = new Intent(this, StreamingService.class);
				i.setAction("com.namelessdev.mpdroid.DIE");
				startService(i);
				((MPDApplication) getApplication()).setStreamingMode(false);
				// Toast.makeText(this, "MPD Streaming Stopped", Toast.LENGTH_SHORT).show();
			} else {
				i = new Intent(this, StreamingService.class);
				i.setAction("com.namelessdev.mpdroid.START_STREAMING");
				startService(i);
				((MPDApplication) getApplication()).setStreamingMode(true);
				// Toast.makeText(this, "MPD Streaming Started", Toast.LENGTH_SHORT).show();
			}

			return true;
		default:
			// showAlert("Menu Item Clicked", "Not yet implemented", "ok", null,
			// false, null);
			return true;
		}

	}

	// private MPDPlaylist playlist;
	public void playlistChanged(MPDPlaylistChangedEvent event) {
		// Can someone explain why this is nessesary? 
		// Maybe the song gets changed before the playlist? 
		// Makes little sense tho.
		try { 
			updateTrackInfo();
		} catch ( Exception e) {
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
		if (state != null) {

			if (state.equals(MPDStatus.MPD_STATE_PLAYING)) {
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

	public void updateTrackInfo()  {
		MPDApplication app = (MPDApplication) getApplication();
		try {
			updateTrackInfo(app.oMPDAsyncHelper.oMPD.getStatus());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	
	}
	
	public void updateTrackInfo(MPDStatus status) {
		if (status != null) {
			String state = status.getState();
			if (state != null) {
				int songId = status.getSongPos();
				if (songId >= 0) {

					MPDApplication app = (MPDApplication) getApplication();
					Music actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getMusic(songId);
					String artist = actSong.getArtist();
					String title = actSong.getTitle();
					String album = actSong.getAlbum();
					artist = artist == null ? "" : artist;
					title = title == null ? "" : title;
					album = album == null ? "" : album;
					artistNameText.setText(artist);
					songNameText.setText(title);
					albumNameText.setText(album);
					progressBarTrack.setMax((int) actSong.getTime());
					if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) {
						// coverSwitcher.setVisibility(ImageSwitcher.INVISIBLE);
						coverSwitcherProgress.setVisibility(ProgressBar.VISIBLE);
						oCoverAsyncHelper.downloadCover(artist, album);
						lastArtist = artist;
						lastAlbum = album;
					}
				} else {
					artistNameText.setText("");
					songNameText.setText("");
					albumNameText.setText("");
					progressBarTrack.setMax(0);
				}
			}
		}
	}
	
	public void trackChanged(MPDTrackChangedEvent event) {
		updateTrackInfo(event.getMpdStatus());
	}

	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void connectionStateChanged(MPDConnectionStateChangedEvent event) {
		// TODO Auto-generated method stub
		checkConnected();
		/*
		 * MPDStatus status = event.getMpdStatus();
		 * 
		 * String state = status.getState();
		 */
	}

	public void checkConnected() {
		connected = ((MPDApplication) getApplication()).oMPDAsyncHelper.oMPD.isConnected();
		if (connected) {
			songNameText.setText(getResources().getString(R.string.noSongInfo));
		} else {
			songNameText.setText(getResources().getString(R.string.notConnected));
		}
		return;
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
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.oMPDAsyncHelper.removeStatusChangeListener(this);
		app.oMPDAsyncHelper.removeTrackPositionListener(this);
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
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		if (cover != null) {
			cover.setDensity((int) metrics.density);
			BitmapDrawable myCover = new BitmapDrawable(cover);
			coverSwitcher.setImageDrawable(myCover);
			// coverSwitcher.setVisibility(ImageSwitcher.VISIBLE);
			coverSwitcher.showNext(); // Little trick so the animation gets displayed
			coverSwitcher.showPrevious();
		} else {
			// Should not be happening, but happened.
			onCoverNotFound();
		}
	}

	public void onCoverNotFound() {
		coverSwitcherProgress.setVisibility(ProgressBar.INVISIBLE);
		coverSwitcher.setImageResource(R.drawable.gmpcnocover);
		// coverSwitcher.setVisibility(ImageSwitcher.VISIBLE);
	}

}
