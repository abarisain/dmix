/*
 * Copyright (C) 2010-2017 The MPDroid Project
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

package com.namelessdev.mpdroid.playlists;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class M3UPlaylist implements Playlist {

    private static final String M3U_HEADER = "#EXTM3U";

    private static final String M3U_METADATA = "#EXTINF";

    private final String mUrl;

    M3UPlaylist(final String url) {
        this.mUrl = url;
    }

    @Override
    public List<PlaylistEntry> getEntries() {
        return Playlists.extractEntries(mUrl, new Playlists.PlaylistEntryExtractor() {
            @Override
            public List<PlaylistEntry> extractEntries(final BufferedReader reader) throws IOException {
                final List<PlaylistEntry> entries = new ArrayList<>();

                boolean isExtended = false;
                String name = null;
                Integer length = null;

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toUpperCase().equals(M3U_HEADER)) {
                        isExtended = true;
                        continue;
                    }
                    if (isExtended && line.toUpperCase().startsWith(M3U_METADATA)) {
                        final int colonPos = line.indexOf(':');
                        final int commaPos = line.indexOf(',');
                        if (colonPos > 0 && commaPos > 0) {
                            try {
                                length = Integer.valueOf(line.substring(colonPos + 1, commaPos));
                                name = line.substring(commaPos + 1);
                            } catch (final NumberFormatException ignore) {
                                length = null;
                                name = null;
                            }
                        }
                        continue;
                    }
                    entries.add(new PlaylistEntry(line, name, length));
                }

                return entries;
            }
        });
    }
}
