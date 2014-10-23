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

package com.namelessdev.mpdroid.service;

import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.ICoverRetriever;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverInfo;
import com.namelessdev.mpdroid.helpers.CoverManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A simple class tailor designed to keep various handlers
 * of the MPDroid service with an updated cover.
 */
class AlbumCoverHandler implements CoverDownloadListener {

    private static final boolean DEBUG = false;

    private static final String TAG = "AlbumCoverHandler";

    private final int mIconHeight;

    private final int mIconWidth;

    /** The current path to the album cover on the local device */
    private String mAlbumCoverPath = null;

    /** The album cover helper instance. */
    private CoverAsyncHelper mCoverAsyncHelper = null;

    private Bitmap mFullSizeAlbumCover = null;

    private FullSizeCallback mFullSizeListener = null;

    private boolean mIsAlbumCacheEnabled;

    private Bitmap mNotificationCover;

    private NotificationCallback mNotificationListener;

    AlbumCoverHandler(final MPDroidService serviceContext) {
        super();

        mIsAlbumCacheEnabled =
                PreferenceManager.getDefaultSharedPreferences(serviceContext)
                        .getBoolean(CoverManager.PREFERENCE_CACHE, true);

        mIconHeight = serviceContext
                .getResources()
                .getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

        mIconWidth = serviceContext.getResources()
                .getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

        if (mIsAlbumCacheEnabled) {
            final int maxSize = -1;
            mCoverAsyncHelper = new CoverAsyncHelper();
            mCoverAsyncHelper.setCachedCoverMaxSize(maxSize);
            mCoverAsyncHelper.setCoverMaxSize(maxSize);
            mCoverAsyncHelper.setCoverRetrieversFromPreferences();
            mCoverAsyncHelper.addCoverDownloadListener(this);
        }
    }

    /**
     * This method retrieves a path to an album cover bitmap from the cache.
     *
     * @return String A path to a cached album cover bitmap.
     */
    private static String retrieveCoverArtPath(final AlbumInfo albumInfo) {
        if (DEBUG) {
            Log.d(TAG, "retrieveCoverArtPath(" + albumInfo + ')');
        }
        final ICoverRetriever cache = new CachedCover();
        String coverArtPath = null;
        String[] coverArtPaths = null;

        try {
            coverArtPaths = cache.getCoverUrl(albumInfo);
        } catch (final Exception e) {
            Log.d(TAG, "Failed to get the cover URL from the cache.", e);
        }

        if (coverArtPaths != null && coverArtPaths.length > 0) {
            coverArtPath = coverArtPaths[0];
        }
        return coverArtPath;
    }

    final void addCallback(final FullSizeCallback callback) {
        mFullSizeListener = callback;
    }

    final void addCallback(final NotificationCallback callback) {
        mNotificationListener = callback;
    }

    /**
     * A method implemented from CoverDownloadListener used for progress.
     *
     * @param cover A current {@code CoverInfo object}.
     */
    @Override
    public void onCoverDownloadStarted(final CoverInfo cover) {
    }

    /**
     * A method implemented from CoverDownloadListener executed
     * after cover download has successfully completed.
     *
     * @param cover A current {@code CoverInfo object}.
     */
    @Override
    public final void onCoverDownloaded(final CoverInfo cover) {
        if (mIsAlbumCacheEnabled) {
            /** This is a workaround for the rare occasion of cover.getBitmap()[0] being null. */
            Bitmap placeholder = null;
            for (final Bitmap bitmap : cover.getBitmap()) {
                if (bitmap != null) {
                    placeholder = bitmap;
                    break;
                }
            }

            if (placeholder != null) {
                mFullSizeAlbumCover = placeholder;
                mNotificationCover =
                        Bitmap.createScaledBitmap(mFullSizeAlbumCover, mIconWidth, mIconHeight,
                                false);
                mAlbumCoverPath = retrieveCoverArtPath(cover);
                mFullSizeListener.onCoverUpdate(mFullSizeAlbumCover);
                mNotificationListener.onCoverUpdate(mNotificationCover);
            }
        }
    }

    /**
     * A method implemented from CoverDownloadListener
     * executed after an album cover was not found.
     *
     * @param coverInfo A current {@code CoverInfo object}.
     */
    @Override
    public void onCoverNotFound(final CoverInfo coverInfo) {
    }

