package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Album;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import com.namelessdev.mpdroid.tools.Tools;

public class AlbumDataBinder implements ArrayIndexerDataBinder {
	CachedCover coverHelper = null;
	String artist = null;
	MPDApplication app = null;
	boolean lightTheme = false;

	public AlbumDataBinder(MPDApplication app, String artist, boolean isLightTheme) {
		this.app = app;
		this.artist = artist;
		this.lightTheme = isLightTheme;
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
		if (artist != null && settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true)) {
			coverHelper = new CachedCover(app);
		}
	}

	public void onDataBind(final Context context, final View targetView, List<? extends Item> items, Object item, int position) {
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
		if (coverHelper != null) {
			final ImageView albumCover = (ImageView) targetView.findViewById(R.id.albumCover);
			final Drawable oldDrawable = albumCover.getDrawable();
			albumCover.setImageResource(lightTheme ? R.drawable.no_cover_art_light : R.drawable.no_cover_art);
			if (oldDrawable != null && oldDrawable instanceof CoverBitmapDrawable) {
				final Bitmap oldBitmap = ((CoverBitmapDrawable) oldDrawable).getBitmap();
				if (oldBitmap != null)
					oldBitmap.recycle();
			}
			final Handler handler = new Handler();
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						final String[] urls = coverHelper.getCoverUrl(artist, album.getName(), null, null);
						if (urls != null && urls.length > 0) {
							int maxSize = albumCover.getLayoutParams().width;
							if (maxSize == 0) {
								// For some reason getWidth returned 0.
								// It shouldn't do that with layoutparams but whatever.
								// Use a fallback value. Better have a low quality image than nothing.
								maxSize = 96;
							}
							final Bitmap cover = Tools.decodeSampledBitmapFromPath(urls[0], maxSize, maxSize, false);
							if (cover != null) {
								handler.post(new Runnable() {
									@Override
									public void run() {
										final CoverBitmapDrawable myCover = new CoverBitmapDrawable(context.getResources(), cover);
										albumCover.setImageDrawable(myCover);
									}
								});
							}
						}
					} catch (Exception e) {
					}
				}
			}).start();
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
