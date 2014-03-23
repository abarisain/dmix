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

    private static final String TAG = "StreamingService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid." + TAG + ".";

    public static final String ACTION_DIE = FULLY_QUALIFIED_NAME + "DIE";

    public static final String ACTION_START = FULLY_QUALIFIED_NAME + "START_STREAMING";

    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + "STOP_STREAMING";

    public static final String ACTION_BUFFERING_BEGIN = FULLY_QUALIFIED_NAME + "BUFFERING_BEGIN";

    public static final String ACTION_BUFFERING_END = FULLY_QUALIFIED_NAME + "BUFFERING_END";

    /**
     * How long to wait before queuing the message into the current handler
     * queue.
     */
    private static final int IDLE_DELAY = 60000;

    private TelephonyManager mTelephonyManager = null;

    private MPDApplication app;

    private MediaPlayer mediaPlayer;

    private AudioManager audioManager;

    private Handler delayedPlayHandler = null;

    /** This field will contain the URL of the MPD server streaming source */
    private String streamSource;

    private String prevMpdState;

    private boolean streamingStoppedForCall = false;

    /** Is MPD playing? */
    private boolean isPlaying;

    /**
     * Setup for the method which allows MPDroid to override behavior during
     * phone events.
     */
    final private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (!app.getApplicationState().streamingMode) {
                stopSelf();
                return;
            }

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    final int ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                    if (ringVolume == 0) {
                        break;
                    } /** Otherwise, continue */
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isPlaying) {
                        streamingStoppedForCall = true;
                        stopStreaming();
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // Resume playback only if music was playing when the call was answered
                    if (streamingStoppedForCall) {
                        beginStreaming();
                        streamingStoppedForCall = false;
                    }
                    break;
            }
        }
    };

    /** Set up the message handler. */
    final private Handler delayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isPlaying) {
                return;
            }
            windDownResources();
        }
    };

    /** Keep track of the number of errors encountered. */
    private int errorIterator = 0;

    /** Keep track when mediaPlayer is preparing a stream */
    private boolean preparingStreaming = false;

    /**
     * Field containing the ID used to stopSelfResult() which will stop the
     * streaming service.
     */
    private Integer lastStartID;

    private String getState() {
        Log.d(TAG, "getState()");
        String state = null;

        try {
            state = app.oMPDAsyncHelper.oMPD.getStatus().getState();
        } catch (MPDServerException e) {
            Log.w(TAG, "Failed to get the current MPD state.", e);
        }

        return state;
    }

    /**
     * If streaming mode is activated this will setup the Android mediaPlayer
     * framework, register the media button events, register the remote control
     * client then setup and the framework streaming.
     */
    private void beginStreaming() {
        Log.d(TAG, "StreamingService.beginStreaming()");
        // just to be sure, we do not want to start when we're not supposed to
        if (preparingStreaming || !isPlaying ||
                !app.getApplicationState().streamingMode) {
            Log.d(TAG, "beginStreaming return called early.");
            return;
        } else if (mediaPlayer == null) {
            windUpResources();

            if (mediaPlayer == null) {
                Log.d(TAG,
                        "mediaPlayer null after attempt to populate, returning beginStreaming().");
                return;
            }
        }

        sendIntent(ACTION_BUFFERING_BEGIN, NotificationService.class);

        preparingStreaming = true;

        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(streamSource);
        } catch (IOException e) {
            /**
             * TODO: Notify the user
             */
            sendIntent(ACTION_BUFFERING_END, NotificationService.class);
            isPlaying = false;
        } catch (IllegalStateException e) {
            // wtf what state ?
            isPlaying = false;
            preparingStreaming = false;
        }

        /**
         * With MediaPlayer, there is a racy bug which affects, minimally, Android KitKat and lower.
         * If mediaPlayer.prepareAsync() is called too soon after mediaPlayer.setDataSource(), and
         * after the initial mediaPlayer.play(), general and non-specific errors are usually emitted
         * for the first few 100 milliseconds.
         *
         * Sometimes, these errors result in nagging Log errors, sometimes these errors result in
         * unrecoverable errors. This handler sets up a 1.5 second delay between
         * mediaPlayer.setDataSource() and mediaPlayer.AsyncPrepare() whether first play after
         * service start or not.
         *
         * The magic number here can be adjusted if there are any more problems. I have witnessed
         * these errors occur at 750ms, but never higher. It's worth doubling, even in optimal
         * conditions, stream buffering is pretty slow anyhow. Adjust if necessary.
         */
        Message msg = delayedPlayHandler.obtainMessage();
        delayedPlayHandler.sendMessageDelayed(msg, 1500);
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
    private void die() {
        Log.d(TAG, "StreamingService.die()");
        onDestroy();

        stopSelfResult(lastStartID);
    }

    /** A method to send a quick message to another class. */
    private void sendIntent(String msg, Class dest) {
        Log.d(TAG, "Sending intent " + msg + " to " + dest + ".");
        Intent i = new Intent(this, dest);
        i.setAction(msg);
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
            stopStreaming();
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

        /**
         * Streaming should already be stopped at this point,
         * but there might be some things to clean up.
         */
        stopStreaming();
    }

    public void onCreate() {
        Log.d(TAG, "StreamingService.onCreate()");
        super.onCreate();

        app = (MPDApplication) getApplication();

        /** If streaming mode is not enabled, return */
        if (app == null || !app.getApplicationState().streamingMode) {
            stopSelf();
            return;
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        lastStartID = 0;

        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.oMPDAsyncHelper.addConnectionListener(this);
        app.setActivity(this);

        streamSource = "http://"
                + app.oMPDAsyncHelper.getConnectionSettings().getConnectionStreamingServer() + ":"
                + app.oMPDAsyncHelper.getConnectionSettings().iPortStreaming + "/"
                + app.oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming;

        /** Seed the prevMpdState, onStatusUpdate() will keep it up-to-date afterwards. */
        prevMpdState = getState();
        isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(prevMpdState);
    }

    /**
     * This happens at the beginning of beginStreaming() to populate all
     * necessary resources for handling the MediaPlayer stream.
     */
    private void windUpResources() {
        Log.d(TAG, "Winding up resources.");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            /**
             * If we can't gain audio focus, let the user know and prior to acquiring resources.
             */
            Log.w(TAG, getText(R.string.audioFocusFailed).toString());
            Toast.makeText(this, R.string.audioFocusFailed, Toast.LENGTH_LONG).show();
            return;
        }

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);

        /**
         * Set up a handler for an Android MediaPlayer bug, for more
         * information, see the target in beginStreaming().
         */
        delayedPlayHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mediaPlayer.prepareAsync();
            }
        };
    }

    public void windDownResources() {
        Log.d(TAG, "Winding down resources.");
        if (audioManager != null) {
            audioManager.abandonAudioFocus(this);
        }

        /**
         * If die()ing this will occur immediately, otherwise,
         * give the user time (60 seconds) to toggle the play button.
         * Send a message to the NotificationService to release the
         * notification if it was generated for StreamingService.
         */
        sendIntent(ACTION_STOP, NotificationService.class);

        if (delayedPlayHandler != null) {
            delayedPlayHandler.removeCallbacksAndMessages(null);
        }

        if (mTelephonyManager != null) {
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        if (mediaPlayer != null) {
            /** This won't happened with delayed handler, but it can with die/destroy. */
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "StreamingSerice.onDestroy()");

        delayedStopHandler.removeCallbacksAndMessages(null);

        /** Remove the current MPD listeners */
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        app.oMPDAsyncHelper.removeConnectionListener(this);

        windDownResources();

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
        final int MAX_ERROR = 4;

        if (errorIterator > 0) {
            Log.d(TAG, "Error occurred while streaming, this is try #" + errorIterator
                    + ", will attempt up to " + MAX_ERROR + " times.");
        }

        /** This keeps from continuous errors and battery draining. */
        if (errorIterator > MAX_ERROR) {
            die();
        }

        /** beginStreaming() will never start otherwise. */
        preparingStreaming = false;

        /** Either way we need to stop streaming. */
        stopStreaming();

        /** onError will often happen if we stop in the middle of preparing. */
        if (isPlaying) {
            beginStreaming();
        }
        errorIterator += 1;
        return true;
    }

    /**
     * A MediaPlayer callback to be invoked when the media source is ready for playback.
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "StreamingService.onPrepared()");
        sendIntent(ACTION_BUFFERING_END, NotificationService.class);
        prevMpdState = "";
        mediaPlayer.start();
        preparingStreaming = false;
        errorIterator = 0; /** Reset the error iterator */
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
            case ACTION_START:
                beginStreaming();
                break;
            case ACTION_STOP:
                stopStreaming();
                break;
        }

        /**
         * We want this service to continue running until it is explicitly
         * stopped, so return sticky.
         */
        return START_STICKY;
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
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

        final String state = mpdStatus.getState();
        if (state == null || state.equals(prevMpdState)) {
            return;
        }

        isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(state);
        prevMpdState = state;

        if (isPlaying) {
            beginStreaming();
        } else {
            stopStreaming();
        }
    }

    private void stopStreaming() {
        Log.d(TAG, "StreamingService.stopStreaming()");

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        /** Wind down resources in 60 seconds, if still idle. */
        Message msg = delayedStopHandler.obtainMessage();
        delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
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
