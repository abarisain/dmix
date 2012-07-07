package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.SongsActivity;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.AlbumDataBinder;

public class AlbumsFragment extends BrowseFragment {
	private MPDApplication app;
	private String artist = "";

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
		app = (MPDApplication) getActivity().getApplication();
		registerForContextMenu(getListView());
		UpdateList();
		if (getActivity().getIntent().getStringExtra("artist") != null) {
			artist=getActivity().getIntent().getStringExtra("artist");
			setActivityTitle(artist, R.drawable.ic_tab_artists_selected);
		} else {
			getActivity().setTitle(getResources().getString(R.string.albums));
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), SongsActivity.class);
		intent.putExtra("album", items.get(position).getName());
		intent.putExtra("artist", getActivity().getIntent().getStringExtra("artist"));
		startActivityForResult(intent, -1);
	}
	
	@Override
	protected ListAdapter getCustomListAdapter() {
		if(items != null) {
			return new ArrayIndexerAdapter(getActivity(), new AlbumDataBinder(), items);
		}
		return super.getCustomListAdapter();
	}

	@Override
	protected void asyncUpdate() {
		try {
			items = app.oMPDAsyncHelper.oMPD.getAlbums(getActivity().getIntent().getStringExtra("artist"));
		} catch (MPDServerException e) {
		}
	}

    @Override
    protected void Add(Item item) {
    	try {
    		app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(app.oMPDAsyncHelper.oMPD.getSongs(artist, item.getName()));
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
    }

}
