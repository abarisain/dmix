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

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.event.StatusChangeListener;
import com.anpmech.mpd.event.TrackPositionListener;
import com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor;
import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.tools.SettingsHelper;
import com.namelessdev.mpdroid.tools.WeakLinkedList;

import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

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

    private static int sJobID = 0;

    public final MPD oMPD;

    private final Collection<AsyncExecListener> mAsyncExecListeners;

    private final Collection<ConnectionInfoListener> mConnectionInfoListeners;

    private final IdleSubsystemMonitor mIdleSubsystemMonitor;

    private final MPDAsyncWorker mMPDAsyncWorker;

    private final Handler mWorkerHandler;

    private ConnectionInfo mConnectionInfo = new ConnectionInfo();

    public MPDAsyncHelper() {
        this(PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance())
                .getBoolean(MPDAsyncWorker.USE_LOCAL_ALBUM_CACHE_KEY, false));
    }

    /**
     * Private constructor for static class
     */
    public MPDAsyncHelper(final boolean cached) {
        super();
        oMPD = new CachedMPD(cached);

        mIdleSubsystemMonitor = new IdleSubsystemMonitor(oMPD);
        mMPDAsyncWorker = new MPDAsyncWorker(new Handler(this), oMPD);
        mWorkerHandler = mMPDAsyncWorker.startThread();
        new SettingsHelper(this).updateConnectionSettings();

        mAsyncExecListeners = new WeakLinkedList<>("AsyncExecListener");
        mConnectionInfoListeners = new WeakLinkedList<>("ConnectionInfoListener");
    }

    public void addAsyncExecListener(final AsyncExecListener listener) {
        if (!mAsyncExecListeners.contains(listener)) {
            mAsyncExecListeners.add(listener);
        }
    }

    public void addConnectionInfoListener(final ConnectionInfoListener listener) {
        if (!mConnectionInfoListeners.contains(listener)) {
            mConnectionInfoListeners.add(listener);
        }
    }

    /**
     * Adds a {@link com.anpmech.mpd.event.StatusChangeListener} from the associated
     * {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     *
     * @param listener The {@link com.anpmech.mpd.event.StatusChangeListener} to add for
     *                 notification for the
     *                 {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     */
    public void addStatusChangeListener(final StatusChangeListener listener) {
        mIdleSubsystemMonitor.addStatusChangeListener(listener);
    }

    /**
     * Adds a {@link com.anpmech.mpd.event.TrackPositionListener} from the associated
     * {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     *
     * @param listener The {@link com.anpmech.mpd.event.TrackPositionListener} to add for
     *                 notification for the
     *                 {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     */
    public void addTrackPositionListener(final TrackPositionListener listener) {
        mIdleSubsystemMonitor.addTrackPositionListener(listener);
    }

    public void connect() {
        /**
         * Because the Handler queue can be incredibly slow.
         */
        mWorkerHandler.sendMessageAtFrontOfQueue(
                Message.obtain(mWorkerHandler, MPDAsyncWorker.EVENT_CONNECT));
    }

    public void disconnect() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_DISCONNECT);
    }

    /**
     * Executes a Runnable Asynchronous. Meant to use for individual long during operations on
     * JMPDComm. Use this method only, when the code to execute is only used once in the project.
     * If it's use more than once, implement individual events and listener in this class.
     *
     * @param run Runnable to execute in background thread.
     * @return JobID, which is brought back with the AsyncExecListener interface.
     */
    public int execAsync(final Runnable run) {
        final int activeJobID = sJobID;
        sJobID++;
        mWorkerHandler.obtainMessage(MPDAsyncWorker.EVENT_EXEC_ASYNC, activeJobID, 0, run)
                .sendToTarget();
        return activeJobID;
    }

    /**
     * Get the current {@code ConnectionInfo} object.
     *
     * @return A current {@code ConnectionInfo} object.
     */
    public ConnectionInfo getConnectionSettings() {
        return mConnectionInfo;
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
            final Object[] args = (Object[]) msg.obj;
            switch (msg.what) {
                case MPDAsyncWorker.EVENT_CONNECTION_CONFIG:
                    mConnectionInfo = (ConnectionInfo) args[0];
                    for (final ConnectionInfoListener listener : mConnectionInfoListeners) {
                        listener.onConnectionConfigChange(mConnectionInfo);
                    }
                    break;
                case MPDAsyncWorker.EVENT_EXEC_ASYNC_FINISHED:
                    // Asynchronous operation finished, call the listeners and supply the JobID...
                    for (final AsyncExecListener listener : mAsyncExecListeners) {
                        if (listener != null) {
                            listener.asyncExecSucceeded(msg.arg1);
                        }
                    }
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

    /**
     * Checks to see if the {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor} is
     * active.
     *
     * @return True if the {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor} is active,
     * false otherwise.
     */
    public boolean isStatusMonitorAlive() {
        return !mIdleSubsystemMonitor.isStopped();
    }

    public void removeAsyncExecListener(final AsyncExecListener listener) {
        mAsyncExecListeners.remove(listener);
    }

    public void removeConnectionInfoListener(final ConnectionInfoListener listener) {
        mConnectionInfoListeners.remove(listener);
    }

    /**
     * Removes a {@link com.anpmech.mpd.event.StatusChangeListener} from the associated
     * {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     *
     * @param listener The {@link com.anpmech.mpd.event.StatusChangeListener} to remove from
     *                 notification for the
     *                 {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     */
    public void removeStatusChangeListener(final StatusChangeListener listener) {
        mIdleSubsystemMonitor.removeStatusChangeListener(listener);
    }

    /**
     * Removes a {@link com.anpmech.mpd.event.TrackPositionListener} from the associated
     * {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     *
     * @param listener The {@link com.anpmech.mpd.event.TrackPositionListener} to remove from
     *                 notification for the
     *                 {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     */
    public void removeTrackPositionListener(final TrackPositionListener listener) {
        mIdleSubsystemMonitor.removeTrackPositionListener(listener);
    }

    /**
     * Stores the {@code ConnectionInfo} object and sends it to the worker.
     *
     * @param connectionInfo A current {@code ConnectionInfo} object.
     */
    public void setConnectionSettings(final ConnectionInfo connectionInfo) {
        mConnectionInfo = connectionInfo;
        mMPDAsyncWorker.setConnectionSettings(connectionInfo);
    }

    public void setUseCache(final boolean useCache) {
        ((CachedMPD) oMPD).setUseCache(useCache);
    }

    /**
     * Starts the associated {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     *
     * @param idleSubsystems The subsystems to track in the associated
     *                       {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     */
    public void startIdleMonitor(final String[] idleSubsystems) {
        mIdleSubsystemMonitor.setSupportedSubsystems(idleSubsystems);
        mIdleSubsystemMonitor.start();
    }

    /**
     * Stops the associated {@link com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor}.
     */
    public void stopIdleMonitor() {
        mIdleSubsystemMonitor.stop();
    }

    // Interface for callback when Asynchronous operations are finished
    public interface AsyncExecListener {

        void asyncExecSucceeded(int jobID);
    }

    public interface ConnectionInfoListener {

        void onConnectionConfigChange(ConnectionInfo connectionInfo);
    }
}
