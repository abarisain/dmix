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
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Genre;
import org.a0z.mpd.item.Item;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

public class ArtistsFragment extends BrowseFragment {

    private static final String TAG = "ArtistsFragment";

    private Genre genre = null;

    public ArtistsFragment() {
        super(R.string.addArtist, R.string.artistAdded, MPDCommand.MPD_SEARCH_ARTIST);
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add((Artist) item, replace, play);
            if (isAdded()) {
                Tools.notifyUser(irAdded, item);
            }
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add to queue.", e);
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Artist) item);
            if (isAdded()) {
                Tools.notifyUser(irAdded, item);
            }
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add to playlist.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            final SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(MPDApplication.getInstance());
            switch (settings.getString(LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE,
                    LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_BOTH).toLowerCase()) {
                case LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_BOTH:
                default:
                    if (genre != null) {
                        items = app.oMPDAsyncHelper.oMPD.getArtists(genre);
                    } else {
                        items = app.oMPDAsyncHelper.oMPD.getArtists();
                    }
                    break;
                case LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST:
                    if (genre != null) {
                        items = app.oMPDAsyncHelper.oMPD.getArtists(genre, true);
                    } else {
                        items = app.oMPDAsyncHelper.oMPD.getArtists(true);
                    }
                    break;
                case LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_ARTIST:
                    if (genre != null) {
                        items = app.oMPDAsyncHelper.oMPD.getArtists(genre, false);
                    } else {
                        items = app.oMPDAsyncHelper.oMPD.getArtists(false);
                    }
                    break;
            }
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    public int getLoadingText() {
        return R.string.loadingArtists;
    }

    @Override
    public String getTitle() {
        if (genre != null) {
            return genre.getName();
        } else {
            return getString(R.string.genres);
        }
    }

    public ArtistsFragment init(Genre g) {
        genre = g;
        return this;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        AlbumsFragment af;
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(app);
        if (settings.getBoolean(LibraryFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
            af = new AlbumsGridFragment((Artist) items.get(position), genre);
        } else {
            af = new AlbumsFragment((Artist) items.get(position), genre);
        }
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(af, "album");
    }
}
