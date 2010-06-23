package org.pmix.ui;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.Directory;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.Music;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SongsActivity extends BrowseActivity {

	private List<Music> musics = null;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.artists);
		List<String> items = new ArrayList<String>();

		try {
			MPDApplication app = (MPDApplication)getApplication();
			String album = (String) this.getIntent().getStringExtra("album");
			this.setTitle(album);
			musics = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, album));

			for (Music music : musics) {
				items.add(music.getTitle());
			}


			// Put the list on the screen...
			ListViewButtonAdapter<String> fsentries = new ListViewButtonAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			PlusListener AddListener = new PlusListener() {
				@Override
				public void OnAdd(CharSequence sSelected, int iPosition)
				{
					Music music = musics.get(iPosition);
					try {
						MPDApplication app = (MPDApplication)getApplication();

						app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
						MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.songAdded),sSelected), SongsActivity.this);
					} catch (MPDServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			};
			fsentries.SetPlusListener(AddListener);
			setListAdapter(fsentries);
		} catch (MPDServerException e) {
			e.printStackTrace();
			this.setTitle(e.getMessage());
		}

	}
	
	@Override
	/**
	 * We also listen to item clicks here...
	 */
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Music music = musics.get(position);
		try {
			MPDApplication app = (MPDApplication)getApplication();

			app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.songAdded,music.getTitle()),music.getName()), SongsActivity.this);
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
