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

package com.anpmech.mpd;

import com.anpmech.mpd.commandresponse.CommandResponse;
import com.anpmech.mpd.commandresponse.KeyValueResponse;
import com.anpmech.mpd.connection.CommandResponseCreator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * This class tests the {@link ResponseObject} class.
 */
public class ResponseObjectTest {

    /**
     * This is the error message given when equality fails for a name and a response.
     */
    private static final String FAILED_EQUALITY = "Equality failed.";

    /**
     * This field allows the expected exception to be changed from none to a specific one.
     */
    @Rule
    public final ExpectedException mException = ExpectedException.none();

    /**
     * A name to use to test this class.
     */
    private final String mSampleName;

    /**
     * A response line to use to test this class.
     */
    private final String mSampleResponseLine;

    /**
     * Sole constructor.
     *
     * @throws IOException Thrown if there is a issue retrieving a result file for the sample
     *                     lines.
     */
    public ResponseObjectTest() throws IOException {
        super();

        final CommandResponse result = CommandResponseCreator.getCommandResponse(
                TestTools.FILE_SINGULAR_TRACK_FILE);
        final Iterable<Map.Entry<String, String>> response = new KeyValueResponse(result);
        final String responseLine = result.iterator().next() + '\n';
        final String name = response.iterator().next().getValue();

        mSampleResponseLine = responseLine;
        mSampleName = name;
    }

    /**
     * This class tests that {@link ResponseObject} instantiation fails when both initializing
     * parameters are {@code null}.
     */
    @Test
    public void testResponseObjectConstruction() {
        mException.expect(IllegalArgumentException.class);
        mException.reportMissingExceptionWithMessage(
                "ResponseObject must fail with null name and response.");
        new ResponseObject(null, null);
    }

    /**
     * This tests for naming consistency for single named equality (name first).
     */
    @Test
    public void testResponseObjectNameConsistency() throws IOException {

        final ResponseObject roName = new ResponseObject(mSampleName, null);
        final ResponseObject roResponse = new ResponseObject(null, mSampleResponseLine);

        assertEquals(FAILED_EQUALITY, roResponse, roName);
    }

    /**
     * This tests for naming consistency for single named equality (response first).
     */
    @Test
    public void testResponseObjectResponseConsistency() throws IOException {
        final ResponseObject roName = new ResponseObject(mSampleName, null);
        final ResponseObject roResponse = new ResponseObject(null, mSampleResponseLine);

        assertEquals(FAILED_EQUALITY, roName, roResponse);
    }
}
