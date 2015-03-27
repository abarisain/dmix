/*
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.fragments;

import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.FilesystemTreeEntry;
import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class FSFragment extends BrowseFragment {

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
            if (toAdd == null) {
                mApp.getMPD().add((FilesystemTreeEntry) item, replace, play);
                if (item instanceof PlaylistFile) {
                    Tools.notifyUser(R.string.playlistAdded, item);
                } else {
                    Tools.notifyUser(R.string.songAdded, item);
                }
            } else {
                // Valid directory
                mApp.getMPD().add(toAdd, replace, play);
                Tools.notifyUser(R.string.addedDirectoryToPlaylist, item);
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Item item, final PlaylistFile playlist) {
        try {
            final Directory toAdd = mCurrentDirectory.getDirectory(item.getName());
            if (toAdd == null) {
                if (item instanceof Music) {
                    mApp.getMPD().addToPlaylist(playlist, (Music) item);
                    Tools.notifyUser(R.string.songAdded, item);
                } else if (item instanceof PlaylistFile) {
                    mApp.getMPD().getPlaylist()
                            .load(((FilesystemTreeEntry) item).getFullPath());
                }
            } else {
                // Valid directory
                mApp.getMPD().addToPlaylist(playlist, toAdd);
                Tools.notifyUser(R.string.addedDirectoryToPlaylist, item);
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        refreshDirectory();
        final Collection<Directory> directories = mCurrentDirectory.getDirectories();
        final Collection<Music> files = mCurrentDirectory.getFiles();
        final Collection<PlaylistFile> playlistFiles = mCurrentDirectory.getPlaylistFiles();
        final int size = directories.size() + files.size() + playlistFiles.size() + 10;
        final ArrayList<FilesystemTreeEntry> newItems = new ArrayList<>(size);
        final String fullPath = mCurrentDirectory.getFullPath();

        // add parent directory:
        if (fullPath != null && !fullPath.isEmpty()) {
            final Directory parent = mCurrentDirectory.makeParentDirectory("‥");
            newItems.add(parent);
        }
        newItems.addAll(directories);
        mNumSubDirs = newItems.size(); // store number if subdirectory
        newItems.addAll(files);
        // Do not show play lists for root directory
        if (!TextUtils.isEmpty(mDirectory)) {
            newItems.addAll(playlistFiles);
        }
        replaceItems(newItems);
    }

    @Override
    protected Artist getArtist(final Item item) {
        return null;
    }

    // Disable the indexer for FSFragment
    @Override
    protected ListAdapter getCustomListAdapter() {
        return new ArrayAdapter<FilesystemTreeEntry>(getActivity(), R.layout.fs_list_item,
                R.id.text1, mItems) {
            @Override
            public View getView(final int position, final View convertView,
                    final ViewGroup parent) {
                final View v = super.getView(position, convertView, parent);
                final TextView subtext = (TextView) v.findViewById(R.id.text2);
                final FilesystemTreeEntry item = (FilesystemTreeEntry) mItems.get(position);
                final String filename;
                if (item instanceof Music) {
                    filename = item.getFullPath();
                    if (TextUtils.isEmpty(filename) || item.toString().equals(filename)) {
                        subtext.setVisibility(View.GONE);
                    } else {
                        subtext.setVisibility(View.VISIBLE);
                        subtext.setText(filename);
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
        String title;

        if (TextUtils.isEmpty(mDirectory)) {
            try {
                title = getString(R.string.files);
            } catch (final IllegalStateException ignored) {
                // Can't get the translated string if we are not attached ...
                // Stupid workaround
                title = "/";
            }
        } else {
            title = mDirectory;
        }

        return title;
    }

    public FSFragment init(final String path) {
        mDirectory = path;
        return this;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            init(savedInstanceState.getString(Directory.EXTRA));
        }
    }

    @Override
    protected void onCreateToolbarMenu() {
        super.onCreateToolbarMenu();
        mToolbar.inflateMenu(R.menu.mpd_fsmenu);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        // click on a file, not dir
        if (position > mNumSubDirs - 1 || mNumSubDirs == 0) {

            final FilesystemTreeEntry item = (FilesystemTreeEntry) mItems.get(position);
            mApp.getAsyncHelper().execAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (item instanceof Music) {
                            mApp.getMPD().add(item, mApp.isInSimpleMode(), mApp.isInSimpleMode());
                            if (!mApp.isInSimpleMode()) {
                                Tools.notifyUser(R.string.songAdded, item);
                            }
                        } else if (item instanceof PlaylistFile) {
                            mApp.getMPD().getPlaylist().load(item.getFullPath());
                        }
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to add.", e);
                    }
                }
            });
        } else {
            final String dir = ((FilesystemTreeEntry) mItems.toArray()[position]).getFullPath();
            ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                    new FSFragment().init(dir), "filesystem");
        }

    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putString(Directory.EXTRA, mDirectory);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected boolean onToolbarMenuItemClick(final MenuItem item) {
        // Menu actions...
        if (item.getItemId() == R.id.menu_update) {
            mApp.getAsyncHelper().execAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        mApp.getMPD().refreshDatabase(mDirectory);
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to refresh database.", e);
                    }
                }
            });
            return true;
        }

        return super.onToolbarMenuItemClick(item);
    }

    private void refreshDirectory() {
        if (TextUtils.isEmpty(mDirectory)) {
            mCurrentDirectory = Directory.getRoot();
        } else {
            mCurrentDirectory = Directory.getRoot().makeChildDirectory(mDirectory);
        }

        try {
            mApp.getMPD().refreshDirectory(mCurrentDirectory);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to refresh current directory", e);
        }

    }

}
