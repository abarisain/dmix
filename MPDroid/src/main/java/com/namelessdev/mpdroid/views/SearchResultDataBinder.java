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
import com.namelessdev.mpdroid.adapters.SeparatedListDataBinder;

import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class SearchResultDataBinder implements SeparatedListDataBinder {

    /**
     * Join not empty strings
     *
     * @param parts : parts to join
     * @return the formatted result
     */
    public static String join(final String... parts) {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (part != null && !part.isEmpty()) {
                result.append(part);
                if (i < parts.length - 1) {
                    result.append(BaseDataBinder.SEPARATOR);
                }
            }
        }
        return result.toString();
    }

    @Override
    public boolean isEnabled(final int position, final List<?> items, final Object item) {
        return true;
    }

    @Override
    public void onDataBind(final Context context, final View targetView, final List<?> items,
            final Object item, final int position) {
        final TextView text1 = (TextView) targetView.findViewById(R.id.line1);
        final TextView text2 = (TextView) targetView.findViewById(R.id.line2);
        String formattedResult1 = null;
        String formattedResult2 = null;

        if (item instanceof Music) {
            final Music music;
            music = (Music) item;
            formattedResult1 = music.getTitle();
            formattedResult2 = join(music.getAlbum(), music.getArtist());
        } else if (item instanceof Artist) {
            formattedResult1 = ((Item) item).mainText();
        } else if (item instanceof Album) {
            final Album album = (Album) item;
            final Artist artist = album.getArtist();

            formattedResult1 = album.mainText();

            if (artist != null) {
                formattedResult2 = artist.mainText();
            }
        }

        if (formattedResult2 == null) {
            text2.setVisibility(View.GONE);
        } else {
            text2.setVisibility(View.VISIBLE);
        }

        text1.setText(formattedResult1);
        text2.setText(formattedResult2);
    }

}
