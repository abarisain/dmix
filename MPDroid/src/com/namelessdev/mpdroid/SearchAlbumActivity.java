package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDServerException;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class SearchAlbumActivity extends BrowseActivity {
	private List<String> musicList = null;
	String searchKeywords = "";

	public SearchAlbumActivity() {
		super(R.string.addAlbum, R.string.albumAdded, MPD.MPD_SEARCH_ALBUM);
		items = new ArrayList<String>();
		musicList = new ArrayList<String>();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();

		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY);
		} else {
			return; // Bye !
		}
		setActivityTitle(getTitle() + " : " + searchKeywords);

		registerForContextMenu(getListView());

		UpdateList();
	}

	public int getLoadingText() {
		return R.string.loadingAlbums;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, SongsActivity.class);
		intent.putExtra("album", items.get(position));
		startActivityForResult(intent, -1);
	}

	@Override
	protected void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getApplication();
			musicList = app.oMPDAsyncHelper.oMPD.listAlbums();
		} catch (MPDServerException e) {
		}

		searchKeywords = searchKeywords.toLowerCase().trim();
		for (String music : musicList) {
			if (music.toLowerCase().contains(searchKeywords))
				items.add(music);
		}
	}
}
