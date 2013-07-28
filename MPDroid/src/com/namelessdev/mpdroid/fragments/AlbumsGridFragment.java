package com.namelessdev.mpdroid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.views.AlbumGridDataBinder;

public class AlbumsGridFragment extends AlbumsFragment {
	public AlbumsGridFragment() {
		super();
		isCountPossiblyDisplayed = false;
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
