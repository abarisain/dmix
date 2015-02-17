/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a playlist in the database
 */
abstract class AbstractPlaylistFile<T extends PlaylistFile> extends Item<PlaylistFile>
        implements FilesystemTreeEntry {

    protected static final String TAG = "PlaylistFile";

    private static final Pattern PLAYLIST_FILE_REGEXP = Pattern.compile("^.*/(.+)\\.(\\w+)$");

    protected final String mFullPath;

    protected AbstractPlaylistFile(final String path) {
        super();
        mFullPath = path;
    }

    /**
     * Compares a PlaylistFile object with a general contract of comparison that is reflexive,
     * symmetric and transitive.
     *
     * @param o The object to compare this instance with.
     * @return True if the objects are equal with regard to te general contract, false otherwise.
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
            /** This has to be the same due to the class check above. */
            //noinspection unchecked
            final T directory = (T) o;

            isEqual = Boolean.valueOf(mFullPath.equals(directory.mFullPath));
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    @Override
    public String getFullPath() {
        return mFullPath;
    }

    @Override
    public String getName() {
        String result = "";

        if (mFullPath != null) {
            final Matcher matcher = PLAYLIST_FILE_REGEXP.matcher(mFullPath);
            if (matcher.matches()) {
                result = matcher.replaceAll("[$2] $1.$2");
            } else {
                result = mFullPath;
            }
        }
        return result;
    }

    /**
     * Returns an integer hash code for this PlaylistFile. By contract, any two objects for which
     * {@link #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * @return This PlaylistFile hash code.
     * @see Object#equals(Object)
     */
    @Override
    public int hashCode() {
        return mFullPath.hashCode();
    }
}
