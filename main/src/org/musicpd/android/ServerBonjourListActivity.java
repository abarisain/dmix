package org.musicpd.android;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
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
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.SettingsHelper;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
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
    	
		servers = new ArrayList<Map<String,String>>() {
			public boolean add(Map<String,String> mt) {
		        int index = Collections.binarySearch(this, mt, new java.util.Comparator<Map<String,String>>() {
					public int compare(Map<String, String> lhs, Map<String, String> rhs) {
						return lhs.get(SERVER_NAME).compareTo(rhs.get(SERVER_NAME));
					}
		        });
		        if (index < 0) index = ~index;
		        super.add(index, mt);
		        return true;
		    }
		};

    	//By default, the android wifi stack will ignore broadcasts, fix that
    	WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    	multicastLock = wm.createMulticastLock("mupeace_bonjour");
    	
    	try {
			jmdns = JmDNS.create();
			jmdns.addServiceListener("_mpd._tcp.local.", this);
		} catch (IOException e) {
			Log.w(e);
		}
		
		listAdapter = new SimpleAdapter(this, servers, android.R.layout.simple_list_item_1, new String[]{SERVER_NAME}, new int[]{android.R.id.text1});
		getListView().setAdapter(listAdapter);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		setTitle(R.string.servers);

		processAddedServices();
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
		threadAddingServers.interrupt();
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
		processAddedServices();
    }
    
    @Override
    protected void onDestroy() {
    	if(jmdns == null) {
    		super.onDestroy();
    		return;
    	}
    	if (threadAddingServers != null)
    		threadAddingServers.interrupt();
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
		Log.i("Addresses: "+TextUtils.join(", ", addresses));
    	for (InetAddress address : addresses) {
    		if (Inet4Address.class.isInstance(address) && !address.isMulticastAddress())
    			return address.getHostAddress();
    	}
    	for (InetAddress address : addresses) {
			return address.getHostAddress();
    	}
    	return null;
    }
    
    java.util.concurrent.LinkedBlockingQueue<ServiceEvent> servicesAdded = new java.util.concurrent.LinkedBlockingQueue<ServiceEvent>();

	@Override
	public void serviceAdded(ServiceEvent event) {
		servicesAdded.add(event);
	}

	Thread threadAddingServers;
	public void processAddedServices() {
		threadAddingServers = new Thread(new Runnable() {
			public void run() {
				while(true)
					try {
						ServiceEvent event = servicesAdded.take();
						final List<Map<String,String>> serversAdded = new java.util.LinkedList<Map<String,String>>();
						do {
							ServiceInfo info = event.getDNS().getServiceInfo(event.getType(),
									event.getName());
							Log.i("Service added: " + event.getName() + (info == null ? null : " resolved"));
							if (info != null)
							{
								String address = chooseAddress(info.getInetAddresses());
								Log.i("Address:   " + address);
								if(address != null) {
									final Map<String, String> server = new HashMap<String, String>();
									server.put(SERVER_NAME, info.getName());
									server.put(SERVER_IP, address);
									server.put(SERVER_PORT, Integer.toString(info.getPort()));
									serversAdded.add(server);
								}
							}
						} while ((event = servicesAdded.poll()) != null);
						runOnUiThread(new Runnable() {
						    public void run() {
								synchronized(servers) {
									for(Map<String,String> server : serversAdded)
										servers.add(server);
								}
								listAdapter.notifyDataSetChanged();
						    }
						});
					}
					catch(InterruptedException e) {}
					catch(Exception e) { try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
					} }
			}
		});
		threadAddingServers.start();
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		String name = event.getName();
		Log.i("Service removed: " + name);
		Iterator<Map<String, String>> i = servers.iterator();
		while (i.hasNext()) {
			if (i.next().get(SERVER_NAME).equals(name)) {
				i.remove();
			}
		}
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
	}
}
