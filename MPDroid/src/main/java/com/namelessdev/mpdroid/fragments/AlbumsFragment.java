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
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.AlbumDataBinder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Genre;
import org.a0z.mpd.item.Item;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AlbumsFragment extends BrowseFragment {

    private static final String ALBUM_YEAR_SORT_KEY = "sortAlbumsByYear";

    private static final String EXTRA_ARTIST = "artist";

    private static final String EXTRA_GENRE = "genre";

    private static final String SHOW_ALBUM_TRACK_COUNT_KEY = "showAlbumTrackCount";

    private static final String TAG = "AlbumsFragment";

    protected Artist mArtist = null;

    protected ProgressBar mCoverArtProgress;

    protected Genre mGenre = null;

    protected boolean mIsCountDisplayed;

    public AlbumsFragment() {
        this(null);
    }

    @SuppressLint("ValidFragment")
    public AlbumsFragment(final Artist artist) {
        this(artist, null);
    }

    public AlbumsFragment(final Artist artist, final Genre genre) {
        super(R.string.addAlbum, R.string.albumAdded, MPDCommand.MPD_SEARCH_ALBUM);
        init(artist, genre);
    }

    private static void refreshCover(final View view, final AlbumInfo album) {
        if (view.getTag() instanceof AlbumViewHolder) {
            final AlbumViewHolder albumViewHolder = (AlbumViewHolder) view.getTag();
            if (albumViewHolder.mAlbumCover
                    .getTag(R.id.CoverAsyncHelper) instanceof CoverAsyncHelper) {
                final CoverAsyncHelper coverAsyncHelper
                        = (CoverAsyncHelper) albumViewHolder.mAlbumCover
                        .getTag(R.id.CoverAsyncHelper);
                coverAsyncHelper.downloadCover(album, true);
            }
        }
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        try {
            mApp.oMPDAsyncHelper.oMPD.add((Album) item, replace, play);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
        try {
            mApp.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Album) item);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mApp);
        final boolean sortByYear = settings.getBoolean(ALBUM_YEAR_SORT_KEY, false);

        try {
            mItems = mApp.oMPDAsyncHelper.oMPD.getAlbums(mArtist, sortByYear, mIsCountDisplayed);

            if (sortByYear) {
                Collections.sort((List<? extends Album>) mItems, Album.SORT_BY_YEAR);
            }

            if (mGenre != null) { // filter albums not in genre
                for (int i = mItems.size() - 1; i >= 0; i--) {
                    if (!mApp.oMPDAsyncHelper.oMPD.isAlbumInGenre((Album) mItems.get(i), mGenre)) {
                        mItems.remove(i);
                    }
                }
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    /**
     * Uses CoverManager to clean up a cover.
     *
     * @param item         The MenuItem from the user interaction.
     * @param isWrongCover True to blacklist the cover, false otherwise.
     */
    private void cleanupCover(final MenuItem item, final boolean isWrongCover) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        final Album album = (Album) mItems.get((int) info.id);
        final AlbumInfo albumInfo = new AlbumInfo(album);

        if (isWrongCover) {
            CoverManager.getInstance()
                    .markWrongCover(albumInfo);
        } else {
            CoverManager.getInstance()
                    .clear(albumInfo);
        }

        refreshCover(info.targetView, albumInfo);
        updateNowPlayingSmallFragment(albumInfo);
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (mItems != null) {
            return new ArrayIndexerAdapter(getActivity(),
                    new AlbumDataBinder(), mItems);
        }
        return super.getCustomListAdapter();
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingAlbums;
    }

    @Override
    public String getTitle() {
        if (mArtist != null) {
            return mArtist.mainText();
        } else {
            return getString(R.string.albums);
        }
    }

    public AlbumsFragment init(final Artist artist, final Genre genre) {
        mArtist = artist;
        mGenre = genre;
        return this;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            init((Artist) savedInstanceState.getParcelable(EXTRA_ARTIST),
                    (Genre) savedInstanceState.getParcelable(EXTRA_GENRE));
        }
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuItem otherCoverItem = menu.add(POPUP_COVER_BLACKLIST,
                POPUP_COVER_BLACKLIST, 0, R.string.otherCover);
        otherCoverItem.setOnMenuItemClickListener(this);
        final MenuItem resetCoverItem = menu.add(POPUP_COVER_SELECTIVE_CLEAN,
                POPUP_COVER_SELECTIVE_CLEAN, 0, R.string.resetCover);
        resetCoverItem.setOnMenuItemClickListener(this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        mCoverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        return view;

    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                new SongsFragment().init((Album) mItems.get(position)),
                "songs");
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        boolean result = false;

        switch (item.getGroupId()) {
            case GOTO_ARTIST:
                final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                final Object selectedItem = mItems.get((int) info.id);
                final Intent intent = new Intent(getActivity(), SimpleLibraryActivity.class);
                final Album a = (Album) selectedItem;

                intent.putExtra("artist", a.getArtist());
                startActivityForResult(intent, -1);
                break;
            case POPUP_COVER_BLACKLIST:
                cleanupCover(item, true);
                break;
            case POPUP_COVER_SELECTIVE_CLEAN:
                cleanupCover(item, false);
                break;
            default:
                result = super.onMenuItemClick(item);
                break;
        }
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsCountDisplayed = PreferenceManager.getDefaultSharedPreferences(mApp)
                .getBoolean(SHOW_ALBUM_TRACK_COUNT_KEY, true);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (mArtist != null) {
            outState.putParcelable(EXTRA_ARTIST, mArtist);
        }

        if (mGenre != null) {
            outState.putParcelable(EXTRA_GENRE, mGenre);
        }
        super.onSaveInstanceState(outState);
    }

    private void updateNowPlayingSmallFragment(final AlbumInfo albumInfo) {
        final NowPlayingSmallFragment nowPlayingSmallFragment;
        if (getActivity() != null) {
            nowPlayingSmallFragment = (NowPlayingSmallFragment) getActivity()
                    .getSupportFragmentManager().findFragmentById(R.id.now_playing_small_fragment);
            if (nowPlayingSmallFragment != null) {
                nowPlayingSmallFragment.updateCover(albumInfo);
            }
        }
    }
}
