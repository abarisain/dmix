package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;

public class SongDataBinder implements ArrayIndexerDataBinder {

	boolean showArtist;
	
	public SongDataBinder() {
		showArtist = false;
	}
	
	public SongDataBinder(boolean showArtist) {
		this.showArtist = showArtist;
	}
	
	public void onDataBind(Context context, View targetView, List<? extends Item> items, Object item, int position) {
		final Music song = (Music) item;
		int trackNumber = song.getTrack();
		if(trackNumber < 0)
			trackNumber = 0;
		((TextView) targetView.findViewById(R.id.track_title)).setText(song.getTitle());
		((TextView) targetView.findViewById(R.id.track_number)).setText(trackNumber < 10 ? "0" + Integer.toString(trackNumber) : Integer.toString(trackNumber));
		((TextView) targetView.findViewById(R.id.track_duration)).setText(song.getFormatedTime());
		if(showArtist)
			((TextView) targetView.findViewById(R.id.track_artist)).setText(song.getArtist());
	}

	public boolean isEnabled(int position, List<? extends Item> items, Object item) {
		return true;
	}

	@Override
	public int getLayoutId() {
		return R.layout.song_list_item;
	}

	@Override
	public void onLayoutInflation(Context context, View targetView, List<? extends Item> items) {
		targetView.findViewById(R.id.track_artist).setVisibility(showArtist ? View.VISIBLE : View.GONE);
	}

}
