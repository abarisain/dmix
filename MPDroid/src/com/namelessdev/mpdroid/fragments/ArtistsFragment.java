package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

import com.namelessdev.mpdroid.AlbumsActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

public class ArtistsFragment extends BrowseFragment {
	private Genre genre = null;
	private boolean albumartist;

	public ArtistsFragment() {
		super(R.string.addArtist, R.string.artistAdded, MPDCommand.MPD_SEARCH_ARTIST);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// load preferences for album artist tag display option
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		albumartist = settings.getBoolean("albumartist", false);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingArtists;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(getListView());
		genre = getActivity().getIntent().getParcelableExtra("genre");
		if (genre != null) {
			setActivityTitle(genre.getName());
		} else {
			getActivity().setTitle(getResources().getString(R.string.artists));
		}
		UpdateList();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), AlbumsActivity.class);
		intent.putExtra("artist", ((Artist) items.get(position)));
		startActivityForResult(intent, -1);
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
