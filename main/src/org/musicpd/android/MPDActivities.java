package org.musicpd.android;

import android.os.Bundle;
import android.text.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockListActivity;
import org.musicpd.android.tools.Log;

public class MPDActivities {

	// Forbid this activity from being instanciated
	private MPDActivities() {
	}

	public static class MPDFragmentActivity extends SherlockFragmentActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			if (TextUtils.isEmpty(Log.tag))
				Log.tag = getResources().getString(R.string.app_name);
			super.onCreate(arg0);
			final MPDApplication app = (MPDApplication) getApplication();
			if ((this instanceof MainMenuActivity && app.isLightNowPlayingThemeSelected()) ||
					(!(this instanceof MainMenuActivity) && app.isLightThemeSelected())) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

	public static class MPDActivity extends SherlockActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			if (TextUtils.isEmpty(Log.tag))
				Log.tag = getResources().getString(R.string.app_name);
			super.onCreate(arg0);
			if (((MPDApplication) getApplication()).isLightThemeSelected()) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

	public static class MPDListActivity extends SherlockListActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			if (TextUtils.isEmpty(Log.tag))
				Log.tag = getResources().getString(R.string.app_name);
			super.onCreate(arg0);
			if (((MPDApplication) getApplication()).isLightThemeSelected()) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

}
