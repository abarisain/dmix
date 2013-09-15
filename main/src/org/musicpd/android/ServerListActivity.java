package org.musicpd.android;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ListView;

public class ServerListActivity extends ListActivity {

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.server_list);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}
}