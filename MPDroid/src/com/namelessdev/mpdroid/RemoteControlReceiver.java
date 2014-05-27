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

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.helpers.MPDControl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

/**
 * RemoteControlReceiver receives media player button stuff. Most of
 * the code was taken from the Android Open Source Project music app.
 *
 * @author Arnaud Barisain Monrose (Dream_Team)
 */
public class RemoteControlReceiver extends BroadcastReceiver {

    private static final String TAG = "com.namelessdev.mpdroid.RemoteControlReceiver";

    private final MPDApplication app = MPDApplication.getInstance();

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

        Log.d(TAG, "Intent: " + intent + " received with context: " + context + " with action: "
                + action);

        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            final int eventKeyCode = event.getKeyCode();
            Log.d(TAG, "with keycode: " + eventKeyCode);
            switch (eventKeyCode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    MPDControl.run(MPDControl.ACTION_STOP);
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    MPDControl.run(MPDControl.ACTION_TOGGLE_PLAYBACK);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    MPDControl.run(MPDControl.ACTION_NEXT);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    MPDControl.run(MPDControl.ACTION_PREVIOUS);
                    break;
                default:
                    break;
            }
        } else {
            switch (action) {
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    if (app.isLocalAudible()) {
                        MPDControl.run(MPDControl.ACTION_PAUSE);
                    }
                    break;
                case NotificationService.ACTION_CLOSE_NOTIFICATION:
                    app.getApplicationState().persistentNotification = false;
                    app.getApplicationState().notificationMode = false;

                    final Intent notificationServiceIntent
                            = new Intent(context, NotificationService.class);
                    context.stopService(notificationServiceIntent);
                    break;
                default:
                    MPDControl.run(action);
                    break;
            }
        }
    }
}