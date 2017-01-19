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

import java.io.Serializable;

public class PlaylistEntry implements Serializable {

    private final String mUrl;

    private String mName;

    private Integer mLength;

    PlaylistEntry(final String url) {
        this.mUrl = url;
    }

    PlaylistEntry(final String url, final String name, final Integer length) {
        this(url);
        this.mName = name;
        this.mLength = length;
    }


    public String getUrl() {
        return mUrl;
    }

    void setName(final String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    void setLength(final Integer length) {
        this.mLength = length;
    }

    public Integer getLength() {
        return mLength;
    }
}
