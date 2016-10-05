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

package com.namelessdev.mpdroid.models;

import com.anpmech.mpd.Tools;
import com.anpmech.mpd.item.Music;

import java.util.ArrayList;
import java.util.Collection;

import static android.text.TextUtils.join;

public class PlaylistSong extends AbstractPlaylistMusic {

    public PlaylistSong(final Music music) {
        super(music);
    }

    @Override
    public String getPlayListMainLine() {
        return getTitle();
    }

    @Override
    public String getPlaylistSubLine() {
        final Collection<String> subLineTexts = new ArrayList<>();
        if (!Tools.isEmpty(getArtistName())) {
            subLineTexts.add(getArtistName());
        }
        if (!Tools.isEmpty(getAlbumName())) {
            subLineTexts.add(getAlbumName());
        }
        return join(" - ", subLineTexts);
    }
}
