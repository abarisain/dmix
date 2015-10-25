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

package com.namelessdev.mpdroid;

import com.anpmech.mpd.MPDCommand;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class stores information about the current connection, at the time of creation.
 *
 * This class is immutable, thus thread-safe.
 */
public final class ConnectionInfo implements Parcelable {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<ConnectionInfo> CREATOR = new ConnectionInfoCreator();

    /**
     * This is an empty object used for initialization.
     *
     * <p>Everything from this object will be {@code false}, {@code empty} or the item default.</p>
     */
    public static final ConnectionInfo EMPTY = new ConnectionInfo.Builder().build();

    /**
     * This is the ClassLoader for this class.
     */
    private static final ClassLoader LOADER = ConnectionInfo.class.getClassLoader();

    /**
     * The class log identifier.
     */
    private static final String TAG = "ConnectionInfo";

    /**
     * This is a convenience string to use as a Intent extra tag.
     */
    public static final String EXTRA = TAG;

    /**
     * This field stores whether the notification should be persistent.
     */
    private final boolean mIsNotificationPersistent;

    /**
     * This field is used to track changes from the last connection to this one.
     */
    private final ConnectionInfo mLastConnection;

    /**
     * This field stores the MPD server password.
     */
    private final String mPassword;

    /**
     * This field stores the MPD server host port.
     */
    private final int mPort;

    /**
     * This field stores the MPD server host address.
     */
    private final String mServer;

    /**
     * This field stores the Stream URL for this connection.
     */
    private final Uri mStream;

    /**
     * This constructor is used to build this immutable class. To build this class, see the {@link
     * Builder}
     *
     * @param server                   The MPD server host address.
     * @param port                     The MPD server host port.
     * @param password                 The MPD server password.
     * @param stream                   This is the Uri for the stream.
     * @param isNotificationPersistent Whether notification persistence is expected.
     * @param lastConnection           This is the connection before the change to this one.
     * @see Builder
     */
    private ConnectionInfo(final String server, final int port, final String password,
            final Uri stream, final boolean isNotificationPersistent,
            final ConnectionInfo lastConnection) {
        super();

        mServer = server;
        mPort = port;
        mPassword = password;

        mStream = stream;

        mIsNotificationPersistent = isNotificationPersistent;
        mLastConnection = lastConnection;
    }

