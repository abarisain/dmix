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
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.StoredPlaylistDataBinder;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class StoredPlaylistFragment extends BrowseFragment {

    private static final String EXTRA_PLAYLIST_NAME = "playlist";

    private static final String TAG = "StoredPlaylistFragment";

    private static MPDApplication app = MPDApplication.getInstance();

    private String playlistName;

    public StoredPlaylistFragment() {
        super(R.string.addSong, R.string.songAdded, MPDCommand.MPD_SEARCH_TITLE);
        setHasOptionsMenu(true);
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        Music music = (Music) item;
        try {
            app.oMPDAsyncHelper.oMPD.add(music, replace, play);
            if (!play) {
                Tools.notifyUser(R.string.songAdded, music.getTitle(), music.getName());
            }
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Music) item);
            Tools.notifyUser(irAdded, item);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    public void asyncUpdate() {
        try {
            if (getActivity() == null) {
                return;
            }
            items = app.oMPDAsyncHelper.oMPD.getPlaylistSongs(playlistName);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (items != null) {
            return new ArrayAdapter(getActivity(),
                    new StoredPlaylistDataBinder(app.isLightThemeSelected()), items);
        }
        return super.getCustomListAdapter();
    }

    @Override
    public int getLoadingText() {
        return R.string.loadingSongs;
    }

    @Override
    public String getTitle() {
        return playlistName;
    }

    public StoredPlaylistFragment init(String name) {
        playlistName = name;
        return this;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            init(icicle.getString(EXTRA_PLAYLIST_NAME));
        }
    }

    /*
     * Create Menu for Playlist View
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_storedplaylistmenu, menu);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, View v, final int position, long id) {
        app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                add((Item) adapterView.getAdapter().getItem(position), app.isInSimpleMode(),
                        app.isInSimpleMode());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu actions...
        Intent i;
        switch (item.getItemId()) {
            case R.id.PLM_EditPL:
                i = new Intent(getActivity(), PlaylistEditActivity.class);
                i.putExtra("playlist", playlistName);
                startActivity(i);
                return true;
            default:
                return false;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(EXTRA_PLAYLIST_NAME, playlistName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public String toString() {
        if (playlistName != null) {
            return playlistName;
        } else {
            return getString(R.string.playlist);
        }
    }
}
