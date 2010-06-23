package org.pmix.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class BrowseActivity extends ListActivity {

	public static final int MAIN = 0;
	public static final int PLAYLIST = 3;

	public BrowseActivity() {
		super();
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.unsetActivity(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0,MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0,PLAYLIST, 1, R.string.playlist).setIcon(R.drawable.ic_menu_pmix_playlist);
		
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		Intent i = null;
		
		switch (item.getItemId()) {
	
		case MAIN:
			i = new Intent(this, MainMenuActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		case PLAYLIST:
			i = new Intent(this, PlaylistActivity.class);
			startActivityForResult(i, PLAYLIST);
			return true;
		}
		return false;
	}

}