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
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Tools;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.item.AlbumParcelable;
import org.a0z.mpd.item.ArtistParcelable;
import org.a0z.mpd.item.Music;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
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

public class NowPlayingFragment extends Fragment implements StatusChangeListener,
        TrackPositionListener,
        OnSharedPreferenceChangeListener, OnMenuItemClickListener,
        UpdateTrackInfo.FullTrackInfoUpdate {

    private static final int ANIMATION_DURATION_MSEC = 1000;

    private static final int POPUP_ALBUM = 2;

    private static final int POPUP_ALBUMARTIST = 1;

    private static final int POPUP_ARTIST = 0;

    private static final int POPUP_COVER_BLACKLIST = 7;

    private static final int POPUP_COVER_SELECTIVE_CLEAN = 8;

    private static final int POPUP_CURRENT = 6;

    private static final int POPUP_FOLDER = 3;

    private static final int POPUP_SHARE = 5;

    private static final int POPUP_STREAM = 4;

    private static final String TAG = "NowPlayingFragment";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private FragmentActivity mActivity;

    private TextView mAlbumNameText;

    private TextView mArtistNameText;

    private TextView mAudioNameText = null;

    private ImageButton mButtonPlayPause = null;

    private ImageView mCoverArt;

    private CoverAsyncHelper mCoverAsyncHelper = null;

    private AlbumCoverDownloadListener mCoverDownloadListener;

    private Music mCurrentSong = null;

    private Handler mHandler;

    private boolean mIsAudioNameTextEnabled = false;

    private View.OnTouchListener mPopupMenuStreamTouchListener = null;

    private View.OnTouchListener mPopupMenuTouchListener = null;

    private Timer mPosTimer = null;

    private ImageButton mRepeatButton = null;

    private boolean mRepeatCurrent = false;

    private SeekBar mSeekBarTrack = null;

    private SeekBar mSeekBarVolume = null;

    private ImageButton mShuffleButton = null;

    private boolean mShuffleCurrent = false;

    private View mSongInfo = null;

    private TextView mSongNameText;

    private ImageButton mStopButton = null;

    private TextView mTrackTime = null;

    private TextView mTrackTotalTime = null;

    private Timer mVolTimer = new Timer();

    private TimerTask mVolTimerTask = null;

    private ImageView mVolumeIcon = null;

    private TextView mYearNameText;

    protected static int getPlayPauseResource(final int state) {
        final int resource;

        if (MPDApplication.getInstance().isLightThemeSelected()) {
            if (state == MPDStatus.STATE_PLAYING) {
                resource = R.drawable.ic_media_pause_light;
            } else {
                resource = R.drawable.ic_media_play_light;
            }
        } else {
            if (state == MPDStatus.STATE_PLAYING) {
                resource = R.drawable.ic_media_pause;
            } else {
                resource = R.drawable.ic_media_play;
            }
        }

        return resource;
    }

    private void applyViewVisibility(SharedPreferences sharedPreferences, View view,
            String property) {
        if (view == null) {
            return;
        }
        view.setVisibility(
                sharedPreferences.getBoolean(property, false) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        if (connected) {
            forceStatusUpdate();
        } else {
            mSongNameText.setText(R.string.notConnected);
        }
    }

    private void downloadCover(final AlbumInfo albumInfo) {
        mCoverAsyncHelper.downloadCover(albumInfo, true);
    }

    private void forceStatusUpdate() {
        final MPDStatus mpdStatus = mApp.oMPDAsyncHelper.oMPD.getStatus();

        if (mpdStatus.isValid()) {
            volumeChanged(mpdStatus, -1);
            updateStatus(mpdStatus);
            updateTrackInfo(mpdStatus, true);
        }
    }

    private QueueFragment getPlaylistFragment() {
        QueueFragment queueFragment;
        queueFragment = (QueueFragment) mActivity.getSupportFragmentManager()
                .findFragmentById(R.id.playlist_fragment);
        return queueFragment;
    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (FragmentActivity) activity;

    }

    /**
     * This is called when cover art needs to be updated due to server information change.
     *
     * @param albumInfo The current albumInfo
     */
    @Override
    public final void onCoverUpdate(final AlbumInfo albumInfo) {
        final int noCoverResource = AlbumCoverDownloadListener.getLargeNoCoverResource();
        mCoverArt.setImageResource(noCoverResource);

        if (albumInfo != null) {
            downloadCover(albumInfo);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mHandler = new Handler();
        setHasOptionsMenu(false);
        mActivity.setTitle(R.string.nowPlaying);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(mApp.isTabletUiEnabled() ? R.layout.main_fragment_tablet
                : R.layout.main_fragment, container, false);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
        settings.registerOnSharedPreferenceChangeListener(this);

        mArtistNameText = (TextView) view.findViewById(R.id.artistName);
        mAlbumNameText = (TextView) view.findViewById(R.id.albumName);
        mSongNameText = (TextView) view.findViewById(R.id.songName);
        mAudioNameText = (TextView) view.findViewById(R.id.audioName);
        mYearNameText = (TextView) view.findViewById(R.id.yearName);
        applyViewVisibility(settings, mYearNameText, "enableAlbumYearText");
        mArtistNameText.setSelected(true);
        mAlbumNameText.setSelected(true);
        mSongNameText.setSelected(true);
        mAudioNameText.setSelected(true);
        mYearNameText.setSelected(true);
        mShuffleButton = (ImageButton) view.findViewById(R.id.shuffle);
        mRepeatButton = (ImageButton) view.findViewById(R.id.repeat);

        mSeekBarVolume = (SeekBar) view.findViewById(R.id.progress_volume);
        mSeekBarTrack = (SeekBar) view.findViewById(R.id.progress_track);
        mVolumeIcon = (ImageView) view.findViewById(R.id.volume_icon);

        mTrackTime = (TextView) view.findViewById(R.id.trackTime);
        mTrackTotalTime = (TextView) view.findViewById(R.id.trackTotalTime);

        Animation fadeIn = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_in);
        fadeIn.setDuration(ANIMATION_DURATION_MSEC);
        Animation fadeOut = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_out);
        fadeOut.setDuration(ANIMATION_DURATION_MSEC);

        mCoverArt = (ImageView) view.findViewById(R.id.albumCover);
        mCoverArt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollToNowPlaying();
            }
        });

        populateCoverArtMenu();

        mCoverAsyncHelper = new CoverAsyncHelper();
        // Scale cover images down to screen width
        mCoverAsyncHelper.setCoverMaxSizeFromScreen(mActivity);
        mCoverAsyncHelper.setCachedCoverMaxSize(mCoverArt.getWidth());

        final ProgressBar coverArtProgress =
                (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        mCoverDownloadListener = new AlbumCoverDownloadListener(mCoverArt, coverArtProgress, true);
        mCoverAsyncHelper.addCoverDownloadListener(mCoverDownloadListener);

        final ButtonEventHandler buttonEventHandler = new ButtonEventHandler();
        ImageButton button = (ImageButton) view.findViewById(R.id.next);
        button.setOnClickListener(buttonEventHandler);

        button = (ImageButton) view.findViewById(R.id.prev);
        button.setOnClickListener(buttonEventHandler);

        mButtonPlayPause = (ImageButton) view.findViewById(R.id.playpause);
        mButtonPlayPause.setOnClickListener(buttonEventHandler);
        mButtonPlayPause.setOnLongClickListener(buttonEventHandler);

        mStopButton = (ImageButton) view.findViewById(R.id.stop);
        mStopButton.setOnClickListener(buttonEventHandler);
        mStopButton.setOnLongClickListener(buttonEventHandler);
        applyViewVisibility(settings, mStopButton, "enableStopButton");

        if (null != mShuffleButton) {
            mShuffleButton.setOnClickListener(buttonEventHandler);
        }
        if (null != mRepeatButton) {
            mRepeatButton.setOnClickListener(buttonEventHandler);
        }

        mSongInfo = view.findViewById(R.id.songInfo);
        if (mSongInfo != null) {
            populateSongInfoMenu();
        }

        mSeekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                mVolTimerTask = new TimerTask() {
                    int mLastSentVol = -1;

                    SeekBar mProgress;

                    public void run() {
                        if (mLastSentVol != mProgress.getProgress()) {
                            mLastSentVol = mProgress.getProgress();
                            MPDControl.run(MPDControl.ACTION_VOLUME_SET, mLastSentVol);
                        }
                    }

                    public TimerTask setProgress(SeekBar prg) {
                        mProgress = prg;
                        return this;
                    }
                }.setProgress(seekBar);

                mVolTimer.scheduleAtFixedRate(mVolTimerTask, (long) MPDCommand.MIN_VOLUME,
                        (long) MPDCommand.MAX_VOLUME);
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mVolTimerTask.cancel();
                mVolTimerTask.run();
            }
        });

        mSeekBarTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        mSongNameText.setText(R.string.notConnected);
        Log.i(TAG, "Initialization succeeded");

        return view;
    }

    @Override
    public void onDestroy() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
        settings.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        mCoverArt.setImageResource(AlbumCoverDownloadListener.getNoCoverResource());
        mCoverDownloadListener.freeCoverDrawable();
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mActivity = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case POPUP_ALBUM:
                final Parcelable albumParcel =
                        new AlbumParcelable(mCurrentSong.getAlbumAsAlbum());
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("album", albumParcel);
                startActivityForResult(intent, -1);
                break;
            case POPUP_ALBUMARTIST:
                final Parcelable albumArtistParcel =
                        new ArtistParcelable(mCurrentSong.getAlbumArtistAsArtist());
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("artist", albumArtistParcel);
                startActivityForResult(intent, -1);
                break;
            case POPUP_ARTIST:
                final Parcelable artistParcel =
                        new ArtistParcelable(mCurrentSong.getArtistAsArtist());
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("artist", artistParcel);
                startActivityForResult(intent, -1);
                break;
            case POPUP_COVER_BLACKLIST:
                CoverManager.getInstance().markWrongCover(mCurrentSong.getAlbumInfo());
                downloadCover(mCurrentSong.getAlbumInfo());
                updatePlaylistCovers(mCurrentSong.getAlbumInfo());
                break;
            case POPUP_COVER_SELECTIVE_CLEAN:
                CoverManager.getInstance().clear(mCurrentSong.getAlbumInfo());
                downloadCover(mCurrentSong.getAlbumInfo()); // Update the
                // playlist covers
                updatePlaylistCovers(mCurrentSong.getAlbumInfo());
                break;
            case POPUP_CURRENT:
                scrollToNowPlaying();
                break;
            case POPUP_FOLDER:
                final String path = mCurrentSong.getFullPath();
                final String parent = mCurrentSong.getParent();
                if (path == null || parent == null) {
                    break;
                }
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("folder", parent);
                startActivityForResult(intent, -1);
                break;
            case POPUP_SHARE:
                String shareString = getString(R.string.sharePrefix);
                shareString += " " + mCurrentSong.getTitle();
                if (!mCurrentSong.isStream()) {
                    shareString += " - " + mCurrentSong.getArtist();
                }
                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareString);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;
            case POPUP_STREAM:
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("streams", true);
                startActivityForResult(intent, -1);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mIsAudioNameTextEnabled = settings.getBoolean("enableAudioText", false);
        forceStatusUpdate();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
            final String key) {
        switch (key) {
            case CoverManager.PREFERENCE_CACHE:
            case CoverManager.PREFERENCE_LASTFM:
            case CoverManager.PREFERENCE_LOCALSERVER:
                mCoverAsyncHelper.setCoverRetrieversFromPreferences();
                break;
            case "enableStopButton":
                applyViewVisibility(sharedPreferences, mStopButton, key);
                break;
            case "enableAlbumYearText":
                applyViewVisibility(sharedPreferences, mYearNameText, key);
                break;
            case "enableAudioText":
                mIsAudioNameTextEnabled = sharedPreferences.getBoolean(key, false);
                updateAudioNameText(mApp.oMPDAsyncHelper.oMPD.getStatus());
                break;
            default:
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mApp.updateTrackInfo == null) {
            mApp.updateTrackInfo = new UpdateTrackInfo();
        }
        mApp.updateTrackInfo.addCallback(this);
        mApp.oMPDAsyncHelper.addStatusChangeListener(this);
        mApp.oMPDAsyncHelper.addTrackPositionListener(this);
        mApp.setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        mApp.updateTrackInfo.removeCallback(this);
        mApp.oMPDAsyncHelper.removeStatusChangeListener(this);
        mApp.oMPDAsyncHelper.removeTrackPositionListener(this);
        stopPosTimer();
        mApp.unsetActivity(this);
    }

    /**
     * Called when a track information change has been detected.
     *
     * @param updatedSong The currentSong item object.
     * @param album       The album change.
     * @param artist      The artist change.
     * @param date        The date change.
     * @param title       The title change.
     */
    @Override
    public final void onTrackInfoUpdate(final Music updatedSong, final CharSequence album,
            final CharSequence artist, final CharSequence date, final CharSequence title) {
        mCurrentSong = updatedSong;
        mAlbumNameText.setText(album);
        mArtistNameText.setText(artist);
        mSongNameText.setText(title);
        mYearNameText.setText(date);
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /**
         * If the current song is a stream, the metadata can change in place, and that will only
         * change the playlist, not the track, so, update if we detect a stream.
         */
        if (mCurrentSong != null && mCurrentSong.isStream() ||
                mpdStatus.isState(MPDStatus.STATE_STOPPED)) {
            updateTrackInfo(mpdStatus, false);
        }
    }

    /**
     * Run during fragment initialization, this sets up the cover art popup menu.
     */
    private void populateCoverArtMenu() {
        final PopupMenu coverMenu = new PopupMenu(mActivity, mCoverArt);
        final Menu menu = coverMenu.getMenu();

        menu.add(Menu.NONE, POPUP_COVER_BLACKLIST, Menu.NONE, R.string.otherCover);
        menu.add(Menu.NONE, POPUP_COVER_SELECTIVE_CLEAN, Menu.NONE, R.string.resetCover);
        coverMenu.setOnMenuItemClickListener(this);
        mCoverArt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {
                if (mCurrentSong != null) {
                    coverMenu.getMenu().setGroupVisible(Menu.NONE,
                            mCurrentSong.getAlbumInfo().isValid());
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
        final PopupMenu popupMenu = new PopupMenu(mActivity, mSongInfo);
        final Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, POPUP_ALBUM, Menu.NONE, R.string.goToAlbum);
        menu.add(Menu.NONE, POPUP_ARTIST, Menu.NONE, R.string.goToArtist);
        menu.add(Menu.NONE, POPUP_ALBUMARTIST, Menu.NONE,
                R.string.goToAlbumArtist);
        menu.add(Menu.NONE, POPUP_FOLDER, Menu.NONE, R.string.goToFolder);
        menu.add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
        menu.add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
        popupMenu.setOnMenuItemClickListener(this);
        mPopupMenuTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenu);

        final PopupMenu popupMenuStream = new PopupMenu(mActivity, mSongInfo);
        final Menu menuStream = popupMenuStream.getMenu();
        menuStream.add(Menu.NONE, POPUP_STREAM, Menu.NONE, R.string.goToStream);
        menuStream.add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
        menuStream.add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
        popupMenuStream.setOnMenuItemClickListener(this);
        mPopupMenuStreamTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenuStream);

        mSongInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mCurrentSong != null) {
                    if (mCurrentSong.isStream()) {
                        popupMenuStream.show();
                    } else {
                        // Enable / Disable menu items that need artist and album defined.
                        final boolean showAA = (!mCurrentSong.getAlbumArtist().isEmpty() &&
                                !mCurrentSong.getAlbumArtist().equals(mCurrentSong.getArtist()));

                        popupMenu.getMenu().findItem(POPUP_ALBUM)
                                .setVisible(!mCurrentSong.getAlbum().isEmpty());
                        popupMenu.getMenu().findItem(POPUP_ARTIST)
                                .setVisible(!mCurrentSong.getArtist().isEmpty());
                        popupMenu.getMenu().findItem(POPUP_ALBUMARTIST).setVisible(showAA);
                        popupMenu.show();
                    }
                }
            }
        });
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
        if (null != mRepeatButton && mRepeatCurrent != on) {
            int[] attrs = new int[]{
                    on ? R.attr.repeatEnabled : R.attr.repeatDisabled
            };
            final TypedArray ta = mActivity.obtainStyledAttributes(attrs);
            final Drawable drawableFromTheme = ta.getDrawable(0);
            mRepeatButton.setImageDrawable(drawableFromTheme);
            mRepeatButton.invalidate();
            mRepeatCurrent = on;
        }
    }

    private void setShuffleButton(boolean on) {
        if (null != mShuffleButton && mShuffleCurrent != on) {
            int[] attrs = new int[]{
                    on ? R.attr.shuffleEnabled : R.attr.shuffleDisabled
            };
            final TypedArray ta = mActivity.obtainStyledAttributes(attrs);
            final Drawable drawableFromTheme = ta.getDrawable(0);
            mShuffleButton.setImageDrawable(drawableFromTheme);
            mShuffleButton.invalidate();
            mShuffleCurrent = on;
        }
    }

    private void startPosTimer(final long start, final long total) {
        stopPosTimer();
        mPosTimer = new Timer();
        final TimerTask posTimerTask = new PosTimerTask(start, total);
        mPosTimer.scheduleAtFixedRate(posTimerTask, 0L, DateUtils.SECOND_IN_MILLIS);
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
        if (mActivity != null) {
            updateStatus(mpdStatus);
        }
    }

    private void stopPosTimer() {
        if (null != mPosTimer) {
            mPosTimer.cancel();
            mPosTimer = null;
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
            mTrackTime.setVisibility(View.INVISIBLE);
            mTrackTotalTime.setVisibility(View.INVISIBLE);
            stopPosTimer();
            mSeekBarTrack.setProgress(0);
            mSeekBarTrack.setEnabled(false);
        } else {
            final long elapsedTime = status.getElapsedTime();

            if (status.isState(MPDStatus.STATE_PLAYING)) {
                startPosTimer(elapsedTime, totalTime);
            } else {
                stopPosTimer();
                updateTrackProgress(elapsedTime, totalTime);
            }

            mSeekBarTrack.setMax((int) totalTime);

            mTrackTime.setVisibility(View.VISIBLE);
            mTrackTotalTime.setVisibility(View.VISIBLE);
            mSeekBarTrack.setEnabled(true);
        }
    }

    /**
     * This enables or disables the volume, depending on the volume given by the server.
     *
     * @param volume The current volume value.
     */
    private void toggleVolumeBar(final int volume) {
        if (volume < MPDCommand.MIN_VOLUME || volume > MPDCommand.MAX_VOLUME) {
            mSeekBarVolume.setEnabled(false);
            mSeekBarVolume.setVisibility(View.GONE);
            mVolumeIcon.setVisibility(View.GONE);
        } else {
            mSeekBarVolume.setEnabled(true);
            mSeekBarVolume.setVisibility(View.VISIBLE);
            mVolumeIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        updateTrackInfo(mpdStatus, false);
    }

    @Override
    public void trackPositionChanged(final MPDStatus status) {
        toggleTrackProgress(status);
    }

    /**
     * This will update the audioNameText (extra track information) field.
     *
     * @param status An {@code MPDStatus} object.
     */
    private void updateAudioNameText(final MPDStatus status) {
        String optionalTrackInfo = null;

        if (mCurrentSong != null && mIsAudioNameTextEnabled &&
                !status.isState(MPDStatus.STATE_STOPPED)) {
            final String extension = Tools.getExtension(mCurrentSong.getFullPath()).toUpperCase();
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
                mAudioNameText.setText(optionalTrackInfo);
                mAudioNameText.setVisibility(View.VISIBLE);
            }
        }

        if (optionalTrackInfo == null || mCurrentSong == null) {
            mAudioNameText.setVisibility(View.GONE);
        }
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

        mButtonPlayPause.setImageResource(getPlayPauseResource(status.getState()));

        setShuffleButton(status.isRandom());
        setRepeatButton(status.isRepeat());

        updateAudioNameText(status);

        View.OnTouchListener currentListener = null;
        if (mCurrentSong != null) {
            if (mCurrentSong.isStream()) {
                currentListener = mPopupMenuStreamTouchListener;
            } else {
                currentListener = mPopupMenuTouchListener;
            }
        }
        mSongInfo.setOnTouchListener(currentListener);
    }

    private void updateTrackInfo(final MPDStatus status, final boolean forcedUpdate) {
        if (mApp.oMPDAsyncHelper.oMPD.isConnected() && isAdded()) {
            toggleTrackProgress(status);
            mApp.updateTrackInfo.refresh(status, forcedUpdate);
        }
    }

    /**
     * Update the track progress numbers and track {@code SeekBar} object.
     *
     * @param elapsed        The current track elapsed time.
     * @param totalTrackTime The current track total time.
     */
    private void updateTrackProgress(final long elapsed, final long totalTrackTime) {
        /** In case the total track time is flawed. */
        final long elapsedTime = elapsed > totalTrackTime ? totalTrackTime : elapsed;
        mSeekBarTrack.setProgress((int) elapsedTime);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTrackTime.setText(Music.timeToString(elapsedTime));
                mTrackTotalTime.setText(Music.timeToString(totalTrackTime));
            }
        });
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        final int volume = mpdStatus.getVolume();

        toggleVolumeBar(volume);
        mSeekBarVolume.setProgress(volume);
    }

    private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

        public void onClick(final View v) {
            MPDControl.run(v.getId());
        }

        public boolean onLongClick(final View v) {
            if (v.getId() == R.id.playpause) {
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

        private final long mTimerStartTime;

        private long mElapsedTime = 0L;

        private long mStartTrackTime = 0L;

        private long mTotalTrackTime = 0L;

        private PosTimerTask(final long start, final long total) {
            super();
            this.mStartTrackTime = start;
            this.mTotalTrackTime = total;
            this.mTimerStartTime = new Date().getTime();
        }

        @Override
        public void run() {
            final long elapsedSinceTimerStart = new Date().getTime() - mTimerStartTime;

            mElapsedTime = mStartTrackTime + elapsedSinceTimerStart / DateUtils.SECOND_IN_MILLIS;

            updateTrackProgress(mElapsedTime, mTotalTrackTime);
        }
    }
}
