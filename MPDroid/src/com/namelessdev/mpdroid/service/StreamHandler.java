/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.service;

import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.MPDStatus;

import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

/**
 * StreamHandler hooks Android's audio framework to the
 * user's MPD streaming server to allow local audio playback.
 *
 * @author Arnaud Barisain Monrose (Dream_Team)
 */
public final class StreamHandler implements
        /**
         * OnInfoListener is not used because it is broken (never gets called, ever)..
         * OnBufferingUpdateListener is not used because it depends on a stream completion time.
         */
        Handler.Callback,
        OnAudioFocusChangeListener,
        OnCompletionListener,
        OnErrorListener,
        OnPreparedListener {

    /** This is the class unique Binder identifier. */
    static final int LOCAL_UID = 400;

    /** Messages that can be sent to clients. */
    public static final int IS_ACTIVE = LOCAL_UID + 1;

    /** Message to send to start this handler. */
    public static final int START = LOCAL_UID + 2;

    /** Message to send to stop this handler. */
    public static final int STOP = LOCAL_UID + 3;

    /** Kills (or hides) the notification if StreamHandler started it. */
    static final int REQUEST_NOTIFICATION_STOP = LOCAL_UID + 4;

    /** Keeps the notification alive, but puts it in non-streaming status. */
    static final int STREAMING_STOP = LOCAL_UID + 5;

    /** Let notification know it's time to display buffering banner. */
    static final int BUFFERING_BEGIN = LOCAL_UID + 6;

    /** Remove the buffering banner from the notification handler. */
    static final int BUFFERING_END = LOCAL_UID + 7;

    private static final boolean DEBUG = MPDroidService.DEBUG;

    /** Workaround to delay preparation of stream on Android 4.4.2 and earlier. */
    private static final int PREPARE_ASYNC = 1;

    /**
     * Called as an argument to windDownResources() when a
     * message is not required to send to the service.
     */
    private static final int INVALID_INT = -1;

    private static final String TAG = "StreamHandler";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid.service." + TAG;

    public static final String ACTION_START = FULLY_QUALIFIED_NAME + ".ACTION_START";

    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + ".ACTION_STOP";

    private final Handler mHandler = new Handler(this);

    private final ConnectionInfo mConnectionInfo
            = MPDroidService.MPD_ASYNC_HELPER.getConnectionSettings();

    private final MPDroidService mServiceContext;

    private AudioManager mAudioManager = null;

    /** Keep track of the number of errors encountered. */
    private int mErrorIterator = 0;

    /** Is this handler active? */
    private boolean mIsActive = false;

    /** Is MPD playing? */
    private boolean mIsPlaying = false;

    private MediaPlayer mMediaPlayer = null;

    /** Keep track when MediaPlayer is preparing a stream. */
    private boolean mPreparingStreaming = false;

    /** Service handler used for communicating with service. */
    private Handler mServiceHandler = null;

    StreamHandler(final MPDroidService serviceContext, final Handler serviceHandler,
            final AudioManager audioManager) {
        super();
        if (DEBUG) {
            Log.d(TAG, "StreamHandler constructor.");
        }

        mServiceContext = serviceContext;
        mAudioManager = audioManager;
        mServiceHandler = serviceHandler;
    }

    /**
     * A function to translate 'what' fields to literal debug name, used primarily for debugging.
     *
     * @param what A 'what' field.
     * @return The literal field name.
     */
    public static String getHandlerValue(final int what) {
        final String result;

        switch (what) {
            case IS_ACTIVE:
                result = "IS_ACTIVE";
                break;
            case START:
                result = "START";
                break;
            case STOP:
                result = "STOP";
                break;
            case REQUEST_NOTIFICATION_STOP:
                result = "REQUEST_NOTIFICATION_STOP";
                break;
            case STREAMING_STOP:
                result = "STREAMING_STOP";
                break;
            case BUFFERING_BEGIN:
                result = "BUFFERING_BEGIN";
                break;
            case BUFFERING_END:
                result = "BUFFERING_END";
                break;
            default:
                result = "{unknown}: " + what;
                break;
        }
        return "StreamHandler." + result;
    }

    /** Get the current server streaming URL. */
    private String getStreamSource() {
        return "http://" + mConnectionInfo.streamServer + ':'
                + mConnectionInfo.streamPort + '/' + mConnectionInfo.streamSuffix;
    }

    private void beginStreaming() {
        if (DEBUG) {
            Log.d(TAG, "StreamHandler.beginStreaming()");
        }
        if (mMediaPlayer == null) {
            windUpResources();
        }

        mServiceHandler.sendEmptyMessage(BUFFERING_BEGIN);
        final String streamSource = getStreamSource();
        final long asyncIdle = 1500L;
        mPreparingStreaming = true;
        mServiceHandler.removeMessages(STOP);

        /**
         * With MediaPlayer, there is a racy bug which affects, minimally, Android KitKat and lower.
         * If mediaPlayer.prepareAsync() is called too soon after mediaPlayer.setDataSource(), and
         * after the initial mediaPlayer.play(), general and non-specific errors are usually emitted
         * for the first few 100 milliseconds.
         *
         * Sometimes, these errors result in nagging Log errors, sometimes these errors result in
         * unrecoverable errors. This handler sets up a 1.5 second delay between
         * mediaPlayer.setDataSource() and mediaPlayer.AsyncPrepare() whether first play after
         * handler start or not.
         *
         * The magic number here can be adjusted if there are any more problems. I have witnessed
         * these errors occur at 750ms, but never higher. It's worth doubling, even in optimal
         * conditions, stream buffering is pretty slow anyhow. Adjust if necessary.
         *
         * This order is very specific and if interrupted can cause big problems.
         */
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(streamSource);
            mHandler.sendEmptyMessageDelayed(PREPARE_ASYNC, asyncIdle); /** Go to onPrepared() */
        } catch (final IOException e) {
            Log.e(TAG, "IO failure while trying to stream from: " + streamSource, e);
            windDownResources(BUFFERING_END);
        } catch (final IllegalStateException e) {
            Log.e(TAG,
                    "This is typically caused by a change in the server state during stream preparation.",
                    e);
            windDownResources(BUFFERING_END);
        }
    }

    @Override
    public boolean handleMessage(final Message msg) {
        boolean result = false;

        if (msg.what == PREPARE_ASYNC) {
            if (mIsPlaying) {
                if (DEBUG) {
                    Log.d(TAG, "Start mediaPlayer buffering.");
                }
                mMediaPlayer.prepareAsync();
                /**
                 * Between here and onPrepared, if the media server
                 * stream pauses, error handling workarounds will be used.
                 */
            } else {
                mPreparingStreaming = false;
                windDownResources(STREAMING_STOP);
            }
            result = true;
        }

        return result;
    }

    final boolean isActive() {
        return mIsActive;
    }

    /**
     * Handle the change of volume if a notification, or any other kind of
     * interrupting audio event.
     *
     * @param focusChange The type of focus change.
     */
    @Override
    public final void onAudioFocusChange(final int focusChange) {
        if (DEBUG) {
            Log.d(TAG, "StreamHandler.onAudioFocusChange() with " + focusChange);
        }
        final float duckVolume = 0.2f;

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer.isPlaying()) {
                    if (DEBUG) {
                        Log.d(TAG, "Regaining after ducked transient loss.");
                    }
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                } else if (!mPreparingStreaming) {
                    if (DEBUG) {
                        Log.d(TAG, "Coming out of transient loss.");
                    }
                    mMediaPlayer.start();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                MPDControl.run(MPDroidService.MPD_ASYNC_HELPER.oMPD, MPDControl.ACTION_PAUSE);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mMediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mMediaPlayer.setVolume(duckVolume, duckVolume);
                break;
            default:
                break;
        }
    }

    /**
     * A MediaPlayer callback to be invoked when playback of a media source has completed.
     *
     * @param mp The MediaPlayer object that reached the end of the stream.
     */
    @Override
    public final void onCompletion(final MediaPlayer mp) {
        if (DEBUG) {
            Log.d(TAG, "StreamHandler.onCompletion()");
        }

        /**
         * If MPD is restarted during streaming, onCompletion() will be called.
         * onStateChange() won't be called. If we still detect playing, restart the stream.
         */
        if (mIsPlaying) {
            tryToStream();
        } else {
            /**
             * The only way we make it here is with an empty playlist. Don't send a
             * message to the notification, it already knows to stop on empty playlist.
             */
            windDownResources(INVALID_INT);
        }
    }

    /**
     * A MediaPlayer callback to be invoked when there has been an error during an asynchronous
     * operation (other errors will throw exceptions at method call time).
     *
     * @param mp    The current mediaPlayer.
     * @param what  The type of error that has occurred.
     * @param extra An extra code, specific to the error. Typically implementation dependent.
     * @return True if the method handled the error, false if it didn't. Returning false, or not
     * having an OnErrorListener at all, will cause the OnCompletionListener to be called.
     */
    @Override
    public final boolean onError(final MediaPlayer mp, final int what, final int extra) {
        if (DEBUG) {
            Log.d(TAG, "StreamHandler.onError()");
        }
        final int maxError = 4;

        if (mErrorIterator > 0) {
            Log.d(TAG, "Error occurred while streaming, this is try #" + mErrorIterator
                    + ", will attempt up to " + maxError + " times.");
        }

        /** This keeps from continuous errors and battery draining. */
        if (mErrorIterator > maxError) {
            stop();
        }

        /** beginStreaming() will never start otherwise. */
        mPreparingStreaming = false;

        /** Either way we need to stop streaming. */
        windDownResources(STREAMING_STOP);

        mErrorIterator += 1;
        return true;
    }

    /**
     * A MediaPlayer callback used when the media file is ready for playback.
     *
     * @param mp The MediaPlayer that is ready for playback.
     */
    @Override
    public final void onPrepared(final MediaPlayer mp) {
        final int focusResult;

        if (DEBUG) {
            Log.d(TAG, "StreamHandler.onPrepared()");
        }

        if (mIsPlaying) {
            focusResult = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        } else {
            focusResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED;
        }

        if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mServiceHandler.sendEmptyMessage(BUFFERING_END);
            mMediaPlayer.start();
        } else {
            /** Because preparingStreaming is still set, this will reset the stream. */
            windDownResources(STREAMING_STOP);
        }

        mPreparingStreaming = false;
        mErrorIterator = 0; /** Reset the error iterator. */
    }

    final void stop() {
        if (DEBUG) {
            Log.d(TAG, "StreamHandler.stop()");
        }

        mHandler.removeMessages(PREPARE_ASYNC);

        windDownResources(REQUEST_NOTIFICATION_STOP);

        mIsActive = false;
    }

    final void start(final String mpdState) {
        mIsActive = true;
        mIsPlaying = MPDStatus.MPD_STATE_PLAYING.equals(mpdState);
        if (!mPreparingStreaming && mIsPlaying) {
            tryToStream();
        }
    }

    /**
     * A JMPDComm callback which is invoked on MPD status change.
     *
     * @param mpdStatus MPDStatus after event.
     */
    final void stateChanged(final MPDStatus mpdStatus) {
        if (DEBUG) {
            Log.d(TAG, "StreamHandler.stateChanged()");
        }

        final String state = mpdStatus.getState();

        if (state != null && mIsActive) {
            switch (state) {
                case MPDStatus.MPD_STATE_PLAYING:
                    mServiceHandler.removeMessages(STOP);
                    mIsPlaying = true;
                    tryToStream();
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                    /** Detect final song and let onCompletion handle it */
                    if (mpdStatus.getNextSongPos() == -1 || mpdStatus.getPlaylistLength() == 0) {
                        break;
                    }
                    /** Fall Through */
                case MPDStatus.MPD_STATE_PAUSED:
                    /**
                     * If in the middle of stream preparation, "Buffering…" notification message
                     * is likely.
                     */
                    if (mPreparingStreaming) {
                        windDownResources(BUFFERING_END);
                    } else {
                        windDownResources(STREAMING_STOP);
                    }
                    mIsPlaying = false;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * If streaming mode is activated this will setup the Android mediaPlayer
     * framework, register the media button events, register the remote control
     * client then setup and the framework streaming.
     */
    private void tryToStream() {
        if (mPreparingStreaming) {
            Log.d(TAG, "A stream is already being prepared.");
        } else if (!mIsPlaying) {
            Log.d(TAG, "MPD is not currently playing, can't stream.");
        } else {
            beginStreaming();
        }
    }

    /**
     * windDownResources occurs after a delay or during stopSelf() to
     * clean up resources and give up focus to the phone and sound.
     */
    private void windDownResources(final int action) {
        if (DEBUG) {
            Log.d(TAG, "Winding down resources.");
        }

        if (action != INVALID_INT) {
            mServiceHandler.sendEmptyMessage(action);
        }

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(this);
        }

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }

            /**
             * Cannot run reset/release when buffering, MediaPlayer will ANR or crash MPDroid, at
             * least on Android 4.4.2. Worst case, not resetting may cause a stale buffer to play at
             * the beginning and restart buffering; not perfect, but this is a pretty good solution.
             */
            if (mPreparingStreaming) {
                Log.w(TAG, "Media player paused during streaming, workarounds running.");
                mHandler.removeMessages(PREPARE_ASYNC);
            } else {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }

    /**
     * This happens at the beginning of beginStreaming() to populate all
     * necessary resources for handling the MediaPlayer stream.
     */
    private void windUpResources() {
        if (DEBUG) {
            Log.d(TAG, "Winding up resources.");
        }

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setWakeMode(mServiceContext, PowerManager.PARTIAL_WAKE_LOCK);
    }
}
