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

package org.a0z.mpd;

import org.a0z.mpd.exception.InvalidResponseException;

import java.util.Collection;

import static org.a0z.mpd.Tools.KEY;
import static org.a0z.mpd.Tools.VALUE;

/*
 * Class representing one configured output
 */
public class MPDOutput {

    static final String CMD_ID = "outputid";

    private static final String CMD_ENABLED = "outputenabled";

    private static final String CMD_NAME = "outputname";

    private static final String TAG = "MPDOutput";

    private final boolean mEnabled;

    private final int mId;

    private final String mName;

    MPDOutput(final String name, final int id, final boolean enabled) {
        super();

        mName = name;
        mId = id;
        mEnabled = enabled;
    }

    public static MPDOutput build(final Collection<String> response) {
        String name = null;
        int id = -1;
        Boolean enabled = null;

        for (final String[] pair : Tools.splitResponse(response)) {
            switch (pair[KEY]) {
                case CMD_ENABLED:
                    enabled = Boolean.valueOf("1".equals(pair[VALUE]));
                    break;
                case CMD_ID:
                    id = Integer.parseInt(pair[VALUE]);
                    break;
                case CMD_NAME:
                    name = pair[VALUE];
                    break;
                default:
                    Log.warning(TAG,
                            "Non-standard line appeared in output response. Key: " + pair[KEY]
                                    + " value: " + pair[VALUE]);
                    break;
            }
        }

        if (name == null || id == -1 || enabled == null) {
            throw new InvalidResponseException("Failed to parse output information.");
        }

        return new MPDOutput(name, id, enabled.booleanValue());
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public String toString() {
        return mName;
    }

}
