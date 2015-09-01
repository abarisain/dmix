/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.namelessdev.mpdroid.cover;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.Collection;

public class AlbumCoverDownloadListener implements CoverDownloadListener {

    private static final String TAG = "CoverDownloadListener";

    private static final MPDApplication sApp = MPDApplication.getInstance();

    private boolean mBigCoverNotFound;

    private ImageView mCoverArt;

    private ProgressBar mCoverArtProgress;

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

    public final void freeCoverDrawable() {
        freeCoverDrawable(null);
    }

    private void freeCoverDrawable(final Drawable oldDrawable) {
        final Drawable coverDrawable;

        if (oldDrawable == null) {
            coverDrawable = mCoverArt.getDrawable();
        } else {
            coverDrawable = oldDrawable;
        }

        if (coverDrawable instanceof CoverBitmapDrawable) {
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
    }

    private boolean isMatchingCover(final AlbumInfo coverInfo) {
        boolean isMatchingCover = false;

        if (coverInfo != null && mCoverArt != null) {
            if (mCoverArt.getTag() == null || mCoverArt.getTag().equals(coverInfo.getKey())) {
                isMatchingCover = true;
            }
        }

        return isMatchingCover;
    }

    @Override
    public void onCoverDownloadStarted(final AlbumInfo albumInfo) {
        if (isMatchingCover(albumInfo) && mCoverArtProgress != null) {
            mCoverArtProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCoverDownloaded(final AlbumInfo albumInfo, final Collection<Bitmap> bitmaps) {
        if (isMatchingCover(albumInfo) && !bitmaps.isEmpty()) {
            final CoverBitmapDrawable coverBitmapDrawable =
                    new CoverBitmapDrawable(sApp.getResources(), bitmaps);

            if (mCoverArtProgress != null) {
                mCoverArtProgress.setVisibility(View.INVISIBLE);
            }

            freeCoverDrawable(mCoverArt.getDrawable());
            mCoverArt.setImageDrawable(coverBitmapDrawable);
            ((CoverInfo) albumInfo).setBitmap();
        }
    }

    @Override
    public void onCoverNotFound(final AlbumInfo albumInfo) {
        if (isMatchingCover(albumInfo)) {
            ((CoverInfo) albumInfo).setBitmap();
            if (mCoverArtProgress != null) {
                mCoverArtProgress.setVisibility(View.INVISIBLE);
            }
            freeCoverDrawable();
        }
    }

    @Override
    public void tagAlbumCover(final AlbumInfo albumInfo) {
        if (mCoverArt != null && albumInfo != null) {
            mCoverArt.setTag(albumInfo.getKey());
        }
    }
}
