package com.namelessdev.mpdroid;

import java.util.Collections;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlbumsActivity extends BrowseActivity {
	public AlbumsActivity() {
		super(R.string.addAlbum, R.string.albumAdded, MPD.MPD_SEARCH_ALBUM);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.artists);
		pd = ProgressDialog.show(AlbumsActivity.this, getResources().getString(R.string.loading), getResources().getString(
				R.string.loadingAlbums));

		if (getIntent().getStringExtra("artist") != null) {
			setTitle((String) getIntent().getStringExtra("artist"));
			findViewById(R.id.header).setVisibility(View.VISIBLE);
			TextView title = (TextView) findViewById(R.id.headerText);
			title.setText(this.getTitle());
			ImageView icon = (ImageView) findViewById(R.id.headerIcon);
			icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_tab_artists_selected));
		} else {
			setTitle(getResources().getString(R.string.albums));
		}

		registerForContextMenu(getListView());

		UpdateList();
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
			if (getIntent().getStringExtra("artist") != null) {
				items = app.oMPDAsyncHelper.oMPD.listAlbums((String) getIntent().getStringExtra("artist"), true);
			} else {
				items = app.oMPDAsyncHelper.oMPD.listAlbums(true);
			}
			//Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
		} catch (MPDServerException e) {
		}
	}

}
