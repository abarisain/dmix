package com.namelessdev.mpdroid.tools;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

public final class Tools {
	public static Object[] toObjectArray(Object... args) {
		return args;
	}

	public static void notifyUser(String message, Context context) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static boolean isHoneycombOrBetter() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
	}
}
