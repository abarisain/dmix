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

package com.anpmech.mpd.subsystem.status;

import com.anpmech.mpd.Log;
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.concurrent.MPDFuture;
import com.anpmech.mpd.connection.CommandResponse;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.exception.MPDException;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a <A HREF="http://www.musicpd.org/doc/protocol/command_reference.html#command_status"
 * target="_top">status</A> command response of the <A HREF="http://www.musicpd.org/doc/protocol"
 * target="_top">MPD protocol</A>.
 */
public class MPDStatusMap extends ResponseMap implements MPDStatus {

    /**
     * This is the value given if there was no float in the map with the given key in the map.
     */
    public static final float DEFAULT_FLOAT = ResponseMap.FLOAT_DEFAULT;

    /**
     * This is the value given if there was no int in the map with the given key in the map.
     */
    public static final int DEFAULT_INTEGER = ResponseMap.INTEGER_DEFAULT;

    /**
     * This is the value given if there was no long in the map with the given key in the map.
     */
    public static final long DEFAULT_LONG = ResponseMap.LONG_DEFAULT;

    /**
     * This is the value given if there was no String in the map with the given key in the map.
     */
    public static final String DEFAULT_STRING = ResponseMap.STRING_DEFAULT;

    /**
     * The media server play state interface representation for RESPONSE_STATE_PAUSED.
     * <p/>
     * This value is compatible with Android's PlaybackState.STATE_PAUSED.
     */
    public static final int STATE_PAUSED = 2;

    /**
     * The media server play state interface representation for RESPONSE_STATE_PLAYING.
     * <p/>
     * This value is compatible with Android's PlaybackState.STATE_PLAYING.
     */
    public static final int STATE_PLAYING = 3;

    /**
     * The media server play state interface representation for RESPONSE_STATE_STOPPED.
     * <p/>
     * This value is compatible with Android's PlaybackState.STATE_STOPPED.
     */
    public static final int STATE_STOPPED = 1;

    /**
     * The default media server play state interface representation.
     * <p/>
     * This value is compatible with Android's PlaybackState.STATE_NONE.
     */
    public static final int STATE_UNKNOWN = 0;

    /**
     * The MPD protocol response maximum volume integer.
     */
    public static final int VOLUME_MAX = 100;

    /**
     * The MPD protocol response minimum volume integer, if a mixer is available.
     */
    public static final int VOLUME_MIN = 0;

    /**
     * The MPD protocol response to volume if there is no available mixer.
     */
    public static final int VOLUME_UNAVAILABLE = -1;

    /**
     * The default number of MPDStatus entries.
     * <p/>
     * The status command responds with ~20 entries during play as of standard MPD implementation
     * 0.19.
     */
    private static final int DEFAULT_ENTRY_COUNT = 20;

    /**
     * The key from the status command for the value of various times related to the currently
     * playing track.
     */
    private static final CharSequence RESPONSE_AUDIO = "audio";

    /**
     * The key from the status command for the value of the bit rate of the currently playing
     * track.
     */
    private static final CharSequence RESPONSE_BIT_RATE = "bitrate";

    /**
     * The key from the status command for the value of the status of the consume option, 1 for
     * enabled, 0 for disabled.
     */
    private static final CharSequence RESPONSE_CONSUME = "consume";

    /**
     * The key from the status command for the value in seconds the crossfade option.
     */
    private static final CharSequence RESPONSE_CROSS_FADE = "xfade";

    /**
     * The key from the status command for the value of the job id of the database update.
     */
    private static final CharSequence RESPONSE_DATABASE_UPDATING = "updating_db";

    /**
     * The key from the status command for the value of the duration of the current track
     * represented by a float.
     */
    private static final CharSequence RESPONSE_DURATION = "duration";

    /**
     * The key from the status command for the value of the elapsed time of the current track
     * represented by a float.
     */
    private static final CharSequence RESPONSE_ELAPSED_HIGH_RESOLUTION = "elapsed";

    /**
     * The key from the status command for the value of the last error message.
     * <p/>
     * This is one way to get retrieve an error message, though, it is better parsed from the error
     * code given by the {@link MPDException}.
     */
    private static final CharSequence RESPONSE_ERROR = "error";

