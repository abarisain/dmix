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

import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.TrackPositionListener;

import android.annotation.TargetApi;
import android.media.RemoteControlClient;
import android.os.Build;
import android.text.format.DateUtils;

import java.util.Date;

/**
 * A simple class to enable Android's RemoteControlClient
 * seek bar. (Requires Android 4.3 and higher).
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RemoteControlSeekBarHandler implements
        RemoteControlClient.OnPlaybackPositionUpdateListener,
        RemoteControlClient.OnGetPlaybackPositionListener, TrackPositionListener {

    private static final String TAG = "RemoteControlSeekBarHandler";

    private final RemoteControlClient mRemoteControlClient;

    /**
     * What was the elapsed time (in ms) when the last status refresh happened?
     * Use this for guessing the elapsed time for the lock screen.
     */
    private long mLastKnownElapsed = 0L;

    /**
     * Last time the status was refreshed, used for track position.
     */
    private long mLastStatusRefresh = 0L;

    private int mPlaybackState = -1;

    RemoteControlSeekBarHandler(final RemoteControlClient remoteControlClient,
            final int controlFlags) {
        super();

        mRemoteControlClient = remoteControlClient;

        mRemoteControlClient.setTransportControlFlags(controlFlags |
                RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE);
    }

    /**
     * Android's callback that queries us for the elapsed time. Here, we are guessing the
     * elapsed time using the last time we updated the elapsed time and its value at the time.
     *
     * @return The guessed song position
     */
    @Override
    public final long onGetPlaybackPosition() {
        /** If we don't know the position, return a negative value as per the API spec */
        long result = -1L;

        if (mLastStatusRefresh > 0L) {
            result = mLastKnownElapsed + new Date().getTime() - mLastStatusRefresh;
        }
        return result;
    }

    /**
     * Android's callback for when the user seeks using the remote control.
     *
     * @param newPositionMs The position in MS where we should seek
     */
    @Override
    public final void onPlaybackPositionUpdate(final long newPositionMs) {
        MPDControl.run(MPDControl.ACTION_SEEK, newPositionMs / DateUtils.SECOND_IN_MILLIS);
    }

    final void setPlaybackState(final int playbackState) {
        mPlaybackState = playbackState;
        mRemoteControlClient.setPlaybackState(mPlaybackState, mLastKnownElapsed, 1.0f);
    }

    final void start() {
        mRemoteControlClient.setOnGetPlaybackPositionListener(this);
        mRemoteControlClient.setPlaybackPositionUpdateListener(this);
        MPDroidService.MPD_ASYNC_HELPER.addTrackPositionListener(this);
    }

    final void stop() {
        MPDroidService.MPD_ASYNC_HELPER.removeTrackPositionListener(this);
        mRemoteControlClient.setOnGetPlaybackPositionListener(null);
        mRemoteControlClient.setPlaybackPositionUpdateListener(null);
    }

    /**
     * Used to keep the remote control client track bar updated.
     *
     * @param status New MPD status, containing current track position
     */
    @Override
    public final void trackPositionChanged(final MPDStatus status) {
        updateSeekTime(status.getElapsedTime());
    }

    /**
     * Only update the refresh date and elapsed time if it is the first start to
     * make sure we have initial data, but updateStatus and trackChanged will take care
     * of that afterwards.
     *
     * @param elapsedTime The current track audio elapsed time.
     */
    final void updateSeekTime(final long elapsedTime) {
        mLastStatusRefresh = new Date().getTime();
        mLastKnownElapsed = elapsedTime * DateUtils.SECOND_IN_MILLIS;
        mRemoteControlClient.setPlaybackState(mPlaybackState, mLastKnownElapsed, 1.0f);
    }
}