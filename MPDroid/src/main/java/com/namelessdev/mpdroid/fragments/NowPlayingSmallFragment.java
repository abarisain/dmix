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
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    private boolean forceStatusUpdate = false;

    private static final String TAG = "com.namelessdev.mpdroid.NowPlayingSmallFragment";

    private final MPDApplication app = MPDApplication.getInstance();
    private CoverAsyncHelper coverHelper;
    private TextView songTitle;

    private TextView songArtist;
    private AlbumCoverDownloadListener coverArtListener;
    private ImageView coverArt;

    private ProgressBar coverArtProgress;
    private ImageButton buttonPrev;
    private ImageButton buttonPlayPause;
    private ImageButton buttonNext;

    final OnClickListener buttonClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            MPDControl.run(v.getId());
        }
    };

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        if (connected && isAdded() && forceStatusUpdate) {
            app.updateTrackInfo.refresh(app.oMPDAsyncHelper.oMPD.getStatus(), true);
        }

        if (!connected && isAdded()) {
            songTitle.setText(R.string.notConnected);
            songArtist.setText("");
        }
    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!MainMenuActivity.class.equals(activity.getClass())) {
            forceStatusUpdate = true;
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
        coverArt.setImageResource(noCoverResource);

        if(albumInfo != null) {
            coverHelper.downloadCover(albumInfo, true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.now_playing_small_fragment, container, false);
        songTitle = (TextView) view.findViewById(R.id.song_title);
        songTitle.setSelected(true);
        songArtist = (TextView) view.findViewById(R.id.song_artist);
        songArtist.setSelected(true);
        buttonPrev = (ImageButton) view.findViewById(R.id.prev);
        buttonPlayPause = (ImageButton) view.findViewById(R.id.playpause);
        buttonNext = (ImageButton) view.findViewById(R.id.next);
        buttonPrev.setOnClickListener(buttonClickListener);
        buttonPlayPause.setOnClickListener(buttonClickListener);
        buttonNext.setOnClickListener(buttonClickListener);

        coverArt = (ImageView) view.findViewById(R.id.albumCover);
        coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        coverArtListener = new AlbumCoverDownloadListener(coverArt, coverArtProgress, false);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

        coverHelper = new CoverAsyncHelper();
        coverHelper.setCoverMaxSizeFromScreen(getActivity());
        final ViewTreeObserver vto = coverArt.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if (coverHelper != null)
                    coverHelper.setCachedCoverMaxSize(coverArt.getMeasuredHeight());
                return true;
            }
        });
        coverHelper.addCoverDownloadListener(coverArtListener);

        return view;
    }

    @Override
    public void onDestroyView() {
        if (coverArt != null) {
            final Drawable oldDrawable = coverArt.getDrawable();
            coverArt.setImageResource(AlbumCoverDownloadListener.getNoCoverResource());
            if (oldDrawable != null && oldDrawable instanceof CoverBitmapDrawable) {
                final Bitmap oldBitmap = ((CoverBitmapDrawable) oldDrawable).getBitmap();
                if (oldBitmap != null)
                    oldBitmap.recycle();
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        app.updateTrackInfo.removeCallback(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        final MPDStatus mpdStatus = app.oMPDAsyncHelper.oMPD.getStatus();

        if (app.updateTrackInfo == null) {
            app.updateTrackInfo = new UpdateTrackInfo();
        }
        app.updateTrackInfo.addCallback(this);

        if(forceStatusUpdate && app.oMPDAsyncHelper.oMPD.isConnected()) {
            app.updateTrackInfo.refresh(mpdStatus, true);
        }

        /** mpdStatus might be null here, no problem it'll be generated in the method. */
        updatePlayPauseButton(mpdStatus);
    }

    @Override
    public void onStart() {
        super.onStart();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
    }

    @Override
    public void onStop() {
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        super.onStop();
    }

    /**
     * Called when a track information change has been detected.
     *
     * @param artist The artist change.
     * @param title The title change.
     */
    @Override
    public final void onTrackInfoUpdate(final CharSequence artist, final CharSequence title) {
        songArtist.setText(artist);
        songTitle.setText(title);
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /**
         * If the current song is a stream, the metadata can change in place, and that will only
         * change the playlist, not the track, so, update if we detect a stream.
         */
        if (isAdded() && forceStatusUpdate) {
            final int songPos = mpdStatus.getSongPos();
            final Music currentSong =
                    app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
            if (currentSong != null && currentSong.isStream() ||
                    mpdStatus.isState(MPDStatus.STATE_STOPPED)) {
                app.updateTrackInfo.refresh(mpdStatus, true);
            }
        }
    }

    @Override
    public void randomChanged(boolean random) {

    }

    @Override
    public void repeatChanged(boolean repeating) {
    }

    @Override
    public void stateChanged(MPDStatus status, int oldState) {
        if (forceStatusUpdate) {
            app.updateTrackInfo.refresh(status);
        }
        updatePlayPauseButton(status);

    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        if (forceStatusUpdate) {
            app.updateTrackInfo.refresh(mpdStatus);
        }
    }

    public void updateCover(AlbumInfo albumInfo) {
        if (coverArt != null && null != coverArt.getTag()
                && coverArt.getTag().equals(albumInfo.getKey())) {
            coverHelper.downloadCover(albumInfo);
        }
    }

    private void updatePlayPauseButton(final MPDStatus status) {
        if (isAdded()) {
            final int playPauseResource =
                    NowPlayingFragment.getPlayPauseResource(status.getState());

            buttonPlayPause.setImageResource(playPauseResource);
        }
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }
}