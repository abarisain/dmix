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

package com.namelessdev.mpdroid.views;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class AlbumDataBinder extends BaseDataBinder {

    @Override
    public AbstractViewHolder findInnerViews(final View targetView) {
        // look up all references to inner views
        final AlbumViewHolder viewHolder = new AlbumViewHolder();
        viewHolder.mAlbumName = (TextView) targetView.findViewById(R.id.album_name);
        viewHolder.mAlbumInfo = (TextView) targetView.findViewById(R.id.album_info);
        viewHolder.mAlbumCover = (ImageView) targetView.findViewById(R.id.albumCover);
        viewHolder.mCoverArtProgress = (ProgressBar) targetView
                .findViewById(R.id.albumCoverProgress);
        return viewHolder;
    }

    @Override
    @LayoutRes
    public int getLayoutId() {
        return R.layout.album_list_item;
    }

    @Override
    public boolean isEnabled(final int position, final List<? extends Item> items,
            final Object item) {
        return true;
    }

    protected void loadAlbumCovers(final AlbumViewHolder holder, final Album album) {
        final Artist artist = album.getArtist();

        if (artist == null || album.isUnknown()) {
            // full albums list or unknown album
            holder.mAlbumCover.setVisibility(View.GONE);
        } else {
            holder.mAlbumCover.setVisibility(View.VISIBLE);

            final CoverAsyncHelper coverHelper = getCoverHelper(holder, 128);
            final AlbumInfo albumInfo = new AlbumInfo(album);

            // display cover art in album listing if caching is on
            if (albumInfo.isValid() && mEnableCache) {
                setCoverListener(holder, coverHelper);
                loadArtwork(coverHelper, albumInfo);
            }
        }
    }

    @Override
    public void onDataBind(final Context context, final View targetView,
            final AbstractViewHolder viewHolder, final List<? extends Item> items,
            final Object item, final int position) {
        final AlbumViewHolder holder = (AlbumViewHolder) viewHolder;
        final Album album = (Album) item;
        final Artist artist = album.getArtist();
        final StringBuilder info = new StringBuilder();
        final long songCount = album.getSongCount();

        if (artist != null) {
            info.append(artist.mainText());
        }

        if (album.getYear() > 0L) {
            if (info.length() != 0) {
                info.append(SEPARATOR);
            }
            info.append(Long.toString(album.getYear()));
        }

        if (songCount > 0L) {
            final String trackHeader;
            final String duration = Music.timeToString(album.getDuration());

            if (info.length() != 0) {
                info.append(SEPARATOR);
            }

            if (songCount > 1L) {
                trackHeader =
                        context.getString(R.string.tracksInfoHeaderPlural, songCount, duration);
            } else {
                trackHeader = context.getString(R.string.tracksInfoHeader, songCount, duration);
            }

            info.append(trackHeader);
        }

        // display "artist - album title"
        holder.mAlbumName.setText(album.mainText());
        if (info.length() == 0) {
            holder.mAlbumInfo.setVisibility(View.GONE);
        } else {
            holder.mAlbumInfo.setVisibility(View.VISIBLE);
            holder.mAlbumInfo.setText(info);
        }

        loadAlbumCovers(holder, album);
    }

    @Override
    public View onLayoutInflation(final Context context, final View targetView,
            final List<? extends Item> items) {
        return setViewVisible(targetView, R.id.albumCover, mEnableCache);
    }
}
