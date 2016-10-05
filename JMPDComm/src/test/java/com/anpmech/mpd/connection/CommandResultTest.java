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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandResultTest extends CommandResult {

    /**
     * This constructor is used to get protected status.
     */
    public CommandResultTest() {
        super(CommandResult.EMPTY);
    }

    /**
     * This method tests the {@link CommandResult#isEmpty()} method against a empty result.
     */
    @Test
    public void testEmptyResponseConsistency() {
        assertTrue("Empty response failed to indicate empty", EMPTY.isEmpty());
    }

    /**
     * This method tests the {@link CommandResult#contains(CharSequence)} method against a empty
     * result.
     */
    @Test
    public void testEmptyResponseContains() {
        /**
         * This call is to make sure we don't NPE or otherwise fail on an empty response. This is
         * false due to the lack of newline required by MPD server response.
         */
        assertFalse("Failed to query contains on an empty response.",
                EMPTY.contains(""));
    }

    /**
     * This method tests connection header validity of a empty CommandResult.
     */
    @Test
    public void testEmptyResponseHeaderValidity() {
        assertFalse("Valid header on empty response.", EMPTY.isHeaderValid());
    }
}
