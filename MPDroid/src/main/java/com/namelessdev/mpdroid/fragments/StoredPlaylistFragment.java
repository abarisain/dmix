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

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.StoredPlaylistDataBinder;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import java.io.IOException;

public class StoredPlaylistFragment extends BrowseFragment {

    private static final String EXTRA_PLAYLIST_NAME = "playlist";

    private static final String TAG = "StoredPlaylistFragment";

    private String mPlaylistName;

    public StoredPlaylistFragment() {
        super(R.string.addSong, R.string.songAdded, MPDCommand.MPD_SEARCH_TITLE);
        setHasOptionsMenu(true);
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        final Music music = (Music) item;
        try {
            mApp.oMPDAsyncHelper.oMPD.add(music, replace, play);
            if (!play) {
                Tools.notifyUser(R.string.songAdded, music.getTitle(), music.getName());
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
        try {
            mApp.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Music) item);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    public void asyncUpdate() {
        try {
            if (getActivity() == null) {
                return;
            }
            mItems = mApp.oMPDAsyncHelper.oMPD.getPlaylistSongs(mPlaylistName);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (mItems != null) {
            return new ArrayAdapter(getActivity(), new StoredPlaylistDataBinder(), mItems);
        }
        return super.getCustomListAdapter();
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingSongs;
    }

    @Override
    public String getTitle() {
        return mPlaylistName;
    }

    public StoredPlaylistFragment init(final String name) {
        mPlaylistName = name;
        return this;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            init(savedInstanceState.getString(EXTRA_PLAYLIST_NAME));
        }
    }

    /*
     * Create Menu for Playlist View
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_storedplaylistmenu, menu);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        mApp.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                add((Item) parent.getAdapter().getItem(position), mApp.isInSimpleMode(),
                        mApp.isInSimpleMode());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Menu actions...
        final Intent intent;
        switch (item.getItemId()) {
            case R.id.PLM_EditPL:
                intent = new Intent(getActivity(), PlaylistEditActivity.class);
                intent.putExtra("playlist", mPlaylistName);
                startActivity(intent);
                return true;
            default:
                return false;
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        updateList();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putString(EXTRA_PLAYLIST_NAME, mPlaylistName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public String toString() {
        if (mPlaylistName != null) {
            return mPlaylistName;
        } else {
            return getString(R.string.playlist);
        }
    }
}
