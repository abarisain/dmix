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

    private static final String TAG = "org.a0z.mpd.MPDStatus";

    private long bitrate;

    private int bitsPerSample;

    private int channels;

    private boolean consume;

    private int crossfade;

    private long elapsedTime;

    private float elapsedTimeHighResolution;

    private String error = null;

    private float mixRampDB;

    private float mixRampDelay;

    private boolean mixRampDisabled;

    private int nextSong;

    private int nextSongId;

    private int playlistLength;

    private int playlistVersion;

    private boolean random;

    private boolean repeat;

    private int sampleRate;

    private boolean single;

    private int song;

    private int songId;

    private int state;

    private long totalTime;

    private long updateTime;

    private boolean updating;

    private int volume;

    MPDStatus() {
        super();
        resetValues();

        /** These are in every status update. */
        consume = false;
        mixRampDB = 0.0f;
        playlistLength = 0;
        playlistVersion = 0;
        random = false;
        repeat = false;
        single = false;
        state = STATE_UNKNOWN;
        volume = 0;
    }

    /**
     * Retrieves current track bitrate.
     *
     * @return current track bitrate.
     */
    public final long getBitrate() {
        return bitrate;
    }

    /**
     * Retrieves bits resolution from playing song.
     *
     * @return bits resolution from playing song.
     */
    public final int getBitsPerSample() {
        return bitsPerSample;
    }

    /**
     * Retrieves number of channels from playing song.
     *
     * @return number of channels from playing song.
     */
    public final int getChannels() {
        return channels;
    }

    /**
     * Retrieves current cross-fade time.
     *
     * @return current cross-fade time in seconds.
     */
    public final int getCrossfade() {
        return crossfade;
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
            final long sinceUpdated = (new Date().getTime() - updateTime) / 1000;

            result = sinceUpdated + elapsedTime;
        } else {
            result = elapsedTime;
        }

        return result;
    }

    /**
     * Retrieves current track time with a higher resolution.
     *
     * @return Current track time (high resolution).
     */
    public final float getElapsedTimeHighResolution() {
        return elapsedTimeHighResolution;
    }

    /**
     * Retrieves error message.
     *
     * @return error message.
     */
    public final String getError() {
        return error;
    }

    public final float getMixRampDelay() {
        return mixRampDelay;
    }

    public final float getMixRampValue() {
        return mixRampDB;
    }

    public final int getNextSongId() {
        return nextSongId;
    }

    public final int getNextSongPos() {
        return nextSong;
    }

    /**
     * Retrieves the length of the playlist.
     *
     * @return the length of the playlist.
     */
    public final int getPlaylistLength() {
        return playlistLength;
    }

    /**
     * Retrieves playlist version.
     *
     * @return playlist version.
     */
    public final int getPlaylistVersion() {
        return playlistVersion;
    }

    /**
     * Retrieves sample rate from playing song.
     *
     * @return sample rate from playing song.
     */
    public final int getSampleRate() {
        return sampleRate;
    }

    /**
     * Retrieves current song playlist id.
     *
     * @return current song playlist id.
     */
    public final int getSongId() {
        return songId;
    }

    /**
     * Retrieves current song playlist number.
     *
     * @return current song playlist number.
     */
    public final int getSongPos() {
        return song;
    }

    /**
     * Retrieves player state. MPD_STATE_PLAYING, MPD_STATE_PAUSED,
     * MPD_STATE_STOPPED or MPD_STATE_UNKNOWN
     *
     * @return player state.
     */
    public final int getState() {
        return state;
    }

    /**
     * Retrieves current track total time.
     *
     * @return current track total time.
     */
    public final long getTotalTime() {
        return totalTime;
    }

    /**
     * Retrieves volume (0-100).
     *
     * @return volume.
     */
    public final int getVolume() {
        return volume;
    }

    public final boolean isConsume() {
        return consume;
    }

    /**
     * MixRampDB can return an invalid value
     *
     * @return True if mixRampDB is enabled, false otherwise.
     */
    public final boolean isMixRampEnabled() {
        return !mixRampDisabled;
    }

    /**
     * If random is enabled return true, return false if random is disabled.
     *
     * @return true if random is enabled, false if random is disabled
     */
    public final boolean isRandom() {
        return random;
    }

    /**
     * If repeat is enabled return true, return false if repeat is disabled.
     *
     * @return true if repeat is enabled, false if repeat is disabled.
     */
    public final boolean isRepeat() {
        return repeat;
    }

    public final boolean isSingle() {
        return single;
    }

    /**
     * A convenience method to query the current state.
     *
     * @param queryState The state to query against.
     * @return True if the same as the current state.
     */
    public final boolean isState(final int queryState) {
        return state == queryState;
    }

    /**
     * Retrieves the process id of any database update task.
     *
     * @return the process id of any database update task.
     */
    public final boolean isUpdating() {
        return updating;
    }

    /**
     * Lets callers know if the current status object is valid.
     *
     * @return True if valid, false otherwise.
     */
    public final boolean isValid() {
        return state != STATE_UNKNOWN;
    }

    /**
     * These values are not necessarily reset by a response
     * and must be reset prior to response parsing.
     */
    private void resetValues() {
        bitrate = 0L;
        bitsPerSample = 0;
        channels = 0;
        crossfade = 0;
        elapsedTime = 0L;
        elapsedTimeHighResolution = 0.0f;
        //noinspection AssignmentToNull
        error = null;
        mixRampDelay = 0.0f;
        mixRampDisabled = false;
        nextSong = -1;
        nextSongId = 0;
        sampleRate = 0;
        song = 0;
        songId = 0;
        totalTime = 0L;
        updating = false;
        volume = 0;
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public final String toString() {
        return "bitsPerSample: " + bitsPerSample +
                ", bitrate: " + bitrate +
                ", channels: " + channels +
                ", consume: " + consume +
                ", crossfade: " + crossfade +
                ", elapsedTime: " + elapsedTime +
                ", elapsedTimeHighResolution: " + elapsedTimeHighResolution +
                ", error: " + error +
                ", nextSong: " + nextSong +
                ", nextSongId: " + nextSongId +
                ", mixRampDB: " + mixRampDB +
                ", mixRampDelay: " + mixRampDelay +
                ", mixRampDisabled: " + mixRampDisabled +
                ", playlist: " + playlistVersion +
                ", playlistLength: " + playlistLength +
                ", random: " + random +
                ", repeat: " + repeat +
                ", sampleRate: " + sampleRate +
                ", single: " + single +
                ", song: " + song +
                ", songid: " + songId +
                ", state: " + state +
                ", totalTime: " + totalTime +
                ", updating: " + updating +
                ", volume: " + volume;
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
                        sampleRate = Integer.parseInt(lines[1].substring(0, delimiterIndex));
                        bitsPerSample = Integer.parseInt(tmp.substring(0, secondIndex));
                        channels = Integer.parseInt(tmp.substring(secondIndex + 1));
                    } catch (final NumberFormatException ignored) {
                        // Sometimes mpd sends "?" as a sampleRate or
                        // bitsPerSample, etc ... hotfix for a bugreport I had.
                    }
                    break;
                case "bitrate":
                    bitrate = Long.parseLong(lines[1]);
                    break;
                case "consume":
                    consume = "1".equals(lines[1]);
                    break;
                case "elapsed":
                    elapsedTimeHighResolution = Float.parseFloat(lines[1]);
                    break;
                case "error":
                    error = lines[1];
                    break;
                case "mixrampdb":
                    try {
                        mixRampDB = Float.parseFloat(lines[1]);
                    } catch (final NumberFormatException e) {
                        if ("nan".equals(lines[1])) {
                            mixRampDisabled = true;
                        } else {
                            Log.error(TAG, "Unexpected value from mixrampdb.", e);
                        }
                    }
                    break;
                case "mixrampdelay":
                    try {
                        mixRampDelay = Float.parseFloat(lines[1]);
                    } catch (final NumberFormatException e) {
                        if ("nan".equals(lines[1])) {
                            mixRampDisabled = true;
                        } else {
                            Log.error(TAG, "Unexpected value from mixrampdelay", e);
                        }
                    }
                    break;
                case "nextsong":
                    nextSong = Integer.parseInt(lines[1]);
                    break;
                case "nextsongid":
                    nextSongId = Integer.parseInt(lines[1]);
                    break;
                case "playlist":
                    playlistVersion = Integer.parseInt(lines[1]);
                    break;
                case "playlistlength":
                    playlistLength = Integer.parseInt(lines[1]);
                    break;
                case "random":
                    random = "1".equals(lines[1]);
                    break;
                case "repeat":
                    repeat = "1".equals(lines[1]);
                    break;
                case "single":
                    single = "1".equals(lines[1]);
                    break;
                case "song":
                    song = Integer.parseInt(lines[1]);
                    break;
                case "songid":
                    songId = Integer.parseInt(lines[1]);
                    break;
                case "state":
                    switch (lines[1]) {
                        case MPD_STATE_PLAYING:
                            state = STATE_PLAYING;
                            break;
                        case MPD_STATE_PAUSED:
                            state = STATE_PAUSED;
                            break;
                        case MPD_STATE_STOPPED:
                            state = STATE_STOPPED;
                            break;
                        case MPD_STATE_UNKNOWN:
                        default:
                            state = STATE_UNKNOWN;
                            break;
                    }
                    break;
                case "time":
                    final int timeIndex = lines[1].indexOf(':');

                    elapsedTime = Long.parseLong(lines[1].substring(0, timeIndex));
                    totalTime = Long.parseLong(lines[1].substring(timeIndex + 1));
                    updateTime = new Date().getTime();
                    break;
                case "volume":
                    volume = Integer.parseInt(lines[1]);
                    break;
                case "xfade":
                    crossfade = Integer.parseInt(lines[1]);
                    break;
                case "updating_db":
                    updating = true;
                    break;
                default:
                    Log.debug(TAG, "Status was sent an unknown response line:" + lines[1] +
                            " from: " + lines[0]);
            }
        }
    }
}
