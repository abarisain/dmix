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
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.PlaylistViewHolder;

import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class StoredPlaylistDataBinder extends BaseDataBinder {

    @Override
    public AbstractViewHolder findInnerViews(final View targetView) {
        final PlaylistViewHolder viewHolder = new PlaylistViewHolder();

        viewHolder.mName = (TextView) targetView.findViewById(R.id.playlist_name);
        viewHolder.mInfo = (TextView) targetView.findViewById(R.id.playlist_info);
        viewHolder.mAlbumCover = (ImageView) targetView.findViewById(R.id.playlist_cover);
        viewHolder.mCoverArtProgress =
                (ProgressBar) targetView.findViewById(R.id.playlist_cover_progress);

        return viewHolder;
    }

    @Override
    @LayoutRes
    public int getLayoutId() {
        return R.layout.playlist_list_item;
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
        final PlaylistViewHolder holder = (PlaylistViewHolder) viewHolder;
        final Music music = (Music) item;
        String artist = music.getArtist();
        String album = music.getAlbum();

        if (Tools.isStringEmptyOrNull(artist)) {
            artist = null;
        }
        if (Tools.isStringEmptyOrNull(album)) {
            album = null;
        }

        String info = null;
        if (artist != null || album != null) {
            if (artist == null) {
                info = album;
            } else if (album == null) {
                info = artist;
            } else {
                info = artist + SEPARATOR + album;
            }
        }

        holder.mName.setText(music.getTitle());
        if (Tools.isStringEmptyOrNull(info)) {
            holder.mInfo.setVisibility(View.GONE);
        } else {
            holder.mInfo.setVisibility(View.VISIBLE);
            holder.mInfo.setText(info);
        }

        final CoverAsyncHelper coverHelper = getCoverHelper(holder, 128);

        // display cover art in album listing if caching is on
        if (artist != null && album != null && mEnableCache) {
            setCoverListener(holder, coverHelper);
            loadArtwork(coverHelper, new AlbumInfo(music));
        }
    }

    @Override
    public View onLayoutInflation(final Context context, final View targetView,
            final List<? extends Item> items) {
        return setViewVisible(targetView, R.id.playlist_cover, mEnableCache);
    }
}
