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

package com.anpmech.mpd.subsystem;

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.Tools;

import org.jetbrains.annotations.NotNull;

/**
 * This class represents a single
 * <A HREF="http://www.musicpd.org/doc/protocol/output_commands.html">audio output</A> in the
 * <A HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>.
 */
abstract class AbstractAudioOutput {

    /**
     * The MPD protocol command given to retrieve information about the current outputs.
     */
    public static final String CMD_ACTION_OUTPUTS = "outputs";

    /**
     * The command response received detailing whether an AudioOutput is enabled.
     */
    public static final String RESPONSE_ENABLED = "outputenabled";

    /**
     * The command response received for an AudioOutput detailing the identification number.
     */
    public static final String RESPONSE_ID = "outputid";

    /**
     * The command response received detailing the AudioOutput name.
     */
    public static final String RESPONSE_NAME = "outputname";

    /**
     * This is where the response for this Object is kept.
     */
    protected final ResponseObject mResponseObject;

    /**
     * Sole constructor.
     *
     * <p>The parameters to this constructor cannot both be null or non-null simultaneously.</p>
     *
     * @param response The MPD server generated response.
     */
    AbstractAudioOutput(@NotNull final ResponseObject response) {
        super();

        mResponseObject = response;
    }

    /**
     * Compares this instance with the specified object and indicates if they are equal. In order
     * to be equal, {@code o} must represent the same object as this instance using a
     * class-specific comparison. The general contract is that this comparison should be reflexive,
     * symmetric, and transitive. Also, no object reference other than null is equal to null.
     *
     * @param o the object to compare this instance with.
     * @return {@code true} if the specified object is equal to this {@code Object}; {@code false}
     * otherwise.
     * @see #hashCode
     */
    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            /** This has to be the same due to the class check above. */
            //noinspection unchecked
            final AbstractAudioOutput entry = (AbstractAudioOutput) o;

            /**
             * Neither can be null at this point, one or the other is not null,
             * checked at construction.
             */
            //noinspection ConstantConditions
            if (Tools.isNotEqual(mResponseObject, entry.mResponseObject)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    /**
     * The identification number of this AudioOutput.
     *
     * @return The identification number of this AudioOutput.
     */
    public int getId() {
        return Tools.parseInteger(mResponseObject.findValue(RESPONSE_ID));
    }

    /**
     * The name of this AudioOutput.
     *
     * @return The name of this AudioOutput.
     */
    public String getName() {
        return mResponseObject.findValue(RESPONSE_NAME);
    }

    /**
     * Returns an integer hash code for this object. By contract, any two objects for which {@link
     * #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * <p>Note that hash values must not change over time unless information used in equals
     * comparisons also changes.</p>
     *
     * @return this object's hash code.
     * @see #equals
     */
    @Override
    public int hashCode() {
        return mResponseObject.hashCode() + super.hashCode();
    }

    /**
     * The current enabled status of this AudioOutput.
     *
     * @return True if this AudioOutput is enabled, false otherwise.
     */
    public boolean isEnabled() {
        final String enabledResponse = mResponseObject.findValue(RESPONSE_ENABLED);
        boolean isEnabled = false;

        if ("1".equals(enabledResponse)) {
            isEnabled = true;
        }

        return isEnabled;
    }

    /**
     * This method returns the generated name for this AudioOutput.
     *
     * @return The {@link #RESPONSE_NAME} of this AudioOutput.
     */
    @Override
    public String toString() {
        return getName();
    }
}
