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
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.namelessdev.mpdroid.models.MusicParcelable;

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
    final static String TAG = "MusicService";

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_UPDATE_INFO = "com.namelessdev.mpdroid.MusicService.UPDATE_INFO";
    public static final String ACTION_TOGGLE_PLAYBACK = StreamingService.CMD_PLAYPAUSE;
    public static final String ACTION_PLAY = StreamingService.CMD_PLAY;
    public static final String ACTION_PAUSE = StreamingService.CMD_PAUSE;
    public static final String ACTION_STOP = StreamingService.CMD_STOP;
    public static final String ACTION_SKIP = StreamingService.CMD_NEXT;
    public static final String ACTION_REWIND = StreamingService.CMD_PREV;

    /**
     * Extra information passed to the intent bundle: the currently playing {@link org.a0z.mpd.Music}
     */
    public static final String EXTRA_CURRENT_MUSIC = "com.namelessdev.mpdroid.MusicService.CurrentMusic";

    Music mCurrentMusic = null, mPreviousMusic = null;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;
    private int mPreviousState = -1;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClient mRemoteControlClient;

    // The component name of MusicIntentReceiver, for use with media button and remote control APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;
    Notification mNotification = null;

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating service");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Create the Audio Focus Helper
        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);

        // Use the media button APIs (if available) to register ourselves for media button events
        mMediaButtonReceiverComponent = new ComponentName(this, RemoteControlReceiver.class);
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        Log.d(TAG, "received command, action=" + action + " from intent: " + intent);

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
        } else if (action.equals(ACTION_UPDATE_INFO)) {
            processUpdateInfo((MusicParcelable) intent.getParcelableExtra(EXTRA_CURRENT_MUSIC));
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

        updatePlayingInfo(RemoteControlClient.PLAYSTATE_PAUSED);
    }

    void processUpdateInfo(MusicParcelable music) {
        Log.d(TAG, "parcelable=" + music + " mCurrentMusic=" + mCurrentMusic);
        if (mCurrentMusic != null && mCurrentMusic.equals((Music) music)) {
            return;
        }
        mCurrentMusic = music;
        updatePlayingInfo(StreamingService.PLAYSTATE_PLAYING);
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
        Log.d(TAG, "update playing info: state=" + state + " (previous state: " + mPreviousState + "), music=" + mCurrentMusic + " (previous music: " + mPreviousMusic + ")");
        boolean updateNotification = false;

        // Create the remote control client
        if (mRemoteControlClient == null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(mMediaButtonReceiverComponent);
            mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(this /*context*/, 0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
            mRemoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            mAudioManager.registerRemoteControlClient(mRemoteControlClient);
        }

        if (state != mPreviousState || mCurrentMusic == null) {
            updateNotification = true;
        }

        //TODO: load this from a background thread
        if (mCurrentMusic == null) {
            final MPDApplication app = (MPDApplication) getApplication();
            if (app != null) {
                try {
                    final MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
                    final int songPos = status.getSongPos();
                    if (songPos >= 0) {
                        mCurrentMusic = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
                    }
                } catch (MPDServerException e) {
                    Log.w("MusicService", "MPDServerException playing next song: " + e.getMessage());
                }
            }
        }

        if (mCurrentMusic != null) {
            // Update the remote controls
            mRemoteControlClient.editMetadata(true) //
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, mCurrentMusic.getArtist()) //
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, mCurrentMusic.getAlbum()) //
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, mCurrentMusic.getTitle()) //
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, mCurrentMusic.getTime()) //
                            // TODO: fetch real item artwork
                            //.putBitmap(RemoteControlClient.MetadataEditorCompat.METADATA_KEY_ARTWORK, mDummyAlbumArt) //
                    .apply();
        }

        if (mPreviousMusic != null && mCurrentMusic != null && !mPreviousMusic.equals(mCurrentMusic)) {
            updateNotification = true;
        }

        if (state == RemoteControlClient.PLAYSTATE_STOPPED) {
            if (mNotificationManager != null) {
                mNotificationManager.cancel(NOTIFICATION_ID);
            }
            relaxResources();
            giveUpAudioFocus();
            stopSelf();
        } else /*if (updateNotification) */ {
            // Update notification & lockscreen widget
            updateNotification(state);
            mRemoteControlClient.setPlaybackState(state);
        }

        mPreviousMusic = mCurrentMusic;
        mPreviousState = state;
    }

    /**
     * Updates the notification.
     */
    void updateNotification(int state) {
        Log.d(TAG, "update notification: " + mCurrentMusic.getArtist() + " - " + mCurrentMusic.getTitle() + ", state: " + state);

        // Build a virtual task stack
        final Intent musicPlayerActivity = new Intent(getApplicationContext(), MainMenuActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainMenuActivity.class);
        stackBuilder.addNextIntent(musicPlayerActivity);

        // Prepare the notification
        final RemoteViews contentView;
        if (mNotification == null || mNotification.contentView == null) {
            contentView = new RemoteViews(getPackageName(), R.layout.notification);
        } else {
            contentView = mNotification.contentView;
        }
        contentView.setTextViewText(R.id.notificationTitle, mCurrentMusic.getTitle());
        contentView.setTextViewText(R.id.notificationArtist, mCurrentMusic.getArtist());

        // Build notification actions
        final Intent playPause = new Intent(this, MusicService.class);
        playPause.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
        final Intent next = new Intent(this, MusicService.class);
        next.setAction(MusicService.ACTION_SKIP);
        final Intent quit = new Intent(this, MusicService.class);
        quit.setAction(MusicService.ACTION_STOP);

        contentView.setOnClickPendingIntent(R.id.notificationPlayPause, PendingIntent.getService(this, 0, playPause, 0));
        contentView.setOnClickPendingIntent(R.id.notificationNext, PendingIntent.getService(this, 0, next, 0));
        contentView.setOnClickPendingIntent(R.id.notificationClose, PendingIntent.getService(this, 0, quit, 0));
        PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set notification play/pause icon state
        contentView.setImageViewResource(R.id.notificationPlayPause, state == RemoteControlClient.PLAYSTATE_PLAYING ? R.drawable.ic_media_pause : R.drawable.ic_media_play);

        // Finish the notification
        if (mNotification == null) {
            final Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.icon_bw);
            builder.setContentIntent(pi);
            builder.setContent(contentView);
            mNotification = builder.build();
        }

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
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
