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
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.ICoverRetriever;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This service schedules various things that run without MPDroid.
 */
public final class MPDroidService extends Service implements Handler.Callback,
        NotificationHandler.Callback,
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

    private static final int NOTIFICATION_ICON_HEIGHT = sApp.getResources()
            .getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

    private static final int NOTIFICATION_ICON_WIDTH = sApp.getResources()
            .getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

    private final Handler mDelayedDisconnectionHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            if (!sApp.oMPDAsyncHelper.oMPD.isConnected()) {
                shutdownNotification();
            }
        }
    };

    private final boolean mIsAlbumCacheEnabled = PreferenceManager.getDefaultSharedPreferences(sApp)
            .getBoolean(CoverManager.PREFERENCE_CACHE, true);

    private Bitmap mAlbumCover = null;

    private String mAlbumCoverPath = null;

    private AudioManager mAudioManager = null;

    private Music mCurrentTrack = null;

    private boolean mIsAudioFocusedOnThis = false;

    private NotificationHandler mNotificationHandler = null;

    private RemoteControlClientHandler mRemoteControlClientHandler = null;

    private boolean mServiceHandlerActive = false;

    /** Set up the message handler. */
    private final Handler mDelayedPauseHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            mServiceHandlerActive = false;
            shutdownNotification();
        }
    };

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
    private static MPDStatus getMPDStatus() {
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
                stateChanged(getMPDStatus(), null);
            }
        } else {
            final long idleDelay = 10000L; /** Give 10 Seconds for Network Problems */

            final Message msg = mDelayedDisconnectionHandler.obtainMessage();
            mDelayedDisconnectionHandler.sendMessageDelayed(msg, idleDelay);
        }
    }

    @Override
    public final boolean handleMessage(final Message message) {
        switch (message.what) {
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
                stateChanged(getMPDStatus(), null);
                break;
            default:
                break;
        }
        return false;
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

    @Override
    public void onCreate() {
        super.onCreate();

        /** Setup the service messenger to communicate with Handler.Callback. */
        mServiceMessenger = new Messenger(new Handler(this));

        //TODO: Acquire a network wake lock here if the user wants us to !
        //Otherwise we'll just shut down on screen off and reconnect on screen on
        sApp.addConnectionLock(this);
        sApp.oMPDAsyncHelper.addStatusChangeListener(this);

        mNotificationHandler = new NotificationHandler();
        mNotificationHandler.addCallback(this);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        mRemoteControlClientHandler = new RemoteControlClientHandler();

        tryToGetAudioFocus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Removing connection lock");
        sApp.removeConnectionLock(this);
        sApp.oMPDAsyncHelper.removeStatusChangeListener(this);
        mDelayedPauseHandler.removeCallbacksAndMessages(null);

        windDownResources();

        if (mNotificationHandler != null) {
            mNotificationHandler.onDestroy();
        }

        if (mAlbumCover != null && !mAlbumCover.isRecycled()) {
            mAlbumCover.recycle();
        }

        mRemoteControlClientHandler.onDestroy();

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
    }

    /**
     * This is when an updated notification is available.
     *
     * @param notification The updated notification object.
     */
    @Override
    public void onNotificationUpdate(final Notification notification) {
        startForeground(NotificationHandler.NOTIFICATION_ID, notification);
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

        stateChanged(getMPDStatus(), null);

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
            updateCurrentMusic(mpdStatus);
            updateAlbumCover();
            mRemoteControlClientHandler.update(mCurrentTrack, mAlbumCover);
            updateNotification();
        }
    }

    @Override
    public void randomChanged(final boolean random) {
    }

    @Override
    public void repeatChanged(final boolean repeating) {
    }

    /**
     * This method retrieves a path to an album cover bitmap from the cache.
     *
     * @return String A path to a cached album cover bitmap.
     */
    private String retrieveCoverArtPath() {
        final ICoverRetriever cache = new CachedCover();
        String coverArtPath = null;
        String[] coverArtPaths = null;

        try {
            coverArtPaths = cache.getCoverUrl(mCurrentTrack.getAlbumInfo());
        } catch (final Exception e) {
            Log.d(TAG, "Failed to get the cover URL from the cache.", e);
        }

        if (coverArtPaths != null && coverArtPaths.length > 0) {
            coverArtPath = coverArtPaths[0];
        }
        return coverArtPath;
    }

    /**
     * This is the idle delay for shutting down this service after inactivity
     * (in milliseconds). This idle is also longer than StreamingService to
     * avoid being unnecessarily brought up to shut right back down.
     */
    private void setupServiceHandler() {
        final long idleDelay = 630000L; /** 10 Minutes 30 Seconds */
        final Message msg = mDelayedPauseHandler.obtainMessage();
        mServiceHandlerActive = true;
        mDelayedPauseHandler.sendMessageDelayed(msg, idleDelay);
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
                        updateCurrentMusic(mpdStatus);
                        updateAlbumCover();
                    }
                    stopServiceHandler();
                    tryToGetAudioFocus();
                    updateNotification();
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
                        updateCurrentMusic(mpdStatus);
                        updateAlbumCover();
                    }
                    if (!mServiceHandlerActive) {
                        setupServiceHandler();
                    }
                    updateNotification();
                    break;
                default:
                    break;
            }
        }
    }

    /** Kills any active service handlers. */
    private void stopServiceHandler() {
        /** If we have a message in the queue, remove it. */
        mDelayedPauseHandler.removeCallbacksAndMessages(null);
        mServiceHandlerActive = false; /** No notification if stopped or paused. */
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        mRemoteControlClientHandler.updateSeekTime(0L);
        updateCurrentMusic(mpdStatus);
        updateAlbumCover();
        mRemoteControlClientHandler.update(mCurrentTrack, mAlbumCover);
        updateNotification();
    }

    /**
     * We try to get audio focus, but don't really try too hard.
     * We just want the lock screen cover art.
     */
    private void tryToGetAudioFocus() {
        if ((!sApp.isStreamingServiceRunning() || mStreamingServiceWoundDown)
                && !mIsAudioFocusedOnThis) {
            Log.d(TAG, "requesting audio focus");
            final int result = mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            mIsAudioFocusedOnThis = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void updateAlbumCover() {
        if (mIsAlbumCacheEnabled) {
            updateAlbumCoverWithCached();
        } /** TODO: Add no cache option */
    }

    /**
     * This method updates mAlbumCover if it is different than currently playing, if cache is
     * enabled.
     */
    private void updateAlbumCoverWithCached() {
        final String coverArtPath = retrieveCoverArtPath();

        if (coverArtPath != null && !coverArtPath.equals(mAlbumCoverPath)) {
            if (mAlbumCover != null && !mAlbumCover.isRecycled()) {
                mAlbumCover.recycle();
            }

            mAlbumCoverPath = coverArtPath;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                /**
                 * Don't resize; it WOULD be nice to use the standard 64x64 large notification
                 * size here, but KitKat and MPDroid allow fullscreen lock screen AlbumArt and
                 * 64x64 looks pretty bad on a higher DPI device.
                 */
                /** TODO: Maybe inBitmap stuff here? */
                mAlbumCover = BitmapFactory.decodeFile(coverArtPath);
            } else {
                mAlbumCover = Tools.decodeSampledBitmapFromPath(coverArtPath,
                        NOTIFICATION_ICON_WIDTH, NOTIFICATION_ICON_HEIGHT, false);
            }
        }
    }

    private void updateNotification() {
        if (mCurrentTrack != null) {
            mNotificationHandler.update(mCurrentTrack, mAlbumCover, mAlbumCoverPath);
        }
    }

    private void updateCurrentMusic(final MPDStatus mpdStatus) {
        if (mpdStatus == null) {
            Log.e(TAG, "Cannot update current track, services may be out of sync.");
        } else {
            final long loopFrequency = 50L;

            int songPos = mpdStatus.getSongPos();
            mCurrentTrack = sApp.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);

            /** Workaround for Bug #558 */
            while (sApp.oMPDAsyncHelper.oMPD.isConnected() &&
                    mpdStatus.getPlaylistLength() != 0 && mCurrentTrack == null) {
                Log.e(TAG, "Current music out of sync, looping..");
                synchronized (this) {
                    try {
                        wait(loopFrequency);
                    } catch (final InterruptedException ignored) {
                    }
                }

                songPos = mpdStatus.getSongPos();
                mCurrentTrack = sApp.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
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
        stopForeground(true);

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }

        if (mNotificationHandler != null) {
            mNotificationHandler.cancelNotification();
        }
    }
}
