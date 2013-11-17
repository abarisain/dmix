package com.namelessdev.mpdroid.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkHelper {

    public static final int MAX_RETRY = 5;

    public static boolean isNetworkConnected(Context context) {
        return isNetworkConnected(context, new int[0]);
    }

    public static Boolean isLocalNetworkConnected(Context context) {
        return isNetworkConnected(context, new int[]{ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_ETHERNET});
    }

    public static Boolean isNetworkConnected(Context context, int... allowedNetworkTypes) {

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            if (innerIsNetworkConnected(context, allowedNetworkTypes)) {
                return true;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //Nothing to do
                }
            }
        }
        return false;
    }

    public static Boolean innerIsNetworkConnected(Context context, int... allowedNetworkTypes) {

        NetworkInfo networkInfo;
        boolean networkConnected = false;
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            networkConnected = false;
        } else {
            networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo == null) {
                networkConnected = false;
            } else if (!networkInfo.isAvailable() || !networkInfo.isConnected()) {
                networkConnected = false;
            } else if (allowedNetworkTypes.length == 0) {
                networkConnected = true;
            } else {
                for (int networkType = 0; networkType < allowedNetworkTypes.length && !networkConnected; networkType++) {
                    if (allowedNetworkTypes[networkType] == networkInfo.getType()) {
                        networkConnected = true;
                    }
                }
            }
        }
        return networkConnected;
    }
}
