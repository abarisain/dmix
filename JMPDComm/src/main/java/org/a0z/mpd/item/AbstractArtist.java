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

import org.a0z.mpd.Tools;

import java.util.Arrays;
import java.util.Locale;

/** This class is the generic base for the Artist items, abstracted for backend. */
abstract class AbstractArtist extends Item {

    private final String mName;

    private final String mSort;

    AbstractArtist(final AbstractArtist artist) {
        this(artist.mName, artist.mSort);
    }

    AbstractArtist(final String name) {
        super();
        mName = name;
        if (null != name && name.toLowerCase(Locale.getDefault()).startsWith("the ")) {
            mSort = name.substring(4);
        } else {
            mSort = null;
        }
    }

    AbstractArtist(final String name, final String sort) {
        super();

        mName = name;
        mSort = sort;
    }

    /**
     * Compares an Artist object with a general contract of
     * comparison that is reflexive, symmetric and transitive.
     *
     * @param o The object to compare this instance with.
     * @return True if the objects are equal with regard to te general contract, false otherwise.
     * @see Object#equals(Object)
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
            final AbstractArtist artist = (AbstractArtist) o;

            if (Tools.isNotEqual(mName, artist.mName) || Tools.isNotEqual(mSort, artist.mSort)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    @Override
    public String getName() {
        return mName;
    }

    /**
     * Returns an integer hash code for this Artist. By contract, any two objects for which
     * {@link #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * @return This Artist hash code.
     * @see Object#equals(Object)
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mName, mSort});
    }

    @Override
    public String sortText() {
        final String result;

        if (mSort == null && mName == null) {
            result = "";
        } else if (mSort == null) {
            result = super.sortText();
        } else {
            result = mSort;
        }

        return result;
    }
}
