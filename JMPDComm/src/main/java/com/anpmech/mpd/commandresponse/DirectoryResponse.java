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

package com.anpmech.mpd.commandresponse;

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.commandresponse.iterator.DirectoryIterator;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.Directory;

import java.util.ListIterator;

/**
 * This class contains methods used to process {@link Directory} entries from a MPD response.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 *
 * @see ListingResponse
 */
public class DirectoryResponse extends ObjectResponse<Directory> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "DirectoryResponse";

    /**
     * This constructor is used to create {@link Directory} objects from a CommandResult.
     *
     * @param result The CommandResult containing a Directory type MPD result.
     */
    public DirectoryResponse(final CommandResult result) {
        super(result);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public DirectoryResponse() {
        super();
    }

    /**
     * This constructor is used to create {@link Directory} objects from another compatible
     * {@link ObjectResponse}.
     *
     * @param response The ObjectResponse containing a Directory type MPD response.
     */
    public DirectoryResponse(final ObjectResponse<?> response) {
        super(response);
    }

    /**
     * This constructor is used to iterate over responses in a {@link ResponseObject}.
     *
     * @param response The ResponseObject to iterate over.
     */
    public DirectoryResponse(final ResponseObject response) {
        super(response);
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response.
     */
    @Override
    protected ListIterator<Directory> listIterator(final int position) {
        return new DirectoryIterator(mResult, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE} if
     * there are more than {@link Integer#MAX_VALUE} elements in this {@code Collection}.
     */
    @Override
    public int size() {
        return DirectoryIterator.size(mResult);
    }
}
