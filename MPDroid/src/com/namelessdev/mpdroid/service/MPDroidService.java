/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.service;

import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.RemoteControlReceiver;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.tools.SettingsHelper;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This service schedules various long running features that runs completely independent of
 * MPDroid.
 */
public final class MPDroidService extends Service implements
        AudioManager.OnAudioFocusChangeListener,
        MPDAsyncHelper.ConnectionInfoListener,
        MPDAsyncHelper.NetworkMonitorListener,
        StatusChangeListener {

    /** This is the class unique Binder identifier. */
    static final int LOCAL_UID = 200;

    /** Disconnects if no connection exists. */
    private static final int DISCONNECT_ON_NO_CONNECTION = LOCAL_UID + 1;

    /** Request that all clients unbind due to service inactivity. */
    public static final int REQUEST_UNBIND = LOCAL_UID + 2;

    /** Immediately attempts to stopSelf(). */
    private static final int STOP_SELF = LOCAL_UID + 3;

    /** Update the clients with the important handler status. */
    public static final int UPDATE_CLIENT_STATUS = LOCAL_UID + 4;

    /** Sends stop() to all handlers. */
    private static final int WIND_DOWN_HANDLERS = LOCAL_UID + 5;

    /** The main process connection changed. */
    public static final int CONNECTION_INFO_CHANGED = LOCAL_UID + 6;

    static final MPDAsyncHelper MPD_ASYNC_HELPER = new MPDAsyncHelper(false);

    /** Enable this to get various DEBUG messages from this module. */
    static final boolean DEBUG = false;

    private static final String TAG = "MPDroidService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid.service." + TAG;

    /** Handled in onStartCommand(), this is the persistent service start intent action. */
    public static final String ACTION_START = FULLY_QUALIFIED_NAME + ".ACTION_START";

    /** Handled in RemoteControlReceiver, this attempts closing this service. */
    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + ".ACTION_STOP";

    /** The inner class which handles messages for this service. */
    private final MessageHandler mMessageHandler = new MessageHandler();

    /** Directs messages to our Message Handler inner class. */
    private final Handler mHandler = new Handler(mMessageHandler);

    private AlbumCoverHandler mAlbumCoverHandler = null;

    /** The Android AudioManager, used by this for audio focus control. */
    private AudioManager mAudioManager = null;

    private Music mCurrentTrack = null;

    private boolean mIsAudioFocusedOnThis = false;

    private NotificationHandler mNotificationHandler = null;

    private RemoteControlClientHandler mRemoteControlClientHandler = null;

    /** The audio stream handler. */
    private StreamHandler mStreamHandler = null;

    /** If the media server is playing, and this is true, notification should show. */
    private boolean mIsNotificationStarted = false;

    private boolean mIsPersistentOverridden = false;

    /** If the media server is playing, and this is true, audio streaming will be attempted. */
    private boolean mIsStreamStarted = false;

    private boolean mNotificationOwnsService = false;

    private boolean mStreamOwnsService = false;

    /**
     * A simple method to return a status with error logging.
     *
     * @return An MPDStatus object.
     */
    private static MPDStatus getMPDStatus() {
        MPDStatus mpdStatus = null;
        try {
            mpdStatus = MPD_ASYNC_HELPER.oMPD.getStatus();
        } catch (final MPDServerException e) {
            Log.e(TAG, "Couldn't retrieve a status object.", e);
        }

        return mpdStatus;
    }

    /**
     * A function to translate 'what' fields to literal debug name, used primarily for debugging.
     *
     * @param what A 'what' field.
     * @return The literal field name.
     */
    public static String getHandlerValue(final int what) {
        final String result;

        switch (what) {
            case CONNECTION_INFO_CHANGED:
                result = "CONNECTION_INFO_CHANGED";
                break;
            case DISCONNECT_ON_NO_CONNECTION:
                result = "DISCONNECT_ON_NO_CONNECTION";
                break;
            case REQUEST_UNBIND:
                result = "REQUEST_UNBIND";
                break;
            case STOP_SELF:
                result = "STOP_SELF";
                break;
            case UPDATE_CLIENT_STATUS:
                result = "UPDATE_CLIENT_STATUS";
                break;
            case WIND_DOWN_HANDLERS:
                result = "WIND_DOWN_HANDLERS";
                break;
            default:
                result = "{unknown}: " + what;
                break;
        }

        return "MPDroidService." + result;
    }

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        if (DEBUG) {
            Log.d(TAG, "connectionStateChanged(" + connected + ", " + connectionLost + ')');
        }

        final MPDStatus mpdStatus = getMPDStatus();
        if (connected) {
            stateChanged(mpdStatus, MPDStatus.MPD_STATE_UNKNOWN);
        } else {
            final long idleDelay = 10000L; /** Give 10 Seconds for Network Problems */

            if (!mHandler.hasMessages(DISCONNECT_ON_NO_CONNECTION)) {
                mHandler.sendEmptyMessageDelayed(DISCONNECT_ON_NO_CONNECTION, idleDelay);
            }
        }
    }

    private void handlerStateChanged(final MPDStatus mpdStatus) {
        if (mRemoteControlClientHandler != null) {
            mRemoteControlClientHandler.stateChanged(mpdStatus);
        }
        if (mStreamHandler != null) {
            mStreamHandler.stateChanged(mpdStatus);
        }
        if (mNotificationHandler != null) {
            mNotificationHandler.stateChanged(mpdStatus);
        }
    }

    /**
     * This method checks to see if there are active service handlers. At the moment, if
     * the notification handler is not active, nothing is. Extend as necessary.
     *
     * @return True if this service has active handlers, false otherwise.
     */
    private boolean hasActiveHandlers() {
        return mIsNotificationStarted || mIsStreamStarted;
    }

    private void initializeAsyncHelper() {
        final SettingsHelper settingsHelper = new SettingsHelper(MPD_ASYNC_HELPER);
        settingsHelper.updateConnectionSettings();

        if (!MPD_ASYNC_HELPER.oMPD.isConnected()) {
            MPD_ASYNC_HELPER.connect();
        }

        if (!MPD_ASYNC_HELPER.isStatusMonitorAlive()) {
            MPD_ASYNC_HELPER.startStatusMonitor();
        }

        if (!MPD_ASYNC_HELPER.isNetworkMonitorAlive()) {
            MPD_ASYNC_HELPER.startNetworkMonitor(this);
        }

        MPD_ASYNC_HELPER.addStatusChangeListener(this);
        MPD_ASYNC_HELPER.addNetworkMonitorListener(this);
        /**
         * From here, upon successful connection, it will go from connectionStateChanged to
         * stateChanged() where handlers will be started as required.
         */
    }

    /** Initializes the notification and handlers which are generally associated with it. */
    private void initializeNotification(final MPDStatus mpdStatus) {
        Log.d(TAG, "initializeNotification()");

        //TODO: Acquire a network wake lock here if the user wants us to !
        //Otherwise we'll just shut down on screen off and reconnect on screen on

        /** We group these together under the notification, but they can easily be split. */
        if (mNotificationHandler == null) {
            mNotificationHandler = new NotificationHandler(this);
            mAlbumCoverHandler = new AlbumCoverHandler(this);
            mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            mRemoteControlClientHandler = new RemoteControlClientHandler(this);
        } else {
            mNotificationHandler.start();
            mRemoteControlClientHandler.start();
        }
        mAlbumCoverHandler.addCallback(mNotificationHandler);
        mAlbumCoverHandler.addCallback(mRemoteControlClientHandler);

        mMessageHandler.sendMessageToClients(NotificationHandler.IS_ACTIVE, true);
    }

    /** Initializes the streaming handler. */
    private void initializeStream(final MPDStatus mpdStatus) {
        if (DEBUG) {
            Log.d(TAG, "initializeStream()");
        }
        if (mIsStreamStarted) {
            if (mStreamHandler == null) {
                mStreamHandler = new StreamHandler(this, mHandler, mAudioManager);
            }
            mStreamHandler.start(mpdStatus.getState());
            mMessageHandler.sendMessageToClients(StreamHandler.IS_ACTIVE, true);
        }
    }

    /** Is the notification persistent when taking override into account? */
    private boolean isNotificationPersistent() {
        return !mIsPersistentOverridden &&
                MPD_ASYNC_HELPER.getConnectionSettings().isNotificationPersistent;
    }

    /** Checks for both service and notification persistence. */
    private boolean isServiceBusy() {
        return mIsNotificationStarted || mIsStreamStarted || isNotificationPersistent();
    }

    /**
     * A JMPDComm callback to be invoked during library state changes.
     *
     * @param updating  true when updating, false when not updating.
     * @param dbChanged true when the server database has been updated, false otherwise.
     */
    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    public void onAudioFocusChange(final int i) {
        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mIsAudioFocusedOnThis = true;
                if (DEBUG) {
                    Log.d(TAG, "Gained audio focus");
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                mIsAudioFocusedOnThis = false;
                if (DEBUG) {
                    Log.d(TAG, "Lost audio focus");
                }
                break;
            default:
                if (DEBUG) {
                    Log.d(TAG, "Did not gain or lose audio focus: " + i);
                }
                break;
        }
    }

    /**
     * Binds to the incoming client.
     *
     * @param intent Intent used to bind to the service.
     * @return A messenger for the incoming client.
     */
    @Override
    public IBinder onBind(final Intent intent) {
        /** Target we publish for clients to send messages to IncomingHandler. */
        final Messenger serviceMessenger = new Messenger(mHandler);

        return serviceMessenger.getBinder();
    }

    /**
     * Called upon connection configuration change.
     *
     * @param connectionInfo The new connection configuration information object.
     */
    @Override
    public void onConnectionConfigChange(final ConnectionInfo connectionInfo) {
        if (connectionInfo.streamingServerInfoChanged && mIsStreamStarted) {
            Log.d(TAG, "Streaming information changed, resetting.");
            windDownHandlers(false);
            startStream();
        } else if (connectionInfo.serverInfoChanged && mIsNotificationStarted) {
            Log.d(TAG, "Notification information changed, resetting.");
            windDownHandlers(false);
            startNotification();
        }
    }

    /** If handlers have activated, use windDownService rather than stopSelf(). */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Log.d(TAG, "onDestroy()");
        }

        windDownHandlers(false);

        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * This method is called when a network has connected that matches the MPD server settings.
     */
    @Override
    public void onNetworkConnect() {
        if (DEBUG) {
            Log.d(TAG, "onNetworkConnect");
        }
        if (isNotificationPersistent()) {
            windDownHandlers(false);
            mIsNotificationStarted = false;
            startNotification();
        }
    }

    /**
     * This is one of the methods to start the service, and to keep the service running
     * semi-persistently.
     *
     * @param intent  The incoming intent used to start the service or containing expected action.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return @see #stopSelfResult(int)
     */
    @Override
    public final int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            final String action = intent.getAction();
            boolean keepActive = false;

            if (action != null) {
                switch (action) {
                    case ACTION_STOP:
                    case NotificationHandler.ACTION_STOP:
                        windDownHandlers(true);
                        break;
                    case ACTION_START:
                        keepActive = true;
                        if (DEBUG) {
                            Log.d(TAG, "Service persistent.");
                        }
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        if (mIsStreamStarted && mStreamHandler.isActive()) {
                            /** Should never be disconnected. We're streaming! */
                            if (MPD_ASYNC_HELPER.oMPD == null ||
                                    !MPD_ASYNC_HELPER.oMPD.isConnected()) {
                                initializeAsyncHelper();
                            }
                            MPDControl.run(MPD_ASYNC_HELPER.oMPD, MPDControl.ACTION_PAUSE);
                        }
                        break;
                    case Intent.ACTION_BOOT_COMPLETED:
                    case NotificationHandler.ACTION_START:
                        startNotification();
                        break;
                    case StreamHandler.ACTION_START:
                        startStream();
                        break;
                    case StreamHandler.ACTION_STOP:
                        stopStream();
                        break;
                    default:
                        break;
                }
            }

            /** If the action didn't start anything, shut it down. */
            if (!hasActiveHandlers() && !keepActive) {
                stopSelf();
            }
        }

        /** Means we started the service, but don't want it to restart in case it's killed. */
        return START_NOT_STICKY;
    }

    /**
     * If the user swipes MPDroid to close, it closes this service by default; this works
     * around that behaviour by restarting the service and getting back to where it was.
     *
     * @param rootIntent The original root Intent that was used to launch the task that is being
     *                   removed.
     */
    @Override
    public final void onTaskRemoved(final Intent rootIntent) {
        final String pendingAction;
        if (mIsStreamStarted) {
            pendingAction = StreamHandler.ACTION_START;
        } else if (mIsNotificationStarted) {
            pendingAction = NotificationHandler.ACTION_START;
        } else {
            pendingAction = null;
        }

        if (pendingAction != null) {
            final Intent restartServiceIntent
                    = new Intent(pendingAction, null, this, RemoteControlReceiver.class);
            final PendingIntent restartService = PendingIntent
                    .getBroadcast(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            final AlarmManager alarmService =
                    (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            alarmService.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + DateUtils.SECOND_IN_MILLIS,
                    restartService);
        }
        super.onTaskRemoved(rootIntent);
    }

    /**
     * Called when all clients have disconnected from a
     * particular interface published by the service.
     *
     * @param intent Intent used to bind to the service.
     * @return False if onRebind() on further binds is desired, true otherwise.
     */
    @Override
    public final boolean onUnbind(final Intent intent) {
        super.onUnbind(intent);

        if (DEBUG) {
            Log.d(TAG, "onUnbind()");
        }

        if (!isServiceBusy()) {
            if (DEBUG) {
                Log.d(TAG, "Service not persistent, last bind disconnected, "
                        + "cleaning up & shutting down.");
            }

            windDownHandlers(false);
        }

        return false;
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /**
         * This is required because streams will emit a playlist (current queue) event as the
         * metadata will change while the same audio file is playing (no track change).
         */
        if (mCurrentTrack != null && mCurrentTrack.isStream()) {
            updateTrack(mpdStatus);
        }
    }

    @Override
    public void randomChanged(final boolean random) {
    }

    @Override
    public void repeatChanged(final boolean repeating) {
    }

    /**
     * Sets the handlerUID as active or inactive.
     *
     * @param handlerUID The handler local UID what.
     * @param isActive True if the handler is being set as active, false otherwise.
     */
    private void setHandlerActivity(final int handlerUID, final boolean isActive) {
        switch(handlerUID) {
            case StreamHandler.LOCAL_UID:
                mIsStreamStarted = isActive;
                mMessageHandler.sendMessageToClients(StreamHandler.IS_ACTIVE, isActive);
                break;
            case NotificationHandler.LOCAL_UID:
                mIsNotificationStarted = isActive;
                mMessageHandler.sendMessageToClients(NotificationHandler.IS_ACTIVE, isActive);
                break;
            default:
                Log.e(TAG, "setStreamHandler set for invalid value.");
                break;
        }
    }

    /**
     * This is the idle delay for shutting down this service after inactivity
     * (in milliseconds). This idle is also longer than StreamHandler to
     * avoid being unnecessarily brought up to shut right back down.
     */
    private void setupServiceHandler() {
        if (!mHandler.hasMessages(WIND_DOWN_HANDLERS)) {
            if (DEBUG) {
                Log.d(TAG, "Setting up service handler.");
            }
            final long idleDelay = 300000L; /** 5 Minutes */

            mHandler.sendEmptyMessageDelayed(WIND_DOWN_HANDLERS, idleDelay);
        }
    }

    /**
     * This is the pre-initialization notification method, don't confuse this method and
     * initializeNotification(), this is run once per mIsNotificationStarted cycle upon
     * NotificationHandler.START.
     */
    private void startNotification() {
        if (!mIsNotificationStarted) {
            if (!mStreamOwnsService) {
                if (DEBUG) {
                    Log.d(TAG, "Notification owns service.");
                }
                mNotificationOwnsService = true;
            }

            setHandlerActivity(NotificationHandler.LOCAL_UID, true);
            if (MPD_ASYNC_HELPER.oMPD.isConnected()) {
                stateChanged(getMPDStatus(), MPDStatus.MPD_STATE_UNKNOWN);
            } else {
                initializeAsyncHelper();
                /**
                 * Don't worry about initializing here,
                 * connectionStateChanged() will call stateChange().
                 */
            }
        }
    }

    /**
     * This is the pre-initialization stream method, don't confuse this method and
     * initializeStream(), this is run once per mIsStreamStarted cycle upon
     * StreamHandler.START.
     */
    private void startStream() {
        if (!mIsStreamStarted) {
            if (!mNotificationOwnsService) {
                if (DEBUG) {
                    Log.d(TAG, "Stream owns service");
                }
                mStreamOwnsService = true;
            }

            setHandlerActivity(StreamHandler.LOCAL_UID, true);
            if (MPD_ASYNC_HELPER.oMPD.isConnected()) {
                stateChanged(getMPDStatus(), MPDStatus.MPD_STATE_UNKNOWN);
            } else {
                initializeAsyncHelper();
                /**
                 * Don't worry about initializing here,
                 * connectionStateChange() will call stateChange().
                 */
            }
        }
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final String oldState) {
        if (mpdStatus == null) {
            Log.w(TAG, "Null mpdStatus received in stateChanged");
        } else {
            switch (mpdStatus.getState()) {
                case MPDStatus.MPD_STATE_PLAYING:
                    stateChangedPlaying(mpdStatus, oldState);
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                    windDownHandlers(true);
                    break;
                case MPDStatus.MPD_STATE_PAUSED:
                    if (!MPDStatus.MPD_STATE_PLAYING.equals(oldState)) {
                        updateTrack(mpdStatus);
                    }
                    setupServiceHandler();
                    break;
                default:
                    break;
            }
            handlerStateChanged(mpdStatus);
        }
    }

    /** This method is called during a stateChanged() when the media server is playing. */
    private void stateChangedPlaying(final MPDStatus mpdStatus, final String oldState) {
        mHandler.removeMessages(WIND_DOWN_HANDLERS);
        final boolean needNotification = mIsNotificationStarted || mIsStreamStarted;

        if (needNotification && (mNotificationHandler == null ||
                !mNotificationHandler.isActive())) {
            initializeNotification(mpdStatus);
        }

        if (mIsStreamStarted && (mStreamHandler == null ||
                !mStreamHandler.isActive())) {
            initializeStream(mpdStatus);
        }

        updateTrack(mpdStatus);
        tryToGetAudioFocus();
    }

    /**
     * This is called to stop the stream. The null checks here are required as these handlers
     * can be called if the service crashes and the main process status gets out of sync.
     */
    private void stopStream() {
        setHandlerActivity(StreamHandler.LOCAL_UID, false);

        if (mNotificationHandler != null) {
            mNotificationHandler.setMediaPlayerWoundDown();
        }

        if (mStreamHandler != null) {
            mStreamHandler.stop();
        }
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        updateTrack(mpdStatus);
    }

    /**
     * We try to get audio focus, but don't really try too hard.
     * We just want the lock screen cover art.
     */
    private void tryToGetAudioFocus() {
        if (mIsNotificationStarted && !mIsStreamStarted && !mIsAudioFocusedOnThis) {
            final int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (DEBUG) {
                Log.d(TAG, "Requested audio focus, received code: " + result);
            }
        }
    }

    /**
     * Updates the current track of all handlers which require a current track.
     *
     * @param mpdStatus A {@code MPDStatus} object.
     */
    private void updateTrack(final MPDStatus mpdStatus) {
        /**
         * Workaround for Bug #558 This is necessary if setMediaPlayerBuffering() is the first
         * method to be called. Optimally, this would be passed into the constructor, but
         * this complication belongs here for now.
         */
        int songPos = mpdStatus.getSongPos();
        mCurrentTrack = MPD_ASYNC_HELPER.oMPD.getPlaylist().getByIndex(songPos);

        while (mCurrentTrack == null && mpdStatus.getPlaylistLength() > 0) {
            Log.w(TAG, "Failed to get current track, likely due to bug #558, looping..");
            synchronized (this) {
                try {
                    wait(100L);
                } catch (final InterruptedException ignored) {
                }
            }

            songPos = mpdStatus.getSongPos();
            mCurrentTrack = MPD_ASYNC_HELPER.oMPD.getPlaylist().getByIndex(songPos);
        }

        if (mNotificationHandler != null && mCurrentTrack != null) {
            mAlbumCoverHandler.update(mCurrentTrack.getAlbumInfo());
            mNotificationHandler.setNewTrack(mCurrentTrack);
            mRemoteControlClientHandler.update(mCurrentTrack);
        }
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
    }

    /** Handles all resource winding down after handlers have completed their work. */
    private void windDownHandlers(final boolean stopSelf) {
        if (DEBUG) {
            Log.d(TAG, "windDownHandlers()");
        }

        if (!isNotificationPersistent()) {
            setHandlerActivity(NotificationHandler.LOCAL_UID, false);
        }

        if (mStreamHandler != null) {
            mStreamHandler.stop();
            setHandlerActivity(StreamHandler.LOCAL_UID, false);
        }

        /** We group these together under the notification, but they can easily be split. */
        if (mNotificationHandler != null) {
            mAlbumCoverHandler.stop();
            mRemoteControlClientHandler.stop();
            mNotificationHandler.stop();

            if (!isNotificationPersistent()) {
                /**
                 * Don't remove the status change listener here. It
                 * causes a bug with the weak linked list, somehow.
                 */
                MPD_ASYNC_HELPER.stopStatusMonitor();
                MPD_ASYNC_HELPER.stopNetworkMonitor(this);
                MPD_ASYNC_HELPER.disconnect();
            }
        }

        if (stopSelf) {
            /**
             * Any time we want to stopSelf, it happens that we want to abandon audio focus as well.
             */
            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(this);
            }
            if (!isServiceBusy()) {
                if (DEBUG) {
                    Log.d(TAG, "Stopping service in " +
                            ServiceBinder.MESSAGE_DELAY / DateUtils.SECOND_IN_MILLIS + " seconds.");
                }

                mMessageHandler.sendMessageToClients(REQUEST_UNBIND);
                mHandler.sendEmptyMessageDelayed(STOP_SELF, ServiceBinder.MESSAGE_DELAY);
            }
        }
    }

    /** This class handles all incoming messages. */
    private class MessageHandler implements Handler.Callback {

        /** Tracker for clients that are currently bound to the service binder. */
        private final List<Messenger> mServiceClients = new ArrayList<>(3);

        /**
         * This service tries to stop self, and if it can't it gives debug
         * messages as to possible reasons the service couldn't shut down.
         * Do not call this or stopSelf() directly, use windDownHandlers().
         */
        private void haltService() {
            /** This assumes services have been wound down prior to call. */
            if (hasActiveHandlers()) {
                if (DEBUG) {
                    Log.d(TAG, "Active handlers exist, cannot stopSelf().");
                }
            } else if (!mServiceClients.isEmpty()) {
                if (DEBUG) {
                    Log.d(TAG, mServiceClients.size()
                            + " clients still attached, cannot stopSelf().");
                }
            } else {
                stopSelf();
            }
        }

        /**
         * Handles messages from the ServiceBinder client.
         *
         * @param message Incoming message from the ServiceBinder client.
         */
        private void handleBinderMessages(final Message message) {
            if (DEBUG) {
                Log.d(TAG, "Message received: " + ServiceBinder.getHandlerValue(message.what));
            }

            switch (message.what) {
                case ServiceBinder.REGISTER_CLIENT:
                    if (!mServiceClients.contains(message.replyTo)) {
                        mServiceClients.add(message.replyTo);
                    }
                    break;
                case ServiceBinder.UNREGISTER_CLIENT:
                    mServiceClients.remove(message.replyTo);
                    break;
                default:
                    break;
            }
        }

        /**
         * Redirects messages to the proper method for further processing.
         *
         * @param msg Incoming message for redirecting.
         * @return If the message was handled.
         */
        @Override
        public final boolean handleMessage(final Message msg) {
            /**
             * ServiceBinder: 100
             * MPDroidService: 200
             * NotificationHandler: 300
             * StreamHandler: 400
             */
            boolean result = true;
            final int what = msg.what;
            if (what >= ServiceBinder.LOCAL_UID && what < LOCAL_UID) {
                handleBinderMessages(msg);
            } else if (what >= LOCAL_UID && what < NotificationHandler.LOCAL_UID) {
                handleServiceMessages(msg);
            } else if (what >= NotificationHandler.LOCAL_UID && what < StreamHandler.LOCAL_UID) {
                handleNotificationMessages(msg);
            } else if (what >= StreamHandler.LOCAL_UID) {
                handleStreamMessage(what);
            } else {
                result = false;
            }

            return result;
        }

        /**
         * Handles messages which ultimately effects the notification handler.
         *
         * @param msg The message with the 'what' to act upon.
         */
        private void handleNotificationMessages(final Message msg) {
            final int what = msg.what;
            Log.d(TAG, "Message received: " + NotificationHandler.getHandlerValue(what));

            switch (what) {
                case NotificationHandler.PERSISTENT_OVERRIDDEN:
                    mIsPersistentOverridden = msg.arg1 == ServiceBinder.TRUE;
                    /** Fall Through */
                case NotificationHandler.START:
                    startNotification();
                    break;
                case NotificationHandler.STOP:
                    setHandlerActivity(NotificationHandler.LOCAL_UID, false);
                    mNotificationOwnsService = false;
                    windDownHandlers(true);
                    sendMessageToClients(NotificationHandler.IS_ACTIVE, false);
                    break;
                default:
                    break;
            }
        }

        /** Handles the messages received for the outer Service class. */
        private void handleServiceMessages(final Message msg) {
            final int what = msg.what;
            Log.d(TAG, "Message received: " + getHandlerValue(what));

            switch (what) {
                case DISCONNECT_ON_NO_CONNECTION:
                    if (MPD_ASYNC_HELPER.oMPD.isConnected()) {
                        break;
                    }
                    /** Fall through */
                case WIND_DOWN_HANDLERS:
                    windDownHandlers(true);
                    break;
                case CONNECTION_INFO_CHANGED:
                    setConnectionSettings(msg.getData());
                    break;
                case STOP_SELF:
                    haltService();
                    break;
                case UPDATE_CLIENT_STATUS:
                    sendHandlerStatus();
                    break;
                default:
                    break;
            }
        }

        /**
         * A method to handle any messages with origin in the stream handling code.
         *
         * @param what The message to handle.
         */
        private void handleStreamMessage(final int what) {
            Log.d(TAG, "Received message: " + StreamHandler.getHandlerValue(what));
            switch (what) {
                case StreamHandler.BUFFERING_BEGIN:
                    mNotificationHandler.setMediaPlayerBuffering(true);
                    mRemoteControlClientHandler.setMediaPlayerBuffering(true);
                    break;
                case StreamHandler.REQUEST_NOTIFICATION_STOP:
                    if (mIsNotificationStarted && MPD_ASYNC_HELPER.oMPD.isConnected() &&
                            MPDStatus.MPD_STATE_PLAYING.equals(getMPDStatus().getState())) {
                        tryToGetAudioFocus();
                    }
                    streamRequestsNotificationStop();
                    break;
                case StreamHandler.START:
                    startStream();
                    break;
                case StreamHandler.STOP:
                    stopStream();
                    break;
                case StreamHandler.STREAMING_STOP:
                    setHandlerActivity(StreamHandler.LOCAL_UID, false);
                    /** Fall Through */
                case StreamHandler.STREAMING_PAUSE:
                    mNotificationHandler.setMediaPlayerWoundDown();
                    setupServiceHandler();
                    /** Fall Through */
                case StreamHandler.BUFFERING_END:
                    mRemoteControlClientHandler.setMediaPlayerBuffering(false);
                    mNotificationHandler.setMediaPlayerBuffering(false);
                    break;
                default:
                    break;
            }
        }

        /** Sends a message to all clients about all important handlers. */
        private void sendHandlerStatus() {
            sendMessageToClients(NotificationHandler.IS_ACTIVE, mIsNotificationStarted);
            sendMessageToClients(StreamHandler.IS_ACTIVE, mIsStreamStarted);
        }

        private void sendMessageToClients(final int what) {
            sendMessageToClients(what, false);
        }

        /** Sends a message to all clients about a specific important handler. */
        private void sendMessageToClients(final int what, final boolean isActive) {
            final Message msg;
            if (mServiceClients.isEmpty()) {
                if (DEBUG) {
                    Log.d(TAG, "No service clients. What: " + ServiceBinder.getHandlerValue(what));
                }
                msg = null;
            } else {
                msg = ServiceBinder.getBoolMessage(mHandler, what, isActive);
            }

            for (int iterator = mServiceClients.size() - 1; iterator >= 0; iterator--) {
                try {
                    mServiceClients.get(iterator).send(msg);
                } catch (final RemoteException e) {
                    /**
                     * The client is dead.  Remove it from the list; we are going through
                     * the list from back to front so this is safe to do inside the loop.
                     */
                    mServiceClients.remove(iterator);
                    Log.w(TAG, "Client died.", e);
                }
            }
        }

        /**
         * This processes the incoming (and likely changed) {@code ConnectionSettings} object.
         *
         * This method is necessary as the {@code MPDAsyncHelper}, which produces the
         * {@code ConnectionSettings} object, only changes in the remote process upon connection
         * settings change. It is then parceled, bundled and sent as a message here then processed
         * back into a {@code ConnectionSettings} object. It is then sent to our
         * {@code MPDAsyncHelper} instance.
         *
         * Once sent to this process instance {@code MPDAsyncHelper}, this will then call the
         * ConnectionInfoListener callback which calls the onConnectionConfigChange().
         *
         * @param bundle The incoming {@code ConnectionInfo} bundle.
         */
        private void setConnectionSettings(final Bundle bundle) {
            if (bundle == null) {
                Log.e(TAG, "Null bundle received");
            } else {
                final ClassLoader classLoader = ConnectionInfo.class.getClassLoader();
                bundle.setClassLoader(classLoader);
                final ConnectionInfo connectionInfo =
                        bundle.getParcelable(ConnectionInfo.BUNDLE_KEY);
                MPD_ASYNC_HELPER.setConnectionSettings(connectionInfo);
            }
        }

        /**
         * If the stream handler requests stop, this
         * method chooses whether to grant the request.
         */
        private void streamRequestsNotificationStop() {
            if (mStreamOwnsService && !isNotificationPersistent()) {
                sendMessageToClients(NotificationHandler.IS_ACTIVE, false);
                sendMessageToClients(ServiceBinder.SET_PERSISTENT, false);
                mNotificationHandler.stop();
                mStreamOwnsService = false;
            } else {
                tryToGetAudioFocus();
            }
        }
    }
}
