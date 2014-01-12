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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.AlbumDataBinder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import org.a0z.mpd.Album;
import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

public class AlbumsFragment extends BrowseFragment {
    private static final String EXTRA_ARTIST = "artist";
    private static final String EXTRA_GENRE = "genre";
    protected Artist artist = null;
    protected Genre genre = null;
    protected boolean isCountPossiblyDisplayed;
    protected ProgressBar coverArtProgress;

    private static final int POPUP_COVER_BLACKLIST = 5;
    private static final int POPUP_COVER_SELECTIVE_CLEAN = 6;

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
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, ((Album) item));
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void asyncUpdate() {
        try {
            items = app.oMPDAsyncHelper.oMPD.getAlbums(artist, isCountPossiblyDisplayed);
            if (genre != null) { // filter albums not in genre
                for (int i = items.size() - 1; i >= 0; i--) {
                    if (!app.oMPDAsyncHelper.oMPD.albumInGenre((Album) items.get(i), genre)) {
                        items.remove(i);
                    }
                }
            }
        } catch (MPDServerException e) {
        }
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (items != null) {
            return new ArrayIndexerAdapter(getActivity(),
                    new AlbumDataBinder(app, app.isLightThemeSelected()), items);
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
            return artist.getName();
        } else {
            return getString(R.string.albums);
        }
    }

    public AlbumsFragment init(Artist artist, Genre genre) {
        isCountPossiblyDisplayed = true;
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
    public boolean onMenuItemClick(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Album album;
        switch (item.getGroupId()) {
            case POPUP_COVER_BLACKLIST:
                album = (Album) items.get((int) info.id);
                CoverManager.getInstance(app,
                        PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext()))
                        .markWrongCover(album.getAlbumInfo());
                refreshCover(info.targetView, album.getAlbumInfo());
                updateNowPlayingSmallFragment(album.getAlbumInfo());
                break;
            case POPUP_COVER_SELECTIVE_CLEAN:
                album = (Album) items.get((int) info.id);
                CoverManager.getInstance(app,
                        PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext()))
                        .clear(album.getAlbumInfo());
                refreshCover(info.targetView, album.getAlbumInfo());
                updateNowPlayingSmallFragment(album.getAlbumInfo());
                break;
            default:
                return super.onMenuItemClick(item);
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(EXTRA_ARTIST, artist);
        outState.putParcelable(EXTRA_GENRE, genre);
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
