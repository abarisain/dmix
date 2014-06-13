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

import com.namelessdev.mpdroid.MPDApplication;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * This service schedules various things that run without MPDroid.
 */
public final class MPDroidService extends Service implements AlbumCoverHandler.Callback,
        Handler.Callback,
        StatusChangeListener {

    private static MPDApplication sApp = MPDApplication.getInstance();

    private static final String TAG = "MPDroidService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid." + TAG + '.';

    /**
     * This will close the notification, no matter the notification state.
     */
    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME
            + "ACTION_STOP";

    /**
     * This readies the notification in accordance with the current state.
     */
    public static final String ACTION_START = FULLY_QUALIFIED_NAME
            + "ACTION_START";

    private static final int DELAYED_DISCONNECT = 11;

    private static final int DELAYED_PAUSE = 12;

    private final Handler mHandler = new Handler(this);

    private AlbumCoverHandler mAlbumCoverHandler = null;

    private AudioManager mAudioManager = null;

    private Music mCurrentTrack = null;

    private boolean mIsAudioFocusedOnThis = false;

    private NotificationHandler mNotificationHandler = null;

    private RemoteControlClientHandler mRemoteControlClientHandler = null;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger mServiceMessenger = null;

    private boolean mStreamingOwnsNotification = false;

    private boolean mStreamingServiceWoundDown = false;

    /**
     * A simple method to return a status with error logging.
     *
     * @return An MPDStatus object.
     */
    static MPDStatus getMPDStatus() {
        MPDStatus mpdStatus = null;
        try {
            mpdStatus = sApp.oMPDAsyncHelper.oMPD.getStatus();
        } catch (final MPDServerException e) {
            Log.d(TAG, "Couldn't retrieve a status object.", e);
        }

        return mpdStatus;
    }

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        if (connected) {
            if (sApp.oMPDAsyncHelper.getConnectionSettings().persistentNotification) {
                stateChanged(getMPDStatus(), MPDStatus.MPD_STATE_UNKNOWN);
            }
        } else {
            final long idleDelay = 10000L; /** Give 10 Seconds for Network Problems */

            if (!mHandler.hasMessages(DELAYED_DISCONNECT)) {
                final Message msg = mHandler.obtainMessage(DELAYED_DISCONNECT);
                mHandler.sendMessageDelayed(msg, idleDelay);
            }
        }
    }

    @Override
    public final boolean handleMessage(final Message message) {
        boolean result = hasHandledStreamingMessage(message.what);

        if (!result) {
            result = true;
            switch (message.what) {
                case DELAYED_DISCONNECT:
                    if (sApp.oMPDAsyncHelper.oMPD.isConnected()) {
                        break;
                    }
                    /** Fall through */
                case DELAYED_PAUSE:
                    shutdownNotification();
                    break;
                default:
                    result = false;
                    break;
            }
        }

        return result;
    }

    /**
     * A method to handle any messages with origin in the stream handling code.
     *
     * @param what The message to handle.
     * @return Whether this method had a streaming message that was handled.
     */
    private boolean hasHandledStreamingMessage(final int what) {
        boolean result = true;
        final MPDStatus mpdStatus = getMPDStatus();

        switch (what) {
            case StreamingService.BUFFERING_BEGIN:
                /** If the notification was requested by StreamingService, set it here. */
                if (!sApp.isMPDroidServiceRunning() &&
                        sApp.isStreamingServiceRunning()) {
                    mStreamingOwnsNotification = true;
                }
                mNotificationHandler.setMediaPlayerBuffering(true);
                mRemoteControlClientHandler.setMediaPlayerBuffering(true);
                mStreamingServiceWoundDown = false;
                break;
            case StreamingService.REQUEST_NOTIFICATION_STOP:
                if (mStreamingOwnsNotification &&
                        !sApp.isNotificationPersistent()) {
                    stopSelf();
                    mStreamingOwnsNotification = false;
                } else {
                    tryToGetAudioFocus();
                    stateChanged(getMPDStatus(), null);
                }
                break;
            case StreamingService.SERVICE_WOUND_DOWN:
                mNotificationHandler.setMediaPlayerWoundDown();
                mStreamingServiceWoundDown = true;
                break;
            case StreamingService.BUFFERING_END:
            case StreamingService.BUFFERING_ERROR:
            case StreamingService.STREAMING_STOP:
                mRemoteControlClientHandler.setMediaPlayerBuffering(false);
                mNotificationHandler.setMediaPlayerBuffering(false);
                stateChanged(getMPDStatus(), MPDStatus.MPD_STATE_UNKNOWN);
                break;
            default:
                result = false;
                break;
        }

        return result;
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
    public IBinder onBind(final Intent intent) {
        return mServiceMessenger.getBinder();
    }

    /**
     * This is called when cover art needs to be updated due to server information change.
     *
     * @param albumCover The current album cover bitmap.
     */
    @Override
    public void onCoverUpdate(final Bitmap albumCover, final String albumCoverPath) {
        mRemoteControlClientHandler.update(albumCover);
        mNotificationHandler.setAlbumCover(albumCover, albumCoverPath);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /** Setup the service messenger to communicate with Handler.Callback. */
        mServiceMessenger = new Messenger(new Handler(this));

        //TODO: Acquire a network wake lock here if the user wants us to !
        //Otherwise we'll just shut down on screen off and reconnect on screen on
        sApp.addConnectionLock(this);
        sApp.oMPDAsyncHelper.addStatusChangeListener(this);

        mAlbumCoverHandler = new AlbumCoverHandler();
        mAlbumCoverHandler.addCallback(this);

        mNotificationHandler = new NotificationHandler(this);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        mRemoteControlClientHandler = new RemoteControlClientHandler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Removing connection lock");
        sApp.removeConnectionLock(this);
        sApp.oMPDAsyncHelper.removeStatusChangeListener(this);
        mHandler.removeCallbacksAndMessages(this);

        windDownResources();

        if (mNotificationHandler != null) {
            mNotificationHandler.onDestroy();
        }

        mAlbumCoverHandler.onDestroy();
        mRemoteControlClientHandler.onDestroy();

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        final String action = intent.getAction();

        Log.d(TAG, "Starting service, received command, action="
                + action + " from intent: " + intent);

        /** An action must be submitted to start this service. */
        if (action == null || !ACTION_START.equals(action)) {
            Log.e(TAG, "Service started without action, stopping...");
            stopSelf();
        }

        stateChanged(getMPDStatus(), MPDStatus.MPD_STATE_UNKNOWN);

        /**
         * Means we started the service, but don't want
         * it to restart in case it's killed.
         */
        return START_NOT_STICKY;
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
     * This is the idle delay for shutting down this service after inactivity
     * (in milliseconds). This idle is also longer than StreamingService to
     * avoid being unnecessarily brought up to shut right back down.
     */
    private void setupServiceHandler() {
        final long idleDelay = 330000L; /** 5 Minutes 30 Seconds */
        final Message msg = mHandler.obtainMessage(DELAYED_PAUSE);
        mHandler.sendMessageDelayed(msg, idleDelay);
    }

    private void shutdownNotification() {
        if (sApp.isNotificationPersistent()) {
            windDownResources();
        } else {
            stopSelf();
        }
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final String oldState) {
        if (mpdStatus == null) {
            Log.w(TAG, "Null mpdStatus received in stateChanged");
        } else {
            mRemoteControlClientHandler.stateChanged(mpdStatus);
            switch (mpdStatus.getState()) {
                case MPDStatus.MPD_STATE_PLAYING:
                    if (!MPDStatus.MPD_STATE_PAUSED.equals(oldState)) {
                        updateTrack(mpdStatus);
                    }
                    mNotificationHandler.setPlayState(true);
                    mHandler.removeMessages(DELAYED_PAUSE);
                    tryToGetAudioFocus();
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                    if (sApp.isNotificationPersistent()) {
                        windDownResources(); /** Hide immediately, requires user intervention */
                    } else {
                        stopSelf();
                    }
                    break;
                case MPDStatus.MPD_STATE_PAUSED:
                    if (!MPDStatus.MPD_STATE_PLAYING.equals(oldState)) {
                        updateTrack(mpdStatus);
                    }
                    mNotificationHandler.setPlayState(false);
                    if (!mHandler.hasMessages(DELAYED_PAUSE)) {
                        setupServiceHandler();
                    }
                    break;
                default:
                    break;
            }
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
        if ((!sApp.isStreamingServiceRunning() || mStreamingServiceWoundDown)
                && !mIsAudioFocusedOnThis) {
            final int result = mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            Log.d(TAG, "Requested audio focus, received code: " + result);

            mIsAudioFocusedOnThis = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void updateTrack(final MPDStatus mpdStatus) {
        if (mpdStatus == null) {
            Log.e(TAG, "Cannot update current track, services may be out of sync.");
        } else if (sApp.oMPDAsyncHelper.oMPD.isConnected()) {
            final int songPos = mpdStatus.getSongPos();
            mCurrentTrack = sApp.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
            if (mCurrentTrack != null) {
                mAlbumCoverHandler.update(mCurrentTrack.getAlbumInfo());
                mNotificationHandler.setNewTrack(mCurrentTrack);
                mRemoteControlClientHandler.update(mCurrentTrack);
            }
        }
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
    }

    /**
     * Used when persistent notification is enabled in
     * leu of selfStop() and used during selfStop().
     */
    private void windDownResources() {
        Log.d(TAG, "windDownResources()");

        if (mAudioManager != null) {
            mIsAudioFocusedOnThis = false;
            mAudioManager.abandonAudioFocus(null);
        }

        if (mNotificationHandler != null) {
            mNotificationHandler.onDestroy();
        }
    }
}
