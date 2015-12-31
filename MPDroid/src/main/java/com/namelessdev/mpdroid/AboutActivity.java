/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.adapters.SeparatedListDataBinder;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AboutActivity extends Activity {

    private static final String TAG = "AboutActivity";

    /**
     * This wraps all the strings of an array in a {@link AboutListItem}.
     *
     * @param from The object to convert.
     * @param to   The collection to put the converted objects into.
     */
    private static void getAboutItems(final String[] from, final Collection<Object> to) {
        for (final String item : from) {
            to.add(new AboutListItem(item));
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        final ListView listView = (ListView) findViewById(android.R.id.list);

        final LayoutInflater inflater = LayoutInflater.from(this);
        final View headerView = inflater.inflate(R.layout.about_header, listView, false);
        final TextView versionInfo = (TextView) headerView.findViewById(R.id.text_version);
        versionInfo.setText(R.string.version);
        versionInfo.append(": ");
        versionInfo.append(BuildConfig.VERSION_NAME);

        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(headerView);

        final Resources resources = getResources();
        final String[] librariesArray = resources.getStringArray(R.array.libraries_array);
        final String[] authorsArray = resources.getStringArray(R.array.authors_array);
        final List<Object> listItems =
                new ArrayList<>(authorsArray.length + librariesArray.length + 2);

        listItems.add(getString(R.string.about_libraries));
        getAboutItems(librariesArray, listItems);

        listItems.add(getString(R.string.about_authors));
        getAboutItems(authorsArray, listItems);

        listView.setAdapter(new SeparatedListAdapter(this, android.R.layout.simple_list_item_1,
                R.layout.list_separator, new AboutDataBinder(), listItems));
    }

    /**
     * This is the DataBinder to use with the {@link SeparatedListAdapter}.
     */
    private static final class AboutDataBinder implements SeparatedListDataBinder {

        /**
         * Sole constructor.
         */
        private AboutDataBinder() {
            super();
        }

        @Override
        public boolean isEnabled(final int position, final List<?> items, final Object item) {
            return false;
        }

        @Override
        public void onDataBind(final Context context, final View targetView, final List<?> items,
                final Object item, final int position) {
            ((TextView) targetView.findViewById(android.R.id.text1)).setText(item.toString());
        }
    }

    /**
     * This is a wrapper for a String to chane the instanceof for the {@link SeparatedListAdapter}.
     */
    private static final class AboutListItem {

        private final String mText;

        private AboutListItem(final String text) {
            super();
            mText = text;
        }

        @Override
        public String toString() {
            return mText;
        }
    }
}
