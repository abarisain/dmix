/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

package com.namelessdev.mpdroid.library;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.adapters.SeparatedListDataBinder;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;
import com.namelessdev.mpdroid.views.TouchInterceptor;

import java.util.ArrayList;
import java.util.List;

public class LibraryTabsSettings extends PreferenceActivity {

    private SeparatedListAdapter adapter;
    private ArrayList<Object> tabList;

    public TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {

        public void drop(int from, int to) {
            if (from == to) {
                return;
            }
            Object item = tabList.get(from);
            tabList.remove(from);
            tabList.add(to, item);
            if (getVisibleTabs().size() == 0) {
                // at least one tab should be visible so revert the changes
                tabList.remove(to);
                tabList.add(from, item);
            } else {
                saveSettings();
                adapter.notifyDataSetChanged();
            }
        }
    };

    private ArrayList<String> getVisibleTabs() {
        ArrayList<String> visibleTabs = new ArrayList<String>();
        // item 0 is a separator so we start with 1
        for (int i = 1; i < tabList.size(); i++) {
            // if the item is a separator break
            if (tabList.get(i) instanceof String) {
                break;
            }
            // if item is a TabItem add it to the list
            if (tabList.get(i) instanceof TabItem) {
                visibleTabs.add(((TabItem) tabList.get(i)).text);
            }
        }
        return visibleTabs;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs_settings);

        // get a list of all tabs
        ArrayList<String> allTabs = LibraryTabsUtil.getAllLibraryTabs();

        // get a list of all currently visible tabs
        ArrayList<String> currentTabs = LibraryTabsUtil
                .getCurrentLibraryTabs(this.getApplicationContext());

        // create a list of all currently hidden tabs
        ArrayList<String> hiddenTabs = new ArrayList<String>();
        for (String tab : allTabs) {
            // add all items not in currentTabs
            if (!currentTabs.contains(tab)) {
                hiddenTabs.add(tab);
            }
        }

        tabList = new ArrayList<Object>();
        // add a separator
        tabList.add(getString(R.string.visibleTabs));
        // add all visible tabs
        for (int i = 0; i < currentTabs.size(); i++) {
            tabList.add(new TabItem(currentTabs.get(i)));
        }
        // add a separator
        tabList.add(getString(R.string.hiddenTabs));
        // add all hidden tabs
        for (int i = 0; i < hiddenTabs.size(); i++) {
            tabList.add(new TabItem(hiddenTabs.get(i)));
        }
        adapter = new SeparatedListAdapter(this,
                R.layout.library_tabs_settings_item, new TabListDataBinder(),
                tabList);

        setListAdapter(adapter);
        ListView mList;
        mList = getListView();
        ((TouchInterceptor) mList).setDropListener(mDropListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        MPDApplication app = (MPDApplication) getApplicationContext();
        app.setActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MPDApplication app = (MPDApplication) getApplicationContext();
        app.unsetActivity(this);
    }

    private void saveSettings() {
        LibraryTabsUtil.saveCurrentLibraryTabs(this.getApplicationContext(),
                getVisibleTabs());
    }

}

class TabItem {
    String text;

    TabItem(String text) {
        this.text = text;
    }
}

class TabListDataBinder implements SeparatedListDataBinder {

    public boolean isEnabled(int position, List<? extends Object> items, Object item) {
        return true;
    }

    public void onDataBind(Context context, View targetView,
            List<? extends Object> items, Object item, int position) {
        final TextView text1 = (TextView) targetView.findViewById(R.id.text1);
        text1.setText(LibraryTabsUtil.getTabTitleResId(((TabItem) item).text));
    }

}
