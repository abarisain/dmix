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

import android.annotation.TargetApi;
import android.media.MediaMetadata;
import android.os.Build;
import android.os.Parcel;

/**
 * This is the Android backend {@code Music} item.
 *
 * @see AbstractMusic
 */
public class Music extends AbstractMusic<Music> {

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

    protected Music(final Music music) {
        super(music);
    }

    public Music(final String response) {
        super(response);
    }

    /**
     * Protected constructor, used by the Android framework when reconstructing the object from a
     * {@link Parcel}.
     *
     * @param in The {@link Parcel} that contains our object
     */
    protected Music(final Parcel in) {
        super(in.readString());
    }

    /**
     * Adds metadata from the current track to a {@code MediaMetadata.Builder} object.
     *
     * @param metadata The constructed {@code MediaMetadata.Builder} object to add the current
     *                 track metadata to.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void getMediaMetadata(final MediaMetadata.Builder metadata) {
        final Album album = getAlbum();

        metadata.putLong(MediaMetadata.METADATA_KEY_DISC_NUMBER, (long) getDisc())
                .putLong(MediaMetadata.METADATA_KEY_DURATION, getTime())
                .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, (long) getTotalTracks())
                .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, (long) getTrack())
                .putLong(MediaMetadata.METADATA_KEY_YEAR, album.getDate())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album.getName())
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, getAlbumArtistName())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, getArtistName())
                .putString(MediaMetadata.METADATA_KEY_COMPOSER, getComposerName())
                .putString(MediaMetadata.METADATA_KEY_DATE, Long.toString(getDate()))
                .putString(MediaMetadata.METADATA_KEY_GENRE, getGenreName())
                .putString(MediaMetadata.METADATA_KEY_TITLE, getTitle());
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mResponse);
    }
}
