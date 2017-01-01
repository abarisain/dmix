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


import android.support.annotation.StringRes;
import android.util.Log;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.namelessdev.mpdroid.R;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class RandomBrowseFragment extends SongsFragment {

    private static final String TAG = "RandomBrowseFragment";

    @Override
    protected void asyncUpdate() {
        try {
            final List<Album> albums = mApp.getMPD().getAlbums(null, false, false);
            mAlbum = !albums.isEmpty() ? albums.get(new Random().nextInt(albums.size())) : null;
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }

        super.asyncUpdate();
    }

    @Override
    public String getTitle() {
        return mAlbum != null ? mAlbum.getName() : null;
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingAlbum;
    }

}

