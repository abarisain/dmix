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
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Genre;
import org.a0z.mpd.item.Item;

import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.io.IOException;

public class GenresFragment extends BrowseFragment {

    private static final String TAG = "GenresFragment";

    public GenresFragment() {
        super(R.string.addGenre, R.string.genreAdded, MPDCommand.MPD_SEARCH_GENRE);
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        try {
            mApp.oMPDAsyncHelper.oMPD.add((Genre) item, replace, play);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add all from playlist.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
        try {
            mApp.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Genre) item);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add all genre to playlist.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            mItems = mApp.oMPDAsyncHelper.oMPD.getGenres();
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update list of genres.", e);
        }
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingGenres;
    }

    @Override
    public String getTitle() {
        return getString(R.string.genres);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                new ArtistsFragment().init((Genre) mItems.get(position)), "artist");
    }
}
