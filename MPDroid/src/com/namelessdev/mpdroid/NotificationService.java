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
import android.content.Context;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * A service that handles the Notification, RemoteControlClient, MediaButtonReceiver and
 * incoming MPD command intents.
 */
final public class NotificationService extends Service implements MusicFocusable,
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

    /**
     * This is the idle delay for shutting down this service after inactivity (in milliseconds).
     * This idle is also longer than StreamingService to avoid being unnecessarily brought
     * up to shut right back down.
     */
    private static final int IDLE_DELAY = 600000;

    /** Pre-built PendingIntent actions */
    private static PendingIntent notificationClick = null;

    private static PendingIntent notificationClose = null;

    private static PendingIntent notificationNext = null;

    private static PendingIntent notificationPause = null;

    private static PendingIntent notificationPlay = null;

    private static PendingIntent notificationPrevious = null;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    private final int NOTIFICATION_ID = 1;

    /** Set up the message handler. */
    final private Handler delayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            stopSelf();
        }
    };

    private RemoteControlClient mRemoteControlClient = null;

    // The component name of MusicIntentReceiver, for use with media button and remote control APIs
    private ComponentName mMediaButtonReceiverComponent = null;

    /**
     * If not available, this will be null. Always check for null before using
     */
    private AudioFocusHelper mAudioFocusHelper = null;

    private int mAudioFocus = AudioFocusHelper.NO_FOCUS_NO_DUCK;

    private AudioManager mAudioManager = null;

    private NotificationManager mNotificationManager = null;

    private Notification mNotification = null;

    private MPDApplication app = null;

    private Music mCurrentMusic = null;

    private boolean mediaPlayerServiceIsBuffering = false;

    /**
     * If something started the notification by another class and
     * not user input, store it here.
     */
    private boolean notificationAutomaticallyGenerated = false;

    private Bitmap mAlbumCover = null;

    private String mAlbumCoverPath = null;

    /**
     * Build a static pending intent for use with the notification button controls.
     *
     * @param context The current context.
     * @param action  The ACTION intent string.
     * @return The pending intent.
     */
    private static PendingIntent buildStaticPendingIntent(Context context, String action) {
        final Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    /**
     * A method to build all the pending intents necessary for the notification.
     *
     * @param context The current context.
     */
    private static void buildStaticPendingIntents(Context context) {
        /** Build click action */
        final Intent musicPlayerActivity = new Intent(context, MainMenuActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainMenuActivity.class);
        stackBuilder.addNextIntent(musicPlayerActivity);
        notificationClick = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        /** Build notification media player button actions */
        notificationClose = buildStaticPendingIntent(context, ACTION_CLOSE_NOTIFICATION);
        notificationNext = buildStaticPendingIntent(context, ACTION_NEXT);
        notificationPause = buildStaticPendingIntent(context, ACTION_PAUSE);
        notificationPlay = buildStaticPendingIntent(context, ACTION_PLAY);
        notificationPrevious = buildStaticPendingIntent(context, ACTION_PREVIOUS);
    }

    /**
     * This registers some media buttons via the RemoteControlReceiver.class which will take
     * action by intent to this onStartCommand().
     */
    private void registerMediaButtons() {
        mMediaButtonReceiverComponent = new ComponentName(this, RemoteControlReceiver.class);
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

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

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating service");

        app = (MPDApplication) getApplication();

        if (app == null) {
            /** Should never happen but it's possible. */
            stopSelf();
        }

        mAudioFocusHelper = new AudioFocusHelper(app, this);

        //TODO: Acquire a network wake lock here if the user wants us to !
        //Otherwise we'll just shut down on screen off and reconnect on screen on
        //Tons of work ahead
        app.addConnectionLock(this);
        app.oMPDAsyncHelper.addStatusChangeListener(this);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        registerMediaButtons();
        tryToGetAudioFocus();

        /** Build the non-dynamic intent actions */
        buildStaticPendingIntents(this);
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
                } else { /** Turn off persistent notification. */
                    action = ACTION_UPDATE_INFO;
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

    /**
     * A simple method to safely send a command to MPD.
     *
     * @param command An ACTION intent.
     */
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
                            mpd.play();
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
                    Log.w(TAG, "Failed to get the current state for toggle.", e);
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

    private void giveUpAudioFocus() {
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

    final public void onGainedAudioFocus() {
        Log.d(TAG, "Gained audio focus.");
        mAudioFocus = AudioFocusHelper.FOCUSED;
    }

    final public void onLostAudioFocus(boolean canDuck) {
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

    /**
     * This method will update the current playing track, notification views,
     * the RemoteControlClient & the cover art.
     */
    private void updatePlayingInfo(final MPDStatus _mpdStatus) {
        Log.d(TAG, "updatePlayingInfo(int,MPDStatus)");

        final MPDStatus mpdStatus = _mpdStatus == null ? getStatus() : _mpdStatus;

        final int state = MPDStatus.MPD_STATE_PLAYING.equals(mpdStatus.getState()) ?
                RemoteControlClient.PLAYSTATE_PLAYING
                : RemoteControlClient.PLAYSTATE_PAUSED;

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

    /**
     * This method retrieves a path to an album cover bitmap from the cache.
     *
     * @return String A path to a cached album cover bitmap.
     */
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
     * @param state      The current RemoteControlClient state.
     * @return The base, otherwise known as, collapsed notification resources for RemoteViews.
     */
    private RemoteViews buildBaseNotification(final RemoteViews resultView, final int state) {
        final int playPauseResId = state == RemoteControlClient.PLAYSTATE_PAUSED
                ? R.drawable.ic_media_play : R.drawable.ic_media_pause;

        /** If in streaming, the notification should be persistent. */
        if (app.getApplicationState().streamingMode) {
            resultView.setViewVisibility(R.id.notificationClose, View.GONE);
        } else {
            resultView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
            resultView.setOnClickPendingIntent(R.id.notificationClose, notificationClose);
        }

        if (state == RemoteControlClient.FLAG_KEY_MEDIA_PAUSE) {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, notificationPlay);
        } else {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, notificationPause);
        }

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mediaPlayerServiceIsBuffering) {
            resultView.setTextViewText(R.id.notificationTitle, getString(R.string.buffering));
            resultView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getTitle());
        } else {
            resultView.setTextViewText(R.id.notificationTitle, mCurrentMusic.getTitle());
            resultView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getArtist());
        }

        resultView.setOnClickPendingIntent(R.id.notificationNext, notificationNext);
        resultView.setImageViewResource(R.id.notificationPlayPause, playPauseResId);

        return resultView;
    }

    /**
     * This generates the collapsed notification from base.
     *
     * @param state The current RemoteControlClient state.
     * @return The collapsed notification resources for RemoteViews.
     */
    private RemoteViews buildCollapsedNotification(final int state) {
        RemoteViews resultView;

        if (mNotification == null || mNotification.contentView == null) {
            resultView = buildBaseNotification(
                    new RemoteViews(getPackageName(), R.layout.notification), state);
        } else {
            resultView = buildBaseNotification(mNotification.contentView, state);
        }

        return resultView;
    }

    /**
     * buildExpandedNotification builds upon the collapsed notification resources to create
     * the resources necessary for the expanded notification RemoteViews.
     *
     * @param state The current RemoteControlClient state.
     * @return The expanded notification RemoteViews.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private RemoteViews buildExpandedNotification(final int state) {
        final RemoteViews resultView;

        if (mNotification == null || mNotification.bigContentView == null) {
            resultView = buildBaseNotification(
                    new RemoteViews(getPackageName(), R.layout.notification_big), state);
        } else {
            resultView = buildBaseNotification(mNotification.bigContentView, state);
        }

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
     * Update the notification.
     *
     * @param state The new current playing state
     */
    private void updateNotification(int state) {
        Log.d(TAG, "update notification: " + mCurrentMusic.getArtist() + " - " + mCurrentMusic
                .getTitle() + ", state: " + state);

        // Create the views
        RemoteViews collapsedNotification = buildCollapsedNotification(state);
        RemoteViews expandedNotification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            expandedNotification = buildExpandedNotification(state);
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
            builder.setContentIntent(notificationClick);
            builder.setContent(collapsedNotification);

            builder.setStyle(new NotificationCompat.BigTextStyle());

            mNotification = builder.build();
        }

        setBigContentView(mNotification, expandedNotification);

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setBigContentView(Notification notification, RemoteViews view) {
        if (view != null) {
            notification.bigContentView = view;
        }
    }

    /**
     * Update the remote controls
     *
     * @param state The current playing RemoteControlClient state.
     */
    private void updateRemoteControlClient(final int state) {
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

        delayedStopHandler.removeCallbacksAndMessages(null);

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
            switch (mpdStatus.getState()) {
                case MPDStatus.MPD_STATE_PLAYING:
                    /** If we have a message in the queue, remove it. */
                    delayedStopHandler.removeCallbacksAndMessages(null);
                    tryToGetAudioFocus();
                    break;
                case MPDStatus.MPD_STATE_PAUSED:
                case MPDStatus.MPD_STATE_STOPPED:
                    /** Call stopSelf() in IDLE_DELAY, if still idle. */
                    Message msg = delayedStopHandler.obtainMessage();
                    delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
                    break;
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
