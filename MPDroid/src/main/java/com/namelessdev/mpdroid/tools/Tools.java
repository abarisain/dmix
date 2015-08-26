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

package com.namelessdev.mpdroid.tools;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.MPDControl;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public final class Tools {

    /**
     * This is a no operation listener for the DialogInterface.OnClickListener().
     */
    public static final DialogInterface.OnClickListener NOOP_CLICK_LISTENER =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                }
            };

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final String TAG = "Tools";

    private Tools() {
        super();
    }

    /**
     * Converts density independent pixels to pixels for the current device.
     *
     * @param context The context to get the resources from.
     * @param dip     The density independent pixel count to convert to pixel count for the device.
     * @return The device pixel equivalent of the incoming dip count.
     */
    public static float convertDpToPixel(final Context context, final float dip) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, metrics);
    }

    /**
     * This method creates a verbose Intent.toString().
     *
     * @param intent          The Intent to debug.
     * @param callingActivity The getCallingActivity(), if available.
     * @return A verbose toString() about the Intent.
     */
    public static String debugIntent(final Intent intent, final ComponentName callingActivity) {
        final StringBuilder stringBuilder = new StringBuilder(intent.toString());
        final Bundle extras = intent.getExtras();
        int endIndex = stringBuilder.lastIndexOf("(has extras)");

        if (endIndex == -1) {
            endIndex = stringBuilder.lastIndexOf("}");
        }

        /** Trim the closing bracket and extend out the string a bit. */
        stringBuilder.setLength(endIndex - 1);

        if (callingActivity != null) {
            stringBuilder.append(" calling activity: ");
            stringBuilder.append(callingActivity.getClassName());
        }

        if (extras != null) {
            for (final String what : extras.keySet()) {
                stringBuilder.append(" intent extra: ");
                stringBuilder.append(what);

                if (Intent.EXTRA_KEY_EVENT.equals(what)) {
                    final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                    if (event != null) {
                        final int eventKeyCode = event.getKeyCode();

                        stringBuilder.append(", with keycode: ");
                        stringBuilder.append(eventKeyCode);
                    }
                }
            }
        }

        stringBuilder.append(". }");
        return stringBuilder.toString();
    }

    /**
     * This method checks the {@link PackageManager} for the package represented by the
     * {@code packageName} argument.
     *
     * @param packageName The packageName to find.
     * @return True if the package appears to be on the localhost, false otherwise.
     */
    public static boolean isPackageInstalled(final String packageName) {
        final PackageManager packageManager = APP.getPackageManager();
        boolean isInstalled = false;

        if (packageManager != null) {
            try {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES);
                isInstalled = true;
            } catch (final PackageManager.NameNotFoundException ignored) {
                Log.d(TAG, packageName + " is not installed, cannot launch.");
            }
        }

        return isInstalled;
    }

    public static boolean isServerLocalhost() {
        return "127.0.0.1".equals(APP.getConnectionSettings().server);
    }

    public static boolean isStringEmptyOrNull(final String str) {
        return str == null || str.isEmpty();
    }

    public static void notifyUser(@StringRes final int resId, final Object... format) {
        final String formattedString = APP.getResources().getString(resId, format);
        Toast.makeText(APP, formattedString, Toast.LENGTH_SHORT).show();
    }

    public static void notifyUser(@StringRes final int resId) {
        Toast.makeText(APP, resId, Toast.LENGTH_SHORT).show();
    }

    public static void notifyUser(final CharSequence message) {
        Toast.makeText(APP, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * This method sets up the connection prior to running, only if necessary.
     *
     * @param command The {@link MPDControl} command to send.
     */
    public static void runCommand(final String command) {
        if (APP.getMPD().getStatus().isValid()) {
            MPDControl.run(command);
        } else {
            final Object token = MPDControl.setupConnection(5L, TimeUnit.SECONDS);

            if (token != null) {
                MPDControl.run(command);
                APP.removeConnectionLock(token);
            }
        }
    }
}
