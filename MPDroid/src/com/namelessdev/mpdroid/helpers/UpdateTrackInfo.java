/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This sets up an AsyncTask to gather and parse all the information to update the
 * track information outside of the UI thread, then sends a callback to the resource
 * listeners.
 */
public class UpdateTrackInfo {

    public interface FullTrackInfoUpdate {

        /**
         * This is called when cover art needs to be updated due to server information change.
         *
         * @param albumInfo The current albumInfo
         */
        void onCoverUpdate(AlbumInfo albumInfo);

        /**
         * Called when a track information change has been detected.
         *
         * @param updatedSong The currentSong item object.
         * @param album       The album change.
         * @param artist      The artist change.
         * @param date        The date change.
         * @param title       The title change.
         */
        void onTrackInfoUpdate(Music updatedSong, CharSequence album, CharSequence artist,
                CharSequence date, CharSequence title);
    }

    public interface TrackInfoUpdate {

        /**
         * This is called when cover art needs to be updated due to server information change.
         *
         * @param albumInfo The current albumInfo
         */
        void onCoverUpdate(AlbumInfo albumInfo);

        /**
         * Called when a track information change has been detected.
         *
         * @param artist The artist change.
         * @param title  The title change.
         */
        void onTrackInfoUpdate(CharSequence artist, CharSequence title);
    }

    private final MPDApplication app = MPDApplication.getInstance();

    private static final boolean DEBUG = false;

    private boolean forceCoverUpdate = false;

    private UpdateTrackInfo.FullTrackInfoUpdate fullTrackInfoListener = null;

    private String lastAlbum = null;

    private String lastArtist = null;

    private final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

    private UpdateTrackInfo.TrackInfoUpdate trackInfoListener = null;

    private static final String TAG = "com.namelessdev.mpdroid.UpdateTrackInfo";

    public final void addCallback(final UpdateTrackInfo.FullTrackInfoUpdate listener) {
        fullTrackInfoListener = listener;
    }

    public final void refresh(final MPDStatus mpdStatus, final boolean forceCoverUpdate) {
        this.forceCoverUpdate = forceCoverUpdate;
        new UpdateTrackInfoAsync().execute(mpdStatus);
    }

    public final void refresh(final MPDStatus mpdStatus) {
        new UpdateTrackInfoAsync().execute(mpdStatus);
    }

    public final void removeCallback(final UpdateTrackInfo.FullTrackInfoUpdate ignored) {
        fullTrackInfoListener = null;
    }

    public final void removeCallback(final UpdateTrackInfo.TrackInfoUpdate ignored) {
        trackInfoListener = null;
    }

    public final void addCallback(final UpdateTrackInfo.TrackInfoUpdate listener) {
        trackInfoListener = listener;
    }

    private class UpdateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Void> {

        private String album = null;

        private AlbumInfo albumInfo = null;

        private String artist = null;

        private Music currentSong = null;

        private String date = null;

        private boolean hasCoverChanged = false;

        private String title = null;

        /**
         * Gather and parse all song track information necessary after change.
         *
         * @param mpdStatuses A {@code MPDStatus} object array.
         * @return A null {@code Void} object, ignore it.
         */
        @Override
        protected final Void doInBackground(final MPDStatus... mpdStatuses) {
            final int songPos = mpdStatuses[0].getSongPos();
            currentSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);

            if (currentSong != null) {
                if (currentSong.isStream()) {
                    if (currentSong.haveTitle()) {
                        album = currentSong.getName();
                        title = currentSong.getTitle();
                    } else {
                        title = currentSong.getName();
                    }

                    artist = currentSong.getArtist();
                    albumInfo = new AlbumInfo(artist, album);
                } else {
                    album = currentSong.getAlbum();

                    date = Long.toString(currentSong.getDate());
                    if (date.isEmpty() || date.charAt(0) == '-') {
                        date = "";
                    } else {
                        date = " - " + date;
                    }

                    title = currentSong.getTitle();
                    addDiscAndTrackNumber();
                    setArtist();
                    albumInfo = currentSong.getAlbumInfo();
                }
                hasCoverChanged = hasCoverChanged();

                if (DEBUG) {
                    Log.i(TAG,
                            "album: " + album + " artist: " + artist + " date: " + date
                                    + " albumInfo: " + albumInfo + " hasTrackChanged: " +
                                    hasCoverChanged
                    );
                }
            }

            lastAlbum = album;
            lastArtist = artist;

            return (Void) null;
        }

        /**
         * Send out the messages to listeners.
         */
        @Override
        protected final void onPostExecute(final Void result) {
            super.onPostExecute(result);

            final boolean sendCoverUpdate = hasCoverChanged || currentSong == null
                    || forceCoverUpdate;

            if (currentSong == null) {
                title = app.getResources().getString(R.string.noSongInfo);
            }

            if (fullTrackInfoListener != null) {
                fullTrackInfoListener
                        .onTrackInfoUpdate(currentSong, album, artist, date, title);

                if (sendCoverUpdate) {
                    fullTrackInfoListener.onCoverUpdate(albumInfo);
                }
            }

            if (trackInfoListener != null) {
                trackInfoListener.onTrackInfoUpdate(album, title);

                if (sendCoverUpdate) {
                    trackInfoListener.onCoverUpdate(albumInfo);
                }
            }
        }

        private boolean hasCoverChanged() {
            final boolean invalid = artist == null || album == null;
            return invalid || !artist.equals(lastArtist) || !album.equals(lastAlbum);
        }

        /**
         * If not a stream, this sets up the artist based on artist and album artist information.
         */
        private void setArtist() {
            final boolean showAlbumArtist = settings.getBoolean("showAlbumArtist", true);
            final String albumArtist = currentSong.getAlbumArtist();

            artist = currentSong.getArtist();
            if (artist.isEmpty()) {
                artist = albumArtist;
            } else if (showAlbumArtist && albumArtist != null &&
                    !artist.toLowerCase().contains(albumArtist.toLowerCase())) {
                artist = albumArtist + " / " + artist;
            }
        }

        private void addDiscAndTrackNumber() {
            final int tracknum = currentSong.getTrack();
            final int discnum  = currentSong.getDisc();
            if (tracknum > - 1) {
                title = tracknum+"] " + title;
                if (discnum > - 1) {
                    title = discnum+"/" + title;
                }
                title = "[" + title;
            }
        }
    }
}
