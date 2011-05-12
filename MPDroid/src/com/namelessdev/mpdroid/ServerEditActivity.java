package com.namelessdev.mpdroid;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.namelessdev.mpdroid.providers.ServerList;

public class ServerEditActivity extends Activity {
	public static final String SERVER_ID = "server";

	private int server_id = -1;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.server_edit);

		Button delete = (Button) findViewById(R.id.deleteServer);
		if (delete != null) {
			delete.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
		}

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			server_id = extras.getInt(SERVER_ID, -1);
		}
	}

	public void btnSaveCallback(View target) {
		ContentValues val = new ContentValues();
		EditText tmp = (EditText) findViewById(R.id.edit_host);
		String tmpStr = null;

		tmpStr = ((EditText) findViewById(R.id.edit_host)).getText().toString();
		if (!TextUtils.isEmpty(tmpStr)) {
			val.put(ServerList.ServerColumns.HOST, tmp.getText().toString());
		}

		tmpStr = ((EditText) findViewById(R.id.edit_name)).getText().toString();
		if (!TextUtils.isEmpty(tmpStr)) {
			val.put(ServerList.ServerColumns.NAME, tmp.getText().toString());
		}

		tmpStr = ((EditText) findViewById(R.id.edit_password)).getText().toString();
		if (!TextUtils.isEmpty(tmpStr)) {
			val.put(ServerList.ServerColumns.PASSWORD, tmp.getText().toString());
		}

		tmpStr = ((EditText) findViewById(R.id.edit_port)).getText().toString();
		if (!TextUtils.isEmpty(tmpStr)) {
			val.put(ServerList.ServerColumns.PORT, tmp.getText().toString());
		}

		tmpStr = ((EditText) findViewById(R.id.edit_streamURL)).getText().toString();
		if (!TextUtils.isEmpty(tmpStr)) {
			val.put(ServerList.ServerColumns.STREAMING_URL, tmp.getText().toString());
		}

		tmpStr = ((EditText) findViewById(R.id.edit_streamingPort)).getText().toString();
		if (!TextUtils.isEmpty(tmpStr)) {
			val.put(ServerList.ServerColumns.STREAMING_PORT, tmp.getText().toString());
		}

		if (server_id == -1) {
			getContentResolver().insert(ServerList.ServerColumns.CONTENT_URI, val);
		} else {
			getContentResolver().update(Uri.parse(ServerList.ServerColumns.CONTENT_URI + "/" + server_id), val, null, null);
		}
	}

	public void btnCancelCallback(View target) {
		// Do stuff
	}

}
