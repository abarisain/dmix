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

package com.anpmech.mpd.item;

import com.anpmech.mpd.ResponseObject;

import org.jetbrains.annotations.NotNull;

import android.annotation.TargetApi;
import android.media.MediaMetadata;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;

/**
 * This class creates a Music Item, a item commonly found in the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">Database Subsystem</A> in the
 * <A HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, for the Android backend.
 */
public class Music extends AbstractMusic<Music> {

    /**
     * Similar to the default {@code Comparable} for the Music class, but it compares without
     * taking disc and track numbers into account.
     */
    public static final Comparator<Music> COMPARE_WITHOUT_TRACK_NUMBER =
            new ComparatorWithoutTrackNumber<>();

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<Music> CREATOR = new MusicParcelCreator();

    /**
     * The copy constructor for this class.
     *
     * @param entry The {@link Entry} to copy.
     */
    public Music(@NotNull final Music entry) {
        super(entry.mResponseObject);
    }

    /**
     * This constructor generates a Music Item from a MPD server response.
     *
     * @param response The MPD server generated response.
     */
    public Music(@NotNull final String response) {
        super(new ResponseObject(null, response));
    }


    /**
     * This constructor is used to create a new Music item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     */
    private Music(@NotNull final ResponseObject object) {
        super(object);
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

    /**
     * This class is used to instantiate a Music Object from a {@code Parcel}.
     */
    private static final class MusicParcelCreator implements Parcelable.Creator<Music> {

        /**
         * Sole constructor.
         */
        private MusicParcelCreator() {
            super();
        }

        /**
         * Create a new instance of the Parcelable class, instantiating it
         * from the given Parcel whose data had previously been written by
         * {@link Parcelable#writeToParcel Parcelable.writeToParcel()}.
         *
         * @param source The Parcel to read the object's data from.
         * @return Returns a new instance of the Parcelable class.
         */
        @Override
        public Music createFromParcel(final Parcel source) {
            return new Music((ResponseObject) source.readParcelable(ResponseObject.LOADER));
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public Music[] newArray(final int size) {
            return new Music[size];
        }
    }
}