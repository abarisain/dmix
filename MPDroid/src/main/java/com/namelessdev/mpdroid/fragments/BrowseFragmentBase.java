/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.commandresponse.PlaylistFileResponse;
import com.anpmech.mpd.connection.MPDConnectionListener;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.PlaylistFile;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;
import com.anpmech.mpd.subsystem.status.StatusChangeListener;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.helpers.MPDAsyncWorker;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.ui.ToolbarHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class BrowseFragmentBase<T extends Item<T>> extends Fragment implements
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

    public static final int POPUP_ADD_TO_FAVORITES = 12;

    public static final int POPUP_REMOVE_FROM_FAVORITES = 24;

    /**
     * This is the group number used to enable or disable the playlist add group.
     */
    protected static final int PLAYLIST_ADD_GROUP = 1;

    /**
     * This is a collection of current playlist files.
     */
    protected static final Collection<PlaylistFile> PLAYLIST_FILES = new ArrayList<>();

    protected static final String TAG = "BrowseFragment";

    /**
     * This token is called back when {@link #asyncComplete(CharSequence)} is called, after a
     * playlist update.
     */
    protected static final CharSequence UPDATE_PLAYLISTS = "UPDATE_PLAYLISTS";

    private static final String ARGUMENT_EMBEDDED = "embedded";

    /**
     * The token called back when {@link #asyncComplete(CharSequence)} is called, requiring a
     * async update.
     */
    private static final CharSequence ASYNC_UPDATE_TOKEN = "ASYNC_UPDATE";

    private static final int MIN_ITEMS_BEFORE_FASTSCROLL = 50;

    protected final MPDApplication mApp = MPDApplication.getInstance();

    protected final List<T> mItems = new ArrayList<>();

    final int mIrAdd;

    final int mIrAdded;

    /**
     * This runnable is run in our {@link MPDAsyncWorker} thread.
     */
    private final Runnable mAsyncUpdate = new Runnable() {
        @Override
        public void run() {
            asyncUpdate();
        }
    };

    /**
     * This is set to true to ensure the playlist is only updated once, since it is static.
     */
    private final AtomicBoolean mPlaylistUpdated = new AtomicBoolean();

    protected AbsListView mList;

    protected TextView mLoadingTextView;

    protected View mLoadingView;

    protected View mNoResultView;

    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            updateList();
        }
    };

    protected Toolbar mToolbar;

    /**
     * This field corresponds to the last DB update time.
     */
    private long mLastDBUpdate;

    protected BrowseFragmentBase(@StringRes final int rAdd, @StringRes final int rAdded) {
        super();

        mIrAdd = rAdd;
        mIrAdded = rAdded;

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
     */
    protected void addAdapterItem(final AdapterView<?> parent, final int position) {
        final boolean simpleMode = mApp.isInSimpleMode();
        final T track; /** final required for runnable. */
        Adapter adapter = null;

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
            reportTrackFailure(parent, adapter);
        } else {
            mApp.getAsyncHelper().execAsync(new Runnable() {
                @Override
                public void run() {
                    add(track, simpleMode, simpleMode);
                }
            });
        }
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
                                        addToPlaylistFile(PlaylistFile.byPath(name), id);
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, Tools.NOOP_CLICK_LISTENER)
                    .show();
        } else {
            add(mItems.get(id), PlaylistFile.byPath(item.getTitle().toString()));
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
    public void asyncComplete(final CharSequence token) {
        if (ASYNC_UPDATE_TOKEN.equals(token)) {
            updateFromItems();
        }
    }

    protected abstract void asyncUpdate();

    /**
     * This method checks the database for change, and updates the list accordingly.
     */
    public void checkDatabase() {
        final MPD mpd = mApp.getMPD();
        final long updateTime = mpd.getStatistics().getDBUpdateTime().getTime();

        if (updateTime != mLastDBUpdate && mpd.isConnected()) {
            updateList();
            mLastDBUpdate = updateTime;
        }
    }

    /**
     * Called upon connection.
     *
     * @param commandErrorCode If this number is non-zero, the number will correspond to a
     *                         {@link MPDException} error code. If this number is zero, the
     *                         connection MPD protocol commands were successful.
     */
    @Override
    public void connectionConnected(final int commandErrorCode) {
        storedPlaylistChanged();
        checkDatabase();
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

    /**
     * Retrieves the Artist for the applicable type.
     *
     * @param item The item to get the Artist for.
     * @return The Artist for the item.
     */
    protected abstract Artist getArtist(final T item);

    protected ListAdapter getCustomListAdapter() {
        return new ArrayIndexerAdapter<>(getActivity(), R.layout.simple_list_item_1, mItems);
    }

    /**
     * Should return a default string resource, -1 if no string resource exists.
     *
     * @return A default string resource, -1 if no string resource exists.
     */
    @StringRes
    public abstract int getDefaultTitle();

    /**
     * Override that method if you want BrowseFragment to inflate another layout.
     *
     * @return The layout resource ID.
     */
    @LayoutRes
    protected int getLayoutResId() {
        return R.layout.browse;
    }

    /*
     * Override this to display a custom loading text
     */
    @StringRes
    public int getLoadingText() {
        return R.string.loading;
    }

    /**
     * Should return the minimum number of songs in the queue before the fastscroll thumb is shown.
     *
     * @return The minimum items count before fast scrolling can begin.
     */
    protected int getMinimumItemsCountBeforeFastscroll() {
        return MIN_ITEMS_BEFORE_FASTSCROLL;
    }

    /**
     * This method sets a custom activity title, if the {@link #getDefaultTitle()} method doesn't
     * return -1.
     *
     * @return The string from the string resource {@link #getDefaultTitle()}, empty string if
     * {@link #getDefaultTitle()} is -1.
     */
    public String getTitle() {
        final int defaultRes = getDefaultTitle();
        final String title;

        if (defaultRes == -1) {
            title = "";
        } else {
            title = mApp.getString(getDefaultTitle());
        }

        return title;
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
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
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
        final T item = mItems.get(index);

        if (index >= 0 && mItems.size() > index) {
            menu.setHeaderTitle(item.toString());
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

            if (mApp.getMPD().isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS)) {
                int id = 0;
                final SubMenu playlistMenu = menu.addSubMenu(PLAYLIST_ADD_GROUP, Menu.NONE,
                        Menu.NONE, R.string.addToPlaylist);
                MenuItem menuItem = playlistMenu.add(ADD_TO_PLAYLIST, id++, index,
                        R.string.newPlaylist);
                menuItem.setOnMenuItemClickListener(this);

                for (final PlaylistFile pl : PLAYLIST_FILES) {
                    menuItem = playlistMenu.add(ADD_TO_PLAYLIST, id++, index, pl.getName());
                    menuItem.setOnMenuItemClickListener(this);
                }
            }

            //if (getArtist(item) != null) {
            //    final MenuItem gotoArtistItem =
            //            menu.add(GOTO_ARTIST, GOTO_ARTIST, 0, R.string.goToArtist);
            //    gotoArtistItem.setOnMenuItemClickListener(this);
            //}
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
        final View view = inflater.inflate(getLayoutResId(), container, false);
        mList = (AbsListView) view.findViewById(R.id.list);
        registerForContextMenu(mList);
        mList.setOnItemClickListener(this);
        mLoadingView = view.findViewById(R.id.loadingLayout);
        mLoadingTextView = (TextView) view.findViewById(R.id.loadingText);
        mNoResultView = view.findViewById(R.id.noResultLayout);
        mLoadingTextView.setText(getLoadingText());

        setupStandardToolbar(view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mList.setNestedScrollingEnabled(true);
        }

        return view;
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {
        mList.setOnItemClickListener(null);

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
                final Intent intent = new Intent(getActivity(), SimpleLibraryActivity.class);
                final Artist artist = getArtist(mItems.get((int) info.id));

                if (artist != null) {
                    intent.putExtra(Artist.EXTRA, artist);
                    startActivityForResult(intent, -1);
                }
                break;
            default:
                final PlaylistFile playlist = PlaylistFile.byPath(item.getTitle().toString());
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
        LocalBroadcastManager.getInstance(MPDApplication.getInstance())
                .unregisterReceiver(mLocalBroadcastReceiver);

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
        final IntentFilter filter = new IntentFilter(MPDApplication.INTENT_ACTION_REFRESH);
        LocalBroadcastManager.getInstance(MPDApplication.getInstance()).registerReceiver(
                mLocalBroadcastReceiver, filter);
        checkDatabase();
    }

    @Override
    public void onStart() {
        super.onStart();

        /**
         * Every Fragment is initialized on start, to prevent initializing the playlist during each
         * Fragment initialization, the mPlaylistUpdated boolean should only allow one through.
         */
        if (!mPlaylistUpdated.getAndSet(true) && mApp.getMPD().isConnected()) {
            storedPlaylistChanged();
        }
    }

    /**
     * Override this to add actions on toolbar menu item click.
     *
     * @param item Information about the menu item which was clicked.
     * @return True if acted upon, false otherwise.
     */
    protected boolean onToolbarMenuItemClick(final MenuItem item) {
        return false;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList.setAdapter(getCustomListAdapter());
        refreshFastScrollStyle();
    }

    /**
     * Called upon a change in the Output idle subsystem.
     */
    @Override
    public void outputsChanged() {
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
     * This method is used for the fast scroll visibility decision.
     *
     * <p>Don't override this if you want to change the fast scroll style, override
     * {@link #refreshFastScrollStyle(boolean)} instead.</p>
     */
    protected void refreshFastScrollStyle() {
        refreshFastScrollStyle(mItems.size() >= getMinimumItemsCountBeforeFastscroll());
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

    protected void replaceItems(final Collection<T> items) {
        synchronized (mItems) {
            mItems.clear();
            mItems.addAll(items);
        }
    }

    /**
     * This method is used to report a strange exception that has occurred.
     *
     * @param parent  The parent AdapterView to log.
     * @param adapter The Adapter to log.
     */
    protected void reportTrackFailure(final View parent, final Adapter adapter) {
        Tools.notifyUser(R.string.generalAddingError);

        Log.e(TAG, trackFailureString(parent, adapter));
    }

    /**
     * Set whether the fragment is embedded or not. An embedded BrowseFragment will not show a
     * toolbar.
     *
     * @param embedded True to embed, false otherwise.
     */
    public void setEmbedded(final boolean embedded) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            arguments = new Bundle();
        }

        arguments.putBoolean(ARGUMENT_EMBEDDED, embedded);

        setArguments(arguments);

        updateToolbarVisibility();
    }

    protected void setupStandardToolbar(final View rootView) {
        mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);

        ToolbarHelper.showBackButton(this, mToolbar);
        ToolbarHelper.addSearchView(getActivity(), mToolbar);
        ToolbarHelper.addRefresh(mToolbar);
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
        final Context context = getContext();
        final boolean playlistAvailable = mApp.getMPD().getIdleConnection()
                .isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS);

        if (playlistAvailable && context != null) {
            final Runnable getPlaylistList =
                    new UpdatePlaylistList(PLAYLIST_FILES, context);

            mApp.getAsyncHelper().execAsync(this, UPDATE_PLAYLISTS, getPlaylistList);
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
     * This method is used to report a strange exception that has occurred.
     *
     * @param parent  The parent AdapterView to log.
     * @param adapter The Adapter to log.
     * @return A string detailing the failure.
     */
    protected String trackFailureString(final View parent, final Adapter adapter) {
        return "Failed to add track. parent: " + parent + " adapter: " + adapter + " track: "
                + null;
    }

    /**
     * Update the view from the items list if items is set.
     */
    public void updateFromItems() {
        if (getView() != null) {
            mList.setAdapter(getCustomListAdapter());
            if (forceEmptyView() || mList instanceof ListView
                    && ((ListView) mList).getHeaderViewsCount() == 0) {
                mList.setEmptyView(mNoResultView);
            } else if (mItems.isEmpty()) {
                mNoResultView.setVisibility(View.VISIBLE);
            }

            mLoadingView.setVisibility(View.GONE);
            refreshFastScrollStyle();
        }
    }

    public void updateList() {
        mList.setAdapter(null);
        mNoResultView.setVisibility(View.GONE);
        mLoadingView.setVisibility(View.VISIBLE);
        mApp.getAsyncHelper().execAsync(this, ASYNC_UPDATE_TOKEN, mAsyncUpdate);
    }

    /**
     * This method updates the {@link NowPlayingSmallFragment} cover.
     *
     * @param albumInfo The new album info.
     */
    protected void updateNowPlayingSmallFragment(final AlbumInfo albumInfo) {
        final FragmentActivity activity = getActivity();

        if (activity != null) {
            final NowPlayingSmallFragment fragment = (NowPlayingSmallFragment) activity
                    .getSupportFragmentManager().findFragmentById(R.id.now_playing_small_fragment);
            if (fragment != null) {
                fragment.updateCover(albumInfo);
            }
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

    /**
     * This class updates the playlist from a {@link Runnable} executor.
     */
    private static final class UpdatePlaylistList implements Runnable {

        /**
         * The MPDApplication instance.
         */
        private final MPDApplication mApp;

        /**
         * The Playlist collection to update.
         */
        private final Collection<PlaylistFile> mPlaylistFiles;

        /**
         * Sole constructor.
         *
         * @param playlistFiles The collection of playlist files to update.
         * @param context       The current context.
         */
        private UpdatePlaylistList(final Collection<PlaylistFile> playlistFiles,
                final Context context) {
            super();

            mApp = (MPDApplication) context.getApplicationContext();
            mPlaylistFiles = playlistFiles;
        }

        /**
         * Starts executing the active part of the class' code. This method is
         * called when a thread is started that has been created with a class which
         * implements {@code Runnable}.
         */
        @Override
        public void run() {
            if (mApp != null) {
                try {
                    final PlaylistFileResponse playlistFiles = mApp.getMPD().getPlaylists();

                    final List<PlaylistFile> playlists = new ArrayList<>(playlistFiles);

                    Collections.sort(playlists);
                    mPlaylistFiles.clear();
                    mPlaylistFiles.addAll(playlists);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to parse playlist files.", e);
                }
            }
        }
    }
}
