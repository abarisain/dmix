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
import com.namelessdev.mpdroid.adapters.ArrayDataBinder;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.SongViewHolder;

import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class SongDataBinder implements ArrayDataBinder {

    private final boolean mShowArtist;

    public SongDataBinder() {
        this(true);
    }

    public SongDataBinder(final boolean showArtist) {
        super();
        mShowArtist = showArtist;
    }

    @Override
    public AbstractViewHolder findInnerViews(final View targetView) {
        // look up all references to inner views
        final SongViewHolder viewHolder = new SongViewHolder();
        viewHolder.mTrackTitle = (TextView) targetView.findViewById(R.id.track_title);
        viewHolder.mTrackNumber = (TextView) targetView.findViewById(R.id.track_number);
        viewHolder.mTrackDuration = (TextView) targetView.findViewById(R.id.track_duration);
        viewHolder.mTrackArtist = (TextView) targetView.findViewById(R.id.track_artist);
        return viewHolder;
    }

    @Override
    @LayoutRes
    public int getLayoutId() {
        return R.layout.song_list_item;
    }

    @Override
    public boolean isEnabled(final int position, final List<? extends Item> items,
            final Object item) {
        return true;
    }

    @Override
    public void onDataBind(final Context context, final View targetView,
            final AbstractViewHolder viewHolder, final List<? extends Item> items,
            final Object item,
            final int position) {
        final SongViewHolder holder = (SongViewHolder) viewHolder;
        final Music song = (Music) item;
        final StringBuilder track = new StringBuilder(3);
        int trackNumber = song.getTrack();

        if (trackNumber < 0) {
            trackNumber = 0;
        }

        if (trackNumber < 10) {
            track.append(0);
        }
        track.append(trackNumber);

        holder.mTrackTitle.setText(song.getTitle());
        holder.mTrackNumber.setText(track);
        holder.mTrackDuration.setText(song.getFormattedTime());

        if (mShowArtist) {
            holder.mTrackArtist.setText(song.getArtist());
        }
    }

    @Override
    public View onLayoutInflation(final Context context, final View targetView,
            final List<? extends Item> items) {
        return BaseDataBinder.setViewVisible(targetView, R.id.track_artist, mShowArtist);
    }

}
