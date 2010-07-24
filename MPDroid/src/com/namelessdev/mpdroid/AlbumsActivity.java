package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.R.layout;
import com.namelessdev.mpdroid.R.string;

import com.namelessdev.mpdroid.MPDAsyncHelper.AsyncExecListener;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class AlbumsActivity extends BrowseActivity implements OnMenuItemClickListener, AsyncExecListener {

	private List<String> items;
	private int iJobID = -1;
	private ProgressDialog pd;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.artists);
		pd = ProgressDialog.show(AlbumsActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingAlbums));

		if (getIntent().getStringExtra("artist") != null) {
			setTitle((String) getIntent().getStringExtra("artist"));
		} else {
			setTitle(getResources().getString(R.string.albums));	
		}

		MPDApplication app = (MPDApplication)getApplication();
			
		// Loading Albums asynchronous...
		app.oMPDAsyncHelper.addAsyncExecListener(this);
		iJobID = app.oMPDAsyncHelper.execAsync(new Runnable(){
			@Override
			public void run() 
			{
				
				try {
					MPDApplication app = (MPDApplication)getApplication();
					if (getIntent().getStringExtra("artist") != null) {
						items = app.oMPDAsyncHelper.oMPD.listAlbums((String) getIntent().getStringExtra("artist"));
					} else {
						items = app.oMPDAsyncHelper.oMPD.listAlbums();
					}
				} catch (MPDServerException e) {
					
				}
			}
		});
		

		registerForContextMenu(getListView());
	}
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		//arrayListId = info.position;
		//songId = (Integer)songlist.get(info.position).get("songid");
		//title = (String)songlist.get(info.position).get("title");

		menu.setHeaderTitle(items.get((int)info.id).toString());
		MenuItem addItem = menu.add(ContextMenu.NONE, 0, 0, R.string.addAlbum);
		addItem.setOnMenuItemClickListener(this);
		
		MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, 1, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
		
    	
    	
    }
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, SongsActivity.class);
		intent.putExtra("album", items.get(position));
		startActivityForResult(intent, -1);
	}

	@Override
	public void asyncExecSucceeded(int jobID) {
		if(iJobID == jobID)
		{
			// Yes, its our job which is done...
			ArrayAdapter<String> notes = new ArrayAdapter<String>(AlbumsActivity.this, android.R.layout.simple_list_item_1, items);
			setListAdapter(notes);
			
			// Use the ListViewButtonAdapter class to show the albums
			ListViewButtonAdapter<String> almumsAdapter = new ListViewButtonAdapter<String>(AlbumsActivity.this, android.R.layout.simple_list_item_1, items);
			
			PlusListener AddListener = new PlusListener() {
				@Override
				public void OnAdd(CharSequence sSelected, int iPosition)
				{
					try {
						MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),sSelected), AlbumsActivity.this);
						MPDApplication app = (MPDApplication)getApplication();
						ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, sSelected.toString()));
						app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
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

	protected void Add(String item) {
		try {
			MPDApplication app = (MPDApplication)getApplication();
			ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, item));
			app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),item), AlbumsActivity.this);
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
				MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
				app.oMPDAsyncHelper.oMPD.getPlaylist().clear();
				
				Add(items.get((int)info.id).toString());
				if ( status.toString() == MPDStatus.MPD_STATE_PLAYING ) {
					//MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),item), AlbumsActivity.this);
				}
				app.oMPDAsyncHelper.oMPD.play();
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
