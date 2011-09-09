package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;
import java.util.Collection;

import org.a0z.mpd.Directory;
import org.a0z.mpd.MPD;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.namelessdev.mpdroid.FSActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

public class FSFragment extends BrowseFragment {
	private Directory currentDirectory = null;

	public FSFragment() {
		super(R.string.addDirectory, R.string.addedDirectoryToPlaylist, MPD.MPD_SEARCH_FILENAME);
		items = new ArrayList<String>();
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.files, container, false);
	}

	@Override
	protected void Add(String item) {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			Directory ToAdd = currentDirectory.getDirectory(item);
			if (ToAdd != null) {
				// Valid directory
				app.oMPDAsyncHelper.oMPD.getPlaylist().add(ToAdd);
				Tools.notifyUser(String.format(getResources().getString(R.string.addedDirectoryToPlaylist), item),
						FSFragment.this.getActivity());
			} else {
				Music music = currentDirectory.getFileByTitle(item);
				if (music != null) {
					app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
					Tools.notifyUser(getResources().getString(R.string.songAdded, item), FSFragment.this.getActivity());
				}
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
			setActivityTitle((String) getActivity().getIntent().getStringExtra("directory"), R.drawable.ic_tab_playlists_selected);
		} else {
			currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory();
		}

		try {
			currentDirectory.refreshData();
		} catch (MPDServerException e) {
			e.printStackTrace();
		}

		Collection<Directory> directories = currentDirectory.getDirectories();
		for (Directory child : directories) {
			items.add(child.getName());
		}

		Collection<Music> musics = currentDirectory.getFiles();
		for (Music music : musics) {
			items.add(music.getTitle());
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// click on a file
		if (position > currentDirectory.getDirectories().size() - 1 || currentDirectory.getDirectories().size() == 0) {

			Music music = (Music) currentDirectory.getFiles().toArray()[position - currentDirectory.getDirectories().size()];

			try {
				MPDApplication app = (MPDApplication) getActivity().getApplication();

				int songId = -1;
				app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
				if (songId > -1) {
					app.oMPDAsyncHelper.oMPD.skipToId(songId);
				}

			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
