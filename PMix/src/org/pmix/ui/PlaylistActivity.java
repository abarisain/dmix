package org.pmix.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.Music;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.event.MPDConnectionStateChangedEvent;
import org.a0z.mpd.event.MPDPlaylistChangedEvent;
import org.a0z.mpd.event.MPDRandomChangedEvent;
import org.a0z.mpd.event.MPDRepeatChangedEvent;
import org.a0z.mpd.event.MPDStateChangedEvent;
import org.a0z.mpd.event.MPDTrackChangedEvent;
import org.a0z.mpd.event.MPDUpdateStateChangedEvent;
import org.a0z.mpd.event.MPDVolumeChangedEvent;
import org.a0z.mpd.event.StatusChangeListener;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PlaylistActivity extends ListActivity implements OnMenuItemClickListener, StatusChangeListener {
	private ArrayList<HashMap<String,Object>> songlist = new ArrayList<HashMap<String,Object>>();
	private List<Music> musics;
	private int arrayListId;
	private int songId;
	private String title;
	
	public static final int MAIN = 0;
	public static final int CLEAR = 1;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		MPDApplication app = (MPDApplication)getApplication();
		setContentView(R.layout.artists);
		
		try {
			MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
			playlist.refresh();
			musics = playlist.getMusics();
			for(Music m : musics) {
				HashMap<String,Object> item = new HashMap<String,Object>();
				item.put( "songid", m.getSongId() );
				item.put( "artist", m.getArtist() );
				item.put( "title", m.getTitle() );
				if(m.getSongId() == app.oMPDAsyncHelper.oMPD.getStatus().getSongId())
					item.put( "play", android.R.drawable.ic_media_play );
				else
					item.put( "play", 0 );
				songlist.add(item);
			}
			SimpleAdapter songs = new SimpleAdapter( 
					this, 
					songlist,
					R.layout.playlist_list_item,
					new String[] { "play",  "title","artist" },
					new int[] { R.id.picture ,android.R.id.text1, android.R.id.text2 }  );
			
			setListAdapter( songs );
		} catch (MPDServerException e) {
		}
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		ListView list = getListView();
		/*
		LinearLayout test = (LinearLayout)list.getChildAt(1);
		ImageView img = (ImageView)test.findViewById(R.id.picture);
		//ImageView img = (ImageView)((LinearLayout)list.getItemAtPosition(3)).findViewById(R.id.picture);
		img.setImageDrawable(getResources().getDrawable(R.drawable.gmpcnocover));
		*/
		registerForContextMenu(list);
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.unsetActivity(this);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		arrayListId = info.position;
		songId = (Integer)songlist.get(info.position).get("songid");
		title = (String)songlist.get(info.position).get("title");

		menu.setHeaderTitle(title);
		MenuItem addArtist = menu.add(ContextMenu.NONE, 0, 0, R.string.removeSong);
		addArtist.setOnMenuItemClickListener(this);
	}


	public boolean onMenuItemClick(MenuItem item) {
		MPDApplication app = (MPDApplication)getApplication();
		switch (item.getItemId()) {
		case 0:
			try {
				app.oMPDAsyncHelper.oMPD.getPlaylist().removeSong(songId);
				songlist.remove(arrayListId); 
				app.oMPDAsyncHelper.oMPD.getPlaylist().refresh(); // If not refreshed an intern Array of JMPDComm get out of sync and throws IndexOutOfBound
				MainMenuActivity.notifyUser(getResources().getString(R.string.deletedSongFromPlaylist), this);
				((SimpleAdapter)getListAdapter()).notifyDataSetChanged();
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}
	
	/*
	 * Create Menu for Playlist View
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0,MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0,CLEAR, 1, R.string.clear).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return result;
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MPDApplication app = (MPDApplication)getApplication();
		// Menu actions...
		switch (item.getItemId()) {
		case MAIN:
			Intent i = new Intent(this, MainMenuActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		case CLEAR:
			try {
				app.oMPDAsyncHelper.oMPD.getPlaylist().clear();
				songlist.clear();
				MainMenuActivity.notifyUser(getResources().getString(R.string.playlistCleared), this);
				((SimpleAdapter)getListAdapter()).notifyDataSetChanged();
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		default:
			return false;
		}

	}

	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		MPDApplication app = (MPDApplication)getApplication();
		// Play selected Song
		Music m = musics.get(position);
	    try {
	    	app.oMPDAsyncHelper.oMPD.skipTo(m.getSongId());
	    } catch (MPDServerException e) {
	    }
			
	}

	@Override
	public void connectionStateChanged(MPDConnectionStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void playlistChanged(MPDPlaylistChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void randomChanged(MPDRandomChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void repeatChanged(MPDRepeatChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stateChanged(MPDStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trackChanged(MPDTrackChangedEvent event) {
		// Mark running track...
		for(HashMap<String,Object> song : songlist)
		{
			if(((Integer)song.get("songid")).intValue()==event.getMpdStatus().getSongId())
				song.put( "play", android.R.drawable.ic_media_play );
			else
				song.put( "play", 0 );
				
		
		}
		((SimpleAdapter)getListAdapter()).notifyDataSetChanged();
	}

	@Override
	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void volumeChanged(MPDVolumeChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
