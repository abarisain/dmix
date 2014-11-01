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

import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.RemoteControlReceiver;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.item.Music;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

/**
 * A class to handle everything necessary for the MPDroid notification.
 */
public class NotificationHandler implements AlbumCoverHandler.NotificationCallback {

    static final int LOCAL_UID = 300;

    public static final int START = LOCAL_UID + 1;

    public static final int STOP = LOCAL_UID + 2;

    public static final int IS_ACTIVE = LOCAL_UID + 3;

    public static final int PERSISTENT_OVERRIDDEN = LOCAL_UID + 4;

    private static final int NOTIFICATION_ID = 1;

    private static final String TAG = "NotificationHandler";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid.service." + TAG;

    public static final String ACTION_START = FULLY_QUALIFIED_NAME + ".ACTION_START";

    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + ".ACTION_STOP";

    private final Notification mNotification;

    private final NotificationManager mNotificationManager;

    private final MPDroidService mServiceContext;

    private Music mCurrentTrack = null;

    private boolean mIsActive;

    private boolean mIsForeground = false;

    private boolean mIsMediaPlayerBuffering = false;

    NotificationHandler(final MPDroidService serviceContext) {
        super();

        mServiceContext = serviceContext;

        mNotificationManager = (NotificationManager) mServiceContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        final RemoteViews resultView = new RemoteViews(mServiceContext.getPackageName(),
                R.layout.notification);

        buildBaseNotification(resultView);
        mNotification = buildCollapsedNotification(serviceContext).setContent(resultView).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            buildExpandedNotification();
        }

