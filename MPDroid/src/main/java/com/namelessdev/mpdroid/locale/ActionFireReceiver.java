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

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.service.MPDroidService;
import com.namelessdev.mpdroid.service.NotificationHandler;
import com.namelessdev.mpdroid.service.StreamHandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ActionFireReceiver extends BroadcastReceiver {

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final boolean DEBUG = false;

    private static final String TAG = "MPDroid Locale Plugin";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Bundle bundle = intent.getBundleExtra(LocaleConstants.EXTRA_BUNDLE);
        if (bundle != null) {
            final String action = bundle.getString(EditActivity.BUNDLE_ACTION_STRING);

            switch (action) {
                case NotificationHandler.ACTION_START:
                case StreamHandler.ACTION_START:
                    redirectIntentToService(true, intent, action);
                    break;
                case NotificationHandler.ACTION_STOP:
                case StreamHandler.ACTION_STOP:
                    redirectIntentToService(false, intent, action);
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
                    break;
            }
        }
    }

    /**
     * This method redirects the incoming broadcast intent to the service, if it's alive. The
     * service cannot be communicated through messages in this class because this BroadcastReceiver
     * is registered through the AndroidManifest {@code <receiver>} tag which means this
     * BroadcastReceiver will no longer exist after return from {@code onReceive()}.
     *
     * @param forceService Force the action, even if the service isn't active.
     * @param intent       The incoming intent through {@code onReceive()}.
     * @param action       The incoming intent action.
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    private void redirectIntentToService(final boolean forceService, final Intent intent,
            final String action) {
        intent.setClass(APP, MPDroidService.class);
        intent.setAction(action);
        final IBinder iBinder = peekService(APP, intent);
        if (forceService || iBinder != null && iBinder.isBinderAlive()) {
            if (DEBUG) {
                Log.d(TAG, "Redirecting action " + action + " to the service.");
            }

            APP.startService(intent);
        }
    }
}
