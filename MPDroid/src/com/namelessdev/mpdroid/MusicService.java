/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

/**
 * Service that handles media playback. This is the Service through which we perform all the media
 * handling in our application. Upon initialization, it waits for Intents (which come from our main activity,
 * {@link MainMenuActivity}, which signal the service to perform specific operations: Play, Pause,
 * Rewind, Skip, etc.
 */
public class MusicService extends Service implements MusicFocusable {
    // The tag we put on debug messages
    final static String TAG = "RandomMusicPlayer";

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_TOGGLE_PLAYBACK = StreamingService.CMD_PLAYPAUSE;
    public static final String ACTION_PLAY = StreamingService.CMD_PLAY;
    public static final String ACTION_PAUSE = StreamingService.CMD_PAUSE;
    public static final String ACTION_STOP = StreamingService.CMD_STOP;
    public static final String ACTION_SKIP = StreamingService.CMD_NEXT;
    public static final String ACTION_REWIND = StreamingService.CMD_PREV;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // title of the song we are currently playing
    String mSongTitle = "";

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClient mRemoteControlClient;

    // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;

    // The component name of MusicIntentReceiver, for use with media button and remote control APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;

    Notification mNotification = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "debug: Creating service");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.no_cover_art_light);

        mMediaButtonReceiverComponent = new ComponentName(this, RemoteControlReceiver.class);
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action == null) {
            return START_NOT_STICKY;
        } else if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
            processTogglePlaybackRequest();
        } else if (action.equals(ACTION_PLAY)) {
            processPlayRequest();
        } else if (action.equals(ACTION_PAUSE)) {
            processPauseRequest();
        } else if (action.equals(ACTION_SKIP)) {
            processSkipRequest();
        } else if (action.equals(ACTION_STOP)) {
            processStopRequest();
        } else if (action.equals(ACTION_REWIND)) {
            processRewindRequest();
        }

        return START_NOT_STICKY; // Means we started the service, but don't want it to restart in case it's killed.
    }

    void processTogglePlaybackRequest() {
        new AsyncTask<MPDApplication, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(MPDApplication... params) {
                String state = null;
                try {
                    state = params[0].oMPDAsyncHelper.oMPD.getStatus().getState();
                } catch (MPDServerException e) {
                    Log.w(MPDApplication.TAG, e.getMessage());
                }
                return MPDStatus.MPD_STATE_PLAYING.equals(state) || MPDStatus.MPD_STATE_PAUSED.equals(state);
            }

            @Override
            protected void onPostExecute(Boolean shouldPause) {
                if (shouldPause) {
                    processPauseRequest();
                } else {
                    processPlayRequest();
                }
            }
        }.execute((MPDApplication) getApplication());
    }

    void processPlayRequest() {
        tryToGetAudioFocus();

        new AsyncTask<MPDApplication, Void, String>() {
            @Override
            protected String doInBackground(MPDApplication... params) {
                try {
                    final MPD mpd = params[0].oMPDAsyncHelper.oMPD;
                    String state = mpd.getStatus().getState();
                    if (!MPDStatus.MPD_STATE_PLAYING.equals(state)) {
                        mpd.play();
                    }
                } catch (MPDServerException e) {
                    Log.w(MPDApplication.TAG, e.getMessage());
                }
                return null;
            }
        }.execute((MPDApplication) getApplication());

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClient != null) {
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
        updatePlayingInfo(RemoteControlClient.PLAYSTATE_PLAYING);
    }

    void processPauseRequest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final MPDApplication app = (MPDApplication) getApplication();
                    if (app != null) {
                        app.oMPDAsyncHelper.oMPD.pause();
                    }
                } catch (MPDServerException e) {
                    Log.w(MPDApplication.TAG, e.getMessage());
                }
            }
        }).start();
        relaxResources(); // while paused, we always retain the MediaPlayer do not give up audio focus

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClient != null) {
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
        updatePlayingInfo(RemoteControlClient.PLAYSTATE_PAUSED);
    }

    void processRewindRequest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final MPDApplication app = (MPDApplication) getApplication();
                    if (app != null) {
                        app.oMPDAsyncHelper.oMPD.seek(0);
                    }
                } catch (MPDServerException e) {
                    Log.w(MPDApplication.TAG, e.getMessage());
                }
            }
        }).start();
        updatePlayingInfo(RemoteControlClient.PLAYSTATE_REWINDING);
    }

    void processSkipRequest() {
        tryToGetAudioFocus();

        final MPDApplication app = (MPDApplication) getApplication();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (app != null) {
                        app.oMPDAsyncHelper.oMPD.next();
                    }
                } catch (MPDServerException e) {
                    Log.w(MPDApplication.TAG, e.getMessage());
                }
            }
        }).start();
        updatePlayingInfo(RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS);
    }

    void processStopRequest() {
        final MPDApplication app = (MPDApplication) getApplication();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (app != null) {
                        app.oMPDAsyncHelper.oMPD.stop();
                    }
                } catch (MPDServerException e) {
                    Log.w(MPDApplication.TAG, e.getMessage());
                }
            }
        }).start();

        // let go of all resources...
        relaxResources();
        giveUpAudioFocus();

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClient != null) {
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        }

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     */
    void relaxResources() {
        stopForeground(true);
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    void updatePlayingInfo(int state) {
        relaxResources(); // release everything except MediaPlayer

        try {
            Music music = null;
            final MPDApplication app = (MPDApplication) getApplication();
            if (app != null) {
                if (app.getApplicationState().streamingMode) {
                    Intent i = new Intent(app, StreamingService.class);
                    i.setAction("com.namelessdev.mpdroid.RESET_STREAMING");
                    startService(i);
                }

                final MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
                final int songPos = status.getSongPos();
                mSongTitle = "";
                if (songPos >= 0) {
                    music = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
                    mSongTitle = music.getTitle();
                }
            }

            setUpAsForeground(mSongTitle);

            // Use the media button APIs (if available) to register ourselves for media button events
            MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager, mMediaButtonReceiverComponent);

            // Use the remote control APIs (if available) to set the playback state
            if (mRemoteControlClient == null) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                intent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(this /*context*/, 0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
                mAudioManager.registerRemoteControlClient(mRemoteControlClient);
            }

            mRemoteControlClient.setPlaybackState(state);
            mRemoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            // Update the remote controls
            final String artist = music == null ? "" : music.getArtist();
            final String album = music == null ? "" : music.getAlbum();
            final String title = music == null ? "" : music.getTitle();
            final long duration = music == null ? 0 : music.getTime();
            mRemoteControlClient.editMetadata(true) //
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist) //
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album) //
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title) //
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration) //
                            // TODO: fetch real item artwork
                            //.putBitmap(RemoteControlClient.MetadataEditorCompat.METADATA_KEY_ARTWORK, mDummyAlbumArt) //
                    .apply();

            updateNotification(artist, title);
        } catch (MPDServerException e) {
            Log.w("MusicService", "MPDServerException playing next song: " + e.getMessage());
        }
    }

    /**
     * Updates the notification.
     */
    void updateNotification(String artist, String text) {
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(getApplicationContext(), MainMenuActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.setLatestEventInfo(getApplicationContext(), artist, text, pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(getApplicationContext(), MainMenuActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.icon_bw;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), "RandomMusicPlayer", text, pi);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    public void onGainedAudioFocus() {
        Toast.makeText(this, "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;
    }

    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(this, "lost audio focus." + (canDuck ? "can duck" : "no duck"), Toast.LENGTH_SHORT).show();
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        relaxResources();
        giveUpAudioFocus();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
