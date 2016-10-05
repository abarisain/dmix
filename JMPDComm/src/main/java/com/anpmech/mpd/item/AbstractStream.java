/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2016 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.anpmech.mpd.item;

import com.anpmech.mpd.ResponseObject;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * This class creates a Stream, a derivative of {@link PlaylistFile}.
 */
abstract class AbstractStream extends ResponseItem<Stream> {

    /**
     * The playlist name to retrieve the special playlist for streams.
     */
    public static final String PLAYLIST_NAME = "[Radio Streams]";

    /**
     * The class log identifier.
     */
    protected static final String TAG = "Stream";

    /**
     * This constructor is used to create a new Stream item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     */
    AbstractStream(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * This method replaces {@link URI#getFragment() URI Fragment} in the url parameter with the
     * name.
     *
     * @param url  The URL to add the name parameter to.
     * @param name The name to use as the new url fragment.
     * @return The new URL as a string.
     */
    public static String addStreamName(final String url, final String name) {
        final StringBuilder streamName;

        if (name == null) {
            streamName = new StringBuilder(url.length() + 3);
        } else {
            streamName = new StringBuilder(url.length() + name.length() + 3);
        }
        streamName.append(url);

        if (name != null && !name.isEmpty()) {
            String path = null;

            try {
                path = new URL(url).getPath();
            } catch (final MalformedURLException ignored) {
            }

            if (path == null || path.isEmpty()) {
                streamName.append('/');
            }
            streamName.append('#');
            streamName.append(name);
        }

        return streamName.toString();
    }

    /**
     * This method returns the URI fragment of the URL from {@link #getUrl()}.
     *
     * @return The URI fragment from the {@link #getUrl() URL}.
     */
    @Override
    public String getName() {
        return getURIFragment(AbstractMusic.RESPONSE_FILE);
    }

    /**
     * This method returns the URL of this Stream.
     *
     * @return The URL of this Stream.
     */
    public String getUrl() {
        return findValue(AbstractMusic.RESPONSE_FILE);
    }
}
