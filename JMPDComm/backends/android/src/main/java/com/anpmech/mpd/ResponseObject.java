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

package com.anpmech.mpd;

import org.jetbrains.annotations.Nullable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents a response or a name for one object, abstracted for the Android backend.
 */
public class ResponseObject extends AbstractResponseObject implements Parcelable {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<ResponseObject> CREATOR = new ResponseParcelCreator();

    /**
     * This loader is used to load this class as a {@link Parcelable}.
     */
    public static final ClassLoader LOADER = ResponseObject.class.getClassLoader();

    /**
     * This int is included in the parcel to designate that the type of instantiating parameter is
     * a identifier.
     */
    private static final int PARCEL_TYPE_IDENTIFIER = 0;

    /**
     * This int is included in the parcel to designate that the type of instantiating parameter is
     * a response.
     */
    private static final int PARCEL_TYPE_RESPONSE = 1;

    /**
     * Sole constructor.
     *
     * @param name     The name for the object.
     * @param response The response for the object.
     */
    public ResponseObject(@Nullable final String name, @Nullable final String response) {
        super(name, response);
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
        if (mResponse == null) {
            dest.writeInt(PARCEL_TYPE_IDENTIFIER);
            dest.writeString(mName);
        } else {
            dest.writeInt(PARCEL_TYPE_RESPONSE);
            dest.writeString(mResponse);
        }
    }

    /**
     * This class is used to instantiate a ResponseObject from a {@link Parcel}.
     */
    private static final class ResponseParcelCreator implements Creator<ResponseObject> {

        /**
         * Sole constructor.
         */
        private ResponseParcelCreator() {
            super();
        }

        /**
         * Create a new instance of the Parcelable class, instantiating it from the given Parcel
         * whose data had previously been written by
         * {@link Parcelable#writeToParcel Parcelable.writeToParcel()}.
         *
         * @param source The Parcel to read the object's data from.
         * @return Returns a new instance of the Parcelable class.
         */
        @Override
        public ResponseObject createFromParcel(final Parcel source) {
            final ResponseObject responseObject;
            final int type = source.readInt();
            final String read = source.readString();

            if (type == PARCEL_TYPE_IDENTIFIER) {
                responseObject = new ResponseObject(read, null);
            } else if (type == PARCEL_TYPE_RESPONSE) {
                responseObject = new ResponseObject(null, read);
            } else {
                throw new IllegalArgumentException("Failed to create from invalid parcel: " + type);
            }

            return responseObject;
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry
         * initialized to null.
         */
        @Override
        public ResponseObject[] newArray(final int size) {
            return new ResponseObject[size];
        }
    }
}
