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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This is the Android backend {@code Album} item.
 *
 * @see org.a0z.mpd.item.AbstractAlbum For generic {@code Album} code.
 */
public class Album extends AbstractAlbum implements Parcelable {

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(final Parcel source) {
            return new Album(source);
        }

        @Override
        public Album[] newArray(final int size) {
            return new Album[size];
        }
    };

    public Album(final Album otherAlbum) {
        super(otherAlbum);
    }

    public Album(final Album otherAlbum, final Artist artist, final boolean hasAlbumArtist) {
        super(otherAlbum, artist, hasAlbumArtist);
    }

    public Album(final String name, final Artist artist) {
        super(name, artist, false, 0L, 0L, 0L, null);
    }

    public Album(final String name, final Artist artist, final boolean hasAlbumArtist) {
        super(name, artist, hasAlbumArtist, 0L, 0L, 0L, null);
    }

    public Album(final String name, final Artist artist, final boolean hasAlbumArtist,
            final long songCount, final long duration,
            final long year, final String path) {
        super(name, artist, hasAlbumArtist, songCount, duration, year, path);
    }

    protected Album(final Parcel in) {
        super(in.readString(), /** name */
                new Artist(in.readString()), /** artist */
                in.readInt() > 0, /** hasAlbumArtist */
                in.readLong(), /** songCount */
                in.readLong(), /** duration */
                in.readLong(), /** year */
                in.readString()); /** path */
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        final String artistName;
        final Artist artist = getArtist();
        final int hasAlbumArtist;

        if (artist == null) {
            artistName = "";
        } else {
            artistName = artist.getName();
        }

        if (hasAlbumArtist()) {
            hasAlbumArtist = 1;
        } else {
            hasAlbumArtist = 0;
        }

        dest.writeString(getName());
        dest.writeString(artistName);
        dest.writeInt(hasAlbumArtist);
        dest.writeLong(getSongCount());
        dest.writeLong(getDuration());
        dest.writeLong(getYear());
        dest.writeString(getPath());
    }
}
