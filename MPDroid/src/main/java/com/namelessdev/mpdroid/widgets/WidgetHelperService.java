/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.widgets;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;

import android.app.IntentService;
import android.content.Intent;

public class WidgetHelperService extends IntentService {
    static final String TAG = "MPDroidWidgetHelperService";

    public static final String CMD_UPDATE_WIDGET = "UPDATE_WIDGET";

    private boolean playing = false;

    private final MPDApplication app = MPDApplication.getInstance();

    public WidgetHelperService() {
        super(TAG);
    }

    public boolean isPlaying() {
        return playing;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // get MPD connection
        app.setActivity(this);

        // prepare values for runnable
        final MPD mpd = app.oMPDAsyncHelper.oMPD;
        final String action = intent.getAction();

        // schedule real work
        app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                processIntent(action, mpd);
            }
        });

        // clean up
        app.unsetActivity(this);
    }

    void processIntent(String action, MPD mpd) {
        switch (action) {
            case CMD_UPDATE_WIDGET:
                playing = mpd.getStatus().isState(MPDStatus.STATE_PLAYING);
                SimpleWidgetProvider.getInstance().notifyChange(this);
                break;
            default:
                MPDControl.run(action);
        }
    }
}
