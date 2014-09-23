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

import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.MPDControl;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class SimpleWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "MPDroidSimpleWidgetProvider";

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
    private boolean hasInstances(final Context context) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                getClass()));
        return appWidgetIds.length > 0;
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     */
    protected void linkButtons(final Context context, final RemoteViews views) {
        Intent intent;
        PendingIntent pendingIntent;

        // text button to start full app
        intent = new Intent(context, MainMenuActivity.class);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_app, pendingIntent);

        // prev button
        intent = new Intent(context, WidgetHelperService.class);
        intent.setAction(MPDControl.ACTION_PREVIOUS);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);

        // play/pause button
        intent = new Intent(context, WidgetHelperService.class);
        intent.setAction(MPDControl.ACTION_TOGGLE_PLAYBACK);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        // next button
        intent = new Intent(context, WidgetHelperService.class);
        intent.setAction(MPDControl.ACTION_NEXT);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
    }

    /**
     * Handle a change notification coming over from
     * {@link android.media.RemoteControlClient}
     */
    void notifyChange(final WidgetHelperService service) {
        if (hasInstances(service)) {
            performUpdate(service);
        }
    }

    public void onUpdate(final RemoteViews views, final Context context,
            final AppWidgetManager appWidgetManager) {
        Log.v(TAG, "Enter onUpdate");

        // Initialise given widgets to default state, where we launch MPDroid on
        // default click and hide actions if service not running.
        linkButtons(context, views);
        pushUpdate(context, views);

        // Start service intent to WidgetHelperService so it can wrap around
        // with an immediate update
        final Intent updateIntent = new Intent(context, WidgetHelperService.class);
        updateIntent.setAction(WidgetHelperService.CMD_UPDATE_WIDGET);
        context.startService(updateIntent);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
            final int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple);

        onUpdate(views, context, appWidgetManager);
    }

    protected void performUpdate(final RemoteViews views, final WidgetHelperService service) {
        // Set correct drawable for pause state
        if (service.isPlaying()) {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }

        // Link actions buttons to intents
        linkButtons(service, views);
        pushUpdate(service, views);
    }

    /**
     * Update all active widget instances by pushing changes
     */
    protected void performUpdate(final WidgetHelperService service) {
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.widget_simple);

        performUpdate(views, service);
    }

    /**
     * Set the RemoteViews to use for all AppWidget instances
     */
    protected void pushUpdate(final Context context, final RemoteViews views) {
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(new ComponentName(context, getClass()), views);
    }
}
