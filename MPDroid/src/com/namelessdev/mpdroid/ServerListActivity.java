package com.namelessdev.mpdroid;

import android.app.ListActivity;
import android.os.Bundle;

public class ServerListActivity extends ListActivity {
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.server_list);
	}
}