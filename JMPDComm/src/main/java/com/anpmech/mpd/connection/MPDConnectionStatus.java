/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2016 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.anpmech.mpd.connection;

import com.anpmech.mpd.Log;
import com.anpmech.mpd.concurrent.MPDExecutor;
import com.anpmech.mpd.exception.MPDException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This class is a {@link MPDConnection} status tracker.
 */
public abstract class MPDConnectionStatus {

    /**
     * This flag enables or disables debug log output.
     */
    private static final boolean DEBUG = false;

    /**
     * The class log identifier.
     */
    private static final String TAG = "ConnectionStatus";

    /**
     * This stores a priority listener to be called for the connection instance.
     */
    private final MPDConnectionListener mConnectionListener;

    /**
     * The callbacks to inform of changes.
     */
    private final Collection<MPDConnectionListener> mConnectionListeners = new ArrayList<>();

    /**
     * The connection status binary semaphore.
     *
     * <p>This Semaphore starts off with no available permits, denoting a lack of connection until
     * set otherwise.</p>
     */
    private final Semaphore mConnectionStatus = new Semaphore(0);

    /**
     * This boolean tracks whether the connection was cancelled by the client.
     */
    private volatile boolean mIsCancelled;

    /**
     * The 'connecting' connection status tracking field.
     */
    private volatile boolean mIsConnecting;

    /**
     * This field stores the last time the status of this connection changed.
     */
    private long mLastChangeTime = -1L;

    /**
     * The sole constructor.
     *
     * @param listener The connected {@link MPDConnection} instance.
     */
    MPDConnectionStatus(final MPDConnectionListener listener) {
        mConnectionListener = listener;
    }

    /**
     * This method outputs the {@code line} parameter to a {@link Log#debug(String, String)} if
     * {@link #DEBUG} is set to {@code true}.
     *
     * @param line The {@link String} to output to the log.
     */
    protected static void debug(final String line) {
        if (DEBUG) {
            Log.debug(TAG, line);
        }
    }

    /**
     * Adds a listener for the connection status.
     *
     * @param listener A listener for connection status.
     */
    public void addListener(final MPDConnectionListener listener) {
        if (!mConnectionListeners.contains(listener)) {
            mConnectionListeners.add(listener);
        }
    }

    /**
     * This is called at the end of the connection class
     * {@link MPDConnectionListener#connectionConnected(int)} callback.
     *
     * <p>This is called from the actual connection class, to prevent calling prior to something
     * that needs to be taken care of by the connection prior to child callbacks.</p>
     *
     * @param commandErrorCode If this number is non-zero, this corresponds to a
     *                         {@link MPDException} error code.
     */
    void connectedCallbackComplete(final int commandErrorCode) {
        for (final MPDConnectionListener listener : mConnectionListeners) {
            MPDExecutor.submitCallback(new Runnable() {
                @Override
                public void run() {
                    listener.connectionConnected(commandErrorCode);
                }
            });
        }
    }

    /**
     * This is called at the end of the connection class
     * {@link MPDConnectionListener#connectionConnecting()} callback.
     *
     * <p>This is called from the actual connection class, to prevent calling prior to something
     * that needs to be taken care of by the connection prior to child callbacks.</p>
     */
    void connectingCallbackComplete() {
        for (final MPDConnectionListener listener : mConnectionListeners) {
            MPDExecutor.submitCallback(new Runnable() {
                @Override
                public void run() {
                    listener.connectionConnecting();
                }
            });
        }
    }

    /**
     * This is called at the end of the connection class
     * {@link MPDConnectionListener#connectionDisconnected(String)} callback.
     *
     * <p>This is called from the actual connection class, to prevent calling prior to something
     * that needs to be taken care of by the connection prior to child callbacks.</p>
     *
     * @param reason The reason for the disconnection.
     */
    void disconnectedCallbackComplete(final String reason) {
        for (final MPDConnectionListener listener : mConnectionListeners) {
            MPDExecutor.submitCallback(new Runnable() {
                @Override
                public void run() {
                    listener.connectionDisconnected(reason);
                }
            });
        }
    }

    /**
     * Returns the last time a status change occurred.
     *
     * @return Returns the last time the connection status was changed in milliseconds since epoch.
     * If this connection has never had a connected state, {@link Long#MIN_VALUE} will be returned.
     */
    public long getChangeTime() {
        return mLastChangeTime;
    }

    /**
     * This should implement the current status of the connection blocking.
     *
     * @return This should return true if the connection has potential to block and is blocking,
     * false otherwise.
     */
    abstract boolean isBlocked();

    /**
     * Whether the connection was cancelled by the client.
     *
     * @return True if cancelled by the local client, false otherwise.
     */
    public boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * Checks this connection for connected status.
     *
     * @return True if this connection is connected, false otherwise.
     */
    public boolean isConnected() {
        boolean isConnected = false;

        try {
            isConnected = mConnectionStatus.tryAcquire();
        } finally {
            if (isConnected) {
                mConnectionStatus.release();
            }
        }

        return isConnected;
    }

