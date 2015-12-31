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

package com.namelessdev.mpdroid.service;

import com.anpmech.mpd.Tools;
import com.namelessdev.mpdroid.cover.CoverAsyncHelper;
import com.namelessdev.mpdroid.cover.CoverDownloadListener;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.Collection;

/**
 * A simple class tailor designed to keep various handlers of the MPDroid service with an updated
 * cover.
 */
class AlbumCoverHandler implements CoverDownloadListener {

    private static final boolean DEBUG = false;

    private static final String TAG = "AlbumCoverHandler";

    private final int mIconHeight;

    private final int mIconWidth;

    /** The album cover helper instance. */
    private CoverAsyncHelper mCoverAsyncHelper = null;

    private Bitmap mFullSizeAlbumCover = null;

    private FullSizeCallback mFullSizeListener = null;

    private AlbumInfo mLastAlbumInfo;

    private Bitmap mNotificationCover;

    private NotificationCallback mNotificationListener;

    AlbumCoverHandler(final MPDroidService serviceContext) {
        super();

        mIconHeight = serviceContext
                .getResources()
                .getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

        mIconWidth = serviceContext.getResources()
                .getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

        final int maxSize = -1;
        mCoverAsyncHelper = new CoverAsyncHelper();
        mCoverAsyncHelper.setCachedCoverMaxSize(maxSize);
        mCoverAsyncHelper.setCoverMaxSize(maxSize);
        CoverAsyncHelper.setCoverRetrieversFromPreferences();
        mCoverAsyncHelper.addCoverDownloadListener(this);
    }

    final void addCallback(final FullSizeCallback callback) {
        mFullSizeListener = callback;
    }

    final void addCallback(final NotificationCallback callback) {
        mNotificationListener = callback;
    }

    private boolean isSameAlbum(final AlbumInfo albumInfo) {
        String albumName = null;
        String lastAlbumName = null;

        if (albumInfo != null) {
            albumName = albumInfo.getAlbumName();
        }

        if (mLastAlbumInfo != null) {
            lastAlbumName = mLastAlbumInfo.getAlbumName();
        }

        return Tools.equals(albumName, lastAlbumName);
    }

    private boolean isSameArtist(final AlbumInfo albumInfo) {
        String artistName = null;
        String lastArtistName = null;

        if (albumInfo != null) {
            artistName = albumInfo.getArtistName();
        }
        if (mLastAlbumInfo != null) {
            lastArtistName = mLastAlbumInfo.getArtistName();
        }

        return Tools.equals(artistName, lastArtistName);
    }

    /**
     * A method implemented from CoverDownloadListener used for progress.
     *
     * @param albumInfo A current {@code AlbumInfo object}.
     */
    @Override
    public void onCoverDownloadStarted(final AlbumInfo albumInfo) {
    }

    /**
     * A method implemented from CoverDownloadListener executed after cover download has
     * successfully completed.
     *
     * @param albumInfo A current {@code AlbumInfo object}.
     */
    @Override
    public final void onCoverDownloaded(final AlbumInfo albumInfo,
            final Collection<Bitmap> bitmaps) {
        mLastAlbumInfo = albumInfo;

        /**
         * This is a workaround for the rare occasion of bitmaps.iterator().getNext() being
         * null.
         */
        Bitmap placeholder = null;
        for (final Bitmap bitmap : bitmaps) {
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
            mFullSizeListener.onCoverUpdate(mFullSizeAlbumCover);
            mNotificationListener.onCoverUpdate(mNotificationCover);
        }
    }

    /**
     * A method implemented from CoverDownloadListener executed after an album cover was not found.
     */
    @Override
    public void onCoverNotFound(final AlbumInfo albumInfo) {
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
            updateAlbumCover(albumInfo);
        }
    }

    /**
     * This method updates the service covers if the current cover path is different than currently
     * playing, if cache is enabled.
     *
     * @param albumInfo The {@link AlbumInfo} to update from the cache.
     */
    private void updateAlbumCover(final AlbumInfo albumInfo) {
        if (DEBUG) {
            Log.d(TAG, "updateAlbumCoverWithCache(music): " + albumInfo);
        }

        final boolean isSame = isSameAlbum(albumInfo) && isSameArtist(albumInfo);
        final boolean fullCoverValid =
                mFullSizeAlbumCover != null && !mFullSizeAlbumCover.isRecycled();
        final boolean smallCoverValid =
                mNotificationCover != null && !mNotificationCover.isRecycled();

        if (isSame && fullCoverValid && smallCoverValid) {
            if (DEBUG) {
                Log.d(TAG, "Cover the same as last time, omitting.");
            }

            mNotificationListener.onCoverUpdate(mNotificationCover);
            mFullSizeListener.onCoverUpdate(mFullSizeAlbumCover);
        } else {
            if (DEBUG) {
                Log.d(TAG, "Cover not found, attempting download.");
            }

            mLastAlbumInfo = null;
            mNotificationListener.onCoverUpdate(null);
            mFullSizeListener.onCoverUpdate(null);
            mCoverAsyncHelper.downloadCover(albumInfo);
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
}
