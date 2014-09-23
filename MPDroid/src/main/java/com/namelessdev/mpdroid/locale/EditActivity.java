/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.locale;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.service.NotificationHandler;
import com.namelessdev.mpdroid.service.StreamHandler;

import org.a0z.mpd.MPDCommand;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

public class EditActivity extends Activity implements AdapterView.OnItemClickListener {

    public static final String BUNDLE_ACTION_EXTRA = "ACTION_EXTRA";

    @SuppressWarnings("unused")
    public static final String BUNDLE_ACTION_LABEL = "ACTION_LABEL";

    public static final String BUNDLE_ACTION_STRING = "ACTION_STRING";

    private List<ActionItem> mItems;

    private void finishWithAction(ActionItem action, String extra, String overrideLabel) {
        final Intent i = new Intent();
        final Bundle b = new Bundle();
        b.putString(BUNDLE_ACTION_STRING, action.mActionString);
        if (extra != null) {
            b.putString(BUNDLE_ACTION_EXTRA, extra);
        }
        i.putExtra(LocaleConstants.EXTRA_BUNDLE, b);
        i.putExtra(LocaleConstants.EXTRA_STRING_BLURB,
                overrideLabel == null ? action.mLabel : overrideLabel);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locale_edit);
        setResult(RESULT_CANCELED);

        final ListView list = (ListView) findViewById(R.id.listView);
        mItems = new ArrayList<>();
        mItems.add(new ActionItem(MPDControl.ACTION_TOGGLE_PLAYBACK,
                getString(R.string.togglePlayback)));
        mItems.add(new ActionItem(MPDControl.ACTION_PLAY, getString(R.string.play)));
        mItems.add(new ActionItem(MPDControl.ACTION_PAUSE, getString(R.string.pause)));
        mItems.add(new ActionItem(MPDControl.ACTION_STOP, getString(R.string.stop)));
        mItems.add(new ActionItem(MPDControl.ACTION_SEEK, getString(R.string.rewind)));
        mItems.add(
                new ActionItem(MPDControl.ACTION_PREVIOUS, getString(R.string.previous)));
        mItems.add(new ActionItem(MPDControl.ACTION_NEXT, getString(R.string.next)));
        mItems.add(new ActionItem(MPDControl.ACTION_MUTE,
                getString(R.string.mute)));
        mItems.add(new ActionItem(MPDControl.ACTION_VOLUME_SET,
                getString(R.string.setVolume)));
        mItems.add(new ActionItem(StreamHandler.ACTION_START,
                getString(R.string.startStreaming)));
        mItems.add(new ActionItem(StreamHandler.ACTION_STOP,
                getString(R.string.stopStreaming)));
        mItems.add(new ActionItem(NotificationHandler.ACTION_START,
                getString(R.string.showNotification)));
        mItems.add(new ActionItem(NotificationHandler.ACTION_STOP,
                getString(R.string.closeNotification)));

        final ArrayAdapter<ActionItem> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mItems);

        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final ActionItem item = mItems.get(position);
        if (item.mActionString.equals(MPDControl.ACTION_VOLUME_SET)) {
            final SeekBar seekBar = new SeekBar(this);
            final int padding = getResources()
                    .getDimensionPixelSize(R.dimen.locale_edit_seekbar_padding);
            seekBar.setPadding(padding, padding, padding, padding);
            seekBar.setMax(MPDCommand.MAX_VOLUME);
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setView(seekBar);
            alert.setTitle(item.mLabel);
            alert.setNegativeButton(R.string.cancel, null);
            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    final String progress = Integer.toString(seekBar.getProgress());
                    finishWithAction(item, progress, item.mLabel + " : " + progress);
                }
            });
            alert.show();
        } else {
            finishWithAction(item, null, null);
        }
    }

    /**
     * Class for listview population
     */
    private static class ActionItem {

        private String mActionString;

        private String mLabel;

        private ActionItem(String actionString, String label) {
            mActionString = actionString;
            mLabel = label;
        }

        @Override
        public String toString() {
            return mLabel;
        }
    }
}
