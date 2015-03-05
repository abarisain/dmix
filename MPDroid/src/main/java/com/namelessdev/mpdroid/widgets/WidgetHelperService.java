/*
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.widgets;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.MPDControl;

import android.app.IntentService;
import android.content.Intent;

public class WidgetHelperService extends IntentService {

    public static final String CMD_UPDATE_WIDGET = "UPDATE_WIDGET";

    static final String TAG = "MPDroidWidgetHelperService";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private boolean mPlaying = false;

    public WidgetHelperService() {
        super(TAG);
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        // get MPD connection
        mApp.addConnectionLock(this);

        // prepare values for runnable
        final MPD mpd = mApp.getMPD();
        final String action = intent.getAction();

        // schedule real work
        mApp.getAsyncHelper().execAsync(new Runnable() {
            @Override
            public void run() {
                processIntent(action, mpd);
            }
        });

        // clean up
        mApp.removeConnectionLock(this);
    }

    void processIntent(final String action, final MPD mpd) {
        switch (action) {
            case CMD_UPDATE_WIDGET:
                mPlaying = mpd.getStatus().isState(MPDStatusMap.STATE_PLAYING);
                SimpleWidgetProvider.getInstance().notifyChange(this);
                break;
            default:
                MPDControl.run(action);
        }
    }
}
