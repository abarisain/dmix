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
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MPDroidActivities.MPDroidListActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
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

import static android.util.Log.d;
import static android.util.Log.e;

public class PlaylistEditActivity extends MPDroidListActivity implements StatusChangeListener,
        OnClickListener {
    private ArrayList<HashMap<String, Object>> songlist = new ArrayList<HashMap<String, Object>>();
    private String playlistName = null;
    private boolean isPlayQueue = true;
    private boolean isFirstRefresh = true;

    private DragSortListView.DropListener mDropListener = new DragSortListView.DropListener() {
        public void drop(int from, int to) {
            if (from == to) {
                return;
            }
            HashMap<String, Object> itemFrom = songlist.get(from);
            Integer songID = (Integer) itemFrom.get("songid");
            MPDApplication app = (MPDApplication) getApplication();
            try {
                // looks like it's not necessary
                /*
                 * if (from < to) {
                 * app.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to - 1);
                 * } else {
                 */
                if (isPlayQueue) {
                    app.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to);
                } else {
                    app.oMPDAsyncHelper.oMPD.movePlaylistSong(playlistName, from, to);
                    update();
                }
                // }
            } catch (MPDServerException e) {
            }
            Tools.notifyUser("Updating ...", getApplication());
        }
    };

    private DragSortListView.RemoveListener mRemoveListener = new DragSortListView.RemoveListener() {
        public void remove(int which) {
            // removePlaylistItem(which);
        }
    };

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        // TODO Auto-generated method stub

    }

    @Override
    public void libraryStateChanged(boolean updating) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Remove:
                MPDApplication app = (MPDApplication) getApplicationContext();
                int count = 0;
                try {
                    ArrayList<HashMap<String, Object>> copy = new ArrayList<HashMap<String, Object>>();
                    copy.addAll(songlist);

                    List<Integer> positions = new LinkedList<Integer>();
                    for (HashMap<String, Object> item : copy) {
                        if (item.get("marked").equals(true)) {
                            positions.add((Integer) item.get("songid"));
                            songlist.remove(copy.indexOf(item) - count);
                            count++;
                        }
                    }
                    Collections.sort(positions);

                    if (isPlayQueue) {
                        for (count = 0; count < positions.size(); ++count) {
                            app.oMPDAsyncHelper.oMPD.getPlaylist().removeById(positions.get(count));
                        }
                    } else {
                        for (count = 0; count < positions.size(); ++count) {
                            app.oMPDAsyncHelper.oMPD.removeFromPlaylist(playlistName,
                                    positions.get(count) - count);
                        }
                    }
                    if (copy.size() != songlist.size()) {
                        ((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
                    }
                    Tools.notifyUser(String.format(
                            getResources().getString(R.string.removeCountSongs), count), this);
                } catch (Exception e) {
                    e("MPDroid", "General: " + e.toString());
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
        playlistName = getIntent().getStringExtra("playlist");
        if (null != playlistName && playlistName.length() > 0) {
            isPlayQueue = false;
        }
        MPDApplication app = (MPDApplication) getApplication();
        setContentView(R.layout.playlist_editlist_activity);
        if (isPlayQueue) {
            this.setTitle(R.string.nowPlaying);
        } else {
            this.setTitle(playlistName);
        }
        update();
        app.oMPDAsyncHelper.addStatusChangeListener(this);

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
        MPDApplication app = (MPDApplication) getApplication();
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        super.onDestroy();
    }
    /**
     * Marks the selected item for deletion
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        HashMap<String, Object> item = songlist.get(position);
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
        MPDApplication app = (MPDApplication) getApplicationContext();
        app.setActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MPDApplication app = (MPDApplication) getApplicationContext();
        app.unsetActivity(this);
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
    public void stateChanged(MPDStatus mpdStatus, String oldState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        if (isPlayQueue) {
            // Mark running track...
            for (HashMap<String, Object> song : songlist) {
                if (((Integer) song.get("songid")).intValue() == mpdStatus.getSongId())
                    song.put("play", android.R.drawable.ic_media_play);
                else
                    song.put("play", 0);
            }
            final SimpleAdapter adapter = (SimpleAdapter) getListAdapter();
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }

    protected void update() {
        // TODO: Preserve position!!!
        MPDApplication app = (MPDApplication) getApplicationContext();
        try {
            List<Music> musics;
            if (isPlayQueue) {
                MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
                musics = playlist.getMusicList();
            } else {
                musics = app.oMPDAsyncHelper.oMPD.getPlaylistSongs(playlistName);
            }
            songlist = new ArrayList<HashMap<String, Object>>();
            int playingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
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
                if (isPlayQueue && m.getSongId() == playingID) {
                    item.put("play", android.R.drawable.ic_media_play);
                    listPlayingId = songlist.size() - 1;
                } else {
                    item.put("play", 0);
                }
                songlist.add(item);
            }
            SimpleAdapter songs = new SimpleAdapter(this, songlist,
                    R.layout.playlist_editlist_item, new String[] {
                            "play", "title", "artist",
                            "marked"
                    }, new int[] {
                            R.id.picture, android.R.id.text1, android.R.id.text2, R.id.removeCBox
                    });

            setListAdapter(songs);
            if (isFirstRefresh) {
                isFirstRefresh = false;
                if (listPlayingId > 0)
                    setSelection(listPlayingId);
            } else {
                if (-1 != pos && -1 != top) {
                    getListView().setSelectionFromTop(pos, top);
                }
            }

        } catch (MPDServerException e) {
            d(PlaylistEditActivity.class.getSimpleName(), "Playlist update failure : " + e);

        }

    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        // TODO Auto-generated method stub

    }

}
