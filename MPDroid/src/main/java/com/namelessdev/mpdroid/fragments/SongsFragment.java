/*
 * Copyright (C) 2010-2015 The MPDroid Project
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
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.melnykov.fab.FloatingActionButton;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverInfo;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.ui.ToolbarHelper;
import com.namelessdev.mpdroid.views.SongDataBinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.PopupMenuCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
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
import java.util.Collections;

public class SongsFragment extends BrowseFragment<Music> {

    public static final String COVER_THUMBNAIL_BUNDLE_KEY = "CoverThumbnailBundle";

    public static final String COVER_TRANSITION_NAME_BASE = "cover";

    private static final String STATE_FIRST_REFRESH = "firstRefresh";

    private static final String STATE_VIEW_TRANSITION_NAME = "viewTransitionName";

    private static final String TAG = "SongsFragment";

    Album mAlbum;

    ImageButton mAlbumMenu;

    ImageView mCoverArt;

    ProgressBar mCoverArtProgress;

    CoverAsyncHelper mCoverHelper;

    Bitmap mCoverThumbnailBitmap;

    boolean mFirstRefresh;

    Handler mHandler;

    TextView mHeaderAlbum;

    TextView mHeaderArtist;

    TextView mHeaderInfo;

    Toolbar mHeaderToolbar;

    View mTracksInfoContainer;

    String mViewTransitionName;

    private AlbumCoverDownloadListener mCoverArtListener;

    private PopupMenu mCoverPopupMenu;

    public SongsFragment() {
        super(R.string.addSong, R.string.songAdded);
        mHandler = new Handler();
        mFirstRefresh = true;
    }

    @Override
    protected void add(final Music item, final boolean replace, final boolean play) {
        try {
            mApp.getMPD().add(item, replace, play);
            Tools.notifyUser(R.string.songAdded, item.getTitle(), item.getName());
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add, remove, play.", e);
        }
    }

    @Override
    protected void add(final Music item, final PlaylistFile playlist) {
        try {
            mApp.getMPD().addToPlaylist(playlist, item);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add to playlist.", e);
        }
    }

    private void applyPaletteWithBitmapAsync(final Bitmap bitmap) {
        Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(final Palette palette) {
                try {
                    final Palette.Swatch vibrantColor = palette.getDarkVibrantSwatch();
                    final Palette.Swatch mutedColor = palette.getVibrantSwatch();

                    if (mTracksInfoContainer != null && vibrantColor != null && !isDetached()) {
                        mTracksInfoContainer.setBackgroundColor(vibrantColor.getRgb());
                        mHeaderAlbum.setTextColor(vibrantColor.getBodyTextColor());
                        mHeaderArtist.setTextColor(vibrantColor.getBodyTextColor());
                        mHeaderInfo.setTextColor(vibrantColor.getBodyTextColor());
                    }

                    if (mutedColor != null && mAlbumMenu instanceof FloatingActionButton) {
                        ((FloatingActionButton) mAlbumMenu).setColorNormal(mutedColor.getRgb());
                    }
                } catch (final NullPointerException | IllegalStateException e) {
                    Log.e(TAG, "Error while applying generated album art palette colors", e);
                }
            }
        });
    }

    @Override
    public void asyncUpdate() {
        try {
            if (getActivity() != null) {
                replaceItems(mApp.getMPD().getSongs(mAlbum));
                Collections.sort(mItems);
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to async update.", e);
        }
    }

    @Override
    protected boolean forceEmptyView() {
        return true;
    }

    @Override
    protected Artist getArtist(final Music item) {
        return new Artist(item.getAlbumArtistOrArtist());
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        boolean differentArtists = false;
        String lastArtist = null;
        for (final Music item : mItems) {
            if (lastArtist == null) {
                lastArtist = item.getArtistName();
                continue;
            }
            if (!lastArtist.equalsIgnoreCase(item.getAlbumArtistOrArtist())) {
                differentArtists = true;
                break;
            }
        }
        return new ArrayAdapter<>(getActivity(), new SongDataBinder<Music>(differentArtists),
                mItems);
    }

    private AlbumInfo getFixedAlbumInfo() {
        AlbumInfo albumInfo = null;
        boolean differentArtists = false;

        for (final Music item : mItems) {
            if (albumInfo == null) {
                albumInfo = new AlbumInfo(item);
                continue;
            }

            final String a = albumInfo.getArtistName();
            if (a != null && !a.equalsIgnoreCase(item.getAlbumArtistOrArtist())) {
                differentArtists = true;
                break;
            }
        }

        if (differentArtists || albumInfo == null) {
            albumInfo = new AlbumInfo(getString(R.string.variousArtists), mAlbum.getName());
        }
        return albumInfo;
    }

    private CharSequence getHeaderInfoString() {
        final int count = mItems.size();
        final int header;

        if (count > 1) {
            header = R.string.tracksInfoHeaderPlural;
        } else {
            header = R.string.tracksInfoHeader;
        }

        return getString(header, count, getTotalTimeForTrackList());
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
            final Bundle bundle = getArguments();

            if (bundle == null) {
                result = mApp.getString(R.string.songs);
            } else {
                result = bundle.getParcelable(Album.EXTRA).toString();
            }
        } else {
            result = mAlbum.toString();
        }

        return result;
    }

    private CharSequence getTotalTimeForTrackList() {
        long totalTime = 0L;

        for (final Music item : mItems) {
            final long time = item.getTime();

            if (time > 0L) {
                totalTime += time;
            }
        }
        return com.anpmech.mpd.Tools.timeToString(totalTime);
    }

    @Override
    protected void hideToolbar() {
        if (mHeaderToolbar != null) {
            mHeaderToolbar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle;
        if (savedInstanceState == null) {
            bundle = getArguments();
        } else {
            bundle = savedInstanceState;
            mFirstRefresh = savedInstanceState.getBoolean(STATE_FIRST_REFRESH, true);
        }

        if (bundle != null) {
            mAlbum = bundle.getParcelable(Album.EXTRA);
            mCoverThumbnailBitmap = bundle.getParcelable(COVER_THUMBNAIL_BUNDLE_KEY);
            mViewTransitionName = bundle.getString(STATE_VIEW_TRANSITION_NAME);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.songs, container, false);
        mList = (AbsListView) view.findViewById(R.id.list);
        registerForContextMenu(mList);
        mList.setOnItemClickListener(this);
        mList.setFastScrollEnabled(false);
        if (mList instanceof ListView) {
            ((ListView) mList).setDivider(null);
        }

        mLoadingView = view.findViewById(R.id.loadingLayout);
        mLoadingTextView = (TextView) view.findViewById(R.id.loadingText);
        mNoResultView = view.findViewById(R.id.noResultLayout);
        mLoadingTextView.setText(getLoadingText());

        final View headerView = inflater.inflate(R.layout.song_header, null, false);
        mCoverArt = (ImageView) view.findViewById(R.id.albumCover);
        if (mCoverArt != null) {
            populateViews(view);
        } else {
            populateViews(headerView);
            mCoverArt = (ImageView) headerView.findViewById(R.id.albumCover);
        }

        ViewCompat.setTransitionName(mCoverArt, mViewTransitionName);
        if (mCoverThumbnailBitmap != null) {
            mCoverArt.setImageBitmap(mCoverThumbnailBitmap);
            applyPaletteWithBitmapAsync(mCoverThumbnailBitmap);
            mCoverThumbnailBitmap = null;
        }

        if (mHeaderToolbar == null) {
            mHeaderToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        }

        if (mHeaderToolbar != null) {
            ToolbarHelper.addSearchView(getActivity(), mHeaderToolbar);
            ToolbarHelper.showBackButton(this, mHeaderToolbar);
        }

        mCoverArtListener = new AlbumCoverDownloadListener(mCoverArt, mCoverArtProgress, false) {
            @Override
            public void onCoverDownloaded(final CoverInfo cover) {
                super.onCoverDownloaded(cover);
                final Drawable drawable = mCoverArt.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    applyPaletteWithBitmapAsync(((BitmapDrawable) drawable).getBitmap());
                }
            }
        };

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

        //((TextView) headerView.findViewById(R.id.separator_title)).setText(R.string.songs);
        ((ListView) mList).addHeaderView(headerView, null, false);

        final PopupMenu popupMenu = new PopupMenu(getActivity(), mAlbumMenu);
        final Menu menu = popupMenu.getMenu();

        menu.add(Menu.NONE, ADD, Menu.NONE, R.string.addAlbum);
        menu.add(Menu.NONE, ADD_REPLACE, Menu.NONE, R.string.addAndReplace);
        menu.add(Menu.NONE, ADD_REPLACE_PLAY, Menu.NONE, R.string.addAndReplacePlay);
        menu.add(Menu.NONE, ADD_PLAY, Menu.NONE, R.string.addAndPlay);
        menu.add(Menu.NONE, GOTO_ARTIST, Menu.NONE, R.string.goToArtist);

        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                final int itemId = item.getItemId();
                if (itemId == GOTO_ARTIST) {
                    final Intent intent = new Intent(getActivity(), SimpleLibraryActivity.class);
                    intent.putExtra(Artist.EXTRA, mAlbum.getArtist());
                    startActivityForResult(intent, -1);
                } else {
                    mApp.getAsyncHelper().execAsync(new Runnable() {
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
                                mApp.getMPD().add(mAlbum, replace, play);
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

        mAlbumMenu.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(popupMenu));
        mAlbumMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                popupMenu.show();
                return true;
            }
        });
        mAlbumMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                mApp.getAsyncHelper().execAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mApp.getMPD().add(mAlbum, false, true);
                        } catch (final IOException | MPDException e) {
                            Log.e(TAG, "Failed to add album.", e);
                        }
                        Tools.notifyUser(R.string.albumAdded, mAlbum);
                    }
                });
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

        updateToolbarVisibility();

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
            mApp.getAsyncHelper().execAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        mApp.getMPD().add(mAlbum, true, true);
                        // Account for the list header
                        int positionCorrection = 0;
                        if (mList instanceof ListView) {
                            positionCorrection = ((ListView) mList).getHeaderViewsCount();
                        }
                        mApp.getMPD().getPlayback().seek(position - positionCorrection, 0L);
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to seek by index.", e);
                    }
                }
            });
        } else {
            addAdapterItem(parent, position);
        }

    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putParcelable(Album.EXTRA, mAlbum);
        outState.putBoolean(STATE_FIRST_REFRESH, mFirstRefresh);
        outState.putString(STATE_VIEW_TRANSITION_NAME, mViewTransitionName);
        super.onSaveInstanceState(outState);
    }

    private void populateViews(final View view) {
        mTracksInfoContainer = view.findViewById(R.id.tracks_info_container);
        mHeaderArtist = (TextView) view.findViewById(R.id.tracks_artist);
        mHeaderAlbum = (TextView) view.findViewById(R.id.tracks_album);
        mHeaderInfo = (TextView) view.findViewById(R.id.tracks_info);
        mHeaderToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mCoverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        mAlbumMenu = (ImageButton) view.findViewById(R.id.album_menu);
    }

    @Override
    protected void refreshFastScrollStyle(final boolean shouldShowFastScroll) {
        // No fast scroll for that view
        refreshFastScrollStyle(View.SCROLLBARS_INSIDE_OVERLAY, false);
    }

    @Override
    protected void showToolbar() {
        if (mHeaderToolbar != null) {
            mHeaderToolbar.setVisibility(View.VISIBLE);
        }
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
        if (!mItems.isEmpty() && mHeaderArtist != null && mHeaderInfo != null) {
            final AlbumInfo fixedAlbumInfo;
            fixedAlbumInfo = getFixedAlbumInfo();
            final String artist = fixedAlbumInfo.getArtistName();
            mHeaderArtist.setText(artist);
            if (mHeaderAlbum != null) {
                mHeaderAlbum.setText(fixedAlbumInfo.getAlbumName());
            }
            mHeaderInfo.setText(getHeaderInfoString());
            if (mCoverHelper != null) {
                // Delay the cover art download for Lollipop transition
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mFirstRefresh) {
                    // Hardcode a delay, we don't have a transition end callback ...
                    // TODO : Refactor this with "onSharedElementEnd", if it's worth it.
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mCoverHelper != null) {
                                mCoverHelper.downloadCover(fixedAlbumInfo, true);
                            }
                        }
                    }, 500L);
                } else {
                    mCoverHelper.downloadCover(fixedAlbumInfo, true);
                }
            } else {
                mCoverArtListener.onCoverNotFound(new CoverInfo(fixedAlbumInfo));
            }
            mFirstRefresh = false;
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
