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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;

public class MPDroidActivities {

    // Forbid this activity from being instanciated
    private MPDroidActivities() {
        super();
    }

    private static void applyTheme(final Activity activity) {
        final boolean lightTheme = MPDApplication.getInstance().isLightThemeSelected();
        int themeID = R.style.AppTheme;
        if (activity instanceof MainMenuActivity) {
            if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(
                    "smallSeekbars", true)) {
                if (lightTheme) {
                    themeID = R.style.AppTheme_MainMenu_Light_SmallSeekBars;
                } else {
                    themeID = R.style.AppTheme_MainMenu_SmallSeekBars;
                }
            } else {
                if (lightTheme) {
                    themeID = R.style.AppTheme_MainMenu_Light;
                } else {
                    themeID = R.style.AppTheme_MainMenu;
                }
            }
        } else if (lightTheme) {
            themeID = R.style.AppTheme_Light;
        }
        activity.setTheme(themeID);
    }

    @SuppressLint("Registered")
    public static class MPDroidActivity extends ActionBarActivity {

        protected final MPDApplication mApp = MPDApplication.getInstance();

        @Override
        protected void onCreate(final Bundle savedInstanceState) {
            applyTheme(this);
            super.onCreate(savedInstanceState);
        }
    }

}
