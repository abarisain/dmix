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
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SearchSongActivity extends BrowseActivity{
	private ArrayList<Music> arrayMusic = null;
	// We need this to store the music that is on display so that we can figure out what one was picked later on
	private ArrayList<Music> dispMusic = null;
	
	String searchKeywords = "";
	
	public SearchSongActivity() {
		super(R.string.addSong, R.string.songAdded, MPD.MPD_SEARCH_TITLE);
		items = new ArrayList<String>();
		dispMusic = new ArrayList<Music>();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY).trim();
		} else {
			return; //Bye !
		}
		
		setContentView(R.layout.artists);
		setTitle(getTitle()+" : "+searchKeywords);
		pd = ProgressDialog.show(SearchSongActivity.this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingSongs));		
		
		registerForContextMenu(getListView());
		
		UpdateList();
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		Add(position);
    }
    
    @Override
	protected void Add(String item) {
    	Add(items.indexOf(item));
	}
	
    protected void Add(int index) {
    	Music music = dispMusic.get(index);
		try {
			MPDApplication app = (MPDApplication)getApplication();

			app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.songAdded,music.getTitle()),music.getName()), this);
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    

	@Override
	protected void asyncUpdate() {
		String finalsearch = this.searchKeywords.toLowerCase();
		
		try {
			MPDApplication app = (MPDApplication)getApplication();
			arrayMusic = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.search("any", finalsearch));
		} catch (MPDServerException e) {	
		}
		
		
		for (Music music : arrayMusic) {
			if(music.getTitle().toLowerCase().contains(finalsearch)) {
				items.add(music.getTitle());
				dispMusic.add(music);
			}
		}
	}
}