    /**
     * The key from the status command for the value of the MixRamp threshold in dB.
     */
    private static final CharSequence RESPONSE_MIX_RAMP_DB = "mixrampdb";

    /**
     * The key from the status command for the value of the MixRamp delay in seconds.
     */
    private static final CharSequence RESPONSE_MIX_RAMP_DELAY = "mixrampdelay";

    /**
     * The key from the status command for the value of the next playlist queue song ID.
     */
    private static final CharSequence RESPONSE_NEXT_SONG_ID = "nextsongid";

    /**
     * The key from the status command for the value of the next playlist queue position.
     */
    private static final CharSequence RESPONSE_NEXT_SONG_POSITION = "nextsong";

    /**
     * The key from the status command for the value of a invalid float.
     */
    private static final String RESPONSE_NOT_A_NUMBER = "nan";

    /**
     * The key from the status command for the value of the length of the current playlist queue.
     */
    private static final CharSequence RESPONSE_PLAYLIST_LENGTH = "playlistlength";

    /**
     * The key from the status command for the value of the version of the current playlist queue.
     */
    private static final CharSequence RESPONSE_PLAYLIST_VERSION = "playlist";

    /**
     * The key from the status command for the value of the status of the random option.
     */
    private static final CharSequence RESPONSE_RANDOM = "random";

    /**
     * The key from the status command for the value of the status of the repeat option.
     */
    private static final CharSequence RESPONSE_REPEAT = "repeat";

    /**
     * The key from the status command for the value of the status of the single option.
     */
    private static final CharSequence RESPONSE_SINGLE = "single";

    /**
     * The key from the status command for the value of the current track song ID.
     */
    private static final CharSequence RESPONSE_SONG_ID = "songid";

    /**
     * The key from the status command for the value of the current track song playlist queue
     * position.
     */
    private static final CharSequence RESPONSE_SONG_POSITION = "song";

    /**
     * The key from the status command for the value of the current playing status of the server.
     * <p/>
     *
     * @see #RESPONSE_STATE_PAUSED
     * @see #RESPONSE_STATE_PLAYING
     * @see #RESPONSE_STATE_STOPPED
     */
    private static final CharSequence RESPONSE_STATE = "state";

    /**
     * A MPD protocol server response value for the play state paused.
     */
    private static final String RESPONSE_STATE_PAUSED = "pause";

    /**
     * A MPD protocol server response value for the play state playing.
     */
    private static final String RESPONSE_STATE_PLAYING = "play";

    /**
     * A MPD protocol server response value for the play state stopped.
     */
    private static final String RESPONSE_STATE_STOPPED = "stop";

    /**
     * The key from the status command for the value total time elapsed (of current playing/paused
     * song).
     */
    private static final CharSequence RESPONSE_TIME = "time";

    /**
     * The key from the status command for the value of the media server's mixer state.
     */
    private static final CharSequence RESPONSE_VOLUME = "volume";

    /**
     * The class log identifier.
     */
    private static final String TAG = "MPDStatus";

    /**
     * The connection used to keep this object up to date.
     */
    private final MPDConnection mConnection;

    /**
     * The locally generated time of the last status update.
     */
    private long mUpdateTime;

    /**
     * This constructor initializes the backend storage for the status response.
     */
    public MPDStatusMap(final MPDConnection connection) {
        super(DEFAULT_ENTRY_COUNT);

        mConnection = connection;
    }

    /**
     * This constructor is used to create a immutable copy of this class.
     *
     * @param responseMap The response map backend storage map.
     * @see #getImmutableStatus()
     */
    private MPDStatusMap(final Map<CharSequence, String> responseMap) {
        super(responseMap);

        mConnection = null;
    }

    /**
     * Retrieves current track bit rate.
     *
     * @return current track bit rate.
     */
    @Override
    public final long getBitrate() {
        return parseMapLong(RESPONSE_BIT_RATE);
    }

