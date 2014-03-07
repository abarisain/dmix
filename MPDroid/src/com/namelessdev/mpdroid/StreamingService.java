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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.ConnectionListener;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * StreamingService hooks Android's audio framework to MPD's streaming server to
 * allow local audio playback, audio metadata parsing and cover art retrieving.
 * 
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id: $
 */

public class StreamingService extends Service implements StatusChangeListener, OnPreparedListener,
        OnCompletionListener,
        OnBufferingUpdateListener, OnErrorListener, OnInfoListener, ConnectionListener,
        OnAudioFocusChangeListener {

    static final String TAG = "MPDroidStreamingService";

    public static final int STREAMINGSERVICE_STATUS = 1;
    public static final int STREAMINGSERVICE_PAUSED = 2;
    public static final int STREAMINGSERVICE_STOPPED = 3;
    public static final String CMD_REMOTE = "com.namelessdev.mpdroid.REMOTE_COMMAND";
    public static final String CMD_COMMAND = "COMMAND";
    public static final String CMD_PAUSE = "PAUSE";
    public static final String CMD_STOP = "STOP";
    public static final String CMD_PLAY = "PLAY";
    public static final String CMD_PLAYPAUSE = "PLAYPAUSE";
    public static final String CMD_PREV = "PREV";
    public static final String CMD_NEXT = "NEXT";
    public static final String CMD_DIE = "DIE"; // Just in case
    public static boolean isServiceRunning = false;

    /**
     * Get the status of the streaming service.
     * 
     * @return bool
     */
    public static Boolean getStreamingServiceStatus() {
        return isServiceRunning;
    }

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private ComponentName remoteControlResponder;

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

    /**
     * Field containing the ID used to stopSelfResult() which will stop the
     * streaming service.
     */
    private Integer lastStartID;
    /** Methods to enable and disable MPDroid to control media buttons. */
    private static Method registerMediaButtonEventReceiver;

    private static Method unregisterMediaButtonEventReceiver;

    /** Field to control remoteControlClient */
    private RemoteControlClient remoteControlClient = null;

    /**
     * How long to wait before queuing the message into the current handler
     * queue.
     */
    private static final int IDLE_DELAY = 60000;

    /**
     * Initializes registerMediaButtonReceiver and
     * unregisterMediaButtonEventReceiver as is required before the
     * RemoteControlClient can be registered through
     * {@link #registerRemoteControlClient()}.
     */
    private static void initializeRemoteControlRegistrationMethods() {
        try {
            if (registerMediaButtonEventReceiver == null) {
                registerMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "registerMediaButtonEventReceiver",
                        new Class[] {
                            ComponentName.class
                        });
            }
            if (unregisterMediaButtonEventReceiver == null) {
                unregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "unregisterMediaButtonEventReceiver",
                        new Class[] {
                            ComponentName.class
                        });
            }
        } catch (NoSuchMethodException nsme) {
            /* Aww we're not 2.2 */
        }
    }

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
            if (app == null)
                return;

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
                if (!isPlaying)
                    return;
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
     * If streaming mode is activated this will setup the Android mediaPlayer
     * framework, register the media button events, register the remote control
     * client then setup and the framework streaming.
     */
    public void beginStreaming() {
        // just to be sure, we do not want to start when we're not supposed to
        if (!((MPDApplication) getApplication()).getApplicationState().streamingMode)
            return;

        isPaused = false;
        buffering = true;

        registerMediaButtonEvent();
        registerRemoteControlClient();

        if (mediaPlayer == null)
            return;

        try {
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(streamSource);
            mediaPlayer.prepareAsync();
            showNotification(true);
        } catch (IOException e) {
            /**
             * TODO: Notify the user
             */
            buffering = false; // Obviously if it failed we are not buffering.
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
        ((MPDApplication) getApplication()).getApplicationState().streamingMode = false;
        stopSelfResult(lastStartID);
    }

    /**
     * This block sets up the cover art bitmap for the lock screen. TODO: Check
     * the sdcard cover cache for this song. TODO: Try to find a more efficient
     * method to accomplish this task.
     */
    private Bitmap getCoverArtBitmap(AlbumInfo albumInfo,
            Notification.Builder notificationBuilder) {
        MPDApplication app = (MPDApplication) getApplication();

        final CachedCover cache = new CachedCover(app);
        String[] coverArtPath = null;

        try {
            coverArtPath = cache.getCoverUrl(albumInfo);
        } catch (Exception e) {
            // TODO: Properly handle exception for getStatus() failure.
        }

        if (coverArtPath != null && coverArtPath.length > 0 && coverArtPath[0] != null) {
            notificationBuilder.setLargeIcon(Tools.decodeSampledBitmapFromPath(coverArtPath[0],
                    getResources()
                            .getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                    getResources()
                            .getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                    true));

            return (Tools.decodeSampledBitmapFromPath(coverArtPath[0],
                    (int) Tools.convertDpToPixel(200, this),
                    (int) Tools.convertDpToPixel(200, this), false));
        }
        return null;
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

    /** Beyond here are stubs. */

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
        buffering = true;
        prevMpdState = "";
        isPlaying = true;
        isPaused = false;
        lastStartID = 0;

        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);

        remoteControlResponder = new ComponentName(getPackageName(),
                RemoteControlReceiver.class.getName());

        initializeRemoteControlRegistrationMethods();

        registerMediaButtonEvent();
        registerRemoteControlClient();

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
        setMusicState(RemoteControlClient.PLAYSTATE_STOPPED);
        unregisterMediaButtonEvent();
        unregisterRemoteControlClient();

        if (audioManager != null)
            audioManager.abandonAudioFocus(this);

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
        buffering = false;
        isPlaying = true;
        prevMpdState = "";
        showNotification();
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
        if (!isPlaying)
            return;

        isPlaying = false;
        isPaused = true;
        buffering = false;

        /** If the Android media framework crashes, try to stop it earlier. */
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        showNotification(true);
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

    /**
     * Register the media button event receiver intents, which is a requirement
     * before registering the {@link #registerRemoteControlClient()}.
     */
    private void registerMediaButtonEvent() {
        if (registerMediaButtonEventReceiver == null)
            return;

        try {
            registerMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds the media button intent.
     */
    private void registerRemoteControlClient() {
        // build the PendingIntent for the remote control client
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(remoteControlResponder);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                mediaButtonIntent, 0);

        // create and register the remote control client
        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        (remoteControlClient)
                .setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                        | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                        | RemoteControlClient.FLAG_KEY_MEDIA_NEXT);

        audioManager.registerRemoteControlClient(remoteControlClient);
    }

    @Override
    public void repeatChanged(boolean repeating) {
    }

    /**
     * This method will grab the metadata from the stream coming from the MPD
     * server and give it to the remote control client for use on the lock
     * screen.
     */
    private void setMusicInfo(Music song, AlbumInfo albumInfo,
            Notification.Builder notificationBuilder) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getApplication());

        if (remoteControlClient == null || song == null
                || !settings.getBoolean(CoverManager.PREFERENCE_CACHE, true)) {
            return;
        }

        MetadataEditor editor = (remoteControlClient).editMetadata(true);

        Bitmap bitmap = getCoverArtBitmap(albumInfo, notificationBuilder);
        if (bitmap != null) {
            editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap);
        }

        editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, song.getTime() * 1000);
        editor.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, song.getTrack());
        editor.putLong(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER, song.getDisc());
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, song.getAlbum());
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, song.getAlbumArtist());
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, song.getArtist());
        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, song.getTitle());
        editor.apply();
    }

    private void setMusicState(int state) {
        if (remoteControlClient != null) {
            (remoteControlClient).setPlaybackState(state);
        }
    }

    public void showNotification() {
        showNotification(false);
    }

    /**
     * Show a notification (and control with Android 4.1+).
     */
    public void showNotification(boolean streamingStatusChanged) {

        MPDApplication app = (MPDApplication) getApplication();
        MPDStatus statusMpd = null;

        try {
            statusMpd = app.oMPDAsyncHelper.oMPD.getStatus();
        } catch (MPDServerException e) {
            /** TODO: Properly handle exception for getStatus() failure. */
        }

        /**
         * Don't show the notification if MPD is paused and where the SDK allows
         * for notification buttons.
         */
        if (statusMpd == null
                || (isPaused && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)) {
            return;
        }

        String state = statusMpd.getState();
        if (state == null || state.equals(prevMpdState) && !streamingStatusChanged) {
            return;
        }

        int songPos = statusMpd.getSongPos();
        if (songPos < 0) {
            return;
        }

        /** Setup the notification manager. */
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .cancel(STREAMINGSERVICE_PAUSED);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .cancel(STREAMINGSERVICE_STOPPED);
        stopForeground(true);

        /** Setup the notification defaults. */
        Notification status;
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.icon_bw)
                .setOngoing(true)
                .setContentTitle(getString(R.string.streamStopped))
                .setContentIntent(
                        PendingIntent.getActivity(this, 0,
                                new Intent("com.namelessdev.mpdroid.PLAYBACK_VIEWER")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0));

        /** Setup the media player. */
        Music actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
        setMusicInfo(actSong, actSong.getAlbumInfo(), notificationBuilder);
        /**
         * Initialize the text strings for the notification panel.
         */
        CharSequence contentText = actSong.getAlbum();
        CharSequence contentTitle = actSong.getTitle();
        /**
         * There's no guarantee that an Artist or an Album exists.
         */
        if (contentText == null) {
            contentText = actSong.getArtist();
        } else {
            if (actSong.getArtist() != null) {
                contentText = contentText + " - " + actSong.getArtist();
            }
        }
        /**
         * There's no guarantee that a title exists, but filename is guaranteed.
         */
        if (contentTitle == null) {
            contentTitle = actSong.getFilename();
        }
        /**
         * If buffering, the main title will be Buffering... alternate will be
         * the title.
         */
        if (buffering) {
            contentText = contentTitle + " - " + contentText;
            contentTitle = getString(R.string.buffering);
        }
        /**
         * Finally, build the notification.
         */
        notificationBuilder.setContentTitle(contentTitle);
        if (contentText != null) {
            notificationBuilder.setContentText(contentText);
        }
        /**
         * Setup the music state. On Android 4.1+ setup notification control
         * buttons.
         */
        if (buffering) {
            setMusicState(RemoteControlClient.PLAYSTATE_BUFFERING);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notificationBuilder.addAction(R.drawable.ic_media_stop, getString(R.string.stop),
                        PendingIntent.getService(
                                this, 41,
                                new Intent(this, StreamingService.class).setAction(CMD_REMOTE)
                                        .putExtra(CMD_COMMAND, CMD_STOP),
                                PendingIntent.FLAG_CANCEL_CURRENT));
            }
        } else {
            setMusicState(isPaused ? RemoteControlClient.PLAYSTATE_PAUSED : RemoteControlClient.PLAYSTATE_PLAYING);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notificationBuilder.addAction(R.drawable.ic_appwidget_music_prev, "", PendingIntent
                        .getService(this, 11,
                                new Intent(this, StreamingService.class).setAction(CMD_REMOTE)
                                        .putExtra(CMD_COMMAND, CMD_PREV),
                                PendingIntent.FLAG_CANCEL_CURRENT));
                notificationBuilder.addAction(
                        isPaused ? R.drawable.ic_appwidget_music_play
                                : R.drawable.ic_appwidget_music_pause,
                        "",
                        PendingIntent.getService(
                                this,
                                21,
                                new Intent(this,
                                        StreamingService.class).setAction(CMD_REMOTE).putExtra(
                                        CMD_COMMAND, CMD_PLAYPAUSE),
                                PendingIntent.FLAG_CANCEL_CURRENT));
                notificationBuilder.addAction(R.drawable.ic_appwidget_music_next, "", PendingIntent
                        .getService(this, 31,
                                new Intent(this, StreamingService.class).setAction(CMD_REMOTE)
                                        .putExtra(CMD_COMMAND, CMD_NEXT),
                                PendingIntent.FLAG_CANCEL_CURRENT));
            }
        }

        status = notificationBuilder.build();

        startForeground(STREAMINGSERVICE_STATUS, status);
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

    /**
     * Stops the playback after the MPD has been stopped.
     */
    public void stopStreaming() {
        prevMpdState = "";
        if (mediaPlayer == null)
            return;
        mediaPlayer.stop();
        stopForeground(true);
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        prevMpdState = "";
        showNotification();

    }

    /**
     * Unregisters the registered media button event receiver intents.
     */
    private void unregisterMediaButtonEvent() {
        if (unregisterMediaButtonEventReceiver == null)
            return;

        try {
            unregisterMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void unregisterRemoteControlClient() {
        if (remoteControlClient != null) {
            audioManager.unregisterRemoteControlClient(remoteControlClient);
        }
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    }
}
