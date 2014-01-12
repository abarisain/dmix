
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
