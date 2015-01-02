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

package org.a0z.mpd.subsystem.status;

/**
 * An interface to the {@link org.a0z.mpd.subsystem.status.MPDStatusMap} class for the methods
 * which are not required for MPDStatusMap modification.
 */
public interface MPDStatus {

    /**
     * Retrieves current track bit rate.
     *
     * @return current track bit rate.
     */
    long getBitrate();

    /**
     * Retrieves bits resolution from playing song.
     *
     * @return bits resolution from playing song.
     */
    int getBitsPerSample();

    /**
     * Retrieves number of channels from playing song.
     *
     * @return number of channels from playing song.
     */
    int getChannels();

    /**
     * Retrieves current cross-fade time.
     *
     * @return current cross-fade time in seconds.
     */
    int getCrossfade();

    /**
     * Returns the current song duration in milliseconds.
     *
     * @return The current song duration in milliseconds.
     */
    float getDuration();

    /**
     * Retrieves current track elapsed time. If the server status is playing, this time is
     * calculated.
     *
     * @return Elapsed time for the current track.
     */
    long getElapsedTime();

    /**
     * Retrieves current track time with a higher resolution.
     *
     * @return Current track time (high resolution).
     */
    float getElapsedTimeHighResolution();

    /**
     * Retrieves error message.
     *
     * @return error message.
     * @see com.anpmech.mpd.exception.MPDException#getAckCommandQueuePosition(String)
     * @see com.anpmech.mpd.exception.MPDException#getAckErrorCode(String)
     * @see com.anpmech.mpd.exception.MPDException#getMessage()
     */
    String getError();

    /**
     * The MixRamp delay in seconds.
     *
     * @return The MixRamp delay in seconds.
     */
    float getMixRampDelay();

    /**
     * Retrieves the MixRamp threshold in dB.
     *
     * @return The MixRamp threshold in dB.
     */
    float getMixRampValue();

    /**
     * Retrieves the next song id from the playlist queue.
     *
     * @return The next song id from the playlist queue.
     */
    int getNextSongId();

    /**
     * Retrieves the next song position from the playlist queue.
     *
     * @return The next song position from the playlist queue.
     */
    int getNextSongPos();

    /**
     * Retrieves the length of the playlist.
     *
     * @return the length of the playlist.
     */
    int getPlaylistLength();

    /**
     * Retrieves playlist version.
     *
     * @return playlist version.
     */
    int getPlaylistVersion();

    /**
     * Retrieves sample rate from playing song.
     *
     * @return sample rate from playing song.
     */
    int getSampleRate();

    /**
     * Retrieves current song playlist id.
     *
     * @return current song playlist id.
     */
    int getSongId();

    /**
     * Retrieves current song playlist number.
     *
     * @return current song playlist number.
     */
    int getSongPos();

    /**
     * Retrieves the play state of the server.
     *
     * @return The player state, {@link org.a0z.mpd.subsystem.status.MPDStatusMap#STATE_PLAYING} if
     * the play state is playing, {@link org.a0z.mpd.subsystem.status.MPDStatusMap#STATE_PAUSED} if
     * paused. {@link org.a0z.mpd.subsystem.status.MPDStatusMap#STATE_STOPPED} if stopped, or
     * {@link org.a0z.mpd.subsystem.status.MPDStatusMap#STATE_UNKNOWN} if not connected or
     * otherwise unknown.
     */
    int getState();

    /**
     * Retrieves current track total time.
     *
     * @return current track total time.
     */
    long getTotalTime();

    /**
     * Retrieves the process id of the database update task, if updating.
     *
     * @return The process id of the database update task, if updating.
     * @see #isUpdating()
     */
    int getUpdatePID();

    /**
     * Retrieves the volume, minimum value of 0, maximum value of 100, if available.
     *
     * @return VOLUME_UNAVAILABLE if set to -1, VOLUME_MIN if 0 or less than -1, VOLUME_MAX if 100
     * or greater, the actual volume if between 0 and 100.
     */
    int getVolume();

    /**
     * Queries for the status of the consume option.
     *
     * @return True if the consume option is enabled, false otherwise.
     */
    boolean isConsume();

    /**
     * Queries for the status of the MixRampDB option.
     *
     * @return True if the MixRampDB option is enabled, false otherwise.
     */
    boolean isMixRampEnabled();

    /**
     * Queries for the status of the random option.
     *
     * @return True if the random option is enabled, false otherwise.
     */
    boolean isRandom();

    /**
     * Queries for the status of the repeat option.
     *
     * @return True if the repeat option is enabled, false otherwise.
     */
    boolean isRepeat();

    /**
     * Queries for the status of the single option.
     *
     * @return True if the single option is enabled, false otherwise.
     */
    boolean isSingle();

    /**
     * A convenience method to query the current state.
     *
     * @param queryState The state to query against.
     * @return True if the same as the current state.
     * @see #getState()
     */
    boolean isState(final int queryState);

    /**
     * Queries for status of database updating.
     *
     * @return True if the MPD server is updating, false otherwise.
     * @see #getUpdatePID()
     */
    boolean isUpdating();

    /**
     * Lets callers know if the subclass Object is valid.
     *
     * @return True if the subclass is valid, false otherwise.
     */
    boolean isValid();

    /**
     * Retrieves a string representation of the {@link ResponseMap} and this object.
     *
     * @return A string representation of the ResponseMap and this resulting object.
     */
    String toString();
}
