package com.namelessdev.mpdroid;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ListView;

public class PlaylistManagerActivity extends ListActivity implements OnMenuItemClickListener{

	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		MPDApplication app = (MPDApplication) getApplication();
		setContentView(R.layout.artists);
		
		ListView list = getListView();
		registerForContextMenu(list);
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		return false;
	}

}
