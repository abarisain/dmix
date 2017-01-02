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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Genre;
import com.anpmech.mpd.item.PlaylistFile;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.models.GenresGroup;
import com.namelessdev.mpdroid.tools.Tools;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GenresFragment extends BrowseFragment<GenresGroup> {

    public static final String PREFERENCE_OPTIMIZE_GENRES = "optimizeGenres";

    public static final String PREFERENCE_GENRE_SEPARATORS = "genreSeparators";

    private static final String TAG = "GenresFragment";

    public GenresFragment() {
        super(R.string.addGenre, R.string.genreAdded);
    }

    @Override
    protected void add(final GenresGroup item, final boolean replace, final boolean play) {
        try {
            for (final Genre genre : item.getGenres()) {
                mApp.getMPD().add(genre, replace, play);
            }
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add all from playlist.", e);
        }
    }

    @Override
    protected void add(final GenresGroup item, final PlaylistFile playlist) {
        try {
            for (final Genre genre : item.getGenres()) {
                mApp.getMPD().addToPlaylist(playlist, genre);
            }
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add all genre to playlist.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            replaceItems(groupGenres(mApp.getMPD().getGenreResponse()));
            Collections.sort(mItems);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update list of genres.", e);
        }
    }

    private List<GenresGroup> groupGenres(final Collection<Genre> genres) {
        final Map<String,GenresGroup> groupMap = new HashMap<>();

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mApp);
        final boolean optimizeGenres = settings.getBoolean(PREFERENCE_OPTIMIZE_GENRES, false);
        final String genreSeparators = optimizeGenres
                ? settings.getString(PREFERENCE_GENRE_SEPARATORS, "").replace(" ", "")
                : "";

        for (final Genre genre : genres) {
            final String[] genreNames = genreSeparators.isEmpty()
                    ? new String[] { genre.getName() }
                    : genre.getName().split("[" + Pattern.quote(genreSeparators) + "]");
            for (String genreName : genreNames) {
                if (optimizeGenres) {
                    genreName = capitalize(genreName.replace("  ", " "));
                }

                GenresGroup group = groupMap.get(genreName);

                if (group == null) {
                    group = new GenresGroup(genreName);
                    groupMap.put(genreName, group);
                }
                group.addGenre(genre);
            }
        }

        return new LinkedList<>(groupMap.values());
    }

    @NonNull
    private String capitalize(String s) {
        s = s.toLowerCase();
        for (final Character delimiter : " &-".toCharArray()) {
            s = capitalize(s, delimiter);
        }
        return s;
    }

    private String capitalize(final String s, final Character delimiter) {
        String result = "";
        for (final String word : s.split(delimiter.toString())) {
            if (!result.isEmpty()) {
                result += delimiter;
            }
            if (!word.isEmpty()) {
                result += word.substring(0, 1).toUpperCase() +
                          (word.length() > 1 ? word.substring(1) : "");
            }
        }
        return result;
    }

    @Override
    protected Artist getArtist(final GenresGroup item) {
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

        bundle.putParcelable(GenresGroup.EXTRA, mItems.get(position));

        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(fragment, Artist.EXTRA);
    }

}
