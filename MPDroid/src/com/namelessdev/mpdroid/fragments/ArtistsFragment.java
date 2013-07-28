package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;

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
	public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
		AlbumsFragment af;
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplication());
		if (settings.getBoolean(LibraryFragment.PREFERENCE_ALBUM_LIBRARY, false)) {
			af = new AlbumsGridFragment();
		} else {
			af = new AlbumsFragment();
		}
		af.init((Artist) items.get(position));
		((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(af, "album");
	}

	@Override
	protected void asyncUpdate() {
		try {
			if (genre != null) {
				items = app.oMPDAsyncHelper.oMPD.getArtists(genre);
			} else {
				items = app.oMPDAsyncHelper.oMPD.getArtists();
			}
		} catch (MPDServerException e) {
		}
	}

	@Override
	protected void add(Item item, boolean replace, boolean play) {
		try {
			app.oMPDAsyncHelper.oMPD.add((Artist) item, replace, play);
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void add(Item item, String playlist) {
		try {
			app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Artist) item);
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
}
