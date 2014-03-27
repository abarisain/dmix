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
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPD;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Service that handles media playback. This is the Service through which we perform all the media
 * handling in our application. Upon initialization, it waits for Intents (which come from our main
 * activity,
 * {@link MainMenuActivity}, which signal the service to perform specific operations: Play, Pause,
 * Rewind, Skip, etc.
 */
public class NotificationService extends Service implements MusicFocusable,
        StatusChangeListener {

    private final static String TAG = "NotificationService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid." + TAG + ".";

    public static final String ACTION_UPDATE_INFO = FULLY_QUALIFIED_NAME + "UPDATE_INFO";

    public static final String ACTION_SHOW_NOTIFICATION = FULLY_QUALIFIED_NAME
            + "SHOW_NOTIFICATION";

    public static final String ACTION_CLOSE_NOTIFICATION = FULLY_QUALIFIED_NAME
            + "CLOSE_NOTIFICATION";

    public static final String ACTION_TOGGLE_PLAYBACK = FULLY_QUALIFIED_NAME + "PLAY_PAUSE";

    public static final String ACTION_PLAY = FULLY_QUALIFIED_NAME + "PLAY";

    public static final String ACTION_PAUSE = FULLY_QUALIFIED_NAME + "PAUSE";

    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + "STOP";

    public static final String ACTION_NEXT = FULLY_QUALIFIED_NAME + "NEXT";

    public static final String ACTION_REWIND = FULLY_QUALIFIED_NAME + "REWIND";

    public static final String ACTION_PREVIOUS = FULLY_QUALIFIED_NAME + "PREVIOUS";

    public static final String ACTION_MUTE = FULLY_QUALIFIED_NAME + "MUTE";

    public static final String ACTION_SET_VOLUME = FULLY_QUALIFIED_NAME + "SET_VOLUME";

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    private final int NOTIFICATION_ID = 1;

    RemoteControlClient mRemoteControlClient;

    // The component name of MusicIntentReceiver, for use with media button and remote control APIs
    ComponentName mMediaButtonReceiverComponent;

    /**
     * If not available, this will be null. Always check for null before using
     */
    AudioFocusHelper mAudioFocusHelper = null;

    int mAudioFocus = AudioFocusHelper.NO_FOCUS_NO_DUCK;

    AudioManager mAudioManager;

    NotificationManager mNotificationManager;

    Notification mNotification = null;

    MPDApplication app;

    private Music mCurrentMusic = null;

    private boolean mediaPlayerServiceIsBuffering = false;

    /**
     * If something started the notification by another class and
     * not user input, store it here.
     */
    private boolean notificationAutomaticallyGenerated = false;

    private Bitmap mAlbumCover = null;

    private String mAlbumCoverPath;

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating service");

        app = (MPDApplication) getApplication();

        if (app == null) {
            /** Should never happen but it's possible. */
            stopSelf();
        }

        mAudioFocusHelper = new AudioFocusHelper(app, this);

        //TODO: Acquire a network wakelock here if the user wants us to !
        //Otherwise we'll just shut down on screen off and reconnect on screen on
        //Tons of work ahead
        app.addConnectionLock(this);
        app.oMPDAsyncHelper.addStatusChangeListener(this);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Use the media button APIs (if available) to register ourselves for media button events
        mMediaButtonReceiverComponent = new ComponentName(this, RemoteControlReceiver.class);
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        Log.d(TAG, "received command, action=" + action + " from intent: " + intent);

        if (action == null) {
            return START_NOT_STICKY;
        }

        /**
         * The only way this happens is if something other
         * than the MainMenu 'Streaming' is checked.
         */
        if (!app.getApplicationState().notificationMode) {
            notificationAutomaticallyGenerated = true;
            app.getApplicationState().notificationMode = true;
        }

        /** StreamingService switches first. */
        switch (action) {
            case StreamingService.ACTION_BUFFERING_BEGIN:
                mediaPlayerServiceIsBuffering = true;
                action = ACTION_SHOW_NOTIFICATION;
                break;
            case StreamingService.ACTION_BUFFERING_END:
                mediaPlayerServiceIsBuffering = false;
                action = ACTION_UPDATE_INFO;
                break;
            case StreamingService.ACTION_STOP:
                if (notificationAutomaticallyGenerated) {
                    notificationAutomaticallyGenerated = false;
                    action = ACTION_CLOSE_NOTIFICATION;
                }
                break;
        }

        switch (action) {
            case ACTION_CLOSE_NOTIFICATION:
                stopSelf();
                break;
            case ACTION_PAUSE:
                sendSimpleMpdCommand(ACTION_PAUSE);
                break;
            case ACTION_PLAY:
                sendSimpleMpdCommand(ACTION_PLAY);
                break;
            case ACTION_PREVIOUS:
                sendSimpleMpdCommand(ACTION_PREVIOUS);
                break;
            case ACTION_REWIND:
                sendSimpleMpdCommand(ACTION_REWIND);
                break;
            case ACTION_NEXT:
                sendSimpleMpdCommand(ACTION_NEXT);
                break;
            case ACTION_STOP:
                sendSimpleMpdCommand(ACTION_STOP);
                stopSelf();
                break;
            case ACTION_TOGGLE_PLAYBACK:
                processTogglePlaybackRequest();
                break;
            case ACTION_SHOW_NOTIFICATION:
                tryToGetAudioFocus();
            case ACTION_UPDATE_INFO:
                updatePlayingInfo(null);
                break;
        }

        return START_NOT_STICKY; // Means we started the service, but don't want it to restart in case it's killed.
    }

    private void sendSimpleMpdCommand(final String command) {
        new Thread(new Runnable() {
            @Override
            final public void run() {

                final MPD mpd = app.oMPDAsyncHelper.oMPD;
                if (mpd == null) {
                    return;
                }

                try {
                    switch (command) {
                        case ACTION_PAUSE:
                            mpd.pause();
                            break;
                        case ACTION_PLAY:
                            String state = mpd.getStatus().getState();
                            if (!MPDStatus.MPD_STATE_PLAYING.equals(state)) {
                                mpd.play();
                            }
                            break;
                        case ACTION_STOP:
                            mpd.stop();
                            break;
                        case ACTION_NEXT:
                            mpd.next();
                            break;
                        case ACTION_PREVIOUS:
                            mpd.previous();
                            break;
                        case ACTION_REWIND:
                            mpd.seek(0);
                            break;
                    }
                } catch (MPDServerException e) {
                    Log.w(TAG, "Failed to send a simple MPD command.", e);
                }
            }
        }
        ).start();
    }

    private void processTogglePlaybackRequest() {
        new AsyncTask<MPDApplication, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(MPDApplication... params) {
                String state = null;
                try {
                    state = params[0].oMPDAsyncHelper.oMPD.getStatus().getState();
                } catch (MPDServerException e) {
                    Log.w(MPDApplication.TAG, e.getMessage());
                }
                return MPDStatus.MPD_STATE_PLAYING.equals(state) || MPDStatus.MPD_STATE_PAUSED
                        .equals(state);
            }

            @Override
            protected void onPostExecute(Boolean shouldPause) {
                if (shouldPause) {
                    sendSimpleMpdCommand(ACTION_PAUSE);
                } else {
                    sendSimpleMpdCommand(ACTION_PLAY);
                }
            }
        }.execute(app);
    }

    void giveUpAudioFocus() {
        Log.d(TAG, "Giving up audio focus.");
        if (mAudioFocus == AudioFocusHelper.FOCUSED && mAudioFocusHelper != null
                && mAudioFocusHelper
                .abandonFocus()) {
            mAudioFocus = AudioFocusHelper.NO_FOCUS_NO_DUCK;
        }
    }

    private void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocusHelper.FOCUSED && mAudioFocusHelper != null
                && mAudioFocusHelper
                .requestFocus()) {
            Log.d(TAG, "Trying to gain audio focus.");
            mAudioFocus = AudioFocusHelper.FOCUSED;
        }
    }

    public void onGainedAudioFocus() {
        Log.d(TAG, "Gained audio focus.");
        mAudioFocus = AudioFocusHelper.FOCUSED;
    }

    public void onLostAudioFocus(boolean canDuck) {
        Log.d(TAG, "Lost audio focus.");
        mAudioFocus = canDuck ? AudioFocusHelper.NO_FOCUS_CAN_DUCK
                : AudioFocusHelper.NO_FOCUS_NO_DUCK;
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
        } catch (MPDServerException e) {
            Log.d(TAG, "Couldn't get the status to updatePlayingInfo()", e);
        }

        if (mpdStatus == null) {
            Log.d(TAG, "mpdStatus was null, could not updatePlayingInfo().");
        }

        return mpdStatus;
    }

    private void updatePlayingInfo(final MPDStatus _mpdStatus) {
        Log.d(TAG, "updatePlayingInfo(int,MPDStatus)");

        final MPDStatus mpdStatus = _mpdStatus == null ? getStatus() : _mpdStatus;

        final int state = MPDStatus.MPD_STATE_PLAYING.equals(mpdStatus.getState()) ?
                RemoteControlClient.PLAYSTATE_PLAYING
                : RemoteControlClient.PLAYSTATE_PAUSED;

        if (mRemoteControlClient == null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(mMediaButtonReceiverComponent);
            mRemoteControlClient = new RemoteControlClient(PendingIntent
                    .getBroadcast(this /*context*/, 0 /*requestCode, ignored*/,
                            intent /*intent*/, 0 /*flags*/));
            mRemoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            mAudioManager.registerRemoteControlClient(mRemoteControlClient);
        }

        /** Update the current playing song. */
        if (mCurrentMusic == null) {
            final int songPos = mpdStatus.getSongPos();
            if (songPos >= 0) {
                mCurrentMusic = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
            }
        }

        if (mCurrentMusic != null) {
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

            if (settings.getBoolean(CoverManager.PREFERENCE_CACHE, true)) {
                updateAlbumCoverWithCached();
            } /** TODO: Add no cache option */
            updateNotification(state);
            updateRemoteControlClient(state);
        }
    }

    private String retrieveCoverArtPath() {
        final ICoverRetriever cache = new CachedCover(app);
        String coverArtPath = null;
        String[] coverArtPaths = null;

        try {
            coverArtPaths = cache.getCoverUrl(mCurrentMusic.getAlbumInfo());
        } catch (Exception e) {
            Log.d(TAG, "Failed to get the cover URL from the cache.", e);
        }

        if (coverArtPaths != null && coverArtPaths.length > 0) {
            coverArtPath = coverArtPaths[0];
        }
        return coverArtPath;
    }

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

    private RemoteViews buildCollapsedNotification(PendingIntent piPlayPause, PendingIntent piNext,
            PendingIntent piCloseNotification, int playPauseResId) {
        final RemoteViews contentView;
        if (mNotification == null || mNotification.contentView == null) {
            contentView = new RemoteViews(getPackageName(), R.layout.notification);
        } else {
            contentView = mNotification.contentView;
        }

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mediaPlayerServiceIsBuffering) {
            contentView.setTextViewText(R.id.notificationTitle, getString(R.string.buffering));
            contentView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getTitle());
        } else {
            contentView.setTextViewText(R.id.notificationTitle, mCurrentMusic.getTitle());
            contentView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getArtist());
        }

        contentView.setOnClickPendingIntent(R.id.notificationPlayPause, piPlayPause);
        contentView.setOnClickPendingIntent(R.id.notificationNext, piNext);

        /** If in streaming, the notification should be persistent. */
        if (app.getApplicationState().streamingMode) {
            contentView.setViewVisibility(R.id.notificationClose, View.GONE);
        } else {
            contentView.setOnClickPendingIntent(R.id.notificationClose, piCloseNotification);
        }

        contentView.setImageViewResource(R.id.notificationPlayPause, playPauseResId);

        return contentView;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private RemoteViews buildExpandedNotification(PendingIntent piPrev, PendingIntent piPlayPause,
            PendingIntent piNext, PendingIntent piCloseNotification, int playPauseResId) {
        final RemoteViews contentView;
        if (mNotification == null || mNotification.bigContentView == null) {
            contentView = new RemoteViews(getPackageName(), R.layout.notification_big);
        } else {
            contentView = mNotification.bigContentView;
        }

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mediaPlayerServiceIsBuffering) {
            contentView.setTextViewText(R.id.notificationTitle, getString(R.string.buffering));
            contentView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getTitle());
            contentView.setTextViewText(R.id.notificationAlbum, mCurrentMusic.getArtist());
        } else {
            contentView.setTextViewText(R.id.notificationTitle, mCurrentMusic.getTitle());
            contentView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getArtist());
            contentView.setTextViewText(R.id.notificationAlbum, mCurrentMusic.getAlbum());
        }

        contentView.setOnClickPendingIntent(R.id.notificationPrev, piPrev);
        contentView.setOnClickPendingIntent(R.id.notificationPlayPause, piPlayPause);
        contentView.setOnClickPendingIntent(R.id.notificationNext, piNext);

        /** If streaming, the notification should be persistent. */
        if (app.getApplicationState().streamingMode) {
            contentView.setViewVisibility(R.id.notificationClose, View.GONE);
        } else {
            contentView.setOnClickPendingIntent(R.id.notificationClose, piCloseNotification);
        }

        contentView.setImageViewResource(R.id.notificationPlayPause, playPauseResId);

        return contentView;
    }

    /**
     * Update the notification.
     *
     * @param state The new current playing state
     */
    private void updateNotification(int state) {
        Log.d(TAG, "update notification: " + mCurrentMusic.getArtist() + " - " + mCurrentMusic
                .getTitle() + ", state: " + state);

        // Build a virtual task stack
        final Intent musicPlayerActivity = new Intent(getApplicationContext(),
                MainMenuActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainMenuActivity.class);
        stackBuilder.addNextIntent(musicPlayerActivity);

        /** Build notification media player button actions */
        /** playPause */
        final Intent playPause = new Intent(this, NotificationService.class);
        playPause.setAction(NotificationService.ACTION_TOGGLE_PLAYBACK);
        final PendingIntent piPlayPause = PendingIntent.getService(this, 0, playPause, 0);
        /** playPause icon state */
        final int playPauseResId = state == RemoteControlClient.PLAYSTATE_PAUSED
                ? R.drawable.ic_media_play : R.drawable.ic_media_pause;

        /** Previous */
        final Intent prev = new Intent(this, NotificationService.class);
        prev.setAction(ACTION_PREVIOUS);
        final PendingIntent piPrev = PendingIntent.getService(this, 0, prev, 0);

        /** Next */
        final Intent next = new Intent(this, NotificationService.class);
        next.setAction(NotificationService.ACTION_NEXT);
        final PendingIntent piNext = PendingIntent.getService(this, 0, next, 0);

        /** Close Notification */
        PendingIntent piCloseNotification = null;
        if (!app.getApplicationState().streamingMode) {
            final Intent closeNotification = new Intent(this, NotificationService.class);
            closeNotification.setAction(NotificationService.ACTION_CLOSE_NOTIFICATION);
            piCloseNotification = PendingIntent.getService(this, 0, closeNotification, 0);
        }

        /** Notification click action */
        PendingIntent piClick = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the views
        RemoteViews collapsedNotification = buildCollapsedNotification(piPlayPause, piNext,
                piCloseNotification, playPauseResId);
        RemoteViews expandedNotification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            expandedNotification = buildExpandedNotification(piPrev, piPlayPause, piNext,
                    piCloseNotification, playPauseResId);
        }

        // Set notification icon, if we have one
        if (mAlbumCover != null) {
            collapsedNotification
                    .setImageViewUri(R.id.notificationIcon, Uri.parse(mAlbumCoverPath));
            if (expandedNotification != null) {
                expandedNotification
                        .setImageViewUri(R.id.notificationIcon, Uri.parse(mAlbumCoverPath));
            }
        }

        // Finish the notification
        if (mNotification == null) {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.icon_bw);
            builder.setContentIntent(piClick);
            builder.setContent(collapsedNotification);

            builder.setStyle(new NotificationCompat.BigTextStyle());

            mNotification = builder.build();
        }

        setBigContentView(mNotification, expandedNotification);

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setBigContentView(Notification notif, RemoteViews view) {
        if (view != null) {
            notif.bigContentView = view;
        }
    }

    /**
     * Update the remote controls
     *
     * @param state The new current playing state
     */
    private void updateRemoteControlClient(int state) {
        mRemoteControlClient.editMetadata(true) //
                .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, mCurrentMusic.getArtist()) //
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, mCurrentMusic.getAlbum()) //
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, mCurrentMusic.getTitle()) //
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, mCurrentMusic.getTime()) //
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, mAlbumCover) //
                .apply();
        mRemoteControlClient.setPlaybackState(state);
        Log.d(TAG, "Updated remote client with state " + state + " for music " + mCurrentMusic);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Removing connection lock");
        app.removeConnectionLock(this);
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        stopForeground(true);

        if (mAlbumCover != null && !mAlbumCover.isRecycled()) {
            mAlbumCover.recycle();
        }

        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }

        giveUpAudioFocus();

        if (mAudioManager != null) {
            if (mRemoteControlClient != null) {
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
            }

            if (mMediaButtonReceiverComponent != null) {
                mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
            }
        }
        app.getApplicationState().notificationMode = false;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        //TODO : Probably do something here
    }

    @Override
    public void libraryStateChanged(boolean updating) {
        // We do not care about that event
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
        // We do not care about that event
    }

    @Override
    public void randomChanged(boolean random) {
        // We do not care about that event
    }

    @Override
    public void repeatChanged(boolean repeating) {
        // We do not care about that event
    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) {
        if (mpdStatus == null) {
            Log.w(TAG, "Null mpdStatus received in stateChanged");
        } else {
            if (MPDStatus.MPD_STATE_STOPPED.equals(mpdStatus.getState())) {
                stopSelf();
            } else if (MPDStatus.MPD_STATE_PLAYING.equals(mpdStatus.getState())) {
                tryToGetAudioFocus();
            }
            updatePlayingInfo(mpdStatus);
        }
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        if (mpdStatus == null) {
            Log.w(TAG, "Null mpdStatus received in trackChanged");
        } else if (mpdStatus.getPlaylistLength() != 0) {
            final int songPos = mpdStatus.getSongPos();
            if (songPos >= 0) {
                mCurrentMusic = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
            }
            updatePlayingInfo(mpdStatus);
        }
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        // We do not care about that event
    }
}
