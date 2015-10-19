/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.anpmech.mpd.subsystem;

import com.anpmech.mpd.concurrent.ResponseFuture;
import com.anpmech.mpd.concurrent.ResultFuture;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;

/**
 * This class manages the
 * <A HREF="http://www.musicpd.org/doc/protocol/playback_commands.html">playback</A> and
 * <A HREF="http://www.musicpd.org/doc/protocol/playback_option_commands.html">playback
 * options</A> of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.
 */
public class Playback {

    /**
     * Command text required to generate a command to play the next track in the playlist queue.
     */
    public static final String CMD_ACTION_NEXT = "next";

    /**
     * Command text required to generate a command to manipulate the pause playback state.
     */
    public static final String CMD_ACTION_PAUSE = "pause";

    /**
     * Command text required to generate a command to manipulate the playlist queue play position
     * or play back state.
     */
    public static final String CMD_ACTION_PLAY = "play";

    /**
     * Command text required to generate a command to manipulate the playlist queue play id state.
     */
    public static final String CMD_ACTION_PLAY_ID = "playid";

    /**
     * Command text required to generate a command to play the previous track in the playlist
     * queue.
     */
    public static final String CMD_ACTION_PREVIOUS = "previous";

    /**
     * This is the argument used with {@link #setMixRampDelay(int)} to disable MixRamp.
     */
    public static final int MIX_RAMP_DELAY_DISABLE = Integer.MIN_VALUE;

    /**
     * This is a response value returned by the {@link #getReplayGainStatus()} response.
     */
    public static final String REPLAY_GAIN_MODE_ALBUM = "album";

    /**
     * This is a response value returned by the {@link #getReplayGainStatus()} response.
     */
    public static final String REPLAY_GAIN_MODE_AUTO = "auto";

    /**
     * This is a response value returned by the {@link #getReplayGainStatus()} response.
     */
    public static final String REPLAY_GAIN_MODE_OFF = "off";

    /**
     * This is a response value returned by the {@link #getReplayGainStatus()} response.
     */
    public static final String REPLAY_GAIN_MODE_TRACK = "track";

    /**
     * Command text required to generate a command to set the consume state.
     *
     * <p>The argument for this command should be STATE. STATE should be {@link #STATE_OFF} or
     * {@link #STATE_ON}. When consume is activated, each song played is removed from playlist.</p>
     */
    private static final String CMD_ACTION_CONSUME = "consume";

    /**
     * Command text required to generate a command to manipulate the {@code crossfade} playback
     * option.
     */
    private static final String CMD_ACTION_CROSS_FADE = "crossfade";

    /**
     * Command text required to generate a command to manipulate the MixRamp decibel setting.
     */
    private static final String CMD_ACTION_MIX_RAMP_DECIBELS = "mixrampdb";

    /**
     * Command text required to generate a command to set the MixRampDelay.
     */
    private static final String CMD_ACTION_MIX_RAMP_DELAY = "mixrampdelay";

    /**
     * Command text required to generate a command to manipulate the {@code random} playback
     * option.
     *
     * <p>The argument for this command should be STATE. STATE should be {@link #STATE_OFF} or
     * {@link #STATE_ON}. This will cause any further playback from the playlist queue to be out
     * of order.</p>
     */
    private static final String CMD_ACTION_RANDOM = "random";

    /**
     * Command text required to generate a command to manipulate the {@code repeat} playback
     * option.
     *
     * <p>The argument for this command should be STATE. STATE should be {@link #STATE_OFF} or
     * {@link #STATE_ON}. This will cause the playlist queue to repeat tracks once all playlist
     * items have been exhausted.</p>
     */
    private static final String CMD_ACTION_REPEAT = "repeat";

    /**
     * Command text required to generate a command to retrieve the replay gain status.
     *
     * <BR><BR><B>Protocol command syntax:</B><BR>
     * {@code replay_gain_status}<BR>
     *
     * <BR><B>Sample protocol output:</B><BR> {@code replay_gain_status}<BR>
     * {@code replay_gain_mode: off}<BR> {@code OK}<BR>
     */
    private static final String CMD_ACTION_REPLAY_GAIN_STATUS = "replay_gain_status";

    /**
     * Command text required to generate a command to seek a track position on the playlist queue.
     */
    private static final String CMD_ACTION_SEEK = "seek";

    /**
     * Command text required to generate a command to seek the current track.
     */
    private static final String CMD_ACTION_SEEK_CURRENT_TRACK = "seekcur";

    /**
     * Command text required to generate a command to seek a track id on the playlist queue.
     */
    private static final String CMD_ACTION_SEEK_ID = "seekid";

    /**
     * Command text required to generate a command to manipulate the mixer volume.
     */
    private static final String CMD_ACTION_SET_VOLUME = "setvol";

    /**
     * Command text required to generate a command to manipulate the {@code seek} playback
     * option.
     */
    private static final String CMD_ACTION_SINGLE = "single";

