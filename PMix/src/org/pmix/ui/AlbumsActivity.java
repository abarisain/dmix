package org.pmix.ui;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.Music;
import org.pmix.ui.MPDAsyncHelper.AsyncExecListener;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AlbumsActivity extends BrowseActivity implements AsyncExecListener {

	private List<String> items;
	private int iJobID = -1;
	private ProgressDialog pd;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.artists);
		
		pd = ProgressDialog.show(AlbumsActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingAlbums));

		setTitle(getResources().getString(R.string.albums));
		

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
						MPDApplication app = (MPDApplication)getApplication();
						ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, sSelected.toString()));
						app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
						MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.albumAdded),sSelected), AlbumsActivity.this);
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
