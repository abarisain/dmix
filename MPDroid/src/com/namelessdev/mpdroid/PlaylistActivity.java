package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PlaylistActivity extends ListActivity implements OnClickListener, StatusChangeListener {
	private ArrayList<HashMap<String, Object>> songlist;
	private List<Music> musics;
	//private int arrayListId;

	private String title;

	public static final int MAIN = 0;
	public static final int CLEAR = 1;
	public static final int MANAGER = 3;
	public static final int SAVE = 4;
	public static final int EDIT = 2;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		MPDApplication app = (MPDApplication) getApplication();
		setContentView(R.layout.playlist_activity);
		this.setTitle(R.string.nowPlaying);
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		ListView list = getListView();
		/*
		 * LinearLayout test = (LinearLayout)list.getChildAt(1); ImageView img = (ImageView)test.findViewById(R.id.picture); //ImageView img =
		 * (ImageView)((LinearLayout)list.getItemAtPosition(3)).findViewById(R.id.picture);
		 * img.setImageDrawable(getResources().getDrawable(R.drawable.gmpcnocover));
		 */

		registerForContextMenu(list);

		Button button = (Button) findViewById(R.id.headerButton);
		button.setVisibility(View.VISIBLE);
		button.setOnClickListener(this);

		Button title = (Button) findViewById(R.id.headerText);
		title.setText(this.getTitle());
		title.setOnClickListener(this);

		ImageView icon = (ImageView) findViewById(R.id.headerIcon);
		icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_tab_playlists_selected));

	}
	
	protected void update() {
		MPDApplication app = (MPDApplication) getApplicationContext();
		try {
			MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
			songlist = new ArrayList<HashMap<String, Object>>();
			musics = playlist.getMusics();
			int playingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
			for (Music m : musics) {
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				item.put("artist", m.getArtist());
				item.put("title", m.getTitle());
				if (m.getSongId() == playingID)
					item.put("play", android.R.drawable.ic_media_play);
				else
					item.put("play", 0);
				songlist.add(item);
			}
			SimpleAdapter songs = new SimpleAdapter(this, songlist, R.layout.playlist_list_item, new String[] { "play", "title", "artist" },
					new int[] { R.id.picture, android.R.id.text1, android.R.id.text2 });

			setListAdapter(songs);
		} catch (MPDServerException e) {
		}

	}

	@Override
	protected void onStart() {
		super.onStart();

		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
		update();
	}

	@Override
	protected void onResume() {
		super.onResume();
		update();
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mpd_playlistcnxmenu, menu);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		//arrayListId = info.position;
		
		title = (String) songlist.get(info.position).get("title");
		menu.setHeaderTitle(title);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		MPDApplication app = (MPDApplication) getApplication();
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int songId = (Integer) songlist.get(info.position).get("songid");
		switch (item.getItemId()) {
		case R.id.PLCX_SkipToHere:
			// skip to selected Song
			try {
				app.oMPDAsyncHelper.oMPD.skipTo(songId);
			} catch (MPDServerException e) {
			}
			return true;
		case R.id.PLCX_playNext:
			try { // Move song to next in playlist
				MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
				if (info.id < status.getSongPos()) {
					app.oMPDAsyncHelper.oMPD.getPlaylist().move(songId, status.getSongPos());
				} else {
					app.oMPDAsyncHelper.oMPD.getPlaylist().move(songId, status.getSongPos() + 1);
				}
				MainMenuActivity.notifyUser("Song moved to next in list", this);
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;	  
		case R.id.PLCX_moveFirst:
			try { // Move song to first in playlist
				app.oMPDAsyncHelper.oMPD.getPlaylist().move(songId, 0);
				MainMenuActivity.notifyUser("Song moved to first in list", this);
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;	  
		case R.id.PLCX_moveLast:
			try { // Move song to last in playlist
				MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
				app.oMPDAsyncHelper.oMPD.getPlaylist().move(songId, status.getPlaylistLength() - 1);
				MainMenuActivity.notifyUser("Song moved to last in list", this);
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;	  
		case R.id.PLCX_removeFromPlaylist:
			try {
				app.oMPDAsyncHelper.oMPD.getPlaylist().removeSong(songId);
				MainMenuActivity.notifyUser(getResources().getString(R.string.deletedSongFromPlaylist), this);
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	



	/*
	 * Create Menu for Playlist View
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mpd_playlistmenu, menu);
	    
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MPDApplication app = (MPDApplication) getApplication();
		// Menu actions...
		switch (item.getItemId()) {
		case R.id.PLM_MainMenu:
			Intent i = new Intent(this, MainMenuActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		case R.id.PLM_LibTab:
			i = new Intent(this, LibraryTabActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		case R.id.PLM_Clear:
			try {
				app.oMPDAsyncHelper.oMPD.getPlaylist().clear();
				songlist.clear();
				MainMenuActivity.notifyUser(getResources().getString(R.string.playlistCleared), this);
				((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.PLM_EditPL:
			i = new Intent(this, PlaylistRemoveActivity.class);
			startActivity(i);
			return true;
		case R.id.PLM_Manage:
			i = new Intent(this, PlaylistManagerActivity.class);
			startActivity(i);
			return true;
		case R.id.PLM_Save:
			i = new Intent(this, PlaylistSaveActivity.class);
			startActivity(i);
			return true;
		default:
			return false;
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		MPDApplication app = (MPDApplication) getApplication(); // Play selected Song

		Music m = musics.get(position);
		try {
			app.oMPDAsyncHelper.oMPD.skipTo(m.getSongId());
		} catch (MPDServerException e) {
		}

	}

	public void scrollToNowPlaying() {
		for (HashMap<String, Object> song : songlist) {
			try {
				if (((Integer) song.get("songid")).intValue() == ((MPDApplication) getApplication()).oMPDAsyncHelper.oMPD.getStatus().getSongId()) {
					getListView().requestFocusFromTouch();
					getListView().setSelection(songlist.indexOf(song));
				}
			} catch (MPDServerException e) {
			}
		}
	}
	
	@Override
	public void connectionStateChanged(MPDConnectionStateChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void playlistChanged(MPDPlaylistChangedEvent event) {
		update();
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
		for (HashMap<String, Object> song : songlist) {
			if (((Integer) song.get("songid")).intValue() == event.getMpdStatus().getSongId())
				song.put("play", android.R.drawable.ic_media_play);
			else
				song.put("play", 0);

		}
		((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
	}

	@Override
	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void volumeChanged(MPDVolumeChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.headerButton:
			Intent i = new Intent(this, PlaylistRemoveActivity.class);
			startActivityForResult(i, EDIT);
			break;
		case R.id.headerText:
			scrollToNowPlaying();
			break;
		default:
			break;
		}
	}

}
