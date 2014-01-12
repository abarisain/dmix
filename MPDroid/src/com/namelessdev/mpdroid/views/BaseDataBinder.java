/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.adapters.ArrayDataBinder;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.Item;

import java.util.List;

public abstract class BaseDataBinder implements ArrayDataBinder {

    MPDApplication app = null;
    boolean lightTheme = false;
    boolean enableCache = true;
    boolean onlyDownloadOnWifi = true;

    public BaseDataBinder(MPDApplication app, boolean isLightTheme) {
        this.app = app;
        this.lightTheme = isLightTheme;
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

        enableCache = settings.getBoolean(CoverManager.PREFERENCE_CACHE, true);
        onlyDownloadOnWifi = settings.getBoolean(CoverManager.PREFERENCE_ONLY_WIFI, false);
    }

    @Override
    public abstract AbstractViewHolder findInnerViews(View targetView);

    @Override
    public abstract int getLayoutId();

    @Override
    public abstract boolean isEnabled(int position, List<? extends Item> items, Object item);

    protected void loadArtwork(CoverAsyncHelper coverHelper, AlbumInfo albumInfo) {
        coverHelper.downloadCover(albumInfo);
    }

    protected void loadPlaceholder(CoverAsyncHelper coverHelper) {
        coverHelper.obtainMessage(CoverAsyncHelper.EVENT_COVER_NOT_FOUND).sendToTarget();
    }

    @Override
    public abstract void onDataBind(Context context, View targetView,
            AbstractViewHolder viewHolder, List<? extends Item> items,
            Object item,
            int position);

    @Override
    public abstract View onLayoutInflation(Context context, View targetView,
            List<? extends Item> items);

}
