/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.namelessdev.mpdroid.views.holders;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.views.SongDataBinder;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * This class holds the {@link View}s required for the {@link SongDataBinder}.
 */
public class SongViewHolder extends AbstractViewHolder {

    /**
     * The comment button View.
     */
    private final ImageButton mComment;

    /**
     * The track artist view.
     */
    private final TextView mTrackArtist;

    /**
     * The track duration View.
     */
    private final TextView mTrackDuration;

    /**
     * The track number View.
     */
    private final TextView mTrackNumber;

    /**
     * The track title View.
     */
    private final TextView mTrackTitle;

    /**
     * Sole constructor.
     *
     * @param view The current {@link View}.
     */
    public SongViewHolder(final View view) {
        super();

        mTrackArtist = (TextView) view.findViewById(R.id.track_artist);
        mTrackDuration = (TextView) view.findViewById(R.id.track_duration);
        mTrackNumber = (TextView) view.findViewById(R.id.track_number);
        mTrackTitle = (TextView) view.findViewById(R.id.track_title);
        mComment = (ImageButton) view.findViewById(R.id.show_comments);
    }

    /**
     * Get the comment button View.
     *
     * @return The comment button View.
     */
    public ImageButton getComment() {
        return mComment;
    }

    /**
     * Gets the track artist View.
     *
     * @return The track artist view.
     */
    public TextView getTrackArtist() {
        return mTrackArtist;
    }

    /**
     * Get the track duration View.
     *
     * @return The track duration View.
     */
    public TextView getTrackDuration() {
        return mTrackDuration;
    }

    /**
     * Gets the track number View.
     *
     * @return The track number View.
     */
    public TextView getTrackNumber() {
        return mTrackNumber;
    }

    /**
     * Get the track title View.
     *
     * @return The track title View.
     */
    public TextView getTrackTitle() {
        return mTrackTitle;
    }
}
