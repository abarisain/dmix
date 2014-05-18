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

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import java.util.List;

public class AlbumGridDataBinder extends AlbumDataBinder {
    SharedPreferences settings;

    public AlbumGridDataBinder(MPDApplication app, boolean isLightTheme) {
        super(isLightTheme);
        settings = PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Override
    public int getLayoutId() {
        return R.layout.album_grid_item;
    }

    @Override
    public void onDataBind(final Context context, final View targetView,
            final AbstractViewHolder viewHolder, List<? extends Item> items, Object item,
            int position) {
        AlbumViewHolder holder = (AlbumViewHolder) viewHolder;

        final Album album = (Album) item;

        // Caching must be switch on to use this view
        final CoverAsyncHelper coverHelper = new CoverAsyncHelper();
        final int height = holder.albumCover.getHeight();
        // If the list is not displayed yet, the height is 0. This is a problem,
        // so set a fallback one.
        coverHelper.setCoverMaxSize(height == 0 ? 256 : height);

        // display "artist - album title"
        String text = album.mainText();
        holder.albumName.setText(text);

        Artist artist = album.getArtist();
        String info = "";
        final long songCount = album.getSongCount();
        if (artist != null) {
            info += artist.mainText();
        }
        if (album.getYear() > 0) {
            if (!info.isEmpty()) {
                info += " - ";
            }
            info += Long.toString(album.getYear());
        }
        if (songCount > 0) {
            if (!info.isEmpty()) {
                info += " - ";
            }
            info += String.format(context.getString(songCount > 1 ? R.string.tracksInfoHeaderPlural
                    : R.string.tracksInfoHeader),
                    songCount, Music.timeToString(album.getDuration()));
        }
        holder.albumName.setText(album.mainText());
        if (info != null && info.length() > 0) {
            holder.albumInfo.setVisibility(View.VISIBLE);
            holder.albumInfo.setText(info);
        } else {
            holder.albumInfo.setVisibility(View.GONE);
        }

        // listen for new artwork to be loaded
        final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener( holder.albumCover,
                holder.coverArtProgress, false);
        final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) holder.albumCover
                .getTag(R.id.AlbumCoverDownloadListener);
        if (oldAcd != null)
            oldAcd.detach();
        holder.albumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
        holder.albumCover.setTag(R.id.CoverAsyncHelper, coverHelper);
        coverHelper.addCoverDownloadListener(acd);

        loadPlaceholder(coverHelper);

        // Can't get artwork for missing album name
        if (album.getAlbumInfo().isValid() && enableCache) {
            loadArtwork(coverHelper, album.getAlbumInfo());
        }
    }

}
