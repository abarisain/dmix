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
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Genre;
import org.a0z.mpd.item.Item;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.io.IOException;

public class ArtistsFragment extends BrowseFragment {

    private static final String EXTRA_GENRE = "genre";

    private static final String TAG = "ArtistsFragment";

    private Genre mGenre = null;

    public ArtistsFragment() {
        super(R.string.addArtist, R.string.artistAdded, MPDCommand.MPD_SEARCH_ARTIST);
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        try {
            mApp.oMPDAsyncHelper.oMPD.add((Artist) item, replace, play);
            if (isAdded()) {
                Tools.notifyUser(mIrAdded, item);
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add to queue.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
        try {
            mApp.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Artist) item);
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
            switch (settings.getString(LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE,
                    LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_BOTH).toLowerCase()) {
                case LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST:
                    if (mGenre != null) {
                        mItems = mApp.oMPDAsyncHelper.oMPD.getArtists(mGenre, true);
                    } else {
                        mItems = mApp.oMPDAsyncHelper.oMPD.getArtists(true);
                    }
                    break;
                case LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_ARTIST:
                    if (mGenre != null) {
                        mItems = mApp.oMPDAsyncHelper.oMPD.getArtists(mGenre, false);
                    } else {
                        mItems = mApp.oMPDAsyncHelper.oMPD.getArtists(false);
                    }
                    break;
                case LibraryFragment.PREFERENCE_ARTIST_TAG_TO_USE_BOTH:
                default:
                    if (mGenre != null) {
                        mItems = mApp.oMPDAsyncHelper.oMPD.getArtists(mGenre);
                    } else {
                        mItems = mApp.oMPDAsyncHelper.oMPD.getArtists();
                    }
                    break;
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingArtists;
    }

    @Override
    public String getTitle() {
        if (mGenre != null) {
            return mGenre.mainText();
        } else {
            return getString(R.string.genres);
        }
    }

    public ArtistsFragment init(final Genre g) {
        mGenre = g;
        return this;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            init((Genre) savedInstanceState.getParcelable(EXTRA_GENRE));
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        final AlbumsFragment af;
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mApp);
        if (settings.getBoolean(LibraryFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
            af = new AlbumsGridFragment((Artist) mItems.get(position), mGenre);
        } else {
            af = new AlbumsFragment((Artist) mItems.get(position), mGenre);
        }
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(af, "album");
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (mGenre != null) {
            outState.putParcelable(EXTRA_GENRE, mGenre);
        }
        super.onSaveInstanceState(outState);
    }

}
