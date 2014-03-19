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

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.ConnectionListener;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * StreamingService hooks Android's audio framework to MPD's streaming server to
 * allow local audio playback.
 *
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id: $
 */
public class StreamingService extends Service implements
        /**
         * OnInfoListener is not used because it is broken (never gets called, ever)..
         * OnBufferingUpdateListener is not used because it depends on a stream completion time.
         */
        ConnectionListener,
        OnAudioFocusChangeListener,
        OnCompletionListener,
        OnErrorListener,
        OnPreparedListener,
        StatusChangeListener {


    public static final String ACTION_DIE = "com.namelessdev.mpdroid.DIE";

    public static final String ACTION_START = "com.namelessdev.mpdroid.START_STREAMING";

    public static final String ACTION_STOP = "com.namelessdev.mpdroid.STOP_STREAMING";

    public static final String ACTION_RESET = "com.namelessdev.mpdroid.RESET_STREAMING";

    public static final String CMD_REMOTE = "com.namelessdev.mpdroid.REMOTE_COMMAND";

    public static final String CMD_COMMAND = "COMMAND";

    public static final String CMD_PAUSE = "PAUSE";

    public static final String CMD_STOP = "STOP";

    public static final String CMD_PLAY = "PLAY";

    public static final String CMD_PLAYPAUSE = "PLAYPAUSE";

    public static final String CMD_PREV = "PREV";

    public static final String CMD_NEXT = "NEXT";

    public static final String CMD_DIE = "DIE"; // Just in case

    static final String TAG = "MPDroidStreamingService";

    /**
     * How long to wait before queuing the message into the current handler
     * queue.
     */
    private static final int IDLE_DELAY = 60000;

    MPDApplication app;

    private MediaPlayer mediaPlayer;

    private AudioManager audioManager;

    /** This field will contain the URL of the MPD server streaming source */
    private String streamSource;

    private String prevMpdState;

    /** Is MPD playing? */
    private boolean isPlaying;

    /**
     * isPaused is required (along with isPlaying) so the service doesn't start
     * when it's not wanted.
     */
    private boolean isPaused;

    /** Set up the message handler. */
    private Handler delayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isPlaying || isPaused) {
                return;
            }
            die();
        }
    };

    /**
     * Setup for the method which allows MPDroid to override behavior during
     * phone events.
     */
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (!app.getApplicationState().streamingMode) {
                stopSelf();
                return;
            }

            if (state == TelephonyManager.CALL_STATE_RINGING) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringvolume > 0 && isPlaying) {
                    isPaused = true;
                    pauseStreaming();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // pause the music while a conversation is in progress
                if (!isPlaying) {
                    return;
                }
                isPaused = (isPaused || isPlaying) && (app.getApplicationState().streamingMode);
                pauseStreaming();
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                // Resume playback only if music was playing when the call was
                // answered
                if (isPaused) {
                    // resume play back only if music was playing
                    // when the call was answered
                    beginStreaming();
                }
            }
        }
    };

    /**
     * Field containing the ID used to stopSelfResult() which will stop the
     * streaming service.
     */
    private Integer lastStartID;

    /**
     * If streaming mode is activated this will setup the Android mediaPlayer
     * framework, register the media button events, register the remote control
     * client then setup and the framework streaming.
     */
    public void beginStreaming() {
        Log.d(TAG, "StreamingService.beginStreaming()");
        // just to be sure, we do not want to start when we're not supposed to
        if (mediaPlayer == null ||
                !app.getApplicationState().streamingMode) {
            return;
        }

        isPlaying = true;
        isPaused = false;

        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(streamSource);
        } catch (IOException e) {
            /**
             * TODO: Notify the user
             */
            endBuffering();
            isPlaying = false;
        } catch (IllegalStateException e) {
            // wtf what state ?
            isPlaying = false;
        }

        mediaPlayer.prepareAsync();
    }

    @Override
    public void connectionFailed(String message) {
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
    }

    @Override
    public void connectionSucceeded(String message) {
    }

    /**
     * This turns streaming mode off and stops the StreamingService.
     */
    public void die() {
        Log.d(TAG, "StreamingService.die()");
        onDestroy();

        stopSelfResult(lastStartID);
    }

    /**
     * Send a message to the NotificationService to let it know to end the buffering banner.
     */
    private void endBuffering() {
        Intent i = new Intent(this, NotificationService.class);
        i.setAction(NotificationService.STREAM_BUFFERING_END);
        this.startService(i);
    }

    /**
     * A JMPDComm callback to be invoked during library state changes.
     *
     * @param updating true when updating, false when not updating.
     */
    @Override
    public void libraryStateChanged(boolean updating) {
    }

    /**
     * This sends the next command to MPD, stops and resumes streaming.
     */
    public void next() {
        Log.d(TAG, "StreamingService.next()");
        MPD mpd = app.oMPDAsyncHelper.oMPD;
        try {
            mpd.next();
        } catch (MPDServerException e) {
            /** Do nothing. */
        }
        stopStreaming();
        beginStreaming();
    }

    /**
     * Handle the change of volume if a notification, or any other kind of
     * interrupting audio event.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "StreamingService.onAudioFocusChange()");
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            mediaPlayer.setVolume(0.2f, 0.2f);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            mediaPlayer.setVolume(1f, 1f);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * A MediaPlayer callback to be invoked when playback of a media source has completed.
     *
     * @param mp the MediaPlayer that reached the end of the file
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "StreamingService.onCompletion()");
        Message msg = delayedStopHandler.obtainMessage();
        delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY); // Don't suck
        // the battery
        // too much

        MPDStatus statusMpd = null;
        try {
            statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
        } catch (MPDServerException e) {
            // TODO: Properly handle exception for getStatus() failure.
        }

        if (statusMpd == null) {
            return;
        }

        String state = statusMpd.getState();
        if (state == null) {
            return;
        }

        if (state.equals(MPDStatus.MPD_STATE_PLAYING)) {
            // TODO Stop resuming if no 3G. There's no point. Add something that
            // says "ok we're waiting for 3G/wifi !"
            beginStreaming();
        } else {
            // Somethings happening, like crappy network or MPD just stopped..
            prevMpdState = state;
            die();
        }
    }

    public void onCreate() {
        Log.d(TAG, "StreamingService.onCreate()");
        super.onCreate();

        app = (MPDApplication) getApplication();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        /** If streaming mode is not enabled, return */
        if (app == null || !app.getApplicationState().streamingMode) {
            stopSelf();
            return;
        }

        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        prevMpdState = "";
        isPlaying = true;
        isPaused = false;
        lastStartID = 0;

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);

        if (audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Toast.makeText(this, R.string.audioFocusFailed, Toast.LENGTH_LONG).show();
            stop();
        }

        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.oMPDAsyncHelper.addConnectionListener(this);
        app.setActivity(this);
        streamSource = "http://"
                + app.oMPDAsyncHelper.getConnectionSettings().getConnectionStreamingServer() + ":"
                + app.oMPDAsyncHelper.getConnectionSettings().iPortStreaming + "/"
                + app.oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "StreamingSerice.onDestroy()");

        delayedStopHandler.removeCallbacksAndMessages(null);

        /** Send a message to the NotificationService that streaming is ending */
        Intent i = new Intent(this, NotificationService.class);
        i.setAction(NotificationService.ACTION_STREAMING_END);
        this.startService(i);

        /** Remove the current MPD listeners */
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        app.oMPDAsyncHelper.removeConnectionListener(this);

        if (audioManager != null) {
            audioManager.abandonAudioFocus(this);
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        app.unsetActivity(this);
        app.getApplicationState().streamingMode = false;
        super.onDestroy();
    }

    /**
     * A MediaPlayer callback to be invoked when there has been an error during an asynchronous
     * operation (other errors will throw exceptions at method call time).
     *
     * @param mp    the MediaPlayer the error pertains to.
     * @param what  the type of error that has occurred.
     * @param extra an extra code, specific to the error. Typically implementation dependent.
     * @return True if the method handled the error, false if it didn't. Returning false, or not
     * having an OnErrorListener at all, will cause the OnCompletionListener to be called.
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "StreamingService.onError()");
        pauseStreaming();
        return false;
    }

    /**
     * A MediaPlayer callback to be invoked when the media source is ready for playback.
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "StreamingService.onPrepared()");
        // Buffering done
        endBuffering();
        isPlaying = true;
        prevMpdState = "";
        mediaPlayer.start();
    }

    /**
     * Called by the system every time a client explicitly starts the service
     * by calling startService(Intent).
     *
     * @param intent  The Intent supplied to startService(Intent), as given. This may be null if
     *                the
     *                service is being restarted after its process has gone away, and it had
     *                previously returned anything except START_STICKY_COMPATIBILITY.
     * @param flags   Additional data about this start request. Currently either 0,
     *                START_FLAG_REDELIVERY, or START_FLAG_RETRY.
     * @param startId A unique integer representing this specific request to start. Use with
     *                stopSelfResult(int).
     * @return The return value indicates what semantics the system should use for the service's
     * current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "StreamingService.onStartCommand()");
        lastStartID = startId;
        if (!app.getApplicationState().streamingMode) {
            stopSelfResult(lastStartID);
            return 0;
        }

        switch (intent.getAction()) {
            case ACTION_DIE:
                die();
                break;
            case ACTION_RESET:
                stopStreaming();
                beginStreaming();
                break;
            case ACTION_START:
                beginStreaming();
                break;
            case ACTION_STOP:
                stopStreaming();
                break;
            case CMD_REMOTE:
                String cmd = intent.getStringExtra(CMD_COMMAND);

                switch (cmd) {
                    case CMD_NEXT:
                        next();
                        break;
                    case CMD_PREV:
                        prev();
                        break;
                    case CMD_PLAYPAUSE:
                        if (!isPaused) {
                            pauseStreaming();
                        } else {
                            beginStreaming();
                        }
                        break;
                    case CMD_PAUSE:
                        pauseStreaming();
                        break;
                    case CMD_STOP:
                        stop();
                        break;
                }
        }

        /**
         * We want this service to continue running until it is explicitly
         * stopped, so return sticky.
         */
        return START_STICKY;
    }

    /**
     * If streaming is playing, then streaming is paused, due to user command or
     * interrupting event this will stop the Android mediaPlayer framework while
     * keeping the notification showing.
     */
    public void pauseStreaming() {
        Log.d(TAG, "StreamingService.pauseStreaming()");
        if (!isPlaying) {
            return;
        }

        isPlaying = false;
        isPaused = true;
        endBuffering();
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
    }

    /**
     * This sends the previous command to MPD, stops and resumes streaming.
     */
    public void prev() {
        Log.d(TAG, "StreamingService.prev()");
        MPD mpd = app.oMPDAsyncHelper.oMPD;
        try {
            mpd.previous();
        } catch (MPDServerException e) {

        }
        stopStreaming();
        beginStreaming();
    }

    @Override
    public void randomChanged(boolean random) {
    }

    @Override
    public void repeatChanged(boolean repeating) {
    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) {
        Log.d(TAG, "StreamingService.stateChanged()");
        Message msg = delayedStopHandler.obtainMessage();
        delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        MPDStatus statusMpd = null;
        try {
            statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
        } catch (MPDServerException e) {
            // TODO: Properly handle exception for getStatus() failure.
        }

        if (statusMpd == null) {
            return;
        }

        String state = statusMpd.getState();
        if (state == null || state.equals(prevMpdState)) {
            return;
        }

        if (state.equals(MPDStatus.MPD_STATE_PLAYING)) {
            isPaused = false;
            beginStreaming();
            isPlaying = true;
        } else {
            prevMpdState = state;
            isPlaying = false;
            stopStreaming();
        }
    }

    /**
     * This stops the streaming, turns streaming mode off and stops the
     * StreamingService.
     */
    public void stop() {
        Log.d(TAG, "StreamingService.stop()");
        stopStreaming();
        die();
    }

    public void stopStreaming() {
        Log.d(TAG, "StreamingService.stopStreaming()");
        prevMpdState = "";
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.stop();
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        Log.d(TAG, "StreamingService.trackChanged()");
        prevMpdState = "";
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }
}
