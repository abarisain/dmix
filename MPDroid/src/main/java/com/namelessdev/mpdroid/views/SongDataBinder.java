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

import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.SongCommentActivity;
import com.namelessdev.mpdroid.adapters.ArrayDataBinder;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.SongViewHolder;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

public class SongDataBinder<T extends Item<T>> implements ArrayDataBinder<T> {

    private final boolean mShowArtist;

    public SongDataBinder(final boolean showArtist) {
        mShowArtist = showArtist;
    }

    @Override
    public AbstractViewHolder findInnerViews(final View targetView) {
        return new SongViewHolder(targetView);
    }

    @Override
    @LayoutRes
    public int getLayoutId() {
        return R.layout.song_list_item;
    }

    @Override
    public boolean isEnabled(final int position, final List<T> items, final Object item) {
        return true;
    }

    @Override
    public void onDataBind(final Context context, final View targetView,
            final AbstractViewHolder viewHolder, final List<T> items, final Object item,
            final int position) {
        final SongViewHolder holder = (SongViewHolder) viewHolder;
        final Music song = (Music) item;
        final StringBuilder track = new StringBuilder();
        final int disc = song.getDisc();
        int trackNumber = song.getTrack();

        if (disc > 0) {
            track.append(disc).append('-');
        }

        if (trackNumber < 0) {
            trackNumber = 0;
        }

        if (trackNumber < 10) {
            track.append(0);
        }
        track.append(trackNumber);

        holder.getTrackTitle().setText(song.getTitle());
        holder.getTrackNumber().setText(track);
        holder.getTrackDuration().setText(song.getFormattedTime());

        if (mShowArtist) {
            holder.getTrackArtist().setText(song.getArtistName());
        }

        final String comments = song.getComments();
        final ImageButton comment = holder.getComment();
        if (TextUtils.isEmpty(comments)) {
            comment.setVisibility(View.GONE);
        } else {
            comment.setVisibility(View.VISIBLE);
            comment.setTag(comments);
        }
    }

    @Override
    public View onLayoutInflation(final Context context, final View targetView,
            final List<T> items) {
        targetView.findViewById(R.id.show_comments).setOnClickListener(new CommentClickListener());
        return BaseDataBinder.setViewVisible(targetView, R.id.track_artist, mShowArtist);
    }

    /**
     * This class is the listener for the Comment button.
     */
    private static final class CommentClickListener implements View.OnClickListener {

        /**
         * Sole constructor.
         */
        private CommentClickListener() {
        }

        @Override
        public void onClick(final View v) {
            final Object tag = v.getTag();

            if (tag instanceof String) {
                final Context context = v.getContext();
                final Intent intent = new Intent(context, SongCommentActivity.class);
                intent.putExtra(SongCommentActivity.COMMENT_KEY, (String) tag);
                context.startActivity(intent);
            }
        }
    }
}
