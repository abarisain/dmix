package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.tools.Tools;

/**
 * Implementation of PlaylistFragment for devices prior to Honeycomb
 * 
 */
public class PlaylistFragmentCompat extends SherlockListFragment implements StatusChangeListener {
	private ArrayList<HashMap<String, Object>> songlist;
	private List<Music> musics;

	private String title;
	private MPDApplication app;

	public static final int MAIN = 0;
	public static final int CLEAR = 1;
	public static final int MANAGER = 3;
	public static final int SAVE = 4;
	public static final int EDIT = 2;

	public PlaylistFragmentCompat() {
		super();
		setHasOptionsMenu(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			Log.e(MPDApplication.TAG, "PlaylistFragmentCompat is not meant to be used on Android 3.0+. Please use the regular version.");
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		app = (MPDApplication) getActivity().getApplication();
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.playlist_activity, container, false);
		registerForContextMenu((ListView) view.findViewById(android.R.id.list));

		return view;
	}
	
	protected void update() {
		try {
			MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
			songlist = new ArrayList<HashMap<String, Object>>();
			musics = playlist.getMusicList();
			int playingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
			// The position in the songlist of the currently played song
			int listPlayingID = -1;
			for (Music m : musics) {
				if (m == null) {
					continue;
				}
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				if (m.isStream()) {
					if (m.haveTitle()) {
						item.put("title", m.getTitle());
						if (Tools.isStringEmptyOrNull(m.getName())) {
							item.put("artist", m.getArtist());
						} else if (Tools.isStringEmptyOrNull(m.getArtist())) {
							item.put("artist", m.getName());
						} else {
							item.put("artist", m.getArtist() + " - " + m.getName());
						}
					} else {
						item.put("title", m.getName());
					}
				} else {
					if (Tools.isStringEmptyOrNull(m.getAlbum())) {
						item.put("artist", m.getArtist());
					} else {
						item.put("artist", m.getArtist() + " - " + m.getAlbum());
					}
					item.put("title", m.getTitle());
				}
				if (m.getSongId() == playingID) {
					item.put("play", android.R.drawable.ic_media_play);
					// Lie a little. Scroll to the previous song than the one playing. That way it shows that there are other songs before
					// it
					listPlayingID = songlist.size() - 1;
				} else {
					item.put("play", 0);
				}
				songlist.add(item);
			}

			final int finalListPlayingID = listPlayingID;

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					SimpleAdapter songs = new SimpleAdapter(getActivity(), songlist, R.layout.playlist_list_item, new String[] { "play",
							"title", "artist" }, new int[] { R.id.picture, android.R.id.text1, android.R.id.text2 });

					setListAdapter(songs);

					// Only scroll if there is a valid song to scroll to. 0 is a valid song but does not require scroll anyway.
					// Also, only scroll if it's the first update. You don't want your playlist to scroll itself while you are looking at
					// other
					// stuff.
					if (finalListPlayingID > 0)
						setSelection(finalListPlayingID);
				}
			});

		} catch (MPDServerException e) {
		}

	}
	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		new Thread(new Runnable() {
			public void run() {
				update();
			}
		}).start();
	}

	@Override
	public void onPause() {
		app.oMPDAsyncHelper.removeStatusChangeListener(this);
		super.onPause();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);
		android.view.MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.mpd_playlistcnxmenu, menu);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		// arrayListId = info.position;

		title = (String) songlist.get(info.position).get("title");
		menu.setHeaderTitle(title);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		MPDApplication app = (MPDApplication) getActivity().getApplication();
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int songId = (Integer) songlist.get(info.position).get("songid");
		switch (item.getItemId()) {
		case R.id.PLCX_SkipToHere:
			// skip to selected Song
			try {
				app.oMPDAsyncHelper.oMPD.skipToId(songId);
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
				Tools.notifyUser("Song moved to next in list", getActivity());
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.PLCX_moveFirst:
			try { // Move song to first in playlist
				app.oMPDAsyncHelper.oMPD.getPlaylist().move(songId, 0);
				Tools.notifyUser("Song moved to first in list", getActivity());
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.PLCX_moveLast:
			try { // Move song to last in playlist
				MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
				app.oMPDAsyncHelper.oMPD.getPlaylist().move(songId, status.getPlaylistLength() - 1);
				Tools.notifyUser("Song moved to last in list", getActivity());
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.PLCX_removeFromPlaylist:
			try {
				app.oMPDAsyncHelper.oMPD.getPlaylist().removeById(songId);
				Tools.notifyUser(getResources().getString(R.string.deletedSongFromPlaylist), getActivity());
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.mpd_playlistmenu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Menu actions...
		Intent i;
		switch (item.getItemId()) {
		case R.id.PLM_Clear:
			try {
				app.oMPDAsyncHelper.oMPD.getPlaylist().clear();
				songlist.clear();
				Tools.notifyUser(getResources().getString(R.string.playlistCleared), getActivity());
				((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.PLM_EditPL:
			i = new Intent(getActivity(), PlaylistEditActivity.class);
			startActivity(i);
			return true;
		case R.id.PLM_Save:
			final EditText input = new EditText(getActivity());
			new AlertDialog.Builder(getActivity())
		    .setTitle(R.string.playlistName)
		    .setMessage(R.string.newPlaylistPrompt)
		    .setView(input)
		    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		            final String name = input.getText().toString().trim();
		            if (null!=name && name.length()>0) {
		            	app.oMPDAsyncHelper.execAsync(new Runnable() {
		    				@Override
		    				public void run() {
		    					try {
									app.oMPDAsyncHelper.oMPD.getPlaylist().savePlaylist(name);
								} catch (MPDServerException e) {
									e.printStackTrace();
								}
		    				}
		    			});
		            }
		        }
		    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		            // Do nothing.
		        }
		    }).show();
			return true;
		default:
			return false;
		}

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		MPDApplication app = (MPDApplication) getActivity().getApplication(); // Play selected Song

		Music m = musics.get(position);
		try {
			app.oMPDAsyncHelper.oMPD.skipToId(m.getSongId());
		} catch (MPDServerException e) {
		}

	}

	public void scrollToNowPlaying() {
		for (HashMap<String, Object> song : songlist) {
			try {
				if (((Integer) song.get("songid")).intValue() == ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.getStatus()
						.getSongId()) {
					getListView().requestFocusFromTouch();
					getListView().setSelection(songlist.indexOf(song));
				}
			} catch (MPDServerException e) {
			}
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
		// Mark running track...
		for (HashMap<String, Object> song : songlist) {
			if (((Integer) song.get("songid")).intValue() == mpdStatus.getSongId())
				song.put("play", android.R.drawable.ic_media_play);
			else
				song.put("play", 0);

		}
		final SimpleAdapter adapter = (SimpleAdapter) getListAdapter();
		if(adapter != null)
			adapter.notifyDataSetChanged();
		
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
