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

/**
 * Class representing MPD Server status.
 *
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDStatus.java 2941 2005-02-09 02:34:21Z galmeida $
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
        resetValues();

        /** These are in every status update. */
        consume = false;
        mixRampDB = 0;
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
    final public long getBitrate() {
        return bitrate;
    }

    /**
     * Retrieves bits resolution from playing song.
     *
     * @return bits resolution from playing song.
     */
    final public int getBitsPerSample() {
        return bitsPerSample;
    }

    /**
     * Retrieves number of channels from playing song.
     *
     * @return number of channels from playing song.
     */
    final public int getChannels() {
        return channels;
    }

    /**
     * Retrieves current cross-fade time.
     *
     * @return current cross-fade time in seconds.
     */
    final public int getCrossfade() {
        return crossfade;
    }

    /**
     * Retrieves current track elapsed time.
     *
     * @return current track elapsed time.
     */
    final public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * Retrieves current track time with a higher resolution.
     *
     * @return Current track time (high resolution).
     */
    final public float getElapsedTimeHighResolution() {
        return elapsedTimeHighResolution;
    }

    /**
     * Retrieves error message.
     *
     * @return error message.
     */
    final public String getError() {
        return error;
    }

    final public int getNextSongId() {
        return nextSongId;
    }

    final public int getNextSongPos() {
        return nextSong;
    }

    /**
     * Retrieves the length of the playlist.
     *
     * @return the length of the playlist.
     */
    final public int getPlaylistLength() {
        return playlistLength;
    }

    /**
     * Retrieves playlist version.
     *
     * @return playlist version.
     */
    final public int getPlaylistVersion() {
        return playlistVersion;
    }

    /**
     * Retrieves sample rate from playing song.
     *
     * @return sample rate from playing song.
     */
    final public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Retrieves current song playlist id.
     *
     * @return current song playlist id.
     */
    final public int getSongId() {
        return songId;
    }

    /**
     * Retrieves current song playlist number.
     *
     * @return current song playlist number.
     */
    final public int getSongPos() {
        return song;
    }

    /**
     * Retrieves player state. MPD_STATE_PLAYING, MPD_STATE_PAUSED,
     * MPD_STATE_STOPPED or MPD_STATE_UNKNOWN
     *
     * @return player state.
     */
    final public String getState() {
        return state;
    }

    /**
     * Retrieves current track total time.
     *
     * @return current track total time.
     */
    final public long getTotalTime() {
        return totalTime;
    }

    /**
     * Retrieves volume (0-100).
     *
     * @return volume.
     */
    final public int getVolume() {
        return volume;
    }

    final public boolean isConsume() {
        return consume;
    }

    /**
     * If random is enabled return true, return false if random is disabled.
     *
     * @return true if random is enabled, false if random is disabled
     */
    final public boolean isRandom() {
        return random;
    }

    /**
     * If repeat is enabled return true, return false if repeat is disabled.
     *
     * @return true if repeat is enabled, false if repeat is disabled.
     */
    final public boolean isRepeat() {
        return repeat;
    }

    final public boolean isSingle() {
        return single;
    }

    /**
     * Retrieves the process id of any database update task.
     *
     * @return the process id of any database update task.
     */
    final public boolean isUpdating() {
        return updating;
    }

    /**
     * These values are not necessarily reset by a response
     * and must be reset prior to response parsing.
     */
    private void resetValues() {
        bitrate = 0;
        bitsPerSample = 0;
        channels = 0;
        crossfade = 0;
        elapsedTime = 0;
        elapsedTimeHighResolution = 0;
        //noinspection AssignmentToNull
        error = null;
        nextSong = 0;
        nextSongId = 0;
        sampleRate = 0;
        song = 0;
        songId = 0;
        totalTime = 0;
        updating = false;
        volume = 0;
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     * @see java.lang.Object#toString()
     */
    final public String toString() {
        return "bitsPerSample: " + bitsPerSample +
                ", bitrate: " + bitrate +
                ", channels: " + channels +
                ", consume: " + consume +
                ", crossfade: " + crossfade +
                ", elapsedTime: " + elapsedTime +
                ", elapsedTimeHighResolution" + elapsedTimeHighResolution +
                ", error: " + error +
                ", nextSong: " + nextSong +
                ", nextSongId: " + nextSongId +
                ", mixRampDB: " + mixRampDB +
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
    final public void updateStatus(Iterable<String> response) {
        resetValues();

        for (String line : response) {
            String[] lines = line.split(": ");

            switch (lines[0]) {
                case "audio":
                    final String[] audio = lines[1].split(":");

                    try {
                        this.sampleRate = Integer.parseInt(audio[0]);
                        this.bitsPerSample = Integer.parseInt(audio[1]);
                        this.channels = Integer.parseInt(audio[2]);
                    } catch (NumberFormatException e) {
                        // Sometimes mpd sends "?" as a sampleRate or
                        // bitsPerSample, etc ... hotfix for a bugreport I had.
                    }
                    break;
                case "bitrate":
                    this.bitrate = Long.parseLong(lines[1]);
                    break;
                case "consume":
                    this.consume = "1".equals(lines[1]);
                    break;
                case "elapsed":
                    this.elapsedTimeHighResolution = Float.parseFloat(lines[1]);
                    break;
                case "error":
                    this.error = lines[1];
                    break;
                case "mixrampdb":
                    this.mixRampDB = Float.parseFloat(lines[1]);
                    break;
                case "nextsong":
                    this.nextSong = Integer.parseInt(lines[1]);
                    break;
                case "nextsongid":
                    this.nextSongId = Integer.parseInt(lines[1]);
                    break;
                case "playlist":
                    this.playlistVersion = Integer.parseInt(lines[1]);
                    break;
                case "playlistlength":
                    this.playlistLength = Integer.parseInt(lines[1]);
                    break;
                case "random":
                    this.random = "1".equals(lines[1]);
                    break;
                case "repeat":
                    this.repeat = "1".equals(lines[1]);
                    break;
                case "single":
                    this.single = "1".equals(lines[1]);
                    break;
                case "song":
                    this.song = Integer.parseInt(lines[1]);
                    break;
                case "songid":
                    this.songId = Integer.parseInt(lines[1]);
                    break;
                case "state":
                    switch (lines[1]) {
                        case MPD_STATE_PAUSED:
                            this.state = MPD_STATE_PAUSED;
                            break;
                        case MPD_STATE_PLAYING:
                            this.state = MPD_STATE_PLAYING;
                            break;
                        case MPD_STATE_STOPPED:
                            this.state = MPD_STATE_STOPPED;
                            break;
                        default:
                            this.state = MPD_STATE_UNKNOWN;
                            break;
                    }
                    break;
                case "time":
                    final String[] time = lines[1].split(":");

                    this.elapsedTime = Long.parseLong(time[0]);
                    this.totalTime = Long.parseLong(time[1]);
                    break;
                case "volume":
                    this.volume = Integer.parseInt(lines[1]);
                    break;
                case "xfade":
                    this.crossfade = Integer.parseInt(lines[1]);
                    break;
                case "updating_db":
                    this.updating = true;
                    break;
                default:
                    Log.d(TAG, "Status was sent an unknown response line:" + lines[1] +
                            "from: " + lines[0]);
            }
        }
    }
}
