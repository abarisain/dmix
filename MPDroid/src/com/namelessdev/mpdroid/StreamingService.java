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

import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
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
 */
public final class StreamingService extends Service implements
        /**
         * OnInfoListener is not used because it is broken (never gets called, ever)..
         * OnBufferingUpdateListener is not used because it depends on a stream completion time.
         */
        OnAudioFocusChangeListener,
        OnCompletionListener,
        OnErrorListener,
        OnPreparedListener,
        ServiceConnection, /** Service binder */
        StatusChangeListener {

    /** Kills (or hides) the notification if StreamingService started it. */
    public static final int REQUEST_NOTIFICATION_STOP = 1;

    /** Keeps the notification alive, but puts it in non-streaming status. */
    public static final int STREAMING_STOP = 2;

    /** Let notification know it's time to display buffering banner. */
    public static final int BUFFERING_BEGIN = 3;

    /** Remove the buffering banner from the notification service. */
    public static final int BUFFERING_END = 4;

    /** Let the notification know that this service is under error conditions. */
    public static final int BUFFERING_ERROR = 5;

    /** Let notification service know this service is running minimal resources. */
    public static final int SERVICE_WOUND_DOWN = 6;

    /** Let the notification service know this service will soon be buffering. */
    public static final int SERVICE_WOUND_UP = 7;

    private static final String TAG = "StreamingService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid." + TAG + '.';

    public static final String ACTION_START = FULLY_QUALIFIED_NAME + "START_STREAMING";

    private final Handler delayedStopHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "Stopping self by handler delay.");
            stopSelf();
        }
    };

    /**
     * Called as an argument to windDownResources() when a
     * message is not required to send to bound service.
     */
    private static final int INVALID_INT = -1;

    private final Handler delayedPlayHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            mediaPlayer.prepareAsync();
        }
    };

    private final MPDApplication app = MPDApplication.getInstance();

    /** Messenger for communicating with service. */
    private Messenger serviceMessenger = null;

    private boolean finalSong = false;

    private boolean serviceControlHandlersActive = false;

    private TelephonyManager mTelephonyManager = null;

    private MediaPlayer mediaPlayer = null;

    private AudioManager audioManager = null;

    private boolean streamingStoppedForCall = false;

    private boolean activeBufferingError = false;

    /** Is MPD playing? */
    private boolean isPlaying = false;

    /**
     * Setup for the method which allows MPDroid to override behavior during
     * phone events.
     */
    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(final int state, final String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    final int ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                    if (ringVolume == 0) {
                        break;
                    } /** Fall Through */
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isPlaying) {
                        streamingStoppedForCall = true;
                        windDownResources(STREAMING_STOP);
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // Resume playback only if music was playing when the call was answered
                    if (streamingStoppedForCall) {
                        tryToStream();
                        streamingStoppedForCall = false;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /** Flag indicating whether we have called bind on the service. */
    private boolean isBoundService = false;

    /** Keep track of the number of errors encountered. */
    private int errorIterator = 0;

    /** Keep track when mediaPlayer is preparing a stream */
    private boolean preparingStreaming = false;

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
        } catch (final MPDServerException e) {
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
        if (preparingStreaming && !activeBufferingError) {
            Log.d(TAG, "A stream is already being prepared.");
        } else if (!isPlaying) {
            Log.d(TAG, "MPD is not currently playing, can't stream.");
        } else if (!app.getApplicationState().streamingMode) {
            Log.d(TAG, "streamingMode is not currently active, won't stream.");
        } else {
            beginStreaming();
        }
    }

    private void beginStreaming() {
        Log.d(TAG, "StreamingService.beginStreaming()");
        if (mediaPlayer == null) {
            windUpResources();
        }

        sendToBoundService(SERVICE_WOUND_UP);
        final String streamSource = getStreamSource();
        final long ASYNC_IDLE = 1500L;
        preparingStreaming = true;
        stopControlHandlers();

        sendToBoundService(BUFFERING_BEGIN);

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
         *
         * This order is very specific and if interrupted can cause big problems.
         */
        try {
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(streamSource);
            final Message msg = delayedPlayHandler.obtainMessage();
            delayedPlayHandler.sendMessageDelayed(msg, ASYNC_IDLE); /** Go to onPrepared() */
        } catch (final IOException e) {
            Log.e(TAG, "IO failure while trying to stream from: " + streamSource, e);
            windDownResources(BUFFERING_ERROR);
        } catch (final IllegalStateException e) {
            Log.e(TAG,
                    "This is typically caused by a change in the server state during stream preparation.",
                    e);
            windDownResources(BUFFERING_ERROR);
        } finally {
            delayedPlayHandler.removeCallbacksAndMessages(delayedPlayHandler);
        }
    }

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
    }

    /**
     * Initiates our service binding, after complete, onBindService() should be called.
     */
    void doBindService() {
        /**
         * Establish a connection with the service.  We use an explicit class name because
         * there is no reason to be able to let other applications replace our component.
         */
        bindService(new Intent(this, NotificationService.class), this, Context.BIND_AUTO_CREATE);
        isBoundService = true;
        Log.e(TAG, "Binding.");
    }

    /**
     * Initiates our unbinding from the bound service, after
     * complete, onServiceDisconnected() should be called.
     */
    void doUnbindService() {
        if (isBoundService) {
            // Detach our existing connection.
            unbindService(this);
            isBoundService = false;
            Log.e(TAG, "Unbinding.");
        }
    }

    /**
     * A JMPDComm callback to be invoked during library state changes.
     *
     * @param updating  true when updating, false when not updating.
     * @param dbChanged true when the server database has been updated, false otherwise.
     */
    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    /**
     * Handle the change of volume if a notification, or any other kind of
     * interrupting audio event.
     *
     * @param focusChange The type of focus change.
     */
    @Override
    public final void onAudioFocusChange(final int focusChange) {
        Log.d(TAG, "StreamingService.onAudioFocusChange() with " + focusChange);
        final float DUCK_VOLUME = 0.2f;

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer.isPlaying()) {
                    Log.d(TAG, "Regaining after ducked transient loss.");
                    mediaPlayer.setVolume(1.0f, 1.0f);
                } else if (!preparingStreaming) {
                    Log.d(TAG, "Coming out of transient loss.");
                    mediaPlayer.start();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                MPDControl.run(MPDControl.ACTION_PAUSE);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
                break;
            default:
                break;
        }
    }

    /**
     * We haven't implemented this yet, as we have no need for anything
     * remote to send us anything other than service intents.
     *
     * @return null until implemented.
     */
    @Override
    public final IBinder onBind(final Intent intent) {
        return null;
    }

    /**
     * A MediaPlayer callback to be invoked when playback of a media source has completed.
     *
     * @param mp The MediaPlayer object that reached the end of the stream.
     */
    @Override
    public final void onCompletion(final MediaPlayer mp) {
        Log.d(TAG, "StreamingService.onCompletion()");

        /**
         * If MPD is restarted during streaming, onCompletion() will be called.
         * onStateChange() won't be called. If we still detect playing, restart the stream.
         */
        if (isPlaying) {
            tryToStream();
        } else {
            /**
             * The only way we make it here is with an empty playlist. Don't send a
             * message to the notification, it already knows to stop on empty playlist.
             */
            windDownResources(INVALID_INT);
        }
    }

    @Override
    public final void onCreate() {
        Log.d(TAG, "StreamingService.onCreate()");
        super.onCreate();

        doBindService();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        final StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.addConnectionLock(this);

        isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(getState());
    }

    private String getStreamSource() {
        return "http://"
                + app.oMPDAsyncHelper.getConnectionSettings().getConnectionStreamingServer() + ':'
                + app.oMPDAsyncHelper.getConnectionSettings().iPortStreaming + '/'
                + app.oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming;
    }

    /**
     * This is used rather than onStartCommand() to begin streaming as we depending on the Binder
     * to send messages to NotificationService.
     *
     * We use this to connect the Binder to NotificationService for IPC. This is called when the
     * connection with the service has been established, giving us the service object we can use to
     * interact with the service.  We are communicating with our service through an IDL interface,
     * so get a client-side representation of that from the raw service object.
     *
     * @param className The Class that has connected through the binder.
     */
    @Override
    public void onServiceConnected(final ComponentName className, final IBinder service) {
        serviceMessenger = new Messenger(service);
        Log.e(TAG, "Attached.");
        tryToStream();
    }

    /**
     * Called when the binder to the NotificationService is disconnected.
     *
     * @param className The Class that has connected through the binder.
     */
    @Override
    public void onServiceDisconnected(final ComponentName className) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        serviceMessenger = null;
        Log.e(TAG, "Disconnected.");
    }

    /**
     * Send a simple message to our bound service.
     *
     * @param message The simple message to send to the bound service.
     */
    private void sendToBoundService(final int message) {
        final Message msg = Message.obtain(null, message);
        try {
            serviceMessenger.send(msg);
        } catch (final RemoteException e) {
            Log.e(TAG, "Failed to communicate with bound service.", e);
            isBoundService = false;
        }
    }

    /**
     * This happens at the beginning of beginStreaming() to populate all
     * necessary resources for handling the MediaPlayer stream.
     */
    private void windUpResources() {
        Log.d(TAG, "Winding up resources.");

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        activeBufferingError = true;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
    }

    /**
     * windDownResources occurs after a delay or during stopSelf() to
     * clean up resources and give up focus to the phone and sound.
     */
    private void windDownResources(final int action) {
        Log.d(TAG, "Winding down resources.");

        sendToBoundService(SERVICE_WOUND_DOWN);

        if (STREAMING_STOP == action) {
            setupServiceControlHandlers();
        }

        if (action != INVALID_INT) {
            sendToBoundService(action);
        }

        if (mTelephonyManager != null) {
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        if (audioManager != null) {
            audioManager.abandonAudioFocus(this);
        }

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            /**
             * Cannot run reset/release when buffering, MediaPlayer will ANR or crash MPDroid, at
             * least on Android 4.4.2. Worst case, not resetting may cause a stale buffer to play at
             * the beginning and restart buffering; not perfect, but this is a pretty good solution.
             */
            if (preparingStreaming) {
                if (BUFFERING_ERROR == action) {
                    activeBufferingError = true;
                }
                delayedPlayHandler.removeCallbacksAndMessages(null);

            } else {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    @Override
    public final void onDestroy() {
        Log.d(TAG, "StreamingService.onDestroy()");
        super.onDestroy();

        stopControlHandlers();

        /** Remove the current MPD listeners */
        app.oMPDAsyncHelper.removeStatusChangeListener(this);

        windDownResources(REQUEST_NOTIFICATION_STOP);

        doUnbindService();

        app.removeConnectionLock(this);
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
    public final boolean onError(final MediaPlayer mp, final int what, final int extra) {
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
        windDownResources(STREAMING_STOP);

        errorIterator += 1;
        return true;
    }

    /**
     * A MediaPlayer callback used when the media file is ready for playback.
     *
     * @param mp The MediaPlayer that is ready for playback.
     */
    @Override
    public final void onPrepared(final MediaPlayer mp) {
        Log.d(TAG, "StreamingService.onPrepared()");
        final int focusResult = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        /**
         * Not to be playing here is unlikely but it's a race we need to avoid.
         */
        if (isPlaying && focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            sendToBoundService(BUFFERING_END);
            mediaPlayer.start();
        } else {
            /** Because preparingStreaming is still set, this will reset the stream. */
            windDownResources(STREAMING_STOP);
        }

        preparingStreaming = false;
        errorIterator = 0; /** Reset the error iterator. */
    }

    /**
     * Called by the system every time a client explicitly
     * starts the service by calling startService(Intent).
     */
    @Override
    public final int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.d(TAG, "StreamingService.onStartCommand()");
        super.onStartCommand(intent, flags, startId);

        /** Do nothing, it'll be done when the service is bound */
        if (!ACTION_START.equals(intent.getAction())) {
            stopSelf(); /** Don't start if someone doesn't know the knock. */
        }

        /**
         * We want this service to continue running until it is explicitly
         * stopped, so return sticky.
         */
        return START_STICKY;
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        /** Detect the final song and let the streaming complete rather than abrupt cut off. */
        finalSong = mpdStatus != null && mpdStatus.getNextSongPos() == -1;
    }

    @Override
    public void randomChanged(final boolean random) {
    }

    @Override
    public void repeatChanged(final boolean repeating) {
    }

    /**
     * A JMPDComm callback which is invoked on MPD status change.
     *
     * @param mpdStatus MPDStatus after event.
     * @param oldState  Previous state.
     */
    @Override
    public final void stateChanged(final MPDStatus mpdStatus, final String oldState) {
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
                    /** Detect final song and let onCompletion handle it */
                    if (finalSong || mpdStatus.getPlaylistLength() == 0) {
                        break;
                    }
                    /** Fall Through */
                case MPDStatus.MPD_STATE_PAUSED:
                    /**
                     * If in the middle of stream preparation, "Buffering…" notification message
                     * is likely.
                     */
                    if (preparingStreaming) {
                        windDownResources(BUFFERING_ERROR);
                    } else {
                        windDownResources(STREAMING_STOP);
                    }
                    isPlaying = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void stopControlHandlers() {
        if (serviceControlHandlersActive) {
            Log.d(TAG, "Removing control handlers");
            delayedStopHandler.removeCallbacksAndMessages(null);
            serviceControlHandlersActive = false;
        }
    }

    private void setupServiceControlHandlers() {
        if (!serviceControlHandlersActive) {
            Log.d(TAG, "Setting up control handlers");
            final long STOP_IDLE_DELAY = 600000L; /** 10 minutes */
            /**
             * Stop handler so we don't annoy the user when they forget to turn streamingMode off.
             */
            final Message msg = delayedStopHandler.obtainMessage();
            delayedStopHandler.sendMessageDelayed(msg, STOP_IDLE_DELAY);
            serviceControlHandlersActive = true;
        }
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
    }
}
