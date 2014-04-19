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

    private class PosTimerTask extends TimerTask {
        Date date = new Date();
        long start = 0;
        long elapsed = 0;

        public PosTimerTask(long start) {
            this.start = start;
        }

        @Override
        public void run() {
            Date now = new Date();
            elapsed = start + ((now.getTime() - date.getTime()) / 1000);
            progressBarTrack.setProgress((int) elapsed);
            if (currentSong != null && !currentSong.isStream()) {
                elapsed = elapsed > lastSongTime ? lastSongTime : elapsed;
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    trackTime.setText(timeToString(elapsed));
                    trackTotalTime.setText(timeToString(lastSongTime));
                }
            });
            lastElapsedTime = elapsed;
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

                int songMax = 0;
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
                    songMax = (int) actSong.getTime();
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
                    songMax = (int) actSong.getTime();
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
                progressBarTrack.setMax(songMax);
                yearNameText.setText(date);

                updateStatus(status);
                if (noSong || actSong.isStream()) {
                    lastArtist = artist;
                    lastAlbum = album;
                    trackTime.setText(timeToString(0));
                    trackTotalTime.setText(timeToString(0));
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
                progressBarTrack.setMax(0);
                yearNameText.setText("");
                audioNameText.setText("");
            }
        }
    }

    public static final String PREFS_NAME = "mpdroid.properties";
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
    private TextView audioNameText;
    private TextView yearNameText;
    private ImageButton shuffleButton = null;
    private ImageButton repeatButton = null;
    private ImageButton stopButton = null;
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

    public static final int VOLUME_STEP = 5;
    private static final int ANIMATION_DURATION_MSEC = 1000;

    private static String timeToString(long seconds) {
        if (seconds < 0) {
            seconds = 0;
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

    public void checkConnected() {
        connected = ((MPDApplication) activity.getApplication()).oMPDAsyncHelper.oMPD.isConnected();
        if (connected) {
            songNameText.setText(activity.getResources().getString(R.string.noSongInfo));
        } else {
            songNameText.setText(activity.getResources().getString(R.string.notConnected));
        }
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        checkConnected();
    }

    private void downloadCover(AlbumInfo albumInfo) {
        oCoverAsyncHelper.downloadCover(albumInfo, true);
    }

    /**
     * This enables or disables the volume, depending on the volume given by the server.
     *
     * @param volume The current volume value.
     */
    private void toggleVolumeBar(final int volume) {
        final int OUTPUT_VOLUME_UNSUPPORTED = -1;

        if (volume == OUTPUT_VOLUME_UNSUPPORTED) {
            progressBarVolume.setEnabled(false);
            progressBarVolume.setVisibility(View.GONE);
            volumeIcon.setVisibility(View.GONE);
        } else {
            progressBarVolume.setEnabled(true);
            progressBarVolume.setVisibility(View.VISIBLE);
            volumeIcon.setVisibility(View.VISIBLE);
        }
    }

    private PlaylistFragment getPlaylistFragment() {
        PlaylistFragment playlistFragment;
        playlistFragment = (PlaylistFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.playlist_fragment);
        return playlistFragment;
    }

    public SeekBar getProgressBarTrack() {
        return progressBarTrack;
    }

    @Override
    public void libraryStateChanged(boolean updating) {
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
        applyViewVisibility(settings, audioNameText, "enableAudioText");
        yearNameText = (TextView) view.findViewById(R.id.yearName);
        applyViewVisibility(settings, yearNameText, "enableAlbumYearText");
        artistNameText.setSelected(true);
        albumNameText.setSelected(true);
        songNameText.setSelected(true);
        audioNameText.setSelected(true);
        yearNameText.setSelected(true);
        shuffleButton = (ImageButton) view.findViewById(R.id.shuffle);
        repeatButton = (ImageButton) view.findViewById(R.id.repeat);

        progressBarVolume = (SeekBar) view.findViewById(R.id.progress_volume);
        progressBarTrack = (SeekBar) view.findViewById(R.id.progress_track);
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

        progressBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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

        progressBarTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            public void onStopTrackingTouch(final SeekBar seekBar) {
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
        // Annoyingly this seems to be run when the app starts the first time
        // to.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        showAlbumArtist = settings.getBoolean("showAlbumArtist", true);

        // Just to make sure that we do actually get an update.
        try {
            updateTrackInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateStatus(null);

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
            applyViewVisibility(sharedPreferences, audioNameText, key);
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
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
        // If the playlist changed but not the song position in the playlist
        // We end up being desynced. Update the current song.
        try {
            updateTrackInfo();
        } catch (Exception e) {
            e.printStackTrace();
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

    private void startPosTimer(long start) {
        stopPosTimer();
        posTimer = new Timer();
        posTimerTask = new PosTimerTask(start);
        posTimer.scheduleAtFixedRate(posTimerTask, 0, 1000);
    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) {
        updateStatus(mpdStatus);
    }

    private void stopPosTimer() {
        if (null != posTimer) {
            posTimer.cancel();
            posTimer = null;
        }
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        updateTrackInfo(mpdStatus);
    }

    @Override
    public void trackPositionChanged(MPDStatus status) {
        startPosTimer(status.getElapsedTime());
    }

    private void updatePlaylistCovers(AlbumInfo albumInfo) {
        PlaylistFragment playlistFragment;
        playlistFragment = getPlaylistFragment();
        if (playlistFragment != null) {
            playlistFragment.updateCover(albumInfo);
        }
    }

    private void updateStatus(MPDStatus status) {
        if (activity == null)
            return;
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
                button.setImageDrawable(activity.getResources().getDrawable(
                        lightTheme ? R.drawable.ic_media_pause_light : R.drawable.ic_media_pause));
            } else {
                stopPosTimer();
                ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
                button.setImageDrawable(activity.getResources().getDrawable(
                        lightTheme ? R.drawable.ic_media_play_light : R.drawable.ic_media_play));
            }
        }
        setShuffleButton(status.isRandom());
        setRepeatButton(status.isRepeat());

        // Update audio properties
        if (audioNameText != null && currentSong != null) {
            StringBuffer sb = new StringBuffer();
            String extension = getExtension(currentSong.getFullpath());

            if (!isEmpty(extension)) {
                sb.append(extension.toUpperCase());
                sb.append(" | ");
            }

            sb.append(status.getBitrate());
            sb.append(" kbps | ");
            sb.append(status.getBitsPerSample());
            sb.append(" bits | ");
            sb.append(status.getSampleRate() / 1000);
            sb.append(" khz");
            audioNameText.setText(sb.toString());
        }

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

    public void updateTrackInfo() {
        new updateTrackInfoAsync().execute((MPDStatus[]) null);
    }

    public void updateTrackInfo(MPDStatus status) {
        new updateTrackInfoAsync().execute(status);
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        final int volume = mpdStatus.getVolume();

        toggleVolumeBar(volume);
        progressBarVolume.setProgress(volume);
    }

}