    /**
     * Command text required to generate a command to change the playlist state to {@code stopped}.
     */
    private static final String CMD_ACTION_STOP = "stop";

    /**
     * This is a response key to the {@link #CMD_ACTION_REPLAY_GAIN_STATUS} command.
     */
    private static final String CMD_RESPONSE_REPLAY_GAIN_STATUS = "replay_gain_mode";

    /**
     * This is the argument actually given to {@link #CMD_ACTION_MIX_RAMP_DELAY} to disable
     * MixRamp.
     */
    private static final String MIX_RAMP_DELAY_DISABLE_ARGUMENT = "nan";

    /**
     * This is a OFF state argument for commands requiring it.
     */
    private static final CharSequence STATE_OFF = "0";

    /**
     * This is a ON state argument for commands requiring it.
     */
    private static final CharSequence STATE_ON = "1";

    /**
     * This is the connection used to manipulate the playback state and options.
     */
    private final MPDConnection mConnection;

    /**
     * This is the status object used to query for the current status of a playback state or
     * option.
     */
    private final MPDStatus mStatus;

    /**
     * The sole constructor.
     *
     * @param status     The status object.
     * @param connection The connection to send playback commands over.
     */
    public Playback(final MPDStatus status, final MPDConnection connection) {
        super();

        mConnection = connection;
        mStatus = status;
    }

    /**
     * Toggle the consume state.
     *
     * @return Returns a ResultFuture for any required error handling.
     * @throws IllegalStateException If the status state is invalid.
     */
    public ResultFuture consume() {
        return toggleState(CMD_ACTION_CONSUME, getStatus().isConsume());
    }

    /**
     * Sets the cross fade to the number given in the parameter.
     *
     * @param seconds The seconds to set the cross fade to.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture crossfade(final int seconds) {
        return mConnection.submit(CMD_ACTION_CROSS_FADE, Integer.toString(seconds));
    }

    /**
     * This method retrieves the ReplayGain status.
     *
     * @return The ReplayGain status.
     */
    public ResponseFuture getReplayGainStatus() {
        return new ResponseFuture(mConnection.submit(CMD_ACTION_REPLAY_GAIN_STATUS));
    }

    /**
     * This method validates the status prior to use.
     *
     * @return A validated status.
     * @throws IllegalStateException If the status state is invalid.
     */
    private MPDStatus getStatus() {
        if (!mStatus.isValid()) {
            throw new IllegalStateException("Cannot use the status when it's not valid.");
        }

        return mStatus;
    }

    /**
     * Plays the next track in the playlist queue.
     *
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture next() {
        return mConnection.submit(CMD_ACTION_NEXT);
    }

    /**
     * This normalizes any integer given as a volume to have a maximum of 100 and a minimum of 0.
     *
     * @param volume The integer to normalize.
     * @return Returns a normalized integer between 0 and 100.
     */
    private int normalizeVolume(final int volume) {
        return Math.max(MPDStatusMap.VOLUME_MIN,
                Math.min(MPDStatusMap.VOLUME_MAX, volume));
    }

    /**
     * If the current playback state is playing, this method will pause it.
     *
     * @return Returns a ResultFuture for any required error handling. Null if no action was taken.
     * @throws IllegalStateException If the status state is invalid.
     * @see #togglePlayback()
     */
    public ResultFuture pause() {
        final ResultFuture future;

        if (getStatus().getState() == MPDStatusMap.STATE_PLAYING) {
            future = mConnection.submit(CMD_ACTION_PAUSE, STATE_ON);
        } else {
            future = null;
        }

        return future;
    }

    /**
     * Changes the play state to playing.
     *
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture play() {
        return mConnection.submit(CMD_ACTION_PLAY);
    }

    /**
     * Plays the playlist queue starting with the {@code track} parameter.
     *
     * <p>This requires the {@link Music} item be populated with a song id.</p>
     *
     * @param track The track to play.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture play(final Music track) {
        return mConnection.submit(CMD_ACTION_PLAY_ID, Integer.toString(track.getSongId()));
    }

    /**
     * Plays the playlist queue starting with the playlist queue position given in the
     * {@code queuePosition} parameter.
     *
     * @param queuePosition The playlist queue position to play.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture play(final int queuePosition) {
        return mConnection.submit(CMD_ACTION_PLAY, Integer.toString(queuePosition));
    }

    /**
     * Plays the previous track in the playlist queue.
     *
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture previous() {
        return mConnection.submit(CMD_ACTION_PREVIOUS);
    }

    /**
     * Toggles the random playback state.
     *
     * @return Returns a ResultFuture for any required error handling.
     * @throws IllegalStateException If the status state is invalid.
     */
    public ResultFuture random() {
        return toggleState(CMD_ACTION_RANDOM, getStatus().isRandom());
    }

