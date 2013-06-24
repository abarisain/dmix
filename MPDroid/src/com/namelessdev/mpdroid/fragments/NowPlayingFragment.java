package com.namelessdev.mpdroid.fragments;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.internal.widget.IcsListPopupWindow;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.StreamingService;
import com.namelessdev.mpdroid.adapters.PopupMenuAdapter;
import com.namelessdev.mpdroid.adapters.PopupMenuItem;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDConnectionHandler;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;

public class NowPlayingFragment extends SherlockFragment implements StatusChangeListener, TrackPositionListener,
		OnSharedPreferenceChangeListener, OnItemClickListener {
	
	public static final String PREFS_NAME = "mpdroid.properties";
	
	private static final int POPUP_ARTIST = 0;
	private static final int POPUP_ALBUM = 1;
	private static final int POPUP_FOLDER = 2;
	private static final int POPUP_STREAM = 3;
	private static final int POPUP_SHARE = 4;

	private TextView artistNameText;
	private TextView songNameText;
	private TextView albumNameText;
	private ImageButton shuffleButton=null;
	private ImageButton repeatButton=null;
	private ImageButton stopButton = null;
	private boolean shuffleCurrent=false;
	private boolean repeatCurrent=false;

	public static final int ALBUMS = 4;

	public static final int FILES = 3;

	private SeekBar progressBarVolume = null;
	private SeekBar progressBarTrack = null;

	private TextView trackTime = null;
	private TextView trackTotalTime = null;

	private CoverAsyncHelper oCoverAsyncHelper = null;
	long lastSongTime = 0;
	long lastElapsedTime = 0;

	private AlbumCoverDownloadListener coverArtListener;
	private ImageView coverArt;
	private ProgressBar coverArtProgress;

	private IcsListPopupWindow popupMenu;

	public static final int VOLUME_STEP = 5;

	private static final int ANIMATION_DURATION_MSEC = 1000;

	private ButtonEventHandler buttonEventHandler;

	@SuppressWarnings("unused")
	private boolean streamingMode;
	private boolean connected;

	private Music currentSong = null;

	private Timer volTimer = new Timer();
	private TimerTask volTimerTask = null;
	private Handler handler;

	private Timer posTimer = null;
	private TimerTask posTimerTask = null;

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
		handler = new Handler();
		setHasOptionsMenu(false);
		getActivity().setTitle(getResources().getString(R.string.nowPlaying));
		getActivity().registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
	}

	@Override
	public void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getActivity().getApplication();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addTrackPositionListener(this);
		app.setActivity(this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDestroyView() {
		coverArt.setImageResource(R.drawable.no_cover_art);
		coverArtListener.freeCoverDrawable();
		super.onDestroyView();
	}

	@Override
	public void onResume() {
		super.onResume();
		// Annoyingly this seems to be run when the app starts the first time to.
		// Just to make sure that we do actually get an update.
		try {
			updateTrackInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
		updateStatus(null);

		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					final int volume = app.oMPDAsyncHelper.oMPD.getStatus().getVolume();
					handler.post(new Runnable() {
						@Override
						public void run() {
							progressBarVolume.setProgress(volume);
						}
					});
				} catch (MPDServerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		View view = inflater.inflate(app.isTabletUiEnabled() ? R.layout.main_fragment_tablet : R.layout.main_fragment, container, false);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		settings.registerOnSharedPreferenceChangeListener(this);

		streamingMode = app.getApplicationState().streamingMode;
		connected = app.oMPDAsyncHelper.oMPD.isConnected();
		artistNameText = (TextView) view.findViewById(R.id.artistName);
		albumNameText = (TextView) view.findViewById(R.id.albumName);
		songNameText = (TextView) view.findViewById(R.id.songName);
		artistNameText.setSelected(true);
		albumNameText.setSelected(true);
		songNameText.setSelected(true);

		shuffleButton = (ImageButton) view.findViewById(R.id.shuffle);
		repeatButton = (ImageButton) view.findViewById(R.id.repeat);

		progressBarVolume = (SeekBar) view.findViewById(R.id.progress_volume);
		progressBarTrack = (SeekBar) view.findViewById(R.id.progress_track);

		trackTime = (TextView) view.findViewById(R.id.trackTime);
		trackTotalTime = (TextView) view.findViewById(R.id.trackTotalTime);

		Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
		fadeIn.setDuration(ANIMATION_DURATION_MSEC);
		Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
		fadeOut.setDuration(ANIMATION_DURATION_MSEC);

		coverArt = (ImageView) view.findViewById(R.id.albumCover);
		coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);

		oCoverAsyncHelper = new CoverAsyncHelper(app, settings);
		// Scale cover images down to screen width
		oCoverAsyncHelper.setCoverMaxSizeFromScreen(getActivity());
		oCoverAsyncHelper.setCachedCoverMaxSize(coverArt.getWidth());

		coverArtListener = new AlbumCoverDownloadListener(getActivity(), coverArt, coverArtProgress, app.isLightThemeSelected());
		oCoverAsyncHelper.addCoverDownloadListener(coverArtListener);

		buttonEventHandler = new ButtonEventHandler();
		ImageButton button = (ImageButton) view.findViewById(R.id.next);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.prev);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.playpause);
		button.setOnClickListener(buttonEventHandler);
		button.setOnLongClickListener(buttonEventHandler);
		
		stopButton = (ImageButton) view.findViewById(R.id.stop);
		stopButton.setOnClickListener(buttonEventHandler);
		stopButton.setOnLongClickListener(buttonEventHandler);
		applyStopButtonVisibility(settings);

		if (null!=shuffleButton) {
			shuffleButton.setOnClickListener(buttonEventHandler);
		}
		if (null!=repeatButton) {
			repeatButton.setOnClickListener(buttonEventHandler);
		}

		final View songInfo = view.findViewById(R.id.songInfo);
		if(songInfo != null) {
			songInfo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentSong == null)
						return;
					popupMenu = new IcsListPopupWindow(getActivity());
					popupMenu.setModal(true);
					popupMenu.setOnItemClickListener(NowPlayingFragment.this);
					PopupMenuItem items[];
					if (currentSong.isStream()) {
						items = new PopupMenuItem[2];
						items[0] = new PopupMenuItem(POPUP_STREAM, R.string.goToStream);
						items[1] = new PopupMenuItem(POPUP_SHARE, R.string.share);
					} else {
						items = new PopupMenuItem[4];
						items[0] = new PopupMenuItem(POPUP_ARTIST, R.string.goToAlbum);
						items[1] = new PopupMenuItem(POPUP_ALBUM, R.string.goToArtist);
						items[2] = new PopupMenuItem(POPUP_FOLDER, R.string.goToFolder);
						items[3] = new PopupMenuItem(POPUP_SHARE, R.string.share);
					}
					popupMenu.setAdapter(new PopupMenuAdapter(getActivity(),
							Build.VERSION.SDK_INT >= 14 ? android.R.layout.simple_spinner_dropdown_item
									: R.layout.sherlock_spinner_dropdown_item, items));
					popupMenu.setContentWidth((int) (v.getWidth() - v.getWidth() * 0.05));
					popupMenu.setAnchorView(v);
					popupMenu.show();
				}
			});
		}

		progressBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				volTimerTask = new TimerTask() {
					public void run() {
						final MPDApplication app = (MPDApplication) getActivity().getApplication();
						if (lastSentVol != progress.getProgress()) {
							lastSentVol = progress.getProgress();
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										app.oMPDAsyncHelper.oMPD.setVolume(lastSentVol);
									} catch (MPDServerException e) {
										e.printStackTrace();
									}
								}
							}).start();
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
				volTimerTask.run();
			}
		});

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
			case R.id.stop:
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							mpd.stop();
						} catch (MPDServerException e) {
							Log.w(MPDApplication.TAG, e.getMessage());
						}
					}
				}).start();
				if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
					i = new Intent(app, StreamingService.class);
					i.setAction("com.namelessdev.mpdroid.DIE");
					getActivity().startService(i);
					((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode = false;
				}
				break;
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
            case R.id.shuffle:
                try {
					mpd.setRandom(!mpd.getStatus().isRandom());
				} catch (MPDServerException e) {
				}
                break;
            case R.id.repeat:
                try {
					mpd.setRepeat(!mpd.getStatus().isRepeat());
				} catch (MPDServerException e) {
				}
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
				String path = null;
				String filename = null;
				int songMax = 0;
				boolean noSong=actSong == null || status.getPlaylistLength() == 0;
				if (noSong) {
					currentSong = null;
					title = getResources().getString(R.string.noSongInfo);
				} else if (actSong.isStream()){
					currentSong = actSong;
					Log.d("MPDroid", "Playing a stream");
					if (actSong.haveTitle()) {
						title = actSong.getTitle();
					}
					artist = actSong.getArtist();
					album = actSong.getName();
					path = actSong.getPath();
					filename = actSong.getFilename();
					songMax = (int) actSong.getTime();
				} else {
					currentSong = actSong;
					Log.d("MPDroid", "We did find an artist");
					artist = actSong.getArtist();
					title = actSong.getTitle();
					album = actSong.getAlbum();
					path = actSong.getPath();
					filename = actSong.getFilename();
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
				if (noSong || actSong.isStream()) {
					lastArtist = artist;
					lastAlbum = album;
					trackTime.setText(timeToString(0));
					trackTotalTime.setText(timeToString(0));
					coverArtListener.onCoverNotFound();
				} else if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) {
					// coverSwitcher.setVisibility(ImageSwitcher.INVISIBLE);
					coverArtProgress.setVisibility(ProgressBar.VISIBLE);
					oCoverAsyncHelper.downloadCover(artist, album, path, filename);
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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(CoverAsyncHelper.PREFERENCE_CACHE) || key.equals(CoverAsyncHelper.PREFERENCE_LASTFM)
				|| key.equals(CoverAsyncHelper.PREFERENCE_LOCALSERVER)) {
			oCoverAsyncHelper.setCoverRetrieversFromPreferences();
		} else if(key.equals("enableStopButton")) {
			applyStopButtonVisibility(sharedPreferences);
		}
	}

	private void applyStopButtonVisibility(SharedPreferences sharedPreferences) {
		if (stopButton == null) {
			return;
		}
		stopButton.setVisibility(sharedPreferences.getBoolean("enableStopButton", false) ? View.VISIBLE : View.GONE);
	}

	@Override
	public void trackPositionChanged(MPDStatus status) {
		startPosTimer(status.getElapsedTime());
	}

	@Override
	public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
		progressBarVolume.setProgress(mpdStatus.getVolume());
	}

	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
		// If the playlist changed but not the song position in the playlist
		// We end up being desynced. Update the current song.
		try {
			updateTrackInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateStatus(MPDStatus status) {
		if (getActivity() == null)
			return;
		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		if (status == null) {
			status = app.getApplicationState().currentMpdStatus;
			if (status == null)
				return;
		} else {
			app.getApplicationState().currentMpdStatus = status;
		}
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
        setShuffleButton(status.isRandom());
        setRepeatButton(status.isRepeat());
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
		setRepeatButton(repeating);
	}

	@Override
	public void randomChanged(boolean random) {
		setShuffleButton(random);
	}

	@Override
	public void connectionStateChanged(boolean connected, boolean connectionLost) {
		checkConnected();
	}

	@Override
	public void libraryStateChanged(boolean updating) {
		// TODO Auto-generated method stub
		
	}

	private void setShuffleButton(boolean on) {
		if (null!=shuffleButton && shuffleCurrent!=on) {
			int[] attrs = new int[] { on ? R.attr.shuffleEnabled : R.attr.shuffleDisabled };
			final TypedArray ta = getActivity().obtainStyledAttributes(attrs);
			final Drawable drawableFromTheme = ta.getDrawable(0);
			shuffleButton.setImageDrawable(drawableFromTheme);
			shuffleButton.invalidate();
			shuffleCurrent=on;
		}
	}

	private void setRepeatButton(boolean on) {
		if (null!=repeatButton && repeatCurrent!=on) {
			int[] attrs = new int[] { on ? R.attr.repeatEnabled : R.attr.repeatDisabled };
			final TypedArray ta = getActivity().obtainStyledAttributes(attrs);
			final Drawable drawableFromTheme = ta.getDrawable(0);
			repeatButton.setImageDrawable(drawableFromTheme);
			repeatButton.invalidate();
			repeatCurrent=on;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adpaterView, View view, int position, long id) {
		popupMenu.dismiss();
		if(currentSong == null)
			return;
		final int action = ((PopupMenuItem) adpaterView.getAdapter().getItem(position)).actionId;
		Intent intent;
		switch(action) {
			case POPUP_ALBUM:
				intent = new Intent(getActivity(), SimpleLibraryActivity.class);
				intent.putExtra("artist", new Artist(currentSong.getArtist(), 0));
				startActivityForResult(intent, -1);
				break;
			case POPUP_ARTIST:
				intent = new Intent(getActivity(), SimpleLibraryActivity.class);
				intent.putExtra("artist", new Artist(currentSong.getArtist(), 0));
				intent.putExtra("album", new Album(currentSong.getAlbum()));
				startActivityForResult(intent, -1);
				break;
			case POPUP_FOLDER:
				final String path = currentSong.getFullpath();
				if (path == null) {
					break;
				}
				intent = new Intent(getActivity(), SimpleLibraryActivity.class);
				intent.putExtra("folder", currentSong.getParent());
				startActivityForResult(intent, -1);
				break;
			case POPUP_STREAM:
				intent = new Intent(getActivity(), SimpleLibraryActivity.class);
				intent.putExtra("steams", true);
				startActivityForResult(intent, -1);
				break;
			case POPUP_SHARE:
				String shareString = getString(R.string.sharePrefix);
				shareString += " " + currentSong.getTitle();
				if (!currentSong.isStream()) {
					shareString += " - " + currentSong.getArtist();
				}
				final Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, shareString);
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
		}
	}
}
