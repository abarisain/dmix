package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.LibraryTabActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.PlaylistEditActivity;
import com.namelessdev.mpdroid.PlaylistSaveActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

public class StoredPlaylistFragment extends SherlockListFragment {
	private ArrayList<HashMap<String, Object>> songlist;
	private List<Music> musics;

	private String playlistName;
	private MPDApplication app;

	public static final int MAIN = 0;
	public static final int CLEAR = 1;
	public static final int SAVE = 3;
	public static final int EDIT = 2;

	public StoredPlaylistFragment() {
		super();
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		app = (MPDApplication) getActivity().getApplication();
		playlistName=getActivity().getIntent().getStringExtra("playlist");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.simple_list, container, false);
		registerForContextMenu((ListView) view.findViewById(android.R.id.list));
		return view;
	}
	
	protected void update() {
		try {
			musics = app.oMPDAsyncHelper.oMPD.getPlaylistSongs(playlistName);
			for (Music m : musics) {
				if (m == null) {
					continue;
				}
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				item.put("artist", m.getArtist());
				item.put("title", m.getTitle());
				item.put("play", 0);
				songlist.add(item);
			}
			SimpleAdapter songs = new SimpleAdapter(getActivity(), songlist, R.layout.playlist_list_item,
					new String[] { "play", "title", "artist" }, new int[] { R.id.picture, android.R.id.text1, android.R.id.text2 });

			setListAdapter(songs);
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
		update();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		android.view.MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.mpd_browsermenu, menu);

		//AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		// arrayListId = info.position;
		menu.setHeaderTitle(playlistName);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		/*
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
		*/
		return false;
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
		switch (item.getItemId()) {
		case R.id.PLM_MainMenu:
			Intent i = new Intent(getActivity(), MainMenuActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		case R.id.PLM_LibTab:
			i = new Intent(getActivity(), LibraryTabActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
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
			i.putExtra("playlist", playlistName);
			startActivity(i);
			return true;
			/*
		case R.id.PLM_Manage:
			i = new Intent(getActivity(), PlaylistManagerActivity.class);
			startActivity(i);
			return true;
			*/
		case R.id.PLM_Save:
			i = new Intent(getActivity(), PlaylistSaveActivity.class);
			startActivity(i);
			return true;
		case R.id.GMM_LibTab:
			i = new Intent(getActivity(), LibraryTabActivity.class);
			startActivity(i);
		default:
			return false;
		}

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
/*
		MPDApplication app = (MPDApplication) getActivity().getApplication(); // Play selected Song

		Music m = musics.get(position);
		try {
			app.oMPDAsyncHelper.oMPD.skipToId(m.getSongId());
		} catch (MPDServerException e) {
*/
	}
}