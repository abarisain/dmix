package com.namelessdev.mpdroid.tools;

import android.content.Context;
import android.widget.Toast;

import com.namelessdev.mpdroid.R;

public final class Tools {
	public static Object[] toObjectArray(Object... args) {
		return args;
	}

	public static void notifyUser(String message, Context context) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static boolean isTabletMode(Context c) {
		return c.getResources().getBoolean(R.bool.isTablet);
	}
}
