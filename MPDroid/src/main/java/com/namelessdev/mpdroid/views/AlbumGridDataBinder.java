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

import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;

import android.support.annotation.LayoutRes;

public class AlbumGridDataBinder extends AlbumDataBinder<Album> {

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
}
