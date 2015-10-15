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

package com.anpmech.mpd.connection;

import com.anpmech.mpd.TestTools;
import com.anpmech.mpd.commandresponse.CommandResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class creates {@link CommandResponse}s from the various test resources.
 */
public enum CommandResponseCreator {
    ;

    public static CommandResponse getCommandResponse(final String filePath) throws IOException {
        final String rawResult = TestTools.readFile(filePath);
        final CommandResult result = new CommandResult(null, rawResult);

        return new CommandResponse(result);
    }

    public static Collection<CommandResponse> getCommandResponses() throws IOException {
        final Collection<CommandResponse> results =
                new ArrayList<>(TestTools.TEST_FILE_PATHS.length);

        for (final String file : TestTools.TEST_FILE_PATHS) {
            results.add(getCommandResponse(file));
        }

        return results;
    }

}
