package com.namelessdev.mpdroid;

import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PlaylistManagerActivity extends BrowseActivity implements OnMenuItemClickListener {

	static final int DELETE = 2;

	public PlaylistManagerActivity() {
		super(R.string.addPlaylist, R.string.playlistAdded, MPD.MPD_SEARCH_ALBUM);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		//MPDApplication app = (MPDApplication) getApplication();
		setContentView(R.layout.artists);

		ListView list = getListView();
		registerForContextMenu(list);

		UpdateList();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Doesn't feel right to make it that easy to add an compleate playlist
		// Add(position);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, DELETE, 0, R.string.deletePlaylist);
		addAndReplaceItem.setOnMenuItemClickListener(this);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case DELETE:
			try {
				MPDApplication app = (MPDApplication) getApplication();
				String playlist = items.get((int) info.id).toString();
				app.oMPDAsyncHelper.oMPD.deletePlaylist(playlist);
				MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.playlistDeleted), playlist), this);
				items.remove((int) info.id);
				updateFromItems();
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		}
		return super.onMenuItemClick(item);
	}

	@Override
	protected void Add(String item) {
		Add(items.indexOf(item));
	}

	protected void Add(int index) {
		String playlist = items.get(index);
		try {
			MPDApplication app = (MPDApplication) getApplication();

			app.oMPDAsyncHelper.oMPD.getPlaylist().load(playlist);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.playlistAdded), playlist), this);
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * An Update function for what is displayed on the screen
	 */
	public void asyncUpdate() {
		MPDApplication app = (MPDApplication) getApplicationContext();
		try {
			items = (List<String>) (app.oMPDAsyncHelper.oMPD.getPlaylists());
		} catch (MPDServerException e) {
		}
	}

}
