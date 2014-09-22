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

package com.namelessdev.mpdroid.models;

import org.a0z.mpd.item.Music;

public abstract class AbstractPlaylistMusic extends Music {

    private int currentSongIconRefID;

    private boolean forceCoverRefresh = false;

    protected AbstractPlaylistMusic(String album, String artist, String albumartist,
            String fullpath, int disc, long date, String genre, long time, String title,
            int totalTracks, int track, int songId, int pos, String name) {
        super(album, artist, albumartist, fullpath, disc, date, genre, time, title,
                totalTracks, track, songId, pos, name);
    }

    public int getCurrentSongIconRefID() {
        return currentSongIconRefID;
    }

    public abstract String getPlayListMainLine();

    public abstract String getPlaylistSubLine();

    public boolean isForceCoverRefresh() {
        return forceCoverRefresh;
    }

    public void setCurrentSongIconRefID(int currentSongIconRefID) {
        this.currentSongIconRefID = currentSongIconRefID;
    }

    public void setForceCoverRefresh(boolean forceCoverRefresh) {
        this.forceCoverRefresh = forceCoverRefresh;
    }
}
