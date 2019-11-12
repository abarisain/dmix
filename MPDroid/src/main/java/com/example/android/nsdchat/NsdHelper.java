/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Adapted from NsdHelper.java in the NsdChat demo application by
// Patrick Wood

package com.example.android.nsdchat;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NsdHelper {

    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private Handler mUpdateHandler;
    private ConcurrentLinkedQueue<NsdServiceInfo>mQueue;

    private  String mservice_type = "_workstation._tcp.";

    private  static final String TAG = "NsdHelper";

    public NsdHelper(Context context, Handler handler) {
        mUpdateHandler = handler;
        mQueue = new ConcurrentLinkedQueue<NsdServiceInfo>();
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd() {
        initializeResolveListener();
    }

    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started: " + regType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success: " + service);
                if(mQueue.isEmpty())
                    mNsdManager.resolveService(service, mResolveListener);
                // head of queue is currently resolving service
                mQueue.add(service);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mDiscoveryListener = null;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: Error code:" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo service) {
                Log.e(TAG, "Resolve Succeeded. " + service);

                Bundle messageBundle = new Bundle();
                messageBundle.putString("name", service.getServiceName());
                messageBundle.putByteArray("address", service.getHost().getAddress());
                messageBundle.putInt("port", service.getPort());
                Message message = new Message();
                message.setData(messageBundle);
                mUpdateHandler.sendMessage(message);

                // pop active resolution
                mQueue.poll();
                if(!mQueue.isEmpty())
                    mNsdManager.resolveService(mQueue.peek(), mResolveListener);
            }
        };
    }

    public void discoverServices(String Service) {
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();
        if(Service != null)
            mservice_type = Service;
        mNsdManager.discoverServices(mservice_type, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
            }
            mDiscoveryListener = null;
        }
    }

    public void tearDown() {
    }
}
