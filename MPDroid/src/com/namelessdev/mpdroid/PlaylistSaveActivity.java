package com.namelessdev.mpdroid;

import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class PlaylistSaveActivity extends Activity implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist_save);
		((Button) findViewById(R.id.save)).setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.save:
			try {
				MPDApplication app = (MPDApplication) getApplication();
				app.oMPDAsyncHelper.oMPD.getPlaylist().savePlaylist(((EditText) findViewById(R.id.editPlaylistName)).getText().toString());
			} catch (MPDServerException e) {
			}

			finish();
			break;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

}
