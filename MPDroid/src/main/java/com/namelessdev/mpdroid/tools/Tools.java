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
import com.namelessdev.mpdroid.R;

import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.Toast;

import java.security.MessageDigest;
import java.util.List;

public final class Tools {

    private static final MPDApplication app = MPDApplication.getInstance();

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
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
    private static String convertToHex(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        final StringBuffer buffer = new StringBuffer();
        for (int byteIndex = 0; byteIndex < data.length; byteIndex++) {
            int halfbyte = (data[byteIndex] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buffer.append((char) ('0' + halfbyte));
                } else {
                    buffer.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[byteIndex] & 0x0F;
            } while (two_halfs++ < 1);
        }

        return buffer.toString();
    }

    public static Bitmap decodeSampledBitmapFromBytes(byte[] bytes, int reqWidth, int reqHeight,
            boolean resizePerfectly) {

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

    public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight,
            boolean resizePerfectlty) {

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
    public static final String getHashFromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }

        try {
            MessageDigest hashEngine = MessageDigest.getInstance("MD5");
            hashEngine.update(value.getBytes("iso-8859-1"), 0, value.length());
            return convertToHex(hashEngine.digest());
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static String getMainText(final Artist artist) {
        final String result;

        if (artist.isUnknown()) {
            result = app.getResources().getString(R.string.unknown_metadata_artist);
        } else {
            result = artist.mainText();
        }

        return result;
    }

    public static String getMainText(final Album album) {
        final String result;

        if (album.isUnknown()) {
            result = app.getResources().getString(R.string.unknown_metadata_album);
        } else {
            result = album.mainText();
        }

        return result;
    }


    public static boolean isServerLocalhost() {
        return "127.0.0.1".equals(app.oMPDAsyncHelper.getConnectionSettings().server);
    }

    public static boolean isStringEmptyOrNull(String str) {
        return (str == null || "".equals(str));
    }

    public static void notifyUser(final int resId, final Object... format) {
        final String formattedString =
                MPDApplication.getInstance().getResources().getString(resId, format);
        Toast.makeText(app, formattedString, Toast.LENGTH_SHORT).show();
    }

    public static void notifyUser(final int resId) {
        Toast.makeText(app, resId, Toast.LENGTH_SHORT).show();
    }

    public static void notifyUser(final CharSequence message) {
        Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
    }

    public static int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list) {
            ret[i++] = e.intValue();
        }
        return ret;
    }

    public static Object[] toObjectArray(Object... args) {
        return args;
    }

}
