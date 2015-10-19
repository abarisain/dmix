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

package com.anpmech.mpd.commandresponse;

import com.anpmech.mpd.TestTools;
import com.anpmech.mpd.connection.CommandResponseCreator;
import com.anpmech.mpd.connection.CommandResult;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * This class tests the {@link SeparatedResponse} class.
 */
public class SeparatedResponseTest
        extends ObjectResponseTest<CommandResponse, SeparatedResponse> {

    /**
     * This is the error message emitted upon a failure testing empty results size consistency.
     */
    private static final String EMPTY_FAILURE = "iterator failed to iterate correct fixed " +
            "number of times when iterating over an empty value.";

    /**
     * This is the error message given if there is an assertion error with a non-separated
     * iteration.
     */
    private static final String NON_SEPARATED_ERROR
            = "Singular response failed size consistency test.";

    /**
     * Sole constructor.
     */
    public SeparatedResponseTest() {
        super();
    }

    /**
     * This returns a empty ObjectResponse for the ObjectResponse subclass.
     *
     * @return A empty ObjectResponse.
     */
    @Override
    protected SeparatedResponse getEmptyResponse() {
        return new SeparatedResponse();
    }

    /**
     * This is common code to get a non-separated {@link CommandResult}.
     *
     * @return A non-separated {@link CommandResult}.
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    private SeparatedResponse getNonSeparatedResult() throws IOException {
        final CommandResult result = CommandResponseCreator
                .getCommandResponse(TestTools.FILE_SINGULAR_TRACK_FILE);

        return instantiate(result);
    }

    /**
     * This returns a path to a test sample file to construct a CommandResult from.
     *
     * @return A path to a test sample file.
     */
    @Override
    protected String getResponsePath() {
        return TestTools.FILE_SEPARATED_COMMAND_RESPONSE;
    }

    /**
     * This method instantiates the ObjectResponse type from the {@code CommandResult} parameter.
     *
     * @param result The {@code CommandResult} to create the ObjectResponse type from.
     * @return A ObjectResponse subclass type.
     */
    @Override
    protected SeparatedResponse instantiate(final CommandResult result) {
        return new SeparatedResponse(result);
    }

    /**
     * This method tests a number of empty results for size consistency.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void multipleEmptyResultSizeConsistency() throws IOException {
        final CommandResult result = CommandResponseCreator.getCommandResponse(
                TestTools.FILE_SEPARATED_EMPTY_SEPARATED_RESPONSES);
        final SeparatedResponse response = instantiate(result);
        final long size = (long) new CommandResponse(result).getList().size();

        assertEquals(EMPTY_FAILURE, size, (long) response.getList().size());
    }

    /**
     * This method tests a number of empty results for size consistency.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void multipleEmptyResultsSizeConsistency() throws IOException {
        final CommandResult result = CommandResponseCreator.getCommandResponse(
                TestTools.FILE_SEPARATED_EMPTY_SEPARATED_RESPONSES);
        final SeparatedResponse response = instantiate(result);
        final CommandResponse commandResponse = new CommandResponse(result);

        assertEquals("Reversed " + EMPTY_FAILURE, (long) commandResponse.getList().size(),
                (long) TestTools.reverseList(response).size());
    }

    /**
     * This tests a {@link SeparatedResponse} to ensure it can gracefully handle a
     * non-separated {@link CommandResult} during iteration.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void nonSeparatedIteration() throws IOException {
        assertEquals(NON_SEPARATED_ERROR, 1L, (long) getNonSeparatedResult().getList().size());
    }

    /**
     * This tests a {@link SeparatedResponse} to ensure it can gracefully handle a
     * non-separated {@link CommandResult} during reverse iteration.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void nonSeparatedReverseIteration() throws IOException {
        assertEquals(NON_SEPARATED_ERROR, 1L, (long)
                TestTools.reverseList(getNonSeparatedResult()).size());
    }
}
