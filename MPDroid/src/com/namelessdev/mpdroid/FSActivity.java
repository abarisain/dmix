package com.namelessdev.mpdroid;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class FSActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		if (!MPDApplication.isHoneycombOrBetter()) {
			setTheme(android.R.style.Theme_Black_NoTitleBar);
		}

		super.onCreate(arg0);
		setContentView(R.layout.fs_activity);
	}
}