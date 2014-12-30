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

package com.namelessdev.mpdroid.library;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.namelessdev.mpdroid.MPDroidActivities;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Music;
import org.a0z.mpd.item.PlaylistFile;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class PlaylistEditActivity extends MPDroidActivities.MPDroidActivity
        implements StatusChangeListener,
        OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = "PlaylistEditActivity";

    private boolean mIsFirstRefresh = true;

    private boolean mIsPlayQueue = true;

    private ListView mListView;

    private PlaylistFile mPlaylist;

    private final DragSortListView.DropListener mDropListener
            = new DragSortListView.DropListener() {
        @Override
        public void drop(final int from, final int to) {
            if (from == to) {
                return;
            }
            final AbstractMap<String, Object> itemFrom = mSongList.get(from);
            final Integer songID = (Integer) itemFrom.get(Music.RESPONSE_SONG_ID);
            if (mIsPlayQueue) {
                try {
                    mApp.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to move a track on the queue.", e);
                }
            } else {
                try {
                    mApp.oMPDAsyncHelper.oMPD.movePlaylistSong(mPlaylist, from, to);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to rename a playlist.", e);
                }
                update();
            }
            Tools.notifyUser("Updating ...");
        }
    };

    private ArrayList<HashMap<String, Object>> mSongList = new ArrayList<>();

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
    }

    private List<Music> getMusic() {
        List<Music> musics;

        if (mIsPlayQueue) {
            final MPDPlaylist playlist = mApp.oMPDAsyncHelper.oMPD.getPlaylist();
            musics = playlist.getMusicList();
        } else {
            try {
                musics = mApp.oMPDAsyncHelper.oMPD.getPlaylistSongs(mPlaylist);
            } catch (final IOException | MPDException e) {
                Log.d(TAG, "Playlist update failure.", e);
                musics = Collections.emptyList();
            }
        }

        return musics;
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.Remove) {
            int count = 0;

            final Collection<HashMap<String, Object>> copy = new ArrayList<>(mSongList);

            final List<Integer> positions = new LinkedList<>();
            for (final AbstractMap<String, Object> item : copy) {
                if (item.get("marked").equals(Boolean.TRUE)) {
                    positions.add((Integer) item.get(Music.RESPONSE_SONG_ID));
                    count++;
                }
            }

            try {
                if (mIsPlayQueue) {
                    mApp.oMPDAsyncHelper.oMPD.getPlaylist().removeById(positions);
                } else {
                    mApp.oMPDAsyncHelper.oMPD.removeFromPlaylist(mPlaylist, positions);
                }
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to remove.", e);
            }

            if (copy.size() != mSongList.size()) {
                ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
            }
            Tools.notifyUser(R.string.removeCountSongs, count);
            update();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlaylist = getIntent().getParcelableExtra(PlaylistFile.EXTRA);
        if (null != mPlaylist) {
            mIsPlayQueue = false;
        }
        setContentView(R.layout.playlist_editlist_activity);

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);

        if (mIsPlayQueue) {
            setTitle(R.string.nowPlaying);
        } else {
            setTitle(mPlaylist.getName());
        }
        update();
        mApp.oMPDAsyncHelper.addStatusChangeListener(this);

        final DragSortListView trackList = (DragSortListView) mListView;
        trackList.setOnCreateContextMenuListener(this);
        trackList.setDropListener(mDropListener);

        final DragSortController controller = new DragSortController(trackList);
        controller.setDragHandleId(R.id.icon);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setDragInitMode(1);

        trackList.setFloatViewManager(controller);
        trackList.setOnTouchListener(controller);
        trackList.setDragEnabled(true);
        trackList.setCacheColorHint(0);

        final Button button = (Button) findViewById(R.id.Remove);
        button.setOnClickListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDestroy() {
        mApp.oMPDAsyncHelper.removeStatusChangeListener(this);
        super.onDestroy();
    }

    /**
     * Marks the selected item for deletion
     */
    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i,
            final long l) {
        final AbstractMap<String, Object> item = mSongList.get(i);
        item.get("marked");
        if (item.get("marked").equals(true)) {
            item.put("marked", false);
        } else {
            item.put("marked", true);
        }

        ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Menu actions...
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mApp.setActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mApp.unsetActivity(this);
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        update();

    }

    @Override
    public void randomChanged(final boolean random) {
    }

    @Override
    public void repeatChanged(final boolean repeating) {
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        if (mIsPlayQueue) {
            // Mark running track...
            for (final AbstractMap<String, Object> song : mSongList) {
                final int songId = ((Integer) song.get(Music.RESPONSE_SONG_ID)).intValue();

                if (songId == mpdStatus.getSongId()) {
                    song.put("play", android.R.drawable.ic_media_play);
                } else {
                    song.put("play", 0);
                }
            }
            final SimpleAdapter adapter = (SimpleAdapter) mListView.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected void update() {
        // TODO: Preserve position!!!
        mSongList = new ArrayList<>();
        final String[] columnNames = {"play", Music.TAG_TITLE, Music.TAG_ARTIST, "marked"};
        final int playingID = mApp.oMPDAsyncHelper.oMPD.getStatus().getSongId();
        final int pos = null == mListView ? -1 : mListView.getFirstVisiblePosition();
        final View view = null == mListView ? null : mListView.getChildAt(0);
        final int top = null == view ? -1 : view.getTop();
        final int[] columnNamesViews =
                {R.id.picture, android.R.id.text1, android.R.id.text2, R.id.removeCBox};
        int listPlayingId = 0;
        int playlistPosition = 0;

        // Copy list to avoid concurrent exception
        for (final Music music : new ArrayList<>(getMusic())) {
            final HashMap<String, Object> item = new HashMap<>(5);

            if (mIsPlayQueue) {
                item.put(Music.RESPONSE_SONG_ID, music.getSongId());
            } else {
                item.put(Music.RESPONSE_SONG_ID, playlistPosition);
            }

            playlistPosition++;
            item.put(Music.TAG_ARTIST, music.getArtist());
            item.put(Music.TAG_TITLE, music.getTitle());
            item.put("marked", false);
            if (mIsPlayQueue && music.getSongId() == playingID) {
                item.put("play", android.R.drawable.ic_media_play);
                listPlayingId = mSongList.size() - 1;
            } else {
                item.put("play", 0);
            }

            mSongList.add(item);

            final ListAdapter songs = new SimpleAdapter(this, mSongList,
                    R.layout.playlist_editlist_item, columnNames, columnNamesViews);

            if (mListView != null) {
                mListView.setAdapter(songs);
                if (mIsFirstRefresh) {
                    mIsFirstRefresh = false;
                    if (listPlayingId > 0) {
                        mListView.setSelection(listPlayingId);
                    }
                } else {
                    if (-1 != pos && -1 != top) {
                        mListView.setSelectionFromTop(pos, top);
                    }
                }
            }
        }
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
    }

}
