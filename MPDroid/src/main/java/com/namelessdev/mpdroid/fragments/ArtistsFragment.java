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
import java.util.Collections;

public class ArtistsFragment extends BrowseFragment<Artist> {

    public static final String PREFERENCE_ALBUM_LIBRARY = "enableAlbumArtLibrary";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE = "artistTagToUse";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST = Music.TAG_ALBUM_ARTIST;

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ARTIST = Music.TAG_ARTIST;

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_BOTH = "both";

    private static final String TAG = "ArtistsFragment";

    private Genre mGenre = null;

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
            switch (settings.getString(PREFERENCE_ARTIST_TAG_TO_USE,
                    PREFERENCE_ARTIST_TAG_TO_USE_BOTH).toLowerCase()) {
                case PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST:
                    if (mGenre == null) {
                        replaceItems(mApp.getMPD().getAlbumArtists());
                    } else {
                        replaceItems(mApp.getMPD().getAlbumArtists(mGenre));
                    }
                    break;
                case PREFERENCE_ARTIST_TAG_TO_USE_ARTIST:
                    if (mGenre == null) {
                        replaceItems(mApp.getMPD().getArtists());
                    } else {
                        replaceItems(mApp.getMPD().getArtists(mGenre));
                    }
                    break;
                case PREFERENCE_ARTIST_TAG_TO_USE_BOTH:
                default:
                    if (mGenre == null) {
                        replaceItems(mApp.getMPD().getArtistsMerged());
                    } else {
                        replaceItems(mApp.getMPD().getArtistsMerged(mGenre));
                    }
                    break;
            }
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

        if (mGenre == null) {
            final Bundle bundle = getArguments();
            String name = null;
            if (bundle != null) {
                final Genre genre = bundle.getParcelable(Genre.EXTRA);

                if (genre != null) {
                    name = genre.getName();
                }
            }

            if (name == null) {
                title = super.getTitle();
            } else {
                title = name;
            }
        } else {
            title = mGenre.toString();
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
            mGenre = bundle.getParcelable(Genre.EXTRA);
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
        bundle.putParcelable(Genre.EXTRA, mGenre);

        if (settings.getBoolean(PREFERENCE_ALBUM_LIBRARY, true)) {
            fragment = Fragment.instantiate(activity, AlbumsGridFragment.class.getName(), bundle);
        } else {
            fragment = Fragment.instantiate(activity, AlbumsFragment.class.getName(), bundle);
        }

        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(fragment, Album.EXTRA);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (mGenre != null) {
            outState.putParcelable(Genre.EXTRA, mGenre);
        }
        super.onSaveInstanceState(outState);
    }

}