    /**
     * Checks this connection for connecting status.
     *
     * @return True if this connection is connecting, false otherwise.
     */
    public boolean isConnecting() {
        return mIsConnecting;
    }

    /**
     * Remove a listener from this connection.
     *
     * @param listener The listener to add for this connection.
     */
    public void removeListener(final MPDConnectionListener listener) {
        if (mConnectionListeners.contains(listener)) {
            mConnectionListeners.remove(listener);
        }
    }

    /**
     * This should implement what occurs when a connection is blocking, if it has potential to
     * block.
     */
    abstract void setBlocked();

    /**
     * This should implement what occurs when a connection is not blocking, if it has potential to
     * block.
     */
    abstract void setNotBlocked();

    /**
     * This is called when called by the disconnection timer.
     *
     * @param reason The reason for the connection cancellation.
     */
    void statusChangeCancelled(final String reason) {
        mIsCancelled = true;
        statusChangeDisconnected(reason);
    }

    /**
     * This is called when the connection is dropped by the client, itself.
     */
    void statusChangeCancelled() {
        statusChangeCancelled("Cancelled by client.");
    }

    /**
     * Changes the status of the connection to connected.
     *
     * @see #statusChangeDisconnected(String)
     */
    void statusChangeConnected() {
        try {
            if (!mConnectionStatus.tryAcquire()) {
                debug("Status changed to connected.");
                mIsCancelled = false;
                mIsConnecting = false;
                mLastChangeTime = System.currentTimeMillis();

                MPDExecutor.submitCallback(new Runnable() {
                    @Override
                    public void run() {
                        mConnectionListener.connectionConnected(0);
                    }
                });
            }
        } finally {
            mConnectionStatus.release();
        }
    }

    /**
     * Changes the status of this connection to a transient 'Connecting' status.
     */
    void statusChangeConnecting() {
        if (!mIsConnecting) {
            mLastChangeTime = System.currentTimeMillis();
            debug("Status changed to connecting");
            mIsCancelled = false;
            mIsConnecting = true;

            /**
             * Acquire a permit, if available. This signifies that we're disconnected, which is
             * implied by connecting.
             */
            mConnectionStatus.tryAcquire();
            MPDExecutor.submitCallback(new Runnable() {
                @Override
                public void run() {
                    mConnectionListener.connectionConnecting();
                }
            });
        }
    }

    /**
     * Changes the status of the connection to disconnected.
     *
     * @param reason The reason for the disconnection.
     * @see #statusChangeConnected()
     */
    void statusChangeDisconnected(final String reason) {
        if (mConnectionStatus.tryAcquire() || mIsConnecting) {
            debug("Status changed to disconnected: " + reason);
            mIsConnecting = false;
            mLastChangeTime = System.currentTimeMillis();

            MPDExecutor.submitCallback(new Runnable() {
                @Override
                public void run() {
                    mConnectionListener.connectionDisconnected(reason);
                }
            });
        }
    }

    @Override
    public String toString() {
        return "MPDConnectionStatus{" +
                "mConnectionListeners=" + mConnectionListeners +
                ", mConnectionStatus=" + mConnectionStatus +
                ", mConnectionListener=" + mConnectionListener +
                ", mIsCancelled=" + mIsCancelled +
                ", mIsConnecting=" + mIsConnecting +
                ", mLastChangeTime=" + mLastChangeTime +
                '}';
    }

    /**
     * This unsets the cancelled connection status, allowing new connections to initiate.
     */
    void unsetCancelled() {
        mIsCancelled = false;
    }

    /**
     * This method blocks indefinitely, when not connected, until connection, unless interrupted.
     *
     * @throws InterruptedException If the current thread is interrupted.
     * @see #waitForConnection(long, TimeUnit)
     */
    public void waitForConnection() throws InterruptedException {
        try {
            mConnectionStatus.acquire();
        } finally {
            mConnectionStatus.release();
        }
    }

    /**
     * This method blocks when not connected until connected, or timeout.
     *
     * @param timeout The maximum time to wait for a connection.
     * @param unit    The time unit of the {@code timeout} argument.
     * @return True if the connected within time limit, false otherwise.
     * @throws InterruptedException If the current thread is interrupted.
     * @see #waitForConnection()
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean waitForConnection(final long timeout, final TimeUnit unit)
            throws InterruptedException {
        boolean connectionAcquired = false;

        try {
            connectionAcquired = mConnectionStatus.tryAcquire(timeout, unit);
        } finally {
            if (connectionAcquired) {
                mConnectionStatus.release();
            }
        }

        return connectionAcquired;
    }
}
