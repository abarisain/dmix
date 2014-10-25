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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class AlbumCoverDownloadListener implements CoverDownloadListener {

    private static final String TAG = "CoverDownloadListener";

    private static final MPDApplication sApp = MPDApplication.getInstance();

    boolean mBigCoverNotFound;

    ImageView mCoverArt;

    ProgressBar mCoverArtProgress;

    public AlbumCoverDownloadListener(final ImageView coverArt) {
        super();
        mCoverArt = coverArt;
        mCoverArt.setVisibility(View.VISIBLE);
        freeCoverDrawable();
    }

    public AlbumCoverDownloadListener(final ImageView coverArt,
            final ProgressBar coverArtProgress,
            final boolean bigCoverNotFound) {
        super();
        mCoverArt = coverArt;
        mBigCoverNotFound = bigCoverNotFound;
        mCoverArt.setVisibility(View.VISIBLE);
        mCoverArtProgress = coverArtProgress;
        mCoverArtProgress.setIndeterminate(true);
        mCoverArtProgress.setVisibility(View.INVISIBLE);
        freeCoverDrawable();
    }

    @DrawableRes
    public static int getLargeNoCoverResource() {
        return getNoCoverResource(true);
    }

    @DrawableRes
    public static int getNoCoverResource() {
        return getNoCoverResource(false);
    }

    /**
     * Get a resource to use when no cover exists, according to current theme.
     *
     * @param isLarge If true a large resolution resource will be returned, false small.
     * @return A resource to use when no cover art exists.
     */
    @DrawableRes
    private static int getNoCoverResource(final boolean isLarge) {
        final int newResource;

        if (sApp.isLightThemeSelected()) {
            if (isLarge) {
                newResource = R.drawable.no_cover_art_light_big;
            } else {
                newResource = R.drawable.no_cover_art_light;
            }
        } else {
            if (isLarge) {
                newResource = R.drawable.no_cover_art_big;
            } else {
                newResource = R.drawable.no_cover_art;
            }
        }

        return newResource;
    }

    public void detach() {
        mCoverArtProgress = null;
        mCoverArt = null;
    }

    public void freeCoverDrawable() {
        freeCoverDrawable(null);
    }

    private void freeCoverDrawable(final Drawable oldDrawable) {
        if (mCoverArt == null) {
            return;
        }
        final Drawable coverDrawable = oldDrawable == null ? mCoverArt.getDrawable() : oldDrawable;
        if (coverDrawable == null || !(coverDrawable instanceof CoverBitmapDrawable)) {
            return;
        }
        if (oldDrawable == null) {
            final int noCoverDrawable;
            if (mBigCoverNotFound) {
                noCoverDrawable = getLargeNoCoverResource();
            } else {
                noCoverDrawable = getNoCoverResource();
            }
            mCoverArt.setImageResource(noCoverDrawable);
        }

        coverDrawable.setCallback(null);
        final Bitmap coverBitmap = ((BitmapDrawable) coverDrawable).getBitmap();
        if (coverBitmap != null) {
            coverBitmap.recycle();
        }
    }

    private boolean isMatchingCover(final CoverInfo coverInfo) {
        return coverInfo != null && mCoverArt != null &&
                (mCoverArt.getTag() == null || mCoverArt.getTag().equals(coverInfo.getKey()));
    }

    @Override
    public void onCoverDownloadStarted(final CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        if (mCoverArtProgress != null) {
            mCoverArtProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCoverDownloaded(final CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        if (cover.getBitmap() == null) {
            return;
        }
        try {
            if (mCoverArtProgress != null) {
                mCoverArtProgress.setVisibility(View.INVISIBLE);
            }
            freeCoverDrawable(mCoverArt.getDrawable());
            mCoverArt.setImageDrawable(new CoverBitmapDrawable(sApp.getResources(), cover
                    .getBitmap()[0]));
            cover.setBitmap(null);
        } catch (final Exception e) {
            Log.w(TAG, "Exception.", e);
        }
    }

    @Override
    public void onCoverNotFound(final CoverInfo coverInfo) {
        if (!isMatchingCover(coverInfo)) {
            return;
        }
        coverInfo.setBitmap(null);
        if (mCoverArtProgress != null) {
            mCoverArtProgress.setVisibility(View.INVISIBLE);
        }
        freeCoverDrawable();
    }

    @Override
    public void tagAlbumCover(final AlbumInfo albumInfo) {
        if (mCoverArt != null && albumInfo != null) {
            mCoverArt.setTag(albumInfo.getKey());
        }
    }
}
