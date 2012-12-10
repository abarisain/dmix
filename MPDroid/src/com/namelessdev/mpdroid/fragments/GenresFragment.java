package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.namelessdev.mpdroid.ArtistsActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

public class GenresFragment extends BrowseFragment {
	private MPDApplication app;

	public GenresFragment() {
		super(R.string.addGenre, R.string.genreAdded, MPDCommand.MPD_SEARCH_GENRE);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingGenres;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		app = (MPDApplication) getActivity().getApplication();
		registerForContextMenu(getListView());
		UpdateList();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), ArtistsActivity.class);
		intent.putExtra("genre", ((Genre) items.get(position)));
		startActivityForResult(intent, -1);
	}

	@Override
	protected void asyncUpdate() {
		try {
			items = app.oMPDAsyncHelper.oMPD.getGenres();
		} catch (MPDServerException e) {
		}
	}

	@Override
	protected void Add(Item item) {
		try {
			app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(app.oMPDAsyncHelper.oMPD.find("genre", item.getName()));
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void Add(Item item, String playlist) {
		try {
			app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, app.oMPDAsyncHelper.oMPD.find("genre", item.getName()));
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
}
