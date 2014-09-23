/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.a0z.mpd;

import java.util.Collection;
import java.util.Date;

/**
 * Class representing MPD Server status.
 *
 * @author Felipe Gustavo de Almeida
 */
public class MPDStatus {

    /**
     * MPD State: paused.
     */
    public static final int STATE_PAUSED = 2;

    /**
     * MPD State: playing.
     */
    public static final int STATE_PLAYING = 0;

    /**
     * MPD State: stopped.
     */
    public static final int STATE_STOPPED = 1;

    /**
     * MPD State: unknown.
     */
    public static final int STATE_UNKNOWN = 3;

    private static final String MPD_STATE_PAUSED = "pause";

    private static final String MPD_STATE_PLAYING = "play";

    private static final String MPD_STATE_STOPPED = "stop";

    private static final String MPD_STATE_UNKNOWN = "unknown";

    private static final String TAG = "MPDStatus";

    private long mBitRate;

    private int mBitsPerSample;

    private int mChannels;

    private boolean mConsume;

    private int mCrossFade;

    private long mElapsedTime;

    private float mElapsedTimeHighResolution;

    private String mError = null;

    private float mMixRampDB;

    private float mMixRampDelay;

    private boolean mMixRampDisabled;

    private int mNextSong;

    private int mNextSongId;

    private int mPlaylistLength;

    private int mPlaylistVersion;

    private boolean mRandom;

    private boolean mRepeat;

    private int mSampleRate;

    private boolean mSingle;

    private int mSong;

    private int mSongId;

    private int mState;

    private long mTotalTime;

    private long mUpdateTime;

    private boolean mUpdating;

    private int mVolume;

    MPDStatus() {
        super();
        resetValues();

        /** These are in every status update. */
        mConsume = false;
        mMixRampDB = 0.0f;
        mPlaylistLength = 0;
        mPlaylistVersion = 0;
        mRandom = false;
        mRepeat = false;
        mSingle = false;
        mState = STATE_UNKNOWN;
        mVolume = 0;
    }

    /**
     * Retrieves current track bitrate.
     *
     * @return current track bitrate.
     */
    public final long getBitrate() {
        return mBitRate;
    }

    /**
     * Retrieves bits resolution from playing song.
     *
     * @return bits resolution from playing song.
     */
    public final int getBitsPerSample() {
        return mBitsPerSample;
    }

    /**
     * Retrieves number of channels from playing song.
     *
     * @return number of channels from playing song.
     */
    public final int getChannels() {
        return mChannels;
    }

    /**
     * Retrieves current cross-fade time.
     *
     * @return current cross-fade time in seconds.
     */
    public final int getCrossfade() {
        return mCrossFade;
    }

    /**
     * Retrieves current track elapsed time. If the server
     * status is playing, this time is calculated.
     *
     * @return Elapsed time for the current track.
     */
    public final long getElapsedTime() {
        final long result;

        if (isState(STATE_PLAYING)) {
            /** We can't expect to always update right before this is called. */
            final long sinceUpdated = (new Date().getTime() - mUpdateTime) / 1000;

            result = sinceUpdated + mElapsedTime;
        } else {
            result = mElapsedTime;
        }

        return result;
    }

    /**
     * Retrieves current track time with a higher resolution.
     *
     * @return Current track time (high resolution).
     */
    public final float getElapsedTimeHighResolution() {
        return mElapsedTimeHighResolution;
    }

    /**
     * Retrieves error message.
     *
     * @return error message.
     */
    public final String getError() {
        return mError;
    }

    public final float getMixRampDelay() {
        return mMixRampDelay;
    }

    public final float getMixRampValue() {
        return mMixRampDB;
    }

    public final int getNextSongId() {
        return mNextSongId;
    }

    public final int getNextSongPos() {
        return mNextSong;
    }

    /**
     * Retrieves the length of the playlist.
     *
     * @return the length of the playlist.
     */
    public final int getPlaylistLength() {
        return mPlaylistLength;
    }

    /**
     * Retrieves playlist version.
     *
     * @return playlist version.
     */
    public final int getPlaylistVersion() {
        return mPlaylistVersion;
    }

