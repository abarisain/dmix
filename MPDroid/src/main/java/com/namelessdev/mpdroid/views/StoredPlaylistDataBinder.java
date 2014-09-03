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
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.PlaylistViewHolder;

import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class StoredPlaylistDataBinder extends BaseDataBinder {

    private final MPDApplication app = MPDApplication.getInstance();

    public StoredPlaylistDataBinder(boolean isLightTheme) {
        super(isLightTheme);
    }

    @Override
    public AbstractViewHolder findInnerViews(View targetView) {
        PlaylistViewHolder viewHolder = new PlaylistViewHolder();
        viewHolder.name = (TextView) targetView.findViewById(R.id.playlist_name);
        viewHolder.info = (TextView) targetView.findViewById(R.id.playlist_info);
        viewHolder.cover = (ImageView) targetView.findViewById(R.id.playlist_cover);
        return viewHolder;
    }

    @Override
    public int getLayoutId() {
        return R.layout.playlist_list_item;
    }

    public boolean isEnabled(int position, List<? extends Item> items, Object item) {
        return true;
    }

    public void onDataBind(final Context context, final View targetView,
            final AbstractViewHolder viewHolder, List<? extends Item> items, Object item,
            int position) {
        PlaylistViewHolder holder = (PlaylistViewHolder) viewHolder;

        final Music music = (Music) item;
        String artist = music.getArtist();
        String album = music.getAlbum();

        if (Tools.isStringEmptyOrNull(artist))
            artist = null;
        if (Tools.isStringEmptyOrNull(album))
            album = null;

        String info = "";
        if (artist != null || album != null) {
            if (artist == null) {
                info = album;
            } else if (album == null) {
                info = artist;
            } else {
                info = artist + " - " + album;
            }
        }

        holder.name.setText(music.getTitle());
        if (!Tools.isStringEmptyOrNull(info)) {
            holder.info.setVisibility(View.VISIBLE);
            holder.info.setText(info);
        } else {
            holder.info.setVisibility(View.GONE);
        }

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

        final CoverAsyncHelper coverHelper = new CoverAsyncHelper();
        final int height = holder.cover.getHeight();
        // If the list is not displayed yet, the height is 0. This is a problem,
        // so set a fallback one.
        coverHelper.setCoverMaxSize(height == 0 ? 128 : height);

        loadPlaceholder(coverHelper);

        // display cover art in album listing if caching is on
        if (artist != null && album != null && enableCache) {
            // listen for new artwork to be loaded
            final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(holder.cover);
            final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) holder.cover
                    .getTag(R.id.AlbumCoverDownloadListener);
            if (oldAcd != null)
                oldAcd.detach();
            holder.cover.setTag(R.id.AlbumCoverDownloadListener, acd);
            coverHelper.addCoverDownloadListener(acd);
            loadArtwork(coverHelper, music.getAlbumInfo());
        }
    }

    @Override
    public View onLayoutInflation(Context context, View targetView, List<? extends Item> items) {
        targetView.findViewById(R.id.playlist_cover).setVisibility(
                enableCache ? View.VISIBLE : View.GONE);
        return targetView;
    }
}
