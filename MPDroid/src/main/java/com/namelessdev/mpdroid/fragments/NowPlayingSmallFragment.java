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
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverDownloadListener;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.item.Music;

import android.app.Activity;
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

public class NowPlayingSmallFragment extends Fragment implements StatusChangeListener,
        UpdateTrackInfo.TrackInfoUpdate {

    private static final String TAG = "NowPlayingSmallFragment";

    final OnClickListener mButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            MPDControl.run(v.getId());
        }
    };

    private final MPDApplication mApp = MPDApplication.getInstance();

    private ImageButton mButtonPlayPause;

    private ImageView mCoverArt;

    private CoverAsyncHelper mCoverHelper;

    private boolean mForceStatusUpdate = false;

    private TextView mSongArtist;

    private TextView mSongTitle;

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        if (connected && isAdded() && mForceStatusUpdate) {
            mApp.updateTrackInfo.refresh(mApp.oMPDAsyncHelper.oMPD.getStatus(), true);
        }

        if (!connected && isAdded()) {
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

        if (!MainMenuActivity.class.equals(activity.getClass())) {
            mForceStatusUpdate = true;
        }
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
        mApp.updateTrackInfo.removeCallback(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        final MPDStatus mpdStatus = mApp.oMPDAsyncHelper.oMPD.getStatus();

        if (mApp.updateTrackInfo == null) {
            mApp.updateTrackInfo = new UpdateTrackInfo();
        }
        mApp.updateTrackInfo.addCallback(this);

        if (mForceStatusUpdate && mApp.oMPDAsyncHelper.oMPD.isConnected()) {
            mApp.updateTrackInfo.refresh(mpdStatus, true);
        }

        /** mpdStatus might be null here, no problem it'll be generated in the method. */
        updatePlayPauseButton(mpdStatus);
    }

    @Override
    public void onStart() {
        super.onStart();
        mApp.oMPDAsyncHelper.addStatusChangeListener(this);
    }

    @Override
    public void onStop() {
        mApp.oMPDAsyncHelper.removeStatusChangeListener(this);
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

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /**
         * If the current song is a stream, the metadata can change in place, and that will only
         * change the playlist, not the track, so, update if we detect a stream.
         */
        if (isAdded() && mForceStatusUpdate) {
            final int songPos = mpdStatus.getSongPos();
            final Music currentSong =
                    mApp.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
            if (currentSong != null && currentSong.isStream() ||
                    mpdStatus.isState(MPDStatus.STATE_STOPPED)) {
                mApp.updateTrackInfo.refresh(mpdStatus, true);
            }
        }
    }

    @Override
    public void randomChanged(final boolean random) {

    }

    @Override
    public void repeatChanged(final boolean repeating) {
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
        if (mForceStatusUpdate) {
            mApp.updateTrackInfo.refresh(mpdStatus);
        }
        updatePlayPauseButton(mpdStatus);

    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        if (mForceStatusUpdate) {
            mApp.updateTrackInfo.refresh(mpdStatus);
        }
    }

    public void updateCover(final AlbumInfo albumInfo) {
        if (mCoverArt != null && null != mCoverArt.getTag()
                && mCoverArt.getTag().equals(albumInfo.getKey())) {
            mCoverHelper.downloadCover(albumInfo);
        }
    }

    private void updatePlayPauseButton(final MPDStatus status) {
        if (isAdded()) {
            final int playPauseResource =
                    NowPlayingFragment.getPlayPauseResource(status.getState());

            mButtonPlayPause.setImageResource(playPauseResource);
        }
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
    }
}