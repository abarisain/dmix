/*
 * Copyright (C) 2010-2016 The MPDroid Project
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
import com.anpmech.mpd.item.Genre;
import com.anpmech.mpd.item.PlaylistFile;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.io.IOException;
import java.util.Collections;

public class GenresFragment extends BrowseFragment<Genre> {

    private static final String TAG = "GenresFragment";

    public GenresFragment() {
        super(R.string.addGenre, R.string.genreAdded);
    }

    @Override
    protected void add(final Genre item, final boolean replace, final boolean play) {
        try {
            mApp.getMPD().add(item, replace, play);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add all from playlist.", e);
        }
    }

    @Override
    protected void add(final Genre item, final PlaylistFile playlist) {
        try {
            mApp.getMPD().addToPlaylist(playlist, item);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add all genre to playlist.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            replaceItems(mApp.getMPD().getGenreResponse().getList());
            Collections.sort(mItems);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update list of genres.", e);
        }
    }

    @Override
    protected Artist getArtist(final Genre item) {
        return null;
    }

    /**
     * This method returns the default string resource.
     *
     * @return The default string resource.
     */
    @Override
    @StringRes
    public int getDefaultTitle() {
        return R.string.genres;
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingGenres;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        final Bundle bundle = new Bundle(1);
        final Fragment fragment =
                Fragment.instantiate(getActivity(), ArtistsFragment.class.getName(), bundle);

        bundle.putParcelable(Genre.EXTRA, mItems.get(position));

        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(fragment, Artist.EXTRA);
    }
}
