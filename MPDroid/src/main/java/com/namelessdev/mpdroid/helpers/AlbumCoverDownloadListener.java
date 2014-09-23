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

import org.a0z.mpd.AlbumInfo;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class AlbumCoverDownloadListener implements CoverDownloadListener {

    private static final String TAG = "CoverDownloadListener";

    private static MPDApplication sApp = MPDApplication.getInstance();

    boolean mBigCoverNotFound;

    ImageView mCoverArt;

    ProgressBar mCoverArtProgress;

    public AlbumCoverDownloadListener(ImageView coverArt) {
        this.mCoverArt = coverArt;
        this.mCoverArt.setVisibility(View.VISIBLE);
        freeCoverDrawable();
    }

    public AlbumCoverDownloadListener(ImageView coverArt,
            ProgressBar coverArtProgress,
            boolean bigCoverNotFound) {
        this.mCoverArt = coverArt;
        this.mBigCoverNotFound = bigCoverNotFound;
        this.mCoverArt.setVisibility(View.VISIBLE);
        this.mCoverArtProgress = coverArtProgress;
        this.mCoverArtProgress.setIndeterminate(true);
        this.mCoverArtProgress.setVisibility(ProgressBar.INVISIBLE);
        freeCoverDrawable();
    }

    public static int getLargeNoCoverResource() {
        return getNoCoverResource(true);
    }

    public static int getNoCoverResource() {
        return getNoCoverResource(false);
    }

    /**
     * Get a resource to use when no cover exists, according to current theme.
     *
     * @param isLarge If true a large resolution resource will be returned, false small.
     * @return A resource to use when no cover art exists.
     */
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

    private void freeCoverDrawable(Drawable oldDrawable) {
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

    private boolean isMatchingCover(CoverInfo coverInfo) {
        return coverInfo != null && mCoverArt != null &&
                (mCoverArt.getTag() == null || mCoverArt.getTag().equals(coverInfo.getKey()));
    }

    @Override
    public void onCoverDownloadStarted(CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        if (mCoverArtProgress != null) {
            this.mCoverArtProgress.setVisibility(ProgressBar.VISIBLE);
        }
    }

    @Override
    public void onCoverDownloaded(CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        if (cover.getBitmap() == null) {
            return;
        }
        try {
            if (mCoverArtProgress != null) {
                mCoverArtProgress.setVisibility(ProgressBar.INVISIBLE);
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
    public void onCoverNotFound(CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        cover.setBitmap(null);
        if (mCoverArtProgress != null) {
            mCoverArtProgress.setVisibility(ProgressBar.INVISIBLE);
        }
        freeCoverDrawable();
    }

    @Override
    public void tagAlbumCover(AlbumInfo albumInfo) {
        if (mCoverArt != null && albumInfo != null) {
            mCoverArt.setTag(albumInfo.getKey());
        }
    }
}
