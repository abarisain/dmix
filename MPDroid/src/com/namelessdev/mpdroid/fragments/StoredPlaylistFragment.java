package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.LibraryTabActivity;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;

public class StoredPlaylistFragment extends SherlockListFragment {
	private static final String EXTRA_PLAYLIST_NAME = "playlist";

	private ArrayList<HashMap<String, Object>> songlist;
	private List<Music> musics;

	private String playlistName;
	private MPDApplication app;

	public StoredPlaylistFragment() {
		super();
		setHasOptionsMenu(true);
	}
	
	public StoredPlaylistFragment init(String name) {
		playlistName = name;
		return this;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null)
			init(icicle.getString(EXTRA_PLAYLIST_NAME));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_PLAYLIST_NAME, playlistName);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		app = (MPDApplication) getActivity().getApplication();
	}

	@Override
	public String toString() {
		if (playlistName != null) {
			return playlistName;
		} else {
			return getString(R.string.playlist);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.simple_list, container, false);
		registerForContextMenu((ListView) view.findViewById(android.R.id.list));
		return view;
	}
	
	protected void update() {
		try {
			musics = app.oMPDAsyncHelper.oMPD.getPlaylistSongs(playlistName);
			songlist = new ArrayList<HashMap<String, Object>>();
			for (Music m : musics) {
				if (m == null) {
					continue;
				}
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				item.put("artist", m.getArtist());
				item.put("title", m.getTitle());
				item.put("play", 0);
				songlist.add(item);
			}
			SimpleAdapter songs = new SimpleAdapter(getActivity(), songlist, R.layout.playlist_list_item,
					new String[] { "play", "title", "artist" }, new int[] { R.id.picture, android.R.id.text1, android.R.id.text2 });

			setListAdapter(songs);
		} catch (MPDServerException e) {
		}

	}
	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		update();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		android.view.MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.mpd_browsermenu, menu);

		//AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		// arrayListId = info.position;
		menu.setHeaderTitle(playlistName);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		return false;
	}

	/*
	 * Create Menu for Playlist View
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.mpd_storedplaylistmenu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Menu actions...
		Intent i;
		switch (item.getItemId()) {
		case R.id.PLM_EditPL:
			i = new Intent(getActivity(), PlaylistEditActivity.class);
			i.putExtra("playlist", playlistName);
			startActivity(i);
			return true;
		case R.id.GMM_LibTab:
			i = new Intent(getActivity(), LibraryTabActivity.class);
			startActivity(i);
		default:
			return false;
		}

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
	}
}