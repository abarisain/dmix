package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Item;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;
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
		coverHelper.downloadCover(artist, album, null, null);
	}

}
