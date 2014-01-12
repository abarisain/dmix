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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

public class WarningActivity extends Activity {
    Activity myWarning;

    @Override
    public void onBackPressed() {

        // eat the event, do nothing
        return;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        myWarning = this;
        setContentView(R.layout.warning);
        Button btnOK = (Button) findViewById(R.id.buttonOK);
        btnOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(myWarning);
                settings.edit().putBoolean("newWarningShown", true).commit();
                finish();
            }
        });

    }
}
