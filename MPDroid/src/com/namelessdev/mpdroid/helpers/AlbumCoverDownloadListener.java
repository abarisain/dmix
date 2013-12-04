package com.namelessdev.mpdroid.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;

public class AlbumCoverDownloadListener implements CoverDownloadListener {
    Context context;
    ImageView coverArt;
    ProgressBar coverArtProgress;
    boolean lightTheme;
    boolean bigCoverNotFound;

    public AlbumCoverDownloadListener(Context context, ImageView coverArt, boolean lightTheme) {
        this.context = context;
        this.coverArt = coverArt;
        this.coverArt.setVisibility(View.VISIBLE);
        this.lightTheme = lightTheme;
        freeCoverDrawable();
    }

    public AlbumCoverDownloadListener(Context context, ImageView coverArt, ProgressBar coverArtProgress, boolean lightTheme,
                                      boolean bigCoverNotFound) {
        this.context = context;
        this.coverArt = coverArt;
        this.lightTheme = lightTheme;
        this.bigCoverNotFound = bigCoverNotFound;
        this.coverArt.setVisibility(View.VISIBLE);
        this.coverArtProgress = coverArtProgress;
        this.coverArtProgress.setIndeterminate(true);
        this.coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
        freeCoverDrawable();
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
            coverArt.setImageDrawable(new CoverBitmapDrawable(context.getResources(), cover.getBitmap()[0]));
            cover.setBitmap(null);
        } catch (Exception e) {
            Log.w(AlbumCoverDownloadListener.class.getSimpleName(), e);
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
        if (coverArt != null) {
            // Allows to retry cover resolving for playlist items (might have been downloaded from the nowplaying fragment)
            coverArt.setTag(null);
        }
        freeCoverDrawable();
    }

    @Override
    public void onCoverDownloadStarted(CoverInfo cover) {
        if (coverArtProgress != null) {
            this.coverArtProgress.setVisibility(ProgressBar.VISIBLE);
        }
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
            int noCoverDrawable;
            if (bigCoverNotFound) {
                noCoverDrawable = lightTheme ? R.drawable.no_cover_art_light_big : R.drawable.no_cover_art_big;
            } else {
                noCoverDrawable = lightTheme ? R.drawable.no_cover_art_light : R.drawable.no_cover_art;
            }
            coverArt.setImageResource(noCoverDrawable);
        }

        coverDrawable.setCallback(null);
        final Bitmap coverBitmap = ((BitmapDrawable) coverDrawable).getBitmap();
        if (coverBitmap != null) {
            coverBitmap.recycle();
        }
    }

    public void detach() {
        coverArtProgress = null;
        coverArt = null;
    }

    private boolean isMatchingCover(CoverInfo coverInfo) {
        return coverInfo != null && coverArt != null &&
                (coverArt.getTag() == null || coverArt.getTag().equals(coverInfo.getKey()));
    }
}
