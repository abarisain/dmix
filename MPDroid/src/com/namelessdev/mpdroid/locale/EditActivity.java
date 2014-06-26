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

    public static final String BUNDLE_ACTION_STRING = "ACTION_STRING";

    public static final String BUNDLE_ACTION_EXTRA = "ACTION_EXTRA";

    @SuppressWarnings("unused")
    public static final String BUNDLE_ACTION_LABEL = "ACTION_LABEL";

    private List<ActionItem> items;

    /**
     * Class for listview population
     */
    private class ActionItem {

        public String actionString;

        public String label;

        public ActionItem(String _actionString, String _label) {
            actionString = _actionString;
            label = _label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locale_edit);
        setResult(RESULT_CANCELED);

        final ListView list = (ListView) findViewById(R.id.listView);
        items = new ArrayList<>();
        items.add(new ActionItem(MPDControl.ACTION_TOGGLE_PLAYBACK,
                getString(R.string.togglePlayback)));
        items.add(new ActionItem(MPDControl.ACTION_PLAY, getString(R.string.play)));
        items.add(new ActionItem(MPDControl.ACTION_PAUSE, getString(R.string.pause)));
        items.add(new ActionItem(MPDControl.ACTION_STOP, getString(R.string.stop)));
        items.add(new ActionItem(MPDControl.ACTION_SEEK, getString(R.string.rewind)));
        items.add(
                new ActionItem(MPDControl.ACTION_PREVIOUS, getString(R.string.previous)));
        items.add(new ActionItem(MPDControl.ACTION_NEXT, getString(R.string.next)));
        items.add(new ActionItem(MPDControl.ACTION_MUTE,
                getString(R.string.mute)));
        items.add(new ActionItem(MPDControl.ACTION_VOLUME_SET,
                getString(R.string.setVolume)));
        items.add(new ActionItem(StreamHandler.ACTION_START,
                getString(R.string.startStreaming)));
        items.add(new ActionItem(StreamHandler.ACTION_STOP,
                getString(R.string.stopStreaming)));
        items.add(new ActionItem(NotificationHandler.ACTION_START,
                getString(R.string.showNotification)));
        items.add(new ActionItem(NotificationHandler.ACTION_STOP,
                getString(R.string.closeNotification)));

        final ArrayAdapter<ActionItem> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, items);

        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final ActionItem item = items.get(position);
        if (item.actionString.equals(MPDControl.ACTION_VOLUME_SET)) {
            final SeekBar seekBar = new SeekBar(this);
            final int padding = getResources()
                    .getDimensionPixelSize(R.dimen.locale_edit_seekbar_padding);
            seekBar.setPadding(padding, padding, padding, padding);
            seekBar.setMax(MPDCommand.MAX_VOLUME);
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setView(seekBar);
            alert.setTitle(item.label);
            alert.setNegativeButton(R.string.cancel, null);
            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    final String progress = Integer.toString(seekBar.getProgress());
                    finishWithAction(item, progress, item.label + " : " + progress);
                }
            });
            alert.show();
        } else {
            finishWithAction(item, null, null);
        }
    }

    private void finishWithAction(ActionItem action, String extra, String overrideLabel) {
        final Intent i = new Intent();
        final Bundle b = new Bundle();
        b.putString(BUNDLE_ACTION_STRING, action.actionString);
        if (extra != null) {
            b.putString(BUNDLE_ACTION_EXTRA, extra);
        }
        i.putExtra(LocaleConstants.EXTRA_BUNDLE, b);
        i.putExtra(LocaleConstants.EXTRA_STRING_BLURB,
                overrideLabel == null ? action.label : overrideLabel);
        setResult(RESULT_OK, i);
        finish();
    }
}
