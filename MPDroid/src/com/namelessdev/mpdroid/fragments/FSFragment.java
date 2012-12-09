package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.Directory;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.namelessdev.mpdroid.FSActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

public class FSFragment extends BrowseFragment {
	private Directory currentDirectory = null;

	public FSFragment() {
		super(R.string.addDirectory, R.string.addedDirectoryToPlaylist, MPDCommand.MPD_SEARCH_FILENAME);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(getListView());
		UpdateList();
	}

	@Override
	protected void Add(Item item) {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			Directory ToAdd = currentDirectory.getDirectory(item.getName());
			if (ToAdd != null) {
				// Valid directory
				app.oMPDAsyncHelper.oMPD.getPlaylist().add(ToAdd);
				Tools.notifyUser(String.format(getResources().getString(R.string.addedDirectoryToPlaylist), item),
						FSFragment.this.getActivity());
			} else {
				app.oMPDAsyncHelper.oMPD.getPlaylist().add((Music) item);
				Tools.notifyUser(getResources().getString(R.string.songAdded, item), FSFragment.this.getActivity());
			}
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void Add(Item item, String playlist) {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			Directory ToAdd = currentDirectory.getDirectory(item.getName());
			if (ToAdd != null) {
				// Valid directory
				app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, ToAdd);
				Tools.notifyUser(String.format(getResources().getString(R.string.addedDirectoryToPlaylist), item),
						FSFragment.this.getActivity());
			} else {
				ArrayList<Music> songs = new ArrayList<Music>();
				songs.add((Music) item);
				app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, songs);
				Tools.notifyUser(getResources().getString(R.string.songAdded, item), FSFragment.this.getActivity());
			}
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void asyncUpdate() {
		MPDApplication app = (MPDApplication) getActivity().getApplication();
		if (this.getActivity().getIntent().getStringExtra("directory") != null) {
			currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory().makeDirectory(
					(String) this.getActivity().getIntent().getStringExtra("directory"));
			setActivityTitle((String) getActivity().getIntent().getStringExtra("directory"));
		} else {
			currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory();
		}

		try {
			currentDirectory.refreshData();
		} catch (MPDServerException e) {
			e.printStackTrace();
		}

        List<Item> dirItems=new ArrayList<Item>();
        dirItems.addAll(currentDirectory.getDirectories());
        dirItems.addAll(currentDirectory.getFiles());
        items=dirItems;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// click on a file
		if (position > currentDirectory.getDirectories().size() - 1 || currentDirectory.getDirectories().size() == 0) {

			final Music music = (Music) currentDirectory.getFiles().toArray()[position - currentDirectory.getDirectories().size()];
			final MPDApplication app = (MPDApplication) getActivity().getApplication();
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						int songId = -1;
						app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
						if (songId > -1) {
							app.oMPDAsyncHelper.oMPD.skipToId(songId);
						}
					} catch (MPDServerException e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			// click on a directory
			// open the same sub activity, it would be better to reuse the
			// same instance

			Intent intent = new Intent(getActivity(), FSActivity.class);
			String dir;

			dir = ((Directory) currentDirectory.getDirectories().toArray()[position]).getFullpath();
			if (dir != null) {
				intent.putExtra("directory", dir);
				startActivityForResult(intent, -1);
			}
		}

	}

}
