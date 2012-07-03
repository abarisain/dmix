package com.namelessdev.mpdroid.tools;

import android.content.Context;
import android.widget.Toast;

public final class Tools {
	public static Object[] toObjectArray(Object... args) {
		return args;
	}

	public static void notifyUser(String message, Context context) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
}