    /**
     * Retrieves bits resolution from playing song.
     *
     * @return bits resolution from playing song.
     */
    @Override
    public final int getBitsPerSample() {
        final String value = getMapValue(RESPONSE_AUDIO);
        final int bitsPerSample;

        if (value == null) {
            bitsPerSample = DEFAULT_INTEGER;
        } else {
            final int delimiterIndex = value.indexOf(':');
            final int secondIndex = value.lastIndexOf(':');

            bitsPerSample = parseInteger(value.substring(delimiterIndex + 1, secondIndex));
        }

        return bitsPerSample;
    }

    /**
     * Retrieves number of channels from playing song.
     *
     * @return number of channels from playing song.
     */
    @Override
    public final int getChannels() {
        final String value = getMapValue(RESPONSE_AUDIO);
        final int channels;

        if (value == null) {
            channels = DEFAULT_INTEGER;
        } else {
            channels = parseInteger(value.substring(value.lastIndexOf(':') + 1));
        }

        return channels;
    }

    /**
     * Retrieves current cross-fade time.
     *
     * @return current cross-fade time in seconds.
     */
    @Override
    public final int getCrossfade() {
        return parseMapInteger(RESPONSE_CROSS_FADE);
    }

    /**
     * Returns the current song duration in milliseconds.
     *
     * @return The current song duration in milliseconds.
     */
    @Override
    public final float getDuration() {
        return parseMapFloat(RESPONSE_DURATION);
    }

    /**
     * Retrieves current track elapsed time. If the server status is playing, this time is
     * calculated.
     *
     * @return Elapsed time for the current track.
     */
    @Override
    public final long getElapsedTime() {
        final String value = getMapValue(RESPONSE_TIME);
        long elapsedTime = DEFAULT_LONG;

        if (value != null) {
            elapsedTime = parseLong(value.substring(0, value.indexOf(':')));

            if (isState(STATE_PLAYING)) {
                /** We can't expect to always update right before this is called. */
                elapsedTime += TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - mUpdateTime);
            }
        }

