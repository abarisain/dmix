package com.namelessdev.mpdroid;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MPDroidActivities {

	// Forbid this activity from being instanciated
	private MPDroidActivities() {
	}

	public static class MPDroidFragmentActivity extends FragmentActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			super.onCreate(arg0);
			final MPDApplication app = (MPDApplication) getApplication();
			if ((this instanceof MainMenuActivity && app.isLightNowPlayingThemeSelected()) ||
					(!(this instanceof MainMenuActivity) && app.isLightThemeSelected())) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

	public static class MPDroidActivity extends Activity {

		@Override
		protected void onCreate(Bundle arg0) {
			super.onCreate(arg0);
			if (((MPDApplication) getApplication()).isLightThemeSelected()) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

	public static class MPDroidListActivity extends ListActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			super.onCreate(arg0);
			if (((MPDApplication) getApplication()).isLightThemeSelected()) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

}
