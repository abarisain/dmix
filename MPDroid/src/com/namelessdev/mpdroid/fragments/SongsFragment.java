package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

public class SongsFragment extends BrowseFragment {

	String album = "";
	String artist = "";

	public SongsFragment() {
		super(R.string.addSong, R.string.songAdded, MPD.MPD_SEARCH_TITLE);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingSongs;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(getListView());
		album = (String) this.getActivity().getIntent().getStringExtra("album");
		artist = (String) this.getActivity().getIntent().getStringExtra("artist");
		UpdateList();

		setActivityTitle(album, R.drawable.ic_tab_albums_selected);

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Add(items.get(position));
	}

	@Override
	protected void Add(Item item) {
		Music music = (Music)item;
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
			Tools.notifyUser(String.format(getResources().getString(R.string.songAdded, music.getTitle()), music.getName()),
					getActivity());
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			items = app.oMPDAsyncHelper.oMPD.getSongs(artist, album);
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
}
