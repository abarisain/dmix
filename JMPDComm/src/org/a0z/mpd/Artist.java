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

package org.a0z.mpd;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class Artist extends Item implements Parcelable {
    public static String singleAlbumFormat = "%1 Album";
    public static String multipleAlbumsFormat = "%1 Albums";

    private final String name;
    private final String sort;
    // private final boolean isVa;
    private final int albumCount;

    public static final Parcelable.Creator<Artist> CREATOR =
            new Parcelable.Creator<Artist>() {
                public Artist createFromParcel(Parcel in) {
                    return new Artist(in);
                }

                public Artist[] newArray(int size) {
                    return new Artist[size];
                }
            };

    public Artist(Artist a) {
        this.name = a.name;
        this.albumCount = a.albumCount;
        this.sort = a.sort;
    }

    protected Artist(Parcel in) {
        this.name = in.readString();
        this.sort = in.readString();
        this.albumCount = in.readInt();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Artist) && ((Artist) o).name.equals(name);
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
    public String mainText() {
        return (name.equals("") ?
                MPD.getApplicationContext().getString(R.string.jmpdcomm_unknown_artist) :
                name);
    }

    @Override
    public boolean nameEquals(Item o) {
        return equals(o);
    }

    public String sortText() {
        return null == sort ? name == null ? "" : super.sortText() : sort;
    }

    @Override
    public String subText() {
        if (0 == albumCount) {
            return null;
        }

        return String
                .format(1 == albumCount ? singleAlbumFormat : multipleAlbumsFormat, albumCount);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.sort);
        dest.writeInt(this.albumCount);
    }

}
