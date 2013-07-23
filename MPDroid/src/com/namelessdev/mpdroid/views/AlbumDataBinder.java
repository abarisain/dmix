package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Album;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

public class AlbumDataBinder extends BaseDataBinder {
	String artist = null;

	public AlbumDataBinder(MPDApplication app, String artist, boolean isLightTheme) {
		super(app, isLightTheme);
		this.artist = artist;
	}

	public void onDataBind(final Context context, final View targetView, final AbstractViewHolder viewHolder, List<? extends Item> items,
			Object item, int position) {
		AlbumViewHolder holder = (AlbumViewHolder) viewHolder;

		final Album album = (Album) item;
		String info = "";
		final long songCount = album.getSongCount();
		if (album.getYear() > 0)
			info = Long.toString(album.getYear());
		if (songCount > 0) {
			if (info != null && info.length() > 0)
				info += " - ";
			info += String.format(context.getString(songCount > 1 ? R.string.tracksInfoHeaderPlural : R.string.tracksInfoHeader),
					songCount, Music.timeToString(album.getDuration()));
		}
		holder.albumName.setText(album.getName());
		if (info != null && info.length() > 0) {
			holder.albumInfo.setVisibility(View.VISIBLE);
			holder.albumInfo.setText(info);
		} else {
			holder.albumInfo.setVisibility(View.GONE);
		}

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

		final CoverAsyncHelper coverHelper = new CoverAsyncHelper(app, settings);
		final int height = holder.albumCover.getHeight();
		// If the list is not displayed yet, the height is 0. This is a problem, so set a fallback one.
		coverHelper.setCoverMaxSize(height == 0 ? 128 : height);

		loadPlaceholder(coverHelper);

		// display cover art in album listing if caching is on
		if (this.artist != null && enableCache) {
			// listen for new artwork to be loaded
			final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(context, holder.albumCover, lightTheme);
			final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) holder.albumCover
					.getTag(R.id.AlbumCoverDownloadListener);
			if (oldAcd != null) {
				oldAcd.detach();
			}

			holder.albumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
			coverHelper.addCoverDownloadListener(acd);

			loadArtwork(coverHelper, artist, album.getName());
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
		targetView.findViewById(R.id.albumCover).setVisibility(enableCache ? View.VISIBLE : View.GONE);
		return targetView;
	}

	@Override
	public AbstractViewHolder findInnerViews(View targetView) {
		// look up all references to inner views
		AlbumViewHolder viewHolder = new AlbumViewHolder();
		viewHolder.albumName = (TextView) targetView.findViewById(R.id.album_name);
		viewHolder.albumInfo = (TextView) targetView.findViewById(R.id.album_info);
		viewHolder.albumCover = (ImageView) targetView.findViewById(R.id.albumCover);
		return viewHolder;
	}
}
