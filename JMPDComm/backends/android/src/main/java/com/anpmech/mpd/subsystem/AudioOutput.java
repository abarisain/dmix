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

package com.anpmech.mpd.subsystem;

import com.anpmech.mpd.ResponseObject;

import org.jetbrains.annotations.NotNull;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents a single
 * <A HREF="http://www.musicpd.org/doc/protocol/output_commands.html">audio output</A> in the
 * <A HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, abstracted for the Android
 * backend.
 */
public class AudioOutput extends AbstractAudioOutput implements Parcelable {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Parcelable.Creator<AudioOutput> CREATOR = new AudioOutputParcelCreator();

    /**
     * This is a convenience string to use as a Intent extra tag.
     */
    public static final String EXTRA = "Outputs";

    /**
     * This constructor constructs a Genre from a MPD protocol response.
     *
     * @param response A MPD protocol response.
     */
    public AudioOutput(@NotNull final String response) {
        super(new ResponseObject(null, response));
    }

    /**
     * The copy constructor for this class.
     *
     * @param entry The AbstractResponseItem to copy.
     */
    public AudioOutput(@NotNull final AudioOutput entry) {
        super(entry.mResponseObject);
    }

    /**
     * This constructor is used to create a new AudioOutput item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     */
    private AudioOutput(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     *
     * @return a bitmask indicating the set of special object types marshalled
     * by the Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(mResponseObject, 0);
    }

    /**
     * This class is used to instantiate a AudioOutput Object from a {@code Parcel}.
     */
    private static final class AudioOutputParcelCreator
            implements Parcelable.Creator<AudioOutput> {

        /**
         * Sole constructor.
         */
        private AudioOutputParcelCreator() {
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
        public AudioOutput createFromParcel(final Parcel source) {
            return new AudioOutput((ResponseObject) source.readParcelable(ResponseObject.LOADER));
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public AudioOutput[] newArray(final int size) {
            return new AudioOutput[size];
        }
    }
}
