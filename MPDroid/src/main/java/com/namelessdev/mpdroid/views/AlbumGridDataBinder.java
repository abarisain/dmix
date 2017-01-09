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

package com.namelessdev.mpdroid.views;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.CoverAsyncHelper;
import com.namelessdev.mpdroid.favorites.Favorites;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.List;

public class AlbumGridDataBinder extends AlbumDataBinder<Album> {

    private static final String TAG = "AlbumGridDataBinder";

    /**
     * Sole constructor.
     *
     * @param displayedArtist This is the {@link Artist} for the group of {@link Album}s being
     *                        displayed. If the Album group has no common artist, this will be
     *                        null.
     */
    public AlbumGridDataBinder(final Artist displayedArtist) {
        super(displayedArtist);
    }

    @Override
    @LayoutRes
    public int getLayoutId() {
        return R.layout.album_grid_item;
    }

    @Override
    protected void loadAlbumCovers(final AlbumViewHolder holder, final Album album) {
        final CoverAsyncHelper coverHelper = getCoverHelper(holder, 256);
        final AlbumInfo albumInfo = new AlbumInfo(album);

        setCoverListener(holder, coverHelper);

        // Can't get artwork for missing album name
        if (albumInfo.isValid() && mEnableCache) {
            loadArtwork(coverHelper, albumInfo);
        }
    }

    @Override
    public AbstractViewHolder findInnerViews(final View targetView) {
        final AlbumViewHolder viewHolder = (AlbumViewHolder) super.findInnerViews(targetView);
        viewHolder.mFavoriteButton = (ToggleButton) targetView.findViewById(R.id.favoriteButton);
        return viewHolder;
    }

    @Override
    public void onDataBind(Context context, View targetView, AbstractViewHolder viewHolder, List<Album> items, Object item, int position) {
        super.onDataBind(context, targetView, viewHolder, items, item, position);

        final AlbumViewHolder holder = (AlbumViewHolder) viewHolder;
        final Album album = (Album) item;

        holder.mFavoriteButton.setOnCheckedChangeListener(null);
        if (Favorites.areFavoritesActivated()) {
            holder.mFavoriteButton.setVisibility(View.VISIBLE);

            final MPDApplication app = MPDApplication.getInstance();

            try {
                holder.mFavoriteButton.setChecked(app.getFavorites().isFavorite(album));
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Unable to determine if album is a favorite.", e);
            }

            holder.mFavoriteButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    try {
                        if (isChecked) {
                            app.getFavorites().addAlbum(album);
                        } else {
                            app.getFavorites().removeAlbum(album);
                        }
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Unable to change favorite state of album.", e);
                    }
                }
            });
        } else {
            holder.mFavoriteButton.setVisibility(View.GONE);
        }
    }
}
