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

package com.anpmech.mpd.connection;

import com.anpmech.mpd.commandresponse.CommandResponse;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This is the core of the {@link CommandResponse} classes, abstracted for the Android backend.
 *
 * <p>This class contains the bare results from the connection. Processing required from this
 * result should be done in another class.</p>
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class CommandResult extends AbstractCommandResult implements Parcelable {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<CommandResult> CREATOR = new CommandResultCreator();

    /**
     * This is the ClassLoader to use when unparceling this class.
     */
    public static final ClassLoader LOADER = CommandResult.class.getClassLoader();

    /**
     * This is an empty no-op CommandResult.
     */
    protected static final CommandResult EMPTY = new CommandResult();

    /**
     * This constructor is used to subclass a CommandResult.
     *
     * @param result The result to subclass.
     */
    protected CommandResult(final CommandResult result) {
        this(result.mConnectionResult, result.mResult);
    }

    /**
     * This constructor is used to create a new core result from the MPD protocol.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     */
    protected CommandResult(final String connectionResult, final String result) {
        super(connectionResult, result);
    }

    /**
     * This constructor is used to create a new empty CommandResult.
     *
     * @see #EMPTY
     */
    private CommandResult() {
        super();
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
        dest.writeString(mConnectionResult);
        dest.writeString(mResult);
    }

    /**
     * This class is used to instantiate a CommandResult from a {@code Parcel}.
     */
    private static final class CommandResultCreator implements Creator<CommandResult> {

        /**
         * Sole constructor.
         */
        private CommandResultCreator() {
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
        public CommandResult createFromParcel(final Parcel source) {
            return new CommandResult(source.readString(), source.readString());
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public CommandResult[] newArray(final int size) {
            return new CommandResult[size];
        }
    }
}
