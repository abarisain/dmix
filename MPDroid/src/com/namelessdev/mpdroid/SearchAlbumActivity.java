package com.namelessdev.mpdroid;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SearchAlbumActivity extends ListActivity implements AsyncExecListener {
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
		pd = ProgressDialog.show(SearchAlbumActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingArtists));		
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
					items = app.oMPDAsyncHelper.oMPD.listAlbums();
				} catch (MPDServerException e) {
					
				}
			}
		});
		
		
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            Intent intent = new Intent(this, SongsActivity.class);
            intent.putExtra("album", itemsList.get(position));
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
			ListViewButtonAdapter<String> almumsAdapter = new ListViewButtonAdapter<String>(SearchAlbumActivity.this, android.R.layout.simple_list_item_1, itemsList);
			
			PlusListener AddListener = new PlusListener() {
				@Override
				public void OnAdd(CharSequence sSelected, int iPosition)
				{
					try {
						MPDApplication app = (MPDApplication)getApplication();
						ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, sSelected.toString()));
						app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
						MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),sSelected), SearchAlbumActivity.this);
					} catch (MPDServerException e) {
						e.printStackTrace();
					}
				}
			};
			getListView().setOnItemLongClickListener( new AdapterView.OnItemLongClickListener (){
                @Override
                public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
					try {
						MPDApplication app = (MPDApplication)getApplication();
						ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, itemsList.get(position).toString()));
						app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
						MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),itemsList.get(position)), SearchAlbumActivity.this);
					} catch (MPDServerException e) {
						e.printStackTrace();
					}
                    return true;
                }
}			);
			almumsAdapter.SetPlusListener(AddListener);
			setListAdapter(almumsAdapter);
			
			
			// No need to listen further...
			MPDApplication app = (MPDApplication)getApplication();
			app.oMPDAsyncHelper.removeAsyncExecListener(this);
			pd.dismiss();
		}
	}
}