    final void setAlbumCache(final boolean value) {
        mIsAlbumCacheEnabled = value;
    }

    final void stop() {
        /** Don't recycle. Android can easily get out of state; let GC do it's magic. */

        mFullSizeListener = null;
        mNotificationListener = null;

        if (mCoverAsyncHelper != null) {
            mCoverAsyncHelper.removeCoverDownloadListener(this);
        }
    }

    /**
     * A method implemented from CoverDownloadListener used for progress.
     *
     * @param albumInfo A current {@code AlbumInfo object}.
     */
    @Override
    public void tagAlbumCover(final AlbumInfo albumInfo) {
    }

    final void update(final AlbumInfo albumInfo) {
        if (DEBUG) {
            Log.d(TAG, "update()");
        }

        if (mFullSizeListener != null) {
            if (mIsAlbumCacheEnabled) {
                updateAlbumCoverWithCached(albumInfo);
            } /** TODO: Add no cache option */
        }
    }

    /**
     * This method updates the service covers if the current cover
     * path is different than currently playing, if cache is enabled.
     */
    private void updateAlbumCoverWithCached(final AlbumInfo albumInfo) {
        if (DEBUG) {
            Log.d(TAG, "updateAlbumCoverWithCache(music): " + albumInfo);
        }
        final String coverArtPath = retrieveCoverArtPath(albumInfo);
        final boolean sameCover = coverArtPath != null && coverArtPath.equals(mAlbumCoverPath);
        final boolean fullCoverValid =
                mFullSizeAlbumCover != null && !mFullSizeAlbumCover.isRecycled();
        final boolean smallCoverValid =
                mNotificationCover != null && !mNotificationCover.isRecycled();

        if (coverArtPath == null) {
            if (DEBUG) {
                Log.d(TAG, "Cover not found, attempting download.");
            }

            mNotificationListener.onCoverUpdate(null);
            mFullSizeListener.onCoverUpdate(null);
            mCoverAsyncHelper.downloadCover(albumInfo);
        } else if (sameCover && fullCoverValid && smallCoverValid) {
            if (DEBUG) {
                Log.d(TAG, "Cover the same as last time, omitting.");
            }

            mNotificationListener.onCoverUpdate(mNotificationCover);
            mFullSizeListener.onCoverUpdate(mFullSizeAlbumCover);
        } else {
            if (DEBUG) {
                Log.d(TAG, "Cover found in cache, decoding.");
            }

            new DecodeAlbumCover().execute(coverArtPath);
        }
    }

    interface FullSizeCallback {

        /**
         * This is called when cover art needs to be updated due to server information change.
         *
         * @param albumCover The current album cover bitmap.
         */
        void onCoverUpdate(Bitmap albumCover);
    }

    interface NotificationCallback {

        /**
         * This is called when cover art needs to be updated due to server information change.
         *
         * @param albumCover the current album cover bitmap.
         */
        void onCoverUpdate(Bitmap albumCover);
    }

    /**
     * This method updates the service covers if the current cover
     * path is different than currently playing, if cache is enabled.
     */
    private class DecodeAlbumCover extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected final Bitmap doInBackground(final String... params) {
            if (DEBUG) {
                Log.d(TAG, "doInBackground()");
            }

            /** Don't recycle. Android can easily get out of state; let GC do it's magic. */

            mAlbumCoverPath = params[0];

            mFullSizeAlbumCover = BitmapFactory.decodeFile(mAlbumCoverPath);

            if (mFullSizeAlbumCover == null) {
                /** TODO: Consider album cover reset here? */
                mNotificationCover = null;
            } else {
                /** This will always scale down, no filter needed. */
                mNotificationCover =
                        Bitmap.createScaledBitmap(mFullSizeAlbumCover, mIconWidth, mIconHeight,
                                false);
            }

            return mFullSizeAlbumCover;
        }

        @Override
        protected final void onPostExecute(final Bitmap result) {
            super.onPostExecute(result);

            if (mFullSizeListener != null) {
                mFullSizeListener.onCoverUpdate(mFullSizeAlbumCover);
            }

            if (mNotificationListener != null) {
                mNotificationListener.onCoverUpdate(mNotificationCover);
            }
        }
    }
}
