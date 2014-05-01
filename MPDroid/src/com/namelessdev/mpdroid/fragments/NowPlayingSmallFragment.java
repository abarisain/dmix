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
import com.namelessdev.mpdroid.helpers.CoverInfo;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

public class NowPlayingSmallFragment extends Fragment implements StatusChangeListener {

    public class updateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Boolean> {
        MPDStatus status = null;

        @Override
        protected Boolean doInBackground(MPDStatus... params) {
            Boolean result = false;

            if (params == null) {
                try {
                    // A recursive call doesn't seem that bad here.
                    result = doInBackground(app.oMPDAsyncHelper.oMPD.getStatus());
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
            if (result != null && result) {
                String albumartist;
                String artist = null;
                String artistlabel = null;
                String title;
                String album = null;
                boolean noSong = actSong == null || status.getPlaylistLength() == 0;
                if (noSong) {
                    title = getResources().getString(R.string.noSongInfo);
                } else {
                    if (actSong.isStream()) {
                        if (actSong.haveTitle()) {
                            title = actSong.getTitle();
                            artist = actSong.getName();
                        } else {
                            title = actSong.getName();
                            artist = "";
                        }
                    } else {
                        albumartist = actSong.getAlbumArtist();
                        artist = actSong.getArtist();
                        if (!showAlbumArtist ||
                                albumartist == null || "".equals(albumartist) ||
                                artist.toLowerCase().contains(albumartist.toLowerCase()))
                            artistlabel = "" + artist;
                        else if ("".equals(artist))
                            artistlabel = "" + albumartist;
                        else {
                            artistlabel = albumartist + " / " + artist;
                            artist = albumartist;
                        }
                        title = actSong.getTitle();
                        album = actSong.getAlbum();
                    }
                }

                artist = artist == null ? "" : artist;
                title = title == null ? "" : title;
                album = album == null ? "" : album;
                artistlabel = artistlabel == null || artistlabel.equals("null") ? "" : artistlabel;

                songArtist.setText(artistlabel);
                songTitle.setText(title);
                if (noSong || actSong.isStream()) {
                    lastArtist = artist;
                    lastAlbum = album;
                    coverArtListener.onCoverNotFound(new CoverInfo(artist, album));
                    coverHelper.downloadCover(new AlbumInfo(artist, album));
                } else if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) {
                    final int noCoverDrawable = lightTheme ? R.drawable.no_cover_art_light_big
                            : R.drawable.no_cover_art_big;
                    coverArt.setImageResource(noCoverDrawable);
                    coverHelper.downloadCover(actSong.getAlbumInfo());
                    lastArtist = artist;
                    lastAlbum = album;
                }
                stateChanged(status, null);
            } else {
                songArtist.setText("");
                songTitle.setText(R.string.noSongInfo);
            }
        }
    }

    private MPDApplication app;
    private CoverAsyncHelper coverHelper;
    private TextView songTitle;

    private TextView songArtist;
    private AlbumCoverDownloadListener coverArtListener;
    private ImageView coverArt;

    private ProgressBar coverArtProgress;
    private ImageButton buttonPrev;
    private ImageButton buttonPlayPause;
    private ImageButton buttonNext;
    private String lastArtist = "";
    private String lastAlbum = "";
    private boolean showAlbumArtist;

    private Music actSong = null;

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
        if (connected && isAdded()) {
            new updateTrackInfoAsync().execute((MPDStatus[]) null);
            if (songTitle != null && songArtist != null) {
                songTitle.setText(getResources().getString(R.string.noSongInfo));
                songArtist.setText("");
            }
        } else {
            songTitle.setText(getResources().getString(R.string.notConnected));
            songArtist.setText("");
        }
    }

    @Override
    public void libraryStateChanged(boolean updating) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        app = (MPDApplication) activity.getApplication();
        lightTheme = app.isLightThemeSelected();
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

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        showAlbumArtist = settings.getBoolean("showAlbumArtist", true);

        coverHelper = new CoverAsyncHelper(app, settings);
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
        if(app.oMPDAsyncHelper.oMPD.isConnected()) {
            new updateTrackInfoAsync().execute((MPDStatus[]) null);
        }
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

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        if (!isDetached() && actSong != null && actSong.isStream() &&
                app.oMPDAsyncHelper.oMPD.isConnected()) {
            /**
             * If the current song is a stream, the metadata can change in place, and that will only
             * change the playlist, not the track, so, update if we detect a stream.
             */
            new updateTrackInfoAsync().execute(mpdStatus);
        }
    }

    @Override
    public void randomChanged(boolean random) {

    }

    @Override
    public void repeatChanged(boolean repeating) {
    }

    @Override
    public void stateChanged(MPDStatus status, String oldState) {
        if (!isAdded())
            return;
        app.getApplicationState().currentMpdStatus = status;
        if (status.getState() != null && buttonPlayPause != null) {
            if (status.getState().equals(MPDStatus.MPD_STATE_PLAYING)) {
                buttonPlayPause.setImageDrawable(getResources().getDrawable(
                        lightTheme ? R.drawable.ic_media_pause_light : R.drawable.ic_media_pause));
            } else {
                buttonPlayPause.setImageDrawable(getResources().getDrawable(
                        lightTheme ? R.drawable.ic_media_play_light : R.drawable.ic_media_play));
            }
        }
    }

   @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        if(app.oMPDAsyncHelper.oMPD.isConnected()) {
            new updateTrackInfoAsync().execute(mpdStatus);
        }
    }

    public void updateCover(AlbumInfo albumInfo) {
        if (coverArt != null && null != coverArt.getTag()
                && coverArt.getTag().equals(albumInfo.getKey())) {
            coverHelper.downloadCover(albumInfo);
        }
    }

    /**
     * ************************** Stuff we don't care about *
     * **************************
     */

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }

}
