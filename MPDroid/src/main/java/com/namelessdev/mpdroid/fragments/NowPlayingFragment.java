/*
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.fragments;

import com.anpmech.mpd.Tools;
import com.anpmech.mpd.connection.MPDConnectionListener;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.Stream;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;
import com.anpmech.mpd.subsystem.status.StatusChangeListener;
import com.anpmech.mpd.subsystem.status.TrackPositionListener;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.annotation.IdRes;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.namelessdev.mpdroid.tools.Tools.notifyUser;

public class NowPlayingFragment extends Fragment implements
        MPDConnectionListener, StatusChangeListener,
        TrackPositionListener, OnSharedPreferenceChangeListener, OnMenuItemClickListener,
        UpdateTrackInfo.FullTrackInfoUpdate {

    /** In milliseconds. */
    private static final long ANIMATION_DURATION = 1000L;

    private static final int POPUP_ALBUM = 2;

    private static final int POPUP_ALBUM_ARTIST = 1;

    private static final int POPUP_ARTIST = 0;

    private static final int POPUP_COVER_BLACKLIST = 7;

    private static final int POPUP_COVER_SELECTIVE_CLEAN = 8;

    private static final int POPUP_CURRENT = 6;

    private static final int POPUP_FOLDER = 3;

    private static final int POPUP_LYRICS = 9;

    private static final int POPUP_SHARE = 5;

    private static final int POPUP_STREAM = 4;

    private static final String QUICK_LYRIC_PACKAGE_NAME = "com.geecko.QuickLyric";

    private static final String TAG = "NowPlayingFragment";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private final MPDStatus mMPDStatus = mApp.getMPD().getStatus();

    private final Timer mVolTimer = new Timer();

    private FragmentActivity mActivity;

    private TextView mAlbumNameText;

    private TextView mArtistNameText;

    private TextView mAudioNameText = null;

    private ImageView mCoverArt;

    private CoverAsyncHelper mCoverAsyncHelper = null;

    private AlbumCoverDownloadListener mCoverDownloadListener;

    private Music mCurrentSong = null;

    private Handler mHandler;

    private boolean mIsAudioNameTextEnabled = false;

    private ImageButton mPlayPauseButton = null;

    private View.OnTouchListener mPopupMenuStreamTouchListener = null;

    private View.OnTouchListener mPopupMenuTouchListener = null;

    private Timer mPosTimer = null;

    private ImageButton mRepeatButton = null;

    private SharedPreferences mSharedPreferences;

    private ImageButton mShuffleButton = null;

    private View mSongInfo = null;

    private TextView mSongNameText;

    private RatingBar mSongRating = null;

    private ImageButton mStopButton = null;

    private SeekBar mTrackSeekBar = null;

    private TextView mTrackTime = null;

    private TextView mTrackTotalTime = null;

    private TimerTask mVolTimerTask = null;

    private ImageView mVolumeIcon = null;

    private SeekBar mVolumeSeekBar = null;

    private TextView mYearNameText;

    /**
     * A convenience method to find a resource and set it as selected.
     *
     * @param view     The view to find the resource in.
     * @param resource The resource to find in the view.
     * @return The TextView found in the {@code view}, set as selected.
     */
    private static TextView findSelected(final View view, @IdRes final int resource) {
        final TextView textView = (TextView) view.findViewById(resource);

        textView.setSelected(true);

        return textView;
    }

    /**
     * This method sets up a resource with the button event handler.
     *
     * @param view      The {@code View} with which to setup the {@code ImageButton}.
     * @param resource  The resource to find in the view.
     * @param longPress Whether long press is supported by this event button.
     * @return The generated {@code ImageButton}.
     */
    private static ImageButton getEventButton(final View view, @IdRes final int resource,
            final boolean longPress) {
        final ImageButton button = (ImageButton) view.findViewById(resource);
        final ButtonEventHandler buttonEventHandler = new ButtonEventHandler();

        button.setOnClickListener(buttonEventHandler);
        if (longPress) {
            button.setOnLongClickListener(buttonEventHandler);
        }

        return button;
    }

    protected static int getPlayPauseResource(final int state) {
        final int resource;

        if (MPDApplication.getInstance().isLightThemeSelected()) {
            if (state == MPDStatusMap.STATE_PLAYING) {
                resource = R.drawable.ic_media_pause_light;
            } else {
                resource = R.drawable.ic_media_play_light;
            }
        } else {
            if (state == MPDStatusMap.STATE_PLAYING) {
                resource = R.drawable.ic_media_pause;
            } else {
                resource = R.drawable.ic_media_play;
            }
        }

        return resource;
    }

    /**
     * Retrieve styled attribute information for the repeat button.
     *
     * @param on True if repeat is enabled, false otherwise.
     * @return Returns the enabled repeat styled attribute if on, returns the disabled repeat styled
     * attribute otherwise.
     */
    private static int getRepeatAttribute(final boolean on) {
        final int attribute;

        if (on) {
            attribute = R.attr.repeatEnabled;
        } else {
            attribute = R.attr.repeatDisabled;
        }

        return attribute;
    }

    /**
     * Retrieve styled attribute information for the random button.
     *
     * @param on True if random is enabled, false otherwise.
     * @return Returns the enabled random styled attribute if on, returns the disabled random styled
     * attribute otherwise.
     */
    private static int getShuffleAttribute(final boolean on) {
        final int attribute;

        if (on) {
            attribute = R.attr.shuffleEnabled;
        } else {
            attribute = R.attr.shuffleDisabled;
        }

        return attribute;
    }

    /**
     * Generates the initial track {@link android.widget.SeekBar}.
     *
     * @param view The view in which to setup the {@code SeekBar} for.
     * @return The constructed SeekBar for the track position modification.
     */
    private static SeekBar getTrackSeekBar(final View view) {
        final SeekBar.OnSeekBarChangeListener seekBarTrackListener =
                new SeekBar.OnSeekBarChangeListener() {
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
                };

        final SeekBar seekBarTrack = (SeekBar) view.findViewById(R.id.progress_track);
        seekBarTrack.setOnSeekBarChangeListener(seekBarTrackListener);

        return seekBarTrack;
    }

    private void applyViewVisibility(final View view, final String property) {
        if (mSharedPreferences.getBoolean(property, false)) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Called upon connection.
     *
     * @param commandErrorCode If this number is non-zero, the number will correspond to a
     *                         {@link MPDException} error code. If this number is zero, the
     *                         connection MPD protocol commands were successful.
     */
    @Override
    public void connectionConnected(final int commandErrorCode) {
        forceStatusUpdate();
    }

    /**
     * Called when connecting.
     */
    @Override
    public void connectionConnecting() {
    }

    /**
     * Called upon disconnection.
     *
     * @param reason The reason given for disconnection.
     */
    @Override
    public void connectionDisconnected(final String reason) {
        mSongNameText.setText(R.string.notConnected);
    }

    private void downloadCover(final AlbumInfo albumInfo) {
        mCoverAsyncHelper.downloadCover(albumInfo, true);
    }

    private void forceStatusUpdate() {
        if (mMPDStatus.isValid()) {
            volumeChanged(MPDStatusMap.VOLUME_UNAVAILABLE);
            updateStatus();
            updateTrackInfo(true);
            setButtonAttribute(getRepeatAttribute(mMPDStatus.isRepeat()), mRepeatButton);
            setButtonAttribute(getShuffleAttribute(mMPDStatus.isRandom()), mShuffleButton);
            setStickerVisibility();
        }
    }

    /**
     * Run during fragment initialization, this sets up the cover art popup menu and the coverArt
     * ImageView.
     *
     * @param view The view to setup the coverArt ImageView in.
     * @return The resulting ImageView.
     */
    private ImageView getCoverArt(final View view) {
        final ImageView coverArt = (ImageView) view.findViewById(R.id.albumCover);
        final PopupMenu coverMenu = new PopupMenu(mActivity, coverArt);
        final Menu menu = coverMenu.getMenu();

        coverArt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                scrollToNowPlaying();
            }
        });

        menu.add(Menu.NONE, POPUP_COVER_BLACKLIST, Menu.NONE, R.string.otherCover);
        menu.add(Menu.NONE, POPUP_COVER_SELECTIVE_CLEAN, Menu.NONE, R.string.resetCover);
        coverMenu.setOnMenuItemClickListener(this);
        coverArt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                final boolean isConsumed;

                if (mCurrentSong != null) {
                    menu.setGroupVisible(Menu.NONE, new AlbumInfo(mCurrentSong).isValid());
                    coverMenu.show();
                    isConsumed = true;
                } else {
                    isConsumed = false;
                }

                return isConsumed;
            }
        });

        return coverArt;
    }

    /**
     * This sets up the {@code CoverAsyncHelper} for this class.
     *
     * @param view The view in which to setup the {@code CoverAsyncHelper} for.
     * @return The CoverAsyncHelper used as a field in this class.
     */
    private CoverAsyncHelper getCoverAsyncHelper(final View view) {
        final CoverAsyncHelper coverAsyncHelper = new CoverAsyncHelper();
        final ProgressBar coverArtProgress =
                (ProgressBar) view.findViewById(R.id.albumCoverProgress);

        // Scale cover images down to screen width
        coverAsyncHelper.setCoverMaxSizeFromScreen(mActivity);
        coverAsyncHelper.setCachedCoverMaxSize(mCoverArt.getWidth());

        mCoverDownloadListener = new AlbumCoverDownloadListener(mCoverArt, coverArtProgress, true);
        coverAsyncHelper.addCoverDownloadListener(mCoverDownloadListener);

        return coverAsyncHelper;
    }

    /**
     * This produces a intent to open QuickLyric.
     *
     * @return An Intent to open QuickLyric.
     */
    private Intent getLyricIntent() {
        final Intent intent = new Intent(QUICK_LYRIC_PACKAGE_NAME + ".getLyrics");
        final String[] tags = {mCurrentSong.getArtistName(), mCurrentSong.getTitle()};

        intent.putExtra("TAGS", tags);

        return intent;
    }

    /**
     * This method generates selected track information to send to another application.
     *
     * <p>The current format of this method should output: header artist - title and if the output
     * is a stream, the URL should be suffixed on the end.</p>
     *
     * @return The track information to send to another application.
     */
    private Intent getShareIntent() {
        final Intent intent = new Intent(Intent.ACTION_SEND, null);
        final char[] separator = {' ', '-', ' '};
        final String fullPath = mCurrentSong.getFullPath();
        final String sharePrefix = getString(R.string.sharePrefix);
        final String trackArtist = mCurrentSong.getArtistName();
        final String trackTitle = mCurrentSong.getTitle();
        final int initialLength = trackTitle.length() + sharePrefix.length() + 64;
        final StringBuilder shareString = new StringBuilder(initialLength);

        shareString.append(sharePrefix);
        shareString.append(' ');

        if (trackArtist != null) {
            shareString.append(trackArtist);
            shareString.append(separator);
        }
        shareString.append(trackTitle);

        /** If track title is empty, the full path will have been substituted.*/
        if (mCurrentSong.isStream() && !fullPath.startsWith(trackTitle)) {
            shareString.append(separator);
            shareString.append(fullPath);
        }

        intent.putExtra(Intent.EXTRA_TEXT, shareString.toString());
        intent.setType("text/plain");

        return intent;
    }

    /**
     * Run during fragment initialization, this sets up the song info popup menu.
     *
     * @param view The view in which to setup the song info View for this class.
     * @return The song info view used as a field in this class.
     */
    private View getSongInfo(final View view) {
        final View songInfo = view.findViewById(R.id.songInfo);

        final PopupMenu popupMenu = new PopupMenu(mActivity, songInfo);
        final Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, POPUP_ALBUM, Menu.NONE, R.string.goToAlbum);
        menu.add(Menu.NONE, POPUP_ARTIST, Menu.NONE, R.string.goToArtist);
        menu.add(Menu.NONE, POPUP_ALBUM_ARTIST, Menu.NONE,
                R.string.goToAlbumArtist);
        menu.add(Menu.NONE, POPUP_FOLDER, Menu.NONE, R.string.goToFolder);
        menu.add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
        if (com.namelessdev.mpdroid.tools.Tools.isPackageInstalled(QUICK_LYRIC_PACKAGE_NAME)) {
            menu.add(Menu.NONE, POPUP_LYRICS, Menu.NONE, R.string.lyrics);
        }
        menu.add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
        popupMenu.setOnMenuItemClickListener(this);
        mPopupMenuTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenu);

        final PopupMenu popupMenuStream = new PopupMenu(mActivity, songInfo);
        final Menu menuStream = popupMenuStream.getMenu();
        menuStream.add(Menu.NONE, POPUP_STREAM, Menu.NONE, R.string.goToStream);
        menuStream.add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.goToCurrent);
        menuStream.add(Menu.NONE, POPUP_SHARE, Menu.NONE, R.string.share);
        popupMenuStream.setOnMenuItemClickListener(this);
        mPopupMenuStreamTouchListener = PopupMenuCompat.getDragToOpenListener(popupMenuStream);

        songInfo.setOnClickListener(new OnClickListener() {

            /**
             * Checks whether the album artist should be on the popup menu for the current track.
             *
             * @return True if the album artist popup menu entry should be visible, false otherwise.
             */
            private boolean isAlbumArtistVisible() {
                boolean albumArtistEnabled = false;
                final String albumArtist = mCurrentSong.getAlbumArtistName();

                if (albumArtist != null && !albumArtist.isEmpty()) {
                    final String artist = mCurrentSong.getArtistName();

                    if (isArtistVisible() && !albumArtist.equals(artist)) {
                        albumArtistEnabled = true;
                    }
                }

                return albumArtistEnabled;
            }

            /**
             * Checks whether the album should be on the popup menu for the current track.
             *
             * @return True if the album popup menu entry should be visible, false otherwise.
             */
            private boolean isAlbumVisible() {
                final boolean isAlbumVisible;
                final String album = mCurrentSong.getAlbumName();

                if (album != null && !album.isEmpty()) {
                    isAlbumVisible = true;
                } else {
                    isAlbumVisible = false;
                }

                return isAlbumVisible;
            }

            /**
             * Checks whether the artist should be on the popup menu for the current track.
             *
             * @return True if the artist popup menu entry should be visible, false otherwise.
             */
            private boolean isArtistVisible() {
                final boolean isArtistVisible;
                final String artist = mCurrentSong.getArtistName();

                if (artist != null && !artist.isEmpty()) {
                    isArtistVisible = true;
                } else {
                    isArtistVisible = false;
                }

                return isArtistVisible;
            }

            /**
             * This method checks the dynamic entries for visibility prior to showing the song info
             * popup menu.
             *
             * @param v The view for the song info popup menu.
             */
            @Override
            public void onClick(final View v) {
                if (mCurrentSong != null) {
                    if (mCurrentSong.isStream()) {
                        popupMenuStream.show();
                    } else {
                        // Enable / Disable menu items that need artist and album defined.
                        menu.findItem(POPUP_ALBUM).setVisible(isAlbumVisible());
                        menu.findItem(POPUP_ARTIST).setVisible(isArtistVisible());
                        menu.findItem(POPUP_ALBUM_ARTIST).setVisible(isAlbumArtistVisible());
                        popupMenu.show();
                    }
                }
            }
        });

        return songInfo;
    }

    private float getTrackRating() {
        float rating = 0.0f;

        try {
            rating = (float) mApp.getMPD().getStickerManager().getRating(mCurrentSong);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to get the current track rating.", e);
        }

        return rating / 2.0f;
    }

    /**
     * Generates the volume {@link android.widget.SeekBar}.
     *
     * @param view The view in which to setup the {@code SeekBar} for.
     * @return The constructed SeekBar for the volume modification.
     */
    private SeekBar getVolumeSeekBar(final View view) {
        final SeekBar.OnSeekBarChangeListener seekBarListener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(final SeekBar seekBar, final int progress,
                            final boolean fromUser) {

                    }

                    @Override
                    public void onStartTrackingTouch(final SeekBar seekBar) {
                        mVolTimerTask = new TimerTask() {
                            private int mLastSentVol = -1;

                            private SeekBar mProgress;

                            @Override
                            public void run() {
                                final int progress = mProgress.getProgress();

                                if (mLastSentVol != progress) {
                                    mLastSentVol = progress;
                                    MPDControl.run(MPDControl.ACTION_VOLUME_SET, progress);
                                }
                            }

                            public TimerTask setProgress(final SeekBar prg) {
                                mProgress = prg;
                                return this;
                            }
                        }.setProgress(seekBar);

                        mVolTimer.scheduleAtFixedRate(mVolTimerTask, (long) MPDStatusMap.VOLUME_MIN,
                                (long) MPDStatusMap.VOLUME_MAX);
                    }

                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        mVolTimerTask.cancel();
                        mVolTimerTask.run();
                    }
                };

        final SeekBar volumeSeekBar = (SeekBar) view.findViewById(R.id.progress_volume);
        volumeSeekBar.setOnSeekBarChangeListener(seekBarListener);

        return volumeSeekBar;
    }

    /**
     * This method handles any simple library activity item ids.
     *
     * @param itemId The itemId to attempt to handle.
     * @return {@code true} if this is handled by a simple library activity, {@code false}
     * otherwise.
     */
    private boolean isSimpleLibraryItem(final int itemId) {
        Intent intent = null;

        switch (itemId) {
            case POPUP_ALBUM:
            case POPUP_ALBUM_ARTIST:
            case POPUP_ARTIST:
            case POPUP_FOLDER:
                if (mCurrentSong != null) {
                    intent = simpleLibraryMusicItem(itemId);
                }
                break;
            case POPUP_STREAM:
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra(Stream.EXTRA, true);
                break;
            default:
                break;
        }

        if (intent != null) {
            /**
             * Set the result for SimpleLibraryActivity to
             * return so getCallingActivity() will work.
             */
            startActivityForResult(intent, 1);
        }

        return intent != null;
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        mActivity = (FragmentActivity) activity;

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
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setHasOptionsMenu(false);
        mActivity.setTitle(R.string.nowPlaying);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final Animation fadeIn = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_in);
        final Animation fadeOut = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_out);
        final int viewLayout;
        final View view;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (mApp.isTabletUiEnabled()) {
            viewLayout = R.layout.main_fragment_tablet;
        } else {
            viewLayout = R.layout.main_fragment;
        }

        view = inflater.inflate(viewLayout, container, false);

        mTrackTime = (TextView) view.findViewById(R.id.trackTime);
        mTrackTotalTime = (TextView) view.findViewById(R.id.trackTotalTime);
        mVolumeIcon = (ImageView) view.findViewById(R.id.volume_icon);

        /** These load the TextView resource, and set it as selected. */
        mAlbumNameText = findSelected(view, R.id.albumName);
        mArtistNameText = findSelected(view, R.id.artistName);
        mAudioNameText = findSelected(view, R.id.audioName);
        mSongNameText = findSelected(view, R.id.songName);
        mSongNameText.setText(R.string.notConnected);
        mYearNameText = findSelected(view, R.id.yearName);
        applyViewVisibility(mYearNameText, "enableAlbumYearText");
        mSongRating = (RatingBar) view.findViewById(R.id.songRating);
        mSongRating.setOnRatingBarChangeListener(new RatingChangedHandler());
        mSongRating.setVisibility(View.GONE);

        /** These get the event button, then setup listeners for them. */
        mPlayPauseButton = getEventButton(view, R.id.playpause, true);
        mRepeatButton = getEventButton(view, R.id.repeat, false);
        mShuffleButton = getEventButton(view, R.id.shuffle, false);
        mStopButton = getEventButton(view, R.id.stop, true);
        applyViewVisibility(mStopButton, "enableStopButton");

        /** Same as above, but these don't require a stored field. */
        getEventButton(view, R.id.next, false);
        getEventButton(view, R.id.prev, false);

        /** These have methods to initialize everything required to get them setup. */
        mCoverArt = getCoverArt(view);
        mCoverAsyncHelper = getCoverAsyncHelper(view);
        mSongInfo = getSongInfo(view);
        mTrackSeekBar = getTrackSeekBar(view);
        mVolumeSeekBar = getVolumeSeekBar(view);

        fadeIn.setDuration(ANIMATION_DURATION);
        fadeOut.setDuration(ANIMATION_DURATION);

        Log.i(TAG, "Initialization succeeded");

        return view;
    }

    @Override
    public void onDestroy() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
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
        mActivity = null;
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final AlbumInfo albumInfo;
        boolean result = true;
        final int itemId = item.getItemId();

        switch (item.getItemId()) {
            case POPUP_COVER_BLACKLIST:
                albumInfo = new AlbumInfo(mCurrentSong);
                CoverManager.getInstance().markWrongCover(albumInfo);
                downloadCover(albumInfo);
                updateQueueCovers(albumInfo);
                break;
            case POPUP_COVER_SELECTIVE_CLEAN:
                albumInfo = new AlbumInfo(mCurrentSong);
                CoverManager.getInstance().clear(albumInfo);
                downloadCover(albumInfo);
                updateQueueCovers(albumInfo);
                break;
            case POPUP_CURRENT:
                scrollToNowPlaying();
                break;
            case POPUP_SHARE:
                if (mCurrentSong != null) {
                    try {
                        startActivity(getShareIntent());
                    } catch (final ActivityNotFoundException ignored) {
                        notifyUser(R.string.noSendActionReceiver);
                    }
                }
                break;
            case POPUP_LYRICS:
                if (mCurrentSong != null) {
                    startActivity(getLyricIntent());
                }
                break;
            default:
                result = isSimpleLibraryItem(itemId);
                break;
        }

        return result;
    }

    @Override
    public void onPause() {
        mApp.getMPD().getConnectionStatus().removeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mApp.getMPD().getConnectionStatus().addListener(this);
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
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
                CoverAsyncHelper.setCoverRetrieversFromPreferences();
                break;
            case "enableStopButton":
                applyViewVisibility(mStopButton, key);
                break;
            case "enableAlbumYearText":
                applyViewVisibility(mYearNameText, key);
                break;
            case "enableAudioText":
                mIsAudioNameTextEnabled = sharedPreferences.getBoolean(key, false);
                updateAudioNameText();
                break;
            case "enableRating":
                setStickerVisibility();
                updateTrackInfo(false);
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
        mApp.addStatusChangeListener(this);
        mApp.addTrackPositionListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        mApp.updateTrackInfo.removeCallback(this);
        mApp.removeStatusChangeListener(this);
        mApp.removeTrackPositionListener(this);
        stopPosTimer();
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
    public final void onTrackInfoUpdate(final Music updatedSong, final float trackRating,
            final CharSequence album, final CharSequence artist, final CharSequence date,
            final CharSequence title) {
        mCurrentSong = updatedSong;
        mAlbumNameText.setText(album);
        mArtistNameText.setText(artist);
        mSongNameText.setText(title);
        mSongRating.setRating(trackRating);
        mYearNameText.setText(date);
        updateAudioNameText();
    }

    @Override
    public void playlistChanged(final int oldPlaylistVersion) {
        /**
         * If the current song is a stream, the metadata can change in place, and that will only
         * change the playlist, not the track, so, update if we detect a stream.
         */
        if (mCurrentSong != null && mCurrentSong.isStream() ||
                mMPDStatus.isState(MPDStatusMap.STATE_STOPPED)) {
            updateTrackInfo(false);
        }
    }

    @Override
    public void randomChanged() {
        setButtonAttribute(getShuffleAttribute(mMPDStatus.isRandom()), mShuffleButton);
    }

    @Override
    public void repeatChanged() {
        setButtonAttribute(getRepeatAttribute(mMPDStatus.isRepeat()), mRepeatButton);
    }

    private void scrollToNowPlaying() {
        final QueueFragment queueFragment = (QueueFragment) mActivity.getSupportFragmentManager()
                .findFragmentById(R.id.queue_fragment);

        if (queueFragment == null) {
            Log.w(TAG, "Queue fragment was not available when scrolling to playing track.");
        } else {
            queueFragment.scrollToNowPlaying();
        }
    }

    /**
     * Sets a buttons attributes.
     *
     * @param attribute The attribute resource to set the button to.
     * @param button    The button with which to set the attribute resource.
     */
    private void setButtonAttribute(@AttrRes final int attribute, final ImageButton button) {
        final int[] attrs = {attribute};
        final TypedArray ta = mActivity.obtainStyledAttributes(attrs);
        final Drawable drawableFromTheme = ta.getDrawable(0);

        button.setImageDrawable(drawableFromTheme);
        button.invalidate();
        ta.recycle();
    }

    private void setStickerVisibility() {
        if (mApp.getMPD().getStickerManager().isAvailable()) {
            applyViewVisibility(mSongRating, "enableRating");
        } else {
            mSongRating.setVisibility(View.GONE);
        }
    }

    /**
     * This method handles any simple library activity item ids which handle music items.
     *
     * @param itemId The itemId to attempt to handle.
     * @return An intent to start the {@link com.namelessdev.mpdroid.library.SimpleLibraryActivity}.
     */
    private Intent simpleLibraryMusicItem(final int itemId) {
        Intent intent = new Intent(mActivity, SimpleLibraryActivity.class);

        switch (itemId) {
            case POPUP_ALBUM:
                intent.putExtra(Album.EXTRA, mCurrentSong.getAlbum());
                break;
            case POPUP_ALBUM_ARTIST:
                intent.putExtra(Artist.EXTRA, mCurrentSong.getAlbumArtist());
                break;
            case POPUP_ARTIST:
                intent.putExtra(Artist.EXTRA, mCurrentSong.getArtist());
                break;
            case POPUP_FOLDER:
                final String path = mCurrentSong.getFullPath();
                final String parent = mCurrentSong.getParentDirectory();

                if (path == null || parent == null) {
                    intent = null;
                } else {
                    intent.putExtra(Directory.EXTRA, parent);
                }
                break;
            default:
                break;
        }

        return intent;
    }

    private void startPosTimer(final long start, final long total) {
        stopPosTimer();
        mPosTimer = new Timer();
        final TimerTask posTimerTask = new PosTimerTask(start, total);
        mPosTimer.scheduleAtFixedRate(posTimerTask, 0L, DateUtils.SECOND_IN_MILLIS);
    }

    @Override
    public void stateChanged(final int oldState) {
        if (mActivity != null) {
            updateStatus();
            updateAudioNameText();
        }
    }

    @Override
    public void stickerChanged() {
        if (mSongRating.getVisibility() == View.VISIBLE && mCurrentSong != null) {
            /** This track is not necessarily the track that was changed. */
            final float rating = getTrackRating();
            mSongRating.setRating(rating);
        }
    }

    private void stopPosTimer() {
        if (null != mPosTimer) {
            mPosTimer.cancel();
            mPosTimer = null;
        }
    }

    /**
     * Called when a stored playlist has been modified, renamed, created or deleted.
     */
    @Override
    public void storedPlaylistChanged() {
    }

    /**
     * Toggle the track progress bar. This should be called only when the track changes, for
     * position changes, startPosTimer() is sufficient.
     */
    private void toggleTrackProgress() {
        final long totalTime = mMPDStatus.getTotalTime();

        if (totalTime == MPDStatusMap.DEFAULT_LONG) {
            mSongRating.setVisibility(View.GONE);
            mTrackTime.setVisibility(View.INVISIBLE);
            mTrackTotalTime.setVisibility(View.INVISIBLE);
            stopPosTimer();
            mTrackSeekBar.setProgress(0);
            mTrackSeekBar.setEnabled(false);
        } else {
            final long elapsedTime = mMPDStatus.getElapsedTime();

            if (mMPDStatus.isState(MPDStatusMap.STATE_PLAYING)) {
                startPosTimer(elapsedTime, totalTime);
            } else {
                stopPosTimer();
                updateTrackProgress(elapsedTime, totalTime);
            }

            mTrackSeekBar.setMax((int) totalTime);

            mTrackTime.setVisibility(View.VISIBLE);
            mTrackTotalTime.setVisibility(View.VISIBLE);
            mTrackSeekBar.setEnabled(true);
        }
    }

    /**
     * This enables or disables the volume, depending on the volume given by the server.
     *
     * @param volume The current volume value.
     */
    private void toggleVolumeBar(final int volume) {
        if (volume < MPDStatusMap.VOLUME_MIN || volume > MPDStatusMap.VOLUME_MAX) {
            mVolumeSeekBar.setEnabled(false);
            mVolumeSeekBar.setVisibility(View.GONE);
            mVolumeIcon.setVisibility(View.GONE);
        } else {
            mVolumeSeekBar.setEnabled(true);
            mVolumeSeekBar.setVisibility(View.VISIBLE);
            mVolumeIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void trackChanged(final int oldTrack) {
        updateTrackInfo(false);
    }

    @Override
    public void trackPositionChanged() {
        toggleTrackProgress();
    }

    /**
     * This will update the audioNameText (extra track information) field.
     */
    private void updateAudioNameText() {
        StringBuilder optionalTrackInfo = null;

        if (mCurrentSong != null && mIsAudioNameTextEnabled &&
                !mMPDStatus.isState(MPDStatusMap.STATE_STOPPED)) {

            final char[] separator = {' ', '|', ' '};
            final String fullPath = mCurrentSong.getFullPath();
            final long bitRate = mMPDStatus.getBitrate();
            final int bitsPerSample = mMPDStatus.getBitsPerSample();
            final int sampleRate = mMPDStatus.getSampleRate();
            String fileExtension = null;
            optionalTrackInfo = new StringBuilder(40);

            if (fullPath != null) {
                fileExtension = Tools.getExtension(fullPath);
            }

            /**
             * Check each individual bit of info, the sever can give
             * out empty (and buggy) information from time to time.
             */
            if (fileExtension != null) {
                optionalTrackInfo.append(fileExtension.toUpperCase());
            }

            /** The server can give out buggy (and empty) information from time to time. */
            if (bitRate > 0L) {
                if (optionalTrackInfo.length() > 0) {
                    optionalTrackInfo.append(separator);
                }
                optionalTrackInfo.append(bitRate);
                optionalTrackInfo.append("kbps");
            }

            if (bitsPerSample > 0) {
                if (optionalTrackInfo.length() > 0) {
                    optionalTrackInfo.append(separator);
                }
                optionalTrackInfo.append(bitsPerSample);
                optionalTrackInfo.append("bits");
            }

            if (sampleRate > 1000) {
                if (optionalTrackInfo.length() > 0) {
                    optionalTrackInfo.append(separator);
                }
                optionalTrackInfo.append(Math.abs(sampleRate / 1000.0f));
                optionalTrackInfo.append("kHz");
            }

            if (optionalTrackInfo.length() > 0) {
                mAudioNameText.setText(optionalTrackInfo);
                mAudioNameText.setVisibility(View.VISIBLE);
            }
        }

        if (optionalTrackInfo == null || optionalTrackInfo.length() == 0) {
            mAudioNameText.setVisibility(View.GONE);
        }
    }

    private void updateQueueCovers(final AlbumInfo albumInfo) {
        final QueueFragment queueFragment = (QueueFragment) mActivity.getSupportFragmentManager()
                .findFragmentById(R.id.queue_fragment);

        if (queueFragment == null) {
            Log.w(TAG, "Queue fragment was not available for cover update.");
        } else {
            queueFragment.updateCover(albumInfo);
        }
    }

    private void updateStatus() {
        toggleTrackProgress();

        mPlayPauseButton.setImageResource(getPlayPauseResource(mMPDStatus.getState()));

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

    private void updateTrackInfo(final boolean forcedUpdate) {
        if (mApp.getMPD().isConnected() && isAdded()) {
            toggleTrackProgress();
            mApp.updateTrackInfo.refresh(forcedUpdate);
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
        final long elapsedTime;

        if (elapsed > totalTrackTime) {
            elapsedTime = totalTrackTime;
        } else {
            elapsedTime = elapsed;
        }

        mTrackSeekBar.setProgress((int) elapsedTime);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTrackTime.setText(Tools.timeToString(elapsedTime));
                mTrackTotalTime.setText(Tools.timeToString(totalTrackTime));
            }
        });
    }

    @Override
    public void volumeChanged(final int oldVolume) {
        final int volume = mMPDStatus.getVolume();

        toggleVolumeBar(volume);
        mVolumeSeekBar.setProgress(volume);
    }

    private static class ButtonEventHandler implements OnClickListener, View.OnLongClickListener {

        @Override
        public void onClick(final View v) {
            MPDControl.run(v.getId());
        }

        @Override
        public boolean onLongClick(final View v) {
            final boolean isConsumed;
            final MPDApplication app = MPDApplication.getInstance();
            final MPDStatus mpdStatus = app.getMPD().getStatus();

            if (v.getId() == R.id.playpause && !mpdStatus.isState(MPDStatusMap.STATE_STOPPED)) {
                MPDControl.run(MPDControl.ACTION_STOP);
                isConsumed = true;
            } else {
                isConsumed = false;
            }

            return isConsumed;
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
            mStartTrackTime = start;
            mTotalTrackTime = total;
            mTimerStartTime = new Date().getTime();
        }

        @Override
        public void run() {
            final long elapsedSinceTimerStart = new Date().getTime() - mTimerStartTime;

            mElapsedTime = mStartTrackTime + elapsedSinceTimerStart / DateUtils.SECOND_IN_MILLIS;

            updateTrackProgress(mElapsedTime, mTotalTrackTime);
        }
    }

    private class RatingChangedHandler implements RatingBar.OnRatingBarChangeListener {

        @Override
        public void onRatingChanged(final RatingBar ratingBar, final float rating,
                final boolean fromUser) {
            final int trackRating = (int) rating * 2;
            if (fromUser && mCurrentSong != null) {
                try {
                    mApp.getMPD().getStickerManager().setRating(mCurrentSong, trackRating);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to set the rating.", e);
                }
                Log.d(TAG, "Rating changed to " + rating);
            }
        }
    }
}
