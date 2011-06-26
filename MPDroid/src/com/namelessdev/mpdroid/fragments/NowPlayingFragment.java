package com.namelessdev.mpdroid.fragments;

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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

import com.namelessdev.mpdroid.CoverAsyncHelper;
import com.namelessdev.mpdroid.CoverAsyncHelper.CoverDownloadListener;
import com.namelessdev.mpdroid.LibraryTabActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MPDConnectionHandler;
import com.namelessdev.mpdroid.PlaylistActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.ServerListActivity;
import com.namelessdev.mpdroid.SettingsActivity;
import com.namelessdev.mpdroid.StreamingService;
import com.namelessdev.mpdroid.WifiConnectionSettings;
import com.namelessdev.mpdroid.providers.ServerList;

/**
 * MainMenuActivity is the starting activity of pmix
 * 
 * @author Rémi Flament, Stefan Agner
 * @version $Id: $
 */
public class NowPlayingFragment extends Fragment implements StatusChangeListener, TrackPositionListener, CoverDownloadListener,
		OnSharedPreferenceChangeListener {

	private Logger myLogger = Logger.global;
	
	public static final String COVER_BASE_URL = "http://%s/music/";

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

	private ButtonEventHandler buttonEventHandler;

	@SuppressWarnings("unused")
	private boolean streamingMode;
	private boolean connected;

	private Timer volTimer = new Timer();
	private TimerTask volTimerTask = null;

	// Used for detecting sideways flings
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	private boolean enableLastFM;
	private boolean newUI;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
		// myLogger.log(Level.INFO, "onCreate");

		setHasOptionsMenu(true);
		getActivity().setTitle(getResources().getString(R.string.nowPlaying));

		// registerReceiver(, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION) );
		getActivity().registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

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
	}

	@Override
	public void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getActivity().getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addTrackPositionListener(this);
		app.setActivity(this);
		// myLogger.log(Level.INFO, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		// myLogger.log(Level.INFO, "onResume");
		// Annoyingly this seams to be run when the app starts the first time to.
		// Just to make sure that we do actually get an update.
		try {
			updateTrackInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			progressBarVolume.setProgress(app.oMPDAsyncHelper.oMPD.getStatus().getVolume());
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE); int wifistate = wifi.getWifiState();
		 * if(wifistate!=wifi.WIFI_STATE_ENABLED && wifistate!=wifi.WIFI_STATE_ENABLING) { setTitle("No WIFI"); return; }
		 * while(wifistate!=wifi.WIFI_STATE_ENABLED) setTitle("Waiting for WIFI");
		 */

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_fragment, container, false);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		settings.registerOnSharedPreferenceChangeListener(this);

		enableLastFM = settings.getBoolean("enableLastFM", true);
		newUI = settings.getBoolean("newUI", false);

		/*
		 * if (newUI) { setContentView(R.layout.main); } else { setContentView(R.layout.main_old); }
		 */

		streamingMode = ((MPDApplication) getActivity().getApplication()).isStreamingMode();
		connected = ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.isConnected();
		artistNameText = (TextView) view.findViewById(R.id.artistName);
		albumNameText = (TextView) view.findViewById(R.id.albumName);
		songNameText = (TextView) view.findViewById(R.id.songName);

		progressBarTrack = (SeekBar) view.findViewById(R.id.progress_track);
		progressBarVolume = (SeekBar) view.findViewById(R.id.progress_volume);

		trackTime = (TextView) view.findViewById(R.id.trackTime);

		Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
		fadeIn.setDuration(ANIMATION_DURATION_MSEC);
		Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
		fadeOut.setDuration(ANIMATION_DURATION_MSEC);

		coverSwitcher = (ImageSwitcher) view.findViewById(R.id.albumCover);
		coverSwitcher.setFactory(new ViewFactory() {

			public View makeView() {
				ImageView i = new ImageView(NowPlayingFragment.this.getActivity());

				i.setBackgroundColor(0x00FF0000);
				i.setScaleType(ImageView.ScaleType.FIT_CENTER);
				// i.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

				return i;
			}
		});
		coverSwitcher.setInAnimation(fadeIn);
		coverSwitcher.setOutAnimation(fadeOut);

		coverSwitcherProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
		coverSwitcherProgress.setIndeterminate(true);
		coverSwitcherProgress.setVisibility(ProgressBar.INVISIBLE);

		oCoverAsyncHelper = new CoverAsyncHelper();
		oCoverAsyncHelper.addCoverDownloadListener(this);
		buttonEventHandler = new ButtonEventHandler();
		ImageButton button = (ImageButton) view.findViewById(R.id.next);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.prev);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.back);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.playpause);
		button.setOnClickListener(buttonEventHandler);
		button.setOnLongClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.forward);
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
						MPDApplication app = (MPDApplication) getActivity().getApplication();
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
				 * try { MPDApplication app = (MPDApplication)getActivity().getApplication(); app.oMPDAsyncHelper.oMPD.setVolume(progress);
				 * } catch (MPDServerException e) { e.printStackTrace(); }
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

				MPDApplication app = (MPDApplication) getActivity().getApplication();
				Runnable async = new Runnable() {
					// @SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							MPDApplication app = (MPDApplication) getActivity().getApplication();
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
				 * try { MPDApplication app = (MPDApplication)getActivity().getApplication(); app.oMPDAsyncHelper.oMPD.seek((int)progress);
				 * } catch (MPDServerException e) { e.printStackTrace(); }
				 */

			}
		});

		songNameText.setText(getResources().getString(R.string.notConnected));
		myLogger.log(Level.INFO, "Initialization succeeded");

		return view;
	}

	private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

		public void onClick(View v) {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			MPD mpd = app.oMPDAsyncHelper.oMPD;
			Intent i = null;
			try {
				switch (v.getId()) {
				case R.id.next:
					mpd.next();
					if (((MPDApplication) getActivity().getApplication()).isStreamingMode()) {
						i = new Intent(app, StreamingService.class);
						i.setAction("com.namelessdev.mpdroid.RESET_STREAMING");
						getActivity().startService(i);
					}
					break;
				case R.id.prev:
					mpd.previous();
					if (((MPDApplication) getActivity().getApplication()).isStreamingMode()) {
						i = new Intent(app, StreamingService.class);
						i.setAction("com.namelessdev.mpdroid.RESET_STREAMING");
						getActivity().startService(i);
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
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			MPD mpd = app.oMPDAsyncHelper.oMPD;
			try {
				switch (v.getId()) {
				case R.id.playpause:
					// Implements the ability to stop playing (may be useful for streams)
					mpd.stop();
					Intent i;
					if (((MPDApplication) getActivity().getApplication()).isStreamingMode()) {
						i = new Intent(app, StreamingService.class);
						i.setAction("com.namelessdev.mpdroid.STOP_STREAMING");
						getActivity().startService(i);
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

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		MPDApplication app = (MPDApplication) getActivity().getApplication();
		try {
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (((MPDApplication) getActivity().getApplication()).isStreamingMode()) {
					return false;
				} else {
					progressBarVolume.incrementProgressBy(VOLUME_STEP);
					app.oMPDAsyncHelper.oMPD.adjustVolume(VOLUME_STEP);
					return true;
				}
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (((MPDApplication) getActivity().getApplication()).isStreamingMode()) {
					return false;
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
				return false;
			}
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.mpd_mainmenu, menu);

		/*
		 * menu.add(0, LIBRARY, 1, R.string.libraryTabActivity).setIcon(R.drawable.ic_menu_music_library); menu.add(0, PLAYLIST, 3,
		 * R.string.playlist).setIcon(R.drawable.ic_menu_pmix_playlist); menu.add(0, STREAM, 4,
		 * R.string.stream).setIcon(android.R.drawable.ic_menu_upload_you_tube); menu.add(0, SETTINGS, 5,
		 * R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
		 */
		// return result;
	}

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

		public MyGestureDetector(Fragment fragment) {
			context = fragment.getActivity();
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
					MPDApplication app = (MPDApplication) getActivity().getApplication();
					MPD mpd = app.oMPDAsyncHelper.oMPD;
					mpd.next();
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// Previous
					Toast.makeText(context, getResources().getString(R.string.previous), Toast.LENGTH_SHORT).show();
					MPDApplication app = (MPDApplication) getActivity().getApplication();
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
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MPDApplication app = (MPDApplication) getActivity().getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		if (!mpd.isConnected()) {
			if (menu.findItem(CONNECT) == null) {
				menu.findItem(R.id.GMM_LibTab).setEnabled(false);
				menu.findItem(R.id.GMM_Playlist).setEnabled(false);
				menu.findItem(R.id.GMM_Stream).setEnabled(false);
				menu.add(0, CONNECT, 0, R.string.connect);
			}
		} else {
			if (menu.findItem(CONNECT) != null) {
				menu.findItem(R.id.GMM_LibTab).setEnabled(true);
				menu.findItem(R.id.GMM_Playlist).setEnabled(true);
				menu.findItem(R.id.GMM_Stream).setEnabled(true);
				menu.removeItem(CONNECT);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent i = null;

		// Handle item selection
		switch (item.getItemId()) {
		/*
		 * TODO: Remove this code it seems unused case ARTISTS: i = new Intent(this, ArtistsActivity.class); //startActivityForResult(i,
		 * ARTISTS); return true; case ALBUMS: i = new Intent(this, AlbumsActivity.class); //startActivityForResult(i, ALBUMS); return true;
		 * case FILES: i = new Intent(this, FSActivity.class); //startActivityForResult(i, FILES); return true;
		 */
		case R.id.GMM_LibTab:
			i = new Intent(getActivity(), LibraryTabActivity.class);
			startActivity(i);
			return true;
		case R.id.GMM_Settings:
			if (((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.isMpdConnectionNull()) {
				startActivityForResult(new Intent(getActivity(), WifiConnectionSettings.class), SETTINGS);
			} else {
				i = new Intent(getActivity(), SettingsActivity.class);
				startActivityForResult(i, SETTINGS);
			}
			return true;
		case R.id.GMM_Playlist:
			i = new Intent(getActivity(), PlaylistActivity.class);
			startActivity(i);
			// TODO juste pour s'y retrouver
			return true;
		case CONNECT:
			((MPDApplication) getActivity().getApplication()).connect();
			return true;
		case R.id.GMM_Stream:
			if (((MPDApplication) getActivity().getApplication()).isStreamingMode()) { // yeah, yeah getApplication for that may be ugly but
																						// ...
				i = new Intent(getActivity(), StreamingService.class);
				i.setAction("com.namelessdev.mpdroid.DIE");
				getActivity().startService(i);
				((MPDApplication) getActivity().getApplication()).setStreamingMode(false);
				// Toast.makeText(this, "MPD Streaming Stopped", Toast.LENGTH_SHORT).show();
			} else {
				i = new Intent(getActivity(), StreamingService.class);
				i.setAction("com.namelessdev.mpdroid.START_STREAMING");
				getActivity().startService(i);
				((MPDApplication) getActivity().getApplication()).setStreamingMode(true);
				// Toast.makeText(this, "MPD Streaming Started", Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.GMM_bonjour:
			ContentResolver cr = getActivity().getContentResolver();
			ContentValues values = new ContentValues();
			values.put(ServerList.ServerColumns.NAME, "bite1");
			values.put(ServerList.ServerColumns.HOST, "bite2");
			values.put(ServerList.ServerColumns.PASSWORD, "");
			cr.insert(ServerList.ServerColumns.CONTENT_URI, values);
			values = new ContentValues();
			values.put(ServerList.ServerColumns.NAME, "bite3");
			values.put(ServerList.ServerColumns.HOST, "bite4");
			values.put(ServerList.ServerColumns.PASSWORD, "");
			cr.insert(ServerList.ServerColumns.CONTENT_URI, values);
			startActivity(new Intent(getActivity(), ServerListActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	// private MPDPlaylist playlist;
	public void playlistChanged(MPDPlaylistChangedEvent event) {
		// Can someone explain why this is nessesary?
		// Maybe the song gets changed before the playlist?
		// Makes little sense tho.
		try {
			updateTrackInfo();
		} catch (Exception e) {
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
				ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
				if (newUI) {
					button.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_pause));
				} else {
					button.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
				}
			} else {
				ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
				if (newUI) {
					button.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_play));
				} else {
					button.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
				}
			}
		}
	}

	private String lastArtist = "";
	private String lastAlbum = "";

	public void updateTrackInfo() {
		new updateTrackInfoAsync().execute((MPDStatus[]) null);
	}

	public void updateTrackInfo(MPDStatus status) {
		new updateTrackInfoAsync().execute(status);
	}

	public class updateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Boolean> {
		Music actSong = null;
		MPDStatus status = null;

		@Override
		protected Boolean doInBackground(MPDStatus... params) {
			if (params == null) {
				MPDApplication app = (MPDApplication) getActivity().getApplication();
				try {
					// A recursive call doesn't seem that bad here.
					return doInBackground(app.oMPDAsyncHelper.oMPD.getStatus());
				} catch (MPDServerException e) {
					e.printStackTrace();
				}
				return false;
			}
			if (params[0] != null) {
				String state = params[0].getState();
				if (state != null) {
					int songId = params[0].getSongPos();
					if (songId >= 0) {
						MPDApplication app = (MPDApplication) getActivity().getApplication();
						actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getMusic(songId);
						status = params[0];
						return true;
					}
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				String artist = null;
				String title = null;
				String album = null;
				int songMax = 0;
				if (actSong == null || status.getPlaylistLength() == 0) {
					title = getResources().getString(R.string.noSongInfo);
				} else {
					Log.d("MPDroid", "We did find an artist");
					artist = actSong.getArtist();
					title = actSong.getTitle();
					album = actSong.getAlbum();
					songMax = (int) actSong.getTime();
				}

				artist = artist == null ? "" : artist;
				title = title == null ? "" : title;
				album = album == null ? "" : album;

				artistNameText.setText(artist);
				songNameText.setText(title);
				albumNameText.setText(album);
				progressBarTrack.setMax(songMax);
				if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) {
					// coverSwitcher.setVisibility(ImageSwitcher.INVISIBLE);
					coverSwitcherProgress.setVisibility(ProgressBar.VISIBLE);
					if (enableLastFM) {
						oCoverAsyncHelper.downloadCover(artist, album);
					} else {
						// Try to find the cover from apache (vortexbox)
						// TODO : Make it configurable ...
						oCoverAsyncHelper.setUrlOverride(String.format(COVER_BASE_URL + actSong.getPath() + "/cover.jpg",
								((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.getConnectionInfoServer()));
						oCoverAsyncHelper.downloadCover(null, null);
						// Dirty hack ? Maybe. I don't feel like writing a new function.
						// onCoverNotFound();
					}
					lastArtist = artist;
					lastAlbum = album;
				}
			} else {
				artistNameText.setText("");
				songNameText.setText(R.string.noSongInfo);
				albumNameText.setText("");
				progressBarTrack.setMax(0);
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
		connected = ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.isConnected();
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
	public void onPause() {
		super.onPause();
		myLogger.log(Level.INFO, "onPause");
	}

	@Override
	public void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getActivity().getApplicationContext();
		app.oMPDAsyncHelper.removeStatusChangeListener(this);
		app.oMPDAsyncHelper.removeTrackPositionListener(this);
		app.unsetActivity(this);
		myLogger.log(Level.INFO, "onStop");
	}

	@Override
	public void onDestroy() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		settings.unregisterOnSharedPreferenceChangeListener(this);
		myLogger.log(Level.INFO, "onDestroy");
		super.onDestroy();
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

	public void onCoverDownloaded(Bitmap cover) {
		coverSwitcherProgress.setVisibility(ProgressBar.INVISIBLE);
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("enableLastFM")) {
			enableLastFM = sharedPreferences.getBoolean("enableLastFM", true);
		}
	}

}
