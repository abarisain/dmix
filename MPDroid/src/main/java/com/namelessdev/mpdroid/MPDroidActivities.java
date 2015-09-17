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

import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.tools.Tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

public class MPDroidActivities {

    // Forbid this activity from being instanciated
    private MPDroidActivities() {
        super();
    }

    private static void applyTheme(final Activity activity) {
        final boolean lightTheme = MPDApplication.getInstance().isLightThemeSelected();
        int themeID = R.style.AppTheme;
        if (activity instanceof MainMenuActivity) {
            if (lightTheme) {
                themeID = R.style.AppTheme_MainMenu_Light;
            } else {
                themeID = R.style.AppTheme_MainMenu;
            }
        } else if (activity instanceof NowPlayingActivity &&
                PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(
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

    @SuppressLint("Registered")
    public static class MPDroidActivity extends AppCompatActivity {

        protected final MPDApplication mApp = MPDApplication.getInstance();

        @Override
        protected void onCreate(final Bundle savedInstanceState) {
            applyTheme(this);
            super.onCreate(savedInstanceState);
        }

        @Override
        public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
            boolean result = true;

            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    Tools.runCommand(MPDControl.ACTION_NEXT);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Tools.runCommand(MPDControl.ACTION_PREVIOUS);
                    break;
                default:
                    result = super.onKeyLongPress(keyCode, event);
                    break;
            }
            return result;
        }

        @Override
        public boolean onKeyUp(final int keyCode, @NonNull final KeyEvent event) {
            boolean result = true;

            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (event.isTracking() && !event.isCanceled() && !mApp.isLocalAudible()) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                            Tools.runCommand(MPDControl.ACTION_VOLUME_STEP_UP);
                        } else {
                            Tools.runCommand(MPDControl.ACTION_VOLUME_STEP_DOWN);
                        }
                    }
                    break;
                default:
                    result = super.onKeyUp(keyCode, event);
                    break;
            }

            return result;
        }
    }

}
