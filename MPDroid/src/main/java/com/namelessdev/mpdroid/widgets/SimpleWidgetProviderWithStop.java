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

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.MPDControl;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class SimpleWidgetProviderWithStop extends SimpleWidgetProvider {

    protected static final String TAG = "MPDroidSimpleWidgetProviderWithStop";

    /**
     * Link up various button actions using {@link PendingIntent}.
     */
    @Override
    protected void linkButtons(final Context context, final RemoteViews views) {
        final Intent intent;
        final PendingIntent pendingIntent;

        super.linkButtons(context, views);

        // stop button
        intent = new Intent(context, WidgetHelperService.class);
        intent.setAction(MPDControl.ACTION_STOP);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_stop, pendingIntent);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
            final int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_simple_with_stop);

        onUpdate(views, context, appWidgetManager);
    }

    /**
     * Update all active widget instances by pushing changes
     */
    @Override
    protected void performUpdate(final WidgetHelperService service) {
        final RemoteViews views = new RemoteViews(service.getPackageName(),
                R.layout.widget_simple_with_stop);

        performUpdate(views, service);
    }
}
