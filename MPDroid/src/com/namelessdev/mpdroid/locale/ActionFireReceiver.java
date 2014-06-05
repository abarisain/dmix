/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.locale;

import com.namelessdev.mpdroid.service.MPDroidService;
import com.namelessdev.mpdroid.RemoteControlReceiver;
import com.namelessdev.mpdroid.helpers.MPDControl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ActionFireReceiver extends BroadcastReceiver {

    private static final String TAG = "MPDroid Locale Plugin";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Bundle bundle = intent.getBundleExtra(LocaleConstants.EXTRA_BUNDLE);
        if (bundle == null) {
            return;
        }
        final String action = bundle.getString(EditActivity.BUNDLE_ACTION_STRING);
        final Intent serviceIntent;

        switch (action) {
            case MPDroidService.ACTION_START:
                serviceIntent = new Intent(context, MPDroidService.class);
                serviceIntent.setAction(action);
                context.startService(serviceIntent);
                break;
            case MPDroidService.ACTION_CLOSE_NOTIFICATION:
                serviceIntent = new Intent(context, RemoteControlReceiver.class);
                serviceIntent.setAction(action);
                context.startService(serviceIntent);
                break;
            default:
                int volume = MPDControl.INVALID_INT;

                if (MPDControl.ACTION_VOLUME_SET.equals(action)) {
                    final String volumeString = bundle
                            .getString(EditActivity.BUNDLE_ACTION_EXTRA);
                    if (volumeString != null) {
                        try {
                            volume = Integer.parseInt(volumeString);
                        } catch (final NumberFormatException e) {
                            Log.e(TAG, "Invalid volume string : " + volumeString, e);
                        }
                    }
                }
                MPDControl.run(action, volume);
                System.exit(0);
                break;
        }
    }
}
