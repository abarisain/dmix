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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;

/**
 * This is a common {@link AppCompatActivity} class used for subclassing with commonly used values
 * and methods.
 */
public abstract class MPDActivity extends AppCompatActivity {

    /**
     * This is the shared MPDApplication instance.
     */
    protected MPDApplication mApp;

    /**
     * This is the initial theme resource.
     */
    private int mInitialThemeRes;

    /**
     * This method returns the current theme resource ID.
     *
     * @return The current theme resource ID.
     */
    protected int getThemeResId() {
        return R.style.AppTheme_Light;
    }

    /**
     * This method returns if the light theme has been selected in the preferences.
     *
     * @return True if the light theme has been selected, false otherwise.
     */
    public boolean isLightThemeSelected() {
        return Tools.isLightThemeSelected(this);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        mApp = (MPDApplication) getApplicationContext();
        mInitialThemeRes = getThemeResId();

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

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mInitialThemeRes != getThemeResId()) {
            Tools.notifyUser(R.string.activityReload);
            Tools.resetActivity(this);
        }
    }

    /**
     * This method overrides {@link ContextThemeWrapper#setTheme(int)} to use
     * {@link #getThemeResId()}.
     *
     * @param resid The resource ID for the current theme.
     */
    @Override
    public void setTheme(final int resid) {
        super.setTheme(getThemeResId());
    }
}
