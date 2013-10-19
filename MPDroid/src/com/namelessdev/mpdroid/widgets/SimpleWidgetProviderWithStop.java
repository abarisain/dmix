package com.namelessdev.mpdroid.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;

public class SimpleWidgetProviderWithStop extends AppWidgetProvider {
    static final String TAG = "MPDroidSimpleWidgetProvider";

    private static SimpleWidgetProviderWithStop sInstance;
    static synchronized SimpleWidgetProviderWithStop getInstance() {
        if (sInstance == null) {
            sInstance = new SimpleWidgetProviderWithStop();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	Log.v(TAG, "Enter onUpdate");

        // Initialise given widgets to default state, where we launch MPDroid on default click and hide actions if service not running.
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple_with_stop);
        linkButtons(context, views);
        pushUpdate(context, appWidgetIds, views);

        // Start service intent to WidgetHelperService so it can wrap around with an immediate update
        Intent updateIntent = new Intent(context, WidgetHelperService.class);
        updateIntent.setAction(WidgetHelperService.CMD_UPDATE_WIDGET);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.startService(updateIntent);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     */
    private void linkButtons(Context context, RemoteViews views) {
    	Intent intent;
    	PendingIntent pendingIntent;

    	// text button to start full app
    	intent = new Intent(context, MainMenuActivity.class);
    	pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
    	views.setOnClickPendingIntent(R.id.control_app, pendingIntent);

    	// prev button
    	intent = new Intent(context, WidgetHelperService.class);
    	intent.setAction(WidgetHelperService.CMD_PREV);
    	pendingIntent = PendingIntent.getService(context, 0, intent, 0);
    	views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);

    	// play/pause button
        intent = new Intent(context, WidgetHelperService.class);
        intent.setAction(WidgetHelperService.CMD_PLAYPAUSE);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        // next button
        intent = new Intent(context, WidgetHelperService.class);
        intent.setAction(WidgetHelperService.CMD_NEXT);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);

        // stop button
        intent = new Intent(context, WidgetHelperService.class);
        intent.setAction(WidgetHelperService.CMD_STOP);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_stop, pendingIntent);
    }

    /**
     * Set the RemoteViews to use for all AppWidget instances
     */
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
     * Check against {@link android.appwidget.AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }
    
    /**
     * Handle a change notification coming over from {@link MediaPlaybackService}
     */
    void notifyChange(WidgetHelperService service) {
        if (hasInstances(service))
                performUpdate(service, null);
    }

	/**
	 * Update all active widget instances by pushing changes
	 */
	void performUpdate(WidgetHelperService service, int[] appWidgetIds) {
		final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.widget_simple_with_stop);
		
        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }
        
        // Link actions buttons to intents
        linkButtons(service, views);
        pushUpdate(service, appWidgetIds, views);
	}
}
