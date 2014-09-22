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
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.AlbumDataBinder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.ArtistParcelable;
import org.a0z.mpd.item.Genre;
import org.a0z.mpd.item.GenreParcelable;
import org.a0z.mpd.item.Item;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

public class AlbumsFragment extends BrowseFragment {
    private static final String EXTRA_ARTIST = "artist";
    private static final String EXTRA_GENRE = "genre";
    protected Artist artist = null;
    protected Genre genre = null;
    protected boolean isCountDisplayed;
    protected ProgressBar coverArtProgress;

    private static final int POPUP_COVER_BLACKLIST = 5;
    private static final int POPUP_COVER_SELECTIVE_CLEAN = 6;

    private static final String TAG = "AlbumsFragment";

    private static final String SHOW_ALBUM_TRACK_COUNT_KEY = "showAlbumTrackCount";

    public AlbumsFragment() {
        this(null);
    }

    @SuppressLint("ValidFragment")
    public AlbumsFragment(Artist artist) {
        this(artist, null);
    }

    public AlbumsFragment(Artist artist, Genre genre) {
        super(R.string.addAlbum, R.string.albumAdded, MPDCommand.MPD_SEARCH_ALBUM);
        init(artist, genre);
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add((Album) item, replace, play);
            Tools.notifyUser(irAdded, item);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, ((Album) item));
            Tools.notifyUser(irAdded, item);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            items = app.oMPDAsyncHelper.oMPD.getAlbums(artist, isCountDisplayed);
            if (genre != null) { // filter albums not in genre
                for (int i = items.size() - 1; i >= 0; i--) {
                    if (!app.oMPDAsyncHelper.oMPD.albumInGenre((Album) items.get(i), genre)) {
                        items.remove(i);
                    }
                }
            }
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    /**
     * Uses CoverManager to clean up a cover.
     *
     * @param item The MenuItem from the user interaction.
     * @param isWrongCover True to blacklist the cover, false otherwise.
     */
    private void cleanupCover(final MenuItem item, final boolean isWrongCover) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        final Album album = (Album) items.get((int) info.id);
        final AlbumInfo albumInfo = album.getAlbumInfo();

        if(isWrongCover) {
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
        if (items != null) {
            return new ArrayIndexerAdapter(getActivity(),
                    new AlbumDataBinder(app.isLightThemeSelected()), items);
        }
        return super.getCustomListAdapter();
    }

    @Override
    public int getLoadingText() {
        return R.string.loadingAlbums;
    }

    @Override
    public String getTitle() {
        if (artist != null) {
            return artist.mainText();
        } else {
            return getString(R.string.albums);
        }
    }

    public AlbumsFragment init(Artist artist, Genre genre) {
        this.artist = artist;
        this.genre = genre;
        return this;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null)
            init((Artist) icicle.getParcelable(EXTRA_ARTIST),
                    (Genre) icicle.getParcelable(EXTRA_GENRE));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        android.view.MenuItem otherCoverItem = menu.add(POPUP_COVER_BLACKLIST,
                POPUP_COVER_BLACKLIST, 0, R.string.otherCover);
        otherCoverItem.setOnMenuItemClickListener(this);
        android.view.MenuItem resetCoverItem = menu.add(POPUP_COVER_SELECTIVE_CLEAN,
                POPUP_COVER_SELECTIVE_CLEAN, 0, R.string.resetCover);
        resetCoverItem.setOnMenuItemClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        return view;

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                new SongsFragment().init((Album) items.get(position)),
                "songs");
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        boolean result = false;

        switch (item.getGroupId()) {
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

        isCountDisplayed = PreferenceManager.getDefaultSharedPreferences(app)
                .getBoolean(SHOW_ALBUM_TRACK_COUNT_KEY, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (artist != null) {
            outState.putParcelable(EXTRA_ARTIST, new ArtistParcelable(artist));
        }

        if (genre != null) {
            outState.putParcelable(EXTRA_GENRE, new GenreParcelable(genre));
        }
        super.onSaveInstanceState(outState);
    }

    private void refreshCover(View view, AlbumInfo album) {
        if (view.getTag() instanceof AlbumViewHolder) {
            AlbumViewHolder albumViewHolder = (AlbumViewHolder) view.getTag();
            if (albumViewHolder.albumCover.getTag(R.id.CoverAsyncHelper) instanceof CoverAsyncHelper) {
                CoverAsyncHelper coverAsyncHelper = (CoverAsyncHelper) albumViewHolder.albumCover
                        .getTag(R.id.CoverAsyncHelper);
                coverAsyncHelper.downloadCover(album, true);
            }
        }
    }

    private void updateNowPlayingSmallFragment(AlbumInfo albumInfo) {
        NowPlayingSmallFragment nowPlayingSmallFragment;
        if (getActivity() != null) {
            nowPlayingSmallFragment = (NowPlayingSmallFragment) getActivity()
                    .getSupportFragmentManager().findFragmentById(R.id.now_playing_small_fragment);
            if (nowPlayingSmallFragment != null) {
                nowPlayingSmallFragment.updateCover(albumInfo);
            }
        }
    }
}
