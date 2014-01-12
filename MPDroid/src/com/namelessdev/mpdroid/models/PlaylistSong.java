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

package com.namelessdev.mpdroid.models;

import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;

import org.a0z.mpd.Music;

import java.util.ArrayList;
import java.util.List;

public class PlaylistSong extends AbstractPlaylistMusic {

    public PlaylistSong(Music m) {
        super(m.getAlbum(), m.getArtist(), m.getAlbumArtist(), m.getFullpath(), m.getDisc(), m
                .getDate(), m.getTime(), m.getParentDirectory(), m.getTitle(), m.getTotalTracks(),
                m.getTrack(), m.getSongId(), m.getPos(), m.getName());
    }

    public String getPlayListMainLine() {
        return getTitle();
    }

    public String getPlaylistSubLine() {
        List<String> sublineTexts = new ArrayList<String>();
        if (!isEmpty(getArtist())) {
            sublineTexts.add(getArtist());
        }
        if (!isEmpty(getAlbum())) {
            sublineTexts.add(getAlbum());
        }
        return join(" - ", sublineTexts);
    }
}
