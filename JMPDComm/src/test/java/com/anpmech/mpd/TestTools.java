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

package com.anpmech.mpd;/*
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

import com.anpmech.mpd.commandresponse.ObjectResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

/**
 * This class contains general tools used for testing.
 */
public enum TestTools {
    ;

    /**
     * This is the filename of a file which contains a "genre" command result.
     */
    public static final String FILE_GENRE_LIST = "mpd-protocol/genre-list.txt";

    public static final String FILE_LISTFILES = "mpd-protocol/listfiles.txt";

    /**
     * This is the filename of a file which contains multiple playlistinfo entries.
     */
    public static final String FILE_MULTIPLE_PLAYLISTINFO
            = "mpd-protocol/playlistinfo/multiple_playlistinfo.txt";

    /**
     * This file contains a outputs response.
     */
    public static final String FILE_OUTPUTS = "mpd-protocol/outputs.txt";

    /**
     * This file contains a root lsinfo entry with the varying directory entry types.
     */
    public static final String FILE_ROOT_LSINFO = "mpd-protocol/lsinfo/root_lsinfo.txt";

    /**
     * This file contains a separated command result.
     *
     * <p>This result was created with the following commands:
     * <li>
     * command_list_ok_begin
     * password (password omitted)
     * list genres
     * list artist
     * command_list_end
     * </li>
     * </p>
     */
    public static final String FILE_SEPARATED_COMMAND_RESPONSE
            = "mpd-protocol/separated-responses/separated-password-listgenre-listartist.txt";

    /**
     * This file includes a few empty results.
     */
    public static final String FILE_SEPARATED_EMPTY_SEPARATED_RESPONSES
            = "mpd-protocol/separated-responses/empty_results.txt";

    /**
     * This directory contains all playlistinfo directory tests.
     */
    public static final String FILE_SINGULAR_PLAYLISTINFO
            = "mpd-protocol/playlistinfo/singular_playlistinfo.txt";

    /**
     * This file contains a single track lsinfo file response.
     */
    public static final String FILE_SINGULAR_TRACK_FILE
            = "mpd-protocol/lsinfo/singular_track_file.txt";

    /**
     * This file contains a single stream lsinfo entry.
     */
    public static final String FILE_SINGULAR_TRACK_STREAM
            = "mpd-protocol/lsinfo/singular_track_stream.txt";

    public static final String FILE_STREAM_PLAYLISTINFO
            = "mpd-protocol/playlistinfo/streams_playlistinfo.txt";

    public static String getMatchMsg(final String message, final String filePath) {
        return message + " failed to match for filepath: " + filePath + '.';
    }

    /**
     * Takes a file path and reads the file into a {@link String}.
     *
     * @param pathname The path name of the file to read into the {@code String}.
     * @return The resulting {@code String}.
     * @throws IOException If there was a problem reading from the file.
     */
    public static String readFile(final String pathname) throws IOException {
        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        final File file = new File(loader.getResource(pathname).getFile());
        final StringBuilder fileContents = new StringBuilder((int) file.length());
        final String lineSeparator = System.getProperty("line.separator");

        try (final Scanner scanner = new Scanner(file, "UTF-8")) {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine());
                fileContents.append(lineSeparator);
            }
        }

        return fileContents.toString();
    }

    public static <T> List<T> reverseList(final ObjectResponse<T> response) {
        final ListIterator<T> iterator = response.reverseListIterator();
        final List<T> list = new ArrayList<>();

        while (iterator.hasPrevious()) {
            list.add(iterator.previous());
        }

        return list;
    }
}
