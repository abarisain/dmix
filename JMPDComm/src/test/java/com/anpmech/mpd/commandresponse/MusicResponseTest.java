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

package com.anpmech.mpd.commandresponse;

import com.anpmech.mpd.TestTools;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.CommandResultCreator;
import com.anpmech.mpd.item.Music;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests the {@link MusicResponse} class.
 */
public class MusicResponseTest extends ObjectResponseTest<Music, MusicResponse> {

    /**
     * Sole constructor.
     */
    public MusicResponseTest() {
        super();
    }

    /**
     * Builds a {@code Music} object from a media server response to a music listing command.
     *
     * @param response A music listing command response.
     * @return A Music object.
     */
    private static List<Music> buildMusicFromList(final List<String> response) {
        final Collection<int[]> ranges = getRanges(response);
        final List<Music> result = new ArrayList<>(ranges.size());

        for (final int[] range : ranges) {
            final String builder = sublistToString(response.subList(range[0], range[1]));
            result.add(new Music(builder));
        }

        return result;
    }

    /**
     * Gets a beginning and an end range of sub-server responses.
     *
     * @param response The server response to parse.
     * @return A two int array. The first int is the beginning range which matched the key
     * parameter. The second number is one int beyond the end of the range (for
     * {@link List#subList(int, int)} compatibility). If no range is found, an empty list will
     * be returned.
     */
    private static Collection<int[]> getRanges(final Collection<String> response) {
        final int responseSize = response.size();
        /** Initialize the range after the capacity is known. */
        Collection<int[]> ranges = null;
        CharSequence key = null;
        int beginIndex = 0;
        int iterator = 0;

        for (final String line : response) {
            final int index = line.indexOf(':');
            final CharSequence formatted;

            if (index == -1) {
                formatted = line;
            } else {
                formatted = line.subSequence(0, index);
            }

            if (iterator == 0) {
                key = formatted;
            }

            if (key.equals(formatted) && iterator != beginIndex) {
                if (ranges == null) {
                    final int capacity = responseSize / (iterator - beginIndex);
                    ranges = new ArrayList<>(capacity);
                }
                ranges.add(new int[]{beginIndex, iterator});
                beginIndex = iterator;
            }

            iterator++;
        }

        if (responseSize == 0) {
            ranges = Collections.emptyList();
        } else if (ranges == null) {
            ranges = Collections.singletonList(new int[]{beginIndex, responseSize});
        } else {
            ranges.add(new int[]{beginIndex, responseSize});
        }

        return ranges;
    }

    /**
     * This is used to test the depreciated method against the new Iterated method.
     *
     * @param stringList The list of strings to turn into a string.
     * @return The collection of strings separated by a new line.
     */
    private static String sublistToString(final Iterable<String> stringList) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String line : stringList) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }

        return stringBuilder.toString();
    }

    /**
     * This returns a empty ObjectResponse for the ObjectResponse subclass.
     *
     * @return A empty ObjectResponse.
     */
    @Override
    protected MusicResponse getEmptyResponse() {
        return new MusicResponse();
    }

    /**
     * This returns a path to a test sample file to construct a CommandResult from.
     *
     * @return A path to a test sample file.
     */
    @Override
    protected String getResponsePath() {
        return TestTools.FILE_MULTIPLE_PLAYLISTINFO;
    }

    /**
     * This method instantiates the ObjectResponse type from the {@code CommandResult} parameter.
     *
     * @param result The {@code CommandResult} to create the ObjectResponse type from.
     * @return A ObjectResponse subclass type.
     */
    @Override
    protected MusicResponse instantiate(final CommandResult result) {
        return new MusicResponse(result);
    }

    /**
     * This method tests iteration consistency of a playlistinfo result.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void musicIteratorPlaylistinfoConsistencyTest() throws IOException {
        final CommandResponse response = new CommandResponse(getResult());
        final List<Music> musicList = buildMusicFromList(response.getList());
        final List<Music> musicResponseList = instantiate(response).getList();

        assertEquals("Music list fails to be consistent with last known good list generator.",
                musicList, musicResponseList);
    }

    /**
     * This method tests iteration consistency of a lsinfo {@link MusicResponse} result against a
     * known value.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void musicIteratorRootLsinfoConsistencyTest() throws IOException {
        final CommandResult result = CommandResultCreator.generate(
                TestTools.FILE_ROOT_LSINFO);
        final List<Music> list = instantiate(result).getList();

        /**
         * This cannot be tested against a buildMusicFromList() result as it only delimits by
         * the incoming delimiter.
         */
        assertEquals("Failed to find only one music element in " + TestTools.FILE_ROOT_LSINFO + '.',
                1L, (long) list.size());
    }

    /**
     * This method tests reverse iteration over a playlistinfo {@link MusicResponse} result,
     * testing consistency of {@link Music#getPos()} against the iteration position.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void reverseMusicIteratorConsistencyTest() throws IOException {
        final ListIterator<Music> iterator = instantiate(getResult()).reverseListIterator();
        int position = Integer.MIN_VALUE;
        boolean testPassed = false;

        while (iterator.hasPrevious()) {
            final Music item = iterator.previous();

            if (position == Integer.MIN_VALUE) {
                position = item.getPos();
            } else {
                final int itemPos = item.getPos();

                position--;
                if (itemPos == 1) {
                    testPassed = true;
                } else if (itemPos != position) {
                    break;
                }
            }
        }

        assertTrue("Music iterator could not be successfully reversed", testPassed);
    }
}