    /**
     * Retrieves sample rate from playing song.
     *
     * @return sample rate from playing song.
     */
    public final int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Retrieves current song playlist id.
     *
     * @return current song playlist id.
     */
    public final int getSongId() {
        return mSongId;
    }

    /**
     * Retrieves current song playlist number.
     *
     * @return current song playlist number.
     */
    public final int getSongPos() {
        return mSong;
    }

    /**
     * Retrieves player state. MPD_STATE_PLAYING, MPD_STATE_PAUSED,
     * MPD_STATE_STOPPED or MPD_STATE_UNKNOWN
     *
     * @return player state.
     */
    public final int getState() {
        return mState;
    }

    /**
     * Retrieves current track total time.
     *
     * @return current track total time.
     */
    public final long getTotalTime() {
        return mTotalTime;
    }

    /**
     * Retrieves volume (0-100).
     *
     * @return volume.
     */
    public final int getVolume() {
        return mVolume;
    }

    public final boolean isConsume() {
        return mConsume;
    }

    /**
     * MixRampDB can return an invalid value
     *
     * @return True if mixRampDB is enabled, false otherwise.
     */
    public final boolean isMixRampEnabled() {
        return !mMixRampDisabled;
    }

    /**
     * If random is enabled return true, return false if random is disabled.
     *
     * @return true if random is enabled, false if random is disabled
     */
    public final boolean isRandom() {
        return mRandom;
    }

    /**
     * If repeat is enabled return true, return false if repeat is disabled.
     *
     * @return true if repeat is enabled, false if repeat is disabled.
     */
    public final boolean isRepeat() {
        return mRepeat;
    }

    public final boolean isSingle() {
        return mSingle;
    }

    /**
     * A convenience method to query the current state.
     *
     * @param queryState The state to query against.
     * @return True if the same as the current state.
     */
    public final boolean isState(final int queryState) {
        return mState == queryState;
    }

    /**
     * Retrieves the process id of any database update task.
     *
     * @return the process id of any database update task.
     */
    public final boolean isUpdating() {
        return mUpdating;
    }

    /**
     * Lets callers know if the current status object is valid.
     *
     * @return True if valid, false otherwise.
     */
    public final boolean isValid() {
        return mState != STATE_UNKNOWN;
    }

