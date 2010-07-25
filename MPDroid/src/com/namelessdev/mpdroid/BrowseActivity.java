package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.R.drawable;
import com.namelessdev.mpdroid.R.string;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class BrowseActivity extends ListActivity implements OnMenuItemClickListener{

	public static final int MAIN = 0;
	public static final int PLAYLIST = 3;
	protected List<String> items;
	
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

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(items.get((int)info.id).toString());
		MenuItem addItem = menu.add(ContextMenu.NONE, 0, 0, R.string.addAlbum);
		addItem.setOnMenuItemClickListener(this);
		
		MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, 1, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
    }

	protected void Add(String item) {
		try {
			MPDApplication app = (MPDApplication)getApplication();
			ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, item));
			app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),item), this);
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case 1:
			try {
				MPDApplication app = (MPDApplication)getApplication();
				String status = app.oMPDAsyncHelper.oMPD.getStatus().getState();
				app.oMPDAsyncHelper.oMPD.stop();
				app.oMPDAsyncHelper.oMPD.getPlaylist().clear();
				
				Add(items.get((int)info.id).toString());
				if ( status.equals(MPDStatus.MPD_STATE_PLAYING) ) {
					app.oMPDAsyncHelper.oMPD.play();
				}
				// TODO Need to find some way of updating the main view here.
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

			break;
		case 0:
			Add(items.get((int)info.id).toString());
			break;
			
		}
		return false;
	}
    
}