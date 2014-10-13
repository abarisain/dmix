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

package com.namelessdev.mpdroid.tools;

import com.namelessdev.mpdroid.MPDApplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.Toast;

import java.security.MessageDigest;
import java.util.Collection;

public final class Tools {

    private static final MPDApplication APP = MPDApplication.getInstance();

    private Tools() {
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
     * Convert byte array to hex string.
     *
     * @param data Target data array.
     * @return Hex string.
     */
    private static String convertToHex(final byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder();
        for (int byteIndex = 0; byteIndex < data.length; byteIndex++) {
            int halfByte = (data[byteIndex] >>> 4) & 0x0F;
            int twoHalves = 0;
            do {
                if ((0 <= halfByte) && (halfByte <= 9)) {
                    buffer.append((char) ('0' + halfByte));
                } else {
                    buffer.append((char) ('a' + (halfByte - 10)));
                }
                halfByte = data[byteIndex] & 0x0F;
            } while (twoHalves++ < 1);
        }

        return buffer.toString();
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

    /**
     * Gets the hash value from the specified string.
     *
     * @param value Target string value to get hash from.
     * @return the hash from string.
     */
    public static String getHashFromString(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            final MessageDigest hashEngine = MessageDigest.getInstance("MD5");
            hashEngine.update(value.getBytes("iso-8859-1"), 0, value.length());
            return convertToHex(hashEngine.digest());
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static boolean isServerLocalhost() {
        return "127.0.0.1".equals(APP.oMPDAsyncHelper.getConnectionSettings().server);
    }

    public static boolean isStringEmptyOrNull(final String str) {
        return str == null || str.isEmpty();
    }

    public static void notifyUser(@StringRes final int resId, final Object... format) {
        final String formattedString =
                MPDApplication.getInstance().getResources().getString(resId, format);
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
