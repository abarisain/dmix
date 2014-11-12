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

import org.a0z.mpd.Tools;
import org.a0z.mpd.item.Music;

public class PlaylistStream extends AbstractPlaylistMusic {

    public PlaylistStream(final Music music) {
        super(music);
    }

    @Override
    public String getPlayListMainLine() {
        final String name = getName();

        return name.replace('.' + Tools.getExtension(name), "");
    }

    @Override
    public String getPlaylistSubLine() {
        return getFullPath();
    }

}
