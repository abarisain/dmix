package com.namelessdev.mpdroid;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.Music;

import com.namelessdev.mpdroid.MPDAsyncHelper.AsyncExecListener;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

public class SearchActivity extends ListActivity implements AsyncExecListener {
	private Collection<Music> items;
	private int iJobID = -1;
	private ProgressDialog pd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		String searchKeywords = "";
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY);
		} else {
			return; //Bye !
		}
		//setContentView(R.layout.artists);
		
		pd = ProgressDialog.show(SearchActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingAlbums));

		//setTitle(getResources().getString(R.string.albums));
		final String finalSearchKeywords = searchKeywords; // wtf java
		MPDApplication app = (MPDApplication)getApplication();
		// Loading Albums asynchronous...
		app.oMPDAsyncHelper.addAsyncExecListener(this);
		iJobID = app.oMPDAsyncHelper.execAsync(new Runnable(){
			@Override
			public void run() 
			{
				try {
					MPDApplication app = (MPDApplication)getApplication();
					items = app.oMPDAsyncHelper.oMPD.search("any",finalSearchKeywords);
				} catch (MPDServerException e) {
					
				}
			}
		});
		
		
	}

	@Override
	public void asyncExecSucceeded(int jobID) {
		// TODO Auto-generated method stub
		if(iJobID == jobID)
		{
			List<String> itemsList = new ArrayList<String>();
			for (Music music : items) {
				//Log.i("MPDroid", music.getTitle());
				itemsList.add(music.getTitle());
			}
			
			// Yes, its our job which is done...
			//ArrayAdapter<String> notes = new ArrayAdapter<String>(SearchActivity.this, android.R.layout.simple_list_item_1, itemsList);
			//setListAdapter(notes);
			
			// Use the ListViewButtonAdapter class to show the albums
			ListViewButtonAdapter<String> almumsAdapter = new ListViewButtonAdapter<String>(SearchActivity.this, android.R.layout.simple_list_item_1, itemsList);
			
			PlusListener AddListener = new PlusListener() {
				@Override
				public void OnAdd(CharSequence sSelected, int iPosition)
				{
					try {
						MPDApplication app = (MPDApplication)getApplication();
						ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, sSelected.toString()));
						app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
						//MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),sSelected), AlbumsActivity.this);
					} catch (MPDServerException e) {
						e.printStackTrace();
					}
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
}
