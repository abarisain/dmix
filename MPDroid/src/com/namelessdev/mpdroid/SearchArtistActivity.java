package com.namelessdev.mpdroid;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;

import com.namelessdev.mpdroid.MPDAsyncHelper.AsyncExecListener;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SearchArtistActivity extends ListActivity implements OnMenuItemClickListener, AsyncExecListener {
	private LinkedList<String> items;
	private List<String> itemsList = null;
	private int iJobID = -1;
	private ProgressDialog pd;
	String searchKeywords = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY);
		} else {
			return; //Bye !
		}
		setContentView(R.layout.artists);
		setTitle(getTitle()+" : "+searchKeywords);
		pd = ProgressDialog.show(SearchArtistActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingArtists));

		//setTitle(getResources().getString(R.string.albums));
		MPDApplication app = (MPDApplication)getApplication();
		// Loading Albums asynchronous...
		itemsList = new ArrayList<String>();
		app.oMPDAsyncHelper.addAsyncExecListener(this);
		iJobID = app.oMPDAsyncHelper.execAsync(new Runnable(){
			@Override
			public void run() 
			{
				try {
					MPDApplication app = (MPDApplication)getApplication();
					items = app.oMPDAsyncHelper.oMPD.listArtists();
				} catch (MPDServerException e) {
					
				}
			}
		});
		
		
		registerForContextMenu(getListView());
	}	
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(itemsList.get((int)info.id).toString());
		MenuItem addItem = menu.add(ContextMenu.NONE, 0, 0, R.string.addArtist);
		addItem.setOnMenuItemClickListener(this);
		
		MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, 1, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            Intent intent = new Intent(this, AlbumsActivity.class);
            intent.putExtra("artist", itemsList.get(position));
            startActivityForResult(intent, -1);
    }
    
	@Override
	public void asyncExecSucceeded(int jobID) {
		// TODO Auto-generated method stub
		if(iJobID == jobID)
		{
			searchKeywords = searchKeywords.toLowerCase().trim();
			for (String music : items) {
				if(music.toLowerCase().contains(searchKeywords))
					itemsList.add(music);
			}
			
			// Use the ListViewButtonAdapter class to show the albums
			ListViewButtonAdapter<String> almumsAdapter = new ListViewButtonAdapter<String>(SearchArtistActivity.this, android.R.layout.simple_list_item_1, itemsList);
			
			PlusListener AddListener = new PlusListener() {
				@Override
				public void OnAdd(CharSequence sSelected, int iPosition)
				{
					Add(sSelected.toString());
				}
			};
			almumsAdapter.SetPlusListener(AddListener);
			setListAdapter(almumsAdapter);
			
			
			// No need to listen further...
			MPDApplication app = (MPDApplication)getApplication();
			app.oMPDAsyncHelper.removeAsyncExecListener(this);
			pd.dismiss();
		}
	}
	
	protected void Add(String item) {
		try {
			MPDApplication app = (MPDApplication)getApplication();
			ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ARTIST, item));
			app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.artistAdded),item), this);
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
				
				Add(itemsList.get((int)info.id).toString());
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
			Add(itemsList.get((int)info.id).toString());
			break;
			
		}
		return false;
	}
}
