package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

import android.view.View;
import android.widget.ListView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

public class ArtistsFragment extends BrowseFragment {
	private Genre genre = null;

	public ArtistsFragment() {
		super(R.string.addArtist, R.string.artistAdded, MPDCommand.MPD_SEARCH_ARTIST);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingArtists;
	}

	public ArtistsFragment init(Genre g) {
		genre = g;
		return this;
	}

	@Override
	public String getTitle() {
		if (genre != null) {
			return genre.getName();
		} else {
			return getString(R.string.genres);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new AlbumsFragment().init((Artist) items.get(position)), "album");
	}

	@Override
	protected void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			if (genre != null) {
				items = app.oMPDAsyncHelper.oMPD.getArtists(genre);
			} else {
				items = app.oMPDAsyncHelper.oMPD.getArtists();
			}
		} catch (MPDServerException e) {
		}
	}

    @Override
    protected void Add(Item item) {
    	try {
    		MPDApplication app = (MPDApplication) getActivity().getApplication();
    		app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(app.oMPDAsyncHelper.oMPD.getSongs(((Artist) item), null));
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
    }
    
    @Override
    protected void Add(Item item, String playlist) {
    	try {
    		MPDApplication app = (MPDApplication) getActivity().getApplication();
    		app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, app.oMPDAsyncHelper.oMPD.getSongs(((Artist) item), null));
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
	}
}
