package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDServerException;
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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class PlaylistRemoveActivity extends ListActivity implements StatusChangeListener, OnClickListener {
	private ArrayList<HashMap<String, Object>> songlist = new ArrayList<HashMap<String, Object>>();
	private List<Music> musics;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		MPDApplication app = (MPDApplication) getApplication();
		setContentView(R.layout.playlist_editlist_activity);
		this.setTitle(R.string.nowPlaying);
		try {
			MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();

			playlist.refresh();
			musics = playlist.getMusics();
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

		TextView title = (TextView) findViewById(R.id.headerText);
		title.setText(this.getTitle());

		ImageView icon = (ImageView) findViewById(R.id.headerIcon);
		icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_tab_playlists_selected));
	}

	protected void update() {
		MPDApplication app = (MPDApplication) getApplicationContext();
		try {
			MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
			playlist.refresh();
			songlist = new ArrayList<HashMap<String, Object>>();
			musics = playlist.getMusics();
			for (Music m : musics) {
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				item.put("artist", m.getArtist());
				item.put("title", m.getTitle());
				item.put("marked", false);
				if (m.getSongId() == app.oMPDAsyncHelper.oMPD.getStatus().getSongId())
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
		// TODO Auto-generated method stub

	}

	@Override
	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void volumeChanged(MPDVolumeChangedEvent event) {
		// TODO Auto-generated method stub

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
			MainMenuActivity.notifyUser("Updating ...", getApplication());
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

				for (HashMap<String, Object> item : new ArrayList<HashMap<String, Object>>(songlist)) {
					try {
						if (item.get("marked").equals(true)) {
							app.oMPDAsyncHelper.oMPD.getPlaylist().removeSong((Integer) item.get("songid"));
							songlist.remove(item);
							count++;
						}
					} catch (MPDServerException e) {
						Log.e("MPDroid", e.toString());
					}
				}
				app.oMPDAsyncHelper.oMPD.getPlaylist().refresh(); // If not refreshed an intern Array of JMPDComm get out of sync and throws
				// IndexOutOfBound
				MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.removeCountSongs), count), this);
				((SimpleAdapter) getListAdapter()).notifyDataSetChanged();

			} catch (MPDServerException e) {
				Log.e("MPDroid", e.toString());
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

}
