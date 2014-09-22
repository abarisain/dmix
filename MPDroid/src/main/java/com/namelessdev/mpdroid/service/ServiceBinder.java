/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.format.DateUtils;
import android.util.Log;

/**
 * A helper class to simplify binding and messaging with the service.
 */
public class ServiceBinder implements
        Handler.Callback,
        ServiceConnection {

    /** Length of time to give a message prior to Messenger termination. */
    public static final long MESSAGE_DELAY = 15000L;

    /** This is the class unique Binder identifier. */
    static final int LOCAL_UID = 100;

    /** Message sent to the client upon successful connection. */
    public static final int CONNECTED = LOCAL_UID + 1;

    /** Message sent to the client upon disconnection. */
    public static final int DISCONNECTED = LOCAL_UID + 2;

    /** Used as an argument in a what/boolean pair message. */
    public static final int TRUE = LOCAL_UID + 3;

    /** Used as an argument in a what/boolean pair message. */
    public static final int FALSE = LOCAL_UID + 4;

    /** Sent to the service upon successful connection. */
    static final int REGISTER_CLIENT = LOCAL_UID + 5;

    /** Sent to the service before unbind. */
    static final int UNREGISTER_CLIENT = LOCAL_UID + 6;

    /** Handled by the client handler to set persistent what/bool set. */
    public static final int SET_PERSISTENT = LOCAL_UID + 7;

    /** Turns on debugging messages. */
    private static final boolean DEBUG = MPDroidService.DEBUG;

    private static final String TAG = "ServiceBinder";

    /** The application context of the client. */
    private final Context mClientContext;

    /** The handler which processes messages on behalf of the client of this class. */
    private final Handler mClientHandler;

    /** The messenger for the ServiceBinder client. */
    private final Messenger mClientMessenger;

    /** Local initiating bind and service persistence. */
    private final Intent mIntent;

    /** Local handler used only for local (usually delayed) messaging. */
    private final Handler mLocalHandler;

    /** If service and bind are persistent. */
    private boolean mIsPersistent = false;

    /** The message to send on successful connection. */
    private Message mMessageOnConnection = null;

    /** The messenger from the service used for two way service communication. */
    private Messenger mServiceMessenger = null;

    /**
     * Constructs the service binder helper with a message receiving client.
     *
     * @param clientContext Context of the client.
     * @param clientHandler The handler where service messages will be delivered.
     */
    public ServiceBinder(final Context clientContext, final Handler clientHandler) {
        super();

        mClientContext = clientContext;
        mClientHandler = clientHandler;
        mClientMessenger = new Messenger(clientHandler);

        mLocalHandler = new Handler(this);

        mIntent = new Intent(clientContext, MPDroidService.class);
    }

    /**
     * A utility method to convert what/bool pairs into a message.
     *
     * @param handler The handler used to generate the message.
     * @param what    The what message to send out.
     * @param isTrue  The boolean the convert.
     * @return The what/bool pair message.
     */
    static Message getBoolMessage(final Handler handler, final int what, final boolean isTrue) {
        final int active;

        if (isTrue) {
            active = TRUE;
        } else {
            active = FALSE;
        }

        return Message.obtain(handler, what, active, 0);
    }

    /**
     * A function to translate 'what' fields to literal debug name, used primarily for debugging.
     *
     * @param what A 'what' field.
     * @return The literal field name.
     */
    public static String getHandlerValue(final int what) {
        String result;

        if (what >= LOCAL_UID && what < MPDroidService.LOCAL_UID) {
            result = "ServiceBinder.";
            switch (what) {
                case CONNECTED:
                    result += "CONNECTED";
                    break;
                case DISCONNECTED:
                    result += "DISCONNECTED";
                    break;
                case FALSE:
                    result += "FALSE";
                    break;
                case REGISTER_CLIENT:
                    result += "REGISTER_CLIENT";
                    break;
                case SET_PERSISTENT:
                    result += "SET_PERSISTENT";
                    break;
                case TRUE:
                    result += "TRUE";
                    break;
                case UNREGISTER_CLIENT:
                    result += "UNREGISTER_CLIENT";
                    break;
                default:
                    result += "{unknown}: " + what;
                    break;
            }
        } else if (what >= MPDroidService.LOCAL_UID && what < NotificationHandler.LOCAL_UID) {
            result = MPDroidService.getHandlerValue(what);
        } else if (what >= NotificationHandler.LOCAL_UID && what < StreamHandler.LOCAL_UID) {
            result = NotificationHandler.getHandlerValue(what);
        } else if (what >= StreamHandler.LOCAL_UID) {
            result = StreamHandler.getHandlerValue(what);
        } else {
            result = "{unknown}: " + what;
        }

        return result;
    }

    /** Initiates our service binding, after complete, onBindService() should be called. */
    private void doBindService() {
        mClientContext.bindService(mIntent, this, Context.BIND_AUTO_CREATE);

        if (DEBUG) {
            Log.d(TAG, "Binding.");
        }
    }

    /**
     * Initiates our unbinding from the bound service, after
     * complete, onServiceDisconnected() should be called.
     */
    public void doUnbindService() {
        if (DEBUG) {
            Log.d(TAG, "doUnbindService()");
        }

        if (mServiceMessenger != null) {
            sendMessageToService(UNREGISTER_CLIENT);

            // Detach our existing connection.
            mClientContext.unbindService(this);
            mServiceMessenger = null;
        }
    }

    /**
     * Message handler for local messages only.
     *
     * @param msg Incoming local message.
     * @return Whether the message was handled.
     */
    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = false;

        if (msg.what == UNREGISTER_CLIENT && !mIsPersistent) {
            doUnbindService();
            result = true;
        }

        return result;
    }

    /**
     * Method to get server binding status.
     *
     * @return True if service is bound to this object client, false otherwise.
     */
    public final boolean isServiceBound() {
        return mServiceMessenger != null;
    }

    /**
     * The ServiceConnection callback called when a connection to the Service has been established.
     *
     * @param name    The component name of the service that has been connected.
     * @param service The communication channel to the service.
     */
    @Override
    public final void onServiceConnected(final ComponentName name, final IBinder service) {
        mServiceMessenger = new Messenger(service);

        if (!mIsPersistent) {
            setupDisconnectionDelay();
        }

        mClientHandler.sendEmptyMessage(CONNECTED);

        /** Register with the service. */
        sendMessageToService(REGISTER_CLIENT);

        if (mMessageOnConnection != null) {
            sendMessageToService(mMessageOnConnection);
            mMessageOnConnection = null;
        }

        if (DEBUG) {
            Log.d(TAG, "Attached.");
        }
    }

    /**
     * The ServiceConnection callback called when a connection to the Service has been lost.
     *
     * @param name The component name of the service whose connection has been lost.
     */
    @Override
    public final void onServiceDisconnected(final ComponentName name) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        mServiceMessenger = null;
        mClientHandler.sendEmptyMessage(DISCONNECTED);
        if (DEBUG) {
            Log.d(TAG, "Disconnected from service.");
        }
    }

    /**
     * The method which actually sends the message generated
     * by the other sendMessageToService methods.
     *
     * @param msg The message which will get sent.
     */
    private void sendMessageToService(final Message msg) {
        if (DEBUG) {
            String log = "Sending message to service: " + getHandlerValue(msg.what);
            if (msg.arg1 == TRUE) {
                log += " with bool: " + getHandlerValue(TRUE);
            } else {
                log += " with bool: " + getHandlerValue(FALSE);
            }

            Log.d(TAG, log);
        }

        msg.replyTo = mClientMessenger;

        try {
            mServiceMessenger.send(msg);
        } catch (final RemoteException e) {
            Log.w(TAG, "Failed to send message: " + msg, e);
        }
    }

    /**
     * Sends a what message to the service.
     *
     * @param what The what message to send out.
     */
    public final void sendMessageToService(final int what) {
        if (mServiceMessenger == null) {
            mMessageOnConnection = mClientHandler.obtainMessage(what);
            doBindService();
        } else {
            final Message msg = mClientHandler.obtainMessage(what);
            sendMessageToService(msg);
        }
    }

    /**
     * Sends a what/bool pair message to the service.
     *
     * @param what   The what message to send out.
     * @param isTrue The whether the 'what' message is true or false.
     */
    public final void sendMessageToService(final int what, final boolean isTrue) {
        if (mServiceMessenger == null) {
            mMessageOnConnection = getBoolMessage(mClientHandler, what, isTrue);
            doBindService();
        } else {
            final Message msg = getBoolMessage(mClientHandler, what, isTrue);
            sendMessageToService(msg);
        }
    }

    /**
     * Sends a what/bundle message to the service.
     *
     * @param what   The what message to send out.
     * @param bundle The bundle to pair with the what to send to the service.
     */
    public final void sendMessageToService(final int what, final Bundle bundle) {
        if (mServiceMessenger == null) {
            mMessageOnConnection = mClientHandler.obtainMessage(what);
            mMessageOnConnection.setData(bundle);
            doBindService();
        } else {
            final Message msg = mClientHandler.obtainMessage(what);
            msg.setData(bundle);
            sendMessageToService(msg);
        }
    }

    /**
     * This method sets the service as persistent until the service has stopped.
     *
     * @param isPersistent If true, set service as persistent, false otherwise.
     */
    public final void setServicePersistent(final boolean isPersistent) {
        if (mIsPersistent != isPersistent) {
            mIsPersistent = isPersistent;

            if (isPersistent) {
                mIntent.setAction(MPDroidService.ACTION_START);
                mClientContext.startService(mIntent);
            } else {
                setupDisconnectionDelay();
            }
        }
    }

    /** Setup unbinding after a short delay. */
    private void setupDisconnectionDelay() {
        if (DEBUG) {
            Log.d(TAG, "If not interrupted, unbinding from service in " +
                    MESSAGE_DELAY / DateUtils.SECOND_IN_MILLIS + " seconds.");
        }

        /** Set the service to disconnect after 15 seconds (should receive a reply by then). */
        mLocalHandler.removeMessages(UNREGISTER_CLIENT);
        mLocalHandler.sendEmptyMessageDelayed(UNREGISTER_CLIENT, MESSAGE_DELAY);
    }
}
