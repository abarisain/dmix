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

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * This class contains general tools used for testing.
 */
public enum TestTools {
    ;

    /**
     * This file contains a single playlistinfo entry.
     */
    public static final String FILE_SINGULAR_PLAYLISTINFO
            = "mpd-protocol/playlistinfo/singular_playlistinfo.txt";

    /**
     * This file contains a single track lsinfo file response.
     */
    public static final String FILE_SINGULAR_TRACK_FILE
            = "mpd-protocol/lsinfo/singular_track_file.txt";

    /**
     * This file contains a empty file, to test the a empty response.
     */
    public static final String FILE_SINGULAR_TRACK_MINIMAL = "mpd-protocol/empty_response.txt";

    /**
     * This file contains a single stream lsinfo entry.
     */
    public static final String FILE_SINGULAR_TRACK_STREAM
            = "mpd-protocol/lsinfo/singular_track_stream.txt";

    /**
     * This array contains all resource files.
     */
    public static final String[] TEST_FILE_PATHS = {
            FILE_SINGULAR_TRACK_FILE,
            FILE_SINGULAR_TRACK_MINIMAL,
            FILE_SINGULAR_TRACK_STREAM,
            FILE_SINGULAR_PLAYLISTINFO
    };

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
}
