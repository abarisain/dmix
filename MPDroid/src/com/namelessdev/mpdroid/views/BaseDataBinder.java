package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Item;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;

public abstract class BaseDataBinder implements ArrayIndexerDataBinder {

	MPDApplication app = null;
	boolean lightTheme = false;
	boolean enableCache = true;
	boolean onlyDownloadOnWifi = true;

	public BaseDataBinder(MPDApplication app, boolean isLightTheme) {
		this.app = app;
		this.lightTheme = isLightTheme;
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

		enableCache = settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true);
		onlyDownloadOnWifi = settings.getBoolean(CoverAsyncHelper.PREFERENCE_ONLY_WIFI, false);
	}

	@Override
	public abstract View onLayoutInflation(Context context, View targetView, List<? extends Item> items);

	@Override
	public abstract void onDataBind(Context context, View targetView, AbstractViewHolder viewHolder, List<? extends Item> items,
			Object item,
			int position);

	@Override
	public abstract AbstractViewHolder findInnerViews(View targetView);

	@Override
	public abstract boolean isEnabled(int position, List<? extends Item> items, Object item);

	@Override
	public abstract int getLayoutId();

	protected void loadPlaceholder(CoverAsyncHelper coverHelper) {
		coverHelper.obtainMessage(CoverAsyncHelper.EVENT_COVERNOTFOUND).sendToTarget();
	}

	protected void loadArtwork(CoverAsyncHelper coverHelper, String artist, String album) {
		// Don't forget to mirror your changes in StoredPlaylistDataBinder, haven't found a nice way to factor this
		boolean haveCachedArtwork = false;

		if (enableCache) {
			try {
				CachedCover cachedCover = new CachedCover(app);
				final String[] urls = cachedCover.getCoverUrl(artist, album, null, null);
				if ((urls != null) && (urls.length > 0)) {
					haveCachedArtwork = true;
				}
			} catch (Exception e) {
				// no cached artwork available
			}
		}

		// Did we find a cached cover ? If yes, skip the download
		// Only continue if we are not on WiFi and Cellular download is enabled
		if (!haveCachedArtwork) {
			// If we are not on WiFi and Cellular download is enabled
			if (coverHelper.isWifi() || !onlyDownloadOnWifi) {
				coverHelper.downloadCover(null, album, null, null);
			}
		} else {
			coverHelper.downloadCover(artist, album, null, null);
		}
	}

}
