package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import com.namelessdev.mpdroid.tools.Tools;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PlaylistRemoveActivity extends ListActivity implements StatusChangeListener, OnClickListener {
	private ArrayList<HashMap<String, Object>> songlist = new ArrayList<HashMap<String, Object>>();
	private List<Music> musics;
	private com.namelessdev.mpdroid.ActionBar compatActionBar;

	@Override
	public void onCreate(Bundle icicle) {
		if (!Tools.isHoneycombOrBetter()) {
			setTheme(android.R.style.Theme_Black_NoTitleBar);
		}

		super.onCreate(icicle);
		MPDApplication app = (MPDApplication) getApplication();
		setContentView(R.layout.playlist_editlist_activity);
		this.setTitle(R.string.nowPlaying);
		try {
			MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();

			musics = playlist.getMusicList();
			int playingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
			for (Music m : musics) {
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				item.put("artist", m.getArtist());
				item.put("title", m.getTitle());
				item.put("marked", false);
				if (m.getSongId() == playingID)
					item.put("play", android.R.drawable.ic_media_play);
				else
					item.put("play", 0);
				songlist.add(item);
			}
			SimpleAdapter songs = new SimpleAdapter(this, songlist, R.layout.playlist_editlist_item, new String[] { "play", "title", "artist",
					"marked" }, new int[] { R.id.picture, android.R.id.text1, android.R.id.text2, R.id.removeCBox });

			setListAdapter(songs);
		} catch (MPDServerException e) {
		}

		app.oMPDAsyncHelper.addStatusChangeListener(this);

		ListView trackList = getListView();
		trackList.setOnCreateContextMenuListener(this);
		((TouchInterceptor) trackList).setDropListener(mDropListener);
		((TouchInterceptor) trackList).setRemoveListener(mRemoveListener);
		trackList.setCacheColorHint(0);

		Button button = (Button) findViewById(R.id.Remove);
		button.setOnClickListener(this);

		button = (Button) findViewById(R.id.Cancel);
		button.setOnClickListener(this);

		try {
			Activity activity = this;
			ActionBar actionBar = activity.getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		} catch (NoClassDefFoundError e) {
			// Older android
		} catch (NullPointerException e) {

		} catch (NoSuchMethodError e) {

		}

		final View tmpView = findViewById(R.id.compatActionbar);
		if (tmpView != null) {
			// We are on a phone
			compatActionBar = (com.namelessdev.mpdroid.ActionBar) tmpView;
			compatActionBar.setTitle(R.string.nowPlaying);
			compatActionBar.setBackActionEnabled(true);
			compatActionBar.showBottomSeparator(true);
		}
	}

	protected void update() {
		MPDApplication app = (MPDApplication) getApplicationContext();
		try {
			MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();

			songlist = new ArrayList<HashMap<String, Object>>();
			musics = playlist.getMusicList();
			int playingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
			for (Music m : musics) {
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				item.put("artist", m.getArtist());
				item.put("title", m.getTitle());
				item.put("marked", false);
				if (m.getSongId() == playingID)
					item.put("play", android.R.drawable.ic_media_play);
				else
					item.put("play", 0);
				songlist.add(item);
			}
			SimpleAdapter songs = new SimpleAdapter(this, songlist, R.layout.playlist_editlist_item, new String[] { "play", "title", "artist",
					"marked" }, new int[] { R.id.picture, android.R.id.text1, android.R.id.text2, R.id.removeCBox });

			setListAdapter(songs);
		} catch (MPDServerException e) {
		}

	}

	/**
	 * Marks the selected item for deletion
	 * */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		HashMap<String, Object> item = songlist.get(position);
		item.get("marked");
		if (item.get("marked").equals(true)) {
			item.put("marked", false);
		} else {
			item.put("marked", true);
		}

		((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
		public void drop(int from, int to) {
			if (from == to) {
				return;
			}
			HashMap<String, Object> itemFrom = songlist.get(from);
			Integer songID = (Integer) itemFrom.get("songid");
			MPDApplication app = (MPDApplication) getApplication();
			try {
				// looks like it's not necessary
				/*
				 * if (from < to) { app.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to - 1); } else {
				 */
				app.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to);
				// }
			} catch (MPDServerException e) {
			}
			Tools.notifyUser("Updating ...", getApplication());
		}
	};

	private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
		public void remove(int which) {
			// removePlaylistItem(which);
		}
	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.Remove:
			MPDApplication app = (MPDApplication) getApplicationContext();
			int count = 0;
			try {

				/*
				 * If in some future the view will not be closed when this action occures its needed to make a copy of the songlist and remove
				 * the items from the original songlist in this for loop
				 * 
				 * And after update the view with ((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
				 * 
				 * But for now neither is nessesary
				 */
				for (HashMap<String, Object> item : songlist) {
					try {
						if (item.get("marked").equals(true)) {
							app.oMPDAsyncHelper.oMPD.getPlaylist().removeById((Integer) item.get("songid"));
							count++;
						}
					} catch (MPDServerException e) {
						Log.e("MPDroid", e.toString());
					}
				}
				Tools.notifyUser(String.format(getResources().getString(R.string.removeCountSongs), count), this);
			} catch (Exception e) {
				Log.e("MPDroid", "General: " + e.toString());
			}

			this.finish();
			break;
		case R.id.Cancel:
			this.finish();
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Menu actions...
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return false;
		}

	}

	@Override
	public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
		update();
		
	}

	@Override
	public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stateChanged(MPDStatus mpdStatus, String oldState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void repeatChanged(boolean repeating) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void randomChanged(boolean random) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connectionStateChanged(boolean connected, boolean connectionLost) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void libraryStateChanged(boolean updating) {
		// TODO Auto-generated method stub
		
	}

}
