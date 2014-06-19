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
    ImageView coverArt;
    ProgressBar coverArtProgress;
    private static MPDApplication sApp = MPDApplication.getInstance();
    boolean bigCoverNotFound;
    private static final String TAG = "CoverDownloadListener";


    public AlbumCoverDownloadListener(ImageView coverArt) {
        this.coverArt = coverArt;
        this.coverArt.setVisibility(View.VISIBLE);
        freeCoverDrawable();
    }

    public AlbumCoverDownloadListener(ImageView coverArt,
            ProgressBar coverArtProgress,
            boolean bigCoverNotFound) {
        this.coverArt = coverArt;
        this.bigCoverNotFound = bigCoverNotFound;
        this.coverArt.setVisibility(View.VISIBLE);
        this.coverArtProgress = coverArtProgress;
        this.coverArtProgress.setIndeterminate(true);
        this.coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
        freeCoverDrawable();
    }

    public static int getNoCoverResource() {
        return getNoCoverResource(false);
    }

    public static int getLargeNoCoverResource() {
        return getNoCoverResource(true);
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
        coverArtProgress = null;
        coverArt = null;
    }

    public void freeCoverDrawable() {
        freeCoverDrawable(null);
    }

    private void freeCoverDrawable(Drawable oldDrawable) {
        if (coverArt == null)
            return;
        final Drawable coverDrawable = oldDrawable == null ? coverArt.getDrawable() : oldDrawable;
        if (coverDrawable == null || !(coverDrawable instanceof CoverBitmapDrawable))
            return;
        if (oldDrawable == null) {
            final int noCoverDrawable;
            if (bigCoverNotFound) {
                noCoverDrawable = getLargeNoCoverResource();
            } else {
                noCoverDrawable = getNoCoverResource();
            }
            coverArt.setImageResource(noCoverDrawable);
        }

        coverDrawable.setCallback(null);
        final Bitmap coverBitmap = ((BitmapDrawable) coverDrawable).getBitmap();
        if (coverBitmap != null) {
            coverBitmap.recycle();
        }
    }

    private boolean isMatchingCover(CoverInfo coverInfo) {
        return coverInfo != null && coverArt != null &&
                (coverArt.getTag() == null || coverArt.getTag().equals(coverInfo.getKey()));
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
            if (coverArtProgress != null) {
                coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
            }
            freeCoverDrawable(coverArt.getDrawable());
            coverArt.setImageDrawable(new CoverBitmapDrawable(sApp.getResources(), cover
                    .getBitmap()[0]));
            cover.setBitmap(null);
        } catch (final Exception e) {
            Log.w(TAG, "Exception.", e);
        }
    }

    @Override
    public void onCoverDownloadStarted(CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        if (coverArtProgress != null) {
            this.coverArtProgress.setVisibility(ProgressBar.VISIBLE);
        }
    }

    @Override
    public void onCoverNotFound(CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        cover.setBitmap(null);
        if (coverArtProgress != null)
            coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
        freeCoverDrawable();
    }

    @Override
    public void tagAlbumCover(AlbumInfo albumInfo) {
        if (coverArt != null && albumInfo != null) {
            coverArt.setTag(albumInfo.getKey());
        }
    }
}
