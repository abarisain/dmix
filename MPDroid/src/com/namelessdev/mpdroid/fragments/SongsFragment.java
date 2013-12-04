package com.namelessdev.mpdroid.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.PopupMenu.OnMenuItemClickListener;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverInfo;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SongDataBinder;
import org.a0z.mpd.*;
import org.a0z.mpd.exception.MPDServerException;

public class SongsFragment extends BrowseFragment {

    private static final String EXTRA_ARTIST = "artist";
    private static final String EXTRA_ALBUM = "album";
    private static final int POPUP_COVER_BLACKLIST = 5;
    private static final int POPUP_COVER_SELECTIVE_CLEAN = 6;

    Album album = null;
    Artist artist = null;
    TextView headerArtist;
    TextView headerInfo;

    private AlbumCoverDownloadListener coverArtListener;
    ImageView coverArt;
    ProgressBar coverArtProgress;

    CoverAsyncHelper coverHelper;
    ImageButton albumMenu;
    PopupMenu popupMenu;
    private PopupMenu coverPopupMenu;

    public SongsFragment() {
        super(R.string.addSong, R.string.songAdded, MPDCommand.MPD_SEARCH_TITLE);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null)
            init((Artist) icicle.getParcelable(EXTRA_ARTIST), (Album) icicle.getParcelable(EXTRA_ALBUM));
    }

    public SongsFragment init(Artist ar, Album al) {
        artist = ar;
        album = al;
        return this;
    }

    @Override
    public void onDestroyView() {
        headerArtist = null;
        headerInfo = null;
        coverArtListener.freeCoverDrawable();
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
        final View view = inflater.inflate(R.layout.songs, container, false);
        list = (ListView) view.findViewById(R.id.list);
        registerForContextMenu(list);
        list.setOnItemClickListener(this);
        loadingView = view.findViewById(R.id.loadingLayout);
        loadingTextView = (TextView) view.findViewById(R.id.loadingText);
        noResultView = view.findViewById(R.id.noResultLayout);
        loadingTextView.setText(getLoadingText());

        final View headerView = inflater.inflate(R.layout.song_header, null, false);
        coverArt = (ImageView) view.findViewById(R.id.albumCover);
        if (coverArt != null) {
            headerArtist = (TextView) view.findViewById(R.id.tracks_artist);
            headerInfo = (TextView) view.findViewById(R.id.tracks_info);
            coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
            albumMenu = (ImageButton) view.findViewById(R.id.album_menu);
        } else {
            headerArtist = (TextView) headerView.findViewById(R.id.tracks_artist);
            headerInfo = (TextView) headerView.findViewById(R.id.tracks_info);
            coverArt = (ImageView) headerView.findViewById(R.id.albumCover);
            coverArtProgress = (ProgressBar) headerView.findViewById(R.id.albumCoverProgress);
            albumMenu = (ImageButton) headerView.findViewById(R.id.album_menu);
        }

        final MPDApplication app = (MPDApplication) getActivity().getApplication();
        coverArtListener = new AlbumCoverDownloadListener(getActivity(), coverArt, coverArtProgress, app.isLightThemeSelected(), false);
        coverHelper = new CoverAsyncHelper(app, PreferenceManager.getDefaultSharedPreferences(getActivity()));
        coverHelper.setCoverMaxSizeFromScreen(getActivity());
        final ViewTreeObserver vto = coverArt.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if (coverHelper != null)
                    coverHelper.setCachedCoverMaxSize(coverArt.getMeasuredHeight());
                return true;
            }
        });
        coverHelper.addCoverDownloadListener(coverArtListener);

        ((TextView) headerView.findViewById(R.id.separator_title)).setText(R.string.songs);
        ((ListView) list).addHeaderView(headerView, null, false);

        popupMenu = new PopupMenu(getActivity(), albumMenu);
        popupMenu.getMenu().add(Menu.NONE, ADD, Menu.NONE, R.string.addAlbum);
        popupMenu.getMenu().add(Menu.NONE, ADDNREPLACE, Menu.NONE, R.string.addAndReplace);
        popupMenu.getMenu().add(Menu.NONE, ADDNREPLACEPLAY, Menu.NONE, R.string.addAndReplacePlay);
        popupMenu.getMenu().add(Menu.NONE, ADDNPLAY, Menu.NONE, R.string.addAndPlay);

        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int itemId = item.getItemId();
                app.oMPDAsyncHelper.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        boolean replace = false;
                        boolean play = false;
                        switch (itemId) {
                            case ADDNREPLACEPLAY:
                                replace = true;
                                play = true;
                                break;
                            case ADDNREPLACE:
                                replace = true;
                                break;
                            case ADDNPLAY:
                                play = true;
                                break;
                        }
                        try {
                            app.oMPDAsyncHelper.oMPD.add(artist, album, replace, play);
                            Tools.notifyUser(String.format(getResources().getString(R.string.albumAdded), album), getActivity());
                        } catch (MPDServerException e) {
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            }
        });

        albumMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });

        coverPopupMenu = new PopupMenu(getActivity(), coverArt);
        coverPopupMenu.getMenu().add(POPUP_COVER_BLACKLIST, POPUP_COVER_BLACKLIST, 0, R.string.otherCover);
        coverPopupMenu.getMenu().add(POPUP_COVER_SELECTIVE_CLEAN, POPUP_COVER_SELECTIVE_CLEAN, 0, R.string.resetCover);
        coverPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getGroupId()) {
                    case POPUP_COVER_BLACKLIST:
                        CoverManager.getInstance(app, PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext())).markWrongCover(album.getAlbumInfo());
                        updateCover(album.getAlbumInfo());
                        updateNowPlayingSmallFragment(album.getAlbumInfo());
                        break;
                    case POPUP_COVER_SELECTIVE_CLEAN:
                        CoverManager.getInstance(app, PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext())).clear(album.getAlbumInfo());
                        updateCover(album.getAlbumInfo());
                        updateNowPlayingSmallFragment(album.getAlbumInfo());
                        break;
                }
                return true;
            }
        });

        coverArt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                coverPopupMenu.show();
                return false;
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(EXTRA_ALBUM, album);
        outState.putParcelable(EXTRA_ARTIST, artist);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, View v, final int position, long id) {
        app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                add((Item) adapterView.getAdapter().getItem(position), false, false);
            }
        });
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        Music music = (Music) item;
        try {
            app.oMPDAsyncHelper.oMPD.add(music, replace, play);
            Tools.notifyUser(String.format(getResources().getString(R.string.songAdded, music.getTitle()), music.getName()),
                    getActivity());
        } catch (MPDServerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, (Music) item);
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void asyncUpdate() {
        try {
            if (getActivity() == null)
                return;
            items = app.oMPDAsyncHelper.oMPD.getSongs(artist, album);
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateFromItems() {
        super.updateFromItems();
        if (items != null) {
            String artistName = getArtistForTrackList();
            headerArtist.setText(artistName);
            headerInfo.setText(getHeaderInfoString());
            if (coverHelper != null) {
                coverHelper.downloadCover(album.getAlbumInfo());
            } else {
                coverArtListener.onCoverNotFound(new CoverInfo(artistName, album.getName()));
            }
        }

    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (items != null) {
            Music song;
            boolean differentArtists = false;
            String lastArtist = null;
            for (Item item : items) {
                song = (Music) item;
                if (lastArtist == null) {
                    lastArtist = song.getArtist();
                    continue;
                }
                if (!lastArtist.equalsIgnoreCase(song.getArtist())) {
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
        for (Item item : items) {
            song = (Music) item;
            if (lastArtist == null) {
                lastArtist = song.getAlbumArtist();
                continue;
            }
            if (!lastArtist.equalsIgnoreCase(song.getAlbumArtist())) {
                differentArtists = true;
                break;
            }
        }
        if (differentArtists || lastArtist == null || lastArtist.equals("")) {
            differentArtists = false;
            for (Item item : items) {
                song = (Music) item;
                if (lastArtist == null) {
                    lastArtist = song.getArtist();
                    continue;
                }
                if (!lastArtist.equalsIgnoreCase(song.getArtist())) {
                    differentArtists = true;
                    break;
                }
            }
            if (differentArtists || lastArtist == null || lastArtist.equals("")) {
                return getString(R.string.variousArtists);
            }
            return lastArtist;
        }
        return lastArtist;
    }

    private String getTotalTimeForTrackList() {
        Music song;
        long totalTime = 0;
        for (Item item : items) {
            song = (Music) item;
            if (song.getTime() > 0)
                totalTime += song.getTime();
        }
        return Music.timeToString(totalTime);
    }

    private String getHeaderInfoString() {
        final int count = items.size();
        return String.format(getString(count > 1 ? R.string.tracksInfoHeaderPlural : R.string.tracksInfoHeader), count,
                getTotalTimeForTrackList());
    }

    @Override
    protected boolean forceEmptyView() {
        return true;
    }

    public void updateCover(AlbumInfo albumInfo) {
        if (coverArt != null && null != coverArt.getTag() && coverArt.getTag().equals(albumInfo.getKey())) {
            coverHelper.downloadCover(albumInfo);
        }
    }

    private void updateNowPlayingSmallFragment(AlbumInfo albumInfo) {
        NowPlayingSmallFragment nowPlayingSmallFragment;
        if (getActivity() != null) {
            nowPlayingSmallFragment = (NowPlayingSmallFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.now_playing_small_fragment);
            if (nowPlayingSmallFragment != null) {
                nowPlayingSmallFragment.updateCover(albumInfo);
            }
        }
    }

}
