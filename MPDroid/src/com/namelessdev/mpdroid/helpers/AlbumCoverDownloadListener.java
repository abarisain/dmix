package com.namelessdev.mpdroid.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper.CoverDownloadListener;

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

	public void onCoverDownloaded(Bitmap cover) {
		if (coverArtProgress != null)
			coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
		if (coverArt == null)
			return;
		try {
			if (cover != null) {
				freeCoverDrawable(coverArt.getDrawable());
				coverArt.setImageDrawable(new CoverBitmapDrawable(context.getResources(), cover));
			} else {
				// Should not be happening, but happened.
				onCoverNotFound();
			}
		} catch (Exception e) {
			//Probably rotated, ignore
			e.printStackTrace();
		}
	}

	public void onCoverNotFound() {
		if (coverArtProgress != null)
			coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
		freeCoverDrawable();
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
		if (coverBitmap != null)
			coverBitmap.recycle();
	}

	public void detach() {
		coverArtProgress = null;
		coverArt = null;
	}

}
