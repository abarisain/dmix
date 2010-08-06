package com.namelessdev.mpdroid;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

public class ArtistsActivity extends BrowseActivity {
	private boolean albumartist;

	public ArtistsActivity() {
		super(R.string.addArtist, R.string.artistAdded, MPD.MPD_SEARCH_ARTIST);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.artists);
		pd = ProgressDialog.show(ArtistsActivity.this, getResources().getString(R.string.loading), getResources().getString(
				R.string.loadingArtists));

		// load preferences for album artist tag display option
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		albumartist = settings.getBoolean("albumartist", false);

		registerForContextMenu(getListView());

		UpdateList();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, AlbumsActivity.class);
		intent.putExtra("artist", items.get(position));
		startActivityForResult(intent, -1);
	}

	@Override
	protected void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getApplication();
			if (albumartist == true) {
				items = app.oMPDAsyncHelper.oMPD.listAlbumArtists();
			} else {
				items = app.oMPDAsyncHelper.oMPD.listArtists();
			}
		} catch (MPDServerException e) {
		}
	}
}
