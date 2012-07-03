package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

public class SongsFragment extends BrowseFragment {

	private List<Music> dispMusic = null;
	String album = "";
	String artist = "";
	private boolean isSortedByTrack;

	public SongsFragment() {
		super(R.string.addSong, R.string.songAdded, MPD.MPD_SEARCH_TITLE);
		items = new ArrayList<String>();
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// load preferences for album Track Sort tag display option
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		isSortedByTrack = settings.getBoolean("albumTrackSort", true);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingSongs;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(getListView());
		album = (String) this.getActivity().getIntent().getStringExtra("album");
		artist = (String) this.getActivity().getIntent().getStringExtra("artist");
		UpdateList();

		setActivityTitle(album, R.drawable.ic_tab_albums_selected);

	}

	@Override
	protected void Add(String item) {
		Add(items.indexOf(item));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Add(position);
	}

	protected void Add(int index) {
		Music music = dispMusic.get(index);
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();

			app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
			Tools.notifyUser(String.format(getResources().getString(R.string.songAdded, music.getTitle()), music.getName()),
					getActivity());
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			dispMusic = new ArrayList<Music>(app.oMPDAsyncHelper.oMPD.find(MPD.MPD_FIND_ALBUM, album));
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
		
		//remove tracks for a different artist than the one we are looking for
		if (artist != null) {
			List<Music> tmpDispMusic = new ArrayList<Music>();
			for (Music music : dispMusic) {
				if (String.CASE_INSENSITIVE_ORDER.compare(music.getArtist(), artist) != 0) {
					tmpDispMusic.add(music);
				}
			}
			dispMusic.removeAll(tmpDispMusic);
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
