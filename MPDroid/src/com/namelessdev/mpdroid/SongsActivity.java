package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.Music;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SongsActivity extends BrowseActivity {

	private List<Music> dispMusic = null;
	String album = "";
	private boolean isSortedByTrack;

	public SongsActivity() {
		super(R.string.addSong, R.string.songAdded, MPD.MPD_SEARCH_TITLE);
		items = new ArrayList<String>();
	}

	@Override
	public void onCreate(Bundle icicle) {
		album = (String) this.getIntent().getStringExtra("album");
		super.onCreate(icicle);

		// load preferences for album Track Sort tag display option
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		isSortedByTrack = settings.getBoolean("albumTrackSort", false);

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
			MainMenuActivity.notifyUser(String.format(getResources().getString(R.string.songAdded, music.getTitle()), music.getName()),
					this);
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
		if (isSortedByTrack) {
			// sort by track number
			Collections.sort(dispMusic, new TrackComparator());
			for (Music music : dispMusic) {
				items.add(music.getTitle());
			}
		} else {
			Collections.sort(dispMusic, new MusicComparator());
			for (Music music : dispMusic) {
				items.add(music.getTitle());
			}
		}
	}

	private static class MusicComparator implements Comparator<Music> {

		@Override
		public int compare(Music music1, Music music2) {
			String title1 = music1.getTitle();
			String title2 = music2.getTitle();
			if (title1 == null) {
				title1 = "";
			}
			if (title2 == null) {
				title2 = "";
			}
			// Compare the two titles
			return String.CASE_INSENSITIVE_ORDER.compare(title1, title2);
		}
	}

	private static class TrackComparator implements Comparator<Music> {

		@Override
		public int compare(Music music1, Music music2) {
			String track1 = music1.getTrack();
			String track2 = music2.getTrack();
			if (track1 == null) {
				track1 = "";
			}
			if (track2 == null) {
				track2 = "";
			}
			// Compare the two track numbers
			final Integer iTrack1 = Integer.parseInt(track1.replaceAll("[\\D]", ""));
			final Integer iTrack2 = Integer.parseInt(track2.replaceAll("[\\D]", ""));
			

			return iTrack1.compareTo(iTrack2);
		}
	}
}
