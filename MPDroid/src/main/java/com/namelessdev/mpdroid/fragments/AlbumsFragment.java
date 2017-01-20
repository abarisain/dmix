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
import com.anpmech.mpd.item.PlaylistFile;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.cover.CoverAsyncHelper;
import com.namelessdev.mpdroid.cover.CoverManager;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.models.GenresGroup;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.AlbumDataBinder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
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

public class AlbumsFragment extends BrowseFragment<Album> {

    private static final String ALBUM_YEAR_SORT_KEY = "sortAlbumsByYear";

    private static final String SHOW_ALBUM_TRACK_COUNT_KEY = "showAlbumTrackCount";

    private static final String TAG = "AlbumsFragment";

    protected Artist mArtist;

    protected ProgressBar mCoverArtProgress;

    private GenresGroup mGenresGroups;

    protected boolean mIsCountDisplayed;

    public AlbumsFragment() {
        super(R.string.addAlbum, R.string.albumAdded);
    }

    private static void refreshCover(final View view, final AlbumInfo album) {
        if (view.getTag() instanceof AlbumViewHolder) {
            final AlbumViewHolder albumViewHolder = (AlbumViewHolder) view.getTag();
            final Object tag = albumViewHolder.mAlbumCover.getTag(R.id.CoverAsyncHelper);

            if (tag instanceof CoverAsyncHelper) {
                ((CoverAsyncHelper) tag).downloadCover(album, true);
            }
        }
    }

    @Override
    protected void add(final Album item, final boolean replace, final boolean play) {
        try {
            mApp.getMPD().add(item, replace, play);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Album item, final PlaylistFile playlist) {
        try {
            mApp.getMPD().addToPlaylist(playlist, item);
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
            replaceItems(mApp.getMPD().getAlbums(mArtist, sortByYear, mIsCountDisplayed));

            if (sortByYear) {
                Collections.sort(mItems, Album.SORT_BY_DATE);
            } else {
                Collections.sort(mItems);
            }

            if (mGenresGroups != null) { // filter albums not in genre
                for (int i = mItems.size() - 1; i >= 0; i--) {
                    if (!isAlbumInOneGenre(mItems.get(i), mGenresGroups)) {
                        mItems.remove(i);
                    }
                }
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    private boolean isAlbumInOneGenre(final Album album, final GenresGroup genres) throws IOException, MPDException {
        for (final Genre genre : genres.getGenres()) {
            if (mApp.getMPD().isAlbumInGenre(album, genre)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uses CoverManager to clean up a cover.
     *
     * @param item         The MenuItem from the user interaction.
     * @param isWrongCover True to blacklist the cover, false otherwise.
     */
    private void cleanupCover(final MenuItem item, final boolean isWrongCover) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        final Album album = mItems.get((int) info.id);
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
    protected Artist getArtist(final Album item) {
        return item.getArtist();
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        return new ArrayIndexerAdapter<>(getActivity(), new AlbumDataBinder<Album>(mArtist),
                mItems);
    }

    /**
     * This method returns the default string resource.
     *
     * @return The default string resource.
     */
    @Override
    @StringRes
    public int getDefaultTitle() {
        return R.string.albums;
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingAlbums;
    }

    @Override
    public String getTitle() {
        final String title;

        if (mArtist == null) {
            title = super.getTitle();
        } else {
            title = mArtist.toString();
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
            mArtist = bundle.getParcelable(Artist.EXTRA);
            mGenresGroups = bundle.getParcelable(GenresGroup.EXTRA);
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
        final Activity activity = getActivity();
        final Bundle bundle = new Bundle();
        final Fragment fragment = Fragment.instantiate(activity, SongsFragment.class.getName(),
                bundle);

        bundle.putParcelable(Album.EXTRA, mItems.get(position));

        // Terribly bugged
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final TransitionInflater inflater = TransitionInflater.from(activity);
            final ImageView albumCoverView = (ImageView) view.findViewById(R.id.albumCover);
            final String transitionName = albumCoverView.getTransitionName();
            final Drawable drawable = albumCoverView.getDrawable();

            if (drawable instanceof BitmapDrawable) {
                bundle.putParcelable(SongsFragment.COVER_THUMBNAIL_BUNDLE_KEY,
                        ((BitmapDrawable) drawable).getBitmap());
            }

            bundle.putString(SongsFragment.COVER_TRANSITION_NAME_BASE, transitionName);

            ((ILibraryFragmentActivity) activity).pushLibraryFragment(
                    fragment, "songs", albumCoverView,
                    albumCoverView.getTransitionName(),
                    inflater.inflateTransition(R.transition.album_songs_transition));
        } else {*/
        ((ILibraryFragmentActivity) activity).pushLibraryFragment(fragment, "songs");
        //}
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

        mIsCountDisplayed = PreferenceManager.getDefaultSharedPreferences(mApp)
                .getBoolean(SHOW_ALBUM_TRACK_COUNT_KEY, true);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (mArtist != null) {
            outState.putParcelable(Artist.EXTRA, mArtist);
        }

        if (mGenresGroups != null) {
            outState.putParcelable(GenresGroup.EXTRA, mGenresGroups);
        }
        super.onSaveInstanceState(outState);
    }
}
