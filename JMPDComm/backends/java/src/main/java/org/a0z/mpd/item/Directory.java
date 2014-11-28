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

import java.util.Map;

/**
 * This is the Java backend {@code Directory} item.
 *
 * @see org.a0z.mpd.item.AbstractDirectory For generic {@code Directory} code.
 */
public class Directory extends AbstractDirectory {

    /**
     * Creates a new directory.
     *
     * @param parent   The parent directory to this directory.
     * @param filename The filename of this directory.
     */
    protected Directory(final Directory parent, final String filename) {
        super(parent, filename);
    }

    /**
     * The base constructor.
     *
     * @param parent           The parent directory to this directory.
     * @param filename         The filename of this directory.
     * @param name             The name of this directory.
     * @param directoryEntries Children directories to this directory.
     * @param fileEntries      Children files to this directory.
     * @param playlistEntries  Children playlists to this directory.
     */
    public Directory(final Directory parent, final String filename,
            final String name,
            final Map<String, Directory> directoryEntries,
            final Map<String, Music> fileEntries,
            final Map<String, PlaylistFile> playlistEntries) {
        super(parent, filename, name, directoryEntries, fileEntries, playlistEntries);
    }
}
