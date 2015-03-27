/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.StoredPlaylistDataBinder;

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

public class StoredPlaylistFragment extends BrowseFragment<Music> {

    private static final String TAG = "StoredPlaylistFragment";

    private PlaylistFile mPlaylist;

    public StoredPlaylistFragment() {
        super(R.string.addSong, R.string.songAdded, Music.TAG_TITLE);
        setHasOptionsMenu(true);
    }

    @Override
    protected void add(final Music item, final boolean replace, final boolean play) {
        try {
            mApp.getMPD().add(item, replace, play);
            if (!play) {
                Tools.notifyUser(R.string.songAdded, item.getTitle(), item.getName());
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Music item, final PlaylistFile playlist) {
        try {
            mApp.getMPD().addToPlaylist(playlist, item);
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
            replaceItems(mApp.getMPD().getPlaylistSongs(mPlaylist));
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    protected Artist getArtist(final Music item) {
        return new Artist(item.getAlbumArtistOrArtist());
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        return new ArrayAdapter<>(getActivity(), new StoredPlaylistDataBinder<Music>(), mItems);
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingSongs;
    }

    @Override
    public String getTitle() {
        return mPlaylist.getName();
    }

    public StoredPlaylistFragment init(final PlaylistFile playlist) {
        mPlaylist = playlist;
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
            init((PlaylistFile) savedInstanceState.getParcelable(PlaylistFile.EXTRA));
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
        final Runnable runnable = addAdapterItem(parent, position);

        if (runnable != null) {
            mApp.getAsyncHelper().execAsync(runnable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Menu actions...
        final Intent intent;
        switch (item.getItemId()) {
            case R.id.PLM_EditPL:
                intent = new Intent(getActivity(), PlaylistEditActivity.class);
                intent.putExtra(PlaylistFile.EXTRA, mPlaylist);
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
        outState.putParcelable(PlaylistFile.EXTRA, mPlaylist);
        super.onSaveInstanceState(outState);
    }

    @Override
    public String toString() {
        if (mPlaylist != null) {
            return mPlaylist.getName();
        } else {
            return getString(R.string.playlist);
        }
    }
}
