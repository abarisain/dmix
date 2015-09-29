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

import com.anpmech.mpd.Log;
import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.exception.InvalidResponseException;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class serves as a abstraction base for database and filesystem MPD protocol entries.
 */
public abstract class AbstractEntry<T extends AbstractEntry<T>> extends ResponseItem<T>
        implements FilesystemTreeEntry {

    /**
     * The media server response key returned for a last modified value.
     */
    public static final String RESPONSE_LAST_MODIFIED = "Last-Modified";

    /**
     * This is a static parser for the last modified time.
     */
    private static final DateFormat DATE_FORMAT;

    /**
     * The media server response key returned for the file size of an entry.
     *
     * <p>This is currently only used on non-music listings.</p>
     */
    private static final String RESPONSE_SIZE = "size";

    /**
     * The class log identifier.
     */
    private static final String TAG = "AbstractEntry";

    static {
        /**
         * This is formatted for ISO8601.
         */
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * This object is used to create a new AbstractEntry with a {@link ResponseObject}.
     *
     * @param object The prepared {@link ResponseObject}.
     */
    protected AbstractEntry(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * This method returns the full path value for a Directory response key.
     *
     * @return A full path value for a Directory response key.
     */
    protected String getDirectoryFullPath() {
        return findValue(AbstractDirectoryBase.RESPONSE_DIRECTORY);
    }

    /**
     * This returns a representation of the source of the media, relative to the media server.
     *
     * <p>This can be a filename, a URL, etc. If this item's filename is a URL, the URI fragment is
     * removed.</p>
     *
     * @return The filename of this item without a URI fragment.
     */
    @Override
    @NotNull
    public String getFullPath() {
        String fullPath = getMusicFullPath();

        if (fullPath == null) {
            fullPath = getPlaylistFileFullPath();
        }

        if (fullPath == null) {
            fullPath = getDirectoryFullPath();
        }

        if (fullPath == null) {
            throw new InvalidResponseException(pathNotFoundError());
        }

        return fullPath;
    }

    /**
     * This method returns the last modified time for this entry in Unix time.
     *
     * <p>The Last-Modified response value is expected to be given in ISO8601.</p>
     *
     * @return The last modified time for this entry in Unix time.
     */
    @Override
    public long getLastModified() {
        final String iso8601LastModified = findValue(RESPONSE_LAST_MODIFIED);
        long result = Long.MIN_VALUE;

        if (iso8601LastModified != null) {
            try {
                /**
                 * Contention isn't an issue here. Parse happens easily in nanos.
                 */
                //noinspection SynchronizationOnStaticField
                synchronized (DATE_FORMAT) {
                    result = DATE_FORMAT.parse(iso8601LastModified).getTime();
                }
            } catch (final ParseException e) {
                Log.error(TAG, "Incorrect time format given by MPD implementation.", e);
            }
        }

        return result;
    }

    /**
     * This returns a representation of the source of the media, relative to the media server.
     *
     * <p>This can be a filename, a URL, etc. If this item's filename is a URL, the URI fragment is
     * removed.</p>
     *
     * @return The filename of this item without a URI fragment.
     */
    protected String getMusicFullPath() {
        String filename = findValue(AbstractMusic.RESPONSE_FILE);

        if (filename != null && filename.contains("://")) {
            final int pos = filename.indexOf('#');

            if (pos != -1) {
                filename = filename.substring(0, pos);
            }
        }

        return filename;
    }

    /**
     * Gets a generic representation of the Item's name.
     *
     * @return A generic representation of the Item's name.
     */
    @Override
    public String getName() {
        return getFullPath();
    }

    /**
     * This method returns the full path value for a PlaylistFile response key.
     *
     * @return A full path value for a PlaylistFile response key.
     */
    protected String getPlaylistFileFullPath() {
        return findValue(AbstractPlaylistFile.RESPONSE_PLAYLIST_FILE);
    }

    /**
     * This method assembles a string for a error message for a fullPath not being found in the
     * response or by name.
     *
     * @return A error message for a fullPath not being found in the  response or by name.
     */
    protected String pathNotFoundError() {
        return "Full path which is required by this class, not found: '" +
                mResponseObject.getResponse() + '\'';
    }

    /**
     * This returns the size a MPD entry file.
     *
     * <p><b>This is only available with some MPD command responses.</b></p>
     *
     * @return The size of a MPD entry file, {@link Integer#MIN_VALUE} if it doesn't exist in this
     * response.
     */
    @Override
    public long size() {
        return Tools.parseLong(findValue(RESPONSE_SIZE));
    }
}