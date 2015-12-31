/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.FilesystemTreeEntry;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.subsystem.Sticker;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * This sets up an AsyncTask to gather and parse all the information to update the track
 * information outside of the UI thread, then sends a callback to the resource listeners.
 */
public class UpdateTrackInfo {

    private static final boolean DEBUG = false;

    private static final String TAG = "UpdateTrackInfo";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private final MPDStatus mMPDStatus = mApp.getMPD().getStatus();

    private final SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(mApp);

    private final Sticker mSticker;

    private boolean mForceCoverUpdate = false;

    private FullTrackInfoUpdate mFullTrackInfoListener = null;

    private String mLastAlbum = null;

    private String mLastArtist = null;

    private TrackInfoUpdate mTrackInfoListener = null;

    public UpdateTrackInfo() {
        mSticker = mApp.getMPD().getStickerManager();
    }

    public final void addCallback(final FullTrackInfoUpdate listener) {
        mFullTrackInfoListener = listener;
    }

    public final void addCallback(final TrackInfoUpdate listener) {
        mTrackInfoListener = listener;
    }

    public final void refresh() {
        refresh(false);
    }

    public final void refresh(final boolean forceCoverUpdate) {
        mForceCoverUpdate = forceCoverUpdate;
        new UpdateTrackInfoAsync().execute();
    }

    public final void removeCallback(final TrackInfoUpdate ignored) {
        mTrackInfoListener = null;
    }

    public final void removeCallback(final FullTrackInfoUpdate ignored) {
        mFullTrackInfoListener = null;
    }

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
         * @param trackRating The current song rating.
         * @param album       The album change.
         * @param artist      The artist change.
         * @param date        The date change.
         * @param title       The title change.
         */
        void onTrackInfoUpdate(Music updatedSong, final float trackRating, CharSequence album,
                CharSequence artist, CharSequence date, CharSequence title);
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

    private class UpdateTrackInfoAsync extends AsyncTask<Void, Music, Music> {

        private AlbumInfo mAlbumInfo = null;

        private String mAlbumName = null;

        private String mArtistName = null;

        private String mDate = null;

        private boolean mHasCoverChanged = false;

        private String mTitle = null;

        private float mTrackRating;

        /**
         * Gather and parse all song track information necessary after change.
         *
         * @param params A {@code MPDStatus} object array.
         * @return A null {@code Void} object, ignore it.
         */
        @Override
        protected final Music doInBackground(final Void... params) {
            try {
                mMPDStatus.waitForValidity();
                mApp.getMPD().getPlaylist().waitForValidity();
            } catch (final InterruptedException ignored) {
            }

            final Music currentTrack = mApp.getMPD().getCurrentTrack();

            if (currentTrack != null) {
                if (currentTrack.isStream()) {
                    final String title = currentTrack.getTitle();

                    if (title != null && !title.isEmpty()) {
                        mAlbumName = currentTrack.getName();
                        mTitle = currentTrack.getTitle();
                    } else {
                        mTitle = currentTrack.getName();
                    }

                    mArtistName = currentTrack.getArtistName();
                    mAlbumInfo = new AlbumInfo(mArtistName, mAlbumName);
                } else {
                    mAlbumName = currentTrack.getAlbumName();

                    mDate = Long.toString(currentTrack.getDate());
                    if (mDate.isEmpty() || mDate.charAt(0) == '-') {
                        mDate = "";
                    } else {
                        mDate = " - " + mDate;
                    }

                    mTitle = currentTrack.getTitle();
                    setArtist(currentTrack);
                    mAlbumInfo = new AlbumInfo(currentTrack);
                }
                mHasCoverChanged = hasCoverChanged();
                mTrackRating = getTrackRating(currentTrack);

                if (DEBUG) {
                    Log.i(TAG, toString());
                }
            }

            mLastAlbum = mAlbumName;
            mLastArtist = mArtistName;

            return currentTrack;
        }

        /**
         * This method retrieves the current rating sticker from the connected media server.
         *
         * @param currentTrack The current playing {@link Music} item.
         * @return Returns the current rating sticker from the connected media server.
         */
        private float getTrackRating(final FilesystemTreeEntry currentTrack) {
            float rating = 0.0f;

            if (currentTrack != null && mSticker.isAvailable() &&
                    mSettings.getBoolean("enableRating", false)) {
                try {
                    rating = (float) mSticker.getRating(currentTrack) / 2.0f;
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to get the current track rating.", e);
                }
            }

            return rating;
        }

        private boolean hasCoverChanged() {
            final boolean invalid = mArtistName == null || mAlbumName == null;
            return invalid || !mArtistName.equals(mLastArtist) || !mAlbumName.equals(mLastAlbum);
        }

        /**
         * Send out the messages to listeners.
         */
        @Override
        protected final void onPostExecute(final Music result) {
            super.onPostExecute(result);

            final boolean sendCoverUpdate = mHasCoverChanged || result == null
                    || mForceCoverUpdate;

            if (result == null) {
                mTitle = mApp.getResources().getString(R.string.noSongInfo);
            }

            if (mFullTrackInfoListener != null) {
                mFullTrackInfoListener.onTrackInfoUpdate(result, mTrackRating, mAlbumName,
                        mArtistName, mDate, mTitle);

                if (sendCoverUpdate) {
                    if (DEBUG) {
                        Log.e(TAG, "Sending cover update to full track info listener.");
                    }
                    mFullTrackInfoListener.onCoverUpdate(mAlbumInfo);
                }
            }

            if (mTrackInfoListener != null) {
                mTrackInfoListener.onTrackInfoUpdate(mAlbumName, mTitle);

                if (sendCoverUpdate) {
                    if (DEBUG) {
                        Log.d(TAG, "Sending cover update to track info listener.");
                    }
                    mTrackInfoListener.onCoverUpdate(mAlbumInfo);
                }
            }
        }

        /**
         * If not a stream, this sets up the mArtistName based on artist and album information.
         *
         * @param currentTrack The current playing {@link Music} item.
         */
        private void setArtist(final Music currentTrack) {
            final boolean showAlbumArtist = mSettings.getBoolean("showAlbumArtist", true);
            final String albumArtist = currentTrack.getAlbumArtistName();

            mArtistName = currentTrack.getArtistName();
            if (mArtistName == null || mArtistName.isEmpty()) {
                mArtistName = albumArtist;
            } else if (showAlbumArtist && albumArtist != null &&
                    !mArtistName.toLowerCase().contains(albumArtist.toLowerCase())) {
                mArtistName = albumArtist + " / " + mArtistName;
            }
        }

        @Override
        public String toString() {
            return "UpdateTrackInfoAsync{" +
                    "mAlbumInfo=" + mAlbumInfo +
                    ", mAlbumName='" + mAlbumName + '\'' +
                    ", mArtistName='" + mArtistName + '\'' +
                    ", mDate='" + mDate + '\'' +
                    ", mHasCoverChanged=" + mHasCoverChanged +
                    ", mTitle='" + mTitle + '\'' +
                    ", mTrackRating=" + mTrackRating +
                    "} " + super.toString();
        }
    }
}
