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

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Directory;
import org.a0z.mpd.item.FilesystemTreeEntry;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;
import org.a0z.mpd.item.PlaylistFile;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FSFragment extends BrowseFragment {

    private static final String EXTRA_DIRECTORY = "directory";

    private static final String TAG = "FSFragment";

    private Directory mCurrentDirectory = null;

    private String mDirectory = null;

    private int mNumSubDirs = 0; // number of subdirectories including ".."

    public FSFragment() {
        super(R.string.addDirectory, R.string.addedDirectoryToPlaylist,
                MPDCommand.MPD_SEARCH_FILENAME);
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        try {
            final Directory toAdd = mCurrentDirectory.getDirectory(item.getName());
            if (toAdd != null) {
                // Valid directory
                mApp.oMPDAsyncHelper.oMPD.add(toAdd, replace, play);
                Tools.notifyUser(R.string.addedDirectoryToPlaylist, item);
            } else {
                mApp.oMPDAsyncHelper.oMPD.add((FilesystemTreeEntry) item, replace, play);
                Tools.notifyUser(R.string.songAdded, item);
            }
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
        try {
            final Directory toAdd = mCurrentDirectory.getDirectory(item.getName());
            if (toAdd != null) {
                // Valid directory
                mApp.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, toAdd);
                Tools.notifyUser(R.string.addedDirectoryToPlaylist, item);
            } else {
                if (item instanceof Music) {
                    final ArrayList<Music> songs = new ArrayList<>();
                    songs.add((Music) item);
                    mApp.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, songs);
                    Tools.notifyUser(R.string.songAdded, item);
                }
                if (item instanceof PlaylistFile) {
                    mApp.oMPDAsyncHelper.oMPD.getPlaylist()
                            .load(((PlaylistFile) item).getFullPath());
                }
            }
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        if (!TextUtils.isEmpty(mDirectory)) {
            mCurrentDirectory = mApp.oMPDAsyncHelper.oMPD.getRootDirectory().makeDirectory(
                    mDirectory);
        } else {
            mCurrentDirectory = mApp.oMPDAsyncHelper.oMPD.getRootDirectory();
        }

        try {
            mCurrentDirectory.refreshData();
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to refresh current directory", e);
        }

        final ArrayList<Item> newItems = new ArrayList<>();
        final String fullPath = mCurrentDirectory.getFullPath();

        // add parent directory:
        if (fullPath != null && !fullPath.isEmpty()) {
            final Directory parent = new Directory(mCurrentDirectory.getParent());
            if (parent != null) {
                parent.setName("..");
                newItems.add(parent);
            }
        }
        newItems.addAll(mCurrentDirectory.getDirectories());
        mNumSubDirs = newItems.size(); // store number if subdirs
        newItems.addAll(mCurrentDirectory.getFiles());
        // Do not show playlists for root directory
        if (!TextUtils.isEmpty(mDirectory)) {
            newItems.addAll(mCurrentDirectory.getPlaylistFiles());
        }
        mItems = newItems;
    }

    // Disable the indexer for FSFragment
    @SuppressWarnings("unchecked")
    protected ListAdapter getCustomListAdapter() {
        return new ArrayAdapter<Item>(getActivity(), R.layout.fs_list_item,
                R.id.text1, (List<Item>) mItems) {
            @Override
            public View getView(final int position, final View convertView,
                    final ViewGroup parent) {
                final View v = super.getView(position, convertView, parent);
                final TextView subtext = (TextView) v.findViewById(R.id.text2);
                final Item item = mItems.get(position);
                final String filename;
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
        if (TextUtils.isEmpty(mDirectory)) {
            try {
                return getString(R.string.files);
            } catch (final IllegalStateException ignored) {
                // Can't get the translated string if we are not attached ...
                // Stupid workaround
                return "/";
            }
        } else {
            return mDirectory;
        }
    }

    public FSFragment init(final String path) {
        mDirectory = path;
        return this;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            init(savedInstanceState.getString(EXTRA_DIRECTORY));
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_fsmenu, menu);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        // click on a file, not dir
        if (position > mNumSubDirs - 1 || mNumSubDirs == 0) {

            final FilesystemTreeEntry item = (FilesystemTreeEntry) mItems.get(position);
            mApp.oMPDAsyncHelper.execAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        final int songId = -1;
                        if (item instanceof Music) {
                            mApp.oMPDAsyncHelper.oMPD
                                    .add(item, mApp.isInSimpleMode(), mApp.isInSimpleMode());
                            if (!mApp.isInSimpleMode()) {
                                Tools.notifyUser(R.string.songAdded, item);
                            }
                        } else if (item instanceof PlaylistFile) {
                            mApp.oMPDAsyncHelper.oMPD.getPlaylist().load(item.getFullPath());
                        }
                        if (songId > -1) {
                            mApp.oMPDAsyncHelper.oMPD.skipToId(songId);
                        }
                    } catch (final MPDServerException e) {
                        Log.e(TAG, "Failed to add.", e);
                    }
                }
            });
        } else {
            final String dir = ((Directory) mItems.toArray()[position]).getFullPath();
            ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                    new FSFragment().init(dir), "filesystem");
        }

    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Menu actions...
        switch (item.getItemId()) {
            case R.id.menu_update:
                mApp.oMPDAsyncHelper.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (TextUtils.isEmpty(mDirectory)) {
                                mApp.oMPDAsyncHelper.oMPD.refreshDatabase();
                            } else {
                                mApp.oMPDAsyncHelper.oMPD.refreshDatabase(mDirectory);
                            }
                        } catch (final MPDServerException e) {
                            Log.e(TAG, "Failed to refresh database.", e);
                        }
                    }
                });
                break;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putString(EXTRA_DIRECTORY, mDirectory);
        super.onSaveInstanceState(outState);
    }

}
