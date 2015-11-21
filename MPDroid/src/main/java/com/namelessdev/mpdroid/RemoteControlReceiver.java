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

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.service.MPDroidService;
import com.namelessdev.mpdroid.service.NotificationHandler;
import com.namelessdev.mpdroid.service.StreamHandler;
import com.namelessdev.mpdroid.tools.Tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

/**
 * RemoteControlReceiver receives media player button stuff. Most of the code was taken from the
 * Android Open Source Project music app.
 *
 * @author Arnaud Barisain Monrose (Dream_Team)
 */
public class RemoteControlReceiver extends BroadcastReceiver {

    /**
     * The debug flag. If true, debug outputs to the logcat.
     */
    private static final boolean DEBUG = false;

    /**
     * The class log identifier.
     */
    private static final String TAG = "RemoteControlReceiver";

    private static boolean isMediaButton(final Intent intent, final String action) {
        final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        boolean isHandled = true;

        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            final int eventKeyCode = event.getKeyCode();

            switch (eventKeyCode) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    Tools.runCommand(MPDControl.ACTION_TOGGLE_PLAYBACK);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    Tools.runCommand(MPDControl.ACTION_NEXT);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    Tools.runCommand(MPDControl.ACTION_PAUSE);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    Tools.runCommand(MPDControl.ACTION_PLAY);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    Tools.runCommand(MPDControl.ACTION_PREVIOUS);
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    Tools.runCommand(MPDControl.ACTION_STOP);
                    break;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    Tools.runCommand(MPDControl.ACTION_VOLUME_STEP_UP);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Tools.runCommand(MPDControl.ACTION_VOLUME_STEP_DOWN);
                    break;
                default:
                    isHandled = false;
                    break;
            }
        } else {
            isHandled = false;
        }

        return isHandled;
    }

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final MPDApplication app = (MPDApplication) context.getApplicationContext();
        final String action = intent.getAction();
        boolean isActionHandled;

        if (BuildConfig.DEBUG) {
            Log.d(TAG, Tools.debugIntent(intent, null));
        }

        isActionHandled = isMediaButton(intent, action);

        if (!isActionHandled) {
            switch (action) {
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    if (Tools.isServerLocalhost()) {
                        Tools.runCommand(MPDControl.ACTION_PAUSE);
                    } else {
                        redirectIntentToService(context, false, intent);
                    }
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    if (app.isNotificationPersistent()) {
                        redirectIntentToService(context, true, intent);
                    }
                    break;
                case MPDroidService.ACTION_STOP:
                    app.setPersistentOverride(true);
                    /** Fall Through */
                case NotificationHandler.ACTION_START:
                case StreamHandler.ACTION_START:
                    redirectIntentToService(context, true, intent);
                    break;
                default:
                    //noinspection ResourceType
                    Tools.runCommand(action);
                    break;
            }
        }
    }

    /**
     * This method redirects the incoming broadcast intent to the service, if it's alive. The
     * service cannot be communicated through messages in this class because this BroadcastReceiver
     * is registered through the AndroidManifest {@code <receiver>} tag which results in this
     * BroadcastReceiver will no longer exist after return from {@code onReceive()}.
     *
     * @param context      The current context given passed to this {@link BroadcastReceiver}.
     * @param forceService Force the action, even if the service isn't active.
     * @param intent       The incoming intent through {@code onReceive()}.
     * @see BroadcastReceiver#onReceive(Context, Intent)
     */
    private void redirectIntentToService(final Context context, final boolean forceService,
            final Intent intent) {
        intent.setClass(context, MPDroidService.class);
        final IBinder iBinder = peekService(context, intent);
        if (forceService || iBinder != null && iBinder.isBinderAlive()) {
            if (DEBUG) {
                Log.d(TAG, "Redirecting action " + intent.getAction() + " to the service.");
            }

            context.startService(intent);
        }
    }
}