        return elapsedTime;
    }

    /**
     * Retrieves current track time with a higher resolution.
     *
     * @return Current track time (high resolution).
     */
    @Override
    public final float getElapsedTimeHighResolution() {
        return parseMapFloat(RESPONSE_ELAPSED_HIGH_RESOLUTION);
    }

    /**
     * Retrieves error message.
     *
     * @return error message.
     * @see MPDException#getAckCommandQueuePosition(String)
     * @see MPDException#getAckErrorCode(String)
     * @see MPDException#getMessage()
     */
    @Override
    public final String getError() {
        return getMapValue(RESPONSE_ERROR);
    }

    /**
     * Gets an immutable copy of this object.
     *
     * @return An immutable copy of this object.
     */
    public final MPDStatus getImmutableStatus() {
        return new MPDStatusMap(Collections.unmodifiableMap(getMap()));
    }

    /**
     * The MixRamp delay in seconds.
     *
     * @return The MixRamp delay in seconds.
     */
    @Override
    public final float getMixRampDelay() {
        return parseMapFloat(RESPONSE_MIX_RAMP_DB);
    }

    /**
     * Retrieves the MixRamp threshold in dB.
     *
     * @return The MixRamp threshold in dB.
     */
    @Override
    public final float getMixRampValue() {
        return parseMapFloat(RESPONSE_MIX_RAMP_DELAY);
    }

    /**
     * Retrieves the next song id from the playlist queue.
     *
     * @return The next song id from the playlist queue.
     */
    @Override
    public final int getNextSongId() {
        return parseMapInteger(RESPONSE_NEXT_SONG_ID);
    }

    /**
     * Retrieves the next song position from the playlist queue.
     *
     * @return The next song position from the playlist queue.
     */
    @Override
    public final int getNextSongPos() {
        return parseMapInteger(RESPONSE_NEXT_SONG_POSITION);
    }

    /**
     * Retrieves the length of the playlist.
     *
     * @return the length of the playlist.
     */
    @Override
    public final int getPlaylistLength() {
        return parseMapInteger(RESPONSE_PLAYLIST_LENGTH);
    }

    /**
     * Retrieves playlist version.
     *
     * @return playlist version.
     */
    @Override
    public final int getPlaylistVersion() {
        return parseMapInteger(RESPONSE_PLAYLIST_VERSION);
    }

    /**
     * Retrieves sample rate from playing song.
     *
     * @return sample rate from playing song.
     */
    @Override
    public final int getSampleRate() {
        final String value = getMapValue(RESPONSE_AUDIO);
        int sampleRate = DEFAULT_INTEGER;

        if (value != null) {
            final int delimiterIndex = value.indexOf(':');

            sampleRate = parseInteger(value.substring(0, delimiterIndex));
        }

        return sampleRate;
    }

    /**
     * Retrieves current song playlist id.
     *
     * @return current song playlist id.
     */
    @Override
    public final int getSongId() {
        return parseMapInteger(RESPONSE_SONG_ID);
    }

    /**
     * Retrieves current song playlist number.
     *
     * @return current song playlist number.
     */
    @Override
    public final int getSongPos() {
        return parseMapInteger(RESPONSE_SONG_POSITION);
    }

    /**
     * Retrieves the current play state of the server.
     *
     * @return The player state, {@link #STATE_PLAYING} if the play state is playing, {@link
     * #STATE_PAUSED} if paused, {@link #STATE_STOPPED} if stopped, or {@link #STATE_UNKNOWN} if
     * not connected or otherwise unknown.
     */
    @Override
    public int getState() {
        final String value = getMapValue(RESPONSE_STATE);
        final int state;

        if (value == null) {
            state = STATE_UNKNOWN;
        } else {
            switch (value) {
                case RESPONSE_STATE_PLAYING:
                    state = STATE_PLAYING;
                    break;
                case RESPONSE_STATE_PAUSED:
                    state = STATE_PAUSED;
                    break;
                case RESPONSE_STATE_STOPPED:
                    state = STATE_STOPPED;
                    break;
                default:
                    state = STATE_UNKNOWN;
                    break;
            }
        }

        return state;
    }

    /**
     * Retrieves current track total time.
     *
     * @return current track total time.
     */
    @Override
    public final long getTotalTime() {
        final String value = getMapValue(RESPONSE_TIME);
        long result = DEFAULT_LONG;

        if (value != null) {
            final int timeIndex = value.indexOf(':');

            result = parseLong(value.substring(timeIndex + 1));
        }

        return result;
    }

    /**
     * Retrieves the process id of the database update task, if updating.
     *
     * @return The process id of the database update task, if updating.
     * @see #isUpdating()
     */
    @Override
    public final int getUpdatePID() {
        return parseMapInteger(RESPONSE_DATABASE_UPDATING);
    }

    /**
     * Retrieves the volume, minimum value of 0, maximum value of 100, if available.
     *
     * @return VOLUME_UNAVAILABLE if set to -1, VOLUME_MIN if 0 or less than -1, VOLUME_MAX if 100
     * or greater, the actual volume if between 0 and 100.
     */
    @Override
    public final int getVolume() {
        int volume = parseMapInteger(RESPONSE_VOLUME);

        if (volume != DEFAULT_INTEGER) {
            /**
             * If necessary, set within the bounds of the MPD protocol.
             */
            if (volume != VOLUME_UNAVAILABLE && volume < VOLUME_MIN) {
                Log.warning(TAG, "Invalid volume: (" + volume + "), setting to " + VOLUME_MIN);
                volume = VOLUME_MIN;
            } else if (volume > VOLUME_MAX) {
                Log.warning(TAG, "Invalid volume: (" + volume + "), setting to " + VOLUME_MAX);
                volume = VOLUME_MAX;
            }
        }

        return volume;
    }

    /**
     * Queries for the status of the consume option.
     *
     * @return True if the consume option is enabled, false otherwise.
     */
    @Override
    public final boolean isConsume() {
        return isMapValueTrue(RESPONSE_CONSUME);
    }

    /**
     * Queries for the status of the MixRampDB option.
     *
     * @return True if the MixRampDB option is enabled, false otherwise.
     */
    @Override
    public final boolean isMixRampEnabled() {
        return !RESPONSE_NOT_A_NUMBER.equals(getMapValue(RESPONSE_MIX_RAMP_DELAY));
    }

    /**
     * Queries for the status of the random option.
     *
     * @return True if the random option is enabled, false otherwise.
     */
    @Override
    public final boolean isRandom() {
        return isMapValueTrue(RESPONSE_RANDOM);
    }

    /**
     * Queries for the status of the repeat option.
     *
     * @return True if the repeat option is enabled, false otherwise.
     */
    @Override
    public final boolean isRepeat() {
        return isMapValueTrue(RESPONSE_REPEAT);
    }

    /**
     * Queries for the status of the single option.
     *
     * @return True if the single option is enabled, false otherwise.
     */
    @Override
    public final boolean isSingle() {
        return isMapValueTrue(RESPONSE_SINGLE);
    }

    /**
     * A convenience method to query the current state.
     *
     * @param queryState The state to query against.
     * @return True if the same as the current state.
     * @see #getState()
     */
    @Override
    public final boolean isState(final int queryState) {
        return getState() == queryState;
    }

    /**
     * Queries for status of database updating.
     *
     * @return True if the MPD server is updating, false otherwise.
     * @see #getUpdatePID()
     */
    @Override
    public final boolean isUpdating() {
        return getMapValue(RESPONSE_DATABASE_UPDATING) != null;
    }

    /**
     * Lets callers know if the subclass Object is valid.
     *
     * @return True if the subclass is valid, false otherwise.
     */
    @Override
    public final boolean isValid() {
        return super.isValid();
    }

    /**
     * Retrieves a string representation of the {@link ResponseMap} and this object.
     *
     * @return A string representation of the ResponseMap and this resulting object.
     */
    @Override
    public final String toString() {
        return super.toString() +
                "MPDStatus: {" +
                "getBitrate(): " + getBitrate() +
                ", getBitsPerSample(): " + getBitsPerSample() +
                ", getChannels(): " + getChannels() +
                ", getCrossfade(): " + getCrossfade() +
                ", getDuration(): " + getDuration() +
                ", getElapsedTime(): " + getElapsedTime() +
                ", getElapsedTimeHighResolution(): " + getElapsedTimeHighResolution() +
                ", getError(): " + getError() +
                ", getNextSongPos(): " + getNextSongPos() +
                ", getNextSongId(): " + getNextSongId() +
                ", getMixRampValue(): " + getMixRampValue() +
                ", getMixRampDelay(): " + getMixRampDelay() +
                ", getPlaylistVersion(): " + getPlaylistVersion() +
                ", getPlaylistLength(): " + getPlaylistLength() +
                ", getSampleRate(): " + getSampleRate() +
                ", getSongPos(): " + getSongPos() +
                ", getSongId(): " + getSongId() +
                ", getState(): " + getState() +
                ", getTotalTime(): " + getTotalTime() +
                ", getUpdatePID(): " + getUpdatePID() +
                ", getVolume(): " + getVolume() +
                ", isConsume(): " + isConsume() +
                ", isMixRampEnabled: " + isMixRampEnabled() +
                ", isRandom(): " + isRandom() +
                ", isRepeat(): " + isRepeat() +
                ", isSingle(): " + isSingle() +
                ", isUpdating(): " + isUpdating() +
                ", mUpdateTime=" + mUpdateTime +
                '}';
    }

    /**
     * Retrieves status of the connected server. Do not call this method directly unless you
     * absolutely know what you are doing. If a long running application needs a status update, use
     * the {@code IdleSubsystemMonitor} instead.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see IdleSubsystemMonitor
     */
    public void update() throws IOException, MPDException {
        final MPDFuture<CommandResponse> future = mConnection.submit(MPDCommand.MPD_CMD_STATUS);

        update(future.get());
    }

    /**
     * Updates the status cache from the MPD Server protocol response.
     *
     * @param commandResponse The response from the server.
     */
    @Override
    public void update(final CommandResponse commandResponse) {
        super.update(commandResponse);
        mUpdateTime = new Date().getTime();
    }
}
