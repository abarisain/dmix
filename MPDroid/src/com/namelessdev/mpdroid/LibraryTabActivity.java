package com.namelessdev.mpdroid;

import android.app.ActionBar;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;

public class LibraryTabActivity extends TabActivity {
	private com.namelessdev.mpdroid.ActionBar compatActionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (!MPDApplication.isHoneycombOrBetter()) {
			setTheme(android.R.style.Theme_Black_NoTitleBar);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_tabs);
		final View tmpView = findViewById(R.id.compatActionbar);
		if (tmpView != null) {
			// We are on a phone
			compatActionBar = (com.namelessdev.mpdroid.ActionBar) tmpView;
			compatActionBar.setTitle(R.string.libraryTabActivity);
			compatActionBar.setBackActionEnabled(true);
		}

		Resources res = getResources();
		TabHost tabHost = getTabHost();

		tabHost.addTab(tabHost.newTabSpec("tab_artist").setIndicator(this.getString(R.string.artists),
				res.getDrawable(R.drawable.ic_tab_artists)).setContent(new Intent(LibraryTabActivity.this, ArtistsActivity.class)));
		tabHost.addTab(tabHost.newTabSpec("tab_album").setIndicator(this.getString(R.string.albums),
				res.getDrawable(R.drawable.ic_tab_albums)).setContent(new Intent(LibraryTabActivity.this, AlbumsActivity.class)));
		tabHost.addTab(tabHost.newTabSpec("tab_songs").setIndicator(this.getString(R.string.songs), res.getDrawable(R.drawable.ic_tab_songs))
				.setContent(new Intent(LibraryTabActivity.this, SongSearchMessage.class)));
		tabHost.addTab(tabHost.newTabSpec("tab_files").setIndicator(this.getString(R.string.files),
				res.getDrawable(R.drawable.ic_tab_playlists)).setContent(new Intent(LibraryTabActivity.this, FSActivity.class)));
		((MPDApplication) getApplication()).setActivity(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		try {
			ActionBar actionBar = this.getActionBar();
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
		} catch (NoClassDefFoundError e) {
			// Older android
		} catch (NullPointerException e) {

		} catch (NoSuchMethodError e) {

		}
	}

	@Override
	protected void onDestroy() {
		((MPDApplication) getApplication()).setActivity(this);
		super.onDestroy();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}

}
