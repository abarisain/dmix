package com.namelessdev.mpdroid.fragments;

import java.util.ArrayList;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper.CoverDownloadListener;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SongDataBinder;

public class SongsFragment extends BrowseFragment implements CoverDownloadListener {

	private static final int FALLBACK_COVER_SIZE = 80; // In DIP

	Album album = null;
	Artist artist = null;
	TextView headerArtist;
	TextView headerInfo;
	ImageView coverArt;
	ProgressBar coverArtProgress;
	CoverAsyncHelper coverHelper;
	Bitmap coverBitmap;

	public SongsFragment() {
		super(R.string.addSong, R.string.songAdded, MPDCommand.MPD_SEARCH_TITLE);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	public SongsFragment init(Artist ar, Album al) {
		artist = ar;
		album = al;
		return this;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (MPDApplication) activity.getApplication();
		coverHelper = new CoverAsyncHelper(app, PreferenceManager.getDefaultSharedPreferences(activity));
		coverHelper.setCoverRetrieversFromPreferences();
		coverHelper.addCoverDownloadListener(this);
		coverHelper.setCoverMaxSizeFromScreen(activity);
		if (coverArt != null) {
			coverHelper.setCachedCoverMaxSize(coverArt.getHeight());
		} else {
			// Fallback on the hardcoded size.
			coverHelper.setCachedCoverMaxSize((int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					FALLBACK_COVER_SIZE,
					getResources().getDisplayMetrics())));
		}
	}

	@Override
	public void onDestroyView() {
		headerArtist = null;
		headerInfo = null;
		coverArtProgress = null;
		coverArt.setImageResource(R.drawable.no_cover_art);
		coverArt = null;
		if (coverBitmap != null)
			coverBitmap.recycle();
		coverBitmap = null;
		super.onDestroyView();
	}

	@Override
	public void onDetach() {
		coverHelper = null;
		super.onDetach();
	}

	@Override
	public String getTitle() {
		if (album != null) {
			return album.getName();
		} else {
			return getString(R.string.songs);
		}
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
		coverArt = (ImageView) headerView.findViewById(R.id.albumCover);
		coverArtProgress = (ProgressBar) headerView.findViewById(R.id.albumCoverProgress);
		((TextView) headerView.findViewById(R.id.separator_title)).setText(R.string.songs);
		list.addHeaderView(headerView, null, false);
		return view;
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
			String artistName = getArtistForTrackList();
			headerArtist.setText(artistName);
			headerInfo.setText(getHeaderInfoString());
			if (coverHelper != null) {
				String filename = null;
				String path = null;
				if (items.size() > 0) {
					song = (Music) items.get(0);
					filename = song.getFilename();
					path = song.getPath();
					artistName = song.getArtist();
				}
				coverArtProgress.setVisibility(ProgressBar.VISIBLE);
				coverHelper.downloadCover(artistName, album.getName(), path, filename);
			} else {
				onCoverNotFound();
			}
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
	
	@Override
	protected boolean forceEmptyView() {
		return true;
	}

	@Override
	public void onCoverDownloaded(Bitmap cover) {
		coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
		try {
			if (cover != null) {
				coverBitmap = cover;
				BitmapDrawable myCover = new BitmapDrawable(getResources(), cover);
				coverArt.setImageDrawable(myCover);
			} else {
				onCoverNotFound();
			}
		} catch (Exception e) {
			// Just ignore
		}
	}

	@Override
	public void onCoverNotFound() {
		coverArtProgress.setVisibility(ProgressBar.INVISIBLE);
		coverArt.setImageResource(R.drawable.no_cover_art);
	}
}
