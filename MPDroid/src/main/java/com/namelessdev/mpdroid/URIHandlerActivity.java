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

import com.namelessdev.mpdroid.fragments.StreamsFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class URIHandlerActivity extends FragmentActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urihandler);

        final Intent intent = getIntent();
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final StreamsFragment streamsFragment =
                (StreamsFragment) fragmentManager.findFragmentById(R.id.streamsFragment);

        if (!Intent.ACTION_VIEW.equals(intent.getAction())) {
            finish();
        }

        streamsFragment.addEdit(-1, intent.getDataString());
    }

}
