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

import org.a0z.mpd.MPD;

import java.util.Locale;

public class Artist extends Item {

    private String name;

    private String sort;

    private int albumCount;

    public Artist(Artist a) {
        this(a.name, a.sort, a.albumCount);
    }

    public Artist(String name) {
        this(name, 0);
    }

    public Artist(String name, int albumCount) {
        this.name = name;
        if (null != name && name.toLowerCase(Locale.getDefault()).startsWith("the ")) {
            sort = name.substring(4);
        } else {
            sort = null;
        }
        this.albumCount = albumCount;
    }

    protected Artist(final String name, final String sort, final int albumCount) {
        super();

        this.name = name;
        this.sort = sort;
        this.albumCount = albumCount;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Artist) && ((Artist) o).name.equals(name);
    }

    protected int getAlbumCount() {
        return albumCount;
    }

    public String getName() {
        return name;
    }

    public String info() {
        return getName();
    }

    /*
     * text for display Item.toString() returns mainText()
     */
    @Override
    public String mainText() {
        final String result;

        if(name.isEmpty()) {
            result = MPD.getUnknownArtist();
        } else {
            result = name;
        }

        return result;
    }

    @Override
    public boolean nameEquals(Item o) {
        return equals(o);
    }

    public String sortText() {
        return null == sort ? name == null ? "" : super.sortText() : sort;
    }
}
