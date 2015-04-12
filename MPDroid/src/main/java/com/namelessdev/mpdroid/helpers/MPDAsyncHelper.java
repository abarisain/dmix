/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.tools.WeakLinkedList;

import android.os.Handler;
import android.os.Message;

import java.util.Collection;

/**
 * This Class implements the whole MPD Communication as a thread. It also "translates" the monitor
 * event of the JMPDComm Library back to the GUI-Thread, and allows to execute custom commands,
 * asynchronously.
 *
 * @author sag
 */
public class MPDAsyncHelper implements Handler.Callback {

    private static final int LOCAL_UID = 600;

    static final int EVENT_SET_USE_CACHE = LOCAL_UID + 1;

    private static final String TAG = "MPDAsyncHelper";

    private final Collection<ConnectionInfoListener> mConnectionInfoListeners;

    private final Handler mHelperHandler;

    private final MPDAsyncWorker mMPDAsyncWorker;

    private Handler mWorkerHandler;

    public MPDAsyncHelper() {
        super();

        mHelperHandler = new Handler(this);
        mMPDAsyncWorker = new MPDAsyncWorker(mHelperHandler);
        mConnectionInfoListeners = new WeakLinkedList<>("ConnectionInfoListener");
    }

    public void addConnectionInfoListener(final ConnectionInfoListener listener) {
        if (!mConnectionInfoListeners.contains(listener)) {
            mConnectionInfoListeners.add(listener);
        }
    }

    /**
     * Executes a Runnable asynchronously.
     *
     * <p>Meant to use for individual long during operations. This method returns immediately and
     * provides no indication of runnable completion.</p>
     *
     * @param runnable Runnable to execute in background thread.
     * @see #execAsync(AsyncExecListener, CharSequence, Runnable)
     */
    public void execAsync(final Runnable runnable) {
        execAsync(null, null, runnable);
    }

    /**
     * Executes a Runnable asynchronously.
     *
     * <p>Meant to use for individual long during operations. This method returns immediately and
     * provides indication through the {@code listener} parameter.</p>
     *
     * @param listener The listener to callback upon completion.
     * @param token    The token key matched to the runnable value.
     * @param runnable The runnable to run.
     */
    public void execAsync(final AsyncExecListener listener, final CharSequence token,
            final Runnable runnable) {
        if (mWorkerHandler == null) {
            mWorkerHandler = mMPDAsyncWorker.startThread();
        }

        final Runnable worker = new WorkerRunnable(mHelperHandler, token, runnable, listener);
        mWorkerHandler.obtainMessage(MPDAsyncWorker.EVENT_EXEC_ASYNC, worker)
                .sendToTarget();
    }


    /**
     * This method handles Messages, which comes from the AsyncWorker. This Message handler runs in
     * the UI-Thread, and can therefore send the information back to the listeners of the matching
     * events...
     */
    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        try {
            switch (msg.what) {
                case MPDAsyncWorker.EVENT_CONNECTION_CONFIG:
                    for (final ConnectionInfoListener listener : mConnectionInfoListeners) {
                        listener.onConnectionConfigChange((ConnectionInfo) msg.obj);
                    }
                    break;
                case MPDAsyncWorker.EVENT_EXEC_ASYNC_FINISHED:
                    final WorkerRunnable run = (WorkerRunnable) msg.obj;
                    run.getListener().asyncComplete(run.getToken());
                    break;
                default:
                    result = false;
                    break;
            }
        } catch (final ClassCastException ignored) {
            // happens when unknown message type is received
        }

        return result;
    }

    public void removeConnectionInfoListener(final ConnectionInfoListener listener) {
        mConnectionInfoListeners.remove(listener);
    }

    /**
     * Updates the connection settings.
     *
     * <p>If the connection settings have changed, the results will call back as well.</p>
     *
     * @return The updated connection settings.
     */
    public ConnectionInfo updateConnectionSettings() {
        return mMPDAsyncWorker.updateConnectionSettings();
    }

    public interface AsyncExecListener {

        void asyncComplete(final CharSequence token);
    }

    public interface ConnectionInfoListener {

        void onConnectionConfigChange(ConnectionInfo connectionInfo);
    }
}
