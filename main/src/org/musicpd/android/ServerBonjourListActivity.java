package org.musicpd.android;

import java.io.IOException;
import java.net.Inet4Address;
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

import org.a0z.mpd.MPD;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.musicpd.android.helpers.MPDAsyncHelper;
import org.musicpd.android.tools.SettingsHelper;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ServerBonjourListActivity extends SherlockListActivity implements ServiceListener {
	
	private static final String SERVER_NAME = "server_name";
	private static final String SERVER_IP = "server_ip";
	private static final String SERVER_PORT = "server_port";
	
	//The multicast lock we'll have to release
	private WifiManager.MulticastLock multicastLock = null;
	private JmDNS jmdns = null;
	private List<Map<String,String>> servers = null;
	private SimpleAdapter listAdapter = null;
	SettingsHelper settings;
	MPDAsyncHelper oMPDAsyncHelper;
  
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	final MPDApplication app = (MPDApplication) getApplicationContext();
    	settings = new SettingsHelper(app, oMPDAsyncHelper = app.oMPDAsyncHelper);
    	
    	servers = new ArrayList<Map<String,String>>();
    	
    	//By default, the android wifi stack will ignore broadcasts, fix that
    	WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    	multicastLock = wm.createMulticastLock("mupeace_bonjour");
    	
    	try {
			jmdns = JmDNS.create();
			jmdns.addServiceListener("_mpd._tcp.local.", this);
		} catch (IOException e) {
			//Do nothing, stuff will just not work
		}
		
		listAdapter = new SimpleAdapter(this, servers, android.R.layout.simple_list_item_1, new String[]{SERVER_NAME}, new int[]{android.R.id.text1});
		getListView().setAdapter(listAdapter);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		setTitle(R.string.servers);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.mpd_servermenu, menu);
		return true;
	}
	
	public static final int SETTINGS = 5;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent i = null;

		// Handle item selection
		switch (item.getItemId()) {
			case R.id.GMM_Settings:
				i = new Intent(this, WifiConnectionSettings.class);
				startActivityForResult(i, SETTINGS);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
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
    protected void onListItemClick (ListView l, View v, int position, long id) {
    	settings.setHostname(
			servers
			.get(position)
			.get(SERVER_IP)
		);
    	oMPDAsyncHelper.disconnect();
    	finish();
    }
    
    String chooseAddress(InetAddress[] addresses) {
    	for (InetAddress address : addresses) {
    		if (Inet4Address.class.isInstance(address) && !address.isMulticastAddress())
    			return address.getHostAddress();
    	}
    	for (InetAddress address : addresses) {
			return address.getHostAddress();
    	}
    	return null;
    }
    
	@Override
	public void serviceAdded(ServiceEvent event) {
		ServiceInfo info = event.getDNS().getServiceInfo(event.getType(),
				event.getName());
		String address = chooseAddress(info.getInetAddresses());
		if(address != null) {
			final Map<String, String> server = new HashMap<String, String>();
			server.put(SERVER_NAME, info.getName());
			server.put(SERVER_IP, address);
			server.put(SERVER_PORT, Integer.toString(info.getPort()));
			runOnUiThread(new Runnable() {
			    public void run() {
					servers.add(server);
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
