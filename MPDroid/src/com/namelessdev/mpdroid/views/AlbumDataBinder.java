package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Artist;
import org.a0z.mpd.Album;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.CoverBitmapDrawable;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper.CoverDownloadListener;
import com.namelessdev.mpdroid.tools.Tools;

public class AlbumDataBinder implements ArrayIndexerDataBinder {
	CoverAsyncHelper coverHelper = null;
	Context context = null;
	String artist = null;
	MPDApplication app = null;
	boolean lightTheme = false;

	public AlbumDataBinder(MPDApplication app, String artist, boolean isLightTheme) {
		this.app = app;
		this.artist = artist;
		this.lightTheme = isLightTheme;
	}

	public void onDataBind(final Context context, final View targetView, List<? extends Item> items, Object item, int position) {
		this.context = context;
		final Album album = (Album) item;
		String info = "";
		final long songCount = album.getSongCount();
		if(album.getYear() > 0)
			info = Long.toString(album.getYear());
		if(songCount > 0) {
			if(info != null && info.length() > 0)
				info += " - ";
			info += String.format(context.getString(songCount > 1 ? R.string.tracksInfoHeaderPlural : R.string.tracksInfoHeader), songCount, Music.timeToString(album.getDuration()));
		}
		((TextView) targetView.findViewById(R.id.album_name)).setText(album.getName());
		final TextView albumInfo = (TextView) targetView.findViewById(R.id.album_info);
		if(info != null && info.length() > 0) {
			albumInfo.setVisibility(View.VISIBLE);
			albumInfo.setText(info);
		} else {
			albumInfo.setVisibility(View.GONE);
		}

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

		// display cover art in album listing if caching is on
		if (this.artist != null && settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true)) {
			coverHelper = new CoverAsyncHelper(app, settings);
			coverHelper.setCoverRetrieversFromPreferences();

			// listen for new artwork to be loaded
			ImageView albumCover = (ImageView) targetView.findViewById(R.id.albumCover);
			coverHelper.addCoverDownloadListener(new AlbumCoverDownloadListener(albumCover));

			loadArtwork(artist, album.getName());
		}
	}

	protected void loadArtwork(String artist, String album) {
		boolean haveCachedArtwork = false;

		try {
			CachedCover cachedCover = new CachedCover(app);
			final String[] urls = cachedCover.getCoverUrl(artist, album, null, null);
			if((urls != null) && (urls.length > 0)) {
				haveCachedArtwork = true;
			}
		} catch (Exception e) {
			// no cached artwork available
		}

		if(haveCachedArtwork == false && coverHelper.isWifi()) {
			// proactively download and cache artwork
			String filename = null;
			String path = null;
			List<? extends Item> songs = null;

			try {
				// load songs for this album
				songs = app.oMPDAsyncHelper.oMPD.getSongs(((artist != null) ? new Artist(artist, 0) : null), new Album(album));

				if (songs.size() > 0) {
					Music song = (Music) songs.get(0);
					filename = song.getFilename();
					path = song.getPath();
					coverHelper.downloadCover(artist, album, path, filename);
				}
			} catch (MPDServerException e) {
				// MPD error, bail on loading artwork
				return;
			}
		}else{
			coverHelper.downloadCover(artist, album, null, null);
		}
	}

	private class AlbumCoverDownloadListener implements CoverDownloadListener {
		ImageView albumCover;

		public AlbumCoverDownloadListener(ImageView albumCover) {
			this.albumCover = albumCover;
			this.albumCover.setVisibility(View.VISIBLE);
		}

		@Override
		public void onCoverDownloaded(Bitmap cover) {
			if (albumCover == null) {
				// The view is detached, bail.
				cover.recycle();
				return;
			}
			try {
				// recycle the placeholder
				final Drawable oldDrawable = albumCover.getDrawable();
				if (oldDrawable != null && oldDrawable instanceof CoverBitmapDrawable) {
					final Bitmap oldBitmap = ((CoverBitmapDrawable) oldDrawable).getBitmap();
					if (oldBitmap != null)
						oldBitmap.recycle();
				}

				if (cover != null) {
					BitmapDrawable myCover = new CoverBitmapDrawable(context.getResources(), cover);
					albumCover.setImageDrawable(myCover);
				} else {
					onCoverNotFound();
				}
			} catch (Exception e) {
				// Just ignore
			}
		}

		@Override
		public void onCoverNotFound() {
			// A placeholder is already shown so do nothing here
		}
	}

	public boolean isEnabled(int position, List<? extends Item> items, Object item) {
		return true;
	}

	@Override
	public int getLayoutId() {
		return R.layout.album_list_item;
	}

	@Override
	public View onLayoutInflation(Context context, View targetView, List<? extends Item> items) {
		targetView.findViewById(R.id.albumCover).setVisibility(coverHelper == null ? View.GONE : View.VISIBLE);
		return targetView;
	}

}
