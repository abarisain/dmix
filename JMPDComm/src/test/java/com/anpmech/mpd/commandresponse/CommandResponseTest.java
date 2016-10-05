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

import com.anpmech.mpd.TestTools;
import com.anpmech.mpd.connection.CommandResult;

/**
 * This class tests the {@link CommandResponse} class.
 */
public class CommandResponseTest extends ObjectResponseTest<String, CommandResponse> {

    /**
     * Sole constructor.
     */
    public CommandResponseTest() {
        super();
    }

    /**
     * This returns a empty ObjectResponse for the ObjectResponse subclass.
     *
     * @return A empty ObjectResponse.
     */
    @Override
    protected CommandResponse getEmptyResponse() {
        return new CommandResponse();
    }

    /**
     * This returns a path to a test sample file to construct a CommandResult from.
     *
     * @return A path to a test sample file.
     */
    @Override
    protected String getResponsePath() {
        return TestTools.FILE_ROOT_LSINFO;
    }

    /**
     * This method instantiates the ObjectResponse type from the {@code CommandResult} parameter.
     *
     * @param result The {@code CommandResult} to create the ObjectResponse type from.
     * @return A ObjectResponse subclass type.
     */
    @Override
    protected CommandResponse instantiate(final CommandResult result) {
        return new CommandResponse(result);
    }
}
