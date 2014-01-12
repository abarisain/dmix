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
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {

    public static String getVersionName(Context context, Class<Activity> cls) {
        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager()
                    .getPackageInfo(comp.getPackageName(), 0);
            return pinfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.about);

        TextView versionInfo = (TextView) findViewById(R.id.versionText);
        versionInfo.setText(getResources().getString(R.string.version) + ": "
                + getVersionName(this, Activity.class));

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

}
