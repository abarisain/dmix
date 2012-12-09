package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SongDataBinder;

public class SongsFragment extends BrowseFragment {

	Album album = null;
	Artist artist = null;
	TextView headerArtist;
	TextView headerInfo;

	public SongsFragment() {
		super(R.string.addSong, R.string.songAdded, MPDCommand.MPD_SEARCH_TITLE);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingSongs;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = super.onCreateView(inflater, container, savedInstanceState);
		final View headerView = inflater.inflate(R.layout.song_header, null, false);
		headerArtist = (TextView) headerView.findViewById(R.id.tracks_artist);
		headerInfo = (TextView) headerView.findViewById(R.id.tracks_info);
		((TextView) headerView.findViewById(R.id.separator_title)).setText(R.string.songs);
		list.addHeaderView(headerView, null, false);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(getListView());
		artist = getActivity().getIntent().getParcelableExtra("artist");
		album = getActivity().getIntent().getParcelableExtra("album");
		UpdateList();

		setActivityTitle(album.getName());

	}

	@Override
	public void onListItemClick(final ListView l, View v, final int position, long id) {
		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		app.oMPDAsyncHelper.execAsync(new Runnable() {
			@Override
			public void run() {
				Add((Item) l.getAdapter().getItem(position));
			}
		});
	}

	@Override
	protected void Add(Item item) {
		Music music = (Music)item;
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
    protected void Add(Item item, String playlist) {
    	try {
    		MPDApplication app = (MPDApplication) getActivity().getApplication();
    		ArrayList<Music> songs = new ArrayList<Music> ();
    		songs.add((Music)item);
    		app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, songs);
    		Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
    	} catch (MPDServerException e) {
    		e.printStackTrace();
    	}
	}
   
	@Override
	public void asyncUpdate() {
		try {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			items = app.oMPDAsyncHelper.oMPD.getSongs(artist, album);
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void updateFromItems() {
		super.updateFromItems();
		if(items != null) {
			Music song;
			String lastArtist = null;
			for(Item item : items) {
				song = (Music) item;
				if(lastArtist == null) {
					lastArtist = song.getArtist();
					continue;
				}
			}
			if(lastArtist == null) {
				for(Item item : items) {
					song = (Music) item;
					if(lastArtist == null) {
						lastArtist = song.getArtist();
						continue;
					}
				}
			}
			headerArtist.setText(getArtistForTrackList());
			headerInfo.setText(getHeaderInfoString());
		}
		
	}
	
	@Override
	protected ListAdapter getCustomListAdapter() {
		if(items != null) {
			Music song;
			boolean differentArtists = false;
			String lastArtist = null;
			for(Item item : items) {
				song = (Music) item;
				if(lastArtist == null) {
					lastArtist = song.getArtist();
					continue;
				}
				if(!lastArtist.equalsIgnoreCase(song.getArtist())) {
					differentArtists = true;
					break;
				}
			}
			return new ArrayIndexerAdapter(getActivity(), new SongDataBinder(differentArtists), items);
		}
		return super.getCustomListAdapter();
	}
	
	private String getArtistForTrackList() {
		Music song;
		String lastArtist = null;
		boolean differentArtists = false;
		for(Item item : items) {
			song = (Music) item;
			if(lastArtist == null) {
				lastArtist = song.getAlbumArtist();
				continue;
			}
			if(!lastArtist.equalsIgnoreCase(song.getAlbumArtist())) {
				differentArtists = true;
				break;
			}
		}
		if(differentArtists || lastArtist == null || lastArtist.equals("")) {
			differentArtists = false;
			for(Item item : items) {
				song = (Music) item;
				if(lastArtist == null) {
					lastArtist = song.getArtist();
					continue;
				}
				if(!lastArtist.equalsIgnoreCase(song.getArtist())) {
					differentArtists = true;
					break;
				}
			}
			if(differentArtists || lastArtist == null || lastArtist.equals("")) {
				return getString(R.string.variousArtists);
			}
			return lastArtist;
		}
		return lastArtist;
	}
	
	private String getTotalTimeForTrackList() {
		Music song;
		long totalTime = 0;
		for(Item item : items) {
			song = (Music) item;
			if(song.getTime() > 0)
				totalTime += song.getTime();
		}
		return Music.timeToString(totalTime);
	}
	
	private String getHeaderInfoString() {
		final int count = items.size();
		return String.format(getString(count > 1 ? R.string.tracksInfoHeaderPlural : R.string.tracksInfoHeader), count, getTotalTimeForTrackList());
	}
}
