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

import com.namelessdev.mpdroid.RemoteControlReceiver;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.item.Music;

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

/**
 * A class to handle everything necessary to integrate
 * the music server with Android's RemoteControlClient.
 */
public class RemoteControlClientHandler implements AlbumCoverHandler.FullSizeCallback {

    private static final String TAG = "RemoteControlClientService";

    private static final boolean DEBUG = MPDroidService.DEBUG;

    private final AudioManager mAudioManager;

    /** The RemoteControlClient Seekbar handled by the {@code RemoteControlClientSeekBarHandler}. */
    private RemoteControlSeekBarHandler mSeekBar = null;

    /** A flag used to inform the RemoteControlClient that a buffering event is taking place. */
    private boolean mIsMediaPlayerBuffering = false;

    /**
     * The component name of MusicIntentReceiver, for
     * use with media button and remote control APIs.
     */
    private ComponentName mMediaButtonReceiverComponent = null;

    private RemoteControlClient mRemoteControlClient = null;

    /**
     * This is kept up to date and used when the state didn't change
     * but the remote control client needs the current state.
     */
    private int mPlaybackState = -1;

    RemoteControlClientHandler(final MPDroidService serviceContext) {
        super();

        mAudioManager =
                (AudioManager) serviceContext.getSystemService(serviceContext.AUDIO_SERVICE);
        mMediaButtonReceiverComponent =
                new ComponentName(serviceContext, RemoteControlReceiver.class);

        final Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(mMediaButtonReceiverComponent);
        mRemoteControlClient = new RemoteControlClient(PendingIntent
                .getBroadcast(serviceContext /*context*/, 0 /*requestCode, ignored*/,
                        intent /*intent*/, 0 /*flags*/));

        final int controlFlags = RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                RemoteControlClient.FLAG_KEY_MEDIA_STOP;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSeekBar = new RemoteControlSeekBarHandler(mRemoteControlClient, controlFlags);
        } else {
            mRemoteControlClient.setTransportControlFlags(controlFlags);
        }
        start();
    }

    /**
     * Get the RemoteControlClient status for the corresponding MPDStatus
     *
     * @param state Current server state
     */
    private void getRemoteState(final int state) {
        final int playbackState;

        switch (state) {
            case MPDStatus.STATE_PLAYING:
                playbackState = RemoteControlClient.PLAYSTATE_PLAYING;
                break;
            case MPDStatus.STATE_STOPPED:
                playbackState = RemoteControlClient.PLAYSTATE_STOPPED;
                break;
            case MPDStatus.STATE_PAUSED:
                playbackState = RemoteControlClient.PLAYSTATE_PAUSED;
                break;
            case MPDStatus.STATE_UNKNOWN:
            default:
                playbackState = RemoteControlClient.PLAYSTATE_ERROR;
                break;
        }

        mPlaybackState = playbackState;

        if (!mIsMediaPlayerBuffering) {
            setPlaybackState(playbackState);
        }

        if (DEBUG) {
            Log.d(TAG, "Updated remote client with state " + state + '.');
        }
    }

    /**
     * This is called when cover art needs to be updated due to server information change.
     *
     * @param albumCover The current album cover bitmap.
     */
    @Override
    public final void onCoverUpdate(final Bitmap albumCover) {
        mRemoteControlClient.editMetadata(false)
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, albumCover)
                .apply();
    }

    final void start() {
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
        mAudioManager.registerRemoteControlClient(mRemoteControlClient);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSeekBar.start();
        }
    }

    /** Cleans up this object prior to closing the parent. */
    final void stop() {
        if (mRemoteControlClient != null) {
            setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
        }

        if (mMediaButtonReceiverComponent != null) {
            mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSeekBar.stop();
        }
    }

    /**
     * This updates the current remote control client state and will override any other state.
     *
     * @param isBuffering true if mediaPlayer is buffering, false otherwise.
     */
    final void setMediaPlayerBuffering(final boolean isBuffering) {
        mIsMediaPlayerBuffering = isBuffering;
        if (isBuffering) {
            setPlaybackState(RemoteControlClient.PLAYSTATE_BUFFERING);
        } else {
            setPlaybackState(mPlaybackState);
        }
    }

    /**
     * Sets the current media server playback state in the
     * RemoteControlClient and RemoteControlClient seek bar.
     *
     * @param playbackState The current playback state.
     */
    private void setPlaybackState(final int playbackState) {
        mRemoteControlClient.setPlaybackState(playbackState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSeekBar.setPlaybackState(playbackState);
        }
    }

    /**
     * Used to keep the state updated.
     *
     * @param mpdStatus An MPDStatus object.
     */
    final void stateChanged(final MPDStatus mpdStatus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSeekBar.updateSeekTime(mpdStatus.getElapsedTime());
        }
        getRemoteState(mpdStatus.getState());
    }

    /**
     * Updates the current album art.
     *
     * @param albumCover The current track album art.
     */
    final void update(final Bitmap albumCover) {
        mRemoteControlClient.editMetadata(false)
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, albumCover)
                .apply();
    }

    /**
     * Update the current metadata information.
     *
     * @param currentTrack A current {@code Music} object.
     */
    final void update(final Music currentTrack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mRemoteControlClient.editMetadata(false)
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
                        .apply();
            }
        }).start();
    }
}
