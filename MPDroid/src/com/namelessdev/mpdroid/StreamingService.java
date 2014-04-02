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

import java.io.IOException;

/**
 * StreamingService hooks Android's audio framework to the
 * user's MPD streaming server to allow local audio playback.
 *
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id: $
 */
final public class StreamingService extends Service implements
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

    public static final String ACTION_START = FULLY_QUALIFIED_NAME + "START_STREAMING";

    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + "STOP_STREAMING";

    public static final String ACTION_BUFFERING_BEGIN = FULLY_QUALIFIED_NAME + "BUFFERING_BEGIN";

    public static final String ACTION_BUFFERING_END = FULLY_QUALIFIED_NAME + "BUFFERING_END";

    final private Handler delayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Stopping self by handler delay.");
            stopSelf();
        }
    };

    final private Handler delayedPlayHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mediaPlayer.prepareAsync();
        }
    };

    final private Handler delayedWindDownHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("TAG", "Winding down resource by delay.");
            windDownResources();
        }
    };

    private boolean serviceControlHandlersActive = false;

    private TelephonyManager mTelephonyManager = null;

    private MPDApplication app = null;

    private MediaPlayer mediaPlayer = null;

    private AudioManager audioManager = null;

    /** This field will contain the URL of the MPD server streaming source */
    private String streamSource = null;

    private boolean streamingStoppedForCall = false;

    /** Is MPD playing? */
    private boolean isPlaying = false;

    /**
     * Setup for the method which allows MPDroid to override behavior during
     * phone events.
     */
    final private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
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
                        tryToStream();
                        streamingStoppedForCall = false;
                    }
                    break;
            }
        }
    };

    /** Keep track of the number of errors encountered. */
    private int errorIterator = 0;

    /** Keep track when mediaPlayer is preparing a stream */
    private boolean preparingStreaming = false;

    private static void setupHandler(final Handler delayedHandler, final int DELAY) {
        Message msg = delayedHandler.obtainMessage();
        delayedHandler.sendMessageDelayed(msg, DELAY);
    }

    /**
     * getState is a convenience method to safely retrieve a state object.
     *
     * @return A current state object.
     */
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
    private void tryToStream() {
        if (preparingStreaming || !isPlaying || !app.getApplicationState().streamingMode) {
            Log.d(TAG, "Not ready to stream.");
        } else {
            beginStreaming();
        }
    }

    private void beginStreaming() {
        Log.d(TAG, "StreamingService.beginStreaming()");
        if (mediaPlayer == null) {
            windUpResources();
        }

        preparingStreaming = true;
        stopControlHandlers();

        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(streamSource);
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Failed to set the MediaPlayer data source for " + streamSource, e);
            windDownResources();
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

    /** A method to send a quick message to another class. */
    private void sendIntent(String msg, Class destination) {
        Log.d(TAG, "Sending intent " + msg + " to " + destination + ".");
        Intent i = new Intent(this, destination);
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
     *
     * @param focusChange The type of focus change.
     */
    @Override
    final public void onAudioFocusChange(int focusChange) {
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
    final public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * A MediaPlayer callback to be invoked when playback of a media source has completed.
     *
     * @param mp The MediaPlayer object that reached the end of the stream.
     */
    @Override
    final public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "StreamingService.onCompletion()");

        /**
         * Streaming should already be stopped at this point,
         * but there might be some things to clean up.
         */
        stopStreaming();

        /**
         * If MPD is restarted during streaming, onCompletion() will be called.
         * onStateChange() won't be called. If we still detect playing, restart the stream.
         */
        if (isPlaying) {
            tryToStream();
        }
    }

    final public void onCreate() {
        Log.d(TAG, "StreamingService.onCreate()");

        app = (MPDApplication) getApplication();

        /** If streaming mode is not enabled, return */
        if (app == null || !app.getApplicationState().streamingMode) {
            stopSelf();
            return;
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.oMPDAsyncHelper.addConnectionListener(this);
        app.setActivity(this);

        streamSource = "http://"
                + app.oMPDAsyncHelper.getConnectionSettings().getConnectionStreamingServer() + ":"
                + app.oMPDAsyncHelper.getConnectionSettings().iPortStreaming + "/"
                + app.oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming;

        isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(getState());
    }

    /**
     * This happens at the beginning of beginStreaming() to populate all
     * necessary resources for handling the MediaPlayer stream.
     */
    private void windUpResources() {
        Log.d(TAG, "Winding up resources.");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    /**
     * windDownResources occurs after a delay or during stopSelf() to
     * clean up resources and give up focus to the phone and sound.
     */
    private void windDownResources() {
        Log.d(TAG, "Winding down resources.");

        /**
         * If stopSelf() this will occur immediately, otherwise,
         * give the user time (60 seconds) to toggle the play button.
         * Send a message to the NotificationService to release the
         * notification if it was generated for StreamingService.
         */
        sendIntent(ACTION_STOP, NotificationService.class);

        delayedPlayHandler.removeCallbacksAndMessages(delayedPlayHandler);

        if (mTelephonyManager != null) {
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        if (mediaPlayer != null) {
            /** This won't happened with delayed handler, but it can with stopSelf(). */
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        /**
         * If we got here due to an exception, try to stream
         * again until the error iterator runs out.
         */
        if (preparingStreaming) {
            Log.d(TAG,
                    "Stream had an error, trying to re-initiate streaming, try: " + errorIterator);
            errorIterator += 1;
            tryToStream();
        } else {
            /**
             * If stopSelf() this will occur immediately, otherwise,
             * give the user time (WIND_DOWN_IDLE_DELAY) to toggle the
             * play button. Send a message to the NotificationService
             * to release the notification if it was generated for
             * StreamingService.
             */
            sendIntent(ACTION_STOP, NotificationService.class);
            preparingStreaming = false;
        }
    }

    @Override
    final public void onDestroy() {
        Log.d(TAG, "StreamingService.onDestroy()");

        stopControlHandlers();

        /** Remove the current MPD listeners */
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        app.oMPDAsyncHelper.removeConnectionListener(this);

        windDownResources();

        app.unsetActivity(this);
        app.getApplicationState().streamingMode = false;
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
    final public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "StreamingService.onError()");
        final int MAX_ERROR = 4;

        if (errorIterator > 0) {
            Log.d(TAG, "Error occurred while streaming, this is try #" + errorIterator
                    + ", will attempt up to " + MAX_ERROR + " times.");
        }

        /** This keeps from continuous errors and battery draining. */
        if (errorIterator > MAX_ERROR) {
            stopSelf();
        }

        /** beginStreaming() will never start otherwise. */
        preparingStreaming = false;

        /** Either way we need to stop streaming. */
        stopStreaming();

        /** onError will often happen if we stop in the middle of preparing. */
        if (isPlaying) {
            tryToStream();
        }
        errorIterator += 1;
        return true;
    }

    /**
     * A MediaPlayer callback used when the media file is ready for playback.
     *
     * @param mp The MediaPlayer that is ready for playback.
     */
    @Override
    final public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "StreamingService.onPrepared()");
        sendIntent(ACTION_BUFFERING_END, NotificationService.class);
        mediaPlayer.start();
        preparingStreaming = false;
        errorIterator = 0; /** Reset the error iterator */
    }

    /**
     * Called by the system every time a client explicitly
     * starts the service by calling startService(Intent).
     */
    @Override
    final public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "StreamingService.onStartCommand()");
        if (!app.getApplicationState().streamingMode) {
            stopSelf();
            return 0;
        }

        switch (intent.getAction()) {
            case ACTION_START:
                tryToStream();
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

    /**
     * A JMPDComm callback which is invoked on MPD status change.
     *
     * @param mpdStatus MPDStatus after event.
     * @param oldState  Previous state.
     */
    @Override
    final public void stateChanged(MPDStatus mpdStatus, String oldState) {
        Log.d(TAG, "StreamingService.stateChanged()");

        final String state = mpdStatus.getState();

        if (state != null) {
            switch (state) {
                case MPDStatus.MPD_STATE_PLAYING:
                    stopControlHandlers();
                    isPlaying = true;
                    tryToStream();
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                case MPDStatus.MPD_STATE_PAUSED:
                    isPlaying = false;
                    stopStreaming();
                    break;
            }
        }
    }

    private void stopStreaming() {
        Log.d(TAG, "StreamingService.stopStreaming()");

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        setupServiceControlHandlers();
    }

    private void stopControlHandlers() {
        if (serviceControlHandlersActive) {
            Log.d(TAG, "Removing control handlers");
            delayedWindDownHandler.removeCallbacksAndMessages(null);
            delayedStopHandler.removeCallbacksAndMessages(null);
            serviceControlHandlersActive = false;
        }
    }

    private void setupServiceControlHandlers() {
        if (!serviceControlHandlersActive) {
            Log.d(TAG, "Setting up control handlers");
            final int STOP_IDLE_DELAY = 900000; /** 15 minutes */
            final int WIND_DOWN_IDLE_DELAY = 210000; /** 3.5 minutes  */
            /**
             * Wind down handler, this gives the user time to come back and click on the
             * notification.
             */
            setupHandler(delayedWindDownHandler, WIND_DOWN_IDLE_DELAY);
            /**
             * Stop handler so we don't annoy the user when they forget to turn streamingMode off.
             */
            setupHandler(delayedStopHandler, STOP_IDLE_DELAY);
            serviceControlHandlersActive = true;
        }
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }
}
