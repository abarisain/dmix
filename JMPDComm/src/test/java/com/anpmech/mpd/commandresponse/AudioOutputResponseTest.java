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
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.subsystem.AudioOutput;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * This class tests the {@link AudioOutput} class.
 */
public class AudioOutputResponseTest extends
        ObjectResponseTest<AudioOutput, AudioOutputResponse> {

    /**
     * Sole constructor.
     */
    public AudioOutputResponseTest() {
        super();
    }

    /**
     * This method tests the known fixed size of the sample 'outputs' result.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void audioOutputResponseSizeTest() throws IOException {
        final List<AudioOutput> list = instantiate(getResult()).getList();

        assertEquals("AudioOutput list size failed against known result.", 2L, (long) list.size());
    }

    /**
     * This returns a empty ObjectResponse for the ObjectResponse subclass.
     *
     * @return A empty ObjectResponse.
     */
    @Override
    protected AudioOutputResponse getEmptyResponse() {
        return new AudioOutputResponse();
    }

    /**
     * This returns a path to a test sample file to construct a CommandResult from.
     *
     * @return A path to a test sample file.
     */
    @Override
    protected String getResponsePath() {
        return TestTools.FILE_OUTPUTS;
    }

    /**
     * This method instantiates the ObjectResponse type from the {@code CommandResult} parameter.
     *
     * @param result The {@code CommandResult} to create the ObjectResponse type from.
     * @return A ObjectResponse subclass type.
     */
    @Override
    protected AudioOutputResponse instantiate(final CommandResult result) {
        return new AudioOutputResponse(result);
    }
}
