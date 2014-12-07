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

import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.adapters.SeparatedListDataBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AboutActivity extends ActionBarActivity {

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final String TAG = "AboutActivity";

    public static String getVersionName(final Class<Activity> cls) {
        String versionName = null;

        try {
            final ComponentName comp = new ComponentName(APP, cls);
            final PackageInfo packageInfo = APP.getPackageManager()
                    .getPackageInfo(comp.getPackageName(), 0);
            versionName = packageInfo.versionName + " (" + packageInfo.versionCode + ')';
        } catch (final PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get the version name.", e);
        }

        return versionName;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        final ListView listView = (ListView) findViewById(android.R.id.list);

        final LayoutInflater inflater = LayoutInflater.from(this);
        final View headerView = inflater.inflate(R.layout.about_header, null, false);
        final TextView versionInfo = (TextView) headerView.findViewById(R.id.text_version);
        versionInfo.setText(R.string.version);
        versionInfo.append(": " + getVersionName(Activity.class));

        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(headerView);

        String[] tmpStringArray;
        final List<Object> listItems = new ArrayList<>();
        listItems.add(getString(R.string.about_libraries));
        tmpStringArray = getResources().getStringArray(R.array.libraries_array);
        for (final String tmpString : tmpStringArray) {
            listItems.add(new AboutListItem(tmpString));
        }
        listItems.add(getString(R.string.about_authors));
        tmpStringArray = getResources().getStringArray(R.array.authors_array);
        for (final String tmpString : tmpStringArray) {
            listItems.add(new AboutListItem(tmpString));
        }

        listView.setAdapter(new SeparatedListAdapter(this, android.R.layout.simple_list_item_1,
                R.layout.list_separator, new SeparatedListDataBinder() {
            @Override
            public boolean isEnabled(final int position, final List<?> items, final Object item) {
                return false;
            }

            @Override
            public void onDataBind(final Context context, final View targetView,
                    final List<?> items,
                    final Object item, final int position) {
                ((TextView) targetView.findViewById(android.R.id.text1)).setText(item.toString());
            }
        }, listItems));
    }

    private static class AboutListItem {

        private final String mText;

        public AboutListItem(final String text) {
            super();
            mText = text;
        }

        @Override
        public String toString() {
            return mText;
        }
    }

}
