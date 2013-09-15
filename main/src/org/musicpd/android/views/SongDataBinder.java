package org.musicpd.android.views;

import java.util.List;

import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.musicpd.android.R;
import org.musicpd.android.adapters.ArrayIndexerDataBinder;
import org.musicpd.android.views.holders.AbstractViewHolder;
import org.musicpd.android.views.holders.SongViewHolder;

public class SongDataBinder implements ArrayIndexerDataBinder {

	boolean showArtist;
	boolean showRelated;
	
	public SongDataBinder() {
		this(false, false);
	}
	
	public SongDataBinder(boolean showArtist, boolean showRelated) {
		this.showArtist = showArtist;
		this.showRelated = showRelated;
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
		
		targetView.setBackgroundColor(showRelated && song.getSelected()? highlight : lowlight);
	}

	public boolean isEnabled(int position, List<? extends Item> items, Object item) {
		return true; //((Music)items.get(position)).getSelected();
	}

	@Override
	public int getLayoutId() {
		return R.layout.song_list_item;
	}

	int highlight;
	int lowlight;
	
	@Override
	public View onLayoutInflation(Context context, View targetView, List<? extends Item> items) {
		highlight = context.getResources().getColor(R.color.highlighted_song_dark);
		lowlight = context.getResources().getColor(R.color.lowlighted_song_dark);
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
