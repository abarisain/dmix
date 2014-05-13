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
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
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

    private boolean lightTheme;

    final OnClickListener buttonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.prev:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                app.oMPDAsyncHelper.oMPD.previous();
                            } catch (MPDServerException e) {
                                Log.w(MPDApplication.TAG, e.getMessage());
                            }
                        }
                    }).start();
                    break;
                case R.id.playpause:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final MPD mpd = app.oMPDAsyncHelper.oMPD;
                                final String state = mpd.getStatus().getState();
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
                case R.id.next:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                app.oMPDAsyncHelper.oMPD.next();
                            } catch (MPDServerException e) {
                                Log.w(MPDApplication.TAG, e.getMessage());
                            }
                        }
                    }).start();
                    break;
            }
        }
    };

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
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
        lightTheme = app.isLightThemeSelected();
    }

    /**
     * This is called when cover art needs to be updated due to server information change.
     *
     * @param albumInfo The current albumInfo
     */
    @Override
    public final void onCoverUpdate(final AlbumInfo albumInfo) {
        if (lightTheme) {
            coverArt.setImageResource(R.drawable.no_cover_art_light_big);
        } else {
            coverArt.setImageResource(R.drawable.no_cover_art_big);
        }

        if(albumInfo != null) {
            coverHelper.downloadCover(albumInfo);
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
        coverArtListener = new AlbumCoverDownloadListener(getActivity(), coverArt,
                coverArtProgress, app.isLightThemeSelected(), false);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

        coverHelper = new CoverAsyncHelper(settings);
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
            coverArt.setImageResource(lightTheme ? R.drawable.no_cover_art_light
                    : R.drawable.no_cover_art);
            if (oldDrawable != null && oldDrawable instanceof CoverBitmapDrawable) {
                final Bitmap oldBitmap = ((CoverBitmapDrawable) oldDrawable).getBitmap();
                if (oldBitmap != null)
                    oldBitmap.recycle();
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        updatePlayPauseButton(null);
    }

    @Override
    public void onStart() {
        super.onStart();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.updateTrackInfo.addCallback(this);
    }

    @Override
    public void onStop() {
        app.updateTrackInfo.removeCallback(this);
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
    }

    @Override
    public void randomChanged(boolean random) {

    }

    @Override
    public void repeatChanged(boolean repeating) {
    }

    @Override
    public void stateChanged(MPDStatus status, String oldState) {
        if (isAdded()) {
            updatePlayPauseButton(status);
        }
    }

   @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
    }

    public void updateCover(AlbumInfo albumInfo) {
        if (coverArt != null && null != coverArt.getTag()
                && coverArt.getTag().equals(albumInfo.getKey())) {
            coverHelper.downloadCover(albumInfo);
        }
    }

    private void updatePlayPauseButton(final MPDStatus status) {
        String state = null;

        if(status == null) {
            try {
                state = app.oMPDAsyncHelper.oMPD.getStatus().getState();
            } catch (final MPDServerException e) {
                Log.e(TAG, "Failed to retrieve server status.", e);
            }
        } else {
            state = status.getState();
        }

        buttonPlayPause.setImageResource(NowPlayingFragment.getPlayPauseResource(state));
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }
}