package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.AdapterView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

public class GenresFragment extends BrowseFragment {

	public GenresFragment() {
		super(R.string.addGenre, R.string.genreAdded, MPDCommand.MPD_SEARCH_GENRE);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingGenres;
	}

	@Override
	public String getTitle() {
		return getString(R.string.genres);
	}

	@Override
	public void onItemClick(AdapterView adapterView, View v, int position, long id) {
		((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new ArtistsFragment().init((Genre) items.get(position)), "artist");
	}

	@Override
	protected void asyncUpdate() {
		try {
			items = app.oMPDAsyncHelper.oMPD.getGenres();
		} catch (MPDServerException e) {
		}
	}

	@Override
	protected void add(Item item, boolean replace, boolean play) {
		try {
			app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(app.oMPDAsyncHelper.oMPD.find("genre", item.getName()));
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void add(Item item, String playlist) {
		try {
			app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, app.oMPDAsyncHelper.oMPD.find("genre", item.getName()));
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
}