    /**
     * This is a simple static method to test for inequality in a null-safe fashion.
     *
     * @param a The Object to test for equality with the Object in parameter {@code b}.
     * @param b The Object to test for equality with the Object in parameter {@code a}.
     * @return True if the two parameters are not equal, false otherwise.
     */
    private static boolean isNotEqualNullSafe(final Object a, final Object b) {
        final boolean isNotEqual;

        if (a == null && b == null) {
            isNotEqual = false;
        } else if (a == null || b == null) {
            isNotEqual = true;
        } else {
            isNotEqual = !a.equals(b);
        }

        return isNotEqual;
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable object's  marshall
     * representation.
     *
     * @return A bit mask indicating the set of special object types marshall by the Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * This method returns the MPD server password for this ConnectionInfo.
     *
     * @return The MPD server password.
     */
    @Nullable
    public String getPassword() {
        return mPassword;
    }

    /**
     * This method returns the MPD server host port for this ConnectionInfo.
     *
     * @return The MPD server host port.
     */
    public int getPort() {
        return mPort;
    }

    /**
     * This method returns the MPD server host address for this ConnectionInfo.
     *
     * @return The MPD server host address.
     */
    @NotNull
    public String getServer() {
        return mServer;
    }

    /**
     * This method returns the stream URL for this connection.
     *
     * @return The stream URL for this connection.
     */
    @NotNull
    public Uri getStream() {
        return mStream;
    }

    /**
     * This method checks for changes in the password.
     *
     * @return True if the host password has changed, false otherwise.
     */
    public boolean hasHostPasswordChanged() {
        return isNotEqualNullSafe(mLastConnection.mPassword, mPassword);
    }

    /**
     * This method checks for changes in the hostname port.
     *
     * @return True if the port has changed, false otherwise.
     */
    public boolean hasHostPortChanged() {
        return mLastConnection.mPort != mPort;
    }

    /**
     * This method checks for changes in the hostname.
     *
     * @return True if the hostname has changed since the prior connection, false otherwise.
     */
    public boolean hasHostnameChanged() {
        return isNotEqualNullSafe(mLastConnection.mServer, mServer);
    }

    /**
     * This method returns whether the server information has changed since the prior
     * ConnectionInfo.
     *
     * <p>This is true if {@link #hasHostnameChanged()}, {@link #hasHostPortChanged()} and
     * {@link #hasHostPasswordChanged()} is true, false otherwise.</p>
     *
     * @return True if the server information has changed since the last ConnectionInfo.
     */
    public boolean hasServerChanged() {
        return hasHostnameChanged() || hasHostPasswordChanged() || hasHostPortChanged();
    }

    /**
     * This returns whether the stream information has changed since the prior ConnectionInfo.
     *
     * @return True if the stream information has changed, false otherwise.
     */
    public boolean hasStreamInfoChanged() {
        return !mLastConnection.mStream.equals(mStream);
    }

    /**
     * This returns whether the notification for this ConnectionInfo is expected to be persistent.
     *
     * @return True if the notification should be persistent, false otherwise.
     */
    public boolean isNotificationPersistent() {
        return mIsNotificationPersistent;
    }

    @Override
    public String toString() {
        return "ConnectionInfo{" +
                "mIsNotificationPersistent=" + mIsNotificationPersistent +
                ", mPassword='" + mPassword + '\'' +
                ", mPort=" + mPort +
                ", mServer='" + mServer + '\'' +
                ", mStream=" + mStream +
                ", mLastConnection=" + mLastConnection +
                '}';
    }

    /**
     * This returns whether the prior ConnectionInfo notification was expected to be persistent.
     *
     * @return True if the prior ConnectionInfo notification was persistent, false otherwise.
     */
    public boolean wasNotificationPersistent() {
        return mLastConnection.mIsNotificationPersistent != mIsNotificationPersistent;
    }

    /**
     * Flatten this object into a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written. May be 0 or {@link
     *              #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        final boolean[] boolArray = {mIsNotificationPersistent};

        dest.writeString(mServer);
        dest.writeInt(mPort);
        dest.writeString(mPassword);
        dest.writeParcelable(mStream, 0);
        dest.writeBooleanArray(boolArray);
        dest.writeParcelable(mLastConnection, 0);
    }

    /**
     * This is the Builder for the enclosing class.
     */
    public static class Builder {

        /**
         * The MPD server password store for this Builder.
         */
        private final String mPassword;

        /**
         * The MPD server host port for this Builder.
         */
        private final int mPort;

        /**
         * The MPD server hostname for this Builder.
         */
        private final String mServer;

        /**
         * Whether this notification is persistent.
         */
        private boolean mIsNotificationPersistent;

        /**
         * This field stores the prior connection information.
         */
        private ConnectionInfo mLastConnection;

        /**
         * This field is a flag marked as true to ensure that persistent notification is set prior
         * to a call to {@link #build()}.
         */
        private boolean mPersistentRunFirst;

        /**
         * This field is a flag marked as {@code true} to ensure that a previous state has been
         * given prior to a call to {@link #build()}.
         */
        private boolean mPreviousRunFirst;

        /**
         * This is the Uri for the stream host built by the UI.
         */
        private Uri mStream;

        /**
         * This is an empty ConnectionInfo object.
         */
        private Builder() {
            super();

            mPreviousRunFirst = true;
            mPersistentRunFirst = true;

            mServer = "";
            mPort = MPDCommand.DEFAULT_MPD_PORT;
            mPassword = "";

            mStream = Uri.EMPTY;

            mLastConnection = build();
        }

        /**
         * This constructor constructs a non-empty ConnectionInfo object.
         *
         * @param server   The MPD server host address.
         * @param port     The MPD server host port.
         * @param password The MPD server host password.
         */
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
        public ConnectionInfo build() {
            if (!mPreviousRunFirst) {
                throw new IllegalStateException("setPreviousConnectionInfo() must be run prior to "
                        + "calling this method.");
            }

            if (!mPersistentRunFirst) {
                throw new IllegalStateException("Notification must be set as persistent or not "
                        + "persistent prior to calling this method.");
            }

            if (mStream == null) {
                throw new IllegalStateException("setStreamServer() must be called prior to " +
                        "calling this method.");
            }

            return new ConnectionInfo(mServer, mPort, mPassword, mStream,
                    mIsNotificationPersistent, mLastConnection);
        }

        /**
         * This method sets the notification is not persistent for this connection.
         */
        public void setNotificationNotPersistent() {
            mIsNotificationPersistent = false;

            mPersistentRunFirst = true;
        }

        /**
         * This method sets the notification as persistent for this connection.
         */
        public void setNotificationPersistent() {
            mPersistentRunFirst = true;

            mIsNotificationPersistent = true;
        }

        /**
         * Logically compares the previous {@code ConnectionInfo} object for changes and changes in
         * the current object.
         *
         * @param connectionInfo The previous {@code ConnectionInfo} object.
         * @throws IllegalStateException If {@code setPersistentNotification()} and {@code
         *                               setStreamServer()} are not run prior to this method.
         */
        public void setPreviousConnectionInfo(final ConnectionInfo connectionInfo) {
            mPreviousRunFirst = true;
            mLastConnection = connectionInfo;
        }

        /**
         * This method sets a streaming server for this connection.
         *
         * @param stream The stream URI.
         */
        public void setStreamingServer(final String stream) {
            mStream = Uri.parse(stream);

            if (BuildConfig.DEBUG && mServer != null && mStream.getPort() == -1) {
                throw new IllegalStateException("Stream must have a port: " + mStream);
            }
        }
    }

    /**
     * This class is used to instantiate a ConnectionInfo Object from a {@code Parcel}.
     */
    private static final class ConnectionInfoCreator implements Creator<ConnectionInfo> {

        /**
         * Sole constructor.
         */
        private ConnectionInfoCreator() {
            super();
        }

        /**
         * This creates the object instance from the Parcel.
         *
         * @param source The source Parcel.
         * @return The instance object from the Parcel.
         */
        @Override
        public ConnectionInfo createFromParcel(final Parcel source) {
            final String server = source.readString();
            final int port = source.readInt();
            final String password = source.readString();
            final Uri stream = source.readParcelable(Uri.class.getClassLoader());
            final boolean[] boolArray = source.createBooleanArray();
            final ConnectionInfo lastConnection = source.readParcelable(LOADER);

            return new ConnectionInfo(server, port, password, stream, boolArray[0],
                    lastConnection);
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public ConnectionInfo[] newArray(final int size) {
            return new ConnectionInfo[size];
        }
    }
}