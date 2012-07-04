package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.tools.Tools;
public class BrowseActivity extends SherlockListActivity implements OnMenuItemClickListener, AsyncExecListener {

	protected int iJobID = -1;

	public static final int MAIN = 0;
	public static final int PLAYLIST = 3;

	public static final int ADD = 0;
	public static final int ADDNREPLACE = 1;
	public static final int ADDNPLAY = 2;

	protected List<String> items = null;

	protected View loadingView;
	protected TextView loadingTextView;
	protected View noResultView;
	
	String context;
	int irAdd, irAdded;

	public BrowseActivity(int rAdd, int rAdded, String pContext) {
		super();
		irAdd = rAdd;
		irAdded = rAdded;

		context = pContext;

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browse);
		loadingView = findViewById(R.id.loadingLayout);
		loadingTextView = (TextView) findViewById(R.id.loadingText);
		noResultView = findViewById(R.id.noResultLayout);
		loadingView.setVisibility(View.VISIBLE);
		loadingTextView.setText(getLoadingText());
	}

	public void setActivityTitle(String title) {
		setTitle(title);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	/*
	 * Override this to display a custom loading text
	 */
	public int getLoadingText() {
		return R.string.loading;
	}
	
	public void UpdateList() {
		MPDApplication app = (MPDApplication) getApplication();

		// Loading Artists asynchronous...
		app.oMPDAsyncHelper.addAsyncExecListener(this);
		iJobID = app.oMPDAsyncHelper.execAsync(new Runnable() {
			@Override
			public void run() {
				asyncUpdate();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.mpd_browsermenu, menu);
		inflater.inflate(R.menu.mpd_searchmenu, menu);
		/*
		 * boolean result = super.onCreateOptionsMenu(menu); menu.add(0, MAIN, 0,
		 * R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert); menu.add(0, PLAYLIST, 1,
		 * R.string.playlist).setIcon(R.drawable.ic_menu_pmix_playlist);
		 */

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = null;

		switch (item.getItemId()) {
		case R.id.BRM_mainmenu:
			i = new Intent(this, MainMenuActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		case R.id.menu_search:
			onSearchRequested();
			return true;
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(items.get((int) info.id).toString());
		android.view.MenuItem addItem = menu.add(ContextMenu.NONE, ADD, 0, getResources().getString(irAdd));
		addItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, ADDNREPLACE, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndPlayItem = menu.add(ContextMenu.NONE, ADDNPLAY, 0, R.string.addAndPlay);
		addAndPlayItem.setOnMenuItemClickListener(this);
	}

	protected void Add(String item) {
		try {
			MPDApplication app = (MPDApplication) getApplication();
			ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(context, item));
			app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(songs);
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), this);
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onMenuItemClick(android.view.MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final MPDApplication app = (MPDApplication) getApplication();
		switch (item.getItemId()) {
		case ADDNREPLACE:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						String status = app.oMPDAsyncHelper.oMPD.getStatus().getState();
						app.oMPDAsyncHelper.oMPD.stop();
						app.oMPDAsyncHelper.oMPD.getPlaylist().clear();

						Add(items.get((int) info.id).toString());
						if (status.equals(MPDStatus.MPD_STATE_PLAYING)) {
							app.oMPDAsyncHelper.oMPD.play();
						}
					} catch (MPDServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});
			break;
		case ADD:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					Add(items.get((int) info.id).toString());
				}
			});

			break;

		case ADDNPLAY:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						MPDPlaylist pl = app.oMPDAsyncHelper.oMPD.getPlaylist();
						int oldsize = pl.size();
						Add(items.get((int) info.id).toString());
						int id = pl.getByIndex(oldsize).getSongId();
						app.oMPDAsyncHelper.oMPD.skipToId(id);
						app.oMPDAsyncHelper.oMPD.play();
					} catch (MPDServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			break;
		}
		return false;
	}

	protected void asyncUpdate() {

	}

	/**
	 * Update the view from the items list if items is set.
	 */
	public void updateFromItems() {
		if (items != null) {
			//ListViewButtonAdapter<String> listAdapter = new ListViewButtonAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			ArrayIndexerAdapter<String> listAdapter = new ArrayIndexerAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			setListAdapter(listAdapter);
			try {
				getListView().setEmptyView(noResultView);
				loadingView.setVisibility(View.GONE);
			} catch (Exception e) {}
		}
	}

	@Override
	public void asyncExecSucceeded(int jobID) {
		if (iJobID == jobID) {
			updateFromItems();
		}
	}

}