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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * A class to handle everything necessary for the MPDroid notification.
 */
public class NotificationHandler {

    /**
     * The ID we use for the notification (the onscreen alert that appears
     * at the notification area at the top of the screen as an icon -- and
     * as text as well if the user expands the notification area).
     */
    static final int NOTIFICATION_ID = 1;

    private static final Notification NOTIFICATION;

    private static final String TAG = "NotificationHandler";

    private static MPDApplication sApp = MPDApplication.getInstance();

    private static final NotificationManager NOTIFICATION_MANAGER =
            (NotificationManager) sApp.getSystemService(sApp.NOTIFICATION_SERVICE);

    private Music mCurrentTrack = null;

    private Callback mNotificationListener = null;

    private boolean mIsMediaPlayerBuffering = false;

    NotificationHandler() {
        super();

        final MPDStatus mpdStatus = MPDroidService.getMPDStatus();

        /**
         * Workaround for Bug #558 This is necessary if setMediaPlayerBuffering() is the first
         * method to be called. Optimally, this would be passed into the constructor, but
         * this complication belongs here for now.
         */
        if (sApp.oMPDAsyncHelper.oMPD.isConnected()) {
            while (mCurrentTrack == null && mpdStatus.getPlaylistLength() > 0
                    || mpdStatus.getPlaylistVersion() == 0) {
                final int songPos = mpdStatus.getSongPos();
                mCurrentTrack = sApp.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);

                if (mCurrentTrack == null && mpdStatus.getPlaylistLength() > 0) {
                    Log.w(TAG, "Failed to get current track, likely due to bug #558, looping..");
                    synchronized (this) {
                        try {
                            wait(1000L);
                        } catch (final InterruptedException ignored) {
                        }
                    }
                }
            }
        }
    }

    static {
        final RemoteViews resultView = new RemoteViews(sApp.getPackageName(),
                R.layout.notification);

        buildBaseNotification(resultView);
        NOTIFICATION = buildCollapsedNotification().setContent(resultView).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            buildExpandedNotification();
        }
    }

    /**
     * Build a pending intent for use with the notification button controls.
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
     * This builds a new collapsed notification.
     *
     * @return Returns a notification builder object.
     */
    private static NotificationCompat.Builder buildCollapsedNotification() {
        final Intent musicPlayerActivity = new Intent(sApp, MainMenuActivity.class);
        final PendingIntent notificationClick = PendingIntent
                .getActivity(sApp, 0, musicPlayerActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(sApp);
        builder.setSmallIcon(R.drawable.icon_bw);
        builder.setContentIntent(notificationClick);
        builder.setStyle(new NotificationCompat.BigTextStyle());

        return builder;
    }

    /**
     * A method to update the album cover view.
     *
     * @param resultView     The notification view to edit.
     * @param albumCover     The new album cover.
     * @param albumCoverPath The new album cover path.
     */
    private static void setAlbumCover(final RemoteViews resultView, final Bitmap albumCover,
            final String albumCoverPath) {
        if (albumCover != null) {
            resultView.setImageViewUri(R.id.notificationIcon, Uri.parse(albumCoverPath));
        } else {
            resultView.setImageViewResource(R.id.notificationIcon,
                    AlbumCoverDownloadListener.getNoCoverResource());
        }
    }

    /**
     * A method to update the play state icon to a "paused" state.
     *
     * @param resultView The notification view to edit.
     */
    private static void updateStatePaused(final RemoteViews resultView) {
        final PendingIntent playAction = buildPendingIntent(MPDControl.ACTION_PLAY);

        resultView.setOnClickPendingIntent(R.id.notificationPlayPause, playAction);
        resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_play);
    }

    /**
     * A method to update the play state icon to a "play" state.
     *
     * @param resultView The notification view to edit.
     */
    private static void updateStatePlaying(final RemoteViews resultView) {
        final PendingIntent pauseAction = buildPendingIntent(MPDControl.ACTION_PAUSE);

        resultView.setOnClickPendingIntent(R.id.notificationPlayPause, pauseAction);
        resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_pause);
    }

    /**
     * Update the collapsed notification view for a "buffering" play state.
     *
     * @param resultView The notification view to edit.
     * @param trackTitle The current track title.
     */
    private static void updateBufferingContent(final RemoteViews resultView,
            final CharSequence trackTitle) {
        resultView.setViewVisibility(R.id.notificationClose, View.GONE);
        resultView.setTextViewText(R.id.notificationTitle, sApp.getString(R.string.buffering));
        resultView.setTextViewText(R.id.notificationArtist, trackTitle);
    }

    /**
     * Update the collapsed notification view for a "not buffering" play state.
     *
     * @param resultView The notification view to edit.
     * @param music      The current {@code Music} object.
     */
    private static void updateNotBufferingContent(final RemoteViews resultView, final Music music) {
        resultView.setTextViewText(R.id.notificationTitle, music.getTitle());
        resultView.setTextViewText(R.id.notificationArtist, music.getArtist());
    }

    /**
     * This method constructs the notification base, otherwise known as the collapsed notification.
     * The expanded notification method builds upon this method.
     *
     * @param resultView The RemoteView to begin with, be it new or from the current notification.
     */
    private static void buildBaseNotification(final RemoteViews resultView) {
        final PendingIntent closeAction = buildPendingIntent(MPDroidService.ACTION_STOP);
        final PendingIntent nextAction = buildPendingIntent(MPDControl.ACTION_NEXT);

        resultView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
        resultView.setOnClickPendingIntent(R.id.notificationClose, closeAction);

        updateStatePaused(resultView);

        resultView.setOnClickPendingIntent(R.id.notificationNext, nextAction);

        resultView.setImageViewResource(R.id.notificationIcon,
                AlbumCoverDownloadListener.getNoCoverResource());
    }

    /**
     * This method builds upon the base notification resources to create
     * the resources necessary for the expanded notification RemoteViews.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void buildExpandedNotification() {
        final PendingIntent previousAction = buildPendingIntent(MPDControl.ACTION_PREVIOUS);
        final RemoteViews resultView = new RemoteViews(sApp.getPackageName(),
                R.layout.notification_big);

        buildBaseNotification(resultView);

        resultView.setOnClickPendingIntent(R.id.notificationPrev, previousAction);

        NOTIFICATION.bigContentView = resultView;
    }

    /**
     * A callback to listen for notification updates.
     *
     * @param listener The current {@code Notification} listener.
     */
    final void addCallback(final Callback listener) {
        mNotificationListener = listener;
    }

    /**
     * A method for cleanup and winding down.
     */
    final void onDestroy() {
        mNotificationListener = null;
    }

    /**
     * A method to update the album cover view of the current notification.
     *
     * @param albumCover     The new album cover.
     * @param albumCoverPath The new album cover path.
     */
    final void setAlbumCover(final Bitmap albumCover, final String albumCoverPath) {
        setAlbumCover(NOTIFICATION.contentView, albumCover, albumCoverPath);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setAlbumCover(NOTIFICATION.bigContentView, albumCover, albumCoverPath);
        }

        NOTIFICATION_MANAGER.notify(NOTIFICATION_ID, NOTIFICATION);
        mNotificationListener.onNotificationUpdate(NOTIFICATION);
    }

    /**
     * A method to update the track information notification views for a buffering play state.
     *
     * @param isBuffering True if buffering, false otherwise.
     */
    final void setMediaPlayerBuffering(final boolean isBuffering) {
        mIsMediaPlayerBuffering = isBuffering;
    }

    /**
     * A method that sets the StreamingService {@code MediaPlayer}
     * as dormant, which allows user access to close the notification.
     */
    final void setMediaPlayerWoundDown() {
        NOTIFICATION.contentView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            NOTIFICATION.bigContentView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
        }

        NOTIFICATION_MANAGER.notify(NOTIFICATION_ID, NOTIFICATION);
        mNotificationListener.onNotificationUpdate(NOTIFICATION);
    }

    /**
     * Update the track information for the current playing track.
     *
     * @param currentTrack A current {@code Music} object.
     */
    final void setNewTrack(final Music currentTrack) {
        mCurrentTrack = currentTrack;

        if (mIsMediaPlayerBuffering) {
            updateBufferingContent(NOTIFICATION.contentView, currentTrack.getTitle());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                updateBufferingContent(NOTIFICATION.bigContentView, currentTrack.getTitle());
                NOTIFICATION.bigContentView.setTextViewText(R.id.notificationAlbum,
                        currentTrack.getArtist());
            }
        } else {
            updateNotBufferingContent(NOTIFICATION.contentView, currentTrack);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                updateNotBufferingContent(NOTIFICATION.bigContentView, currentTrack);
                NOTIFICATION.bigContentView.setTextViewText(R.id.notificationAlbum,
                        currentTrack.getAlbum());
            }
        }

        NOTIFICATION_MANAGER.notify(NOTIFICATION_ID, NOTIFICATION);
        mNotificationListener.onNotificationUpdate(NOTIFICATION);

    }

    /**
     * A method to update the play state button on the notification.
     *
     * @param isPlaying True if playing, false otherwise.
     */
    final void setPlayState(final boolean isPlaying) {
        if (isPlaying) {
            updateStatePlaying(NOTIFICATION.contentView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                updateStatePlaying(NOTIFICATION.bigContentView);
            }
        } else {
            updateStatePaused(NOTIFICATION.contentView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                updateStatePaused(NOTIFICATION.bigContentView);
            }
        }

        NOTIFICATION_MANAGER.notify(NOTIFICATION_ID, NOTIFICATION);
        mNotificationListener.onNotificationUpdate(NOTIFICATION);
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
