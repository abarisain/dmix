package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.tools.Tools;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SongsActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		if (!Tools.isHoneycombOrBetter()) {
			setTheme(android.R.style.Theme_Black_NoTitleBar);
		}
			
		super.onCreate(arg0);
		setContentView(R.layout.songs_activity);
	}
}