    /**
     * These values are not necessarily reset by a response
     * and must be reset prior to response parsing.
     */
    private void resetValues() {
        mBitRate = 0L;
        mBitsPerSample = 0;
        mChannels = 0;
        mCrossFade = 0;
        mElapsedTime = 0L;
        mElapsedTimeHighResolution = 0.0f;
        //noinspection AssignmentToNull
        mError = null;
        mMixRampDelay = 0.0f;
        mMixRampDisabled = false;
        mNextSong = -1;
        mNextSongId = 0;
        mSampleRate = 0;
        mSong = 0;
        mSongId = 0;
        mTotalTime = 0L;
        mUpdating = false;
        mVolume = 0;
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public final String toString() {
        return "bitsPerSample: " + mBitsPerSample +
                ", bitrate: " + mBitRate +
                ", channels: " + mChannels +
                ", consume: " + mConsume +
                ", crossfade: " + mCrossFade +
                ", elapsedTime: " + mElapsedTime +
                ", elapsedTimeHighResolution: " + mElapsedTimeHighResolution +
                ", error: " + mError +
                ", nextSong: " + mNextSong +
                ", nextSongId: " + mNextSongId +
                ", mixRampDB: " + mMixRampDB +
                ", mixRampDelay: " + mMixRampDelay +
                ", mixRampDisabled: " + mMixRampDisabled +
                ", playlist: " + mPlaylistVersion +
                ", playlistLength: " + mPlaylistLength +
                ", random: " + mRandom +
                ", repeat: " + mRepeat +
                ", sampleRate: " + mSampleRate +
                ", single: " + mSingle +
                ", song: " + mSong +
                ", songid: " + mSongId +
                ", state: " + mState +
                ", totalTime: " + mTotalTime +
                ", updating: " + mUpdating +
                ", volume: " + mVolume;
    }

    /**
     * Updates the state of the MPD Server...
     *
     * @param response The response from the server.
     */
    public final void updateStatus(final Collection<String> response) {
        resetValues();

        for (final String[] lines : Tools.splitResponse(response)) {

            switch (lines[0]) {
                case "audio":
                    final int delimiterIndex = lines[1].indexOf(':');
                    final String tmp = lines[1].substring(delimiterIndex + 1);
                    final int secondIndex = tmp.indexOf(':');

                    try {
                        mSampleRate = Integer.parseInt(lines[1].substring(0, delimiterIndex));
                        mBitsPerSample = Integer.parseInt(tmp.substring(0, secondIndex));
                        mChannels = Integer.parseInt(tmp.substring(secondIndex + 1));
                    } catch (final NumberFormatException ignored) {
                        // Sometimes mpd sends "?" as a sampleRate or
                        // bitsPerSample, etc ... hotfix for a bugreport I had.
                    }
                    break;
                case "bitrate":
                    mBitRate = Long.parseLong(lines[1]);
                    break;
                case "consume":
                    mConsume = "1".equals(lines[1]);
                    break;
                case "elapsed":
                    mElapsedTimeHighResolution = Float.parseFloat(lines[1]);
                    break;
                case "error":
                    mError = lines[1];
                    break;
                case "mixrampdb":
                    try {
                        mMixRampDB = Float.parseFloat(lines[1]);
                    } catch (final NumberFormatException e) {
                        if ("nan".equals(lines[1])) {
                            mMixRampDisabled = true;
                        } else {
                            Log.error(TAG, "Unexpected value from mixrampdb.", e);
                        }
                    }
                    break;
                case "mixrampdelay":
                    try {
                        mMixRampDelay = Float.parseFloat(lines[1]);
                    } catch (final NumberFormatException e) {
                        if ("nan".equals(lines[1])) {
                            mMixRampDisabled = true;
                        } else {
                            Log.error(TAG, "Unexpected value from mixrampdelay", e);
                        }
                    }
                    break;
                case "nextsong":
                    mNextSong = Integer.parseInt(lines[1]);
                    break;
                case "nextsongid":
                    mNextSongId = Integer.parseInt(lines[1]);
                    break;
                case "playlist":
                    mPlaylistVersion = Integer.parseInt(lines[1]);
                    break;
                case "playlistlength":
                    mPlaylistLength = Integer.parseInt(lines[1]);
                    break;
                case "random":
                    mRandom = "1".equals(lines[1]);
                    break;
                case "repeat":
                    mRepeat = "1".equals(lines[1]);
                    break;
                case "single":
                    mSingle = "1".equals(lines[1]);
                    break;
                case "song":
                    mSong = Integer.parseInt(lines[1]);
                    break;
                case "songid":
                    mSongId = Integer.parseInt(lines[1]);
                    break;
                case "state":
                    switch (lines[1]) {
                        case MPD_STATE_PLAYING:
                            mState = STATE_PLAYING;
                            break;
                        case MPD_STATE_PAUSED:
                            mState = STATE_PAUSED;
                            break;
                        case MPD_STATE_STOPPED:
                            mState = STATE_STOPPED;
                            break;
                        case MPD_STATE_UNKNOWN:
                        default:
                            mState = STATE_UNKNOWN;
                            break;
                    }
                    break;
                case "time":
                    final int timeIndex = lines[1].indexOf(':');

                    mElapsedTime = Long.parseLong(lines[1].substring(0, timeIndex));
                    mTotalTime = Long.parseLong(lines[1].substring(timeIndex + 1));
                    mUpdateTime = new Date().getTime();
                    break;
                case "volume":
                    mVolume = Integer.parseInt(lines[1]);
                    break;
                case "xfade":
                    mCrossFade = Integer.parseInt(lines[1]);
                    break;
                case "updating_db":
                    mUpdating = true;
                    break;
                default:
                    Log.debug(TAG, "Status was sent an unknown response line:" + lines[1] +
                            " from: " + lines[0]);
            }
        }
    }
}
