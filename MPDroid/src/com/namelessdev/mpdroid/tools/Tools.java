package com.namelessdev.mpdroid.tools;

import java.security.MessageDigest;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.widget.Toast;

public final class Tools {
	public static Object[] toObjectArray(Object... args) {
		return args;
	}

	public static void notifyUser(String message, Context context) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Gets the hash value from the specified string.
	 * 
	 * @param value
	 *            Target string value to get hash from.
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
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Convert byte array to hex string.
	 * 
	 * @param data
	 *            Target data array.
	 * @return Hex string.
	 */
	private static final String convertToHex(byte[] data) {
		if (data == null || data.length == 0) {
			return null;
		}

		final StringBuffer buffer = new StringBuffer();
		for (int byteIndex = 0; byteIndex < data.length; byteIndex++) {
			int halfbyte = (data[byteIndex] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buffer.append((char) ('0' + halfbyte));
				else
					buffer.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[byteIndex] & 0x0F;
			} while (two_halfs++ < 1);
		}

		return buffer.toString();
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight, boolean resizePerfectlty) {

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
			final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, true);
			bitmap.recycle();
			return scaledBitmap;
		} else {
			return bitmap;
		}
	}

	public static float convertDpToPixel(float dp, Context context) {
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float px = dp * (metrics.densityDpi / 160f);
		return px;
	}

	public static boolean isStringEmptyOrNull(String str) {
		return (str == null || "".equals(str));
	}
}
