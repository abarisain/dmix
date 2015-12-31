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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class serves as a base for non-music MPD database entries, abstracted for the Android
 * backend.
 */
public class Entry extends AbstractEntry<Entry> {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<Entry> CREATOR = new EntryParcelCreator();

    /**
     * The copy constructor for this class.
     *
     * @param entry The Entry to copy.
     */
    public Entry(@NotNull final Entry entry) {
        super(entry.mResponseObject);
    }

    /**
     * This constructor creates a new Entry using the MPD server response.
     *
     * @param response The MPD server generated response.
     */
    public Entry(@NotNull final String response) {
        super(new ResponseObject(null, response));
    }

    /**
     * This object is used to create a new Entry with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     */
    private Entry(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * This class is used to instantiate a Entry Object from a {@code Parcel}.
     */
    private static final class EntryParcelCreator implements Parcelable.Creator<Entry> {

        /**
         * The sole constructor.
         */
        private EntryParcelCreator() {
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
        public Entry createFromParcel(final Parcel source) {
            return new Entry((ResponseObject) source.readParcelable(ResponseObject.LOADER));
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public Entry[] newArray(final int size) {
            return new Entry[size];
        }
    }
}
