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

import com.namelessdev.mpdroid.MPDApplication;
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
    static final int REQUEST_NOTIFICATION_STOP = 1;

    /** Keeps the notification alive, but puts it in non-streaming status. */
    static final int STREAMING_STOP = 2;

    /** Let notification know it's time to display buffering banner. */
    static final int BUFFERING_BEGIN = 3;

    /** Remove the buffering banner from the notification service. */
    static final int BUFFERING_END = 4;

    /** Let the notification know that this service is under error conditions. */
    static final int BUFFERING_ERROR = 5;

    /** Let notification service know this service is running minimal resources. */
    static final int SERVICE_WOUND_DOWN = 6;

    /** Let the notification service know this service will soon be buffering. */
    static final int SERVICE_WOUND_UP = 7;

    /**
     * Called as an argument to windDownResources() when a
     * message is not required to send to bound service.
     */
    private static final int INVALID_INT = -1;

    private static final String TAG = "StreamingService";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid." + TAG + '.';

    public static final String ACTION_START = FULLY_QUALIFIED_NAME + "START_STREAMING";

    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "Stopping self by handler delay.");
            stopSelf();
        }
    };

    private static MPDApplication sApp = MPDApplication.getInstance();

    private final Handler mDelayedPlayHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            mMediaPlayer.prepareAsync();
        }
    };

    /** Keep track if we're in an active error. */
    private boolean mActiveBufferingError = false;

    private AudioManager mAudioManager = null;

    /** Keep track of the number of errors encountered. */
    private int mErrorIterator = 0;

    /** Keeps track if the final song is playing. */
    private boolean mFinalSong = false;

    /** Flag indicating whether we have called bind on the service. */
    private boolean mIsBoundService = false;

    /** Is MPD playing? */
    private boolean mIsPlaying = false;

    private MediaPlayer mMediaPlayer = null;

    /** Keep track when MediaPlayer is preparing a stream. */
    private boolean mPreparingStreaming = false;

    /** Keep track of active service handler. */
    private boolean mServiceControlHandlersActive = false;

    /** Messenger for communicating with service. */
    private Messenger mServiceMessenger = null;

    /**
     * getState is a convenience method to safely retrieve a state object.
     *
     * @return A current state object.
     */
    private static String getState() {
        Log.d(TAG, "getState()");
        String state = null;

        try {
            state = sApp.oMPDAsyncHelper.oMPD.getStatus().getState();
        } catch (final MPDServerException e) {
            Log.w(TAG, "Failed to get the current MPD state.", e);
        }

        return state;
    }

    /** Get the current server streaming URL. */
    private static String getStreamSource() {
        return "http://"
                + sApp.oMPDAsyncHelper.getConnectionSettings().getConnectionStreamingServer() + ':'
                + sApp.oMPDAsyncHelper.getConnectionSettings().iPortStreaming + '/'
                + sApp.oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming;
    }

    private void beginStreaming() {
        Log.d(TAG, "StreamingService.beginStreaming()");
        if (mMediaPlayer == null) {
            windUpResources();
        }

        sendToBoundService(SERVICE_WOUND_UP);
        final String streamSource = getStreamSource();
        final long asyncIdle = 1500L;
        mPreparingStreaming = true;
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
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(streamSource);
            final Message msg = mDelayedPlayHandler.obtainMessage();
            mDelayedPlayHandler.sendMessageDelayed(msg, asyncIdle); /** Go to onPrepared() */
        } catch (final IOException e) {
            Log.e(TAG, "IO failure while trying to stream from: " + streamSource, e);
            windDownResources(BUFFERING_ERROR);
        } catch (final IllegalStateException e) {
            Log.e(TAG,
                    "This is typically caused by a change in the server state during stream preparation.",
                    e);
            windDownResources(BUFFERING_ERROR);
        } finally {
            mDelayedPlayHandler.removeCallbacksAndMessages(mDelayedPlayHandler);
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
        bindService(new Intent(this, MPDroidService.class), this, Context.BIND_AUTO_CREATE);
        mIsBoundService = true;
        Log.d(TAG, "Binding.");
    }

    /**
     * Initiates our unbinding from the bound service, after
     * complete, onServiceDisconnected() should be called.
     */
    void doUnbindService() {
        if (mIsBoundService) {
            // Detach our existing connection.
            unbindService(this);
            mIsBoundService = false;
            Log.d(TAG, "Unbinding.");
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
        final float duckVolume = 0.2f;

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer.isPlaying()) {
                    Log.d(TAG, "Regaining after ducked transient loss.");
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                } else if (!mPreparingStreaming) {
                    Log.d(TAG, "Coming out of transient loss.");
                    mMediaPlayer.start();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                MPDControl.run(MPDControl.ACTION_PAUSE);
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

    @Override
    public final void onCreate() {
        Log.d(TAG, "StreamingService.onCreate()");
        super.onCreate();

        doBindService();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        final StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        sApp.oMPDAsyncHelper.addStatusChangeListener(this);
        sApp.addConnectionLock(this);

        mIsPlaying = MPDStatus.MPD_STATE_PLAYING.equals(getState());
    }

    @Override
    public final void onDestroy() {
        Log.d(TAG, "StreamingService.onDestroy()");
        super.onDestroy();

        stopControlHandlers();

        /** Remove the current MPD listeners */
        sApp.oMPDAsyncHelper.removeStatusChangeListener(this);

        windDownResources(REQUEST_NOTIFICATION_STOP);

        doUnbindService();

        sApp.removeConnectionLock(this);
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
        final int maxError = 4;

        if (mErrorIterator > 0) {
            Log.d(TAG, "Error occurred while streaming, this is try #" + mErrorIterator
                    + ", will attempt up to " + maxError + " times.");
        }

        /** This keeps from continuous errors and battery draining. */
        if (mErrorIterator > maxError) {
            stopSelf();
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
        Log.d(TAG, "StreamingService.onPrepared()");
        final int focusResult = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        /**
         * Not to be playing here is unlikely but it's a race we need to avoid.
         */
        if (mIsPlaying && focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            sendToBoundService(BUFFERING_END);
            mMediaPlayer.start();
        } else {
            /** Because preparingStreaming is still set, this will reset the stream. */
            windDownResources(STREAMING_STOP);
        }

        mPreparingStreaming = false;
        mErrorIterator = 0; /** Reset the error iterator. */
    }


    /**
     * This is used rather than onStartCommand() to begin streaming as we depending on the Binder
     * to send messages to MPDroidService.
     *
     * We use this to connect the Binder to MPDroidService for IPC. This is called when the
     * connection with the service has been established, giving us the service object we can use to
     * interact with the service.  We are communicating with our service through an IDL interface,
     * so get a client-side representation of that from the raw service object.
     *
     * @param className The Class that has connected through the binder.
     */
    @Override
    public void onServiceConnected(final ComponentName className, final IBinder service) {
        mServiceMessenger = new Messenger(service);
        Log.d(TAG, "Attached.");
        tryToStream();
    }

    /**
     * Called when the binder to the MPDroidService is disconnected.
     *
     * @param className The Class that has connected through the binder.
     */
    @Override
    public void onServiceDisconnected(final ComponentName className) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        mServiceMessenger = null;
        Log.d(TAG, "Disconnected.");
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
        mFinalSong = mpdStatus != null && mpdStatus.getNextSongPos() == -1;
    }

    @Override
    public void randomChanged(final boolean random) {
    }

    @Override
    public void repeatChanged(final boolean repeating) {
    }

    /**
     * Send a simple message to our bound service.
     *
     * @param message The simple message to send to the bound service.
     */
    private void sendToBoundService(final int message) {
        final Message msg = Message.obtain(null, message);
        try {
            mServiceMessenger.send(msg);
        } catch (final RemoteException e) {
            Log.e(TAG, "Failed to communicate with bound service.", e);
            mIsBoundService = false;
        }
    }

    private void setupServiceControlHandlers() {
        if (!mServiceControlHandlersActive) {
            Log.d(TAG, "Setting up control handlers");
            final long stopIdleDelay = 600000L; /** 10 minutes */
            /**
             * Stop handler so we don't annoy the user when they forget to turn streamingMode off.
             */
            final Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, stopIdleDelay);
            mServiceControlHandlersActive = true;
        }
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
                    mIsPlaying = true;
                    tryToStream();
                    break;
                case MPDStatus.MPD_STATE_STOPPED:
                    /** Detect final song and let onCompletion handle it */
                    if (mFinalSong || mpdStatus.getPlaylistLength() == 0) {
                        break;
                    }
                    /** Fall Through */
                case MPDStatus.MPD_STATE_PAUSED:
                    /**
                     * If in the middle of stream preparation, "Buffering…" notification message
                     * is likely.
                     */
                    if (mPreparingStreaming) {
                        windDownResources(BUFFERING_ERROR);
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

    private void stopControlHandlers() {
        if (mServiceControlHandlersActive) {
            Log.d(TAG, "Removing control handlers");
            mDelayedStopHandler.removeCallbacksAndMessages(null);
            mServiceControlHandlersActive = false;
        }
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
    }

    /**
     * If streaming mode is activated this will setup the Android mediaPlayer
     * framework, register the media button events, register the remote control
     * client then setup and the framework streaming.
     */
    private void tryToStream() {
        if (mPreparingStreaming && !mActiveBufferingError) {
            Log.d(TAG, "A stream is already being prepared.");
        } else if (!mIsPlaying) {
            Log.d(TAG, "MPD is not currently playing, can't stream.");
        } else {
            beginStreaming();
        }
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
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
                if (BUFFERING_ERROR == action) {
                    mActiveBufferingError = true;
                }
                mDelayedPlayHandler.removeCallbacksAndMessages(null);

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
        Log.d(TAG, "Winding up resources.");

        mActiveBufferingError = true;

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
    }
}
