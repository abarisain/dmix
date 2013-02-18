package com.namelessdev.mpdroid.library;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.R.id;
import com.namelessdev.mpdroid.R.layout;
import com.namelessdev.mpdroid.R.menu;

public class StoredPlaylistActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.stored_playlist_activity);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.mpd_searchmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_search:
			this.onSearchRequested();
			return true;
		case android.R.id.home:
			final Intent i = new Intent(this, MainMenuActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		}
		return false;
	}
}