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

package com.namelessdev.mpdroid.service;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.RemoteControlReceiver;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class NotificationHandler {

    /**
     * The ID we use for the notification (the onscreen alert that appears
     * at the notification area at the top of the screen as an icon -- and
     * as text as well if the user expands the notification area).
     */
    static final int NOTIFICATION_ID = 1;

    private static final String TAG = "NotificationHandler";

    /** Pre-built PendingIntent actions */
    private static final PendingIntent NOTIFICATION_CLOSE =
            buildPendingIntent(MPDroidService.ACTION_STOP);

    private static final PendingIntent NOTIFICATION_NEXT =
            buildPendingIntent(MPDControl.ACTION_NEXT);

    private static final PendingIntent NOTIFICATION_PAUSE =
            buildPendingIntent(MPDControl.ACTION_PAUSE);

    private static final PendingIntent NOTIFICATION_PLAY =
            buildPendingIntent(MPDControl.ACTION_PLAY);

    private static final PendingIntent NOTIFICATION_PREVIOUS =
            buildPendingIntent(MPDControl.ACTION_PREVIOUS);

    private static MPDApplication sApp = MPDApplication.getInstance();

    private static final NotificationManager NOTIFICATION_MANAGER =
            (NotificationManager) sApp.getSystemService(sApp.NOTIFICATION_SERVICE);

    private Callback mNotificationListener = null;

    private boolean mIsMediaPlayerBuffering = false;

    private boolean mIsMediaPlayerStreaming = false;

    private Notification mNotification = null;

    private Thread mThread = null;

    NotificationHandler() {
        super();
    }

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
    private static MPDStatus getStatus() {
        MPDStatus mpdStatus = null;
        try {
            mpdStatus = sApp.oMPDAsyncHelper.oMPD.getStatus();
        } catch (final MPDServerException e) {
            Log.d(TAG, "Couldn't retrieve a status object.", e);
        }

        return mpdStatus;
    }

    final void addCallback(final Callback listener) {
        mNotificationListener = listener;
    }

    /**
     * This method builds the base, otherwise known as the collapsed notification. The expanded
     * notification method builds upon this method.
     *
     * @param resultView The RemoteView to begin with, be it new or from the current notification.
     * @return The base, otherwise known as, collapsed notification resources for RemoteViews.
     */
    private RemoteViews buildBaseNotification(final RemoteViews resultView,
            final Music currentTrack, final Bitmap albumCover, final String albumCoverPath) {
        final String title = currentTrack.getTitle();
        final boolean isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(getStatus().getState());

        /** If in streaming, the notification should be persistent. */
        if (mIsMediaPlayerStreaming) {
            resultView.setViewVisibility(R.id.notificationClose, View.GONE);
        } else {
            resultView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
            resultView.setOnClickPendingIntent(R.id.notificationClose, NOTIFICATION_CLOSE);
        }

        if (isPlaying) {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, NOTIFICATION_PAUSE);
            resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_pause);
        } else {
            resultView.setOnClickPendingIntent(R.id.notificationPlayPause, NOTIFICATION_PLAY);
            resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_play);
        }

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mIsMediaPlayerBuffering) {
            resultView.setTextViewText(R.id.notificationTitle, sApp.getString(R.string.buffering));
            resultView.setTextViewText(R.id.notificationArtist, title);
        } else {
            resultView.setTextViewText(R.id.notificationTitle, title);
            resultView.setTextViewText(R.id.notificationArtist, currentTrack.getArtist());
        }

        resultView.setOnClickPendingIntent(R.id.notificationNext, NOTIFICATION_NEXT);

        if (albumCover != null) {
            resultView.setImageViewUri(R.id.notificationIcon, Uri.parse(albumCoverPath));
        } else {
            resultView.setImageViewResource(R.id.notificationIcon,
                    AlbumCoverDownloadListener.getNoCoverResource());
        }

        return resultView;
    }

    /**
     * This generates the collapsed notification from base.
     *
     * @return The collapsed notification resources for RemoteViews.
     */
    private NotificationCompat.Builder buildNewCollapsedNotification(final Music currentTrack,
            final Bitmap albumCover, final String albumCoverPath) {
        final RemoteViews resultView = buildBaseNotification(
                new RemoteViews(sApp.getPackageName(), R.layout.notification), currentTrack,
                albumCover, albumCoverPath);
        return buildStaticCollapsedNotification().setContent(resultView);
    }

    /**
     * buildExpandedNotification builds upon the collapsed notification resources to create
     * the resources necessary for the expanded notification RemoteViews.
     *
     * @return The expanded notification RemoteViews.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private RemoteViews buildExpandedNotification(final Music currentTrack, final Bitmap albumCover,
            final String albumCoverPath) {
        final RemoteViews resultView;

        if (mNotification == null || mNotification.bigContentView == null) {
            resultView = new RemoteViews(sApp.getPackageName(), R.layout.notification_big);
        } else {
            resultView = mNotification.bigContentView;
        }

        buildBaseNotification(resultView, currentTrack, albumCover, albumCoverPath);

        /** When streaming, move things down (hopefully, very) temporarily. */
        if (mIsMediaPlayerBuffering) {
            resultView.setTextViewText(R.id.notificationAlbum, currentTrack.getArtist());
        } else {
            resultView.setTextViewText(R.id.notificationAlbum, currentTrack.getAlbum());
        }

        resultView.setOnClickPendingIntent(R.id.notificationPrev, NOTIFICATION_PREVIOUS);

        return resultView;
    }

    final void cancelNotification() {
        if (NOTIFICATION_MANAGER != null) {
            NOTIFICATION_MANAGER.cancel(NOTIFICATION_ID);
        }
    }

    final void onDestroy() {
        mNotificationListener = null;
    }

    final void setMediaPlayerWoundDown() {
        mIsMediaPlayerStreaming = false;
    }

    final void setMediaPlayerBuffering(final boolean isBuffering) {
        mIsMediaPlayerBuffering = isBuffering;

        if (isBuffering) {
            mIsMediaPlayerStreaming = true;
        }
    }

    /**
     * Build a new notification or perform an update on an existing notification.
     */
    final void update(final Music currentTrack, final Bitmap albumCover,
            final String albumCoverPath) {
        if (currentTrack != null) {
            if (mThread != null && mThread.isAlive()) {
                mThread.interrupt();
            }

            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "update notification: " + currentTrack.getArtist() + " - "
                            + currentTrack
                            .getTitle());

                    NotificationCompat.Builder builder = null;
                    RemoteViews expandedNotification = null;

                    /** These have a very specific order. */
                    if (mNotification == null || mNotification.contentView == null) {
                        builder = buildNewCollapsedNotification(currentTrack, albumCover,
                                albumCoverPath);
                    } else {
                        buildBaseNotification(mNotification.contentView, currentTrack, albumCover,
                                albumCoverPath);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        expandedNotification = buildExpandedNotification(currentTrack, albumCover,
                                albumCoverPath);
                    }

                    if (builder != null) {
                        mNotification = builder.build();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mNotification.bigContentView = expandedNotification;
                    }

                    NOTIFICATION_MANAGER.notify(NOTIFICATION_ID, mNotification);
                    mNotificationListener.onNotificationUpdate(mNotification);
                }
            });

            mThread.start();
        }
    }

    interface Callback {

        /**
         * This is when an updated notification is available.
         *
         * @param notification The updated notification object.
         */
        void onNotificationUpdate(Notification notification);
    }
}
