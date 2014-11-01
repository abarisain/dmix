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
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverInfo;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SongDataBinder;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.widget.PopupMenuCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

public class SongsFragment extends BrowseFragment {

    private static final String EXTRA_ALBUM = "album";

    private static final String TAG = "SongsFragment";

    Album mAlbum = null;

    ImageButton mAlbumMenu;

    ImageView mCoverArt;

    ProgressBar mCoverArtProgress;

    CoverAsyncHelper mCoverHelper;

    TextView mHeaderArtist;

    TextView mHeaderInfo;

    PopupMenu mPopupMenu;

    private AlbumCoverDownloadListener mCoverArtListener;

    private PopupMenu mCoverPopupMenu;

    public SongsFragment() {
        super(R.string.addSong, R.string.songAdded, MPDCommand.MPD_SEARCH_TITLE);
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        final Music music = (Music) item;
        try {
            mApp.oMPDAsyncHelper.oMPD.add(music, replace, play);
            Tools.notifyUser(R.string.songAdded, music.getTitle(), music.getName());
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add, remove, play.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
        try {
            mApp.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Music) item);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add to playlist.", e);
        }
    }

    @Override
    public void asyncUpdate() {
        try {
            if (getActivity() == null) {
                return;
            }
            mItems = mApp.oMPDAsyncHelper.oMPD.getSongs(mAlbum);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to async update.", e);
        }
    }

    @Override
    protected boolean forceEmptyView() {
        return true;
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (mItems != null) {
            Music song;
            boolean differentArtists = false;
            String lastArtist = null;
            for (final Item item : mItems) {
                song = (Music) item;
                if (lastArtist == null) {
                    lastArtist = song.getArtist();
                    continue;
                }
                if (!lastArtist.equalsIgnoreCase(song.getAlbumArtistOrArtist())) {
                    differentArtists = true;
                    break;
                }
            }
            return new ArrayAdapter(getActivity(), new SongDataBinder(differentArtists), mItems);
        }
        return super.getCustomListAdapter();
    }

    private AlbumInfo getFixedAlbumInfo() {
        Music song;
        AlbumInfo albumInfo = null;
        boolean differentArtists = false;

        for (final Item item : mItems) {
            song = (Music) item;
            if (albumInfo == null) {
                albumInfo = new AlbumInfo(song);
                continue;
            }
            final String a = albumInfo.getArtist();
            if (a != null && !a.equalsIgnoreCase(song.getAlbumArtistOrArtist())) {
                differentArtists = true;
                break;
            }
        }

        if (differentArtists || albumInfo == null) {
            return new AlbumInfo(getString(R.string.variousArtists), mAlbum.getName());
        }
        return albumInfo;
    }

    private CharSequence getHeaderInfoString() {
        final int count = mItems.size();
        return getString(count > 1 ? R.string.tracksInfoHeaderPlural
                        : R.string.tracksInfoHeader, count,
                getTotalTimeForTrackList()
        );
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingSongs;
    }

    @Override
    public String getTitle() {
        final String result;

        if (mAlbum == null) {
            result = getString(R.string.songs);
        } else {
            result = mAlbum.mainText();
        }

        return result;
    }

    private String getTotalTimeForTrackList() {
        Music song;
        long totalTime = 0;
        for (final Item item : mItems) {
            song = (Music) item;
            if (song.getTime() > 0) {
                totalTime += song.getTime();
            }
        }
        return Music.timeToString(totalTime);
    }

    public SongsFragment init(final Album al) {
        mAlbum = al;
        return this;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            init((Album) savedInstanceState.getParcelable(EXTRA_ALBUM));
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.songs, container, false);
        mList = (AbsListView) view.findViewById(R.id.list);
        registerForContextMenu(mList);
        mList.setOnItemClickListener(this);

        mLoadingView = view.findViewById(R.id.loadingLayout);
        mLoadingTextView = (TextView) view.findViewById(R.id.loadingText);
        mNoResultView = view.findViewById(R.id.noResultLayout);
        mLoadingTextView.setText(getLoadingText());

        final View headerView = inflater.inflate(R.layout.song_header, null, false);
        mCoverArt = (ImageView) view.findViewById(R.id.albumCover);
        if (mCoverArt != null) {
            mHeaderArtist = (TextView) view.findViewById(R.id.tracks_artist);
            mHeaderInfo = (TextView) view.findViewById(R.id.tracks_info);
            mCoverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
            mAlbumMenu = (ImageButton) view.findViewById(R.id.album_menu);
        } else {
            mHeaderArtist = (TextView) headerView.findViewById(R.id.tracks_artist);
            mHeaderInfo = (TextView) headerView.findViewById(R.id.tracks_info);
            mCoverArt = (ImageView) headerView.findViewById(R.id.albumCover);
            mCoverArtProgress = (ProgressBar) headerView.findViewById(R.id.albumCoverProgress);
            mAlbumMenu = (ImageButton) headerView.findViewById(R.id.album_menu);
        }

        mCoverArtListener = new AlbumCoverDownloadListener(mCoverArt, mCoverArtProgress, false);
        mCoverHelper = new CoverAsyncHelper();
        mCoverHelper.setCoverMaxSizeFromScreen(getActivity());
        final ViewTreeObserver vto = mCoverArt.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mCoverHelper != null) {
                    mCoverHelper.setCachedCoverMaxSize(mCoverArt.getMeasuredHeight());
                }
                return true;
            }
        });
        mCoverHelper.addCoverDownloadListener(mCoverArtListener);

        ((TextView) headerView.findViewById(R.id.separator_title)).setText(R.string.songs);
        ((ListView) mList).addHeaderView(headerView, null, false);

        mPopupMenu = new PopupMenu(getActivity(), mAlbumMenu);
        mPopupMenu.getMenu().add(Menu.NONE, ADD, Menu.NONE, R.string.addAlbum);
        mPopupMenu.getMenu().add(Menu.NONE, ADD_REPLACE, Menu.NONE, R.string.addAndReplace);
        mPopupMenu.getMenu()
                .add(Menu.NONE, ADD_REPLACE_PLAY, Menu.NONE, R.string.addAndReplacePlay);
        mPopupMenu.getMenu().add(Menu.NONE, ADD_PLAY, Menu.NONE, R.string.addAndPlay);
        mPopupMenu.getMenu().add(Menu.NONE, GOTO_ARTIST, Menu.NONE, R.string.goToArtist);

        mPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                final int itemId = item.getItemId();
                if (itemId == GOTO_ARTIST) {
                    final Intent intent = new Intent(getActivity(), SimpleLibraryActivity.class);
                    intent.putExtra("artist", mAlbum.getArtist());
                    startActivityForResult(intent, -1);
                } else {
                    mApp.oMPDAsyncHelper.execAsync(new Runnable() {
                        @Override
                        public void run() {
                            boolean replace = false;
                            boolean play = false;
                            switch (itemId) {
                                case ADD_REPLACE_PLAY:
                                    replace = true;
                                    play = true;
                                    break;
                                case ADD_REPLACE:
                                    replace = true;
                                    break;
                                case ADD_PLAY:
                                    play = true;
                                    break;
                                default:
                                    break;
                            }
                            try {
                                mApp.oMPDAsyncHelper.oMPD.add(mAlbum, replace, play);
                                Tools.notifyUser(R.string.albumAdded, mAlbum);
                            } catch (final IOException | MPDException e) {
                                Log.e(TAG, "Failed to add, replace, play.", e);
                            }
                        }
                    });
                }
                return true;
            }
        });

        mAlbumMenu.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(mPopupMenu));
        mAlbumMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                mPopupMenu.show();
            }
        });

        mCoverPopupMenu = new PopupMenu(getActivity(), mCoverArt);
        mCoverPopupMenu.getMenu().add(POPUP_COVER_BLACKLIST, POPUP_COVER_BLACKLIST, 0,
                R.string.otherCover);
        mCoverPopupMenu.getMenu().add(POPUP_COVER_SELECTIVE_CLEAN, POPUP_COVER_SELECTIVE_CLEAN, 0,
                R.string.resetCover);
        mCoverPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                final int groupId = item.getGroupId();
                boolean result = true;

                if (groupId == POPUP_COVER_BLACKLIST) {
                    final AlbumInfo albumInfo = new AlbumInfo(mAlbum);

                    CoverManager.getInstance().markWrongCover(albumInfo);
                    updateCover(albumInfo);
                    updateNowPlayingSmallFragment(albumInfo);
                } else if (groupId == POPUP_COVER_SELECTIVE_CLEAN) {
                    final AlbumInfo albumInfo = new AlbumInfo(mAlbum);

                    CoverManager.getInstance().clear(albumInfo);
                    updateCover(albumInfo);
                    updateNowPlayingSmallFragment(albumInfo);
                } else {
                    result = false;
                }

                return result;
            }
        });

        mCoverArt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                mCoverPopupMenu.show();
                return false;
            }
        });

        updateFromItems();

        return view;
    }

    @Override
    public void onDestroyView() {
        mHeaderArtist = null;
        mHeaderInfo = null;
        mCoverArtListener.freeCoverDrawable();
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mCoverHelper = null;
        super.onDetach();
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        // If in simple mode : add, replace and play the shown album.
        if (mApp.isInSimpleMode()) {
            mApp.oMPDAsyncHelper.execAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        mApp.oMPDAsyncHelper.oMPD.add(mAlbum, true, true);
                        // Account for the list header
                        int positionCorrection = 0;
                        if (mList instanceof ListView) {
                            positionCorrection = ((ListView) mList).getHeaderViewsCount();
                        }
                        mApp.oMPDAsyncHelper.oMPD.seekByIndex(position - positionCorrection, 0l);
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to seek by index.", e);
                    }
                }
            });
        } else {
            mApp.oMPDAsyncHelper.execAsync(new Runnable() {
                @Override
                public void run() {
                    add((Item) parent.getAdapter().getItem(position), false, false);
                }
            });
        }

    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putParcelable(EXTRA_ALBUM, mAlbum);
        super.onSaveInstanceState(outState);
    }

    public void updateCover(final AlbumInfo albumInfo) {
        if (mCoverArt != null && null != mCoverArt.getTag()
                && mCoverArt.getTag().equals(albumInfo.getKey())) {
            mCoverHelper.downloadCover(albumInfo, true);
        }
    }

    @Override
    public void updateFromItems() {
        super.updateFromItems();
        if (mItems != null && mHeaderArtist != null && mHeaderInfo != null) {
            final AlbumInfo fixedAlbumInfo;
            fixedAlbumInfo = getFixedAlbumInfo();
            final String artist = fixedAlbumInfo.getArtist();
            mHeaderArtist.setText(artist);
            mHeaderInfo.setText(getHeaderInfoString());
            if (mCoverHelper != null) {
                mCoverHelper.downloadCover(fixedAlbumInfo, true);
            } else {
                mCoverArtListener.onCoverNotFound(new CoverInfo(fixedAlbumInfo));
            }
        }

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
