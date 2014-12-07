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

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.AbstractAlbum;
import org.a0z.mpd.item.AbstractMusic;
import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public abstract class BrowseFragment extends Fragment implements OnMenuItemClickListener,
        AsyncExecListener, OnItemClickListener,
        OnRefreshListener {

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

    private static final int MIN_ITEMS_BEFORE_FASTSCROLL = 50;

    private static final String TAG = "BrowseFragment";

    protected final MPDApplication mApp = MPDApplication.getInstance();

    final String mContext;

    final int mIrAdd;

    final int mIrAdded;

    protected List<? extends Item> mItems = null;

    protected int mJobID = -1;

    protected AbsListView mList;

    protected TextView mLoadingTextView;

    protected View mLoadingView;

    protected View mNoResultView;

    protected PullToRefreshLayout mPullToRefreshLayout;

    private boolean mFirstUpdateDone = false;

    protected BrowseFragment(@StringRes final int rAdd, @StringRes final int rAdded,
            final String pContext) {
        super();
        mIrAdd = rAdd;
        mIrAdded = rAdded;

        mContext = pContext;

        setHasOptionsMenu(false);
    }

    protected abstract void add(Item item, boolean replace, boolean play);

    protected abstract void add(Item item, String playlist);

    private void addAndReplace(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        mApp.oMPDAsyncHelper.execAsync(new Runnable() {
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
                        final MPDStatus status = mApp.oMPDAsyncHelper.oMPD.getStatus();

                        /**
                         * Let the user know if we're not going to play the added music.
                         */
                        if (status.isRandom() && status.isState(MPDStatus.STATE_PLAYING)) {
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
                                        mApp.oMPDAsyncHelper.execAsync(new Runnable() {
                                            @Override
                                            public void run() {
                                                add(mItems.get(id), name);
                                            }
                                        });
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
            add(mItems.get(id), item.getTitle().toString());
        }
    }

    @Override
    public void asyncExecSucceeded(final int jobID) {
        if (mJobID == jobID) {
            updateFromItems();
        }

    }

    protected void asyncUpdate() {

    }

    // Override if you want setEmptyView to be called on the list even if you have a header
    protected boolean forceEmptyView() {
        return false;
    }

    protected ListAdapter getCustomListAdapter() {
        return new ArrayIndexerAdapter(getActivity(), R.layout.simple_list_item_1, mItems);
    }

    /*
     * Override this to display a custom loading text
     */
    @StringRes
    public int getLoadingText() {
        return R.string.loading;
    }

    /**
     * Should return the minimum number of songs in the queue before the
     * fastscroll thumb is shown
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
                    mApp.oMPDAsyncHelper.oMPD
                            .isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS)) {

                int id = 0;
                final SubMenu playlistMenu = menu.addSubMenu(R.string.addToPlaylist);
                MenuItem item = playlistMenu.add(ADD_TO_PLAYLIST, id++, (int) info.id,
                        R.string.newPlaylist);
                item.setOnMenuItemClickListener(this);

                try {
                    final List<Item> playlists = mApp.oMPDAsyncHelper.oMPD.getPlaylists();

                    if (null != playlists) {
                        for (final Item pl : playlists) {
                            item = playlistMenu.add(ADD_TO_PLAYLIST, id++, (int) info.id,
                                    pl.getName());
                            item.setOnMenuItemClickListener(this);
                        }
                    }
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to parse playlists.", e);
                }
            }
            final MenuItem gotoArtistItem = menu
                    .add(GOTO_ARTIST, GOTO_ARTIST, 0, R.string.goToArtist);
            gotoArtistItem.setOnMenuItemClickListener(this);

        }
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
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.pullToRefresh);

        return view;
    }

    @Override
    public void onDestroy() {
        try {
            mApp.oMPDAsyncHelper.removeAsyncExecListener(this);
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
                    artist = ((AbstractAlbum) selectedItem).getArtist();
                } else if (selectedItem instanceof Artist) {
                    artist = (Artist) selectedItem;
                } else if (selectedItem instanceof Music) {
                    artist = new Artist(((AbstractMusic) selectedItem).getAlbumArtistOrArtist());
                }

                if (artist != null) {
                    intent.putExtra("artist", artist);
                    startActivityForResult(intent, -1);
                }
                break;
            default:
                final String name = item.getTitle().toString();
                final int id = item.getOrder();
                mApp.oMPDAsyncHelper.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        add(mItems.get(id), name);
                    }
                });
                break;
        }
        return false;
    }

    @Override
    public void onRefreshStarted(final View view) {
        mPullToRefreshLayout.setRefreshComplete();
        updateList();
    }

    @Override
    public void onStart() {
        super.onStart();
        mApp.setActivity(getActivity());
        if (!mFirstUpdateDone) {
            mFirstUpdateDone = true;
            updateList();
        }
    }

    @Override
    public void onStop() {
        mApp.unsetActivity(getActivity());
        super.onStop();
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mItems != null) {
            mList.setAdapter(getCustomListAdapter());
        }
        refreshFastScrollStyle();
        if (mPullToRefreshLayout != null) {
            ActionBarPullToRefresh.from(getActivity())
                    .allChildrenArePullable()
                    .listener(this)
                    .setup(mPullToRefreshLayout);
        }
    }

    /**
     * This method is used for the fastcroll visibility decision.<br/>
     * Don't override this if you want to change the fastscroll style, override
     * {@link #refreshFastScrollStyle(boolean)} instead.
     */
    protected void refreshFastScrollStyle() {
        refreshFastScrollStyle(mItems != null
                && mItems.size() >= getMinimumItemsCountBeforeFastscroll());
    }

    /**
     * This is required because setting the fast scroll prior to KitKat was
     * important because of a bug. This bug has since been corrected, but the
     * opposite order is now required or the fast scroll will not show.
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

    public void scrollToTop() {
        try {
            mList.setSelection(-1);
        } catch (final Exception e) {
            // What if the list is empty or some other bug ? I don't want any
            // crashes because of that
        }
    }

    public void setActivityTitle(final CharSequence title) {
        getActivity().setTitle(title);
    }

    /**
     * Update the view from the items list if items is set.
     */
    public void updateFromItems() {
        if (getView() == null) {
            // The view has been destroyed, bail.
            return;
        }
        if (mPullToRefreshLayout != null) {
            mPullToRefreshLayout.setEnabled(true);
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
        mList.setAdapter(null);
        mNoResultView.setVisibility(View.GONE);
        mLoadingView.setVisibility(View.VISIBLE);
        if (mPullToRefreshLayout != null) {
            mPullToRefreshLayout.setEnabled(false);
        }

        // Loading Artists asynchronous...
        mApp.oMPDAsyncHelper.addAsyncExecListener(this);
        mJobID = mApp.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                asyncUpdate();
            }
        });
    }
}
