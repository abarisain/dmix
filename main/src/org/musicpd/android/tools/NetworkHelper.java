package org.musicpd.android.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkHelper {

	public static boolean isNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null)
			return false;
		return (cm.getActiveNetworkInfo().isAvailable() && cm
				.getActiveNetworkInfo().isConnected());
	}

	public static Boolean isLocalNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) {
			return false;
		}
		NetworkInfo netWorkinfo = cm.getActiveNetworkInfo();
		return (netWorkinfo != null
				&& (netWorkinfo.getType() == ConnectivityManager.TYPE_WIFI || netWorkinfo
						.getType() == ConnectivityManager.TYPE_ETHERNET) && netWorkinfo
					.isConnected());
	}
}
