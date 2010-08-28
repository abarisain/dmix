package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.Music;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SongsActivity extends BrowseActivity {

	private List<Music> dispMusic = null;
	String album = "";

	public SongsActivity() {
		super(R.string.addSong, R.string.songAdded, MPD.MPD_SEARCH_TITLE);
		items = new ArrayList<String>();
	}

	@Override
	public void onCreate(Bundle icicle) {
		album = (String) this.getIntent().getStringExtra("album");
		super.onCreate(icicle);

		setContentView(R.layout.artists);

		this.setTitle(album);
		findViewById(R.id.header).setVisibility(View.VISIBLE);
		TextView title = (TextView) findViewById(R.id.headerText);
		title.setText(this.getTitle());
		ImageView icon = (ImageView) findViewById(R.id.headerIcon);
		icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_tab_albums_selected));
		pd = ProgressDialog.show(this, getResources().getString(R.string.loading), getResources().getString(R.string.loadingSongs));

		registerForContextMenu(getListView());
		UpdateList();
	}

	@Override
	protected void Add(String item) {
		Add(items.indexOf(item));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Add(position);
	}

	protected void Add(int index) {
		Music music = dispMusic.get(index);
		try {
			MPDApplication app = (MPDApplication) getApplication();

			app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.songAdded, music.getTitle()), music.getName()), this);
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getApplication();
			dispMusic = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, album));
		} catch (MPDServerException e) {
		}
		Collections.sort(dispMusic, new MusicComparator());
		for (Music music : dispMusic) {
			items.add(music.getTitle());
		}
	}

	private static class MusicComparator implements Comparator<Music> {

		public int compare(Music music1, Music music2) {
			String title1 = music1.getTitle();
			String title2 = music2.getTitle();
			// Compare the two titles
			return String.CASE_INSENSITIVE_ORDER.compare(title1, title2);
		}
	}
}
