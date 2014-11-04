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

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverDownloadListener;
import com.namelessdev.mpdroid.helpers.QueueControl;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.models.AbstractPlaylistMusic;
import com.namelessdev.mpdroid.models.PlaylistSong;
import com.namelessdev.mpdroid.models.PlaylistStream;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.holders.PlayQueueViewHolder;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.text.TextUtils.isEmpty;

/**
 * A fragment for showing the media player's current playlist queue.
 */
public class QueueFragment extends ListFragment implements StatusChangeListener,
        OnMenuItemClickListener {

    protected static final boolean DEBUG = false;

    // Minimum number of songs in the queue before the fastscroll thumb is shown
    protected static final int MIN_SONGS_BEFORE_FASTSCROLL = 50;

    private static final String TAG = "QueueFragment";

    protected final MPDApplication mApp = MPDApplication.getInstance();

    protected final boolean mLightTheme = mApp.isLightThemeSelected();

    protected ActionMode mActionMode;

    protected FragmentActivity mActivity;

    protected DragSortController mController;

    protected String mFilter = null;

    protected final DragSortListView.DropListener mDropListener
            = new DragSortListView.DropListener() {
        @Override
        public void drop(final int from, final int to) {
            if (from != to && mFilter == null) {
                final AbstractPlaylistMusic itemFrom = mSongList.get(from);
                final int songID = itemFrom.getSongId();

                QueueControl.run(QueueControl.MOVE, songID, to);
            }
        }
    };

    protected int mLastPlayingID = -1;

    protected DragSortListView mList;

    protected String mPlaylistToSave = "";

    protected Integer mPopupSongID;

    protected final View.OnClickListener mItemMenuButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            mPopupSongID = (Integer) v.getTag();
            final PopupMenu popupMenu = new PopupMenu(mActivity, v);
            popupMenu.getMenuInflater().inflate(R.menu.mpd_playlistcnxmenu, popupMenu.getMenu());
            if (getPlaylistItemSong(mPopupSongID).isStream()) {
                popupMenu.getMenu().findItem(R.id.PLCX_goto).setVisible(false);
            }
            popupMenu.setOnMenuItemClickListener(QueueFragment.this);
            popupMenu.show();
        }
    };

    protected ViewGroup mRootView;

    protected SearchView mSearchView;

    protected ArrayList<AbstractPlaylistMusic> mSongList;

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
    }

    protected AbstractPlaylistMusic getPlaylistItemSong(final int songID) {
        AbstractPlaylistMusic song = null;
        for (final AbstractPlaylistMusic music : mSongList) {
            if (music.getSongId() == songID) {
                song = music;
                break;
            }
        }
        return song;
    }

    protected boolean isFiltered(final String item) {
        final String processedItem;

        if (item == null) {
            processedItem = "";
        } else {
            processedItem = item.toLowerCase(Locale.getDefault());
        }

        return processedItem.contains(mFilter);
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
        refreshListColorCacheHint();
    }

    /*
     * Create Menu for Playlist View
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_playlistmenu, menu);
        menu.removeItem(R.id.PLM_EditPL);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mRootView = container;
        final View view = inflater.inflate(R.layout.playlist_activity, container, false);
        mSearchView = (SearchView) view.findViewById(R.id.search);
        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(final String newText) {
                mFilter = newText;
                if (newText != null && newText.isEmpty()) {
                    mFilter = null;
                }
                if (mFilter != null) {
                    mFilter = mFilter.toLowerCase();
                }
                mList.setDragEnabled(mFilter == null);
                update(false);
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(final String query) {
                // Hide the keyboard and give focus to the list
                final InputMethodManager imm = (InputMethodManager) mActivity
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
                mList.requestFocus();
                return true;
            }
        });
        mList = (DragSortListView) view.findViewById(android.R.id.list);
        mList.requestFocus();
        mList.setDropListener(mDropListener);
        mController = new DragSortController(mList);
        mController.setDragHandleId(R.id.cover);
        mController.setRemoveEnabled(false);
        mController.setSortEnabled(true);
        mController.setDragInitMode(1);

        mList.setFloatViewManager(mController);
        mList.setOnTouchListener(mController);
        mList.setDragEnabled(true);

        refreshListColorCacheHint();
        mList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mList.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {

                final SparseBooleanArray checkedItems = mList.getCheckedItemPositions();
                final int count = mList.getCount();
                final ListAdapter adapter = mList.getAdapter();
                final int itemId = item.getItemId();
                int j = 0;
                int[] positions = null;
                boolean result = true;

                if (itemId == R.id.menu_delete) {
                    positions = new int[mList.getCheckedItemCount()];
                    for (int i = 0; i < count && j < positions.length; i++) {
                        if (checkedItems.get(i)) {
                            positions[j] = ((Music) adapter.getItem(i)).getSongId();
                            j++;
                        }
                    }
                } else if (itemId == R.id.menu_crop) {
                    positions = new int[mList.getCount() - mList.getCheckedItemCount()];
                    for (int i = 0; i < count && j < positions.length; i++) {
                        if (!checkedItems.get(i)) {
                            positions[j] = ((Music) adapter.getItem(i)).getSongId();
                            j++;
                        }
                    }
                } else {
                    result = false;
                }

                if (j > 0) {
                    QueueControl.run(QueueControl.REMOVE_BY_ID, positions);
                    mode.finish();
                }

                return result;
            }

            @Override
            public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
                final MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.mpd_queuemenu, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(final ActionMode mode) {
                mActionMode = null;
                mController.setSortEnabled(true);
            }

            @Override
            public void onItemCheckedStateChanged(
                    final ActionMode mode, final int position, final long id,
                    final boolean checked) {
                final int selectCount = mList.getCheckedItemCount();
                if (selectCount == 0) {
                    mode.finish();
                }
                if (selectCount == 1) {
                    mode.setTitle(R.string.actionSongSelected);
                } else {
                    mode.setTitle(getString(R.string.actionSongsSelected, selectCount));
                }
            }

            @Override
            public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
                mActionMode = mode;
                mController.setSortEnabled(false);
                return false;
            }
        });

        return view;
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        super.onListItemClick(l, v, position, id);

        final int song = ((Music) l.getAdapter().getItem(position)).getSongId();

        QueueControl.run(QueueControl.SKIP_TO_ID, song);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final Intent intent;
        final AbstractPlaylistMusic music;

        switch (item.getItemId()) {
            case R.id.PLCX_playNext:
                QueueControl.run(QueueControl.MOVE_TO_NEXT, mPopupSongID);
                Tools.notifyUser("Song moved to next in list");
                break;
            case R.id.PLCX_moveFirst:
                // Move song to first in playlist
                QueueControl.run(QueueControl.MOVE, mPopupSongID, 0);
                Tools.notifyUser("Song moved to first in list");
                break;
            case R.id.PLCX_moveLast:
                QueueControl.run(QueueControl.MOVE_TO_LAST, mPopupSongID);
                Tools.notifyUser("Song moved to last in list");
                break;
            case R.id.PLCX_removeFromPlaylist:
                QueueControl.run(QueueControl.REMOVE_BY_ID, mPopupSongID);

                if (isAdded()) {
                    Tools.notifyUser(R.string.deletedSongFromPlaylist);
                }
                break;
            case R.id.PLCX_removeAlbumFromPlaylist:
                if (DEBUG) {
                    Log.d(TAG, "Remove Album " + mPopupSongID);
                }
                QueueControl.run(QueueControl.REMOVE_ALBUM_BY_ID, mPopupSongID);
                if (isAdded()) {
                    Tools.notifyUser(R.string.deletedSongFromPlaylist);
                }
                break;
            case R.id.PLCX_goToArtist:
                music = getPlaylistItemSong(mPopupSongID);
                if (music == null || isEmpty(music.getArtist())) {
                    break;
                }

                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("artist", music.getArtistAsArtist());
                startActivityForResult(intent, -1);
                break;
            case R.id.PLCX_goToAlbum:
                music = getPlaylistItemSong(mPopupSongID);

                if (music == null || isEmpty(music.getArtist()) || isEmpty(music.getAlbum())) {
                    break;
                }

                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("album", music.getAlbumAsAlbum());
                startActivityForResult(intent, -1);
                break;
            case R.id.PLCX_goToFolder:
                music = getPlaylistItemSong(mPopupSongID);
                if (music == null || isEmpty(music.getFullPath())) {
                    break;
                }
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("folder", music.getParent());
                startActivityForResult(intent, -1);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);

        // Menu actions...
        boolean result = true;

        if (item.getItemId() == R.id.PLM_Clear) {
            QueueControl.run(QueueControl.CLEAR);
            mSongList.clear();
            if (isAdded()) {
                Tools.notifyUser(R.string.playlistCleared);
            }
            ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
        } else if (item.getItemId() == R.id.PLM_Save) {
            List<Item> playLists;
            try {
                playLists = mApp.oMPDAsyncHelper.oMPD.getPlaylists();
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to receive list of playlists.", e);
                playLists = new ArrayList<>(0);
            }
            Collections.sort(playLists);
            final String[] playlistsArray = new String[playLists.size() + 1];
            for (int p = 0; p < playLists.size(); p++) {
                playlistsArray[p] = playLists.get(p).getName(); // old playlists
            }
            playlistsArray[playlistsArray.length - 1] = getResources()
                    .getString(R.string.newPlaylist); // "new playlist"
            mPlaylistToSave = playlistsArray[playlistsArray.length - 1];
            new AlertDialog.Builder(mActivity) // dialog with list of playlists
                    .setTitle(R.string.playlistName)
                    .setSingleChoiceItems
                            (playlistsArray, playlistsArray.length - 1,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                final DialogInterface dialog, final int which) {
                                            mPlaylistToSave = playlistsArray[which];
                                        }
                                    }
                            )
                    .setPositiveButton
                            (android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(final DialogInterface dialog,
                                                final int which) {
                                            savePlaylist(mPlaylistToSave);
                                        }
                                    }
                            )
                    .setNegativeButton
                            (android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(final DialogInterface dialog,
                                                final int which) {
                                            // Do nothing.
                                        }
                                    }
                            )
                    .create().show();
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public void onPause() {
        mApp.oMPDAsyncHelper.removeStatusChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mApp.oMPDAsyncHelper.addStatusChangeListener(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                update();
            }
        }).start();
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        update();

    }

    @Override
    public void randomChanged(final boolean random) {
    }

    protected void refreshListColorCacheHint() {
        if (mList != null) {
            if (mLightTheme) {
                mList.setCacheColorHint(getResources().getColor(android.R.color.background_light));
            } else {
                mList.setCacheColorHint(getResources().getColor(R.color.nowplaying_background));
            }
        }
    }

    protected void refreshPlaylistItemView(final AbstractPlaylistMusic... playlistSongs) {
        final int start = mList.getFirstVisiblePosition();

        for (int i = start; i <= mList.getLastVisiblePosition(); i++) {
            final AbstractPlaylistMusic playlistMusic =
                    (AbstractPlaylistMusic) mList.getAdapter().getItem(i);
            for (final AbstractPlaylistMusic song : playlistSongs) {
                if (playlistMusic.getSongId() == song.getSongId()) {
                    final View view = mList.getChildAt(i - start);
                    mList.getAdapter().getView(i, view, mList);
                }
            }
        }
    }

    @Override
    public void repeatChanged(final boolean repeating) {
    }

    void savePlaylist(final String name) {
        if (name.equals(getResources().getString(R.string.newPlaylist))) {
            // if "new playlist", show dialog with EditText for new playlist:
            final EditText input = new EditText(mActivity);
            new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.newPlaylistPrompt)
                    .setView(input)
                    .setPositiveButton
                            (android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(final DialogInterface dialog,
                                                final int which) {
                                            final String name = input.getText().toString().trim();
                                            if (!name.isEmpty() && !name
                                                    .equals(MPD.STREAMS_PLAYLIST)) {
                                                // TODO: Need to warn user if they attempt to save to MPD.STREAMS_PLAYLIST
                                                savePlaylist(name);
                                            }
                                        }
                                    }
                            )
                    .setNegativeButton
                            (android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(final DialogInterface dialog,
                                                final int which) {
                                            // Do nothing.
                                        }
                                    }
                            )
                    .create().show();
        } else if (!name.isEmpty()) {
            // actually save:
            QueueControl.run(QueueControl.SAVE_PLAYLIST, name);
        }
    }

    public void scrollToNowPlaying() {
        final int songPos = mApp.oMPDAsyncHelper.oMPD.getStatus().getSongPos();

        if (songPos == -1) {
            Log.d(TAG, "Missing list item.");
        } else {

            if (mActivity instanceof MainMenuActivity) {
                ((MainMenuActivity) mActivity).showQueue();
            }

            final ListView listView = getListView();
            listView.requestFocusFromTouch();
            listView.setSelection(songPos);
            listView.clearFocus();
        }
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        if (mSongList != null) {
            // Mark running track...
            for (final AbstractPlaylistMusic song : mSongList) {
                final int newPlay;
                if (song.getSongId() == mpdStatus.getSongId()) {
                    if (mLightTheme) {
                        newPlay = R.drawable.ic_media_play_light;
                    } else {
                        newPlay = R.drawable.ic_media_play;
                    }
                } else {
                    newPlay = 0;
                }
                if (song.getCurrentSongIconRefID() != newPlay) {
                    song.setCurrentSongIconRefID(newPlay);
                    refreshPlaylistItemView(song);
                }
            }
        }
    }

    void update() {
        update(true);
    }

    /**
     * Update the current playlist fragment.
     *
     * @param forcePlayingIDRefresh Force the current track to refresh.
     */
    void update(final boolean forcePlayingIDRefresh) {
        // Save the scroll bar position to restore it after update
        final MPDPlaylist playlist = mApp.oMPDAsyncHelper.oMPD.getPlaylist();
        final List<Music> musics = playlist.getMusicList();
        final ArrayList<AbstractPlaylistMusic> newSongList = new ArrayList<>(musics.size());

        if (mLastPlayingID == -1 || forcePlayingIDRefresh) {
            mLastPlayingID = mApp.oMPDAsyncHelper.oMPD.getStatus().getSongId();
        }

        // The position in the song list of the currently played song
        int listPlayingID = -1;

        // Copy list to avoid concurrent exception
        for (final Music music : new ArrayList<>(musics)) {
            if (music == null) {
                continue;
            }

            final AbstractPlaylistMusic item;
            if (music.isStream()) {
                item = new PlaylistStream(music);
            } else {
                item = new PlaylistSong(music);
            }

            if (mFilter != null) {
                if (!(isFiltered(item.getAlbumArtist()) || isFiltered(item.getAlbum()) ||
                        isFiltered(item.getTitle()))) {
                    continue;
                }
            }

            if (item.getSongId() == mLastPlayingID) {
                if (mLightTheme) {
                    item.setCurrentSongIconRefID(R.drawable.ic_media_play_light);
                } else {
                    item.setCurrentSongIconRefID(R.drawable.ic_media_play);
                }

                /**
                 * Lie a little. Scroll to the previous song than the one playing.
                 * That way it shows that there are other songs before it.
                 */
                listPlayingID = newSongList.size() - 1;
            } else {
                item.setCurrentSongIconRefID(0);
            }
            newSongList.add(item);
        }

        updateScrollbar(newSongList, listPlayingID);
    }

    public void updateCover(final AlbumInfo albumInfo) {

        final List<AbstractPlaylistMusic> musicsToBeUpdated = new ArrayList<>(mSongList.size());

        for (final AbstractPlaylistMusic playlistMusic : mSongList) {
            final AlbumInfo abstractAlbumInfo = new AlbumInfo(playlistMusic);

            if (abstractAlbumInfo.equals(albumInfo)) {
                playlistMusic.setForceCoverRefresh(true);
                musicsToBeUpdated.add(playlistMusic);
            }
        }
        refreshPlaylistItemView(musicsToBeUpdated
                .toArray(new AbstractPlaylistMusic[musicsToBeUpdated.size()]));
    }

    /**
     * Updates the scrollbar.
     *
     * @param newSongList   The updated list of songs for the playlist.
     * @param listPlayingID The current playing playlist id.
     */
    protected void updateScrollbar(final ArrayList newSongList, final int listPlayingID) {
        mActivity.runOnUiThread(new Runnable() {
            /**
             * This is a helper method to workaround shortcomings of the fast scroll API.
             *
             * @param scrollbarStyle The {@code View} scrollbar style.
             * @param isAlwaysVisible The visibility of the scrollbar.
             */
            private void refreshFastScrollStyle(final int scrollbarStyle,
                    final boolean isAlwaysVisible) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mList.setFastScrollAlwaysVisible(isAlwaysVisible);
                    mList.setScrollBarStyle(scrollbarStyle);
                } else {
                    mList.setScrollBarStyle(scrollbarStyle);
                    mList.setFastScrollAlwaysVisible(isAlwaysVisible);
                }
            }

            @Override
            public void run() {
                final int firstVisibleElementIndex = mList.getFirstVisiblePosition();
                final View firstVisibleItem = mList.getChildAt(0);
                final int firstVisiblePosition;
                final ArrayAdapter songs = new QueueAdapter(mActivity, R.layout.playlist_queue_item,
                        newSongList
                );

                if (firstVisibleItem != null) {
                    firstVisiblePosition = firstVisibleItem.getTop();
                } else {
                    firstVisiblePosition = 0;
                }

                setListAdapter(songs);
                mSongList = newSongList;
                songs.notifyDataSetChanged();

                /**
                 * Note : Setting the scrollbar style before setting the fast scroll state is very
                 * important pre-KitKat, because of a bug. It is also very important post-KitKat
                 * because it needs the opposite order or it won't show the FastScroll.
                 *
                 * This is so stupid I don't even .... argh.
                 */
                if (newSongList.size() >= MIN_SONGS_BEFORE_FASTSCROLL) {
                    refreshFastScrollStyle(View.SCROLLBARS_INSIDE_INSET, true);
                } else {
                    refreshFastScrollStyle(View.SCROLLBARS_INSIDE_OVERLAY, false);
                }

                if (mActionMode != null) {
                    mActionMode.finish();
                }

                // Restore the scroll bar position
                if (firstVisibleElementIndex == 0 || firstVisiblePosition == 0) {
                    /**
                     * Only scroll if there is a valid song to scroll to. 0 is a valid song but
                     * does not require scroll anyway. Also, only scroll if it's the first update.
                     * You don't want your playlist to scroll itself while you are looking at other
                     * stuff.
                     */
                    if (listPlayingID > 0 && getView() != null) {
                        setSelection(listPlayingID);
                    }
                } else {
                    mList.setSelectionFromTop(firstVisibleElementIndex, firstVisiblePosition);
                }
            }
        });
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
    }

    private class QueueAdapter extends ArrayAdapter<AbstractPlaylistMusic> {

        QueueAdapter(final Context context, @LayoutRes final int resource,
                final List<AbstractPlaylistMusic> data) {
            super(context, resource, data);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final PlayQueueViewHolder viewHolder;
            final View view;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(
                        R.layout.playlist_queue_item, mRootView);
                viewHolder = new PlayQueueViewHolder();
                viewHolder.mArtist = (TextView) view.findViewById(android.R.id.text2);
                viewHolder.mTitle = (TextView) view.findViewById(android.R.id.text1);
                viewHolder.mPlay = (ImageView) view.findViewById(R.id.picture);
                viewHolder.mAlbumCover = (ImageView) view.findViewById(R.id.cover);
                viewHolder.mCoverHelper = new CoverAsyncHelper();
                int height = viewHolder.mAlbumCover.getHeight();
                // If the list is not displayed yet, the height is 0.
                // This is a problem, so set a fallback one.
                final int fallbackHeight = 128;
                if (height == 0) {
                    height = fallbackHeight;
                }
                viewHolder.mCoverHelper.setCoverMaxSize(height);
                final CoverDownloadListener acd = new AlbumCoverDownloadListener(
                        viewHolder.mAlbumCover);
                final AlbumCoverDownloadListener oldAcd
                        = (AlbumCoverDownloadListener) viewHolder.mAlbumCover
                        .getTag(R.id.AlbumCoverDownloadListener);
                if (oldAcd != null) {
                    oldAcd.detach();
                }
                viewHolder.mAlbumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
                viewHolder.mAlbumCover.setTag(R.id.CoverAsyncHelper, viewHolder.mCoverHelper);
                viewHolder.mCoverHelper.addCoverDownloadListener(acd);
                viewHolder.mMenuButton = view.findViewById(R.id.menu);
                viewHolder.mMenuButton.setOnClickListener(mItemMenuButtonListener);
                view.setTag(viewHolder);
            } else {
                viewHolder = (PlayQueueViewHolder) convertView.getTag();
                view = convertView;
            }

            final AbstractPlaylistMusic music = getItem(position);

            viewHolder.mArtist.setText(music.getPlaylistSubLine());
            viewHolder.mTitle.setText(music.getPlayListMainLine());
            viewHolder.mMenuButton.setTag(music.getSongId());
            viewHolder.mPlay.setImageResource(music.getCurrentSongIconRefID());

            final AlbumInfo albumInfo = new AlbumInfo(music);

            if (music.isForceCoverRefresh() || viewHolder.mAlbumCover.getTag() == null
                    || !viewHolder.mAlbumCover.getTag().equals(albumInfo.getKey())) {
                if (!music.isForceCoverRefresh()) {
                    final int noCoverResource = AlbumCoverDownloadListener.getNoCoverResource();
                    viewHolder.mAlbumCover.setImageResource(noCoverResource);
                }
                music.setForceCoverRefresh(false);
                viewHolder.mCoverHelper.downloadCover(albumInfo, false);
            }
            return view;
        }
    }
}
