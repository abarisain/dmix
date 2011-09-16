package com.namelessdev.mpdroid.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.StreamingService;

public class SimpleWidgetProvider extends AppWidgetProvider {
    static final String TAG = "MPDroidSimpleWidgetProvider";

    private static SimpleWidgetProvider sInstance;
    static synchronized SimpleWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new SimpleWidgetProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	Log.v(TAG, "Enter onUpdate");
    	
        // Initialize given widgets to default state, where we launch Music on default click and hide actions if service not running.
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple);
        views.setViewVisibility(R.id.title, View.GONE);
        views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));
        linkButtons(context, views, false /* not playing */);
        
        pushUpdate(context, appWidgetIds, views);

        // Send broadcast intent to any running StreamingService so it can wrap around with an immediate update.
        Intent updateIntent = new Intent(StreamingService.CMD_REMOTE);
        updateIntent.putExtra(StreamingService.CMD_COMMAND, StreamingService.CMD_UPDATE_WIDGET);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }
    
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link StreamingService}
     */
    void notifyChange(StreamingService service, String what) {
        if (hasInstances(service)) {
            //if (StreamingService.META_CHANGED.equals(what) || StreamingService.PLAYSTATE_CHANGED.equals(what)) {
            //    performUpdate(service, null);
            //}
        }
    }

    /**
     * Update all active widget instances by pushing changes
     */
	void performUpdate(StreamingService service, int[] appWidgetIds) {
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.widget_simple);

        CharSequence titleName = "artist"; // service.getTrackName();
        CharSequence artistName = "title"; //service.getArtistName();
        CharSequence errorState = null;

        // Format title string with track number, or error message (when implemented)
        errorState = null;

        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.title, View.GONE);
            views.setTextViewText(R.id.artist, errorState);
        } else {
            // No error, so show normal titles
            views.setViewVisibility(R.id.title, View.VISIBLE);
            views.setTextViewText(R.id.title, titleName);
            views.setTextViewText(R.id.artist, artistName);
        }

        // Set correct drawable for pause state
        final boolean playing = false; //service.isPlaying();
        views.setImageViewResource(R.id.control_play, playing ? R.drawable.ic_appwidget_music_pause : R.drawable.ic_appwidget_music_play);

        // Link actions buttons to intents
        linkButtons(service, views, playing);

        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     *
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivity},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        /*final ComponentName serviceName = new ComponentName(context, StreamingService.class);
        if (playerActive) {
            intent = new Intent(context, MediaPlaybackActivity.class);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        } else {
            intent = new Intent(context, MusicBrowserActivity.class);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        }*/

        Intent i = new Intent(context, StreamingService.class);
        i.setAction(StreamingService.CMD_REMOTE);
        i.putExtra(StreamingService.CMD_COMMAND, StreamingService.CMD_PLAYPAUSE);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        views.setOnClickPendingIntent(R.id.control_play, pi);

        Intent i2 = new Intent(context, StreamingService.class);
        i2.setAction(StreamingService.CMD_REMOTE);
        i2.putExtra(StreamingService.CMD_COMMAND, StreamingService.CMD_NEXT);
        PendingIntent pi2 = PendingIntent.getService(context, 0, i2, 0);
        views.setOnClickPendingIntent(R.id.control_next, pi2);
    }}
