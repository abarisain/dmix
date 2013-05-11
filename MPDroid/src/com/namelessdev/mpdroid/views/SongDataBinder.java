package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.SongViewHolder;

public class SongDataBinder implements ArrayIndexerDataBinder {

	boolean showArtist;
	
	public SongDataBinder() {
		showArtist = false;
	}
	
	public SongDataBinder(boolean showArtist) {
		this.showArtist = showArtist;
	}
	
	public void onDataBind(final Context context, final View targetView, final AbstractViewHolder viewHolder, List<? extends Item> items, Object item, int position) {
		SongViewHolder holder = (SongViewHolder) viewHolder;

		final Music song = (Music) item;
		int trackNumber = song.getTrack();
		if(trackNumber < 0)
			trackNumber = 0;

		holder.trackTitle.setText(song.getTitle());
		holder.trackNumber.setText(trackNumber < 10 ? "0" + Integer.toString(trackNumber) : Integer.toString(trackNumber));
		holder.trackDuration.setText(song.getFormatedTime());

		if(showArtist)
			holder.trackArtist.setText(song.getArtist());
	}

	public boolean isEnabled(int position, List<? extends Item> items, Object item) {
		return true;
	}

	@Override
	public int getLayoutId() {
		return R.layout.song_list_item;
	}

	@Override
	public View onLayoutInflation(Context context, View targetView, List<? extends Item> items) {
		targetView.findViewById(R.id.track_artist).setVisibility(showArtist ? View.VISIBLE : View.GONE);
		return targetView;
	}

	@Override
	public AbstractViewHolder findInnerViews(View targetView) {
		// look up all references to inner views
		SongViewHolder viewHolder = new SongViewHolder();
		viewHolder.trackTitle = (TextView) targetView.findViewById(R.id.track_title);
		viewHolder.trackNumber = (TextView) targetView.findViewById(R.id.track_number);
		viewHolder.trackDuration = (TextView) targetView.findViewById(R.id.track_duration);
		viewHolder.trackArtist = (TextView) targetView.findViewById(R.id.track_artist);
		return viewHolder;
	}

}
