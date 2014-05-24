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
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.PlaylistControl;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.models.AbstractPlaylistMusic;
import com.namelessdev.mpdroid.models.PlaylistSong;
import com.namelessdev.mpdroid.models.PlaylistStream;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.holders.PlayQueueViewHolder;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.text.TextUtils.isEmpty;

public class PlaylistFragment extends ListFragment implements StatusChangeListener,
        OnMenuItemClickListener {

    private class QueueAdapter extends ArrayAdapter {

        private boolean lightTheme;

        public QueueAdapter(Context context, List<?> data, int resource) {
            super(context, resource, data);

            lightTheme = app.isLightThemeSelected();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PlayQueueViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new PlayQueueViewHolder();
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.playlist_queue_item, null);
                viewHolder.artist = (TextView) convertView.findViewById(android.R.id.text2);
                viewHolder.title = (TextView) convertView.findViewById(android.R.id.text1);
                viewHolder.play = (ImageView) convertView.findViewById(R.id.picture);
                viewHolder.cover = (ImageView) convertView.findViewById(R.id.cover);
                viewHolder.coverHelper = new CoverAsyncHelper();
                final int height = viewHolder.cover.getHeight();
                // If the list is not displayed yet, the height is 0. This is a
                // problem, so set a fallback one.
                viewHolder.coverHelper.setCoverMaxSize(height == 0 ? 128 : height);
                final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(
                        viewHolder.cover);
                final AlbumCoverDownloadListener oldAcd
                        = (AlbumCoverDownloadListener) viewHolder.cover
                        .getTag(R.id.AlbumCoverDownloadListener);
                if (oldAcd != null) {
                    oldAcd.detach();
                }
                viewHolder.cover.setTag(R.id.AlbumCoverDownloadListener, acd);
                viewHolder.cover.setTag(R.id.CoverAsyncHelper, viewHolder.coverHelper);
                viewHolder.coverHelper.addCoverDownloadListener(acd);
                viewHolder.menuButton = convertView.findViewById(R.id.menu);
                viewHolder.menuButton.setOnClickListener(itemMenuButtonListener);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (PlayQueueViewHolder) convertView.getTag();
            }

            AbstractPlaylistMusic music = (AbstractPlaylistMusic) getItem(position);

            viewHolder.artist.setText(music.getPlaylistSubLine());
            viewHolder.title.setText(music.getPlayListMainLine());
            viewHolder.menuButton.setTag(music.getSongId());
            viewHolder.play.setImageResource(music.getCurrentSongIconRefID());

            if (music.isForceCoverRefresh() || viewHolder.cover.getTag() == null
                    || !viewHolder.cover.getTag().equals(music.getAlbumInfo().getKey())) {
                if (!music.isForceCoverRefresh()) {
                    viewHolder.cover.setImageResource(lightTheme ? R.drawable.no_cover_art_light
                            : R.drawable.no_cover_art);
                }
                music.setForceCoverRefresh(false);
                viewHolder.coverHelper.downloadCover(music.getAlbumInfo(), false);
            }
            return convertView;
        }
    }

    // Minimum number of songs in the queue before the fastscroll thumb is shown
    private static final int MIN_SONGS_BEFORE_FASTSCROLL = 50;

    private ArrayList<AbstractPlaylistMusic> songlist;

    private final MPDApplication app = MPDApplication.getInstance();

    private DragSortListView list;

    private ActionMode actionMode;

    private SearchView searchView;

    private String filter = null;

    private PopupMenu popupMenu;

    private Integer popupSongID;

    private DragSortController controller;

    private FragmentActivity activity;

    private static final boolean DEBUG = false;

    private int lastPlayingID = -1;

    private boolean lightTheme = false;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        public void drop(int from, int to) {
            if (from != to || filter == null) {
                AbstractPlaylistMusic itemFrom = songlist.get(from);
                final int songID = itemFrom.getSongId();

                PlaylistControl.run(PlaylistControl.MOVE, songID, to);
            }
        }
    };

    private View.OnClickListener itemMenuButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            popupSongID = (Integer) v.getTag();
            popupMenu = new PopupMenu(activity, v);
            popupMenu.getMenuInflater().inflate(R.menu.mpd_playlistcnxmenu, popupMenu.getMenu());
            if (getPlaylistItemSong(popupSongID).isStream()) {
                popupMenu.getMenu().findItem(R.id.PLCX_goto).setVisible(false);
            }
            popupMenu.setOnMenuItemClickListener(PlaylistFragment.this);
            popupMenu.show();
        }
    };

    public PlaylistFragment() {
        super();
        //setHasOptionsMenu(true);
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
    }

    private AbstractPlaylistMusic getPlaylistItemSong(int songID) {
        AbstractPlaylistMusic song = null;
        for (AbstractPlaylistMusic music : songlist) {
            if (music.getSongId() == songID) {
                song = music;
                break;
            }
        }
        return song;
    }

    private boolean isFiltered(final String item) {
        final String processedItem;
        if (item != null) {
            processedItem = item.toLowerCase(Locale.getDefault());
        } else {
            processedItem = "".toLowerCase(Locale.getDefault());
        }

        return processedItem.contains(filter);
    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.activity = getActivity();
        lightTheme = app.isLightThemeSelected();
        refreshListColorCacheHint();
        if (list != null) {
            ((MainMenuActivity) this.activity).onQueueListAttached(list);
        }
    }

    /*
     * Create Menu for Playlist View
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_playlistmenu, menu);
        menu.removeItem(R.id.PLM_EditPL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.playlist_activity, container, false);
        searchView = (SearchView) view.findViewById(R.id.search);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {
                filter = newText;
                if ("".equals(newText)) {
                    filter = null;
                }
                if (filter != null) {
                    filter = filter.toLowerCase();
                }
                list.setDragEnabled(filter == null);
                update(false);
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // Hide the keyboard and give focus to the list
                InputMethodManager imm = (InputMethodManager) activity
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                list.requestFocus();
                return true;
            }
        });
        list = (DragSortListView) view.findViewById(android.R.id.list);
        list.requestFocus();
        list.setDropListener(onDrop);
        controller = new DragSortController(list);
        controller.setDragHandleId(R.id.cover);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setDragInitMode(1);

        list.setFloatViewManager(controller);
        list.setOnTouchListener(controller);
        list.setDragEnabled(true);

        refreshListColorCacheHint();
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                final SparseBooleanArray checkedItems = list.getCheckedItemPositions();
                final int count = list.getCount();
                final ListAdapter adapter = list.getAdapter();
                int j = 0;
                final int positions[];
                boolean result = true;

                switch (item.getItemId()) {
                    case R.id.menu_delete:
                        positions = new int[list.getCheckedItemCount()];
                        for (int i = 0; i < count && j < positions.length; i++) {
                            if (checkedItems.get(i)) {
                                positions[j] = ((AbstractPlaylistMusic) adapter.getItem(i))
                                        .getSongId();
                                j++;
                            }
                        }

                        PlaylistControl.run(PlaylistControl.REMOVE_BY_ID, positions);
                        mode.finish();
                        break;
                    case R.id.menu_crop:
                        positions = new int[list.getCount() - list.getCheckedItemCount()];
                        for (int i = 0; i < count && j < positions.length; i++) {
                            if (!checkedItems.get(i)) {
                                positions[j] = ((AbstractPlaylistMusic) adapter.getItem(i))
                                        .getSongId();
                                j++;
                            }
                        }

                        PlaylistControl.run(PlaylistControl.REMOVE_BY_ID, positions);
                        mode.finish();
                        break;
                    default:
                        result = false;
                        break;
                }
                return result;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                final MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.mpd_queuemenu, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                controller.setSortEnabled(true);
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                    boolean checked) {
                final int selectCount = list.getCheckedItemCount();
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
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                actionMode = mode;
                controller.setSortEnabled(false);
                return false;
            }
        });

        return view;
    }

    @Override
    public void onListItemClick(final ListView l, View v, final int position, long id) {
        final int song = ((AbstractPlaylistMusic) l.getAdapter().getItem(position)).getSongId();

        PlaylistControl.run(PlaylistControl.SKIP_TO_ID, song);
    }

    private static final String TAG = "com.namelessdev.mpdroid.PlaylistFragment";

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final Intent intent;
        final AbstractPlaylistMusic music;

        switch (item.getItemId()) {
            case R.id.PLCX_playNext:
                PlaylistControl.run(PlaylistControl.MOVE_TO_NEXT, popupSongID);
                Tools.notifyUser("Song moved to next in list");
                break;
            case R.id.PLCX_moveFirst:
                // Move song to first in playlist
                PlaylistControl.run(PlaylistControl.MOVE, popupSongID, 0);
                Tools.notifyUser("Song moved to first in list");
                break;
            case R.id.PLCX_moveLast:
                PlaylistControl.run(PlaylistControl.MOVE_TO_LAST, popupSongID);
                Tools.notifyUser("Song moved to last in list");
                break;
            case R.id.PLCX_removeFromPlaylist:
                PlaylistControl.run(PlaylistControl.REMOVE_BY_ID, popupSongID);

                if (isAdded()) {
                    Tools.notifyUser(R.string.deletedSongFromPlaylist);
                }
                break;
            case R.id.PLCX_removeAlbumFromPlaylist:
                if (DEBUG) {
                    Log.d(TAG, "Remove Album " + popupSongID);
                }
                PlaylistControl.run(PlaylistControl.REMOVE_ALBUM_BY_ID, popupSongID);
                if (isAdded()) {
                    Tools.notifyUser(R.string.deletedSongFromPlaylist);
                }
                break;
            case R.id.PLCX_goToArtist:
                music = getPlaylistItemSong(popupSongID);
                if (music == null || isEmpty(music.getArtist())) {
                    break;
                }
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("artist", music.getArtistAsArtist());
                startActivityForResult(intent, -1);
                break;
            case R.id.PLCX_goToAlbum:
                music = getPlaylistItemSong(popupSongID);
                if (music == null || isEmpty(music.getArtist()) || isEmpty(music.getAlbum())) {
                    break;
                }
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("album", music.getAlbumAsAlbum());
                startActivityForResult(intent, -1);
                break;
            case R.id.PLCX_goToFolder:
                music = getPlaylistItemSong(popupSongID);
                if (music == null || isEmpty(music.getFullpath())) {
                    break;
                }
                intent = new Intent(activity, SimpleLibraryActivity.class);
                intent.putExtra("folder", music.getParent());
                startActivityForResult(intent, -1);
                break;
            default:
                break;
        }
        return true;
    }

    private String playlistToSave = "";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu actions...
        boolean result = true;
        Intent i;

        switch (item.getItemId()) {
            case R.id.PLM_Clear:
                Log.e(TAG, "Playlist Clear");
                PlaylistControl.run(PlaylistControl.CLEAR);
                songlist.clear();
                if (isAdded()) {
                    Tools.notifyUser(R.string.playlistCleared);
                }
                ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
                break;
            case R.id.PLM_EditPL:
                i = new Intent(activity, PlaylistEditActivity.class);
                startActivity(i);
                break;
            case R.id.PLM_Save:
                List<Item> plists;
                try {
                    plists = app.oMPDAsyncHelper.oMPD.getPlaylists();
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to receive list of playlists.", e);
                    plists = new ArrayList<>(0);
                }
                Collections.sort(plists);
                final String[] playlistsArray = new String[plists.size() + 1];
                for (int p = 0; p < plists.size(); p++) {
                    playlistsArray[p] = plists.get(p).getName(); // old playlists
                }
                playlistsArray[playlistsArray.length - 1] = getResources()
                        .getString(R.string.newPlaylist); // "new playlist"
                playlistToSave = playlistsArray[playlistsArray.length - 1];
                new AlertDialog.Builder(activity) // dialog with list of playlists
                        .setTitle(R.string.playlistName)
                        .setSingleChoiceItems
                                (playlistsArray, playlistsArray.length - 1,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                playlistToSave = playlistsArray[which];
                                            }
                                        }
                                )
                        .setPositiveButton
                                (android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                                savePlaylist(playlistToSave);
                                            }
                                        }
                                )
                        .setNegativeButton
                                (android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                                // Do nothing.
                                            }
                                        }
                                )
                        .create().show();
                break;
            default:
                result = false;
                break;
        }
        return result;
    }

    protected void savePlaylist(final String name) {
        if (name.equals(getResources().getString(R.string.newPlaylist))) {
            // if "new playlist", show dialog with EditText for new playlist:
            final EditText input = new EditText(activity);
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.newPlaylistPrompt)
                    .setView(input)
                    .setPositiveButton
                            (android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                            final String name = input.getText().toString().trim();
                                            if (null != name && name.length() > 0 && !name
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
                                        public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                            // Do nothing.
                                        }
                                    }
                            )
                    .create().show();
        } else if (!name.isEmpty()) {
            // actually save:
            PlaylistControl.run(PlaylistControl.SAVE_PLAYLIST, name);
        }
    }

    @Override
    public void onPause() {
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        new Thread(new Runnable() {
            public void run() {
                update();
            }
        }).start();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
        update();

    }

    @Override
    public void randomChanged(boolean random) {
    }

    private void refreshListColorCacheHint() {
        if (list != null) {
            if (app.isLightThemeSelected()) {
                list.setCacheColorHint(getResources().getColor(android.R.color.background_light));
            } else {
                list.setCacheColorHint(getResources().getColor(R.color.nowplaying_background));
            }
        }
    }

    private void refreshPlaylistItemView(final AbstractPlaylistMusic... playlistSongs) {
        final int start = list.getFirstVisiblePosition();

        for (int i = start; i <= list.getLastVisiblePosition(); i++) {
            final AbstractPlaylistMusic playlistMusic =
                    (AbstractPlaylistMusic) list.getAdapter().getItem(i);
            for (final AbstractPlaylistMusic song : playlistSongs) {
                if (playlistMusic.getSongId() == song.getSongId()) {
                    final View view = list.getChildAt(i - start);
                    list.getAdapter().getView(i, view, list);
                }
            }
        }
    }

    @Override
    public void repeatChanged(boolean repeating) {
    }

    public void scrollToNowPlaying() {

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                Integer songIndex = Integer.valueOf(-1);
                try {
                    songIndex = app.oMPDAsyncHelper.oMPD.getStatus().getSongPos();
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Cannot find the current playing song position.", e);
                }
                return songIndex;
            }

            @Override
            protected void onPostExecute(Integer songIndex) {
                if (songIndex != null) {

                    if (activity instanceof MainMenuActivity) {
                        ((MainMenuActivity) activity).showQueue();
                    }

                    getListView().requestFocusFromTouch();
                    getListView().setSelection(songIndex);
                    getListView().clearFocus();
                } else {
                    Log.d(PlaylistFragment.class.getSimpleName(), "Missing list item : "
                            + songIndex);
                }
            }
        }.execute();
    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) {
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        if (songlist != null) {
            // Mark running track...
            for (AbstractPlaylistMusic song : songlist) {
                int newPlay;
                if ((song.getSongId()) == mpdStatus.getSongId()) {
                    newPlay = lightTheme ? R.drawable.ic_media_play_light
                            : R.drawable.ic_media_play;
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

    public void update() {
        update(true);
    }

    /**
     * Update the current playlist fragment.
     *
     * @param forcePlayingIDRefresh Force the current track to refresh.
     */
    protected void update(final boolean forcePlayingIDRefresh) {
        // Save the scroll bar position to restore it after update
        final MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
        final List<Music> musics = playlist.getMusicList();
        final ArrayList<AbstractPlaylistMusic> newSongList = new ArrayList<>(musics.size());

        if (lastPlayingID == -1 || forcePlayingIDRefresh) {
            try {
                lastPlayingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
            } catch (final MPDServerException e) {
                Log.e(TAG, "Failed to get the current song id.", e);
            }
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

            if (filter != null) {
                if (isFiltered(item.getAlbumArtist()) || isFiltered(item.getAlbum()) ||
                        isFiltered(item.getTitle())) {
                    continue;
                }
            }

            if (item.getSongId() == lastPlayingID) {
                if (lightTheme) {
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

    /**
     * Updates the scrollbar.
     *
     * @param newSongList   The updated list of songs for the playlist.
     * @param listPlayingID The current playing playlist id.
     */
    private void updateScrollbar(final ArrayList newSongList, final int listPlayingID) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int firstVisibleElementIndex = list.getFirstVisiblePosition();
                final View firstVisibleItem = list.getChildAt(0);
                final int firstVisiblePosition;
                final ArrayAdapter songs = new QueueAdapter(activity, newSongList,
                        R.layout.playlist_queue_item);

                if (firstVisibleItem != null) {
                    firstVisiblePosition = firstVisibleItem.getTop();
                } else {
                    firstVisiblePosition = 0;
                }

                setListAdapter(songs);
                songlist = newSongList;
                songs.notifyDataSetChanged();

                /**
                 * Note : Setting the scrollbar style before setting the fast scroll state is very
                 * important pre-KitKat, because of a bug. It is also very important post-KitKat
                 * because it needs the opposite order or it won't show the FastScroll.
                 *
                 * This is so stupid I don't even .... argh.
                 */
                if (newSongList.size() >= MIN_SONGS_BEFORE_FASTSCROLL) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        // No need to enable FastScroll, this setter enables
                        // it.
                        list.setFastScrollAlwaysVisible(true);
                        list.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                    } else {
                        list.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                        list.setFastScrollAlwaysVisible(true);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        list.setFastScrollAlwaysVisible(false);
                        // Default Android value
                        list.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                    } else {
                        list.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                        list.setFastScrollAlwaysVisible(false);
                    }
                }

                if (actionMode != null) {
                    actionMode.finish();
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
                    list.setSelectionFromTop(firstVisibleElementIndex, firstVisiblePosition);
                }
            }
        });
    }

    public void updateCover(AlbumInfo albumInfo) {

        List<AbstractPlaylistMusic> musicsToBeUpdated = new ArrayList<AbstractPlaylistMusic>();

        for (AbstractPlaylistMusic playlistMusic : songlist) {
            if (playlistMusic.getAlbumInfo().equals(albumInfo)) {
                playlistMusic.setForceCoverRefresh(true);
                musicsToBeUpdated.add(playlistMusic);
            }
        }
        refreshPlaylistItemView(musicsToBeUpdated
                .toArray(new AbstractPlaylistMusic[musicsToBeUpdated.size()]));
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }
}