        mCurrentTrack = new Music();
        mIsActive = true;
    }

    /**
     * This builds a new collapsed notification.
     *
     * @return Returns a notification builder object.
     */
    private static NotificationCompat.Builder
    buildCollapsedNotification(final MPDroidService context) {
        final Intent musicPlayerActivity = new Intent(context, MainMenuActivity.class);
        final PendingIntent notificationClick = PendingIntent
                .getActivity(context, 0, musicPlayerActivity,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.icon_notification);
        builder.setContentIntent(notificationClick);
        builder.setStyle(new NotificationCompat.BigTextStyle());

        return builder;
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
            case LOCAL_UID:
                result = "LOCAL_UID";
                break;
            case START:
                result = "START";
                break;
            case STOP:
                result = "STOP";
                break;
            case IS_ACTIVE:
                result = "IS_ACTIVE";
                break;
            case PERSISTENT_OVERRIDDEN:
                result = "PERSISTENT_OVERRIDDEN";
                break;
            default:
                result = "{unknown}: " + what;
                break;
        }

        return "NotificationHandler." + result;
    }

    /**
     * A method to update the album cover view.
     *
     * @param resultView The notification view to edit.
     * @param albumCover The new album cover.
     */
    private static void setAlbumCover(final RemoteViews resultView, final Bitmap albumCover) {
        if (albumCover == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
                resultView.setImageViewResource(R.id.notificationIcon, R.drawable.no_cover_art);
            } else {
                resultView.setImageViewResource(R.id.notificationIcon,
                        R.drawable.no_cover_art_light);
            }
        } else {
            resultView.setImageViewBitmap(R.id.notificationIcon, albumCover);
        }
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
    private void buildBaseNotification(final RemoteViews resultView) {
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
    private void buildExpandedNotification() {
        final PendingIntent previousAction = buildPendingIntent(MPDControl.ACTION_PREVIOUS);
        final RemoteViews resultView = new RemoteViews(mServiceContext.getPackageName(),
                R.layout.notification_big);

        buildBaseNotification(resultView);

        resultView.setOnClickPendingIntent(R.id.notificationPrev, previousAction);

        mNotification.bigContentView = resultView;
    }

    /**
     * Build a pending intent for use with the notification button controls.
     *
     * @param action The ACTION intent string.
     * @return The pending intent.
     */
    private PendingIntent buildPendingIntent(final String action) {
        final Intent intent = new Intent(mServiceContext, RemoteControlReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(mServiceContext, 0, intent, 0);
    }

    final boolean isActive() {
        return mIsActive;
    }

    /**
     * This is called when cover art needs to be updated due to server information change.
     *
     * @param albumCover the current album cover bitmap.
     */
    @Override
    public final void onCoverUpdate(final Bitmap albumCover) {
        if (mIsActive) {
            setAlbumCover(mNotification.contentView, albumCover);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                setAlbumCover(mNotification.bigContentView, albumCover);
            }
            updateNotification();
        }
    }

    /**
     * A method to update the track information notification views for a buffering play state.
     *
     * @param isBuffering True if buffering, false otherwise.
     */
    final void setMediaPlayerBuffering(final boolean isBuffering) {
        mIsMediaPlayerBuffering = isBuffering;
        setNewTrack(mCurrentTrack);
    }

    /**
     * A method that sets the StreamHandler {@code MediaPlayer}
     * as dormant, which allows user access to close the notification.
     */
    final void setMediaPlayerWoundDown() {
        if (mIsActive) {
            if (mIsMediaPlayerBuffering) {
                setMediaPlayerBuffering(false);
            }

            mNotification.contentView.setViewVisibility(R.id.notificationClose, View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mNotification.bigContentView
                        .setViewVisibility(R.id.notificationClose, View.VISIBLE);
            }

            updateNotification();
        }
    }

    /**
     * Update the track information for the current playing track.
     *
     * @param currentTrack A current {@code Music} object.
     */
    final void setNewTrack(final Music currentTrack) {
        if (mIsActive) {
            mCurrentTrack = currentTrack;

            if (mIsMediaPlayerBuffering) {
                final String title = currentTrack.getTitle();

                updateBufferingContent(mNotification.contentView, title);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    updateBufferingContent(mNotification.bigContentView, title);
                    mNotification.bigContentView.setTextViewText(R.id.notificationAlbum,
                            currentTrack.getArtist());
                }
            } else {
                updateNotBufferingContent(mNotification.contentView, currentTrack);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    updateNotBufferingContent(mNotification.bigContentView, currentTrack);
                    mNotification.bigContentView.setTextViewText(R.id.notificationAlbum,
                            currentTrack.getAlbum());
                }
            }

            updateNotification();
        }
    }

    /**
     * A method to update the play state button on the notification.
     *
     * @param isPlaying True if playing, false otherwise.
     */
    final void setPlayState(final boolean isPlaying) {
        if (mIsActive) {
            if (isPlaying) {
                updateStatePlaying(mNotification.contentView);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    updateStatePlaying(mNotification.bigContentView);
                }
            } else {
                updateStatePaused(mNotification.contentView);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    updateStatePaused(mNotification.bigContentView);
                }
            }

            updateNotification();
        }
    }

    final void start() {
        mIsActive = true;
        mIsMediaPlayerBuffering = false;
    }

    final void stateChanged(final MPDStatus mpdStatus) {
        switch (mpdStatus.getState()) {
            case MPDStatus.STATE_PLAYING:
                setPlayState(true);
                break;
            case MPDStatus.STATE_PAUSED:
                setPlayState(false);
                break;
            case MPDStatus.STATE_STOPPED:
            default:
                break;
        }
    }

    /**
     * A method for cleanup and winding down.
     */
    final void stop() {
        mServiceContext.stopForeground(true);
        mNotificationManager.cancel(NOTIFICATION_ID);
        mIsActive = false;
        mIsForeground = false;
    }

    /**
     * Update the collapsed notification view for a "buffering" play state.
     *
     * @param resultView The notification view to edit.
     * @param trackTitle The current track title.
     */
    private void updateBufferingContent(final RemoteViews resultView,
            final CharSequence trackTitle) {
        resultView.setViewVisibility(R.id.notificationClose, View.GONE);
        resultView.setTextViewText(R.id.notificationTitle,
                mServiceContext.getString(R.string.buffering));
        resultView.setTextViewText(R.id.notificationArtist, trackTitle);
    }

    private void updateNotification() {
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);

        if (!mIsForeground) {
            mServiceContext.startForeground(NOTIFICATION_ID, mNotification);
            mIsForeground = true;
        }
    }

    /**
     * A method to update the play state icon to a "paused" state.
     *
     * @param resultView The notification view to edit.
     */
    private void updateStatePaused(final RemoteViews resultView) {
        final PendingIntent playAction = buildPendingIntent(MPDControl.ACTION_PLAY);

        resultView.setOnClickPendingIntent(R.id.notificationPlayPause, playAction);
        resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_play);
    }

    /**
     * A method to update the play state icon to a "play" state.
     *
     * @param resultView The notification view to edit.
     */
    private void updateStatePlaying(final RemoteViews resultView) {
        final PendingIntent pauseAction = buildPendingIntent(MPDControl.ACTION_PAUSE);

        resultView.setOnClickPendingIntent(R.id.notificationPlayPause, pauseAction);
        resultView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_media_pause);
    }
}
