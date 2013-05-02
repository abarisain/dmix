package com.namelessdev.mpdroid.helpers;

import android.widget.ImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ProgressBar;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper.CoverDownloadListener;

public class AlbumCoverDownloadListener implements CoverDownloadListener {
	Context context;
	ImageView coverArt;
	ProgressBar coverArtProgress;

	public AlbumCoverDownloadListener(Context context, ImageView coverArt) {
		this.context = context;
		this.coverArt = coverArt;
		this.coverArt.setVisibility(View.VISIBLE);
	}

	public AlbumCoverDownloadListener(Context context, ImageView coverArt, ProgressBar coverArtProgress) {
		this.context = context;
		this.coverArt = coverArt;
		this.coverArt.setVisibility(View.VISIBLE);
		this.coverArtProgress = coverArtProgress;
		this.coverArtProgress.setIndeterminate(true);
		this.coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
	}

	public void onCoverDownloaded(Bitmap cover) {
		if (coverArtProgress != null)
			coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
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
		coverArt.setImageResource(R.drawable.no_cover_art);
		freeCoverDrawable(null);
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
			coverArt.setImageResource(R.drawable.no_cover_art);

		coverDrawable.setCallback(null);
		final Bitmap coverBitmap = ((BitmapDrawable) coverDrawable).getBitmap();
		if (coverBitmap != null)
			coverBitmap.recycle();
	}

}
