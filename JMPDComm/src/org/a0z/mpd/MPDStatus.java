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

import android.util.Log;

import java.util.regex.Pattern;

/**
 * Class representing MPD Server status.
 *
 * @author Felipe Gustavo de Almeida
 */
public class MPDStatus {

    /**
     * MPD State: playing.
     */
    public static final String MPD_STATE_PLAYING = "play";

    /**
     * MPD State: stopped.
     */
    public static final String MPD_STATE_STOPPED = "stop";

    /**
     * MPD State: paused.
     */
    public static final String MPD_STATE_PAUSED = "pause";

    /**
     * MPD State: unknown.
     */
    public static final String MPD_STATE_UNKNOWN = "unknown";

    private static final String TAG = "org.a0z.mpd.MPDStatus";

    private int bitsPerSample;

    private long bitrate;

    private int channels;

    private boolean consume;

    private int crossfade;

    private long elapsedTime;

    private float elapsedTimeHighResolution;

    private String error = null;

    private float mixRampDB;

    private float mixRampDelay;

    private int nextSong;

    private int nextSongId;

    private int playlistVersion;

    private int playlistLength;

    private boolean random;

    private boolean repeat;

    private int sampleRate;

    private boolean single;

    private int song;

    private int songId;

    private String state = null;

    private long totalTime;

    private boolean updating;

    private int volume;

    MPDStatus() {
        super();
        resetValues();

        /** These are in every status update. */
        consume = false;
        mixRampDB = 0.0f;
        mixRampDelay = 0.0f;
        playlistLength = 0;
        playlistVersion = 0;
        random = false;
        repeat = false;
        single = false;
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
     * Retrieves current track elapsed time.
     *
     * @return current track elapsed time.
     */
    public final long getElapsedTime() {
        return elapsedTime;
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
    public final String getState() {
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
     * Retrieves the process id of any database update task.
     *
     * @return the process id of any database update task.
     */
    public final boolean isUpdating() {
        return updating;
    }

    /**
     * The time response has it's own delimiter.
     */
    private static final Pattern COMMA_DELIMITER = Pattern.compile(":");

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
    public final void updateStatus(final Iterable<String> response) {
        resetValues();

        for (final String line : response) {
            final String[] lines = StringsUtils.MPD_DELIMITER.split(line, 2);

            switch (lines[0]) {
                case "audio":
                    final String[] audio = COMMA_DELIMITER.split(lines[1]);

                    try {
                        sampleRate = Integer.parseInt(audio[0]);
                        bitsPerSample = Integer.parseInt(audio[1]);
                        channels = Integer.parseInt(audio[2]);
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
                    mixRampDB = Float.parseFloat(lines[1]);
                    break;
                case "mixrampdelay":
                    mixRampDelay = Float.parseFloat(lines[1]);
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
                        case MPD_STATE_PAUSED:
                            state = MPD_STATE_PAUSED;
                            break;
                        case MPD_STATE_PLAYING:
                            state = MPD_STATE_PLAYING;
                            break;
                        case MPD_STATE_STOPPED:
                            state = MPD_STATE_STOPPED;
                            break;
                        default:
                            state = MPD_STATE_UNKNOWN;
                            break;
                    }
                    break;
                case "time":
                    final String[] time = COMMA_DELIMITER.split(lines[1]);

                    elapsedTime = Long.parseLong(time[0]);
                    totalTime = Long.parseLong(time[1]);
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
                    Log.d(TAG, "Status was sent an unknown response line:" + lines[1] +
                            " from: " + lines[0]);
            }
        }
    }
}
