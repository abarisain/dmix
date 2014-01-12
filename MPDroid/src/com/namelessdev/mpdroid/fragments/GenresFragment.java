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

import android.view.View;
import android.widget.AdapterView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

public class GenresFragment extends BrowseFragment {

    public GenresFragment() {
        super(R.string.addGenre, R.string.genreAdded, MPDCommand.MPD_SEARCH_GENRE);
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.getPlaylist().addAll(
                    app.oMPDAsyncHelper.oMPD.find("genre", item.getName()));
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist,
                    app.oMPDAsyncHelper.oMPD.find("genre", item.getName()));
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            items = app.oMPDAsyncHelper.oMPD.getGenres();
        } catch (MPDServerException e) {
        }
    }

    @Override
    public int getLoadingText() {
        return R.string.loadingGenres;
    }

    @Override
    public String getTitle() {
        return getString(R.string.genres);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                new ArtistsFragment().init((Genre) items.get(position)), "artist");
    }
}
