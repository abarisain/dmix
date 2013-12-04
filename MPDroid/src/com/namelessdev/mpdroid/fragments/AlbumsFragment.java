package com.namelessdev.mpdroid.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.util.Log;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.AlbumDataBinder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;
import org.a0z.mpd.*;
import org.a0z.mpd.exception.MPDServerException;

public class AlbumsFragment extends BrowseFragment {
    private static final String EXTRA_ARTIST = "artist";
    private static final String EXTRA_ARTISTS = "artists";
    protected Artist[] artists = null;
    protected boolean isCountPossiblyDisplayed;
    protected ProgressBar coverArtProgress;

    private static final int POPUP_COVER_BLACKLIST = 5;
    private static final int POPUP_COVER_SELECTIVE_CLEAN = 6;

    public AlbumsFragment() {
        this((Artist)null);
    }

    public AlbumsFragment(Artist artist) {
        super(R.string.addAlbum, R.string.albumAdded, MPDCommand.MPD_SEARCH_ALBUM);
        this.artists = new Artist[1];
        this.artists[0] = artist;
        isCountPossiblyDisplayed = true;
    }

    public AlbumsFragment(Artist[] artists) {
        super(R.string.addAlbum, R.string.albumAdded, MPDCommand.MPD_SEARCH_ALBUM);
        isCountPossiblyDisplayed = true;
        this.artists = artists;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public int getLoadingText() {
        return R.string.loadingAlbums;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArray(EXTRA_ARTISTS, artists);
        super.onSaveInstanceState(outState);
    }

    @Override
    public String getTitle() {
        if (artists != null) {
            String t = artists[0].getName();
            for (int i = 1; i < artists.length; i++) {
                t += ("/"+artists[i].getName());
            }
            return t;
        } else {
            return getString(R.string.albums);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new SongsFragment().init((Album) items.get(position)),
                "songs");
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (items != null) {
            String artistname = null;
            if (artists != null && artists.length > 0 && artists[0] != null) {
                artistname = artists[0].getName();
            }
            return new ArrayIndexerAdapter
                (getActivity(), new AlbumDataBinder(app, artistname, app.isLightThemeSelected()), items);
        }
        return super.getCustomListAdapter();
    }

    @Override
    protected void asyncUpdate() {
        try {
            items = app.oMPDAsyncHelper.oMPD.getAlbums(artists, isCountPossiblyDisplayed);
        } catch (MPDServerException e) {
        }
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add((Album) item, replace, play);
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void add(Item item, String playlist) {
        try {
            app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, ((Album) item));
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        return view;

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        android.view.MenuItem otherCoverItem = menu.add(POPUP_COVER_BLACKLIST, POPUP_COVER_BLACKLIST, 0, R.string.otherCover);
        otherCoverItem.setOnMenuItemClickListener(this);
        android.view.MenuItem resetCoverItem = menu.add(POPUP_COVER_SELECTIVE_CLEAN, POPUP_COVER_SELECTIVE_CLEAN, 0, R.string.resetCover);
        resetCoverItem.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Album album = (Album) items.get((int) info.id);

        switch (item.getGroupId()) {
            case POPUP_COVER_BLACKLIST:
                CoverManager.getInstance(app, PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext())).markWrongCover(album.getAlbumInfo());
                refreshCover(info.targetView, album.getAlbumInfo());
                updateNowPlayingSmallFragment(album.getAlbumInfo());
                break;
            case POPUP_COVER_SELECTIVE_CLEAN:
                CoverManager.getInstance(app, PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext())).clear(album.getAlbumInfo());
                refreshCover(info.targetView, album.getAlbumInfo());
                updateNowPlayingSmallFragment(album.getAlbumInfo());
                break;
            default:
                return super.onMenuItemClick(item);
        }
        return false;
    }

    private void refreshCover(View view, AlbumInfo album) {
        if (view.getTag() instanceof AlbumViewHolder) {
            AlbumViewHolder albumViewHolder = (AlbumViewHolder) view.getTag();
            if (albumViewHolder.albumCover.getTag(R.id.CoverAsyncHelper) instanceof CoverAsyncHelper) {
                CoverAsyncHelper coverAsyncHelper = (CoverAsyncHelper) albumViewHolder.albumCover.getTag(R.id.CoverAsyncHelper);
                albumViewHolder.coverArtProgress.setVisibility(ProgressBar.VISIBLE);
                coverAsyncHelper.downloadCover(album, true);
            }
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
