package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.Collections;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SearchResultDataBinder;

public class SearchActivity extends SherlockListActivity implements OnMenuItemClickListener, AsyncExecListener {
	public static final int MAIN = 0;
	public static final int PLAYLIST = 3;

	public static final int ADD = 0;
	public static final int ADDNREPLACE = 1;
	public static final int ADDNREPLACEPLAY = 3;
	public static final int ADDNPLAY = 2;
	
	private MPDApplication app;
	private ArrayList<Object> arrayResults;
	
	protected int iJobID = -1;
	protected View loadingView;
	protected TextView loadingTextView;
	protected View noResultView;

	private int addString, addedString;
	private String searchKeywords = "";

	public SearchActivity() {
		addString = R.string.addSong;
		addedString = R.string.songAdded;
		arrayResults = new ArrayList<Object>();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (MPDApplication) getApplication();
		
		setContentView(R.layout.browse);
		loadingView = findViewById(R.id.loadingLayout);
		loadingTextView = (TextView) findViewById(R.id.loadingText);
		noResultView = findViewById(R.id.noResultLayout);
		loadingView.setVisibility(View.VISIBLE);
		loadingTextView.setText(R.string.loading);
		
		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();

		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY).trim();
		} else {
			return; // Bye !
		}

		setTitle(getTitle() + " : " + searchKeywords);

		registerForContextMenu(getListView());
		updateList();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
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
	
	private String getItemName(Object o) {
		if(o instanceof Music) {
			return ((Music) o).getTitle();
		} else if(o instanceof Artist) {
			return ((Artist) o).getName();
		} else if(o instanceof Album) {
			return ((Album) o).getName();
		}
		return "";
	}

	private void setContextForObject(Object object) {
		if(object instanceof Music) {
			addString = R.string.addSong;
			addedString = R.string.songAdded;
		} else if (object instanceof Artist) {
			addString = R.string.addArtist;
			addedString = R.string.artistAdded;
		} else if (object instanceof Album) {
			addString = R.string.addAlbum;
			addedString = R.string.albumAdded;
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Object selectedItem = l.getAdapter().getItem(position);
		if(selectedItem instanceof Music) {
			add((Music) selectedItem);
		} else if(selectedItem instanceof Artist) {
			Intent intent = new Intent(this, AlbumsActivity.class);
			intent.putExtra("artist", ((Artist) selectedItem));
			startActivityForResult(intent, -1);
		} else if(selectedItem instanceof Album) {
			Intent intent = new Intent(this, SongsActivity.class);
			intent.putExtra("album", ((Album) selectedItem));
			startActivityForResult(intent, -1);
		}
	}

	protected void add(Object object) {
		setContextForObject(object);
		if(object instanceof Music) {
			add((Music) object);
		} else if (object instanceof Artist) {
			add(((Artist) object), null);
		} else if (object instanceof Album) {
			add(null, ((Album) object));
		}
	}
	
	protected void add(Artist artist, Album album) {
		try {
			MPDApplication app = (MPDApplication) getApplication();
			ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.getSongs(artist, album));
			app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(songs);
			Tools.notifyUser(String.format(getResources().getString(addedString), null == album ? artist.getName() : (null == artist ? album.getName() : artist.getName() + " - " + album.getName())), this);
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
	
	protected void add(Music music) {
		try {
			app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
			Tools.notifyUser(String.format(getResources().getString(R.string.songAdded, music.getTitle()), music.getName()), this);
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateList() {
		app.oMPDAsyncHelper.addAsyncExecListener(this);
		iJobID = app.oMPDAsyncHelper.execAsync(new Runnable() {
			@Override
			public void run() {
				asyncUpdate();
			}
		});
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final Object item = arrayResults.get((int) info.id);
		menu.setHeaderTitle(getItemName(item));
		setContextForObject(item);
		android.view.MenuItem addItem = menu.add(ContextMenu.NONE, ADD, 0, getResources().getString(addString));
		addItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, ADDNREPLACE, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplacePlayItem = menu.add(ContextMenu.NONE, ADDNREPLACEPLAY, 0, R.string.addAndReplacePlay);
		addAndReplacePlayItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndPlayItem = menu.add(ContextMenu.NONE, ADDNPLAY, 0, R.string.addAndPlay);
		addAndPlayItem.setOnMenuItemClickListener(this);
	}
	
	@Override
	public boolean onMenuItemClick(final android.view.MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final MPDApplication app = (MPDApplication) getApplication();
		final Object selectedItem = arrayResults.get((int) info.id);
		switch (item.getItemId()) {
		case ADDNREPLACEPLAY:
		case ADDNREPLACE:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						String status = app.oMPDAsyncHelper.oMPD.getStatus().getState();
						app.oMPDAsyncHelper.oMPD.stop();
						app.oMPDAsyncHelper.oMPD.getPlaylist().clear();

						add(selectedItem);
						if (status.equals(MPDStatus.MPD_STATE_PLAYING) || item.getItemId() == ADDNREPLACEPLAY) {
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
					add(selectedItem);
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
						add(selectedItem);
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
		final String finalsearch = this.searchKeywords.toLowerCase();

		ArrayList<Music> arrayMusic = null;
		
		try {
			arrayMusic = (ArrayList<Music>) app.oMPDAsyncHelper.oMPD.search("any", finalsearch);
		} catch (MPDServerException e) {
		}

		final ArrayList<Music> musicItems = new ArrayList<Music>();
		final ArrayList<Artist> artistItems = new ArrayList<Artist>();
		final ArrayList<Album> albumItems = new ArrayList<Album>();
		String tmpValue;
		boolean valueFound;
		for (Music music : arrayMusic) {
			if(music.getTitle() != null &&  music.getTitle().toLowerCase().contains(finalsearch)) {
				musicItems.add(music);
			}
			valueFound = false;
			tmpValue = music.getArtist();
			if(tmpValue != null && tmpValue.toLowerCase().contains(finalsearch)) {
				for (Artist artistItem : artistItems) {
					if(artistItem.getName().equalsIgnoreCase(tmpValue))
						valueFound = true;
				}
				if(!valueFound)
					artistItems.add(new Artist(tmpValue, 0));
			}
			valueFound = false;
			tmpValue = music.getAlbum();
			if(tmpValue != null &&  tmpValue.toLowerCase().contains(finalsearch)) {
				for (Album albumItem : albumItems) {
					if(albumItem.getName().equalsIgnoreCase(tmpValue))
						valueFound = true;
				}
				if(!valueFound)
					albumItems.add(new Album(tmpValue));
			}			
		}
		
		Collections.sort(musicItems);
		Collections.sort(artistItems);
		Collections.sort(albumItems);
		
		arrayResults.clear();
		if(!artistItems.isEmpty()) {
			arrayResults.add(getString(R.string.artists));
			arrayResults.addAll(artistItems);
		}
		if(!albumItems.isEmpty()) {
			arrayResults.add(getString(R.string.albums));
			arrayResults.addAll(albumItems);
		}
		if(!musicItems.isEmpty()) {
			arrayResults.add(getString(R.string.songs));
			arrayResults.addAll(musicItems);
		}
	}
	
	/**
	 * Update the view from the items list if items is set.
	 */
	public void updateFromItems() {
		if (arrayResults != null) {
			setListAdapter(new SeparatedListAdapter(this,
					R.layout.simple_list_item_1,
					new SearchResultDataBinder(),
					arrayResults));
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
