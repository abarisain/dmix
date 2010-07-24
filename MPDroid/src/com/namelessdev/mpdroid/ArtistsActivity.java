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
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ArtistsActivity extends BrowseActivity implements OnMenuItemClickListener, AsyncExecListener {
	// Define this as public, more efficient due to the access of a anonymous inner class...
	// TODO: Is static really the solution? No, should be cashed in JMPDComm ,but it loads 
	// it only once with this "hotfix"...
	public static List<String> items = null;
	private int iJobID = -1;
	private ProgressDialog pd;
	private boolean albumartist;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.artists);
		
		//load preferences for album artist tag display option
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		albumartist = settings.getBoolean("albumartist", false);

		pd = ProgressDialog.show(ArtistsActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingArtists));

		if(items == null)
		{
			// Loading Artists asynchronous...
			MPDApplication app = (MPDApplication)getApplication();
			app.oMPDAsyncHelper.addAsyncExecListener(this);
			iJobID = app.oMPDAsyncHelper.execAsync(new Runnable(){
				@Override
				public void run() 
				{
					try {
						MPDApplication app = (MPDApplication)getApplication();
						if(albumartist == true) {
							items = app.oMPDAsyncHelper.oMPD.listAlbumArtists();
						}else{
							items = app.oMPDAsyncHelper.oMPD.listArtists();
						}
					} catch (MPDServerException e) {
						
					}
				}
			});
		}
		else
		{
			// Yes, its our job which is done...
			OnArtistsLoaded();
		}
		

		registerForContextMenu(getListView());
	}
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(items.get((int)info.id).toString());
		MenuItem addItem = menu.add(ContextMenu.NONE, 0, 0, R.string.addArtist);
		addItem.setOnMenuItemClickListener(this);
		
		MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, 1, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            Intent intent = new Intent(this, AlbumsActivity.class);
            intent.putExtra("artist", items.get(position));
            startActivityForResult(intent, -1);
    }

	@Override
	public void asyncExecSucceeded(int jobID) {
		if(iJobID == jobID)
		{
			// Yes, its our job which is done, no need to listen further...
			MPDApplication app = (MPDApplication)getApplication();
			app.oMPDAsyncHelper.removeAsyncExecListener(this);
			OnArtistsLoaded();
		}
	}

	protected void OnArtistsLoaded()
	{
		ListViewButtonAdapter<String> artistsAdapter = new ListViewButtonAdapter<String>(ArtistsActivity.this, android.R.layout.simple_list_item_1, items);
		
		PlusListener AddListener = new PlusListener() {
			@Override
			public void OnAdd(CharSequence sSelected, int iPosition)
			{
				Add(sSelected.toString());
			}
		};
		artistsAdapter.SetPlusListener(AddListener);
		setListAdapter(artistsAdapter);
		pd.dismiss();
	}
	
	protected void Add(String item) {
		try {
			MPDApplication app = (MPDApplication)getApplication();
			ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ARTIST, item));
			app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.artistAdded), item), ArtistsActivity.this);
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
