package org.musicpd.android.views;

import java.util.List;

import org.a0z.mpd.Artist;
import org.a0z.mpd.Album;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.util.DisplayMetrics;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.View;

import org.musicpd.android.MPDApplication;
import org.musicpd.android.R;
import org.musicpd.android.cover.CachedCover;
import org.musicpd.android.helpers.CoverAsyncHelper;
import org.musicpd.android.helpers.AlbumCoverDownloadListener;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.Tools;
import org.musicpd.android.views.holders.AbstractViewHolder;
import org.musicpd.android.views.holders.AlbumViewHolder;

public class AlbumGridDataBinder extends AlbumDataBinder {

	public AlbumGridDataBinder(MPDApplication app, boolean isLightTheme) {
		super(app, null, isLightTheme);
	}

	@Override
	public void onDataBind(final Context context, final View targetView, final AbstractViewHolder viewHolder, List<? extends Item> items, Object item, int position) {
		AlbumViewHolder holder = (AlbumViewHolder) viewHolder;

		final Album album = (Album) item;

		// Caching must be switch on to use this view
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
		coverHelper = new CoverAsyncHelper(app, settings);
		coverHelper.setCoverRetrieversFromPreferences();

		// display the album title
		holder.albumName.setText(album.getName());

		// display message if wifi off
		if(coverHelper.isWifi() == false) {
			try {
				Tools.notifyUser(context.getResources().getString(R.string.albumGridWifiUnavailable), context);
			} catch (Exception e) {
				Log.w(e);
			}
		}

		// listen for new artwork to be loaded
		coverHelper.addCoverDownloadListener(new AlbumCoverDownloadListener(context, holder.albumCover));

		// Can't get artwork for missing album name
		if((!album.getName().equals("")) && (!album.getName().equals("-"))) {
			loadArtwork(null, album.getName());
		}
	}

	@Override
	public int getLayoutId() {
		return R.layout.album_grid_item;
	}

}
