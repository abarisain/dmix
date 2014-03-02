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

public class SimpleWidgetProvider extends AppWidgetProvider {
    static final String TAG = "MPDroidSimpleWidgetProvider";

    private static SimpleWidgetProvider sInstance;

    static synchronized SimpleWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new SimpleWidgetProvider();
        }
        return sInstance;
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this
     * widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this
                .getClass()));
        return (appWidgetIds.length > 0);
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
    }

    /**
     * Handle a change notification coming over from
     * {@link MediaPlaybackService}
     */
    void notifyChange(WidgetHelperService service) {
        if (hasInstances(service))
            performUpdate(service, null);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(TAG, "Enter onUpdate");

        // Initialise given widgets to default state, where we launch MPDroid on
        // default click and hide actions if service not running.
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple);
        linkButtons(context, views);
        pushUpdate(context, appWidgetIds, views);

        // Start service intent to WidgetHelperService so it can wrap around
        // with an immediate update
        Intent updateIntent = new Intent(context, WidgetHelperService.class);
        updateIntent.setAction(WidgetHelperService.CMD_UPDATE_WIDGET);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.startService(updateIntent);
    }

    /**
     * Update all active widget instances by pushing changes
     */
    void performUpdate(WidgetHelperService service, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.widget_simple);

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

    /**
     * Set the RemoteViews to use for all AppWidget instances
     */
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to
        // all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }
}
