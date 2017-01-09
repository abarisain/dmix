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

import com.anpmech.mpd.AbstractResponseObject;
import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.Tools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class provides a abstract base for {@link Item}s which can be built from a MPD protocol
 * response.
 *
 * @param <T> The type of object which can be built from a MPD protocol response.
 */
public abstract class AbstractResponseItem<T extends AbstractResponseItem<T>> extends Item<T> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "AbstractResponseItem";

    /**
     * This Object stores the response and queries for values.
     */
    protected final ResponseObject mResponseObject;

    /**
     * This object is used to create a new ResponseItem.
     *
     * @param object The prepared ResponseObject.
     */
    AbstractResponseItem(@NotNull final ResponseObject object) {
        super();

        mResponseObject = object;
    }

    /**
     * Compares this instance with the specified object and indicates if they
     * are equal. In order to be equal, {@code o} must represent the same object
     * as this instance using a class-specific comparison. The general contract
     * is that this comparison should be reflexive, symmetric, and transitive.
     * Also, no object reference other than null is equal to null.
     *
     * <p>The default implementation returns {@code true} only if {@code this ==
     * o}. See <a href="{@docRoot}reference/java/lang/Object.html#writing_equals">Writing a correct
     * {@code equals} method</a>
     * if you intend implementing your own {@code equals} method.
     *
     * <p>The general contract for the {@code equals} and {@link
     * #hashCode()} methods is that if {@code equals} returns {@code true} for
     * any two objects, then {@code hashCode()} must return the same value for
     * these objects. This means that subclasses of {@code Object} usually
     * override either both methods or neither of them.
     *
     * @param o the object to compare this instance with.
     * @return {@code true} if the specified object is equal to this {@code
     * Object}; {@code false} otherwise.
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
            final T entry = (T) o;

            if (Tools.isNotEqual(mResponseObject, entry.mResponseObject)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual;
    }

    /**
     * Returns a key's value from the {@link AbstractResponseObject#mResponse}, if a response
     * exists, otherwise the {@link AbstractResponseObject#mName} field is returned, regardless
     * of the {@code key} parameter value.
     *
     * @param key The key to get the value for.
     * @return The value paired to the key, null if not found.
     */
    @Nullable
    protected String findValue(final String... key) {
        return mResponseObject.findValue(key);
    }

    /**
     * Returns the URI fragment if it exists.
     *
     * @param key The key to find the URI value for.
     * @return The URI fragment if it exists, null otherwise.
     */
    protected String getURIFragment(final String key) {
        final String fullPath = findValue(key);
        final int pos;
        String streamName = null;

        if (fullPath == null) {
            pos = 0;
        } else {
            pos = fullPath.indexOf('#');
        }

        if (pos > 1) {
            streamName = fullPath.substring(pos + 1, fullPath.length());
        }

        return streamName;
    }

    /**
     * Returns an integer hash code for this Item. By contract, any two objects for which {@link
     * #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * @return This Item hash code.
     * @see Object#equals(Object)
     */
    @Override
    public int hashCode() {
        return mResponseObject.hashCode() + getClass().hashCode();
    }
}
