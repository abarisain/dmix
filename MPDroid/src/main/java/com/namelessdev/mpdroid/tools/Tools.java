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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.Collection;

public final class Tools {

    private static final MPDApplication APP = MPDApplication.getInstance();

    private Tools() {
        super();
    }

    public static int calculateInSampleSize(final BitmapFactory.Options options, final int reqWidth,
            final int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
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
                    final int eventKeyCode = event.getKeyCode();

                    stringBuilder.append(", with keycode: ");
                    stringBuilder.append(eventKeyCode);
                }
            }
        }

        stringBuilder.append(". }");
        return stringBuilder.toString();
    }

    public static Bitmap decodeSampledBitmapFromBytes(
            final byte[] bytes, final int reqWidth, final int reqHeight,
            final boolean resizePerfectly) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (resizePerfectly) {
            final Bitmap scaledBitmap = Bitmap
                    .createScaledBitmap(bitmap, reqWidth, reqHeight, true);
            bitmap.recycle();
            return scaledBitmap;
        } else {
            return bitmap;
        }
    }

    public static Bitmap decodeSampledBitmapFromPath(
            final String path, final int reqWidth, final int reqHeight,
            final boolean resizePerfectlty) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        final Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (resizePerfectlty) {
            final Bitmap scaledBitmap = Bitmap
                    .createScaledBitmap(bitmap, reqWidth, reqHeight, true);
            bitmap.recycle();
            return scaledBitmap;
        } else {
            return bitmap;
        }
    }

    public static boolean isServerLocalhost() {
        return "127.0.0.1".equals(APP.oMPDAsyncHelper.getConnectionSettings().server);
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

    public static int[] toIntArray(final Collection<Integer> list) {
        final int[] ret = new int[list.size()];
        int i = 0;
        for (final Integer e : list) {
            ret[i++] = e.intValue();
        }
        return ret;
    }

    public static Object[] toObjectArray(final Object... args) {
        return args;
    }

}
