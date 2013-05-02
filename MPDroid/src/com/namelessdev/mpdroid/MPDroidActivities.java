package com.namelessdev.mpdroid;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockListActivity;

public class MPDroidActivities {

	// Forbid this activity from being instanciated
	private MPDroidActivities() {
	}

	public static class MPDroidFragmentActivity extends SherlockFragmentActivity {

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

	public static class MPDroidActivity extends SherlockActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			super.onCreate(arg0);
			if (((MPDApplication) getApplication()).isLightThemeSelected()) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

	public static class MPDroidListActivity extends SherlockListActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			super.onCreate(arg0);
			if (((MPDApplication) getApplication()).isLightThemeSelected()) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

}
