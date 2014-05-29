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
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
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

import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public abstract class BrowseFragment extends Fragment implements OnMenuItemClickListener,
        AsyncExecListener, OnItemClickListener,
        OnRefreshListener {

    private static final int MIN_ITEMS_BEFORE_FASTSCROLL = 50;

    private static final String TAG = "BrowseFragment";

    protected int iJobID = -1;

    public static final int MAIN = 0;
    public static final int PLAYLIST = 3;

    public static final int ADD = 0;
    public static final int ADDNREPLACE = 1;
    public static final int ADDNREPLACEPLAY = 4;
    public static final int ADDNPLAY = 2;
    public static final int ADD_TO_PLAYLIST = 3;

    protected List<? extends Item> items = null;

    protected MPDApplication app = MPDApplication.getInstance();
    protected View loadingView;
    protected TextView loadingTextView;
    protected View noResultView;
    protected AbsListView list;
    protected PullToRefreshLayout pullToRefreshLayout;
    private boolean firstUpdateDone = false;

    String context;
    int irAdd, irAdded;

    public BrowseFragment(int rAdd, int rAdded, String pContext) {
        super();
        irAdd = rAdd;
        irAdded = rAdded;

        context = pContext;

        setHasOptionsMenu(false);
    }

    protected abstract void add(Item item, boolean replace, boolean play);

    protected abstract void add(Item item, String playlist);

    @Override
    public void asyncExecSucceeded(int jobID) {
        if (iJobID == jobID) {
            updateFromItems();
        }

    }

    protected void asyncUpdate() {

    }

    // Override if you want setEmptyView to be called on the list even if you
    // have a header
    protected boolean forceEmptyView() {
        return false;
    }

    protected ListAdapter getCustomListAdapter() {
        return new ArrayIndexerAdapter(getActivity(), R.layout.simple_list_item_1, items);
    }

    /*
     * Override this to display a custom loading text
     */
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            Activity activity = this.getActivity();
            ActionBar actionBar = activity.getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        } catch (NoClassDefFoundError e) {
            // Older android
        } catch (NullPointerException e) {

        } catch (NoSuchMethodError e) {

        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        int index = (int) info.id;
        if (index >= 0 && items.size() > index) {
            menu.setHeaderTitle(items.get((int) info.id).toString());
            // If in simple mode, show "Play" (add, replace & play), "Add to queue" and "Add to playlist"
            if (app.isInSimpleMode()) {
                android.view.MenuItem playItem = menu.add(ADDNREPLACEPLAY,
                        ADDNREPLACEPLAY, 0, R.string.play);
                playItem.setOnMenuItemClickListener(this);
                android.view.MenuItem addItem = menu.add(ADD, ADD, 0, R.string.addToQueue);
                addItem.setOnMenuItemClickListener(this);
            } else {
                android.view.MenuItem addItem = menu.add(ADD, ADD, 0, irAdd);
                addItem.setOnMenuItemClickListener(this);
                android.view.MenuItem addAndReplaceItem = menu.add(ADDNREPLACE, ADDNREPLACE, 0,
                        R.string.addAndReplace);
                addAndReplaceItem.setOnMenuItemClickListener(this);
                android.view.MenuItem addAndReplacePlayItem = menu.add(ADDNREPLACEPLAY,
                        ADDNREPLACEPLAY, 0, R.string.addAndReplacePlay);
                addAndReplacePlayItem.setOnMenuItemClickListener(this);
                android.view.MenuItem addAndPlayItem = menu.add(ADDNPLAY, ADDNPLAY, 0,
                        R.string.addAndPlay);
                addAndPlayItem.setOnMenuItemClickListener(this);
            }

            if (R.string.addPlaylist != irAdd && R.string.addStream != irAdd) {
                int id = 0;
                SubMenu playlistMenu = menu.addSubMenu(R.string.addToPlaylist);
                android.view.MenuItem item = playlistMenu.add(ADD_TO_PLAYLIST, id++, (int) info.id,
                        R.string.newPlaylist);
                item.setOnMenuItemClickListener(this);

                try {
                    List<Item> playlists = app.oMPDAsyncHelper.oMPD.getPlaylists();

                    if (null != playlists) {
                        for (Item pl : playlists) {
                            item = playlistMenu.add(ADD_TO_PLAYLIST, id++, (int) info.id,
                                    pl.getName());
                            item.setOnMenuItemClickListener(this);
                        }
                    }
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browse, container, false);
        list = (ListView) view.findViewById(R.id.list);
        registerForContextMenu(list);
        list.setOnItemClickListener(this);
        loadingView = view.findViewById(R.id.loadingLayout);
        loadingTextView = (TextView) view.findViewById(R.id.loadingText);
        noResultView = view.findViewById(R.id.noResultLayout);
        loadingTextView.setText(getLoadingText());
        pullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.pullToRefresh);

        return view;
    }

    @Override
    public void onDestroy() {
        try {
            app.oMPDAsyncHelper.removeAsyncExecListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error while destroying BrowseFragment", e);
        }
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        // help out the GC; imitated from ListFragment source
        loadingView = null;
        loadingTextView = null;
        noResultView = null;
        super.onDestroyView();
    }

    @Override
    public boolean onMenuItemClick(final android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getGroupId()) {
            case ADDNREPLACEPLAY:
            case ADDNREPLACE:
            case ADD:
            case ADDNPLAY:
                app.oMPDAsyncHelper.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        boolean replace = false;
                        boolean play = false;
                        switch (item.getGroupId()) {
                            case ADDNREPLACEPLAY:
                                replace = true;
                                play = true;
                                break;
                            case ADDNREPLACE:
                                replace = true;
                                break;
                            case ADDNPLAY:
                                MPDStatus status = null;
                                try {
                                    status = app.oMPDAsyncHelper.oMPD.getStatus();
                                } catch (final MPDServerException e) {
                                    Log.e(TAG, "Failed to get random state status.", e);
                                }

                                /**
                                 * Let the user know if we're not going to play the added music.
                                 */
                                if(status != null && status.isRandom() &&
                                        MPDStatus.MPD_STATE_PLAYING.equals(status.getState())) {
                                    Tools.notifyUser(R.string.notPlayingInRandomMode);
                                } else {
                                    play = true;
                                }
                                break;
                        }
                        add(items.get((int) info.id), replace, play);
                    }
                });
                break;
            case ADD_TO_PLAYLIST: {
                final EditText input = new EditText(getActivity());
                final int id = item.getOrder();
                if (item.getItemId() == 0) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.playlistName)
                            .setMessage(R.string.newPlaylistPrompt)
                            .setView(input)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            final String name = input.getText().toString().trim();
                                            if (null != name && name.length() > 0) {
                                                app.oMPDAsyncHelper.execAsync(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        add(items.get(id), name);
                                                    }
                                                });
                                            }
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            // Do nothing.
                                        }
                                    }).show();
                } else {
                    add(items.get(id), item.getTitle().toString());
                }
                break;
            }
            default:
                final String name = item.getTitle().toString();
                final int id = item.getOrder();
                app.oMPDAsyncHelper.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        add(items.get(id), name);
                    }
                });
                break;
        }
        return false;
    }

    @Override
    public void onRefreshStarted(View view) {
        pullToRefreshLayout.setRefreshComplete();
        UpdateList();
    }

    @Override
    public void onStart() {
        super.onStart();
        app.setActivity(getActivity());
        if (!firstUpdateDone) {
            firstUpdateDone = true;
            UpdateList();
        }
    }

    @Override
    public void onStop() {
        app.unsetActivity(getActivity());
        super.onStop();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (items != null) {
            list.setAdapter(getCustomListAdapter());
        }
        refreshFastScrollStyle();
        if (pullToRefreshLayout != null) {
            ActionBarPullToRefresh.from(getActivity())
                    .allChildrenArePullable()
                    .listener(this)
                    .setup(pullToRefreshLayout);
        }
    }

    /**
     * This method is used for the fastcroll visibility decision.<br/>
     * Don't override this if you want to change the fastscroll style, override
     * {@link #refreshFastScrollStyle(boolean)} instead.
     */
    protected void refreshFastScrollStyle() {
        refreshFastScrollStyle(items != null
                && items.size() >= getMinimumItemsCountBeforeFastscroll());
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
     * @param scrollbarStyle The {@code View} scrollbar style.
     * @param isAlwaysVisible The visibility of the scrollbar.
     */
    final void refreshFastScrollStyle(final int scrollbarStyle, final boolean isAlwaysVisible) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            list.setFastScrollAlwaysVisible(isAlwaysVisible);
            list.setScrollBarStyle(scrollbarStyle);
        } else {
            list.setScrollBarStyle(scrollbarStyle);
            list.setFastScrollAlwaysVisible(isAlwaysVisible);
        }
    }

    public void scrollToTop() {
        try {
            list.setSelection(-1);
        } catch (Exception e) {
            // What if the list is empty or some other bug ? I don't want any
            // crashes because of that
        }
    }

    public void setActivityTitle(String title) {
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
        if (pullToRefreshLayout != null) {
            pullToRefreshLayout.setEnabled(true);
        }
        if (items != null) {
            list.setAdapter(getCustomListAdapter());
        }
        try {
            if (forceEmptyView()
                    || ((list instanceof ListView) && ((ListView) list).getHeaderViewsCount() == 0)) {
                list.setEmptyView(noResultView);
            } else {
                if (items == null || items.isEmpty()) {
                    noResultView.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
        }
        loadingView.setVisibility(View.GONE);
        refreshFastScrollStyle();
    }

    public void UpdateList() {
        list.setAdapter(null);
        noResultView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        if (pullToRefreshLayout != null) {
            pullToRefreshLayout.setEnabled(false);
        }

        // Loading Artists asynchronous...
        app.oMPDAsyncHelper.addAsyncExecListener(this);
        iJobID = app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                asyncUpdate();
            }
        });
    }
}
