package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.a0z.mpd.MPD;
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
import com.namelessdev.mpdroid.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SearchResultDataBinder;
import com.namelessdev.mpdroid.views.SeparatedListAdapter;

public class SearchActivity extends SherlockListActivity implements OnMenuItemClickListener, AsyncExecListener {
	public static final int MAIN = 0;
	public static final int PLAYLIST = 3;

	public static final int ADD = 0;
	public static final int ADDNREPLACE = 1;
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
	}
	
	private String getItemName(Object o) {
		if(o instanceof Music) {
			return ((Music) o).getTitle();
		} else if(o instanceof ArtistItem) {
			return ((ArtistItem) o).getName();
		} else if(o instanceof AlbumItem) {
			return ((AlbumItem) o).getName();
		}
		return "";
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Object selectedItem = l.getAdapter().getItem(position);
		if(selectedItem instanceof Music) {
			add((Music) selectedItem);
		} else if(selectedItem instanceof ArtistItem) {
			Intent intent = new Intent(this, AlbumsActivity.class);
			intent.putExtra("artist", ((ArtistItem) selectedItem).getName());
			startActivityForResult(intent, -1);
		} else if(selectedItem instanceof AlbumItem) {
			Intent intent = new Intent(this, SongsActivity.class);
			intent.putExtra("album", ((AlbumItem) selectedItem).getName());
			startActivityForResult(intent, -1);
		}
	}

	protected void add(Object object) {
		if(object instanceof Music) {
			addString = R.string.addSong;
			addedString = R.string.songAdded;
			add((Music) object);
		} else if(object instanceof ArtistItem){
			addString = R.string.addArtist;
			addedString = R.string.artistAdded;
			add(((ArtistItem) object).getName(), MPD.MPD_SEARCH_ARTIST);
		} else if(object instanceof AlbumItem){
			addString = R.string.addAlbum;
			addedString = R.string.albumAdded;
			add(((AlbumItem) object).getName(), MPD.MPD_SEARCH_ALBUM);
		}
	}
	
	protected void add(String item, String mpdContext) {
		try {
			MPDApplication app = (MPDApplication) getApplication();
			ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(mpdContext, item));
			app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(songs);
			Tools.notifyUser(String.format(getResources().getString(addedString), item), this);
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
		menu.setHeaderTitle(getItemName(arrayResults.get((int) info.id)));
		android.view.MenuItem addItem = menu.add(ContextMenu.NONE, ADD, 0, getResources().getString(addString));
		addItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, ADDNREPLACE, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndPlayItem = menu.add(ContextMenu.NONE, ADDNPLAY, 0, R.string.addAndPlay);
		addAndPlayItem.setOnMenuItemClickListener(this);
	}
	
	@Override
	public boolean onMenuItemClick(android.view.MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final MPDApplication app = (MPDApplication) getApplication();
		final Object selectedItem = arrayResults.get((int) info.id);
		switch (item.getItemId()) {
		case ADDNREPLACE:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						String status = app.oMPDAsyncHelper.oMPD.getStatus().getState();
						app.oMPDAsyncHelper.oMPD.stop();
						app.oMPDAsyncHelper.oMPD.getPlaylist().clear();

						add(selectedItem);
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
		final ArrayList<ArtistItem> artistItems = new ArrayList<ArtistItem>();
		final ArrayList<AlbumItem> albumItems = new ArrayList<AlbumItem>();
		String tmpValue;
		musicLoop:
		for (Music music : arrayMusic) {
			if(music.getTitle() != null &&  music.getTitle().toLowerCase().contains(finalsearch)) {
				musicItems.add(music);
				continue;
			}
			tmpValue = music.getArtist();
			if(tmpValue != null && tmpValue.toLowerCase().contains(finalsearch)) {
				for(ArtistItem artistItem : artistItems) {
					if(artistItem.getName().equalsIgnoreCase(tmpValue))
						continue musicLoop;
				}
				artistItems.add(new ArtistItem(tmpValue));
				continue;
			}
			tmpValue = music.getAlbum();
			if(tmpValue != null &&  tmpValue.toLowerCase().contains(finalsearch)) {
				for(AlbumItem albumItem : albumItems) {
					if(albumItem.getName().equalsIgnoreCase(tmpValue))
						continue musicLoop;
				}
				albumItems.add(new AlbumItem(tmpValue));
				continue;
			}			
		}
		
		Collections.sort(musicItems, new Music.MusicTitleComparator());
		Collections.sort(artistItems, new ArtistItemComparator());
		Collections.sort(albumItems, new AlbumItemComparator());
		
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
	
	/* Item classes/comparators */
	
	public static class ArtistItem {
		private String name;
		
		public ArtistItem(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
	public static class AlbumItem {
		private String name;
		
		public AlbumItem(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
	public static class ArtistItemComparator implements Comparator<ArtistItem> {
		public int compare(ArtistItem o1, ArtistItem o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
		}
	}
	
	public static class AlbumItemComparator implements Comparator<AlbumItem> {
		public int compare(AlbumItem o1, AlbumItem o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
		}
	}
}