    /**
     * Toggles the repeat playback state.
     *
     * @return Returns a ResultFuture for any required error handling.
     * @throws IllegalStateException If the status state is invalid.
     */
    public ResultFuture repeat() {
        return toggleState(CMD_ACTION_REPEAT, getStatus().isRepeat());
    }

    /**
     * Seeks to a {@code position} in the current playing {@code track}.
     *
     * @param position The position to seek to in the current playing track, in seconds.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture seek(final long position) {
        return mConnection.submit(CMD_ACTION_SEEK_CURRENT_TRACK, Long.toString(position));
    }

    /**
     * Seeks to a {@code position} in playlist queue {@code position}.
     *
     * @param queuePosition The queue position to seek to.
     * @param position      The position in the track to seek to.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture seek(final int queuePosition, final long position) {
        return mConnection.submit(CMD_ACTION_SEEK, Integer.toString(queuePosition),
                Long.toString(position));
    }

    /**
     * Seeks to a {@code position} in a {@code track}.
     *
     * @param trackToSeek The track to seek in.
     * @param position    The position to seek to in the {@code trackToSeek} parameter, in seconds.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture seek(final Music trackToSeek, final long position) {
        return mConnection.submit(CMD_ACTION_SEEK_ID, Integer.toString(trackToSeek.getSongId()),
                Long.toString(position));
    }

    /**
     * Sets the MixRamp decibel setting.
     *
     * @param decibels The decibels to set the MixRamp settings to.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture setMixRampDecibels(final float decibels) {
        return mConnection.submit(CMD_ACTION_MIX_RAMP_DECIBELS, Float.toString(decibels));
    }

    /**
     * This method sets the MixRamp delay (in seconds).
     *
     * <p>If set to {@link #MIX_RAMP_DELAY_DISABLE}, MixRamp will be disabled and will fall back
     * to crossfading.</p>
     *
     * @param delay The delay to set MixRamp to.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture setMixRampDelay(final int delay) {
        final ResultFuture future;

        if (delay == MIX_RAMP_DELAY_DISABLE) {
            future = mConnection.submit(CMD_ACTION_MIX_RAMP_DELAY,
                    MIX_RAMP_DELAY_DISABLE_ARGUMENT);
        } else {
            future = mConnection.submit(CMD_ACTION_MIX_RAMP_DELAY, Integer.toString(delay));
        }

        return future;
    }

    /**
     * Sets volume to an absolute volume defined in the parameter.
     *
     * @param volume Sets the volume, this volume will be normalized between 0 and 100.
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture setVolume(final int volume) {
        final int vol = normalizeVolume(volume);

        return mConnection.submit(CMD_ACTION_SET_VOLUME, Integer.toString(vol));
    }

    /**
     * Toggles the {@code single} state.
     *
     * @return Returns a ResultFuture for any required error handling.
     * @throws IllegalStateException If the status state is invalid.
     */
    public ResultFuture single() {
        return toggleState(CMD_ACTION_SINGLE, getStatus().isSingle());
    }

    /**
     * Steps the volume up or down in a positive or negative increment given in the volume
     * parameter.
     *
     * @param volume The amount to step the volume up or down. This volume will be normalized to
     *               between 0 and 100.
     * @return Returns a ResultFuture for any required error handling.
     * @throws IllegalStateException If the status state is invalid.
     */
    public ResultFuture stepVolume(final int volume) {
        final int steppedVolume = normalizeVolume(getStatus().getVolume() + volume);

        return mConnection.submit(CMD_ACTION_SET_VOLUME, Integer.toString(steppedVolume));
    }

    /**
     * Stops the playback.
     *
     * @return Returns a ResultFuture for any required error handling.
     */
    public ResultFuture stop() {
        return mConnection.submit(CMD_ACTION_STOP);
    }

    /**
     * Switches playback state playing to paused and visa versa. Otherwise, no action.
     *
     * @return Returns a ResultFuture for any required error handling. Null if no action was taken.
     * @throws IllegalStateException If the status state is invalid.
     */
    public ResultFuture togglePlayback() {
        final ResultFuture future;

        switch (getStatus().getState()) {
            case MPDStatusMap.STATE_PLAYING:
                future = pause();
                break;
            case MPDStatusMap.STATE_PAUSED:
            case MPDStatusMap.STATE_STOPPED:
                future = play();
                break;
            default:
                future = null;
                break;
        }

        return future;
    }

    /**
     * Toggles the state of a playback state or option.
     *
     * @param command    The MPD protocol command to toggle.
     * @param toggleFrom Toggle from the argument. If {@code true}, toggle to {@code false} or
     *                   {@code off}. If {@code false}, toggle to {@code true} or {@code on}.
     * @return Returns a ResultFuture for any required error handling.
     */
    private ResultFuture toggleState(final CharSequence command, final boolean toggleFrom) {
        final ResultFuture future;

        if (toggleFrom) {
            future = mConnection.submit(command, STATE_OFF);
        } else {
            future = mConnection.submit(command, STATE_ON);
        }

        return future;
    }
}
