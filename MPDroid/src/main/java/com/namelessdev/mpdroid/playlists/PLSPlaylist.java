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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PLSPlaylist implements Playlist {

    private static final String NUMBER_OF_ENTRIES = "NUMBEROFENTRIES";

    private static final String FILE = "FILE";

    private static final String TITLE = "TITLE";

    private static final String LENGTH = "LENGTH";

    private final String mUrl;

    PLSPlaylist(final String url) {
        this.mUrl = url;
    }

    @Override
    public List<PlaylistEntry> getEntries() {
        return Playlists.extractEntries(mUrl, new Playlists.PlaylistEntryExtractor() {
            @Override
            public List<PlaylistEntry> extractEntries(final BufferedReader reader) throws IOException {
                final List<PlaylistEntry> entries = new ArrayList<>();

                final Map<String, String> content = new HashMap<>();

                String line;
                while ((line = reader.readLine()) != null) {
                    final int divideIndex = line.indexOf('=');
                    if (divideIndex < 0) {
                        continue;
                    }

                    final String key = line.substring(0, divideIndex);
                    final String value = line.substring(divideIndex + 1);

                    content.put(key.trim().toUpperCase(), value.trim());
                }

                for (int i = 1; i <= parseInteger(content.get(NUMBER_OF_ENTRIES)); i++) {
                    final String file = content.get(FILE + i);
                    if (file == null) {
                        continue;
                    }
                    entries.add(new PlaylistEntry(file, content.get(TITLE + i),
                            parseInteger(content.get(LENGTH + i))));
                }

                return entries;
            }

            private int parseInteger(final String value) {
                if (value == null) {
                    return 0;
                }
                try {
                    return Integer.valueOf(value);
                } catch (final NumberFormatException ignore) {
                    return 0;
                }
            }
        });
    }
}
