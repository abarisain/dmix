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

public class ArtistsActivity extends BrowseActivity {
	private boolean albumartist;
	
	public ArtistsActivity() {
		super(R.string.addArtist, R.string.artistAdded, MPD.MPD_SEARCH_ARTIST);		
	}
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.artists);
		pd = ProgressDialog.show(ArtistsActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingArtists));

		//load preferences for album artist tag display option
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		albumartist = settings.getBoolean("albumartist", false);

		registerForContextMenu(getListView());
		
		UpdateList();
	}
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            Intent intent = new Intent(this, AlbumsActivity.class);
            intent.putExtra("artist", items.get(position));
            startActivityForResult(intent, -1);
    }
	
	@Override
	protected void asyncUpdate()
	{
		try {
			MPDApplication app = (MPDApplication)getApplication();
			if(albumartist == true) {
				items = app.oMPDAsyncHelper.oMPD.listAlbumArtists();
			} else {
				items = app.oMPDAsyncHelper.oMPD.listArtists();
			}
		} catch (MPDServerException e) {
		}
	}
}
