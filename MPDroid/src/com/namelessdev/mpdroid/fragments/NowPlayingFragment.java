/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.fragments;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.helpers.MPDConnectionHandler;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;

import org.a0z.mpd.AlbumInfo;
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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.PopupMenuCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.text.TextUtils.isEmpty;
import static com.namelessdev.mpdroid.tools.StringUtils.getExtension;

public class NowPlayingFragment extends Fragment implements StatusChangeListener,
        TrackPositionListener,
        OnSharedPreferenceChangeListener, OnMenuItemClickListener {

    private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

        public void onClick(View v) {
            final MPD mpd = app.oMPDAsyncHelper.oMPD;
            Intent i;

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
                    break;
                case R.id.playpause:
                    /**
                     * If playing or paused, just toggle state, otherwise start
                     * playing.
                     * 
                     * @author slubman
                     */
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String state;
                            try {
                                state = mpd.getStatus().getState();
                                if (state.equals(MPDStatus.MPD_STATE_PLAYING)
                                        || state.equals(MPDStatus.MPD_STATE_PAUSED)) {
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
            MPD mpd = app.oMPDAsyncHelper.oMPD;
            try {
                switch (v.getId()) {
                    case R.id.playpause:
                        // Implements the ability to stop playing (may be useful
                        // for streams)
                        mpd.stop();
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

    /**
     * This class runs a timer to keep the time elapsed since last track elapsed time updated for
     * the purpose of keeping the track progress up to date without continual server polling.
     */
    private class PosTimerTask extends TimerTask {

        private final long timerStartTime;

        private long startTrackTime = 0L;

        private long totalTrackTime = 0L;

        private long elapsedTime = 0L;

        private PosTimerTask(final long start, final long total) {
            super();
            this.startTrackTime = start;
            this.totalTrackTime = total;
            this.timerStartTime = new Date().getTime();
        }

        @Override
        public void run() {
            final long elapsedSinceTimerStart = new Date().getTime() - timerStartTime;

            elapsedTime = startTrackTime + elapsedSinceTimerStart / THOUSAND_MILLISECONDS;

            updateTrackProgress(elapsedTime, totalTrackTime);
        }
    }

    public class updateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Boolean> {
        Music actSong = null;
        MPDStatus status = null;

        @Override
        protected Boolean doInBackground(MPDStatus... params) {
            Boolean result = false;

            if (params == null) {
                try {
                    // A recursive call doesn't seem that bad here.
                    result = doInBackground(app.oMPDAsyncHelper.oMPD.getStatus(true));
                } catch (MPDServerException e) {
                    Log.d(MPDApplication.TAG, "Failed to populate params in the background.", e);
                }
            } else if (params[0] != null) {
                String state = params[0].getState();
                if (state != null) {
                    int songPos = params[0].getSongPos();
                    if (songPos >= 0) {
                        actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
                        status = params[0];
                        result = true;
                    }
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result != null && result && activity != null) {
                String albumartist = null;
                String artist = null;
                String title;
                String album = null;
                String date = null;

                boolean noSong = actSong == null || status.getPlaylistLength() == 0;
                if (noSong) {
                    currentSong = null;
                    title = activity.getResources().getString(R.string.noSongInfo);
                } else if (actSong.isStream()) {
                    currentSong = actSong;
                    Log.d("MPDroid", "Playing a stream");
                    if (actSong.haveTitle()) {
                        title = actSong.getTitle();
                        album = actSong.getName();
                    } else {
                        title = actSong.getName();
                        album = "";
                    }
                    artist = actSong.getArtist();
                    date = "";

                } else {
                    currentSong = actSong;
                    if (DEBUG)
                        Log.d("MPDroid", "We did find an artist");
                    albumartist = actSong.getAlbumArtist();
                    artist = actSong.getArtist();
                    title = actSong.getTitle();
                    album = actSong.getAlbum();
                    date = Long.toString(actSong.getDate());
                }

                albumartist = albumartist == null ? "" : albumartist;
                artist = artist == null ? "" : artist;
                title = title == null ? "" : title;
                album = album == null ? "" : album;
                date = date != null && date.length() > 1 && !date.startsWith("-") ? " - " + date
                        : "";
                if (!showAlbumArtist || "".equals(albumartist) ||
                        artist.toLowerCase().contains(albumartist.toLowerCase()))
                    artistNameText.setText(artist);
                else if ("".equals(artist))
                    artistNameText.setText(albumartist);
                else
                    artistNameText.setText(albumartist + " / " + artist);
                songNameText.setText(title);
                albumNameText.setText(album);
                yearNameText.setText(date);

                updateStatus(status);
                if (noSong || actSong.isStream()) {
                    lastArtist = artist;
                    lastAlbum = album;
                    trackTime.setText(Music.timeToString(0L));
                    trackTotalTime.setText(Music.timeToString(0L));
                    downloadCover(new AlbumInfo(artist, album));
                } else if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) {
                    // coverSwitcher.setVisibility(ImageSwitcher.INVISIBLE);
                    int noCoverDrawable = lightTheme ? R.drawable.no_cover_art_light_big
                            : R.drawable.no_cover_art_big;
                    coverArt.setImageResource(noCoverDrawable);
                    downloadCover(actSong.getAlbumInfo());
                    lastArtist = artist;
                    lastAlbum = album;
                }
            } else {
                artistNameText.setText("");
                songNameText.setText(R.string.noSongInfo);
                albumNameText.setText("");
                yearNameText.setText("");
            }
        }
    }

    private static final String TAG = "com.namelessdev.mpdroid.NowPlayingFragment";

    private static final long THOUSAND_MILLISECONDS = 1000L;
    private static final boolean DEBUG = false;
    private static final int POPUP_ARTIST = 0;
    private static final int POPUP_ALBUMARTIST = 1;
    private static final int POPUP_ALBUM = 2;
    private static final int POPUP_FOLDER = 3;
    private static final int POPUP_STREAM = 4;
    private static final int POPUP_SHARE = 5;

    private static final int POPUP_CURRENT = 6;
    private static final int POPUP_COVER_BLACKLIST = 7;
    private static final int POPUP_COVER_SELECTIVE_CLEAN = 8;
    private TextView artistNameText;
    private boolean showAlbumArtist;
    private TextView songNameText;
    private TextView albumNameText;
    private TextView audioNameText = null;
    private TextView yearNameText;
    private ImageButton shuffleButton = null;
    private ImageButton repeatButton = null;
    private ImageButton stopButton = null;
    private boolean isAudioNameTextEnabled = false;
    private boolean shuffleCurrent = false;
    private boolean repeatCurrent = false;
    private View songInfo = null;

    private PopupMenu popupMenu = null;
    private View.OnTouchListener popupMenuTouchListener = null;

    private PopupMenu popupMenuStream = null;
    private View.OnTouchListener popupMenuStreamTouchListener = null;

    private ImageView volumeIcon = null;
    public static final int ALBUMS = 4;

    public static final int FILES = 3;


    private SeekBar seekBarTrack = null;

    private SeekBar seekBarVolume = null;

    private TextView trackTime = null;
    private TextView trackTotalTime = null;

    private CoverAsyncHelper oCoverAsyncHelper = null;

    private AlbumCoverDownloadListener coverArtListener;

    private ImageView coverArt;

    private ProgressBar coverArtProgress;

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

    private MPDApplication app;

    private FragmentActivity activity;

    private PopupMenu coverMenu;

    private boolean lightTheme;

    private String lastArtist = "";

    private String lastAlbum = "";

    private void applyViewVisibility(SharedPreferences sharedPreferences, View view, String property) {
        if (view == null) {
            return;
        }
        view.setVisibility(sharedPreferences.getBoolean(property, false) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        if(connected) {
            forceStatusUpdate();
            songNameText.setText(activity.getResources().getString(R.string.noSongInfo));
        } else {
            songNameText.setText(activity.getResources().getString(R.string.notConnected));
        }
    }

    private void downloadCover(AlbumInfo albumInfo) {
        oCoverAsyncHelper.downloadCover(albumInfo, true);
    }

    /**
     * This is a very important block. This should be used exclusively in onResume() and/or in
     * connectedStateChanged(). These should be the only two places in the non-library code that
     * will require a forced status update, as our idle status monitor will otherwise take care of
     * any other updates.
     *
     * This SHOULD NOT be used elsewhere unless you know exactly what you're doing.
     */
    private void forceStatusUpdate() {
        MPDStatus mpdStatus = null;
        try {
            mpdStatus = app.oMPDAsyncHelper.oMPD.getStatus(true);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to seed the status.", e);
        }

        updateTrackInfo(mpdStatus);
    }

    private PlaylistFragment getPlaylistFragment() {
        PlaylistFragment playlistFragment;
        playlistFragment = (PlaylistFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.playlist_fragment);
        return playlistFragment;
    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            default:
                break;
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;

    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        app = (MPDApplication) activity.getApplication();
        lightTheme = app.isLightThemeSelected();
        handler = new Handler();
        setHasOptionsMenu(false);
        activity.setTitle(activity.getResources().getString(R.string.nowPlaying));
        activity.registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(
                WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(app.isTabletUiEnabled() ? R.layout.main_fragment_tablet
                : R.layout.main_fragment, container, false);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        settings.registerOnSharedPreferenceChangeListener(this);

        streamingMode = app.getApplicationState().streamingMode;
        connected = app.oMPDAsyncHelper.oMPD.isConnected();
        artistNameText = (TextView) view.findViewById(R.id.artistName);
        albumNameText = (TextView) view.findViewById(R.id.albumName);
        songNameText = (TextView) view.findViewById(R.id.songName);
        audioNameText = (TextView) view.findViewById(R.id.audioName);
        yearNameText = (TextView) view.findViewById(R.id.yearName);
        applyViewVisibility(settings, yearNameText, "enableAlbumYearText");
        artistNameText.setSelected(true);
        albumNameText.setSelected(true);
        songNameText.setSelected(true);
        audioNameText.setSelected(true);
        yearNameText.setSelected(true);
        shuffleButton = (ImageButton) view.findViewById(R.id.shuffle);
        repeatButton = (ImageButton) view.findViewById(R.id.repeat);

        seekBarVolume = (SeekBar) view.findViewById(R.id.progress_volume);
        seekBarTrack = (SeekBar) view.findViewById(R.id.progress_track);
        volumeIcon = (ImageView) view.findViewById(R.id.volume_icon);

        trackTime = (TextView) view.findViewById(R.id.trackTime);
        trackTotalTime = (TextView) view.findViewById(R.id.trackTotalTime);

        Animation fadeIn = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
        fadeIn.setDuration(ANIMATION_DURATION_MSEC);
        Animation fadeOut = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
        fadeOut.setDuration(ANIMATION_DURATION_MSEC);

        coverArt = (ImageView) view.findViewById(R.id.albumCover);
        coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);

        coverArt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollToNowPlaying();
            }
        });

        coverMenu = new PopupMenu(activity, coverArt);
        coverMenu.getMenu().add(Menu.NONE, POPUP_COVER_BLACKLIST, Menu.NONE, R.string.otherCover);
        coverMenu.getMenu().add(Menu.NONE, POPUP_COVER_SELECTIVE_CLEAN, Menu.NONE,
                R.string.resetCover);
        coverMenu.setOnMenuItemClickListener(NowPlayingFragment.this);
        coverArt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (currentSong != null) {
                    coverMenu.show();
                }
                return true;
            }
        });

        oCoverAsyncHelper = new CoverAsyncHelper(app, settings);
        // Scale cover images down to screen width
        oCoverAsyncHelper.setCoverMaxSizeFromScreen(activity);
        oCoverAsyncHelper.setCachedCoverMaxSize(coverArt.getWidth());

        coverArtListener = new AlbumCoverDownloadListener(activity, coverArt, coverArtProgress,
                app.isLightThemeSelected(), true);
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
        applyViewVisibility(settings, stopButton, "enableStopButton");

        if (null != shuffleButton) {
            shuffleButton.setOnClickListener(buttonEventHandler);
        }
        if (null != repeatButton) {
            repeatButton.setOnClickListener(buttonEventHandler);
        }

        songInfo = view.findViewById(R.id.songInfo);
        if (songInfo != null) {
            popupMenu = new PopupMenu(activity, songInfo);
            popupMenu.getMenu().add(Menu.NONE, POPUP_ALBUM, Menu.NONE, R.string.goToAlbum);
            popupMenu.getMenu().add(Menu.NONE, POPUP_ARTIST, Menu.NONE, R.string.goToArtist);
            popupMenu.getMenu().add(Menu.NONE, POPUP_ALBUMARTIST, Menu.NONE,
                    R.string.goToAlbumArtist);
            popupMenu.getMenu().add(Menu.NONE, POPUP_FOLDER, Menu.NONE, R.string.goToFolder);
            popupMenu.getMenu().add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
            popupMenu.getMenu().add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
            popupMenu.setOnMenuItemClickListener(NowPlayingFragment.this);

            popupMenuStream = new PopupMenu(activity, songInfo);
            popupMenuStream.getMenu().add(Menu.NONE, POPUP_STREAM, Menu.NONE, R.string.goToStream);
            popupMenuStream.getMenu()
                    .add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
            popupMenuStream.getMenu().add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
            popupMenuStream.setOnMenuItemClickListener(NowPlayingFragment.this);

            popupMenuTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenu);
            popupMenuStreamTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenuStream);

            songInfo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentSong == null)
                        return;

                    if (currentSong.isStream()) {
                        popupMenuStream.show();
                    } else {
                        popupMenu.show();
                    }
                }
            });
        }

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                volTimerTask = new TimerTask() {
                    int lastSentVol = -1;

                    SeekBar progress;

                    public void run() {
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

        seekBarTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress,
                    final boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            app.oMPDAsyncHelper.oMPD.seek((long) seekBar.getProgress());
                        } catch (final MPDServerException e) {
                            Log.e(TAG, "Failed to seek using the progress bar.", e);
                        }
                    }
                }).start();
            }
        });

        songNameText.setText(activity.getResources().getString(R.string.notConnected));
        Log.i(MPDApplication.TAG, "Initialization succeeded");

        return view;
    }

    @Override
    public void onDestroy() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        settings.unregisterOnSharedPreferenceChangeListener(this);
        activity.unregisterReceiver(MPDConnectionHandler.getInstance());
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        coverArt.setImageResource(R.drawable.no_cover_art);
        coverArtListener.freeCoverDrawable();
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity = null;

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case POPUP_ARTIST:
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("artist", currentSong.getArtistAsArtist());
                startActivityForResult(intent, -1);
                break;
            case POPUP_ALBUMARTIST:
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("artist", currentSong.getAlbumArtistAsArtist());
                startActivityForResult(intent, -1);
                break;
            case POPUP_ALBUM:
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("album", currentSong.getAlbumAsAlbum());
                startActivityForResult(intent, -1);
                break;
            case POPUP_FOLDER:
                final String path = currentSong.getFullpath();
                final String parent = currentSong.getParent();
                if (path == null || parent == null) {
                    break;
                }
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("folder", parent);
                startActivityForResult(intent, -1);
                break;
            case POPUP_CURRENT:
                scrollToNowPlaying();
                break;
            case POPUP_STREAM:
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("streams", true);
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
                break;

            case POPUP_COVER_BLACKLIST:
                CoverManager.getInstance(app,
                        PreferenceManager.getDefaultSharedPreferences(activity)).markWrongCover(
                        currentSong.getAlbumInfo());
                downloadCover(currentSong.getAlbumInfo());
                updatePlaylistCovers(currentSong.getAlbumInfo());
                break;
            case POPUP_COVER_SELECTIVE_CLEAN:
                CoverManager.getInstance(app,
                        PreferenceManager.getDefaultSharedPreferences(activity)).clear(
                        currentSong.getAlbumInfo());
                downloadCover(currentSong.getAlbumInfo()); // Update the
                                                           // playlist covers
                updatePlaylistCovers(currentSong.getAlbumInfo());
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        showAlbumArtist = settings.getBoolean("showAlbumArtist", true);
        isAudioNameTextEnabled = settings.getBoolean("enableAudioText", false);

        if (app.oMPDAsyncHelper.oMPD.isConnected()) {
            forceStatusUpdate();
        }

        // Update the cover on resume (when you update the current cover from
        // the library activity)
        if (currentSong != null) {
            downloadCover(currentSong.getAlbumInfo());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(CoverManager.PREFERENCE_CACHE) || key.equals(CoverManager.PREFERENCE_LASTFM)
                || key.equals(CoverManager.PREFERENCE_LOCALSERVER)) {
            oCoverAsyncHelper.setCoverRetrieversFromPreferences();
        } else if (key.equals("enableStopButton")) {
            applyViewVisibility(sharedPreferences, stopButton, key);
        } else if (key.equals("enableAlbumYearText")) {
            applyViewVisibility(sharedPreferences, yearNameText, key);
        } else if (key.equals("enableAudioText")) {
            isAudioNameTextEnabled = sharedPreferences.getBoolean(key, false);
            try {
                updateAudioNameText(app.oMPDAsyncHelper.oMPD.getStatus());
            } catch (final MPDServerException e) {
                Log.e(TAG, "Could not get a current status.", e);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.oMPDAsyncHelper.addTrackPositionListener(this);
        app.setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        app.oMPDAsyncHelper.removeTrackPositionListener(this);
        stopPosTimer();
        app.unsetActivity(this);
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /**
         * If the current song is a stream, the metadata can change in place, and that will only
         * change the playlist, not the track, so, update if we detect a stream.
         */
        if (currentSong != null && currentSong.isStream()) {
            updateTrackInfo(mpdStatus);
        }
    }

    @Override
    public void randomChanged(boolean random) {
        setShuffleButton(random);
    }

    @Override
    public void repeatChanged(boolean repeating) {
        setRepeatButton(repeating);
    }

    private void scrollToNowPlaying() {
        PlaylistFragment playlistFragment;
        playlistFragment = getPlaylistFragment();
        if (playlistFragment != null) {
            playlistFragment.scrollToNowPlaying();
        }
    }

    private void setRepeatButton(boolean on) {
        if (null != repeatButton && repeatCurrent != on) {
            int[] attrs = new int[] {
                    on ? R.attr.repeatEnabled : R.attr.repeatDisabled
            };
            final TypedArray ta = activity.obtainStyledAttributes(attrs);
            final Drawable drawableFromTheme = ta.getDrawable(0);
            repeatButton.setImageDrawable(drawableFromTheme);
            repeatButton.invalidate();
            repeatCurrent = on;
        }
    }

    private void setShuffleButton(boolean on) {
        if (null != shuffleButton && shuffleCurrent != on) {
            int[] attrs = new int[] {
                    on ? R.attr.shuffleEnabled : R.attr.shuffleDisabled
            };
            final TypedArray ta = activity.obtainStyledAttributes(attrs);
            final Drawable drawableFromTheme = ta.getDrawable(0);
            shuffleButton.setImageDrawable(drawableFromTheme);
            shuffleButton.invalidate();
            shuffleCurrent = on;
        }
    }

    private void startPosTimer(final long start, final long total) {
        stopPosTimer();
        posTimer = new Timer();
        posTimerTask = new PosTimerTask(start, total);
        posTimer.scheduleAtFixedRate(posTimerTask, 0L, THOUSAND_MILLISECONDS);
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final String oldState) {
        if (activity != null && mpdStatus != null) {
            updateStatus(mpdStatus);
        }
    }

    private void stopPosTimer() {
        if (null != posTimer) {
            posTimer.cancel();
            posTimer = null;
        }
    }

    /**
     * Toggle the track progress bar. Sets it up for when it's necessary, hides it otherwise. This
     * should be called only when the track changes, for position changes, startPosTimer() is
     * sufficient.
     *
     * @param status A current {@code MPDStatus} object.
     */
    private void toggleTrackProgress(final MPDStatus status) {
        final long totalTime = status.getTotalTime();

        if (totalTime == 0) {
            trackTime.setVisibility(View.GONE);
            trackTotalTime.setVisibility(View.GONE);
            seekBarTrack.setVisibility(View.GONE);
        } else {
            final long elapsedTime = status.getElapsedTime();

            if(MPDStatus.MPD_STATE_PLAYING.equals(status.getState())) {
                startPosTimer(elapsedTime, totalTime);
            } else {
                stopPosTimer();
                updateTrackProgress(elapsedTime, totalTime);
            }

            seekBarTrack.setMax((int) totalTime);

            trackTime.setVisibility(View.VISIBLE);
            trackTotalTime.setVisibility(View.VISIBLE);
            seekBarTrack.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This enables or disables the volume, depending on the volume given by the server.
     *
     * @param volume The current volume value.
     */
    private void toggleVolumeBar(final int volume) {
        final int OUTPUT_VOLUME_UNSUPPORTED = -1;

        if (volume == OUTPUT_VOLUME_UNSUPPORTED) {
            seekBarVolume.setEnabled(false);
            seekBarVolume.setVisibility(View.GONE);
            volumeIcon.setVisibility(View.GONE);
        } else {
            seekBarVolume.setEnabled(true);
            seekBarVolume.setVisibility(View.VISIBLE);
            volumeIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        updateTrackInfo(mpdStatus);
    }

    @Override
    public void trackPositionChanged(final MPDStatus status) {
        if (activity != null && status != null) {
            final long totalTime = status.getTotalTime();
            final long elapsedTime = status.getElapsedTime();

            startPosTimer(elapsedTime, totalTime);
        }
    }

    private void updatePlaylistCovers(AlbumInfo albumInfo) {
        PlaylistFragment playlistFragment;
        playlistFragment = getPlaylistFragment();
        if (playlistFragment != null) {
            playlistFragment.updateCover(albumInfo);
        }
    }

    private void updateStatus(final MPDStatus status) {
        toggleTrackProgress(status);

        final ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
        if (MPDStatus.MPD_STATE_PLAYING.equals(status.getState())) {
            button.setImageDrawable(activity.getResources().getDrawable(
                lightTheme ? R.drawable.ic_media_pause_light : R.drawable.ic_media_pause));
        } else {
            button.setImageDrawable(activity.getResources().getDrawable(
                lightTheme ? R.drawable.ic_media_play_light : R.drawable.ic_media_play));
        }

        setShuffleButton(status.isRandom());
        setRepeatButton(status.isRepeat());

        updateAudioNameText(status);

        // Update the popup menus
        if (currentSong != null) {
            coverMenu.getMenu().setGroupVisible(Menu.NONE, currentSong.getAlbumInfo().isValid());
            // Enable / Disable menu items that need artist and album defined.
            popupMenu.getMenu().findItem(POPUP_ALBUM).setVisible(!isEmpty(currentSong.getAlbum()));
            popupMenu.getMenu().findItem(POPUP_ARTIST)
                    .setVisible(!isEmpty(currentSong.getArtist()));
            boolean showAA = (!isEmpty(currentSong.getAlbumArtist()) &&
                    !currentSong.getAlbumArtist().equals(currentSong.getArtist()));
            popupMenu.getMenu().findItem(POPUP_ALBUMARTIST).setVisible(showAA);
            songInfo.setOnTouchListener(currentSong.isStream() ? popupMenuStreamTouchListener : popupMenuTouchListener);
        } else {
            songInfo.setOnTouchListener(null);
        }
    }

    /**
     * This will update the audioNameText (extra track information) field.
     *
     * @param status An {@code MPDStatus} object.
     */
    private void updateAudioNameText(final MPDStatus status) {
        String optionalTrackInfo = null;
        final String state = status.getState();

        if(currentSong != null && isAudioNameTextEnabled &&
                !MPDStatus.MPD_STATE_STOPPED.equals(state)) {
            final String extension = getExtension(currentSong.getFullpath()).toUpperCase();
            final long bitRate = status.getBitrate();
            final int bitsPerSample = status.getBitsPerSample();
            final int sampleRate = status.getSampleRate();

            /**
             * Check each individual bit of info, the sever can give
             * out empty (and buggy) information from time to time.
             */
            if (!extension.isEmpty()) {
                optionalTrackInfo = extension;
            }

            /** The server can give out buggy (and empty) information from time to time. */
            if (bitRate > 0L) {
                if (optionalTrackInfo != null) {
                    optionalTrackInfo += " | ";
                }
                optionalTrackInfo += bitRate + "kbps";
            }

            if (bitsPerSample > 0) {
                if (optionalTrackInfo != null) {
                    optionalTrackInfo += " | ";
                }
                optionalTrackInfo += bitsPerSample + "bits";
            }

            if (sampleRate > 1000) {
                if (optionalTrackInfo != null) {
                    optionalTrackInfo += " | ";
                }
                optionalTrackInfo += sampleRate / 1000 + "khz";
            }

            if (optionalTrackInfo != null) {
                audioNameText.setText(optionalTrackInfo);
                audioNameText.setVisibility(View.VISIBLE);
            }
        }

        if(optionalTrackInfo == null || currentSong == null) {
            audioNameText.setVisibility(View.GONE);
        }
    }

    public void updateTrackInfo() {
        new updateTrackInfoAsync().execute((MPDStatus[]) null);
    }

    public void updateTrackInfo(MPDStatus status) {
        if(app.oMPDAsyncHelper.oMPD.isConnected()) {
            new updateTrackInfoAsync().execute(status);
        }
    }

    /**
     * Update the track progress numbers and track {@code SeekBar} object.
     *
     * @param elapsed The current track elapsed time.
     * @param totalTrackTime The current track total time.
     */
    private void updateTrackProgress(final long elapsed, final long totalTrackTime) {
        /** In case the total track time is flawed. */
        final long elapsedTime = elapsed > totalTrackTime ? totalTrackTime : elapsed;
        seekBarTrack.setProgress((int) elapsedTime);

        handler.post(new Runnable() {
            @Override
            public void run() {
                trackTime.setText(Music.timeToString(elapsedTime));
                trackTotalTime.setText(Music.timeToString(totalTrackTime));
            }
        });
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        final int volume = mpdStatus.getVolume();

        toggleVolumeBar(volume);
        seekBarVolume.setProgress(volume);
    }

}
