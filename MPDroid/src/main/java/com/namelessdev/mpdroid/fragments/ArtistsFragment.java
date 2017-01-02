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
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Genre;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.models.GenresGroup;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class ArtistsFragment extends BrowseFragment<Artist> {

    public static final String PREFERENCE_ALBUM_LIBRARY = "enableAlbumArtLibrary";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE = "artistTagToUse";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST = Music.TAG_ALBUM_ARTIST;

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ARTIST = Music.TAG_ARTIST;

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_BOTH = "both";

    private static final String TAG = "ArtistsFragment";

    private GenresGroup mGenresGroup;

    public ArtistsFragment() {
        super(R.string.addArtist, R.string.artistAdded);
    }

    @Override
    protected void add(final Artist item, final boolean replace, final boolean play) {
        try {
            mApp.getMPD().add(item, replace, play);
            if (isAdded()) {
                Tools.notifyUser(mIrAdded, item);
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add to queue.", e);
        }
    }

    @Override
    protected void add(final Artist item, final PlaylistFile playlist) {
        try {
            mApp.getMPD().addToPlaylist(playlist, item);
            if (isAdded()) {
                Tools.notifyUser(mIrAdded, item);
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add to playlist.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            final SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(MPDApplication.getInstance());
            final Collection<Artist> artists;
            switch (settings.getString(PREFERENCE_ARTIST_TAG_TO_USE,
                    PREFERENCE_ARTIST_TAG_TO_USE_BOTH).toLowerCase()) {
                case PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST:
                    if (mGenresGroup == null) {
                        artists = mApp.getMPD().getAlbumArtists();
                    } else {
                        artists = new HashSet<>();
                        for (final Genre genre : mGenresGroup.getGenres()) {
                            artists.addAll(mApp.getMPD().getAlbumArtists(genre));
                        }
                    }
                    break;
                case PREFERENCE_ARTIST_TAG_TO_USE_ARTIST:
                    if (mGenresGroup == null) {
                        artists = mApp.getMPD().getArtists();
                    } else {
                        artists = new HashSet<>();
                        for (final Genre genre : mGenresGroup.getGenres()) {
                            artists.addAll(mApp.getMPD().getArtists(genre));
                        }
                    }
                    break;
                case PREFERENCE_ARTIST_TAG_TO_USE_BOTH:
                default:
                    if (mGenresGroup == null) {
                        artists = mApp.getMPD().getArtistsMerged();
                    } else {
                        artists = new HashSet<>();
                        for (final Genre genre : mGenresGroup.getGenres()) {
                            artists.addAll(mApp.getMPD().getArtistsMerged(genre));
                        }
                    }
                    break;
            }
            replaceItems(artists);
            Collections.sort(mItems);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    protected Artist getArtist(final Artist item) {
        return item;
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
        return R.string.loadingArtists;
    }

    @Override
    public String getTitle() {
        final String title;

        if (mGenresGroup == null) {
            final Bundle bundle = getArguments();
            String name = null;
            if (bundle != null) {
                final GenresGroup genresGroup = bundle.getParcelable(GenresGroup.EXTRA);

                if (genresGroup != null) {
                    name = genresGroup.getName();
                }
            }

            if (name == null) {
                title = super.getTitle();
            } else {
                title = name;
            }
        } else {
            title = mGenresGroup.toString();
        }

        return title;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle;
        if (savedInstanceState == null) {
            bundle = getArguments();
        } else {
            bundle = savedInstanceState;
        }

        if (bundle != null) {
            mGenresGroup = bundle.getParcelable(GenresGroup.EXTRA);
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mApp);
        final Activity activity = getActivity();
        final Bundle bundle = new Bundle(2);
        final Fragment fragment;

        bundle.putParcelable(Artist.EXTRA, mItems.get(position));
        bundle.putParcelable(GenresGroup.EXTRA, mGenresGroup);

        if (settings.getBoolean(PREFERENCE_ALBUM_LIBRARY, true)) {
            fragment = Fragment.instantiate(activity, AlbumsGridFragment.class.getName(), bundle);
        } else {
            fragment = Fragment.instantiate(activity, AlbumsFragment.class.getName(), bundle);
        }

        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(fragment, Album.EXTRA);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (mGenresGroup != null) {
            outState.putParcelable(GenresGroup.EXTRA, mGenresGroup);
        }
        super.onSaveInstanceState(outState);
    }

}
