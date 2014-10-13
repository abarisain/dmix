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
import com.namelessdev.mpdroid.helpers.CoverDownloadListener;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class AlbumDataBinder extends BaseDataBinder {

    private final MPDApplication mApp = MPDApplication.getInstance();

    public AlbumDataBinder(final boolean isLightTheme) {
        super(isLightTheme);
    }

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

    @Override
    public void onDataBind(final Context context, final View targetView,
            final AbstractViewHolder viewHolder, final List<? extends Item> items,
            final Object item, final int position) {
        final AlbumViewHolder holder = (AlbumViewHolder) viewHolder;

        final Album album = (Album) item;
        final Artist artist = album.getArtist();
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
        holder.mAlbumName.setText(album.mainText());
        if (info != null && !info.isEmpty()) {
            holder.mAlbumInfo.setVisibility(View.VISIBLE);
            holder.mAlbumInfo.setText(info);
        } else {
            holder.mAlbumInfo.setVisibility(View.GONE);
        }

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mApp);

        if (artist == null || album.isUnknown()) { // full albums list or
            // unknown album
            holder.mAlbumCover.setVisibility(View.GONE);
        } else {
            holder.mAlbumCover.setVisibility(View.VISIBLE);
            final CoverAsyncHelper coverHelper = new CoverAsyncHelper();
            final int height = holder.mAlbumCover.getHeight();
            // If the list is not displayed yet, the height is 0. This is a
            // problem, so set a fallback one.
            coverHelper.setCoverMaxSize(height == 0 ? 128 : height);

            loadPlaceholder(coverHelper);

            // display cover art in album listing if caching is on
            if (album.getAlbumInfo().isValid() && mEnableCache) {
                // listen for new artwork to be loaded
                final CoverDownloadListener acd = new AlbumCoverDownloadListener(
                        holder.mAlbumCover, holder.mCoverArtProgress, false);
                final AlbumCoverDownloadListener oldAcd
                        = (AlbumCoverDownloadListener) holder.mAlbumCover
                        .getTag(R.id.AlbumCoverDownloadListener);
                if (oldAcd != null) {
                    oldAcd.detach();
                }

                holder.mAlbumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
                holder.mAlbumCover.setTag(R.id.CoverAsyncHelper, coverHelper);
                coverHelper.addCoverDownloadListener(acd);
                loadArtwork(coverHelper, album.getAlbumInfo());
            }
        }
    }

    @Override
    public View onLayoutInflation(final Context context, final View targetView,
            final List<? extends Item> items) {
        targetView.findViewById(R.id.albumCover).setVisibility(
                mEnableCache ? View.VISIBLE : View.GONE);
        return targetView;
    }
}
