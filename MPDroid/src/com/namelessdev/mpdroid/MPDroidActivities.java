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
import android.app.ListActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

public class MPDroidActivities {

    public static class MPDroidActivity extends Activity {

        @Override
        protected void onCreate(Bundle arg0) {
            super.onCreate(arg0);
            applyTheme(this, (MPDApplication) getApplication());
        }
    }

    public static class MPDroidFragmentActivity extends FragmentActivity {

        @Override
        protected void onCreate(Bundle arg0) {
            super.onCreate(arg0);
            applyTheme(this, (MPDApplication) getApplication());
        }
    }

    public static class MPDroidListActivity extends ListActivity {

        @Override
        protected void onCreate(Bundle arg0) {
            super.onCreate(arg0);
            applyTheme(this, (MPDApplication) getApplication());
        }
    }

    private static void applyTheme(Activity activity, MPDApplication app) {
        final boolean lightTheme = app.isLightThemeSelected();
        int themeID = R.style.AppTheme;
        if (activity instanceof MainMenuActivity
                && PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(
                        "smallSeekbars", true)) {
            if (lightTheme) {
                themeID = R.style.AppTheme_Light_SmallSeekBars;
            } else {
                themeID = R.style.AppTheme_SmallSeekBars;
            }
        } else if (lightTheme) {
            themeID = R.style.AppTheme_Light;
        }
        activity.setTheme(themeID);
    }

    // Forbid this activity from being instanciated
    private MPDroidActivities() {
    }

}
