package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Album;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.MPDConnection;
import org.a0z.mpd.UnknownAlbum;

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
	private Artist artist = null;

	public AlbumsFragment() {
		super(R.string.addAlbum, R.string.albumAdded, MPDCommand.MPD_SEARCH_ALBUM);
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
		artist = getActivity().getIntent().getParcelableExtra("artist");
		if (artist != null) {
			setActivityTitle(artist.getName());
		} else {
			getActivity().setTitle(getResources().getString(R.string.albums));
		}
		UpdateList();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), SongsActivity.class);
		intent.putExtra("album", ((Album) items.get(position)));
		intent.putExtra("artist", artist);
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
			items = app.oMPDAsyncHelper.oMPD.getAlbums(artist);
		} catch (MPDServerException e) {
		}
	}

    @Override
    protected void Add(Item item) {
    	try {
			app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(app.oMPDAsyncHelper.oMPD.getSongs(artist, ((Album) item)));
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
    }

    @Override
    protected void Add(Item item, String playlist) {
    	try {
			app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, app.oMPDAsyncHelper.oMPD.getSongs(artist, ((Album) item)));
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
	}
}
