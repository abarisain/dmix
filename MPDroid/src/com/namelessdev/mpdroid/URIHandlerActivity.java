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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.namelessdev.mpdroid.fragments.StreamsFragment;

public class URIHandlerActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urihandler);
        if (!getIntent().getAction().equals("android.intent.action.VIEW")) {
            finish();
        }
        final StreamsFragment sf = (StreamsFragment) getSupportFragmentManager().findFragmentById(
                R.id.streamsFragment);
        sf.addEdit(-1, getIntent().getDataString());
    }

}
