package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.R.id;
import com.namelessdev.mpdroid.R.layout;
import com.namelessdev.mpdroid.R.string;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;

public class SongSearchMessage extends Activity {
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.song_search_message);
	}
}
