package org.musicpd.android.views;

import java.util.List;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.musicpd.android.R;
import org.musicpd.android.adapters.SeparatedListDataBinder;

public class SearchResultDataBinder implements SeparatedListDataBinder {

	public void onDataBind(Context context, View targetView, List<Object> items, Object item, int position) {
		final TextView text1 = (TextView) targetView.findViewById(R.id.text1);
		//final View separator = targetView.findViewById(R.id.separator_line);
		if(item instanceof Music) {
			text1.setText(((Music) item).getTitle());
		} else if(item instanceof Artist){
			text1.setText(((Artist) item).getName());
		} else if(item instanceof Album){
			text1.setText(((Album) item).getName());
		}
		
		/*if(position < (items.size() - 1) && (items.get(position + 1) instanceof String)) {
			separator.setVisibility(View.GONE);
		} else {
			separator.setVisibility(View.VISIBLE);
		}*/
	}

	public boolean isEnabled(int position, List<Object> items, Object item) {
		return true;
	}

}
