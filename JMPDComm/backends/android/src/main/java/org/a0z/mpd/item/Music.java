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
 * This is the Android backend {@code Music} item.
 *
 * @see org.a0z.mpd.item.AbstractMusic For generic {@code Music} code.
 */
public class Music extends AbstractMusic implements Parcelable {

    public static final Creator<Music> CREATOR = new Creator<Music>() {
        @Override
        public Music createFromParcel(final Parcel source) {
            return new Music(source);
        }

        @Override
        public Music[] newArray(final int size) {
            return new Music[size];
        }
    };

    public Music() {
        super();
    }

    protected Music(final Music music) {
        super(music);
    }

    Music(final String album, final String artist, final String albumArtist,
            final String composer, final String fullPath, final int disc, final long date,
            final String genre, final long time, final String title, final int totalTracks,
            final int track, final int songId, final int songPos, final String name) {
        super(album, artist, albumArtist, composer, fullPath, disc, date, genre, time, title,
                totalTracks, track, songId, songPos, name);
    }

    /**
     * Protected constructor, used by the Android framework when reconstructing the object from a
     * {@link android.os.Parcel}<br />
     *
     * @param in The {@link android.os.Parcel} that contains our object
     */
    protected Music(final Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readString(), in.readString(),
                in.readInt(), in.readLong(), in.readString(), in.readLong(), in.readString(),
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mAlbum);
        dest.writeString(mArtist);
        dest.writeString(mAlbumArtist);
        dest.writeString(mComposer);
        dest.writeString(mFullPath);
        dest.writeInt(mDisc);
        dest.writeLong(mDate);
        dest.writeString(mGenre);
        dest.writeLong(mTime);
        dest.writeString(mTitle);
        dest.writeInt(mTotalTracks);
        dest.writeInt(mTrack);
        dest.writeInt(mSongId);
        dest.writeInt(mSongPos);
        dest.writeString(mName);
    }
}
