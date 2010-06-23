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
import android.widget.ListView;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ArtistsActivity extends BrowseActivity implements AsyncExecListener {
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
				try {
					MPDApplication app = (MPDApplication)getApplication();
					ArrayList<Music> songs = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ARTIST, sSelected.toString()));
					app.oMPDAsyncHelper.oMPD.getPlaylist().add(songs);
					MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.artistAdded), sSelected), ArtistsActivity.this);
				} catch (MPDServerException e) {
					e.printStackTrace();
				}
			}
		};

		
		
		artistsAdapter.SetPlusListener(AddListener);
		setListAdapter(artistsAdapter);
		pd.dismiss();
	}
}
