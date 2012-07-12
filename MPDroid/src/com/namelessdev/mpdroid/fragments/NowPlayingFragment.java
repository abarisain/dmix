package com.namelessdev.mpdroid.fragments;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.StreamingService;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper.CoverDownloadListener;
import com.namelessdev.mpdroid.helpers.MPDConnectionHandler;

public class NowPlayingFragment extends SherlockFragment implements StatusChangeListener, TrackPositionListener, CoverDownloadListener,
		OnSharedPreferenceChangeListener {
	
	public static final String COVER_BASE_URL = "http://%s/music/";

	public static final String PREFS_NAME = "mpdroid.properties";

	private TextView artistNameText;

	private TextView songNameText;

	private TextView albumNameText;

	public static final int ALBUMS = 4;

	public static final int FILES = 3;

	private SeekBar progressBarTrack = null;

	private TextView trackTime = null;
	private TextView trackTotalTime = null;

	private CoverAsyncHelper oCoverAsyncHelper = null;
	long lastSongTime = 0;
	long lastElapsedTime = 0;

	private ImageView coverArt;

	private ProgressBar coverArtProgress;

	public static final int VOLUME_STEP = 5;

	private static final int ANIMATION_DURATION_MSEC = 1000;

	private ButtonEventHandler buttonEventHandler;

	@SuppressWarnings("unused")
	private boolean streamingMode;
	private boolean connected;

	private Timer volTimer = new Timer();
	private TimerTask volTimerTask = null;
	private Handler handler;

	private Timer posTimer = null;
	private TimerTask posTimerTask = null;

	private boolean enableLastFM;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		default:
			break;
		}

	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
		// myLogger.log(Level.INFO, "onCreate");
		handler = new Handler();
		setHasOptionsMenu(false);
		getActivity().setTitle(getResources().getString(R.string.nowPlaying));

		// registerReceiver(, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION) );
		getActivity().registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
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

		/*final MPDApplication app = (MPDApplication) getActivity().getApplication();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					final int volume = app.oMPDAsyncHelper.oMPD.getStatus().getVolume();
					handler.post(new Runnable() {
						@Override
						public void run() {
							//progressBarVolume.setProgress(volume);
						}
					});
				} catch (MPDServerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();*/
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_fragment, container, false);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		settings.registerOnSharedPreferenceChangeListener(this);

		enableLastFM = settings.getBoolean("enableLastFM", true);

		streamingMode = ((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode;
		connected = ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.isConnected();
		artistNameText = (TextView) view.findViewById(R.id.artistName);
		albumNameText = (TextView) view.findViewById(R.id.albumName);
		songNameText = (TextView) view.findViewById(R.id.songName);
		artistNameText.setSelected(true);
		albumNameText.setSelected(true);
		songNameText.setSelected(true);

		progressBarTrack = (SeekBar) view.findViewById(R.id.progress_track);

		trackTime = (TextView) view.findViewById(R.id.trackTime);
		trackTotalTime = (TextView) view.findViewById(R.id.trackTotalTime);

		Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
		fadeIn.setDuration(ANIMATION_DURATION_MSEC);
		Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
		fadeOut.setDuration(ANIMATION_DURATION_MSEC);

		coverArt = (ImageView) view.findViewById(R.id.albumCover);

		coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
		coverArtProgress.setIndeterminate(true);
		coverArtProgress.setVisibility(ProgressBar.INVISIBLE);

		oCoverAsyncHelper = new CoverAsyncHelper();
		oCoverAsyncHelper.addCoverDownloadListener(this);
		buttonEventHandler = new ButtonEventHandler();
		ImageButton button = (ImageButton) view.findViewById(R.id.next);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.prev);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.playpause);
		button.setOnClickListener(buttonEventHandler);
		button.setOnLongClickListener(buttonEventHandler);
		
		final View songInfo = view.findViewById(R.id.songInfo);
		if(songInfo != null) {
			songInfo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
				}
			});
		}

		progressBarTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onStopTrackingTouch(final SeekBar seekBar) {

				final MPDApplication app = (MPDApplication) getActivity().getApplication();
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.seek(seekBar.getProgress());
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		});

		songNameText.setText(getResources().getString(R.string.notConnected));
		Log.i(MPDApplication.TAG, "Initialization succeeded");

		return view;
	}

	private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

		public void onClick(View v) {
			final MPDApplication app = (MPDApplication) getActivity().getApplication();
			final MPD mpd = app.oMPDAsyncHelper.oMPD;
			Intent i = null;

			switch (v.getId()) {
			case R.id.next:
				new Thread(new Runnable() {					
					@Override
					public void run() {
						try {
							mpd.next();
						} catch (MPDServerException e) {
							Log.w(MPDApplication.TAG, e.getMessage());
						}
					}
				}).start();
				if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
					i = new Intent(app, StreamingService.class);
					i.setAction("com.namelessdev.mpdroid.RESET_STREAMING");
					getActivity().startService(i);
				}
				break;
			case R.id.prev:
				new Thread(new Runnable() {					
					@Override
					public void run() {
						try {
							mpd.previous();
						} catch (MPDServerException e) {
							Log.w(MPDApplication.TAG, e.getMessage());
						}
					}
				}).start();
						
				if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
					i = new Intent(app, StreamingService.class);
					i.setAction("com.namelessdev.mpdroid.RESET_STREAMING");
					getActivity().startService(i);
				}
				break;
			case R.id.playpause:
				/**
				 * If playing or paused, just toggle state, otherwise start playing.
				 * 
				 * @author slubman
				 */
				new Thread(new Runnable() {					
					@Override
					public void run() {
						String state;
						try {
							state = mpd.getStatus().getState();
							if (state.equals(MPDStatus.MPD_STATE_PLAYING) || state.equals(MPDStatus.MPD_STATE_PAUSED)) {
								mpd.pause();
							} else {
								mpd.play();
							}
						} catch (MPDServerException e) {
							Log.w(MPDApplication.TAG, e.getMessage());
						}
					}
				}).start();
				break;

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
					if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
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

	private class PosTimerTask extends TimerTask {
		Date date = new Date();
		long start=0;
		long ellapsed=0;
		public PosTimerTask(long start) {
			this.start=start;
		}
		@Override
		public void run() {
			Date now=new Date();
			ellapsed=start+((now.getTime()-date.getTime())/1000);
			progressBarTrack.setProgress((int)ellapsed);
			handler.post(new Runnable() {
				@Override
				public void run() {
			    	 trackTime.setText(timeToString(ellapsed));
			    	 trackTotalTime.setText(timeToString(lastSongTime));
			    }
			});
			lastElapsedTime = ellapsed;
		}
	}

	private void startPosTimer(long start) {
		stopPosTimer();
		posTimer = new Timer();
		posTimerTask = new PosTimerTask(start);
		posTimer.scheduleAtFixedRate(posTimerTask, 0, 1000);
	}

	private void stopPosTimer() {
		if (null!=posTimer) {
			posTimer.cancel();
			posTimer=null;
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
					int songPos = params[0].getSongPos();
					if (songPos >= 0) {
						MPDApplication app = (MPDApplication) getActivity().getApplication();
						actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
						status = params[0];
						return true;
					}
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result != null && result) {
				String artist = null;
				String title = null;
				String album = null;
				int songMax = 0;
				boolean noSong=actSong == null || status.getPlaylistLength() == 0;
				if (noSong) {
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
				updateStatus(status);
				if (noSong) {
					lastArtist = artist;
					lastAlbum = album;
					trackTime.setText(timeToString(0));
					trackTotalTime.setText(timeToString(0));
					onCoverNotFound();
				} else if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) {
					// coverSwitcher.setVisibility(ImageSwitcher.INVISIBLE);
					coverArtProgress.setVisibility(ProgressBar.VISIBLE);
					if (enableLastFM) {
						oCoverAsyncHelper.downloadCover(artist, album);
					} else {
						// Try to find the cover from apache (vortexbox)
						// TODO : Make it configurable ...
						oCoverAsyncHelper.setUrlOverride(String.format(COVER_BASE_URL + "%s/cover.jpg", ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.getConnectionSettings().sServer, actSong.getPath().replace(" ", "%20")));
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

	public void checkConnected() {
		connected = ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.isConnected();
		if (connected) {
			songNameText.setText(getResources().getString(R.string.noSongInfo));
		} else {
			songNameText.setText(getResources().getString(R.string.notConnected));
		}
		return;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getActivity().getApplicationContext();
		app.oMPDAsyncHelper.removeStatusChangeListener(this);
		app.oMPDAsyncHelper.removeTrackPositionListener(this);
		stopPosTimer();
		app.unsetActivity(this);
	}

	@Override
	public void onDestroy() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		settings.unregisterOnSharedPreferenceChangeListener(this);
		getActivity().unregisterReceiver(MPDConnectionHandler.getInstance());
		super.onDestroy();
	}

	public SeekBar getProgressBarTrack() {
		return progressBarTrack;
	}

	private static String timeToString(long seconds) {
		if (seconds<0) {
			seconds=0;
		}

		long hours = seconds / 3600;
		seconds -= 3600 * hours;
		long minutes = seconds / 60;
		seconds -= minutes * 60;
		if (hours == 0) {
			return String.format("%02d:%02d", minutes, seconds);
		} else {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		}
	}

	public void onCoverDownloaded(Bitmap cover) {
		coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
		DisplayMetrics metrics = new DisplayMetrics();
		try {
			getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
			if (cover != null) {
				cover.setDensity((int) metrics.density);
				BitmapDrawable myCover = new BitmapDrawable(getResources(), cover);
				coverArt.setImageDrawable(myCover);
			} else {
				// Should not be happening, but happened.
				onCoverNotFound();
			}
		} catch (Exception e) {
			//Probably rotated, ignore
			e.printStackTrace();
		}
	}

	public void onCoverNotFound() {
		coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
		coverArt.setImageResource(R.drawable.no_cover_art);
		// coverSwitcher.setVisibility(ImageSwitcher.VISIBLE);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("enableLastFM")) {
			enableLastFM = sharedPreferences.getBoolean("enableLastFM", true);
		}
	}

	@Override
	public void trackPositionChanged(MPDStatus status) {
		startPosTimer(status.getElapsedTime());
		/*lastElapsedTime = status.getElapsedTime();
		lastSongTime = status.getTotalTime();
		trackTime.setText(timeToString(lastElapsedTime));
   	 	trackTotalTime.setText(timeToString(lastSongTime));
		progressBarTrack.setProgress((int) status.getElapsedTime());*/
	}

	@Override
	public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
		//progressBarVolume.setProgress(mpdStatus.getVolume());
	}

	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
		// Can someone explain why this is nessesary?
		// Maybe the song gets changed before the playlist?
		// Makes little sense tho.
		try {
			updateTrackInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateStatus(MPDStatus status) {
		lastElapsedTime = status.getElapsedTime();
		lastSongTime = status.getTotalTime();
		trackTime.setText(timeToString(lastElapsedTime));
   	 	trackTotalTime.setText(timeToString(lastSongTime));
		progressBarTrack.setProgress((int) status.getElapsedTime());
		if (status.getState() != null) {

			if (status.getState().equals(MPDStatus.MPD_STATE_PLAYING)) {
				startPosTimer(status.getElapsedTime());
				ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
				button.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_pause));
			} else {
				stopPosTimer();
				ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
				button.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_play));
			}
		}
	}

	@Override
	public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
		updateTrackInfo(mpdStatus);
	}

	@Override
	public void stateChanged(MPDStatus mpdStatus, String oldState) {
		updateStatus(mpdStatus);
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
		checkConnected();
	}

	@Override
	public void libraryStateChanged(boolean updating) {
		// TODO Auto-generated method stub
		
	}

}
