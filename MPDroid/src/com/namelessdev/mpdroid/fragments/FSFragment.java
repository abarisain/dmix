/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.Directory;
import org.a0z.mpd.FilesystemTreeEntry;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Music;
import org.a0z.mpd.PlaylistFile;
import org.a0z.mpd.exception.MPDServerException;

import java.util.ArrayList;
import java.util.List;

public class FSFragment extends BrowseFragment {
    private static final String EXTRA_DIRECTORY = "directory";

    private Directory currentDirectory = null;
    private String directory = null;
    private int numSubdirs = 0; // number of subdirectories including ".."

    public FSFragment() {
        super(R.string.addDirectory, R.string.addedDirectoryToPlaylist,
                MPDCommand.MPD_SEARCH_FILENAME);
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            final Directory ToAdd = currentDirectory.getDirectory(item.getName());
            if (ToAdd != null) {
                // Valid directory
                app.oMPDAsyncHelper.oMPD.add(ToAdd, replace, play);
                Tools.notifyUser(String.format(
                        getResources().getString(R.string.addedDirectoryToPlaylist), item),
                        FSFragment.this.getActivity());
            } else {
                app.oMPDAsyncHelper.oMPD.add((FilesystemTreeEntry) item, replace, play);
                Tools.notifyUser(getResources().getString(R.string.songAdded, item),
                        FSFragment.this.getActivity());
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
                Tools.notifyUser(String.format(
                        getResources().getString(R.string.addedDirectoryToPlaylist), item),
                        FSFragment.this.getActivity());
            } else {
                if (item instanceof Music) {
                    ArrayList<Music> songs = new ArrayList<Music>();
                    songs.add((Music) item);
                    app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, songs);
                    Tools.notifyUser(getResources().getString(R.string.songAdded, item),
                            FSFragment.this.getActivity());
                }
                if (item instanceof PlaylistFile) {
                    app.oMPDAsyncHelper.oMPD.getPlaylist()
                            .load(((PlaylistFile) item).getFullpath());
                }
            }
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void asyncUpdate() {
        if (!TextUtils.isEmpty(directory)) {
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
        // Do not show playlists for root directory
        if (!TextUtils.isEmpty(directory)) {
            newItems.addAll(currentDirectory.getPlaylistFiles());
        }
        items = newItems;
    }

    // Disable the indexer for FSFragment
    @SuppressWarnings("unchecked")
    protected ListAdapter getCustomListAdapter() {
        return new ArrayAdapter<Item>(getActivity(), R.layout.fs_list_item,
                R.id.text1, (List<Item>) items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final View v = super.getView(position, convertView, parent);
                final TextView subtext = (TextView) v.findViewById(R.id.text2);
                final Item item = items.get(position);
                String filename;
                if (item instanceof Music) {
                    filename = ((Music) item).getFilename();
                    if (!TextUtils.isEmpty(filename) && !item.toString().equals(filename)) {
                        subtext.setVisibility(View.VISIBLE);
                        subtext.setText(filename);
                    } else {
                        subtext.setVisibility(View.GONE);
                    }
                } else {
                    subtext.setVisibility(View.GONE);
                }

                return v;
            }
        };
    }

    @Override
    public String getTitle() {
        if (TextUtils.isEmpty(directory)) {
            try {
                return getString(R.string.files);
            } catch (IllegalStateException e) {
                // Can't get the translated string if we are not attached ...
                // Stupid workaround
                return "/";
            }
        } else {
            return directory;
        }
    }

    public FSFragment init(String path) {
        directory = path;
        return this;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        if (icicle != null)
            init(icicle.getString(EXTRA_DIRECTORY));
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        // click on a file, not dir
        if (position > numSubdirs - 1 || numSubdirs == 0) {

            final FilesystemTreeEntry item = (FilesystemTreeEntry) items.get(position);
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
            ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                    new FSFragment().init(dir), "filesystem");
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(EXTRA_DIRECTORY, directory);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_fsmenu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu actions...
        switch (item.getItemId()) {
            case R.id.menu_update:
                app.oMPDAsyncHelper.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (TextUtils.isEmpty(directory)) {
                                app.oMPDAsyncHelper.oMPD.refreshDatabase();
                            } else {
                                app.oMPDAsyncHelper.oMPD.refreshDatabase(directory);
                            }
                        } catch (MPDServerException e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
        }
        return false;
    }

}
