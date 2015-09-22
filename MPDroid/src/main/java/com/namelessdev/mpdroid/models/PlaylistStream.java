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

package com.namelessdev.mpdroid.models;

import com.anpmech.mpd.item.Music;

public class PlaylistStream extends AbstractPlaylistMusic {

    public PlaylistStream(final Music music) {
        super(music);
    }

    @Override
    public String getPlayListMainLine() {
        final CharSequence fileExtension = getFileExtension();
        String mainline = getName();

        if (fileExtension != null && !isStream()) {
            final int extLength = fileExtension.length() + 1;
            final int mainLength = mainline.length();

            if (extLength < mainLength) {
                mainline = mainline.substring(mainLength - extLength);
            }
        }

        return mainline;
    }

    @Override
    public String getPlaylistSubLine() {
        return getFullPath();
    }

}
