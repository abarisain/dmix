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
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.RemoteControlReceiver;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.ICoverRetriever;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * This service schedules various things that run without MPDroid.
 */
public final class MPDroidService extends Service implements Handler.Callback,
        StatusChangeListener {

    private static MPDApplication sApp = MPDApplication.getInstance();

    private static final String TAG = "MPDroidService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid." + TAG + '.';

    /**
     * This will close the notification, no matter the notification state.
     */
    public static final String ACTION_CLOSE_NOTIFICATION = FULLY_QUALIFIED_NAME
            + "CLOSE_NOTIFICATION";

    private static final PendingIntent NOTIFICATION_CLOSE =
            buildPendingIntent(ACTION_CLOSE_NOTIFICATION);

    /**
     * This readies the notification in accordance with the current state.
     */
    public static final String ACTION_START = FULLY_QUALIFIED_NAME
            + "NOTIFICATION_OPEN";

    /**
     * The ID we use for the notification (the onscreen alert that appears
     * at the notification area at the top of the screen as an icon -- and
     * as text as well if the user expands the notification area).
     */
    private static final int NOTIFICATION_ID = 1;

    private static final int NOTIFICATION_ICON_HEIGHT = sApp.getResources()
            .getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

    private static final int NOTIFICATION_ICON_WIDTH = sApp.getResources()
            .getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

    private static final PendingIntent NOTIFICATION_NEXT =
            buildPendingIntent(MPDControl.ACTION_NEXT);

    private static final PendingIntent NOTIFICATION_PAUSE =
            buildPendingIntent(MPDControl.ACTION_PAUSE);

    private static final PendingIntent NOTIFICATION_PLAY =
            buildPendingIntent(MPDControl.ACTION_PLAY);

    private static final PendingIntent NOTIFICATION_PREVIOUS =
            buildPendingIntent(MPDControl.ACTION_PREVIOUS);

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

    private boolean mMediaPlayerServiceIsBuffering = false;

    private Notification mNotification = null;

    private NotificationManager mNotificationManager = null;

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
     * Build a static pending intent for use with the notification button controls.
     *
     * @param action The ACTION intent string.
     * @return The pending intent.
     */
    private static PendingIntent buildPendingIntent(final String action) {
        final Intent intent = new Intent(sApp, RemoteControlReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(sApp, 0, intent, 0);
    }

    /**
     * This builds the static bits of a new collapsed notification
     *
     * @return Returns a notification builder object.
     */
    private static NotificationCompat.Builder buildStaticCollapsedNotification() {
        /** Build the click PendingIntent */
        final Intent musicPlayerActivity = new Intent(sApp, MainMenuActivity.class);
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(sApp);
        stackBuilder.addParentStack(MainMenuActivity.class);
        stackBuilder.addNextIntent(musicPlayerActivity);
        final PendingIntent notificationClick = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(sApp);
        builder.setSmallIcon(R.drawable.icon_bw);
        builder.setContentIntent(notificationClick);
        builder.setStyle(new NotificationCompat.BigTextStyle());

        return builder;
    }

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

    /**
     * buildExpandedNotification builds upon the collapsed notification resources to create
     * the resources necessary for the expanded notification RemoteViews.
     *
     * @return The expanded notification RemoteViews.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private RemoteViews buildExpandedNotification() {
        final RemoteViews resultView;

        if (mNotification == null || mNotification.bigContentView == null) {
            resultView = new RemoteViews(getPackageName(), R.layout.notification_big);
        } else {
            resultView = mNotification.bigContentView;
        }

        buildBaseNotification(resultView);

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mMediaPlayerServiceIsBuffering) {
            resultView.setTextViewText(R.id.notificationAlbum, mCurrentTrack.getArtist());
        } else {
            resultView.setTextViewText(R.id.notificationAlbum, mCurrentTrack.getAlbum());
        }

        resultView.setOnClickPendingIntent(R.id.notificationPrev, NOTIFICATION_PREVIOUS);

        return resultView;
    }

    /**
     * This generates the collapsed notification from base.
     *
     * @return The collapsed notification resources for RemoteViews.
     */
    private NotificationCompat.Builder buildNewCollapsedNotification() {
        final RemoteViews resultView = buildBaseNotification(
                new RemoteViews(getPackageName(), R.layout.notification));
        return buildStaticCollapsedNotification().setContent(resultView);
    }

    /**
     * This method builds the base, otherwise known as the collapsed notification. The expanded
     * notification method builds upon this method.
     *
     * @param resultView The RemoteView to begin with, be it new or from the current notification.
     * @return The base, otherwise known as, collapsed notification resources for RemoteViews.
     */
    private RemoteViews buildBaseNotification(final RemoteViews resultView) {
        final String title = mCurrentTrack.getTitle();

        /** If in streaming, the notification should be persistent. */
        if (sApp.getApplicationState().streamingMode && !mStreamingServiceWoundDown) {
            resultView.setViewVisibility(R.id.notificationClose, View.GONE);
        } else {
            resultView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
            resultView.setOnClickPendingIntent(R.id.notificationClose, NOTIFICATION_CLOSE);
        }

        if (MPDStatus.MPD_STATE_PLAYING.equals(getMPDStatus().getState())) {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, NOTIFICATION_PAUSE);
            resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_pause);
        } else {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, NOTIFICATION_PLAY);
            resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_play);
        }

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mMediaPlayerServiceIsBuffering) {
            resultView.setTextViewText(R.id.notificationTitle, getString(R.string.buffering));
            resultView.setTextViewText(R.id.notificationArtist, title);
        } else {
            resultView.setTextViewText(R.id.notificationTitle, title);
            resultView.setTextViewText(R.id.notificationArtist, mCurrentTrack.getArtist());
        }

        resultView.setOnClickPendingIntent(R.id.notificationNext, NOTIFICATION_NEXT);

        if (mAlbumCover != null) {
            resultView.setImageViewUri(R.id.notificationIcon, Uri.parse(mAlbumCoverPath));
        }

        return resultView;
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
                mRemoteControlClientHandler.setMediaPlayerBuffering(true);
                mMediaPlayerServiceIsBuffering = true;
                stateChanged(getMPDStatus(), null);
                break;
            case StreamingService.REQUEST_NOTIFICATION_STOP:
                if (mStreamingOwnsNotification &&
                        !sApp.getApplicationState().persistentNotification) {
                    sApp.getApplicationState().persistentNotification = false;
                    sApp.getApplicationState().notificationMode = false;
                    stopSelf();

                    mStreamingOwnsNotification = false;
                } else {
                    tryToGetAudioFocus();
                    stateChanged(getMPDStatus(), null);
                }
                break;
            case StreamingService.SERVICE_WOUND_DOWN:
                mStreamingServiceWoundDown = true;
                break;
            case StreamingService.SERVICE_WOUND_UP:
                /** If the notification was requested by StreamingService, set it here. */
                if (!sApp.getApplicationState().notificationMode &&
                        sApp.getApplicationState().streamingMode) {
                    mStreamingOwnsNotification = true;
                    sApp.getApplicationState().notificationMode = true;
                }
                mStreamingServiceWoundDown = false;
                break;
            case StreamingService.BUFFERING_END:
            case StreamingService.BUFFERING_ERROR:
            case StreamingService.STREAMING_STOP:
                mRemoteControlClientHandler.setMediaPlayerBuffering(false);
                mMediaPlayerServiceIsBuffering = false;
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

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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

        sApp.getApplicationState().notificationMode = false;

        windDownResources();

        if (mAlbumCover != null && !mAlbumCover.isRecycled()) {
            mAlbumCover.recycle();
        }

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
        if (action == null || !ACTION_START.equals(action) ||
                !sApp.getApplicationState().notificationMode) {
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
     * Build a new notification or perform an update on an existing notification.
     */
    private void setupNotification() {
        Log.d(TAG, "update notification: " + mCurrentTrack.getArtist() + " - " + mCurrentTrack
                .getTitle());

        NotificationCompat.Builder builder = null;
        RemoteViews expandedNotification = null;

        /** These have a very specific order. */
        if (mNotification == null || mNotification.contentView == null) {
            builder = buildNewCollapsedNotification();
        } else {
            buildBaseNotification(mNotification.contentView);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            expandedNotification = buildExpandedNotification();
        }

        if (builder != null) {
            mNotification = builder.build();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNotification.bigContentView = expandedNotification;
        }

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        startForeground(NOTIFICATION_ID, mNotification);
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
        if (sApp.getApplicationState().persistentNotification) {
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
                    if (sApp.getApplicationState().persistentNotification) {
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
        if ((!sApp.getApplicationState().streamingMode || mStreamingServiceWoundDown)
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
            setupNotification();
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

        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }
}
