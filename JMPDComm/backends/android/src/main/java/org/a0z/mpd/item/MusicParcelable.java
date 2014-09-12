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

/** A class to put org.a0z.mpd.item.Music in a Parcelable wrapper. */
public class MusicParcelable extends Music implements Parcelable {

    public static final Creator<Music> CREATOR = new Creator<Music>() {
        @Override
        public Music createFromParcel(final Parcel source) {
            return new MusicParcelable(source);
        }

        @Override
        public Music[] newArray(final int size) {
            return new MusicParcelable[size];
        }
    };

    /**
     * Public constructor, used to encapsulate a {@link org.a0z.mpd.item.Music} into this {@link
     * android.os.Parcelable}
     *
     * @param music The target {@link org.a0z.mpd.item.Music} object
     */
    public MusicParcelable(final Music music) {
        super(music);
    }

    /**
     * Protected constructor, used by the Android framework when reconstructing the object from a
     * {@link android.os.Parcel}<br />
     * This constructor will instantiate the object through the default {@link
     * org.a0z.mpd.item.Music#Music(String, String, String, String, int, long, long,
     * org.a0z.mpd.item.Directory, String, int, int, int, int, String)} constructor.
     *
     * @param in The {@link android.os.Parcel} that contains our object
     */
    protected MusicParcelable(final Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readString(), in.readInt(),
                in.readLong(), in.readLong(), null, in.readString(), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(), in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(getAlbum());
        dest.writeString(getArtist());
        dest.writeString(getAlbumArtist());
        dest.writeString(getFullpath());
        dest.writeInt(getDisc());
        dest.writeLong(getDate());
        dest.writeLong(getTime());
        //dest.writeString(getParent()); // TODO: is it used?
        dest.writeString(getTitle());
        dest.writeInt(getTotalTracks());
        dest.writeInt(getTrack());
        dest.writeInt(getSongId());
        dest.writeInt(getPos());
        dest.writeString(getName());
    }
}
