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

package com.namelessdev.mpdroid.preferences;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This Activity is used to modify connection preferences.
 */
public class ConnectionSettings extends PreferenceActivity {

    /**
     * This is the modifier fragment which will be called from the {@link ConnectionChooser} class
     * upon preference choice.
     */
    public static final String FRAGMENT_MODIFIER_NAME
            = "com.namelessdev.mpdroid.preferences.ConnectionModifier";

    public static final int MAIN = 0;

    /**
     * The class log identifier.
     */
    private static final String TAG = "ConnectionSettings";

    /**
     * This method checks to see if preferences have been setup for this application previously.
     *
     * @return True if preferences haven't been setup for this application, false otherwise.
     */
    private boolean hasEmptyPreferences() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        return preferences.getString(ConnectionModifier.KEY_HOSTNAME, "").isEmpty();
    }

    /**
     * Subclasses should override this method and verify that the given fragment is a valid type
     * to be attached to this activity. The default implementation returns {@code true} for
     * apps built for {@code android:targetSdkVersion} older than
     * {@link Build.VERSION_CODES#KITKAT}. For later versions, it will throw an exception.
     *
     * @param fragmentName the class name of the Fragment about to be attached to this activity.
     * @return true if the fragment class name is valid for this Activity and false otherwise.
     */
    @Override
    protected boolean isValidFragment(final String fragmentName) {
        return FRAGMENT_MODIFIER_NAME.equals(fragmentName);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasEmptyPreferences()) {
            // Initialize the default preferences.
            PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, false);

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.warningText1);
            builder.setPositiveButton(R.string.ok, Tools.NOOP_CLICK_LISTENER);

            builder.show();
        }

        final Fragment fragment = Fragment.instantiate(this, ConnectionChooser.class.getName());
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final boolean result;

        if (item.getItemId() == MAIN) {
            final Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            result = true;
        } else {
            result = super.onOptionsItemSelected(item);
        }

        return result;
    }

    /**
     * This is the callback for preference choices which have set the
     * {@link Preference#setFragment(String)} method.
     *
     * <p>No need to limit the code down to expected callers, {@link #isValidFragment(String)}
     * would have to be modified to allow unexpected callers to this method.</p>
     *
     * @param caller The calling fragment.
     * @param pref   The Preference clicked.
     * @return True if {@link #FRAGMENT_MODIFIER_NAME} has been set as the fragment, false
     * otherwise.
     */
    @Override
    public boolean onPreferenceStartFragment(final PreferenceFragment caller,
            final Preference pref) {
        final Fragment fragment = Fragment.instantiate(this, pref.getFragment(), pref.getExtras());
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.addToBackStack(caller.getClass().getName());
        transaction.replace(android.R.id.content, fragment).commit();

        return true;
    }

    @Override
    public void setTheme(final int resid) {
        if (MPDApplication.getInstance().isLightThemeSelected()) {
            super.setTheme(R.style.AppTheme_Light);
        } else {
            super.setTheme(R.style.AppTheme);
        }
    }
}
