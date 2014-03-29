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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.IOException;

/**
 * StreamingService hooks Android's audio framework to MPD's streaming server to
 * allow local audio playback.
 *
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id: $
 */

public class StreamingService extends Service implements StatusChangeListener, OnPreparedListener,
        OnCompletionListener,
        OnBufferingUpdateListener, OnErrorListener, OnInfoListener, ConnectionListener,
        OnAudioFocusChangeListener {

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

    public static boolean isServiceRunning = false;

    private MediaPlayer mediaPlayer;

    private AudioManager audioManager;

    /** This field will contain the URL of the MPD server streaming source */
    private String streamSource;

    /** Is the Android media framework buffering the stream? */
    private Boolean buffering;

    private String prevMpdState;

    /** Is MPD playing? */
    private boolean isPlaying;

    /**
     * isPaused is required (along with isPlaying) so the service doesn't start
     * when it's not wanted.
     */
    private boolean isPaused;

    /** Set up the message handler. */
    @SuppressLint("HandlerLeak")
    private Handler delayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isPlaying || isPaused || buffering) {
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
            MPDApplication app = (MPDApplication) getApplication();
            if (app == null) {
                return;
            }

            if (!(app).getApplicationState().streamingMode) {
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
     * Get the status of the streaming service.
     *
     * @return bool
     */
    public static Boolean getStreamingServiceStatus() {
        return isServiceRunning;
    }

    /**
     * If streaming mode is activated this will setup the Android mediaPlayer
     * framework, register the media button events, register the remote control
     * client then setup and the framework streaming.
     */
    public void beginStreaming() {
        // just to be sure, we do not want to start when we're not supposed to
        if (!((MPDApplication) getApplication()).getApplicationState().streamingMode) {
            return;
        }

        isPaused = false;
        isBuffering(true);

        if (mediaPlayer == null) {
            return;
        }

        try {
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(streamSource);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            /**
             * TODO: Notify the user
             */
            isBuffering(false);
            isPlaying = false;
        } catch (IllegalStateException e) {
            // wtf what state ?
            isPlaying = false;
        }
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
        onDestroy();

        ((MPDApplication) getApplication()).getApplicationState().streamingMode = false;
        stopSelfResult(lastStartID);
    }

    /**
     * This will send a message to the NotificationService to let it know the stream
     * isbbuffering, so it will inform the user via the notification itself.
     */
    private void isBuffering(boolean _buffering) {
        buffering = _buffering;
        Intent i = new Intent(this, NotificationService.class);
        if (buffering) {
            i.setAction(NotificationService.STREAM_BUFFERING_BEGIN);
        } else {
            i.setAction(NotificationService.STREAM_BUFFERING_END);
        }
        this.startService(i);
    }

    @Override
    public void libraryStateChanged(boolean updating) {
    }

    /**
     * This sends the next command to MPD, stops and resumes streaming.
     */
    public void next() {
        MPDApplication app = (MPDApplication) getApplication();
        MPD mpd = app.oMPDAsyncHelper.oMPD;
        try {
            mpd.next();
        } catch (MPDServerException e) {

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

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    /**
     * This will be called when the end of the stream is reached during
     * playback.
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Message msg = delayedStopHandler.obtainMessage();
        delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY); // Don't suck
        // the battery
        // too much
        MPDApplication app = (MPDApplication) getApplication();

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
        super.onCreate();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        /** If streaming mode is not enabled, return */
        if (!((MPDApplication) getApplication()).getApplicationState().streamingMode) {
            stopSelf();
            return;
        }

        isServiceRunning = true;
        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        isBuffering(true);
        prevMpdState = "";
        isPlaying = true;
        isPaused = false;
        lastStartID = 0;

        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);

        if (audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Toast.makeText(this, R.string.audioFocusFailed, Toast.LENGTH_LONG).show();
            stop();
        }

        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        MPDApplication app = (MPDApplication) getApplication();
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
        isServiceRunning = false;

        /** Send a message to the NotificationService that streaming is ending */
        Intent i = new Intent(this, NotificationService.class);
        i.setAction(NotificationService.ACTION_STREAMING_END);
        this.startService(i);

        if (audioManager != null) {
            audioManager.abandonAudioFocus(this);
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        MPDApplication app = (MPDApplication) getApplication();
        app.unsetActivity(this);
        app.getApplicationState().streamingMode = false;
        super.onDestroy();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        pauseStreaming();
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    /**
     * This will be called when MPDroid is ready to stream the MPD playback.
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        // Buffering done
        isBuffering(false);
        isPlaying = true;
        prevMpdState = "";
        mediaPlayer.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lastStartID = startId;
        if (!((MPDApplication) getApplication()).getApplicationState().streamingMode) {
            stopSelfResult(lastStartID);
            return 0;
        }

        if (intent.getAction().equals("com.namelessdev.mpdroid.START_STREAMING")) {
            beginStreaming();
        } else if (intent.getAction().equals("com.namelessdev.mpdroid.STOP_STREAMING")) {
            stopStreaming();
        } else if (intent.getAction().equals("com.namelessdev.mpdroid.RESET_STREAMING")) {
            stopStreaming();
            beginStreaming();
        } else if (intent.getAction().equals("com.namelessdev.mpdroid.DIE")) {
            die();
        } else if (intent.getAction().equals(CMD_REMOTE)) {
            String cmd = intent.getStringExtra(CMD_COMMAND);
            if (cmd.equals(CMD_NEXT)) {
                next();
            } else if (cmd.equals(CMD_PREV)) {
                prev();
            } else if (cmd.equals(CMD_PLAYPAUSE)) {
                if (!isPaused) {
                    pauseStreaming();
                } else {
                    beginStreaming();
                }
            } else if (cmd.equals(CMD_PAUSE)) {
                pauseStreaming();
            } else if (cmd.equals(CMD_STOP)) {
                stop();
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
        if (!isPlaying) {
            return;
        }

        isPlaying = false;
        isPaused = true;
        isBuffering(false);

        /** If the Android media framework crashes, try to stop it earlier. */
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
    }

    /**
     * This sends the previous command to MPD, stops and resumes streaming.
     */
    public void prev() {
        MPDApplication app = (MPDApplication) getApplication();
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
        Message msg = delayedStopHandler.obtainMessage();
        delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        MPDApplication app = (MPDApplication) getApplication();
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
        stopStreaming();
        die();
    }

    public void stopStreaming() {
        prevMpdState = "";
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.stop();
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        prevMpdState = "";

    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }
}
