/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

public class ArtistsFragment extends BrowseFragment {
    private Genre genre = null;

    public ArtistsFragment() {
        super(R.string.addArtist, R.string.artistAdded, MPDCommand.MPD_SEARCH_ARTIST);
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add((Artist) item, replace, play);
            if (isAdded()) {
                Tools.notifyUser(String.format(getResources().getString(irAdded), item),
                        getActivity());
            }
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Artist) item);
            if (isAdded()) {
                Tools.notifyUser(String.format(getResources().getString(irAdded), item),
                        getActivity());
            }
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            if (genre != null) {
                items = app.oMPDAsyncHelper.oMPD.getArtists(genre);
            } else {
                items = app.oMPDAsyncHelper.oMPD.getArtists();
            }
        } catch (MPDServerException e) {
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
                .getDefaultSharedPreferences(getActivity().getApplication());
        if (settings.getBoolean(LibraryFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
            af = new AlbumsGridFragment((Artist) items.get(position), genre);
        } else {
            af = new AlbumsFragment((Artist) items.get(position), genre);
        }
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(af, "album");
    }
}
