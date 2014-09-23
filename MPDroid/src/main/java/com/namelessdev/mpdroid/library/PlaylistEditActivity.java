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
import com.namelessdev.mpdroid.MPDroidActivities.MPDroidListActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Music;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class PlaylistEditActivity extends MPDroidListActivity implements StatusChangeListener,
        OnClickListener {

    private static final String TAG = "PlaylistEditActivity";

    private boolean mIsFirstRefresh = true;

    private boolean mIsPlayQueue = true;

    private String mPlaylistName = null;

    private DragSortListView.DropListener mDropListener = new DragSortListView.DropListener() {
        public void drop(int from, int to) {
            if (from == to) {
                return;
            }
            HashMap<String, Object> itemFrom = mSongList.get(from);
            Integer songID = (Integer) itemFrom.get("songid");
            if (mIsPlayQueue) {
                try {
                    mApp.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to);
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to move a track on the queue.", e);
                }
            } else {
                try {
                    mApp.oMPDAsyncHelper.oMPD.movePlaylistSong(mPlaylistName, from, to);
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to rename a playlist.");
                }
                update();
            }
            Tools.notifyUser("Updating ...");
        }
    };

    private DragSortListView.RemoveListener mRemoveListener
            = new DragSortListView.RemoveListener() {
        public void remove(int which) {
            // removePlaylistItem(which);
        }
    };

    private ArrayList<HashMap<String, Object>> mSongList = new ArrayList<HashMap<String, Object>>();

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        // TODO Auto-generated method stub

    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Remove:
                int count = 0;
                try {
                    ArrayList<HashMap<String, Object>> copy
                            = new ArrayList<HashMap<String, Object>>();
                    copy.addAll(mSongList);

                    List<Integer> positions = new LinkedList<Integer>();
                    for (HashMap<String, Object> item : copy) {
                        if (item.get("marked").equals(true)) {
                            positions.add((Integer) item.get("songid"));
                            mSongList.remove(copy.indexOf(item) - count);
                            count++;
                        }
                    }
                    Collections.sort(positions);

                    if (mIsPlayQueue) {
                        for (count = 0; count < positions.size(); ++count) {
                            mApp.oMPDAsyncHelper.oMPD.getPlaylist()
                                    .removeById(positions.get(count));
                        }
                    } else {
                        for (count = 0; count < positions.size(); ++count) {
                            mApp.oMPDAsyncHelper.oMPD.removeFromPlaylist(mPlaylistName,
                                    positions.get(count) - count);
                        }
                    }
                    if (copy.size() != mSongList.size()) {
                        ((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
                    }
                    Tools.notifyUser(R.string.removeCountSongs, count);
                } catch (final Exception e) {
                    Log.e(TAG, "General Error.", e);
                    update();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPlaylistName = getIntent().getStringExtra("playlist");
        if (null != mPlaylistName && mPlaylistName.length() > 0) {
            mIsPlayQueue = false;
        }
        setContentView(R.layout.playlist_editlist_activity);
        if (mIsPlayQueue) {
            this.setTitle(R.string.nowPlaying);
        } else {
            this.setTitle(mPlaylistName);
        }
        update();
        mApp.oMPDAsyncHelper.addStatusChangeListener(this);

        final DragSortListView trackList = (DragSortListView) getListView();
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

        Button button = (Button) findViewById(R.id.Remove);
        button.setOnClickListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        HashMap<String, Object> item = mSongList.get(position);
        item.get("marked");
        if (item.get("marked").equals(true)) {
            item.put("marked", false);
        } else {
            item.put("marked", true);
        }

        ((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
        update();

    }

    @Override
    public void randomChanged(boolean random) {
        // TODO Auto-generated method stub

    }

    @Override
    public void repeatChanged(boolean repeating) {
        // TODO Auto-generated method stub

    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, int oldState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        if (mIsPlayQueue) {
            // Mark running track...
            for (HashMap<String, Object> song : mSongList) {
                if (((Integer) song.get("songid")).intValue() == mpdStatus.getSongId()) {
                    song.put("play", android.R.drawable.ic_media_play);
                } else {
                    song.put("play", 0);
                }
            }
            final SimpleAdapter adapter = (SimpleAdapter) getListAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected void update() {
        // TODO: Preserve position!!!
        try {
            List<Music> musics;
            if (mIsPlayQueue) {
                MPDPlaylist playlist = mApp.oMPDAsyncHelper.oMPD.getPlaylist();
                musics = playlist.getMusicList();
            } else {
                musics = mApp.oMPDAsyncHelper.oMPD.getPlaylistSongs(mPlaylistName);
            }
            mSongList = new ArrayList<HashMap<String, Object>>();
            int playingID = mApp.oMPDAsyncHelper.oMPD.getStatus().getSongId();
            int pos = null == getListView() ? -1 : getListView().getFirstVisiblePosition();
            View view = null == getListView() ? null : getListView().getChildAt(0);
            int top = null == view ? -1 : view.getTop();
            int listPlayingId = 0;
            // Copy list to avoid concurrent exception
            for (Music m : new ArrayList<Music>(musics)) {
                HashMap<String, Object> item = new HashMap<String, Object>();
                item.put("songid", m.getSongId());
                item.put("artist", m.getArtist());
                item.put("title", m.getTitle());
                item.put("marked", false);
                if (mIsPlayQueue && m.getSongId() == playingID) {
                    item.put("play", android.R.drawable.ic_media_play);
                    listPlayingId = mSongList.size() - 1;
                } else {
                    item.put("play", 0);
                }
                mSongList.add(item);
            }
            SimpleAdapter songs = new SimpleAdapter(this, mSongList,
                    R.layout.playlist_editlist_item, new String[]{
                    "play", "title", "artist",
                    "marked"
            }, new int[]{
                    R.id.picture, android.R.id.text1, android.R.id.text2, R.id.removeCBox
            });

            setListAdapter(songs);
            if (mIsFirstRefresh) {
                mIsFirstRefresh = false;
                if (listPlayingId > 0) {
                    setSelection(listPlayingId);
                }
            } else {
                if (-1 != pos && -1 != top) {
                    getListView().setSelectionFromTop(pos, top);
                }
            }

        } catch (MPDServerException e) {
            Log.d(TAG, "Playlist update failure.", e);

        }

    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        // TODO Auto-generated method stub

    }

}
