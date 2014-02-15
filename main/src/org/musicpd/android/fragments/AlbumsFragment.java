package org.musicpd.android.fragments;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView;

import org.musicpd.android.R;
import org.musicpd.android.adapters.ArrayIndexerAdapter;
import org.musicpd.android.library.ILibraryFragmentActivity;
import org.musicpd.android.tools.AlbumGroup;
import org.musicpd.android.tools.AlbumGroups;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.Tools;
import org.musicpd.android.views.AlbumDataBinder;

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
		Album album = (Album)lookup(position);
		if (album instanceof AlbumGroup)
			((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new AlbumGroupFragment().init(artist, (AlbumGroup)album),
				"albumgroup");
		else
			((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new SongsFragment().init(artist, album),
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

	protected static final java.util.HashMap<Artist, java.util.List<Album>> albumCache = new java.util.HashMap<Artist, java.util.List<Album>>();
	protected static final Artist none = new Artist("", 0);
	
	@Override
	protected void asyncUpdate() {
		if (artist == null)
			Log.w("Listing all albums");
		try {
			if ((items = albumCache.get(artist == null? none : artist)) == null) {
				java.util.List<Album> albums = app.oMPDAsyncHelper.oMPD.getAlbums(artist);
				if (artist != null)
					albums = AlbumGroups.items(albums, albums.size() > 200);
				albumCache.put(artist, albums);
				items = albums;
			}
		} catch (MPDServerException e) {
			Log.w(e);
		} catch (Exception e) {
			Log.e(e);
		}
	}

	@Override
	protected void add(Item item, boolean replace, boolean play) {
		try {
			app.oMPDAsyncHelper.oMPD.add((Album) item, replace, play);
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (Exception e) {
			Log.w(e);
		}
	}

	@Override
	protected void add(Item item, String playlist) {
		try {
			app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, artist, ((Album) item));
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (Exception e) {
			Log.w(e);
		}
	}
}
