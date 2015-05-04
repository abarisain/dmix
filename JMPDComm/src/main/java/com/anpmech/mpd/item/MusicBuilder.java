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

import com.anpmech.mpd.Tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the builder for {@code Music} objects.
 */
public final class MusicBuilder {

    /**
     * The class log identifier.
     */
    private static final String TAG = "MusicBuilder";

    private MusicBuilder() {
        super();
    }

    /**
     * Builds a {@code Music} object from a media server response to a music listing command.
     *
     * @param response A music listing command response.
     * @return A Music object.
     */
    public static List<Music> buildMusicFromList(final List<String> response) {
        final Collection<int[]> ranges = Tools.getRanges(response);
        final List<Music> result = new ArrayList<>(ranges.size());

        for (final int[] range : ranges) {
            final String builder = sublistToString(response.subList(range[0], range[1]));
            result.add(new Music(builder));
        }

        return result;
    }

    private static String sublistToString(final Iterable<String> stringList) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String line : stringList) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }

        return stringBuilder.toString();
    }
}
