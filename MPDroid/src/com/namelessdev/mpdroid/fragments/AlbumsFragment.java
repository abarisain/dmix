package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.AlbumDataBinder;

public class AlbumsFragment extends BrowseFragment {
	private static final String EXTRA_ARTIST = "artist";
	protected Artist artist = null;

	public AlbumsFragment() {
		super(R.string.addAlbum, R.string.albumAdded, MPDCommand.MPD_SEARCH_ALBUM);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null)
			init((Artist) icicle.getParcelable(EXTRA_ARTIST));
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingAlbums;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(EXTRA_ARTIST, artist);
		super.onSaveInstanceState(outState);
	}

	public AlbumsFragment init(Artist a) {
		artist = a;
		return this;
	}

	@Override
	public String getTitle() {
		if (artist != null) {
			return artist.getName();
		} else {
			return getString(R.string.albums);
		}
	}

	@Override
	public void onItemClick(AdapterView adapterView, View v, int position, long id) {
		((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new SongsFragment().init(artist, (Album) items.get(position)),
				"songs");
	}
	
	@Override
	protected ListAdapter getCustomListAdapter() {
		if(items != null) {
			return new ArrayIndexerAdapter(getActivity(),
					new AlbumDataBinder(app, artist == null ? null : artist.getName(), app.isLightThemeSelected()), items);
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
	protected void add(Item item, boolean replace, boolean play) {
		try {
			app.oMPDAsyncHelper.oMPD.add((Album) item, replace, play);
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void add(Item item, String playlist) {
		try {
			app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, artist, ((Album) item));
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
}
