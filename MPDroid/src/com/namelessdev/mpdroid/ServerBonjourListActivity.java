package com.namelessdev.mpdroid;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.ListActivity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.SimpleAdapter;

public class ServerBonjourListActivity extends ListActivity implements ServiceListener {
	
	private static final String SERVER_NAME = "server_name";
	private static final String SERVER_IP = "server_ip";
	private static final String SERVER_PORT = "server_port";
	
	//The multicast lock we'll have to release
	private WifiManager.MulticastLock multicastLock = null;
	private JmDNS jmdns = null;
	private List<Map<String,String>> servers = null;
	private SimpleAdapter listAdapter = null;
	    	
  
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	servers = new ArrayList<Map<String,String>>();
    	
    	//By default, the android wifi stack will ignore broadcasts, fix that
    	WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    	multicastLock = wm.createMulticastLock("mpdroid_bonjour");
    	
    	try {
			jmdns = JmDNS.create();
			jmdns.addServiceListener("_mpd._tcp.local.", this);
		} catch (IOException e) {
			//Do nothing, stuff will just not work
		}
			
		listAdapter = new SimpleAdapter(this, servers, android.R.layout.simple_list_item_1, new String[]{SERVER_NAME}, new int[]{android.R.id.text1});
		getListView().setAdapter(listAdapter);
    }
    
    @Override
    protected void onPause() {
    	if(multicastLock == null || jmdns == null) {
    		super.onPause();
    		return;
    	}
    	multicastLock.release();
    	
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if(multicastLock == null || jmdns == null) {
    		return;
    	}
    	
    	//Ask android to allow us to get WiFi broadcasts
    	multicastLock.acquire();
    }
    
    @Override
    protected void onDestroy() {
    	if(jmdns == null) {
    		super.onDestroy();
    		return;
    	}
    	
    	try {
			jmdns.close();
		} catch (IOException e) {
			//Closing fails ? LIKE I GIVE A SHIT
		}
		
		super.onDestroy();
    }
    
	@Override
	public void serviceAdded(ServiceEvent event) {
		ServiceInfo info = event.getDNS().getServiceInfo(event.getType(),
				event.getName());
		InetAddress[] addresses = info.getInetAddresses();
		if(addresses[0] != null) {
			Map<String, String> server = new HashMap<String, String>();
			server.put(SERVER_NAME, info.getName());
			server.put(SERVER_IP, addresses[0].toString());
			server.put(SERVER_PORT, Integer.toString(info.getPort()));
			servers.add(server);
			runOnUiThread(new Runnable() {
			    public void run() {
			    	listAdapter.notifyDataSetChanged();
			    }
			});
			
		}
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		Iterator<Map<String, String>> i = servers.iterator();
		while (i.hasNext()) {
			if (i.next().get(SERVER_NAME).equals(event.getName())) {
				i.remove();
			}

		}
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
	}
}
