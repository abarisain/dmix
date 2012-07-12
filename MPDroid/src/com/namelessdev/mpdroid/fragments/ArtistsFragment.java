package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDServerException;

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
	private boolean albumartist;

	public ArtistsFragment() {
		super(R.string.addArtist, R.string.artistAdded, MPD.MPD_SEARCH_ARTIST);
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
		UpdateList();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), AlbumsActivity.class);
		intent.putExtra("artist", items.get(position).getName());
		startActivityForResult(intent, -1);
	}

	@Override
	protected void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			items=app.oMPDAsyncHelper.oMPD.getArtists();
		} catch (MPDServerException e) {
		}
	}

    @Override
    protected void Add(Item item) {
    	try {
    		MPDApplication app = (MPDApplication) getActivity().getApplication();
    		app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(app.oMPDAsyncHelper.oMPD.getSongs(item.getName(), null));
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
    }
    
    @Override
    protected void Add(Item item, String playlist) {
    	try {
    		MPDApplication app = (MPDApplication) getActivity().getApplication();
    		app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, app.oMPDAsyncHelper.oMPD.getSongs(item.getName(), null));
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
	}
}
