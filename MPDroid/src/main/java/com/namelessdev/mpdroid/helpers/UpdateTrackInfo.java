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

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Music;
import org.a0z.mpd.subsystem.Sticker;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * This sets up an AsyncTask to gather and parse all the information to update the
 * track information outside of the UI thread, then sends a callback to the resource
 * listeners.
 */
public class UpdateTrackInfo {

    private static final boolean DEBUG = false;

    private static final String TAG = "UpdateTrackInfo";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private final SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(mApp);

    private final Sticker mSticker;

    private boolean mForceCoverUpdate = false;

    private FullTrackInfoUpdate mFullTrackInfoListener = null;

    private String mLastAlbum = null;

    private String mLastArtist = null;

    private TrackInfoUpdate mTrackInfoListener = null;

    public UpdateTrackInfo() {
        mSticker = mApp.oMPDAsyncHelper.oMPD.getStickerManager();
    }

    public final void addCallback(final FullTrackInfoUpdate listener) {
        mFullTrackInfoListener = listener;
    }

    public final void addCallback(final TrackInfoUpdate listener) {
        mTrackInfoListener = listener;
    }

    public final void refresh(final MPDStatus mpdStatus) {
        refresh(mpdStatus, false);
    }

    public final void refresh(final MPDStatus mpdStatus, final boolean forceCoverUpdate) {
        mForceCoverUpdate = forceCoverUpdate;
        new UpdateTrackInfoAsync().execute(mpdStatus);
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

    private class UpdateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Void> {

        private String mAlbum = null;

        private AlbumInfo mAlbumInfo = null;

        private String mArtist = null;

        private Music mCurrentTrack = null;

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
        protected final Void doInBackground(final MPDStatus... params) {
            final int songPos = params[0].getSongPos();
            mCurrentTrack = mApp.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);

            if (mCurrentTrack != null) {
                if (mCurrentTrack.isStream()) {
                    if (mCurrentTrack.hasTitle()) {
                        mAlbum = mCurrentTrack.getName();
                        mTitle = mCurrentTrack.getTitle();
                    } else {
                        mTitle = mCurrentTrack.getName();
                    }

                    mArtist = mCurrentTrack.getArtist();
                    mAlbumInfo = new AlbumInfo(mArtist, mAlbum);
                } else {
                    mAlbum = mCurrentTrack.getAlbum();

                    mDate = Long.toString(mCurrentTrack.getDate());
                    if (mDate.isEmpty() || mDate.charAt(0) == '-') {
                        mDate = "";
                    } else {
                        mDate = " - " + mDate;
                    }

                    mTitle = mCurrentTrack.getTitle();
                    setArtist();
                    mAlbumInfo = new AlbumInfo(mCurrentTrack);
                }
                mHasCoverChanged = hasCoverChanged();
                mTrackRating = getTrackRating();

                if (DEBUG) {
                    Log.i(TAG,
                            "mAlbum: " + mAlbum +
                                    " mArtist: " + mArtist +
                                    " mDate: " + mDate +
                                    " mAlbumInfo: " + mAlbumInfo +
                                    " mHasTrackChanged: " + mHasCoverChanged +
                                    " mCurrentTrack: " + mCurrentTrack +
                                    " mForceCoverUpdate: " + mForceCoverUpdate
                    );
                }
            }

            mLastAlbum = mAlbum;
            mLastArtist = mArtist;

            return null;
        }

        /**
         * This method retrieves the current rating sticker from the connected media server.
         *
         * @return Returns the current rating sticker from the connected media server.
         */
        private float getTrackRating() {
            float rating = 0.0f;

            if (mCurrentTrack != null && mSticker.isAvailable() &&
                    mSettings.getBoolean("enableRating", false)) {
                try {
                    rating = (float) mSticker.getRating(mCurrentTrack) / 2.0f;
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to get the current track rating.", e);
                }
            }

            return rating;
        }

        private boolean hasCoverChanged() {
            final boolean invalid = mArtist == null || mAlbum == null;
            return invalid || !mArtist.equals(mLastArtist) || !mAlbum.equals(mLastAlbum);
        }

        /**
         * Send out the messages to listeners.
         */
        @Override
        protected final void onPostExecute(final Void result) {
            super.onPostExecute(result);

            final boolean sendCoverUpdate = mHasCoverChanged || mCurrentTrack == null
                    || mForceCoverUpdate;

            if (mCurrentTrack == null) {
                mTitle = mApp.getResources().getString(R.string.noSongInfo);
            }

            if (mFullTrackInfoListener != null) {
                mFullTrackInfoListener.onTrackInfoUpdate(mCurrentTrack, mTrackRating, mAlbum,
                        mArtist, mDate, mTitle);

                if (sendCoverUpdate) {
                    if (DEBUG) {
                        Log.e(TAG, "Sending cover update to full track info listener.");
                    }
                    mFullTrackInfoListener.onCoverUpdate(mAlbumInfo);
                }
            }

            if (mTrackInfoListener != null) {
                mTrackInfoListener.onTrackInfoUpdate(mAlbum, mTitle);

                if (sendCoverUpdate) {
                    if (DEBUG) {
                        Log.d(TAG, "Sending cover update to track info listener.");
                    }
                    mTrackInfoListener.onCoverUpdate(mAlbumInfo);
                }
            }
        }

        /**
         * If not a stream, this sets up the mArtist based on mArtist and album mArtist
         * information.
         */
        private void setArtist() {
            final boolean showAlbumArtist = mSettings.getBoolean("showAlbumArtist", true);
            final String albumArtist = mCurrentTrack.getAlbumArtist();

            mArtist = mCurrentTrack.getArtist();
            if (mArtist == null || mArtist.isEmpty()) {
                mArtist = albumArtist;
            } else if (showAlbumArtist && albumArtist != null &&
                    !mArtist.toLowerCase().contains(albumArtist.toLowerCase())) {
                mArtist = albumArtist + " / " + mArtist;
            }
        }
    }
}
