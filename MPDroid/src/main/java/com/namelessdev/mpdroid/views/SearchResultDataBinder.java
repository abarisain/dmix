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
import org.a0z.mpd.item.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class SearchResultDataBinder implements SeparatedListDataBinder {

    public static final String SEPARATOR = " - ";

    /**
     * Join not empty strings
     *
     * @param parts : parts to join
     * @return the formatted result
     */
    public static String join(String... parts) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part != null && part.length() > 0) {
                result.append(part);
                if (SEPARATOR != null && i < parts.length - 1) {
                    result.append(SEPARATOR);
                }
            }
        }
        return result.toString();
    }

    public boolean isEnabled(int position, List<?> items, Object item) {
        return true;
    }

    public void onDataBind(Context context, View targetView, List<?> items,
            Object item, int position) {
        final TextView text1 = (TextView) targetView.findViewById(R.id.line1);
        final TextView text2 = (TextView) targetView.findViewById(R.id.line2);
        String formattedResult1 = "";
        String formattedResult2 = null;

        if (item instanceof Music) {
            Music music;
            music = (Music) item;
            formattedResult1 = music.getTitle();
            formattedResult2 = join(music.getAlbum(), music.getArtist());
        } else if (item instanceof Artist) {
            formattedResult1 = (((Artist) item).mainText());
        } else if (item instanceof Album) {
            Album album;
            album = (Album) item;
            formattedResult1 = album.mainText();
            formattedResult2 = album.getArtist().mainText();
        }
        text1.setText(formattedResult1);
        text2.setVisibility(formattedResult2 != null ? View.VISIBLE : View.GONE);
        text2.setText(formattedResult2);
    }

}
