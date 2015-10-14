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

import com.anpmech.mpd.commandresponse.ObjectResponse;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.AbstractEntry;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.FilesystemTreeEntry;
import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.StringComparators;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FSFragment extends BrowseFragment {

    /**
     * This extra is used to keep the state of whether to use the back stack if a previous
     * directory ListView entry is clicked.
     */
    private static final String EXTRA_USE_BACK_STACK = "USE_BACK_STACK";

    /**
     * This is the descriptor for a subdirectory count entry in a {@link Bundle}.
     */
    private static final String SUBDIRECTORY_COUNT = "SUBDIRECTORY_COUNT";

    /**
     * The class log identifier.
     */
    private static final String TAG = "FSFragment";

    /**
     * The current directory.
     */
    private Directory mDirectory = Directory.byPath(Directory.ROOT_DIRECTORY);

    /**
     * The number of subdirectories in this directory, including the parent directory.
     */
    private int mNumSubDirs;

    /**
     * This field tracks whether to use the back stack if a previous directory list view entry is
     * clicked.
     */
    private boolean mUseBackStack;

    /**
     * Sole constructor.
     */
    public FSFragment() {
        super(R.string.addDirectory, R.string.addedDirectoryToPlaylist);
    }

    /**
     * This method takes a ObjectResponse and returns a ordered List.
     *
     * @param response The response return a ordered List for.
     * @param <T>      The type of the entries.
     * @return A ordered list.
     */
    private static <T extends AbstractEntry<T>> List<T> getOrderedEntries(
            final ObjectResponse<T> response) {
        final List<T> list = response.getList();

        Collections.sort(list, new EntryComparator<T>());

        return list;
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        try {
            final String name = item.getName();

            if (mDirectory.containsPath(name)) {
                // Valid directory
                mApp.getMPD().add(Directory.byPath(name), replace, play);
                Tools.notifyUser(R.string.addedDirectoryToPlaylist, item);
            } else {
                mApp.getMPD().add((FilesystemTreeEntry) item, replace, play);
                if (item instanceof PlaylistFile) {
                    Tools.notifyUser(R.string.playlistAdded, item);
                } else {
                    Tools.notifyUser(R.string.songAdded, item);
                }
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Item item, final PlaylistFile playlist) {
        try {
            final String name = item.getName();

            if (mDirectory.containsPath(name)) {
                // Valid directory
                mApp.getMPD().addToPlaylist(playlist, Directory.byPath(name));
                Tools.notifyUser(R.string.addedDirectoryToPlaylist, item);
            } else {
                if (item instanceof Music) {
                    mApp.getMPD().addToPlaylist(playlist, (Music) item);
                    Tools.notifyUser(R.string.songAdded, item);
                } else if (item instanceof PlaylistFile) {
                    mApp.getMPD().getPlaylist()
                            .load(((FilesystemTreeEntry) item).getFullPath());
                }
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        final List<FilesystemTreeEntry> newItems;
        refreshDirectory();

        /**
         * This should not happen, unless, there's a connection problem.
         */
        if (mDirectory.isEmpty()) {
            Log.e(TAG, "Directory failed to update. Displaying blank directory.");
            newItems = Collections.emptyList();
        } else {
            final Collection<Directory> directories =
                    getOrderedEntries(mDirectory.getDirectoryEntries());
            final Collection<Music> files = getOrderedEntries(mDirectory.getMusicEntries());
            final int size = directories.size() + files.size();
            final boolean rootDirectory = mDirectory.getFullPath().isEmpty();

            newItems = new ArrayList<>(size);
            // Hack to add the two dot leader for parent directory
            if (!rootDirectory) {
                newItems.add(Directory.byPath("‥"));
            }
            newItems.addAll(directories);
            mNumSubDirs = newItems.size(); // store number if subdirectory
            newItems.addAll(files);
            // Do not show play lists for root directory
            if (!rootDirectory) {
                newItems.addAll(getOrderedEntries(mDirectory.getPlaylistFileEntries()));
            }
        }
        replaceItems(newItems);
    }

    /**
     * This method creates and pushes a filesystem fragment.
     *
     * @param path         The path of the filesystem fragment to create.
     * @param useBackStack Whether to use this Activity back stack if the previous directory entry
     *                     is hit.
     */
    private void createFilesystemFragment(final String path, final boolean useBackStack) {
        final Activity activity = getActivity();

        if (activity != null) {
            final Bundle bundle = new Bundle(2);
            final Fragment fragment =
                    Fragment.instantiate(activity, FSFragment.class.getName(), bundle);

            bundle.putParcelable(Directory.EXTRA, Directory.byPath(path));
            bundle.putBoolean(EXTRA_USE_BACK_STACK, useBackStack);

            ((ILibraryFragmentActivity) activity)
                    .pushLibraryFragment(fragment, "filesystem");
        }
    }

    @Override
    protected Artist getArtist(final Item item) {
        return null;
    }

    // Disable the indexer for FSFragment
    @Override
    protected ListAdapter getCustomListAdapter() {
        return new FSTreeEntryArrayAdapter(getActivity(), mItems);
    }

    /**
     * This method returns the default string resource.
     *
     * @return The default string resource.
     */
    @Override
    @StringRes
    public int getDefaultTitle() {
        return R.string.files;
    }

    @Override
    public String getTitle() {
        final String title;
        final String fullPath = mDirectory.getFullPath();

        /** If fullPath is empty, we're at the root directory. */
        if (fullPath.isEmpty()) {
            title = super.getTitle();
        } else {
            title = fullPath;
        }

        return title;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle;
        if (savedInstanceState == null) {
            bundle = getArguments();
        } else {
            bundle = savedInstanceState;
        }

        if (bundle != null) {
            if (bundle.containsKey(Directory.EXTRA)) {
                mDirectory = bundle.getParcelable(Directory.EXTRA);
                mNumSubDirs = bundle.getInt(SUBDIRECTORY_COUNT);
            }
            mUseBackStack = bundle.getBoolean(EXTRA_USE_BACK_STACK, true);
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
            mApp.getAsyncHelper().execAsync(new AddFSItem(item));
        } else {
            final String path = ((FilesystemTreeEntry) mItems.get(position)).getFullPath();

            /**
             * This hack is the back stack action that happens if the directory name is a two dot
             * leader.
             */
            if ((int) path.charAt(0) - (int) '‥' == 0) {
                final FragmentManager fragmentManager = getFragmentManager();

                if (fragmentManager == null || fragmentManager.getBackStackEntryCount() == 0 ||
                        !mUseBackStack) {
                    createFilesystemFragment(mDirectory.getParent(), false);
                } else {
                    fragmentManager.popBackStack();
                }
            } else {
                createFilesystemFragment(path, true);
            }
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putParcelable(Directory.EXTRA, mDirectory);
        outState.putInt(SUBDIRECTORY_COUNT, mNumSubDirs);
        outState.putBoolean(EXTRA_USE_BACK_STACK, mUseBackStack);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected boolean onToolbarMenuItemClick(final MenuItem item) {
        final boolean itemConsumed;

        // Menu actions...
        if (item.getItemId() == R.id.menu_update) {
            mApp.getAsyncHelper().execAsync(new RefreshDatabase(mDirectory));
            itemConsumed = true;
        } else {
            itemConsumed = super.onToolbarMenuItemClick(item);
        }

        return itemConsumed;
    }

    private void refreshDirectory() {
        boolean successfulRefresh = false;

        try {
            mApp.getMPD().refresh(mDirectory);
            successfulRefresh = true;
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to refresh current directory: " + mDirectory, e);
        }

        if (!successfulRefresh) {
            final Directory directory = Directory.byPath(Directory.ROOT_DIRECTORY);

            /**
             * The current directory disappeared. Perhaps the server changed, or an update
             * completed.
             */
            try {
                mApp.getMPD().refresh(directory);
                successfulRefresh = true;
            } catch (final IOException | MPDException ignore) {
            }

            if (successfulRefresh) {
                mDirectory = directory;
            }
        }
    }

    /**
     * This class adds an item.
     */
    private static final class AddFSItem implements Runnable {

        /**
         * The item to add.
         */
        private final FilesystemTreeEntry mItem;

        /**
         * Sole constructor.
         *
         * @param item Item to add.
         */
        private AddFSItem(final FilesystemTreeEntry item) {
            super();

            mItem = item;
        }

        @Override
        public void run() {
            final MPDApplication app = MPDApplication.getInstance();

            try {
                if (mItem instanceof Music) {
                    final boolean inSimpleMode = app.isInSimpleMode();
                    app.getMPD().add(mItem, inSimpleMode, inSimpleMode);
                    if (!inSimpleMode) {
                        Tools.notifyUser(R.string.songAdded, mItem);
                    }
                } else if (mItem instanceof PlaylistFile) {
                    app.getMPD().getPlaylist().load(mItem.getFullPath());
                }
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to add item: " + mItem, e);
            }
        }
    }

    /**
     * This is the comparator used to sort entries in accordance to their natural order.
     *
     * @param <T> The AbstractEntry to sort.
     */
    private static final class EntryComparator<T extends AbstractEntry<T>>
            implements Comparator<T> {

        /**
         * Sole constructor.
         */
        private EntryComparator() {
            super();
        }

        /**
         * Compares two strings using the current locale's rules and comparing contained numbers
         * based on their numeric values.
         *
         * @param lhs The first string to compare.
         * @param rhs The second string to compare.
         * @return zero iff {@code s} and {@code t} are equal, a value less than zero iff {@code s}
         * lexicographically precedes {@code t} and a value larger than zero iff {@code s}
         * lexicographically follows {@code t}
         */
        @Override
        public int compare(final T lhs, final T rhs) {
            return StringComparators.compareNatural(lhs.getName(), rhs.getName());
        }
    }

    /**
     * This class is a custom ArrayAdapter for this class.
     */
    private static final class FSTreeEntryArrayAdapter extends ArrayAdapter<FilesystemTreeEntry> {

        /**
         * Sole constructor.
         *
         * @param activity The current activity.
         * @param items    The items for this ArrayAdapter.
         */
        private FSTreeEntryArrayAdapter(final Activity activity,
                final List<FilesystemTreeEntry> items) {
            super(activity, R.layout.fs_list_item, R.id.name_metadata, items);
        }

        @Override
        public View getView(final int position, final View convertView,
                final ViewGroup parent) {
            final View v = super.getView(position, convertView, parent);
            final TextView subtext = (TextView) v.findViewById(R.id.full_path);
            final FilesystemTreeEntry item = getItem(position);

            if (item instanceof Music) {
                final String filename = item.getFullPath();
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
    }

    /**
     * This class implements a {@link Runnable} to refresh the database of a specific {@link
     * Directory}.
     */
    private static final class RefreshDatabase implements Runnable {

        /**
         * The {@link Directory} to refresh.
         */
        private final Directory mDirectory;

        /**
         * Sole constructor.
         *
         * @param directory The directory to refresh.
         */
        private RefreshDatabase(final Directory directory) {
            super();

            mDirectory = directory;
        }

        @Override
        public void run() {
            try {
                MPDApplication.getInstance().getMPD().refreshDatabase(mDirectory.getFullPath());
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to refresh database.", e);
            }
        }
    }
}
