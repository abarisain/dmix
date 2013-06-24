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

	public AlbumCoverDownloadListener(Context context, ImageView coverArt, boolean lightTheme) {
		this.context = context;
		this.coverArt = coverArt;
		this.coverArt.setVisibility(View.VISIBLE);
		this.lightTheme = lightTheme;
		freeCoverDrawable();
	}

	public AlbumCoverDownloadListener(Context context, ImageView coverArt, ProgressBar coverArtProgress, boolean lightTheme) {
		this.context = context;
		this.coverArt = coverArt;
		this.lightTheme = lightTheme;
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
				final Drawable oldDrawable = coverArt.getDrawable();
				coverArt.setImageDrawable(new CoverBitmapDrawable(context.getResources(), cover));
				freeCoverDrawable(oldDrawable);
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
		if (oldDrawable == null)
			coverArt.setImageResource(lightTheme ? R.drawable.no_cover_art_light : R.drawable.no_cover_art);

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
