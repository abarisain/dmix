package com.namelessdev.mpdroid;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.namelessdev.mpdroid.providers.ServerList;

public class ServerListActivity extends ListActivity {
	private static final String[] PROJECTION = new String[] { ServerList.ServerColumns._ID, ServerList.ServerColumns.NAME,
			ServerList.ServerColumns.HOST };

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.server_list);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		Cursor cursor = managedQuery(ServerList.ServerColumns.CONTENT_URI, // Use the default content URI for the provider.
				PROJECTION, // Return the note ID and title for each note.
				null, // No where clause, return all records.
				null, // No where clause, therefore no where column values.
				ServerList.ServerColumns.DEFAULT_SORT_ORDER // Use the default sort order.
		);

		// Creates the backing adapter for the ListView.
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, // The Context for the ListView
				android.R.layout.simple_list_item_single_choice, // Points to the XML for a list item
				cursor, // The cursor to get items from
				new String[] { ServerList.ServerColumns.NAME }, new int[] { android.R.id.text1 });
		getListView().setAdapter(adapter);
	}
}