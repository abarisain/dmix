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

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.connection.MPDConnectionListener;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;
import com.anpmech.mpd.subsystem.status.StatusChangeListener;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.NowPlayingActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.cover.CoverAsyncHelper;
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;
import com.namelessdev.mpdroid.cover.CoverDownloadListener;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class NowPlayingSmallFragment extends Fragment implements
        MPDConnectionListener, StatusChangeListener,
        UpdateTrackInfo.TrackInfoUpdate {

    private static final String TAG = "NowPlayingSmallFragment";

    final OnClickListener mButtonClickListener = new OnClickControlListener();

    private final MPDApplication mApp = MPDApplication.getInstance();

    private final MPDStatus mMPDStatus = mApp.getMPD().getStatus();

    private ImageButton mButtonPlayPause;

    private ImageView mCoverArt;

    private CoverAsyncHelper mCoverHelper;

    private TextView mSongArtist;

    private TextView mSongTitle;

    /**
     * Called upon connection.
     *
     * @param commandErrorCode If this number is non-zero, the number will correspond to a
     *                         {@link MPDException} error code. If this number is zero, the
     *                         connection MPD protocol commands were successful.
     */
    @Override
    public void connectionConnected(final int commandErrorCode) {
        if (isAdded()) {
            mApp.updateTrackInfo.refresh(true);
        }
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
        if (isAdded()) {
            mSongTitle.setText(R.string.notConnected);
            mSongArtist.setText("");
        }
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
    }

    /**
     * This is called when cover art needs to be updated due to server information change.
     *
     * @param albumInfo The current albumInfo
     */
    @Override
    public final void onCoverUpdate(final AlbumInfo albumInfo) {
        final int noCoverResource = AlbumCoverDownloadListener.getNoCoverResource();
        mCoverArt.setImageResource(noCoverResource);

        if (albumInfo != null) {
            mCoverHelper.downloadCover(albumInfo, true);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.now_playing_small_fragment, container, false);
        mSongTitle = (TextView) view.findViewById(R.id.song_title);
        mSongTitle.setSelected(true);
        mSongArtist = (TextView) view.findViewById(R.id.song_artist);
        mSongArtist.setSelected(true);
        final ImageButton buttonPrev = (ImageButton) view.findViewById(R.id.prev);
        mButtonPlayPause = (ImageButton) view.findViewById(R.id.playpause);
        final ImageButton buttonNext = (ImageButton) view.findViewById(R.id.next);
        buttonPrev.setOnClickListener(mButtonClickListener);
        mButtonPlayPause.setOnClickListener(mButtonClickListener);
        buttonNext.setOnClickListener(mButtonClickListener);

        mCoverArt = (ImageView) view.findViewById(R.id.albumCover);
        final ProgressBar coverArtProgress = (ProgressBar) view
                .findViewById(R.id.albumCoverProgress);
        final CoverDownloadListener coverArtListener = new AlbumCoverDownloadListener(
                mCoverArt, coverArtProgress, false);
        mCoverHelper = new CoverAsyncHelper();
        mCoverHelper.setCoverMaxSizeFromScreen(getActivity());
        final ViewTreeObserver vto = mCoverArt.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mCoverHelper != null) {
                    mCoverHelper.setCachedCoverMaxSize(mCoverArt.getMeasuredHeight());
                }
                return true;
            }
        });
        mCoverHelper.addCoverDownloadListener(coverArtListener);

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Activity activity = getActivity();

                activity.startActivity(new Intent(activity, NowPlayingActivity.class));
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        if (mCoverArt != null) {
            final Drawable oldDrawable = mCoverArt.getDrawable();
            mCoverArt.setImageResource(AlbumCoverDownloadListener.getNoCoverResource());
            if (oldDrawable != null && oldDrawable instanceof CoverBitmapDrawable) {
                final Bitmap oldBitmap = ((BitmapDrawable) oldDrawable).getBitmap();
                if (oldBitmap != null) {
                    oldBitmap.recycle();
                }
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        mApp.getMPD().getConnectionStatus().removeListener(this);
        mApp.updateTrackInfo.removeCallback(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mApp.getMPD().getConnectionStatus().addListener(this);

        if (mApp.updateTrackInfo == null) {
            mApp.updateTrackInfo = new UpdateTrackInfo();
        }
        mApp.updateTrackInfo.addCallback(this);

        if (mApp.getMPD().isConnected()) {
            mApp.updateTrackInfo.refresh(true);
        }

        /** mpdStatus might be null here, no problem it'll be generated in the method. */
        updatePlayPauseButton();
    }

    @Override
    public void onStart() {
        super.onStart();
        mApp.addStatusChangeListener(this);
    }

    @Override
    public void onStop() {
        mApp.removeStatusChangeListener(this);
        super.onStop();
    }

    /**
     * Called when a track information change has been detected.
     *
     * @param artist The artist change.
     * @param title  The title change.
     */
    @Override
    public final void onTrackInfoUpdate(final CharSequence artist, final CharSequence title) {
        mSongArtist.setText(artist);
        mSongTitle.setText(title);
    }

    /**
     * Called upon a change in the Output idle subsystem.
     */
    @Override
    public void outputsChanged() {
    }

    @Override
    public void playlistChanged(final int oldPlaylistVersion) {
        /**
         * If the current song is a stream, the metadata can change in place, and that will only
         * change the playlist, not the track, so, update if we detect a stream.
         */
        if (isAdded()) {
            final MPD mpd = mApp.getMPD();
            final MPDStatus mpdStatus = mpd.getStatus();
            final Music currentSong = mpd.getCurrentTrack();

            if (currentSong != null && currentSong.isStream() ||
                    mpdStatus.isState(MPDStatusMap.STATE_STOPPED)) {
                mApp.updateTrackInfo.refresh(true);
            }
        }
    }

    @Override
    public void randomChanged() {

    }

    @Override
    public void repeatChanged() {
    }

    @Override
    public void stateChanged(final int oldState) {
        mApp.updateTrackInfo.refresh();
        updatePlayPauseButton();

    }

    @Override
    public void stickerChanged() {
    }

    /**
     * Called when a stored playlist has been modified, renamed, created or deleted.
     */
    @Override
    public void storedPlaylistChanged() {
    }

    @Override
    public void trackChanged(final int oldTrack) {
        mApp.updateTrackInfo.refresh();
    }

    public void updateCover(final AlbumInfo albumInfo) {
        if (mCoverArt != null && null != mCoverArt.getTag()
                && mCoverArt.getTag().equals(albumInfo.getKey())) {
            mCoverHelper.downloadCover(albumInfo);
        }
    }

    private void updatePlayPauseButton() {
        if (isAdded()) {
            final int playPauseResource =
                    NowPlayingFragment.getPlayPauseResource(mMPDStatus.getState());

            mButtonPlayPause.setImageResource(playPauseResource);
        }
    }

    @Override
    public void volumeChanged(final int oldVolume) {
    }

    static class OnClickControlListener implements OnClickListener {

        protected static void runCommand(final int command) {
            final MPDApplication app = MPDApplication.getInstance();

            if (app.getMPD().getStatus().isValid()) {
                MPDControl.run(command);
            } else {
                final Object token = MPDControl.setupConnection(5L, TimeUnit.SECONDS);

                if (token != null) {
                    MPDControl.run(command);
                    app.removeConnectionLock(token);
                }
            }
        }

        @Override
        public void onClick(final View v) {
            runCommand(v.getId());
        }
    }
}