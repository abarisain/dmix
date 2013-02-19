package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Album;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.tools.Tools;

public class AlbumDataBinder implements ArrayIndexerDataBinder {
	CachedCover coverHelper = null;
	String artist = null;
	DisplayMetrics metrics = null;
	MPDApplication app = null;

	public AlbumDataBinder(MPDApplication app, Activity activity, String artist) {
		this.app = app;
		this.artist = artist;
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
		if(settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true)) {
			coverHelper = new CachedCover(app);
			metrics = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		}
	}

	public void onDataBind(Context context, View targetView, List<? extends Item> items, Object item, int position) {
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
			final Handler handler = new Handler();
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						final String[] urls = coverHelper.getCoverUrl(artist, album.getName(), null, null);
						if(urls == null || urls.length == 0) {
							handler.post(new Runnable() {
								@Override
								public void run() {
									albumCover.setImageResource(R.drawable.no_cover_art);
								}
							});
						} else {
							final int maxSize = albumCover.getHeight();
							final Bitmap cover = Tools.decodeSampledBitmapFromPath(urls[0], maxSize, maxSize, false);
							if (cover != null) {
								cover.setDensity((int) metrics.density);
								final BitmapDrawable myCover = new BitmapDrawable(app.getResources(), cover);
								handler.post(new Runnable() {
									@Override
									public void run() {
										albumCover.setImageDrawable(myCover);
									}
								});
							}
						}
					} catch (Exception e) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								albumCover.setImageResource(R.drawable.no_cover_art);
							}
						});
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
