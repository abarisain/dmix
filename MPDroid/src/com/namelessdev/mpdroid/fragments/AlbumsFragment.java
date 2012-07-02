package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.SongsActivity;

public class AlbumsFragment extends BrowseFragment {
	public AlbumsFragment() {
		super(R.string.addAlbum, R.string.albumAdded, MPD.MPD_SEARCH_ALBUM);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingAlbums;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(getListView());
		UpdateList();
		if (getActivity().getIntent().getStringExtra("artist") != null) {
			setActivityTitle((String) getActivity().getIntent().getStringExtra("artist"), R.drawable.ic_tab_artists_selected);
		} else {
			getActivity().setTitle(getResources().getString(R.string.albums));
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), SongsActivity.class);
		intent.putExtra("album", items.get(position));
		startActivityForResult(intent, -1);
	}

	@Override
	protected void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			if (getActivity().getIntent().getStringExtra("artist") != null) {
				items = app.oMPDAsyncHelper.oMPD.listAlbums((String) getActivity().getIntent().getStringExtra("artist"), true);
			} else {
				items = app.oMPDAsyncHelper.oMPD.listAlbums(true);
			}
			//Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
		} catch (MPDServerException e) {
		}
	}

}
