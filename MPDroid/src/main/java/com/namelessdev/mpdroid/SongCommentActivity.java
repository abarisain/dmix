/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;

public class SongCommentActivity extends MPDroidActivities.MPDroidActivity {

    public static final String COMMENT_KEY = "comment";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_comment);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final String comment = getIntent().getStringExtra(COMMENT_KEY);

        if (TextUtils.isEmpty(comment)) {
            finish();
        } else {
            ((TextView) findViewById(R.id.comments)).setText(comment);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean handled = true;

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                handled = false;
                break;
        }

        return handled;
    }
}
