package org.pmix.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class MPDConnectionHandler extends BroadcastReceiver {

	private static MPDConnectionHandler instance;

	public static MPDConnectionHandler getInstance()
	{
		if(instance==null)
			instance=new MPDConnectionHandler();
		return instance;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		MPDApplication app = (MPDApplication)context.getApplicationContext();
		String action = intent.getAction();
		if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
			System.out.println("WIFI-STATE:"+intent.getAction().toString());
			System.out.println("WIFI-STATE:"+(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)));
		} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			System.out.println("NETW-STATE:"+intent.getAction().toString());
			NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			System.out.println("NETW-STATE: Connected: "+networkInfo.isConnected());
			System.out.println("NETW-STATE: Connected: "+networkInfo.getState().toString());
			
			
			if(networkInfo.isConnected())
				app.setWifiConnected(true);
			else
				app.setWifiConnected(false);
			
			
		}
		
	}
}
