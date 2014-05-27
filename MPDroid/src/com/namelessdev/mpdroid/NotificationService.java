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

package com.namelessdev.mpdroid;

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
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Date;

/**
 * A service that handles the Notification, RemoteControlClient, MediaButtonReceiver and
 * incoming MPD command intents.
 */
public final class NotificationService extends Service implements Handler.Callback,
        StatusChangeListener {

    private static final String TAG = "NotificationService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid." + TAG + '.';

    /**
     * This will close the notification, no matter the notification state.
     */
    public static final String ACTION_CLOSE_NOTIFICATION = FULLY_QUALIFIED_NAME
            + "CLOSE_NOTIFICATION";

    /**
     * This readies the notification in accordance with the current state.
     */
    public static final String ACTION_OPEN_NOTIFICATION = FULLY_QUALIFIED_NAME
            + "NOTIFICATION_OPEN";

    /** Pre-built PendingIntent actions */
    private static final PendingIntent notificationClose =
            buildStaticPendingIntent(ACTION_CLOSE_NOTIFICATION);

    private static final PendingIntent notificationNext =
            buildStaticPendingIntent(MPDControl.ACTION_NEXT);

    private static final PendingIntent notificationPause =
            buildStaticPendingIntent(MPDControl.ACTION_PAUSE);

    private static final PendingIntent notificationPlay =
            buildStaticPendingIntent(MPDControl.ACTION_PLAY);

    private static final PendingIntent notificationPrevious =
            buildStaticPendingIntent(MPDControl.ACTION_PREVIOUS);

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    private static final int NOTIFICATION_ID = 1;

    /** Set up the message handler. */
    private final Handler delayedPauseHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            shutdownNotification();
        }
    };

    private final Handler delayedDisconnectionHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            if (!app.oMPDAsyncHelper.oMPD.isConnected()) {
                shutdownNotification();
            }
        }
    };

    private final MPDApplication app = MPDApplication.getInstance();

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger serviceMessenger = null;

    private RemoteControlClient mRemoteControlClient = null;

    // The component name of MusicIntentReceiver, for use with media button and remote control APIs
    private ComponentName mMediaButtonReceiverComponent = null;

    private AudioManager mAudioManager = null;

    private boolean isAudioFocusedOnThis = false;

    private NotificationManager mNotificationManager = null;

    private Notification mNotification = null;

    private Music mCurrentMusic = null;

    private boolean mediaPlayerServiceIsBuffering = false;

    private boolean serviceHandlerActive = false;

    private boolean streamingServiceWoundDown = false;

    /**
     * Last time the status was refreshed
     */
    private long lastStatusRefresh = 0L;

    /**
     * What was the elapsed time (in ms) when the last status refresh happened?
     * Use this for guessing the elapsed time for the lock screen.
     */
    private long lastKnownElapsed = 0L;

    private boolean notificationAutomaticallyGenerated = false;

    private Bitmap mAlbumCover = null;

    private String mAlbumCoverPath = null;

    /**
     * Build a static pending intent for use with the notification button controls.
     *
     * @param action The ACTION intent string.
     * @return The pending intent.
     */
    private static PendingIntent buildStaticPendingIntent(final String action) {
        final Intent intent = new Intent(MPDApplication.getInstance(), RemoteControlReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(MPDApplication.getInstance(), 0, intent, 0);
    }

    /**
     * This builds the static bits of a new collapsed notification
     *
     * @return Returns a notification builder object.
     */
    private static NotificationCompat.Builder buildStaticCollapsedNotification() {
        /** Build the click PendingIntent */
        final Intent musicPlayerActivity = new Intent(MPDApplication.getInstance(),
                MainMenuActivity.class);
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(MPDApplication.getInstance());
        stackBuilder.addParentStack(MainMenuActivity.class);
        stackBuilder.addNextIntent(musicPlayerActivity);
        final PendingIntent notificationClick = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(MPDApplication.getInstance());
        builder.setSmallIcon(R.drawable.icon_bw);
        builder.setContentIntent(notificationClick);
        builder.setStyle(new NotificationCompat.BigTextStyle());

        return builder;
    }

    /**
     * This registers some media buttons via the RemoteControlReceiver.class which will take
     * action by intent to this onStartCommand().
     */
    private void registerMediaButtons() {
        mMediaButtonReceiverComponent = new ComponentName(this, RemoteControlReceiver.class);
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

        final Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(mMediaButtonReceiverComponent);
        mRemoteControlClient = new RemoteControlClient(PendingIntent
                .getBroadcast(this /*context*/, 0 /*requestCode, ignored*/,
                        intent /*intent*/, 0 /*flags*/));

        final int controlFlags = RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                RemoteControlClient.FLAG_KEY_MEDIA_STOP;

        mRemoteControlClient.setTransportControlFlags(controlFlags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            enableSeeking(controlFlags);
        }

        mAudioManager.registerRemoteControlClient(mRemoteControlClient);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /** Setup the service messenger to communicate with Handler.Callback. */
        serviceMessenger = new Messenger(new Handler(this));

        //TODO: Acquire a network wake lock here if the user wants us to !
        //Otherwise we'll just shut down on screen off and reconnect on screen on
        app.addConnectionLock(this);
        app.oMPDAsyncHelper.addStatusChangeListener(this);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        registerMediaButtons();
        tryToGetAudioFocus();
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
        if (action != null && ACTION_OPEN_NOTIFICATION.equals(action) &&
                app.getApplicationState().notificationMode) {
            stateChanged(getStatus(), null);
        } else {
            Log.e(TAG, "NotificationService started without action, stopping...");
            stopSelf();
        }

        /**
         * Means we started the service, but don't want
         * it to restart in case it's killed.
         */
        return START_NOT_STICKY;
    }

    /**
     * A simple method to enable lock screen seeking on 4.3 and higher.
     *
     * @param controlFlags The control flags you set beforehand, so that we can add our
     *                     required flag
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void enableSeeking(final int controlFlags) {
        mRemoteControlClient.setTransportControlFlags(controlFlags |
                RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE);

        /* Allows Android to show the song position */
        mRemoteControlClient.setOnGetPlaybackPositionListener(
                new RemoteControlClient.OnGetPlaybackPositionListener() {
                    /**
                     * Android's callback that queries us for the elapsed time
                     * Here, we are guessing the elapsed time using the last time we
                     * updated the elapsed time and its value at the time.
                     *
                     * @return The guessed song position
                     */
                    @Override
                    public long onGetPlaybackPosition() {
                        /**
                         * If we don't know the position, return
                         * a negative value as per the API spec.
                         */
                        long result = -1L;

                        if (lastStatusRefresh > 0L) {
                            result = lastKnownElapsed + new Date().getTime() - lastStatusRefresh;
                        }
                        return result;
                    }
                }
        );

        /* Allows Android to seek */
        mRemoteControlClient.setPlaybackPositionUpdateListener(
                new RemoteControlClient.OnPlaybackPositionUpdateListener() {
                    /**
                     * Android's callback for when the user seeks using the remote
                     * control.
                     *
                     * @param newPositionMs The position in MS where we should seek
                     */
                    @Override
                    public void onPlaybackPositionUpdate(final long newPositionMs) {
                        MPDControl.run(MPDControl.ACTION_SEEK, newPositionMs /
                                DateUtils.SECOND_IN_MILLIS);
                        mRemoteControlClient.setPlaybackState(getRemoteState(getStatus()),
                                newPositionMs, 1.0f);
                    }
                }
        );
    }

    /**
     * We try to get audio focus, but don't really try too hard.
     * We just want the lock screen cover art.
     */
    private void tryToGetAudioFocus() {
        if ((!app.getApplicationState().streamingMode || streamingServiceWoundDown)
                && !isAudioFocusedOnThis) {
            Log.d(TAG, "requesting audio focus");
            final int result = mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            isAudioFocusedOnThis = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    /**
     * A simple method to return a status with error logging.
     *
     * @return An MPDStatus object.
     */
    private MPDStatus getStatus() {
        MPDStatus mpdStatus = null;
        try {
            mpdStatus = app.oMPDAsyncHelper.oMPD.getStatus();
        } catch (final MPDServerException e) {
            Log.d(TAG, "Couldn't retrieve a status object.", e);
        }

        return mpdStatus;
    }

    /**
     * Get the RemoteControlClient status for the corresponding MPDStatus
     *
     * @param mpdStatus MPDStatus to parse
     * @return state to give to RemoteControlClient
     */
    private int getRemoteState(final MPDStatus mpdStatus) {
        final int state;

        if (mpdStatus == null) {
            state = RemoteControlClient.PLAYSTATE_ERROR;
        } else if (mediaPlayerServiceIsBuffering) {
            state = RemoteControlClient.PLAYSTATE_BUFFERING;
        } else {
            switch (mpdStatus.getState()) {
                case MPDStatus.MPD_STATE_PLAYING:
                    state = RemoteControlClient.PLAYSTATE_PLAYING;
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                    state = RemoteControlClient.PLAYSTATE_STOPPED;
                    break;
                default:
                    state = RemoteControlClient.PLAYSTATE_PAUSED;
                    break;
            }
        }
        return state;
    }

    @Override
    public final boolean handleMessage(final Message msg) {
        switch (msg.what) {
            case StreamingService.BUFFERING_BEGIN:
                mediaPlayerServiceIsBuffering = true;
                stateChanged(getStatus(), null);
                break;
            case StreamingService.REQUEST_NOTIFICATION_STOP:
                if (notificationAutomaticallyGenerated &&
                        !app.getApplicationState().persistentNotification) {
                    app.getApplicationState().persistentNotification = false;
                    app.getApplicationState().notificationMode = false;
                    stopSelf();

                    notificationAutomaticallyGenerated = false;
                } else {
                    tryToGetAudioFocus();
                    stateChanged(getStatus(), null);
                }
                break;
            case StreamingService.SERVICE_WOUND_DOWN:
                streamingServiceWoundDown = true;
                break;
            case StreamingService.SERVICE_WOUND_UP:
                /** If the notification was requested by StreamingService, set it here. */
                if (!app.getApplicationState().notificationMode &&
                        app.getApplicationState().streamingMode) {
                    notificationAutomaticallyGenerated = true;
                    app.getApplicationState().notificationMode = true;
                }
                streamingServiceWoundDown = false;
                break;
            case StreamingService.BUFFERING_END:
                mediaPlayerServiceIsBuffering = false;
                /** Fall through */
            case StreamingService.BUFFERING_ERROR:
            case StreamingService.STREAMING_STOP:
                stateChanged(getStatus(), null);
                break;
            default:
                break;
        }
        return false;
    }

    /**
     * This method will update the current playing track, notification views,
     * the RemoteControlClient & the cover art.
     */
    private void updatePlayingInfo(final MPDStatus status) {
        Log.d(TAG, "updatePlayingInfo(int,MPDStatus)");

        final MPDStatus mpdStatus = status == null ? getStatus() : status;

        if (lastStatusRefresh <= 0L && mpdStatus != null) {
            /**
             * Only update the refresh date and elapsed time if it is the first start to
             * make sure we have initial data, but updateStatus and trackChanged will take care
             * of that afterwards.
             */
            lastStatusRefresh = new Date().getTime();
            lastKnownElapsed = mpdStatus.getElapsedTime() * DateUtils.SECOND_IN_MILLIS;
        }

        if (mpdStatus != null) {
            final int songPos = mpdStatus.getSongPos();
            mCurrentMusic = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
        }

        if (mCurrentMusic != null) {
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

            if (settings.getBoolean(CoverManager.PREFERENCE_CACHE, true)) {
                updateAlbumCoverWithCached();
            } /** TODO: Add no cache option */
            setupNotification();
            updateRemoteControlClient(mpdStatus);
        }
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
            coverArtPaths = cache.getCoverUrl(mCurrentMusic.getAlbumInfo());
        } catch (final Exception e) {
            Log.d(TAG, "Failed to get the cover URL from the cache.", e);
        }

        if (coverArtPaths != null && coverArtPaths.length > 0) {
            coverArtPath = coverArtPaths[0];
        }
        return coverArtPath;
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
                mAlbumCover = Tools
                        .decodeSampledBitmapFromPath(coverArtPath, getResources()
                                        .getDimensionPixelSize(
                                                android.R.dimen.notification_large_icon_width),
                                getResources()
                                        .getDimensionPixelSize(
                                                android.R.dimen.notification_large_icon_height),
                                false
                        );
            }
        }
    }

    /**
     * This method builds the base, otherwise known as the collapsed notification. The expanded
     * notification method builds upon this method.
     *
     * @param resultView The RemoteView to begin with, be it new or from the current notification.
     * @return The base, otherwise known as, collapsed notification resources for RemoteViews.
     */
    private RemoteViews buildBaseNotification(final RemoteViews resultView) {
        final String title = mCurrentMusic.getTitle();

        /** If in streaming, the notification should be persistent. */
        if (app.getApplicationState().streamingMode && !streamingServiceWoundDown) {
            resultView.setViewVisibility(R.id.notificationClose, View.GONE);
        } else {
            resultView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
            resultView.setOnClickPendingIntent(R.id.notificationClose, notificationClose);
        }

        if (MPDStatus.MPD_STATE_PLAYING.equals(getStatus().getState())) {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, notificationPause);
            resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_pause);
        } else {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, notificationPlay);
            resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_play);
        }

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mediaPlayerServiceIsBuffering) {
            resultView.setTextViewText(R.id.notificationTitle, getString(R.string.buffering));
            resultView.setTextViewText(R.id.notificationArtist, title);
        } else {
            resultView.setTextViewText(R.id.notificationTitle, title);
            resultView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getArtist());
        }

        resultView.setOnClickPendingIntent(R.id.notificationNext, notificationNext);

        if (mAlbumCover != null) {
            resultView.setImageViewUri(R.id.notificationIcon, Uri.parse(mAlbumCoverPath));
        }

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
        if (mediaPlayerServiceIsBuffering) {
            resultView.setTextViewText(R.id.notificationAlbum, mCurrentMusic.getArtist());
        } else {
            resultView.setTextViewText(R.id.notificationAlbum, mCurrentMusic.getAlbum());
        }

        resultView.setOnClickPendingIntent(R.id.notificationPrev, notificationPrevious);

        return resultView;
    }

    /**
     * Build a new notification or perform an update on an existing notification.
     */
    private void setupNotification() {
        Log.d(TAG, "update notification: " + mCurrentMusic.getArtist() + " - " + mCurrentMusic
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

    private void shutdownNotification() {
        if (app.getApplicationState().persistentNotification) {
            windDownResources();
        } else {
            stopSelf();
        }
    }

    /**
     * Update the remote controls.
     *
     * @param mpdStatus The current server status object.
     */
    private void updateRemoteControlClient(final MPDStatus mpdStatus) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int state = getRemoteState(mpdStatus);

                mRemoteControlClient.editMetadata(true)
                        .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                                mCurrentMusic.getAlbum())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                                mCurrentMusic.getAlbumArtist())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                                mCurrentMusic.getArtist())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                                (long) mCurrentMusic.getTrack())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER,
                                (long) mCurrentMusic.getDisc())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                                mCurrentMusic.getTime() * DateUtils.SECOND_IN_MILLIS)
                        .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                                mCurrentMusic.getTitle())
                        .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK,
                                mAlbumCover)
                        .apply();

                /** Notify of the elapsed time if on 4.3 or higher */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mRemoteControlClient.setPlaybackState(state, lastKnownElapsed, 1.0f);
                } else {
                    mRemoteControlClient.setPlaybackState(state);
                }
                Log.d(TAG, "Updated remote client with state " + state + " for music "
                        + mCurrentMusic);
            }
        }).start();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Removing connection lock");
        app.removeConnectionLock(this);
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        delayedPauseHandler.removeCallbacksAndMessages(null);

        app.getApplicationState().notificationMode = false;

        windDownResources();

        if (mAlbumCover != null && !mAlbumCover.isRecycled()) {
            mAlbumCover.recycle();
        }

        if (mAudioManager != null) {
            if (mRemoteControlClient != null) {
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
            }

            if (mMediaButtonReceiverComponent != null) {
                mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
            }

            mAudioManager.abandonAudioFocus(null);
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return serviceMessenger.getBinder();
    }

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        if (connected) {
            if (app.oMPDAsyncHelper.getConnectionSettings().persistentNotification) {
                stateChanged(getStatus(), null);
            }
        } else {
            final long idleDelay = 10000L; /** Give 10 Seconds for Network Problems */

            final Message msg = delayedDisconnectionHandler.obtainMessage();
            delayedDisconnectionHandler.sendMessageDelayed(msg, idleDelay);
        }
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
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /**
         * This is required because streams will emit a playlist (current queue) event as the
         * metadata will change while the same audio file is playing (no track change).
         */
        if (mCurrentMusic != null && mCurrentMusic.isStream()) {
            updatePlayingInfo(mpdStatus);
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
        final long idleDelay = 630000L; /** 10 Minutes 30 Seconds */
        final Message msg = delayedPauseHandler.obtainMessage();
        serviceHandlerActive = true;
        delayedPauseHandler.sendMessageDelayed(msg, idleDelay);
    }

    /** Kills any active service handlers. */
    private void stopServiceHandler() {
        /** If we have a message in the queue, remove it. */
        delayedPauseHandler.removeCallbacksAndMessages(null);
        serviceHandlerActive = false; /** No notification if stopped or paused. */
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final String oldState) {
        if (mpdStatus == null) {
            Log.w(TAG, "Null mpdStatus received in stateChanged");
        } else {
            lastStatusRefresh = new Date().getTime();
            // MPDs elapsed time is in seconds, convert to milliseconds
            lastKnownElapsed = mpdStatus.getElapsedTime() * DateUtils.SECOND_IN_MILLIS;
            switch (mpdStatus.getState()) {
                case MPDStatus.MPD_STATE_PLAYING:
                    stopServiceHandler();
                    tryToGetAudioFocus();
                    updatePlayingInfo(mpdStatus);
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                    if (app.getApplicationState().persistentNotification) {
                        windDownResources(); /** Hide immediately, requires user intervention */
                    } else {
                        stopSelf();
                    }
                    break;
                case MPDStatus.MPD_STATE_PAUSED:
                    if (!serviceHandlerActive) {
                        setupServiceHandler();
                        updatePlayingInfo(mpdStatus);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        lastStatusRefresh = new Date().getTime();
        lastKnownElapsed = 0L;

        updatePlayingInfo(mpdStatus);
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
    }
}
