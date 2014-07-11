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
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPDCommand;
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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.PopupMenuCompat;
import android.text.format.DateUtils;
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

import static com.namelessdev.mpdroid.tools.StringUtils.getExtension;

public class NowPlayingFragment extends Fragment implements StatusChangeListener,
        TrackPositionListener,
        OnSharedPreferenceChangeListener, OnMenuItemClickListener,
        UpdateTrackInfo.FullTrackInfoUpdate {

    private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

        public void onClick(final View v) {
            MPDControl.run(v.getId());
        }

        public boolean onLongClick(final View v) {
            if(v.getId() == R.id.playpause) {
                MPDControl.run(MPDControl.ACTION_STOP);
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

            elapsedTime = startTrackTime + elapsedSinceTimerStart / DateUtils.SECOND_IN_MILLIS;

            updateTrackProgress(elapsedTime, totalTrackTime);
        }
    }

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
    private TextView songNameText;
    private TextView albumNameText;
    private TextView audioNameText = null;
    private TextView yearNameText;
    private ImageButton shuffleButton = null;
    private ImageButton repeatButton = null;
    private ImageButton stopButton = null;
    private ImageButton buttonPlayPause = null;
    private boolean isAudioNameTextEnabled = false;
    private boolean shuffleCurrent = false;
    private boolean repeatCurrent = false;
    private View songInfo = null;

    private View.OnTouchListener popupMenuTouchListener = null;

    private View.OnTouchListener popupMenuStreamTouchListener = null;

    private ImageView volumeIcon = null;

    private SeekBar seekBarTrack = null;

    private SeekBar seekBarVolume = null;

    private TextView trackTime = null;
    private TextView trackTotalTime = null;

    private CoverAsyncHelper oCoverAsyncHelper = null;

    private AlbumCoverDownloadListener coverArtListener;

    private ImageView coverArt;

    private static final int ANIMATION_DURATION_MSEC = 1000;

    private Music currentSong = null;
    private Timer volTimer = new Timer();
    private TimerTask volTimerTask = null;
    private Handler handler;
    private Timer posTimer = null;

    private static final String TAG = "NowPlayingFragment";

    private final MPDApplication app = MPDApplication.getInstance();

    private FragmentActivity activity;

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
        } else {
            songNameText.setText(R.string.notConnected);
        }
    }

    private void downloadCover(final AlbumInfo albumInfo) {
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

        if (app.oMPDAsyncHelper.oMPD.isConnected()) {
            try {
                mpdStatus = app.oMPDAsyncHelper.oMPD.getStatus(true);
            } catch (final MPDServerException e) {
                Log.e(TAG, "Failed to seed the status.", e);
            }
        }

        if (mpdStatus != null) {
            updateStatus(mpdStatus);
            updateTrackInfo(mpdStatus);
        } else {
            Log.e(TAG, "Failed to get a force updated status object.");
        }
    }

    private QueueFragment getPlaylistFragment() {
        QueueFragment queueFragment;
        queueFragment = (QueueFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.playlist_fragment);
        return queueFragment;
    }

    protected static int getPlayPauseResource(final String state) {
        final int resource;
        final boolean isPlaying = state.equals(MPDStatus.MPD_STATE_PLAYING);

        if(MPDApplication.getInstance().isLightThemeSelected()) {
            if (isPlaying) {
                resource = R.drawable.ic_media_pause_light;
            } else {
                resource = R.drawable.ic_media_play_light;
            }
        } else {
            if (isPlaying) {
                resource = R.drawable.ic_media_pause;
            } else {
                resource = R.drawable.ic_media_play;
            }
        }

        return resource;
    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;

    }

    /**
     * This is called when cover art needs to be updated due to server information change.
     *
     * @param albumInfo The current albumInfo
     */
    @Override
    public final void onCoverUpdate(final AlbumInfo albumInfo) {
        final int noCoverResource = AlbumCoverDownloadListener.getLargeNoCoverResource();
        coverArt.setImageResource(noCoverResource);

        if(albumInfo != null) {
            downloadCover(albumInfo);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        handler = new Handler();
        setHasOptionsMenu(false);
        activity.setTitle(R.string.nowPlaying);
        activity.registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(
                WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(app.isTabletUiEnabled() ? R.layout.main_fragment_tablet
                : R.layout.main_fragment, container, false);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        settings.registerOnSharedPreferenceChangeListener(this);

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
        coverArt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollToNowPlaying();
            }
        });

        populateCoverArtMenu();

        oCoverAsyncHelper = new CoverAsyncHelper();
        // Scale cover images down to screen width
        oCoverAsyncHelper.setCoverMaxSizeFromScreen(activity);
        oCoverAsyncHelper.setCachedCoverMaxSize(coverArt.getWidth());

        final ProgressBar coverArtProgress =
                (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        coverArtListener = new AlbumCoverDownloadListener(coverArt, coverArtProgress, true);
        oCoverAsyncHelper.addCoverDownloadListener(coverArtListener);

        final ButtonEventHandler buttonEventHandler = new ButtonEventHandler();
        ImageButton button = (ImageButton) view.findViewById(R.id.next);
        button.setOnClickListener(buttonEventHandler);

        button = (ImageButton) view.findViewById(R.id.prev);
        button.setOnClickListener(buttonEventHandler);

        buttonPlayPause = (ImageButton) view.findViewById(R.id.playpause);
        buttonPlayPause.setOnClickListener(buttonEventHandler);
        buttonPlayPause.setOnLongClickListener(buttonEventHandler);

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
            populateSongInfoMenu();
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
                            MPDControl.run(MPDControl.ACTION_VOLUME_SET, lastSentVol);
                        }
                    }

                    public TimerTask setProgress(SeekBar prg) {
                        progress = prg;
                        return this;
                    }
                }.setProgress(seekBar);

                volTimer.scheduleAtFixedRate(volTimerTask, (long) MPDCommand.MIN_VOLUME,
                        (long) MPDCommand.MAX_VOLUME);
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
                MPDControl.run(MPDControl.ACTION_SEEK, seekBar.getProgress());
            }
        });

        songNameText.setText(R.string.notConnected);
        Log.i(TAG, "Initialization succeeded");

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
        coverArt.setImageResource(AlbumCoverDownloadListener.getNoCoverResource());
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
                CoverManager.getInstance().markWrongCover(currentSong.getAlbumInfo());
                downloadCover(currentSong.getAlbumInfo());
                updatePlaylistCovers(currentSong.getAlbumInfo());
                break;
            case POPUP_COVER_SELECTIVE_CLEAN:
                CoverManager.getInstance().clear(currentSong.getAlbumInfo());
                downloadCover(currentSong.getAlbumInfo()); // Update the
                                                           // playlist covers
                updatePlaylistCovers(currentSong.getAlbumInfo());
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Called when a track information change has been detected.
     *
     * @param updatedSong The currentSong item object.
     * @param album The album change.
     * @param artist The artist change.
     * @param date The date change.
     * @param title The title change.
     */
    @Override
    public final void onTrackInfoUpdate(final Music updatedSong, final CharSequence album,
            final CharSequence artist, final CharSequence date, final CharSequence title) {
        currentSong = updatedSong;
        albumNameText.setText(album);
        artistNameText.setText(artist);
        songNameText.setText(title);
        yearNameText.setText(date);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        isAudioNameTextEnabled = settings.getBoolean("enableAudioText", false);
        forceStatusUpdate();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
            final String key) {
        switch(key) {
            case CoverManager.PREFERENCE_CACHE:
            case CoverManager.PREFERENCE_LASTFM:
            case CoverManager.PREFERENCE_LOCALSERVER:
                oCoverAsyncHelper.setCoverRetrieversFromPreferences();
                break;
            case "enableStopButton":
                applyViewVisibility(sharedPreferences, stopButton, key);
                break;
            case "enableAlbumYearText":
                applyViewVisibility(sharedPreferences, yearNameText, key);
                break;
            case "enableAudioText":
                isAudioNameTextEnabled = sharedPreferences.getBoolean(key, false);
                try {
                    updateAudioNameText(app.oMPDAsyncHelper.oMPD.getStatus());
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Could not get a current status.", e);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        app.updateTrackInfo = new UpdateTrackInfo();
        app.updateTrackInfo.addCallback(this);
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.oMPDAsyncHelper.addTrackPositionListener(this);
        app.setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        app.updateTrackInfo.removeCallback(this);
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        app.oMPDAsyncHelper.removeTrackPositionListener(this);
        stopPosTimer();
        app.unsetActivity(this);
    }

    /**
     * Run during fragment initialization, this sets up the cover art popup menu.
     */
    private void populateCoverArtMenu() {
        final PopupMenu coverMenu = new PopupMenu(activity, coverArt);
        final Menu menu = coverMenu.getMenu();

        menu.add(Menu.NONE, POPUP_COVER_BLACKLIST, Menu.NONE, R.string.otherCover);
        menu.add(Menu.NONE, POPUP_COVER_SELECTIVE_CLEAN, Menu.NONE, R.string.resetCover);
        coverMenu.setOnMenuItemClickListener(this);
        coverArt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {
                if (currentSong != null) {
                    coverMenu.getMenu().setGroupVisible(Menu.NONE,
                            currentSong.getAlbumInfo().isValid());
                    coverMenu.show();
                }
                return true;
            }
        });
    }

    /**
     * Run during fragment initialization, this sets up the song info popup menu.
     */
    private void populateSongInfoMenu() {
        final PopupMenu popupMenu = new PopupMenu(activity, songInfo);
        final Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, POPUP_ALBUM, Menu.NONE, R.string.goToAlbum);
        menu.add(Menu.NONE, POPUP_ARTIST, Menu.NONE, R.string.goToArtist);
        menu.add(Menu.NONE, POPUP_ALBUMARTIST, Menu.NONE,
                R.string.goToAlbumArtist);
        menu.add(Menu.NONE, POPUP_FOLDER, Menu.NONE, R.string.goToFolder);
        menu.add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
        menu.add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenuTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenu);

        final PopupMenu popupMenuStream = new PopupMenu(activity, songInfo);
        final Menu menuStream = popupMenuStream.getMenu();
        menuStream.add(Menu.NONE, POPUP_STREAM, Menu.NONE, R.string.goToStream);
        menuStream.add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
        menuStream.add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
        popupMenuStream.setOnMenuItemClickListener(this);
        popupMenuStreamTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenuStream);

        songInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (currentSong != null) {
                    if (currentSong.isStream()) {
                        popupMenuStream.show();
                    } else {
                        // Enable / Disable menu items that need artist and album defined.
                        final boolean showAA = (!currentSong.getAlbumArtist().isEmpty() &&
                                !currentSong.getAlbumArtist().equals(currentSong.getArtist()));

                        popupMenu.getMenu().findItem(POPUP_ALBUM)
                                .setVisible(!currentSong.getAlbum().isEmpty());
                        popupMenu.getMenu().findItem(POPUP_ARTIST)
                                .setVisible(!currentSong.getArtist().isEmpty());
                        popupMenu.getMenu().findItem(POPUP_ALBUMARTIST).setVisible(showAA);
                        popupMenu.show();
                    }
                }
            }
        });
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /**
         * If the current song is a stream, the metadata can change in place, and that will only
         * change the playlist, not the track, so, update if we detect a stream.
         */
        if (currentSong != null && currentSong.isStream() ||
                MPDStatus.MPD_STATE_STOPPED.equals(mpdStatus.getState())) {
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
        QueueFragment queueFragment;
        queueFragment = getPlaylistFragment();
        if (queueFragment != null) {
            queueFragment.scrollToNowPlaying();
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
        final TimerTask posTimerTask = new PosTimerTask(start, total);
        posTimer.scheduleAtFixedRate(posTimerTask, 0L, DateUtils.SECOND_IN_MILLIS);
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
        if (volume < MPDCommand.MIN_VOLUME || volume > MPDCommand.MAX_VOLUME) {
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
        toggleTrackProgress(status);
    }

    private void updatePlaylistCovers(AlbumInfo albumInfo) {
        QueueFragment queueFragment;
        queueFragment = getPlaylistFragment();
        if (queueFragment != null) {
            queueFragment.updateCover(albumInfo);
        }
    }

    private void updateStatus(final MPDStatus status) {
        toggleTrackProgress(status);

        buttonPlayPause.setImageResource(getPlayPauseResource(status.getState()));

        setShuffleButton(status.isRandom());
        setRepeatButton(status.isRepeat());

        updateAudioNameText(status);

        View.OnTouchListener currentListener = null;
        if (currentSong != null) {
            if(currentSong.isStream()) {
                currentListener = popupMenuStreamTouchListener;
            } else {
                currentListener = popupMenuTouchListener;
            }
        }
        songInfo.setOnTouchListener(currentListener);
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
                optionalTrackInfo += sampleRate / 1000 + "kHz";
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

    private void updateTrackInfo(final MPDStatus status) {
        if (app.oMPDAsyncHelper.oMPD.isConnected() && status != null && isAdded()) {
            toggleTrackProgress(status);
            app.updateTrackInfo.refresh(status);
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
