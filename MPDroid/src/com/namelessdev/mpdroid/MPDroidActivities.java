package com.namelessdev.mpdroid;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class MPDroidActivities {

	// Forbid this activity from being instanciated
	private MPDroidActivities() {
	}

	public static class MPDroidFragmentActivity extends SherlockFragmentActivity {

		@Override
		protected void onCreate(Bundle arg0) {
			super.onCreate(arg0);
			if(MPDApplication.isWhiteThemeSelected()) {
				setTheme(R.style.AppTheme_Light);
			}
		}
	}

}
