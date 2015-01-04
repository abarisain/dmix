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

package org.a0z.mpd.item;

import android.annotation.TargetApi;
import android.media.MediaMetadata;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This is the Android backend {@code Music} item.
 *
 * @see org.a0z.mpd.item.AbstractMusic For generic {@code Music} code.
 */
public class Music extends AbstractMusic<Music> implements Parcelable {

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

    Music(final String album, final String albumArtist, final String artist, final String composer,
            final long date, final int disc, final String fullPath, final String genre,
            final String name, final int songId, final int songPos, final long time,
            final String title, final int totalTracks, final int track) {
        super(album, albumArtist, artist, composer, date, disc, fullPath, genre, name, songId,
                songPos, time, title, totalTracks, track);
    }

    /**
     * Protected constructor, used by the Android framework when reconstructing the object from a
     * {@link android.os.Parcel}<br />
     *
     * @param in The {@link android.os.Parcel} that contains our object
     */
    protected Music(final Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readString(), in.readLong(),
                in.readInt(), in.readString(), in.readString(), in.readString(), in.readInt(),
                in.readInt(), in.readLong(), in.readString(), in.readInt(), in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Adds metadata from the current track to a {@code MediaMetadata.Builder} object.
     *
     * @param metadata The constructed {@code MediaMetadata.Builder} object to add the current track
     *                 metadata to.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void getMediaMetadata(final MediaMetadata.Builder metadata) {
        final Album album = getAlbum();

        metadata.putLong(MediaMetadata.METADATA_KEY_DISC_NUMBER, (long) mDisc)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, mTime)
                .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, (long) mTotalTracks)
                .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, (long) mTrack)
                .putLong(MediaMetadata.METADATA_KEY_YEAR, album.getDate())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album.getName())
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, mAlbumArtistName)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, mArtistName)
                .putString(MediaMetadata.METADATA_KEY_COMPOSER, mComposerName)
                .putString(MediaMetadata.METADATA_KEY_DATE, Long.toString(mDate))
                .putString(MediaMetadata.METADATA_KEY_GENRE, mGenreName)
                .putString(MediaMetadata.METADATA_KEY_TITLE, mTitle);
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mAlbumName);
        dest.writeString(mAlbumArtistName);
        dest.writeString(mArtistName);
        dest.writeString(mComposerName);
        dest.writeLong(mDate);
        dest.writeInt(mDisc);
        dest.writeString(mFullPath);
        dest.writeString(mGenreName);
        dest.writeString(mName);
        dest.writeInt(mSongId);
        dest.writeInt(mSongPos);
        dest.writeLong(mTime);
        dest.writeString(mTitle);
        dest.writeInt(mTotalTracks);
        dest.writeInt(mTrack);
    }
}
