/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.MPDroidActivities.MPDroidActivity;
import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SearchResultDataBinder;

import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.AlbumParcelable;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.ArtistParcelable;
import org.a0z.mpd.item.Music;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;

public class SearchActivity extends MPDroidActivity implements OnMenuItemClickListener,
        AsyncExecListener, OnItemClickListener, TabListener {

    public static final int ADD = 0;

    public static final int ADD_PLAY = 2;

    public static final int ADD_REPLACE = 1;

    public static final int ADD_REPLACE_PLAY = 3;

    public static final int MAIN = 0;

    public static final int PLAYLIST = 3;

    private static final String TAG = "SearchActivity";

    private final ArrayList<Album> mAlbumResults;

    private final ArrayList<Artist> mArtistResults;

    private final ArrayList<Music> mSongResults;

    protected int mJobID = -1;

    protected View mLoadingView;

    protected View mNoResultAlbumsView;

    protected View mNoResultArtistsView;

    protected View mNoResultSongsView;

    protected ViewPager mPager;

    private int mAddString;

    private int mAddedString;

    private ListView mListAlbums = null;

    private View mListAlbumsFrame = null;

    private ListView mListArtists = null;

    private View mListArtistsFrame = null;

    private ListView mListSongs = null;

    private View mListSongsFrame = null;

    private String mSearchKeywords = "";

    private Tab mTabAlbums;

    private Tab mTabArtists;

    private Tab mTabSongs;

    public SearchActivity() {
        super();
        mAddString = R.string.addSong;
        mAddedString = R.string.songAdded;
        mArtistResults = new ArrayList<>();
        mAlbumResults = new ArrayList<>();
        mSongResults = new ArrayList<>();
    }

    protected void add(final Artist artist, final Album album, final boolean replace,
            final boolean play) {
        try {
            final String note;
            if (artist == null) {
                mApp.oMPDAsyncHelper.oMPD.add(album, replace, play);
                note = album.getArtist().getName() + " - " + album.getName();
            } else if (album == null) {
                mApp.oMPDAsyncHelper.oMPD.add(artist, replace, play);
                note = artist.getName();
            } else {
                return;
            }
            Tools.notifyUser(mAddedString, note);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    protected void add(final Music music, final boolean replace, final boolean play) {
        try {
            mApp.oMPDAsyncHelper.oMPD.add(music, replace, play);
            Tools.notifyUser(R.string.songAdded, music.getTitle(), music.getName());
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    protected void add(final Object object, final boolean replace, final boolean play) {
        setContextForObject(object);
        if (object instanceof Music) {
            add((Music) object, replace, play);
        } else if (object instanceof Artist) {
            add((Artist) object, null, replace, play);
        } else if (object instanceof Album) {
            add(null, (Album) object, replace, play);
        }
    }

    @Override
    public void asyncExecSucceeded(final int jobID) {
        if (mJobID == jobID) {
            updateFromItems();
        }
    }

    protected void asyncUpdate() {
        final String finalsearch = mSearchKeywords.toLowerCase();

        ArrayList<Music> arrayMusic = null;

        try {
            arrayMusic = (ArrayList<Music>) mApp.oMPDAsyncHelper.oMPD.search("any", finalsearch);
        } catch (final MPDServerException e) {
            Log.e(TAG, "MPD search failure.", e);

        }

        if (arrayMusic == null) {
            return;
        }

        mArtistResults.clear();
        mAlbumResults.clear();
        mSongResults.clear();

        String tmpValue;
        boolean valueFound;
        for (final Music music : arrayMusic) {
            if (music.getTitle() != null && music.getTitle().toLowerCase().contains(finalsearch)) {
                mSongResults.add(music);
            }
            valueFound = false;
            Artist artist = music.getAlbumArtistAsArtist();
            if (artist == null || artist.isUnknown()) {
                artist = music.getArtistAsArtist();
            }
            if (artist != null) {
                tmpValue = artist.getName().toLowerCase();
                if (tmpValue.contains(finalsearch)) {
                    for (final Artist artistItem : mArtistResults) {
                        if (artistItem.getName().equalsIgnoreCase(tmpValue)) {
                            valueFound = true;
                        }
                    }
                    if (!valueFound) {
                        mArtistResults.add(artist);
                    }
                }
            }
            valueFound = false;
            final Album album = music.getAlbumAsAlbum();
            if (album != null && album.getName() != null) {
                tmpValue = album.getName().toLowerCase();
                if (tmpValue.contains(finalsearch)) {
                    for (final Album albumItem : mAlbumResults) {
                        if (albumItem.getName().equalsIgnoreCase(tmpValue)) {
                            valueFound = true;
                        }
                    }
                    if (!valueFound) {
                        mAlbumResults.add(album);
                    }
                }
            }
        }

        Collections.sort(mArtistResults);
        Collections.sort(mAlbumResults);
        Collections.sort(mSongResults);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTabArtists.setText(getString(R.string.artists) + " (" + mArtistResults.size()
                        + ')');
                mTabAlbums.setText(getString(R.string.albums) + " (" + mAlbumResults.size()
                        + ')');
                mTabSongs.setText(getString(R.string.songs) + " (" + mSongResults.size() + ')');
            }
        });
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);

        final SearchResultsPagerAdapter adapter = new SearchResultsPagerAdapter();
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(adapter);
        mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(final int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mTabArtists = actionBar.newTab()
                .setText(R.string.artists)
                .setTabListener(this);
        actionBar.addTab(mTabArtists);

        mTabAlbums = actionBar.newTab()
                .setText(R.string.albums)
                .setTabListener(this);
        actionBar.addTab(mTabAlbums);

        mTabSongs = actionBar.newTab()
                .setText(R.string.songs)
                .setTabListener(this);
        actionBar.addTab(mTabSongs);

        mListArtistsFrame = findViewById(R.id.list_artists_frame);
        mNoResultArtistsView = mListArtistsFrame.findViewById(R.id.no_artist_result);
        mListArtists = (ListView) mListArtistsFrame.findViewById(android.R.id.list);
        mListArtists.setOnItemClickListener(this);

        mListAlbumsFrame = findViewById(R.id.list_albums_frame);
        mNoResultAlbumsView = mListAlbumsFrame.findViewById(R.id.no_album_result);
        mListAlbums = (ListView) mListAlbumsFrame.findViewById(android.R.id.list);
        mListAlbums.setOnItemClickListener(this);

        mListSongsFrame = findViewById(R.id.list_songs_frame);
        mNoResultSongsView = mListSongsFrame.findViewById(R.id.no_song_result);
        mListSongs = (ListView) mListSongsFrame.findViewById(android.R.id.list);
        mListSongs.setOnItemClickListener(this);

        mLoadingView = findViewById(R.id.loadingLayout);
        mLoadingView.setVisibility(View.VISIBLE);

        final Intent queryIntent = getIntent();
        final String queryAction = queryIntent.getAction();

        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            mSearchKeywords = queryIntent.getStringExtra(SearchManager.QUERY).trim();
            final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchRecentProvider.AUTHORITY, SearchRecentProvider.MODE);
            suggestions.saveRecentQuery(mSearchKeywords, null);
        } else {
            return; // Bye !
        }

        setTitle(getTitle() + " : " + mSearchKeywords);

        registerForContextMenu(mListArtists);
        registerForContextMenu(mListAlbums);
        registerForContextMenu(mListSongs);

        updateList();
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final MenuItem addItem = menu.add(Menu.NONE, ADD, 0, getString(mAddString));
        final MenuItem addAndReplaceItem = menu.add(Menu.NONE, ADD_REPLACE, 0,
                R.string.addAndReplace);
        final MenuItem addReplacePlayItem = menu.add(Menu.NONE, ADD_REPLACE_PLAY,
                0, R.string.addAndReplacePlay);
        final MenuItem addAndPlayItem = menu.add(Menu.NONE, ADD_PLAY, 0, R.string.addAndPlay);

        switch (mPager.getCurrentItem()) {
            case 0:
                final Artist artist = mArtistResults.get((int) info.id);
                menu.setHeaderTitle(artist.mainText());
                setContextForObject(artist);
                break;
            case 1:
                final Album album = mAlbumResults.get((int) info.id);
                menu.setHeaderTitle(album.mainText());
                setContextForObject(album);
                break;
            case 2:
                final Music music = mSongResults.get((int) info.id);
                menu.setHeaderTitle(music.mainText());
                setContextForObject(music);
                break;
            default:
                break;
        }

        addItem.setOnMenuItemClickListener(this);
        addAndReplaceItem.setOnMenuItemClickListener(this);
        addReplacePlayItem.setOnMenuItemClickListener(this);
        addAndPlayItem.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mpd_searchmenu, menu);
        return true;
    }

    @Override
    public void onDestroy() {
        mApp.oMPDAsyncHelper.removeAsyncExecListener(this);
        super.onDestroy();
    }

    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        final Object selectedItem = parent.getAdapter().getItem(position);
        if (selectedItem instanceof Music) {
            add((Music) selectedItem, false, false);
        } else if (selectedItem instanceof Artist) {
            final Parcelable parcel = new ArtistParcelable((Artist) selectedItem);
            final Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("artist", parcel);
            startActivityForResult(intent, -1);
        } else if (selectedItem instanceof Album) {
            final Parcelable parcel = new AlbumParcelable((Album) selectedItem);
            final Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("album", parcel);
            startActivityForResult(intent, -1);
        }
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final ArrayList<?> targetArray;
        switch (mPager.getCurrentItem()) {
            default:
            case 0:
                targetArray = mArtistResults;
                break;
            case 1:
                targetArray = mAlbumResults;
                break;
            case 2:
                targetArray = mSongResults;
                break;
        }
        final Object selectedItem = targetArray.get((int) info.id);
        mApp.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                boolean replace = false;
                boolean play = false;
                switch (item.getItemId()) {
                    case ADD_REPLACE_PLAY:
                        replace = true;
                        play = true;
                        break;
                    case ADD_REPLACE:
                        replace = true;
                        break;
                    case ADD_PLAY:
                        play = true;
                        break;
                }
                add(selectedItem, replace, play);
            }
        });
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                onSearchRequested();
                return true;
            case android.R.id.home:
                final Intent intent = new Intent(this, MainMenuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        mApp.setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mApp.unsetActivity(this);
    }

    /**
     * *** ActionBar TabListener methods ****
     */

    @Override
    public void onTabReselected(final Tab tab, final FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(final Tab tab, final FragmentTransaction ft) {
        mPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(final Tab tab, final FragmentTransaction ft) {
    }

    private void setContextForObject(final Object object) {
        if (object instanceof Music) {
            mAddString = R.string.addSong;
            mAddedString = R.string.songAdded;
        } else if (object instanceof Artist) {
            mAddString = R.string.addArtist;
            mAddedString = R.string.artistAdded;
        } else if (object instanceof Album) {
            mAddString = R.string.addAlbum;
            mAddedString = R.string.albumAdded;
        }
    }

    /**
     * Update the view from the items list if items is set.
     */
    public void updateFromItems() {
        if (mArtistResults != null) {
            mListArtists.setAdapter(new SeparatedListAdapter(this,
                    R.layout.search_list_item,
                    new SearchResultDataBinder(),
                    mArtistResults));
            try {
                mListArtists.setEmptyView(mNoResultArtistsView);
                mLoadingView.setVisibility(View.GONE);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to update artists from items.", e);
            }
        }
        if (mAlbumResults != null) {
            mListAlbums.setAdapter(new SeparatedListAdapter(this,
                    R.layout.search_list_item,
                    new SearchResultDataBinder(),
                    mAlbumResults));
            try {
                mListAlbums.setEmptyView(mNoResultAlbumsView);
                mLoadingView.setVisibility(View.GONE);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to update albums from items.", e);
            }
        }
        if (mSongResults != null) {
            mListSongs.setAdapter(new SeparatedListAdapter(this,
                    R.layout.search_list_item,
                    new SearchResultDataBinder(),
                    mSongResults));
            try {
                mListSongs.setEmptyView(mNoResultSongsView);
                mLoadingView.setVisibility(View.GONE);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to update songs from items.", e);
            }
        }
    }

    public void updateList() {
        mApp.oMPDAsyncHelper.addAsyncExecListener(this);
        mJobID = mApp.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                asyncUpdate();
            }
        });
    }

    class SearchResultsPagerAdapter extends PagerAdapter {

        @Override
        public void destroyItem(final ViewGroup container, final int position,
                final Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return 3;
        }

        public Object instantiateItem(final ViewGroup container, final int position) {

            final View v;
            switch (position) {
                default:
                case 0:
                    v = mListArtistsFrame;
                    break;
                case 1:
                    v = mListAlbumsFrame;
                    break;
                case 2:
                    v = mListSongsFrame;
                    break;
            }
            if (v.getParent() == null) {
                mPager.addView(v);
            }
            return v;
        }

        @Override
        public boolean isViewFromObject(final View arg0, final Object arg1) {
            return arg0.equals(arg1);
        }
    }

}
