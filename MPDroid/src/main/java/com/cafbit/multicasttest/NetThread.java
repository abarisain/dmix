/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/entry/testing_multicast_support_on_android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cafbit.multicasttest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Set;

import com.cafbit.netlib.NetUtil;
import com.cafbit.netlib.dns.DNSMessage;

import android.content.Context;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

/**
 * This thread runs in the background while the user has our
 * program in the foreground, and handles sending mDNS queries
 * and processing incoming mDNS packets.
 * @author simmons
 */
public class NetThread extends Thread {

    private static final String TAG = "NetThread";
    
    // the standard mDNS multicast address and port number
    private static final byte[] MDNS_ADDR =
        new byte[] {(byte) 224,(byte) 0,(byte) 0,(byte) 251};
    private static final int MDNS_PORT = 5353;

    private static final int BUFFER_SIZE = 4096;

    private NetworkInterface networkInterface;
    private InetAddress groupAddress;
    private MulticastSocket multicastSocket = null;
    private NetUtil netUtil;
    private String servicename;

    /**
     * Construct the network thread.
     * @param activity
     * @param servicename Name of service to search for
     */
    public NetThread(Context activity, String servicename) {
        super("net");
        this.servicename = servicename;
        netUtil = new NetUtil(activity);
    }
    
    /**
     * Open a multicast socket on the mDNS address and port.
     * @throws IOException
     */
    private void openSocket() throws IOException {
        multicastSocket = new MulticastSocket(MDNS_PORT);
        multicastSocket.setTimeToLive(2);
        multicastSocket.setSoTimeout(5000);
        multicastSocket.setReuseAddress(true);
        multicastSocket.setNetworkInterface(networkInterface);
        multicastSocket.joinGroup(groupAddress);
    }

    /**
     */
    @Override
    public void run() {
        Log.v(TAG, "starting network thread");

        Set<InetAddress> localAddresses = NetUtil.getLocalAddresses();
        MulticastLock multicastLock = null;
        
        try {
            networkInterface = netUtil.getFirstWifiOrEthernetInterface();
            if (networkInterface == null) {
                throw new IOException("Your WiFi is not enabled.");
            }
            groupAddress = InetAddress.getByAddress(MDNS_ADDR); 

            multicastLock = netUtil.getWifiManager().createMulticastLock("unmote");
            multicastLock.acquire();
            Log.v(TAG, "acquired multicast lock: "+multicastLock);

            openSocket();
            Log.v(TAG, "opensocket returned");

            query(servicename);
            Log.v(TAG, "sent query");

            // set up the buffer for incoming packets
            byte[] responseBuffer = new byte[BUFFER_SIZE];
            DatagramPacket response = new DatagramPacket(responseBuffer, BUFFER_SIZE);
            multicastSocket.receive(response);
            Log.v(TAG, "received response"+response.getSocketAddress());
            multicastSocket.close();
        } catch (IOException e1) {
            Log.v(TAG, "send mdns query failed "+e1.toString());
            return;
        }

        if(multicastSocket != null)
            multicastSocket.close();

        // release the multicast lock
        if(multicastLock != null)
            multicastLock.release();

        Log.v(TAG, "stopping network thread");
    }
    
    /**
     * Transmit an mDNS query on the local network.
     * @param servicename
     * @throws IOException
     */
    private void query(String servicename) throws IOException {
        byte[] requestData = (new DNSMessage(servicename)).serialize();
        DatagramPacket request =
            new DatagramPacket(requestData, requestData.length, InetAddress.getByAddress(MDNS_ADDR), MDNS_PORT);
        multicastSocket.send(request);
    }
}
