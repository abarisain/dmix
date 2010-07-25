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
}
