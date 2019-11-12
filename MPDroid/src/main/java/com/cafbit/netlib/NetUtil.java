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
package com.cafbit.netlib;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Various Android network utility methods
 * @author simmons
 */
public class NetUtil {
    
    private static final String TAG = "NetLib";
    private WifiManager wifiManager;
    
    public static class NetInfoException extends Exception {
        private static final long serialVersionUID = 5543786811674326615L;
        public NetInfoException() {}
        public NetInfoException(String message) {
            super(message);
        }
        public NetInfoException(Throwable e) {
            super(e);
        }
        public NetInfoException(String message, Throwable e) {
            super(message, e);
        }
    }
    
    public static class InterfaceInfo {
        private NetworkInterface networkInterface;
        private List<InetAddress> addresses;
        private int flags = 0;
        public static final int NET_ETHERNET  = 1<<2;
        public static final int NET_LOCALHOST = 1<<0;
        public static final int NET_OTHER     = 1<<3;
        public static final int NET_WIFI      = 1<<1;
        
        public InterfaceInfo(NetworkInterface networkInterface, List<InetAddress> addresses, int flags) {
            this.networkInterface = networkInterface;
            this.addresses = addresses;
            this.flags = flags;
        }
        
        public NetworkInterface getNetworkInterface() {
            return networkInterface;
        }
        public List<InetAddress> getAddresses() {
            return addresses;
        }
        public int getFlags() {
            return flags;
        }
        public boolean isLocalhost() {
            return ((flags & NET_LOCALHOST) != 0);
        }
        public boolean isWifi() {
            return ((flags & NET_WIFI) != 0);
        }
        public boolean isEthernet() {
            return ((flags & NET_ETHERNET) != 0);
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("interface "+networkInterface+" :");
            if ((flags & NET_LOCALHOST)!=0) { sb.append(" localhost"); }
            if ((flags & NET_WIFI)!=0) { sb.append(" wifi"); }
            if ((flags & NET_ETHERNET)!=0) { sb.append(" ethernet"); }
            sb.append("\n");
            for (InetAddress address : addresses) {
                sb.append("  addr "+address.toString()+"\n");
            }
            return sb.toString();
        }
        
    }
    
    public NetUtil(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }
    
    public WifiManager getWifiManager() {
        return wifiManager;
    }   

    public static Set<InetAddress> getLocalAddresses() {
        Set<InetAddress> addresses = new HashSet<InetAddress>();
        
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            Log.v(TAG, "getNetworkInterfaces(): "+e.getMessage(), e);
            return null;
        }
        
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addressEnum = networkInterface.getInetAddresses();
            while (addressEnum.hasMoreElements()) {
                addresses.add(addressEnum.nextElement());
            }
        }

        return addresses;
    }
    
    public List<InterfaceInfo> getNetworkInformation() throws NetInfoException {
        List<InterfaceInfo> interfaceList = new ArrayList<InterfaceInfo>();
        
        InetAddress wifiAddress = null;
        InetAddress reversedWifiAddress = null;
        if (wifiManager.isWifiEnabled()) {
            // get the ip address of the wifi interface
            int rawAddress = wifiManager.getConnectionInfo().getIpAddress();
            try {
                wifiAddress = InetAddress.getByAddress(new byte[] {
                    (byte) ((rawAddress >> 0) & 0xFF),
                    (byte) ((rawAddress >> 8) & 0xFF),
                    (byte) ((rawAddress >> 16) & 0xFF),
                    (byte) ((rawAddress >> 24) & 0xFF),
                });
                // It's unclear how to interpret the byte order
                // of the WifiInfo.getIpAddress() int value, so
                // we also compare with the reverse order.  The
                // result is probably consistent with ByteOrder.nativeOrder(),
                // but we don't know for certain since there's no documentation.
                reversedWifiAddress = InetAddress.getByAddress(new byte[] {
                    (byte) ((rawAddress >> 24) & 0xFF),
                    (byte) ((rawAddress >> 16) & 0xFF),
                    (byte) ((rawAddress >> 8) & 0xFF),
                    (byte) ((rawAddress >> 0) & 0xFF),
                });
            } catch (UnknownHostException e) {
                throw new NetInfoException("problem retreiving wifi ip address", e);
            }
        }
        
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (Exception e) {
            throw new NetInfoException("cannot determine the localhost address", e);
        }

        // get a list of all network interfaces
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new NetInfoException("problem getting net interfaces", e);
        }

        // find the wifi network interface based on the ip address
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            int flags = 0;
            Enumeration<InetAddress> addressEnum = networkInterface.getInetAddresses();
            List<InetAddress> addresses = new ArrayList<InetAddress>();
            while (addressEnum.hasMoreElements()) {
                InetAddress address = addressEnum.nextElement();

                // check for localhost
                if (address.equals(localhost)) {
                    flags |= InterfaceInfo.NET_LOCALHOST;
                }
                
                // check for wifi
                if ( (wifiAddress != null) &&
                     (reversedWifiAddress != null) &&
                     (address.equals(wifiAddress) || address.equals(reversedWifiAddress))
                ) {
                    flags |= InterfaceInfo.NET_WIFI;
                }
                
                addresses.add(address);
            }
            
            // assume an eth* interface that isn't wifi is wired ethernet.
            if (((flags & InterfaceInfo.NET_WIFI)==0) && networkInterface.getName().startsWith("eth")) {
                flags |= InterfaceInfo.NET_ETHERNET;
            }

            interfaceList.add(new InterfaceInfo(networkInterface, addresses, flags));
        }
        return interfaceList;
    }
    
    public NetworkInterface getFirstWifiInterface() {
        try {
            for (InterfaceInfo ii : getNetworkInformation()) {
                if (ii.isWifi()) {
                    return ii.getNetworkInterface();
                }
            }
        } catch (NetInfoException e) {
            Log.w(TAG, "cannot find a wifi interface");
        }
        return null;
    }

    public NetworkInterface getFirstWifiOrEthernetInterface() {
        try {
            for (InterfaceInfo ii : getNetworkInformation()) {
                if (ii.isWifi() || ii.isEthernet()) {
                    return ii.getNetworkInterface();
                }
            }
        } catch (NetInfoException e) {
            Log.w(TAG, "cannot find a wifi/ethernet interface");
        }
        return null;
    }


}
