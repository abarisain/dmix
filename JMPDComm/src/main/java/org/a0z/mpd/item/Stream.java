/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
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

package org.a0z.mpd.item;

import org.a0z.mpd.Tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class Stream extends Item {

    private final String mName;

    private final String mUrl;

    private int mPos;

    public Stream(final String name, final String url, final int pos) {
        super();

        mName = name;
        mUrl = url;
        mPos = pos;
    }

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
     * Compares an Artist object with a general contract of
     * comparison that is reflexive, symmetric and transitive.
     *
     * @param o The object to compare this instance with.
     * @return True if the objects are equal with regard to te general contract, false otherwise.
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            final Stream stream = (Stream) o;

            if (Tools.isNotEqual(mName, stream.mName)) {
                isEqual = Boolean.FALSE;
            }

            if (Tools.isNotEqual(mUrl, stream.mUrl)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    @Override
    public String getName() {
        return mName;
    }

    public int getPos() {
        return mPos;
    }

    public String getUrl() {
        return mUrl;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mName, mUrl});
    }

    public void setPos(final int pos) {
        mPos = pos;
    }
}
