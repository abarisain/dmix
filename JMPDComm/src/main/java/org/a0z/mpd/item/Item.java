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

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Item implements Comparable<Item> {

    public static final Collator DEFAULT_COLLATOR = Collator.getInstance(Locale.getDefault());

    /*
     * Merge item lists, for example received by album artist and artist
     * requests. Sorted lists required!
     */
    public static <T extends Item> List<T> merged(final List<T> albumArtists,
            final List<T> artists) {
        int jStart = albumArtists.size() - 1;
        for (int i = artists.size() - 1; i >= 0; i--) { // artists
            for (int j = jStart; j >= 0; j--) { // album artists
                if (albumArtists.get(j).doesNameExist(artists.get(i))) {
                    jStart = j;
                    artists.remove(i);
                    break;
                }
            }
        }
        artists.addAll(albumArtists);
        Collections.sort(artists);
        return artists;
    }

    @Override
    public int compareTo(final Item another) {
        final int comparisonResult;

        // sort "" behind everything else
        if (sortText() != null && sortText().isEmpty()) {
            if (another.sortText() != null && another.sortText().isEmpty()) {
                comparisonResult = 0;
            } else {
                comparisonResult = 1;
            }
        } else if (another.sortText() != null && another.sortText().isEmpty()) {
            comparisonResult = -1;
        } else {
            comparisonResult = DEFAULT_COLLATOR.compare(sortText(), another.sortText());
        }

        return comparisonResult;
    }

    public boolean doesNameExist(final Item o) {
        return getName().equals(o.getName());
    }

    public abstract String getName();

    public boolean isUnknown() {
        return getName().isEmpty();
    }

    public String mainText() {
        return getName();
    }

    public String sortText() {
        return getName().toLowerCase(Locale.getDefault());
    }

    @Override
    public String toString() {
        return mainText();
    }

}
