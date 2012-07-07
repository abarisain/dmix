package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Album;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;

public class AlbumDataBinder implements ArrayIndexerDataBinder {

	public void onDataBind(Context context, View targetView, List<? extends Item> items, Object item, int position) {
		final Album album = (Album) item;
		String info = null;
		final int songCount = album.getSongCount();
		if(songCount > 0) {
			info = String.format(context.getString(songCount > 1 ? R.string.tracksInfoHeaderPlural : R.string.tracksInfoHeader), songCount, Music.timeToString(album.getDuration()));
		}
		((TextView) targetView.findViewById(R.id.album_name)).setText(album.getName());
		final TextView albumInfo = (TextView) targetView.findViewById(R.id.album_info);
		if(info != null) {
			albumInfo.setVisibility(View.VISIBLE);
			albumInfo.setText(info);
		} else {
			albumInfo.setVisibility(View.GONE);
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
		return targetView;
	}

}
