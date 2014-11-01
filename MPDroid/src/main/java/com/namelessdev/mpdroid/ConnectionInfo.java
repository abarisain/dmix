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

package com.namelessdev.mpdroid;

import org.a0z.mpd.MPDCommand;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectionInfo implements Parcelable {

    public static final Creator<ConnectionInfo> CREATOR = new Creator<ConnectionInfo>() {

        /**
         * This creates the object instance from the Parcel.
         *
         * @param source The source Parcel.
         * @return The instance object from the Parcel.
         */
        @Override
        public ConnectionInfo createFromParcel(final Parcel source) {
            final String pServer = source.readString();
            final int pPort = source.readInt();
            final String pPassword = source.readString();
            final String pStreamServer = source.readString();
            final int pStreamPort = source.readInt();
            final String pStreamSuffix = source.readString();
            final boolean[] pBoolArray = source.createBooleanArray();

            return new ConnectionInfo(
                    pServer, pPort, pPassword, pStreamServer, pStreamPort, pStreamSuffix,
                    pBoolArray[0], pBoolArray[1], pBoolArray[2], pBoolArray[3]);
        }

        @Override
        public ConnectionInfo[] newArray(final int size) {
            return new ConnectionInfo[size];
        }
    };

    private static final String TAG = "ConnectionInfo";

    public static final String BUNDLE_KEY = TAG;

    public final boolean isNotificationPersistent;

    public final String password;

    public final int port;

    public final String server;

    public final boolean serverInfoChanged;

    public final int streamPort;

    public final String streamServer;

    public final String streamSuffix;

    public final boolean streamingServerInfoChanged;

    public final boolean wasNotificationPersistent;

    /** Only call this to initialize. */
    public ConnectionInfo() {
        super();

        server = null;
        port = MPDCommand.DEFAULT_MPD_PORT;
        password = null;

        streamServer = null;
        streamPort = MPDCommand.DEFAULT_MPD_PORT;
        streamSuffix = null;

        isNotificationPersistent = false;
        wasNotificationPersistent = false;
        serverInfoChanged = false;
        streamingServerInfoChanged = false;
    }

    /** The private constructor, constructed by the Build inner class. */
    private ConnectionInfo(final String pServer, final int pPort, final String pPassword,
            final String pStreamServer, final int pStreamPort, final String pStreamSuffix,
            final boolean pIsNotificationPersistent, final boolean pWasNotificationPersistent,
            final boolean pServerInfoChanged, final boolean pStreamingInfoChanged) {
        super();

        server = pServer;
        port = pPort;
        password = pPassword;

        streamServer = pStreamServer;
        streamPort = pStreamPort;
        streamSuffix = pStreamSuffix;

        isNotificationPersistent = pIsNotificationPersistent;
        wasNotificationPersistent = pWasNotificationPersistent;
        serverInfoChanged = pServerInfoChanged;
        streamingServerInfoChanged = pStreamingInfoChanged;
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable object's  marshall
     * representation.
     *
     * @return A bit mask indicating the set of special object types marshall by the Parcelable.
     */
    @Override
    public final int describeContents() {
        return 0;
    }

    /**
     * The {@code ConnectionInfo} toString() implementation.
     *
     * @return A string containing all the {@code ConnectionInfo} fields.
     */
    @Override
    public final String toString() {
        return "isNotificationPersistent: " + isNotificationPersistent +
                " password: " + password +
                " port: " + port +
                " server: " + server +
                " serverInfoChanged: " + serverInfoChanged +
                " streamServerInfoChanged: " + streamingServerInfoChanged +
                " streamServer: " + streamServer +
                " streamPort: " + streamPort +
                " streamSuffix: " + streamSuffix +
                " wasNotificationPersistent: " + wasNotificationPersistent;
    }

    /**
     * Flatten this object into a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public final void writeToParcel(final Parcel dest, final int flags) {
        final boolean[] boolArray = {isNotificationPersistent, wasNotificationPersistent,
                serverInfoChanged, streamingServerInfoChanged};

        dest.writeString(server);
        dest.writeInt(port);
        dest.writeString(password);
        dest.writeString(streamServer);
        dest.writeInt(streamPort);
        dest.writeString(streamSuffix);
        dest.writeBooleanArray(boolArray);
    }

    public static class Builder {

        private final String mPassword;

        private final int mPort;

        private boolean mNotificationPersistent;

        private boolean mPersistentRunFirst = false;

        private boolean mPreviousRunFirst = false;

        private String mServer = null;

        private boolean mServerInfoChanged;

        private int mStreamPort;

        private String mStreamServer = null;

        private String mStreamSuffix;

        private boolean mStreamingServerInfoChanged;

        private boolean mWasNotificationPersistent;

        public Builder(final String server, final int port, final String password) {
            super();

            mServer = server;
            mPort = port;
            mPassword = password;
        }

        /**
         * Builds the {@code ConnectionInfo} after the builder methods are run.
         *
         * @return The {@code ConnectionInfo} object.
         * @throws IllegalStateException If {@code setPreviousConnectionInfo()} is not run.
         */
        public final ConnectionInfo build() {
            if (!mPreviousRunFirst) {
                throw new IllegalStateException("setPreviousConnectionInfo() must be run prior to" +
                        " build()");
            }

            return new ConnectionInfo(mServer, mPort, mPassword,
                    mStreamServer, mStreamPort, mStreamSuffix, mNotificationPersistent,
                    mWasNotificationPersistent, mServerInfoChanged, mStreamingServerInfoChanged);
        }

        /**
         * Compares two {@code ConnectionInfo} objects for logical equality with regard to the main
         * media server information.
         *
         * @param connectionInfo The object containing server information to compare against.
         * @return True if the media server information is different, false otherwise.
         */
        private boolean hasServerChanged(final ConnectionInfo connectionInfo) {
            final boolean result;

            if (connectionInfo == null) {
                result = true;
            } else if (connectionInfo.server == null ||
                    !connectionInfo.server.equals(mServer)) {
                result = true;
            } else if (connectionInfo.port != mPort) {
                result = true;
            } else if (connectionInfo.password == null ||
                    !connectionInfo.password.equals(mPassword)) {
                result = true;
            } else {
                result = false;
            }

            return result;
        }

        /**
         * Compares two {@code ConnectionInfo} objects for logical equality with regard to the
         * streaming server information.
         *
         * @param connectionInfo The object containing stream server information to compare
         *                       against.
         * @return True if the streaming server information is different, false otherwise.
         */
        private boolean hasStreamingServerChanged(final ConnectionInfo connectionInfo) {
            final boolean result;

            if (connectionInfo == null) {
                result = true;
            } else if (connectionInfo.streamServer == null ||
                    !connectionInfo.streamServer.equals(mStreamServer)) {
                result = true;
            } else if (connectionInfo.streamPort != mStreamPort) {
                result = true;
            } else if (connectionInfo.streamSuffix == null ||
                    !connectionInfo.streamSuffix.equals(mStreamSuffix)) {
                result = true;
            } else {
                result = false;
            }

            return result;
        }

        public final void setPersistentNotification(final boolean isPersistent) {
            mPersistentRunFirst = true;

            mNotificationPersistent = isPersistent;
        }

        /**
         * Logically compares the previous {@code ConnectionInfo} object
         * for changes and changes in the current object.
         *
         * @param connectionInfo The previous {@code ConnectionInfo} object.
         * @throws IllegalStateException If {@code setPersistentNotification()} and
         *                               {@code setStreamServer()} are not run prior to this
         *                               method.
         */
        public final void setPreviousConnectionInfo(final ConnectionInfo connectionInfo) {
            if (!mPersistentRunFirst || mStreamServer == null) {
                throw new IllegalStateException("setPersistentNotification() && setStreamServer()" +
                        "must be" + " run prior to setPersistentConnectionInfo()");
            }
            mPreviousRunFirst = true;

            if (connectionInfo == null) {
                mWasNotificationPersistent = false;
                mServerInfoChanged = true;
                mStreamingServerInfoChanged = true;
            } else {
                mWasNotificationPersistent = connectionInfo.isNotificationPersistent;

                mServerInfoChanged = hasServerChanged(connectionInfo);
                mStreamingServerInfoChanged = hasStreamingServerChanged(connectionInfo);
            }
        }

        public final void setStreamingServer(final String server, final int port,
                final String suffix) {
            if (server == null || server.isEmpty()) {
                mStreamServer = mServer;
            } else {
                mStreamServer = server;
            }
            mStreamPort = port;
            mStreamSuffix = suffix;
        }
    }
}