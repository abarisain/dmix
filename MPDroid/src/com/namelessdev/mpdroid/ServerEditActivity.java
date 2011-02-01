package com.namelessdev.mpdroid;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.Button;

public class ServerEditActivity extends Activity {
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.server_edit);

		Button delete = (Button) findViewById(R.id.deleteServer);
		if (delete != null) {
			delete.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
		}
	}
}
