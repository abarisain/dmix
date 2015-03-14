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
import com.anpmech.mpd.connection.MPDConnectionListener;
import com.anpmech.mpd.event.StatusChangeListener;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.closedbits.CrashlyticsWrapper;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.ui.ToolbarHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BrowseFragment<T extends Item<T>> extends Fragment implements
        OnMenuItemClickListener, AsyncExecListener, OnItemClickListener, StatusChangeListener,
        MPDConnectionListener {

    public static final int ADD = 0;

    public static final int ADD_PLAY = 2;

    public static final int ADD_REPLACE = 1;

    public static final int ADD_REPLACE_PLAY = 4;

    public static final int ADD_TO_PLAYLIST = 3;

    public static final int GOTO_ARTIST = 5;

    public static final int MAIN = 0;

    public static final int PLAYLIST = 3;

    public static final int POPUP_COVER_BLACKLIST = 10;

    public static final int POPUP_COVER_SELECTIVE_CLEAN = 11;

    private static final String ARGUMENT_EMBEDDED = "embedded";

    private static final int MIN_ITEMS_BEFORE_FASTSCROLL = 50;

    private static final String TAG = "BrowseFragment";

    protected final MPDApplication mApp = MPDApplication.getInstance();

    final String mContext;

    final int mIrAdd;

    final int mIrAdded;

    private final List<PlaylistFile> mStoredPlaylists = new ArrayList<>();

    protected List<T> mItems;

    protected int mJobID = -1;

    protected AbsListView mList;

    protected TextView mLoadingTextView;

    protected View mLoadingView;

    protected View mNoResultView;

    protected Toolbar mToolbar;

    protected BrowseFragment(@StringRes final int rAdd, @StringRes final int rAdded,
            final String pContext) {
        super();

        mIrAdd = rAdd;
        mIrAdded = rAdded;

        mContext = pContext;

        setHasOptionsMenu(false);
    }

    protected abstract void add(final T item, final boolean replace, final boolean play);

    protected abstract void add(final T item, final PlaylistFile playlist);

    /**
     * This returns a runnable for adding from the parent adapter view adapter, used for clickable
     * items.
     *
     * @param parent   The parent AdapterView.
     * @param position The position in the adapter of the item to add.
     * @return A Runnable to execute in a thread, null if an error occurred.
     */
    Runnable addAdapterItem(final AdapterView<?> parent, final int position) {
        final boolean simpleMode = mApp.isInSimpleMode();
        final T track; /** final required for runnable. */
        Adapter adapter = null;
        Runnable runnable = null;

        if (parent == null) {
            track = null;
        } else {
            adapter = parent.getAdapter();
            if (adapter == null) {
                track = null;
            } else {
                track = (T) adapter.getItem(position);
            }
        }

        if (parent == null || adapter == null || track == null) {
            Tools.notifyUser(R.string.generalAddingError);
            final String errorMessage = "Failed to add track. parent: " + parent + " adapter: "
                    + adapter + " track: " + null;

            /** Temporary, I want to find out exactly what's null. */
            CrashlyticsWrapper.log(Log.ERROR, TAG, errorMessage);

            /** Track will always be null here. */
            Log.e(TAG, errorMessage);
        } else {
            runnable = new Runnable() {
                @Override
                public void run() {
                    add(track, simpleMode, simpleMode);
                }
            };
        }

        return runnable;
    }

    private void addAndReplace(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        mApp.getAsyncHelper().execAsync(new Runnable() {
            @Override
            public void run() {
                boolean replace = false;
                boolean play = false;
                switch (item.getGroupId()) {
                    case ADD_REPLACE_PLAY:
                        replace = true;
                        play = true;
                        break;
                    case ADD_REPLACE:
                        replace = true;
                        break;
                    case ADD_PLAY:
                        final MPDStatus status = mApp.getMPD().getStatus();

                        /**
                         * Let the user know if we're not going to play the added music.
                         */
                        if (status.isRandom() && status.isState(MPDStatusMap.STATE_PLAYING)) {
                            Tools.notifyUser(R.string.notPlayingInRandomMode);
                        } else {
                            play = true;
                        }
                        break;
                    default:
                        break;
                }
                add(mItems.get((int) info.id), replace, play);
            }
        });
    }

    private void addToPlaylist(final MenuItem item) {
        final EditText input = new EditText(getActivity());
        final int id = item.getOrder();
        if (item.getItemId() == 0) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.playlistName)
                    .setMessage(R.string.newPlaylistPrompt)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog,
                                        final int which) {
                                    final String name = input.getText().toString().trim();
                                    if (!name.isEmpty()) {
                                        addToPlaylistFile(new PlaylistFile(name), id);
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog,
                                        final int which) {
                                    // Do nothing.
                                }
                            }).show();
        } else {
            add(mItems.get(id), new PlaylistFile(item.getTitle().toString()));
        }
    }

    private void addToPlaylistFile(final PlaylistFile playlistFile, final int id) {
        mApp.getAsyncHelper().execAsync(new Runnable() {
            @Override
            public void run() {
                add(mItems.get(id), playlistFile);
            }
        });
    }

    @Override
    public void asyncExecSucceeded(final int jobID) {
        if (mJobID == jobID) {
            updateFromItems();
        }
    }

    protected abstract void asyncUpdate();

    /**
     * Called upon connection.
     */
    @Override
    public void connectionConnected() {
        storedPlaylistChanged();
        updateList();
    }

    /**
     * Called when connecting.
     */
    @Override
    public void connectionConnecting() {
    }

    /**
     * Called upon disconnection.
     *
     * @param reason The reason given for disconnection.
     */
    @Override
    public void connectionDisconnected(final String reason) {
    }

    // Override if you want setEmptyView to be called on the list even if you have a header
    protected boolean forceEmptyView() {
        return false;
    }

    protected ListAdapter getCustomListAdapter() {
        return new ArrayIndexerAdapter<>(getActivity(), R.layout.simple_list_item_1, mItems);
    }

    /*
     * Override this to display a custom loading text
     */
    @StringRes
    public int getLoadingText() {
        return R.string.loading;
    }

    /**
     * Should return the minimum number of songs in the queue before the fastscroll thumb is shown
     */
    protected int getMinimumItemsCountBeforeFastscroll() {
        return MIN_ITEMS_BEFORE_FASTSCROLL;
    }

    /*
     * Override this to display a custom activity title
     */
    public String getTitle() {
        return "";
    }

    /**
     * Override if you have your own toolbar
     */
    protected void hideToolbar() {
        if (mToolbar != null) {
            mToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Called when the MPD server update database starts and stops.
     *
     * @param updating  true when updating, false when not updating.
     * @param dbChanged After update, if the database has changed, this will be true else false.
     */
    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ActionBarActivity activity = (ActionBarActivity) getActivity();
        if (activity != null) {
            final ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        final int index = (int) info.id;
        if (index >= 0 && mItems.size() > index) {
            menu.setHeaderTitle(mItems.get((int) info.id).toString());
            // If in simple mode, show "Play" (add, replace & play), "Add to queue" and "Add to playlist"
            if (mApp.isInSimpleMode()) {
                final MenuItem playItem = menu.add(ADD_REPLACE_PLAY,
                        ADD_REPLACE_PLAY, 0, R.string.play);
                playItem.setOnMenuItemClickListener(this);
                final MenuItem addItem = menu.add(ADD, ADD, 0, R.string.addToQueue);
                addItem.setOnMenuItemClickListener(this);
            } else {
                final MenuItem addItem = menu.add(ADD, ADD, 0, mIrAdd);
                addItem.setOnMenuItemClickListener(this);
                final MenuItem addAndReplaceItem = menu
                        .add(ADD_REPLACE, ADD_REPLACE, 0,
                                R.string.addAndReplace);
                addAndReplaceItem.setOnMenuItemClickListener(this);
                final MenuItem addAndReplacePlayItem = menu.add(ADD_REPLACE_PLAY,
                        ADD_REPLACE_PLAY, 0, R.string.addAndReplacePlay);
                addAndReplacePlayItem.setOnMenuItemClickListener(this);
                final MenuItem addAndPlayItem = menu.add(ADD_PLAY, ADD_PLAY, 0,
                        R.string.addAndPlay);
                addAndPlayItem.setOnMenuItemClickListener(this);
            }

            if (R.string.addPlaylist != mIrAdd && R.string.addStream != mIrAdd &&
                    mApp.getMPD().isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS)) {

                int id = 0;
                final SubMenu playlistMenu = menu.addSubMenu(R.string.addToPlaylist);
                MenuItem item = playlistMenu.add(ADD_TO_PLAYLIST, id++, (int) info.id,
                        R.string.newPlaylist);
                item.setOnMenuItemClickListener(this);

                for (final PlaylistFile pl : mStoredPlaylists) {
                    item = playlistMenu.add(ADD_TO_PLAYLIST, id++, (int) info.id,
                            pl.getName());
                    item.setOnMenuItemClickListener(this);
                }
            }
            final MenuItem gotoArtistItem = menu
                    .add(GOTO_ARTIST, GOTO_ARTIST, 0, R.string.goToArtist);
            gotoArtistItem.setOnMenuItemClickListener(this);

        }
    }

    /**
     * Override this to add custom items to the Toolbar.
     */
    protected void onCreateToolbarMenu() {

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.browse, container, false);
        mList = (AbsListView) view.findViewById(R.id.list);
        registerForContextMenu(mList);
        mList.setOnItemClickListener(this);
        mLoadingView = view.findViewById(R.id.loadingLayout);
        mLoadingTextView = (TextView) view.findViewById(R.id.loadingText);
        mNoResultView = view.findViewById(R.id.noResultLayout);
        mLoadingTextView.setText(getLoadingText());

        setupStandardToolbar(view);

        return view;
    }

    @Override
    public void onDestroy() {
        try {
            mApp.getAsyncHelper().removeAsyncExecListener(this);
        } catch (final Exception e) {
            Log.e(TAG, "Error while destroying BrowseFragment", e);
        }
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        // help out the GC; imitated from ListFragment source
        mLoadingView = null;
        mLoadingTextView = null;
        mNoResultView = null;
        super.onDestroyView();
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        switch (item.getGroupId()) {
            case ADD_REPLACE_PLAY:
            case ADD_REPLACE:
            case ADD:
            case ADD_PLAY:
                addAndReplace(item);
                break;
            case ADD_TO_PLAYLIST:
                addToPlaylist(item);
                break;
            case GOTO_ARTIST:
                final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                final Object selectedItem = mItems.get((int) info.id);
                final Intent intent = new Intent(getActivity(), SimpleLibraryActivity.class);
                Artist artist = null;

                if (selectedItem instanceof Album) {
                    artist = ((Album) selectedItem).getArtist();
                } else if (selectedItem instanceof Artist) {
                    artist = (Artist) selectedItem;
                } else if (selectedItem instanceof Music) {
                    artist = new Artist(((Music) selectedItem).getAlbumArtistOrArtist());
                }

                if (artist != null) {
                    intent.putExtra(Artist.EXTRA, artist);
                    startActivityForResult(intent, -1);
                }
                break;
            default:
                final PlaylistFile playlist = new PlaylistFile(item.getTitle().toString());
                addToPlaylistFile(playlist, item.getOrder());
                break;
        }
        return false;
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        mApp.removeStatusChangeListener(this);
        mApp.getMPD().getConnectionStatus().removeListener(this);

        super.onPause();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();

        mApp.getMPD().getConnectionStatus().addListener(this);
        mApp.addStatusChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        updateList();
    }

    /**
     * Override this to add actions on toolbar menu item click
     */
    protected boolean onToolbarMenuItemClick(final MenuItem item) {
        return false;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mItems != null) {
            mList.setAdapter(getCustomListAdapter());
        }
        refreshFastScrollStyle();
    }

    /**
     * Called when playlist changes on MPD server.
     *
     * @param oldPlaylistVersion old playlist version.
     */
    @Override
    public void playlistChanged(final int oldPlaylistVersion) {
    }

    /**
     * Called when MPD server random feature changes state.
     */
    @Override
    public void randomChanged() {
    }

    /**
     * This method is used for the fast scroll visibility decision.<br/> Don't override this if you
     * want to change the fast scroll style, override {@link #refreshFastScrollStyle(boolean)}
     * instead.
     */
    protected void refreshFastScrollStyle() {
        refreshFastScrollStyle(mItems != null
                && mItems.size() >= getMinimumItemsCountBeforeFastscroll());
    }

    /**
     * This is required because setting the fast scroll prior to KitKat was important because of a
     * bug. This bug has since been corrected, but the opposite order is now required or the fast
     * scroll will not show.
     *
     * @param shouldShowFastScroll If the fast scroll should be shown or not
     */
    protected void refreshFastScrollStyle(final boolean shouldShowFastScroll) {
        if (shouldShowFastScroll) {
            refreshFastScrollStyle(View.SCROLLBARS_INSIDE_INSET, true);
        } else {
            refreshFastScrollStyle(View.SCROLLBARS_INSIDE_OVERLAY, false);
        }
    }

    /**
     * This is a helper method to workaround shortcomings of the fast scroll API.
     *
     * @param scrollbarStyle  The {@code View} scrollbar style.
     * @param isAlwaysVisible The visibility of the scrollbar.
     */
    final void refreshFastScrollStyle(final int scrollbarStyle, final boolean isAlwaysVisible) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mList.setFastScrollAlwaysVisible(isAlwaysVisible);
            mList.setScrollBarStyle(scrollbarStyle);
        } else {
            mList.setScrollBarStyle(scrollbarStyle);
            mList.setFastScrollAlwaysVisible(isAlwaysVisible);
        }
    }

    /**
     * Called when MPD server repeat feature changes state.
     */
    @Override
    public void repeatChanged() {
    }

    public void scrollToTop() {
        try {
            mList.setSelection(-1);
        } catch (final Exception e) {
            // What if the list is empty or some other bug ? I don't want any
            // crashes because of that
        }
    }

    /**
     * Set whether the fragment is embedded or not. An embedded BrowseFragment will not show a
     * toolbar.
     */
    public void setEmbedded(boolean embedded) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            arguments = new Bundle();
        }

        arguments.putBoolean(ARGUMENT_EMBEDDED, embedded);

        setArguments(arguments);

        updateToolbarVisibility();
    }

    /**
     * Set the view transition name in case it's used with a ListView
     */
    public void setViewTransitionName(String name) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            arguments = new Bundle();
        }

        //arguments.putBoolean(ARGUMENT_EMBEDDED, embedded);

        setArguments(arguments);
    }

    protected void setupStandardToolbar(View rootview) {
        mToolbar = (Toolbar) rootview.findViewById(R.id.toolbar);

        ToolbarHelper.showBackButton(this, mToolbar);
        ToolbarHelper.addSearchView(getActivity(), mToolbar);
        ToolbarHelper.addStandardMenuItemClickListener(this, mToolbar,
                new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(final MenuItem menuItem) {
                        return onToolbarMenuItemClick(menuItem);
                    }
                });
        onCreateToolbarMenu();
        mToolbar.setTitle(getTitle());
        updateToolbarVisibility();
        // TODO : Add "refresh" menu item
        // FIXME : Embedded fragments can't show their menu item. This is problematic, especially for FS and Streams.
    }

    /**
     * Override if you have your own toolbar
     */
    protected void showToolbar() {
        if (mToolbar != null) {
            mToolbar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Called when MPD state changes on server.
     *
     * @param oldState previous state.
     */
    @Override
    public void stateChanged(final int oldState) {
    }

    /**
     * Called when any sticker of any track has been changed on server.
     */
    @Override
    public void stickerChanged() {
    }

    /**
     * Called when a stored playlist has been modified, renamed, created or deleted.
     */
    @Override
    public void storedPlaylistChanged() {
        try {
            mStoredPlaylists.clear();
            mStoredPlaylists.addAll(mApp.getMPD().getPlaylists());
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to parse playlists.", e);
        }
    }

    /**
     * Called when playing track is changed on server.
     *
     * @param oldTrack track number before event.
     */
    @Override
    public void trackChanged(final int oldTrack) {
    }

    /**
     * Update the view from the items list if items is set.
     */
    public void updateFromItems() {
        if (getView() == null) {
            // The view has been destroyed, bail.
            return;
        }

        if (mItems != null) {
            mList.setAdapter(getCustomListAdapter());
        }
        try {
            if (forceEmptyView()
                    || mList instanceof ListView
                    && ((ListView) mList).getHeaderViewsCount() == 0) {
                mList.setEmptyView(mNoResultView);
            } else {
                if (mItems == null || mItems.isEmpty()) {
                    mNoResultView.setVisibility(View.VISIBLE);
                }
            }
        } catch (final Exception e) {
            Log.e(TAG, "Exception.", e);
        }

        mLoadingView.setVisibility(View.GONE);
        refreshFastScrollStyle();
    }

    public void updateList() {
        if (mApp.getMPD().isConnected()) {
            mList.setAdapter(null);
            mNoResultView.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.VISIBLE);

            // Loading Artists asynchronous...
            mApp.getAsyncHelper().addAsyncExecListener(this);
            mJobID = mApp.getAsyncHelper().execAsync(new Runnable() {
                @Override
                public void run() {
                    asyncUpdate();
                }
            });
        }
    }

    protected void updateToolbarVisibility() {
        boolean shouldShowToolbar = true;

        final Bundle arguments = getArguments();

        if (arguments != null) {
            shouldShowToolbar = !arguments.getBoolean(ARGUMENT_EMBEDDED, false);
        }

        if (shouldShowToolbar) {
            showToolbar();
        } else {
            hideToolbar();
        }
    }

    /**
     * Called when volume changes on MPD server.
     *
     * @param oldVolume volume before event
     */
    @Override
    public void volumeChanged(final int oldVolume) {
    }
}
