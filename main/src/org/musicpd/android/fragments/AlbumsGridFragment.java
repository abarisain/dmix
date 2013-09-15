package org.musicpd.android.fragments;

import org.a0z.mpd.MPDCommand;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ListAdapter;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.GridView;

import org.musicpd.android.R;
import org.musicpd.android.adapters.ArrayIndexerAdapter;
import org.musicpd.android.views.AlbumGridDataBinder;

public class AlbumsGridFragment extends AlbumsFragment {
	public AlbumsGridFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.browsegrid, container, false);
		list = (GridView) view.findViewById(R.id.grid);
		registerForContextMenu(list);
		list.setOnItemClickListener(this);
		loadingView = view.findViewById(R.id.loadingLayout);
		loadingTextView = (TextView) view.findViewById(R.id.loadingText);
		noResultView = view.findViewById(R.id.noResultLayout);
		loadingTextView.setText(getLoadingText());
		return view;
	}

	@Override
	protected ListAdapter getCustomListAdapter() {
		if(items != null) {
			return new ArrayIndexerAdapter(getActivity(),
					new AlbumGridDataBinder(app, app.isLightThemeSelected()), items);
		}
		return super.getCustomListAdapter();
	}

}
