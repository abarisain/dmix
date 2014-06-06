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
import com.namelessdev.mpdroid.RemoteControlReceiver;
import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.TrackPositionListener;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.Date;

public class RemoteControlClientHandler implements TrackPositionListener {

    private static MPDApplication sApp = MPDApplication.getInstance();

    private static final AudioManager AUDIO_MANAGER =
            (AudioManager) sApp.getSystemService(sApp.AUDIO_SERVICE);

    private static final String MEDIA_PLAYER_BUFFERING = "MediaPlayerBuffering";

    private static final String TAG = "RemoteControlClientService";

    /**
     * This is kept up to date and used when the state didn't change
     * but the remote control client needs the current state.
     */
    private String mCurrentMPDState = MPDStatus.MPD_STATE_UNKNOWN;

    /**
     * What was the elapsed time (in ms) when the last status refresh happened?
     * Use this for guessing the elapsed time for the lock screen.
     */
    private long mLastKnownElapsed = 0L;

    /**
     * Last time the status was refreshed, used for track position.
     */
    private long mLastStatusRefresh = 0L;

    /**
     * The component name of MusicIntentReceiver, for
     * use with media button and remote control APIs.
     */

    private ComponentName mMediaButtonReceiverComponent = null;

    private RemoteControlClient mRemoteControlClient = null;

    RemoteControlClientHandler() {
        super();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            sApp.oMPDAsyncHelper.addTrackPositionListener(this);
        }
        registerMediaButtons();
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

                        if (mLastStatusRefresh > 0L) {
                            result = mLastKnownElapsed + new Date().getTime() - mLastStatusRefresh;
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
                    }
                }
        );
    }

    /**
     * Get the RemoteControlClient status for the corresponding MPDStatus
     *
     * @param state Current server state
     */
    private void getRemoteState(final String state) {
        final int playbackState;

        if (state.equals(MEDIA_PLAYER_BUFFERING)) {
            playbackState = RemoteControlClient.PLAYSTATE_BUFFERING;
        } else {
            switch (state) {
                case MPDStatus.MPD_STATE_PLAYING:
                    playbackState = RemoteControlClient.PLAYSTATE_PLAYING;
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                    playbackState = RemoteControlClient.PLAYSTATE_STOPPED;
                    break;
                case MPDStatus.MPD_STATE_PAUSED:
                    playbackState = RemoteControlClient.PLAYSTATE_PAUSED;
                    break;
                case MPDStatus.MPD_STATE_UNKNOWN:
                default:
                    playbackState = RemoteControlClient.PLAYSTATE_ERROR;
                    break;
            }
        }

        /** Notify of the elapsed time if on 4.3 or higher */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mRemoteControlClient.setPlaybackState(playbackState, mLastKnownElapsed, 1.0f);
        } else {
            mRemoteControlClient.setPlaybackState(playbackState);
        }

        Log.d(TAG, "Updated remote client with state " + state + '.');
    }

    /**
     * Cleans up this object prior to closing the parent.
     */
    final void onDestroy() {
        if (AUDIO_MANAGER != null) {
            if (mRemoteControlClient != null) {
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                AUDIO_MANAGER.unregisterRemoteControlClient(mRemoteControlClient);
            }

            if (mMediaButtonReceiverComponent != null) {
                AUDIO_MANAGER.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            sApp.oMPDAsyncHelper.removeTrackPositionListener(this);
        }
    }

    /**
     * This registers some media buttons via the RemoteControlReceiver.class which will take
     * action by intent to this onStartCommand().
     */
    private void registerMediaButtons() {
        mMediaButtonReceiverComponent = new ComponentName(sApp, RemoteControlReceiver.class);
        AUDIO_MANAGER.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

        final Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(mMediaButtonReceiverComponent);
        mRemoteControlClient = new RemoteControlClient(PendingIntent
                .getBroadcast(sApp /*context*/, 0 /*requestCode, ignored*/,
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

        AUDIO_MANAGER.registerRemoteControlClient(mRemoteControlClient);
    }

    /**
     * This updates the current remote control client state and will override any other state.
     *
     * @param isBuffering true if mediaPlayer is buffering, false otherwise.
     */
    final void setMediaPlayerBuffering(final boolean isBuffering) {
        if (isBuffering) {
            getRemoteState(MEDIA_PLAYER_BUFFERING);
        } else {
            getRemoteState(mCurrentMPDState);
        }
    }

    /**
     * Used to keep the state updated.
     *
     * @param mpdStatus An MPDStatus object.
     */
    final void stateChanged(final MPDStatus mpdStatus) {
        mCurrentMPDState = mpdStatus.getState();

        updateSeekTime(mpdStatus.getElapsedTime());
        getRemoteState(mCurrentMPDState);
    }

    /**
     * Used to keep the remote control client track bar updated.
     *
     * @param status New MPD status, containing current track position
     */
    @Override
    public final void trackPositionChanged(final MPDStatus status) {
        if (status != null) {
            updateSeekTime(status.getElapsedTime());
            getRemoteState(mCurrentMPDState);
        }
    }

    /**
     * Updates the current metadata information.
     */
    final void update(final Music currentTrack, final Bitmap albumCover) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mRemoteControlClient.editMetadata(true)
                        .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                                currentTrack.getAlbum())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                                currentTrack.getAlbumArtist())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                                currentTrack.getArtist())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                                (long) currentTrack.getTrack())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER,
                                (long) currentTrack.getDisc())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                                currentTrack.getTime() * DateUtils.SECOND_IN_MILLIS)
                        .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                                currentTrack.getTitle())
                        .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK,
                                albumCover)
                        .apply();
            }
        }).start();
    }

    /**
     * Only update the refresh date and elapsed time if it is the first start to
     * make sure we have initial data, but updateStatus and trackChanged will take care
     * of that afterwards.
     *
     * @param elapsedTime The current track audio elapsed time.
     */
    final void updateSeekTime(final long elapsedTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mLastStatusRefresh = new Date().getTime();
            mLastKnownElapsed = elapsedTime * DateUtils.SECOND_IN_MILLIS;
        }
    }
}
