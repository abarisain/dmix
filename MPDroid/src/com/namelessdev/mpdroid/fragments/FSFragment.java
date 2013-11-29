package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.Directory;
import org.a0z.mpd.FilesystemTreeEntry;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Music;
import org.a0z.mpd.PlaylistFile;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.util.Log;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

public class FSFragment extends BrowseFragment {
	private static final String EXTRA_DIRECTORY = "directory";

	private Directory currentDirectory = null;
	private String directory = null;
        private int numSubdirs = 0; // number of subdirectories including ".."

	public FSFragment() {
		super(R.string.addDirectory, R.string.addedDirectoryToPlaylist, MPDCommand.MPD_SEARCH_FILENAME);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null)
			init(icicle.getString(EXTRA_DIRECTORY));
	}

	@Override
	public String getTitle() {
		if(directory == null) {
			return getString(R.string.files);
		} else {
			return directory;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_DIRECTORY, directory);
		super.onSaveInstanceState(outState);
	}

	public FSFragment init(String path) {
		directory = path;
		return this;
	}

	@Override
	protected void add(Item item, boolean replace, boolean play) {
		try {
			final Directory ToAdd = currentDirectory.getDirectory(item.getName());
			if (ToAdd != null) {
				// Valid directory
				app.oMPDAsyncHelper.oMPD.add(ToAdd, replace, play);
				Tools.notifyUser(String.format(getResources().getString(R.string.addedDirectoryToPlaylist), item),
						FSFragment.this.getActivity());
			} else {
				app.oMPDAsyncHelper.oMPD.add((FilesystemTreeEntry) item, replace, play);
				Tools.notifyUser(getResources().getString(R.string.songAdded, item), FSFragment.this.getActivity());
			}
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void add(Item item, String playlist) {
		try {
			Directory ToAdd = currentDirectory.getDirectory(item.getName());
			if (ToAdd != null) {
				// Valid directory
				app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, ToAdd);
				Tools.notifyUser(String.format(getResources().getString(R.string.addedDirectoryToPlaylist), item),
						FSFragment.this.getActivity());
			} else {
				if (item instanceof Music) {
					ArrayList<Music> songs = new ArrayList<Music>();
					songs.add((Music) item);
					app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, songs);
					Tools.notifyUser(getResources().getString(R.string.songAdded, item), FSFragment.this.getActivity());
				}  if (item instanceof PlaylistFile) {
					app.oMPDAsyncHelper.oMPD.getPlaylist().load(((PlaylistFile)item).getFullpath());
				}
			}
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void asyncUpdate() {
		if (directory != null) {
			currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory().makeDirectory(directory);
		} else {
			currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory();
		}

		try {
			currentDirectory.refreshData();
		} catch (MPDServerException e) {
			e.printStackTrace();
		}

		ArrayList<Item> newItems = new ArrayList<Item>();
                // add parent directory:
                if (!"".equals(currentDirectory.getFullpath())) {
                    Directory parent = new Directory(currentDirectory.getParent());
                    if (parent != null) {
                        parent.setName("..");
                        newItems.add(parent);
                    }
                }
		newItems.addAll(currentDirectory.getDirectories());
                numSubdirs = newItems.size(); // stors number if subdirs
		newItems.addAll(currentDirectory.getFiles());
		//Do not show playlists for root directory
		if (directory != null) {
			newItems.addAll(currentDirectory.getPlaylistFiles());
		}
		items=newItems;
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
            // click on a file, not dir
            if (position > numSubdirs - 1 || numSubdirs == 0) {

			final FilesystemTreeEntry item = (FilesystemTreeEntry)items.get(position);
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						int songId = -1;
						if (item instanceof Music) {
							app.oMPDAsyncHelper.oMPD.getPlaylist().add(item);
						} else if (item instanceof PlaylistFile) {
							app.oMPDAsyncHelper.oMPD.getPlaylist().load(item.getFullpath());
						}
						if (songId > -1) {
							app.oMPDAsyncHelper.oMPD.skipToId(songId);
						}
					} catch (MPDServerException e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			final String dir = ((Directory) items.toArray()[position]).getFullpath();
			((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new FSFragment().init(dir), "filesystem");
		}

	}

	//Disable the indexer for FSFragment
	@SuppressWarnings("unchecked")
	protected ListAdapter getCustomListAdapter() {
		return new ArrayAdapter<Item>(getActivity(), R.layout.simple_list_item_1, (List<Item>) items);
	}

}
