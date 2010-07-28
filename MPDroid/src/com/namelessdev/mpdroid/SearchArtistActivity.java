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

public class SearchArtistActivity extends BrowseActivity implements AsyncExecListener {
	private List<String> musicList = null;
	String searchKeywords = "";
	
	public SearchArtistActivity() {
		super(R.string.addArtist, R.string.artistAdded, MPD.MPD_SEARCH_ARTIST);
		items = new ArrayList<String>();
		musicList = new ArrayList<String>();
	}
	
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
		app.oMPDAsyncHelper.addAsyncExecListener(this);
		iJobID = app.oMPDAsyncHelper.execAsync(new Runnable(){
			@Override
			public void run() {
				try {
					MPDApplication app = (MPDApplication)getApplication();
					musicList = app.oMPDAsyncHelper.oMPD.listArtists();
				} catch (MPDServerException e) {
					
				}
			}
		});
		
		
		registerForContextMenu(getListView());
	}	
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            Intent intent = new Intent(this, AlbumsActivity.class);
            intent.putExtra("artist", items.get(position));
            startActivityForResult(intent, -1);
    }
    
	@Override
	public void asyncExecSucceeded(int jobID) {
		if(iJobID == jobID) {
			searchKeywords = searchKeywords.toLowerCase().trim();
			for (String music : musicList) {
				if(music.toLowerCase().contains(searchKeywords))
					items.add(music);
			}
			
			super.asyncExecSucceeded(jobID);
		}
	}
}
