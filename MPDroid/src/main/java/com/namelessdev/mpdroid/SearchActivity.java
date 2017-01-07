/*
 * Copyright (C) 2010-2016 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.favorites.Favorites;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.ui.ToolbarHelper;
import com.namelessdev.mpdroid.views.SearchResultDataBinder;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
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
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchActivity extends MPDActivity implements OnMenuItemClickListener,
        AsyncExecListener, OnItemClickListener, ActionBar.TabListener {

    public static final int ADD = 0;

    public static final int ADD_PLAY = 2;

    public static final int ADD_REPLACE = 1;

    public static final int ADD_REPLACE_PLAY = 3;

    public static final int GOTO_ALBUM = 4;

    //public static final int MAIN = 0;

    //public static final int PLAYLIST = 3;

    private static final String PLAY_SERVICES_ACTION_SEARCH
            = "com.google.android.gms.actions.SEARCH_ACTION";

    private static final int RESULT_ALBUM = 1;

    private static final int RESULT_ARTIST = 0;

    private static final int RESULT_MUSIC = 2;

    private static final String TAG = "SearchActivity";

    private static final String UNKNOWN_ITEM_ERROR = "Unknown item.";

    /**
     * The token called back when {@link #asyncComplete(CharSequence)} is called, to indicate
     * updated items being available.
     */
    private static final CharSequence UPDATE_ITEMS_TOKEN = "UPDATE_ITEMS";

    private final ArrayList<Album> mAlbumResults;

    private final ArrayList<Artist> mArtistResults;

    private final ArrayList<Music> mSongResults;

    //protected int mJobID = -1;

    protected View mLoadingView;

    protected View mNoResultAlbumsView;

    protected View mNoResultArtistsView;

    protected View mNoResultSongsView;

    protected ViewPager mPager;

    @StringRes
    private int mAddString;

    @StringRes
    private int mAddedString;

    private ListView mListAlbums = null;

    private View mListAlbumsFrame = null;

    private ListView mListArtists = null;

    private View mListArtistsFrame = null;

    private ListView mListSongs = null;

    private View mListSongsFrame = null;

    private String mSearchKeywords = null;

    private ActionBar.Tab mTabAlbums;

    private ActionBar.Tab mTabArtists;

    private ActionBar.Tab mTabSongs;

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
        String note = null;

        try {
            if (artist == null) {
                final Artist albumArtist = album.getArtist();

                mApp.getMPD().add(album, replace, play);
                if (albumArtist != null) {
                    note = albumArtist.getName() + " - " + album.getName();
                }
            } else if (album == null) {
                mApp.getMPD().add(artist, replace, play);
                note = artist.getName();
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }

        if (note != null) {
            Tools.notifyUser(mAddedString, note);
        }
    }

    protected void add(final Music music, final boolean replace, final boolean play) {
        try {
            mApp.getMPD().add(music, replace, play);
            Tools.notifyUser(R.string.songAdded, music.getTitle(), music.getName());
        } catch (final IOException | MPDException e) {
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
    public void asyncComplete(final CharSequence token) {
        if (UPDATE_ITEMS_TOKEN.equals(token)) {
            updateFromItems();
        }
    }

    protected void asyncUpdate() {
        final String finalSearch = mSearchKeywords.toLowerCase();

        List<Music> arrayMusic = null;

        try {
            arrayMusic = new ArrayList<>(mApp.getMPD().search("any", finalSearch));
            Collections.sort(arrayMusic);
        } catch (final IOException | MPDException e) {
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
            if (music.getTitle() != null && music.getTitle().toLowerCase().contains(finalSearch)) {
                mSongResults.add(music);
            }
            valueFound = false;

            String artistName = music.getAlbumArtistName();
            if (TextUtils.isEmpty(artistName)) {
                artistName = music.getArtistName();
            }

            if (artistName != null) {
                final Artist artist = Artist.byName(artistName);

                tmpValue = artistName.toLowerCase();
                if (tmpValue.contains(finalSearch)) {
                    for (final Artist artistItem : mArtistResults) {
                        final String artistItemName = artistItem.getName();
                        if (artistItemName != null &&
                                artistItemName.equalsIgnoreCase(tmpValue)) {
                            valueFound = true;
                        }
                    }
                    if (!valueFound) {
                        mArtistResults.add(artist);
                    }
                }
            }

            valueFound = false;
            final Album album = music.getAlbum();
            if (album != null) {
                final String albumName = album.getName();
                if (albumName != null) {
                    tmpValue = albumName.toLowerCase();
                    if (tmpValue.contains(finalSearch)) {
                        for (final Album albumItem : mAlbumResults) {
                            final String albumItemName = albumItem.getName();
                            if (albumItemName.equalsIgnoreCase(tmpValue)) {
                                valueFound = true;
                            }
                        }
                        if (!valueFound) {
                            mAlbumResults.add(album);
                        }
                    }
                }
            }
        }

        Collections.sort(mArtistResults);
        Collections.sort(mAlbumResults);
        Collections.sort(mSongResults, Music.COMPARE_WITHOUT_TRACK_NUMBER);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTabArtists.setText(
                        getString(R.string.artists) + " (" + mArtistResults.size() + ')');
                mTabAlbums.setText(getString(R.string.albums) + " (" + mAlbumResults.size() + ')');
                mTabSongs.setText(getString(R.string.songs) + " (" + mSongResults.size() + ')');
            }
        });
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);

        final SearchResultsPagerAdapter adapter = new SearchResultsPagerAdapter();
        final ActionBar actionBar = getSupportActionBar();

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(adapter);
        mPager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(final int position) {
                        // When swiping between pages, select the corresponding tab.
                        actionBar.setSelectedNavigationItem(position);
                    }
                });

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

        if (Intent.ACTION_SEARCH.equals(queryAction) || PLAY_SERVICES_ACTION_SEARCH
                .equals(queryAction) && queryIntent.hasExtra(SearchManager.QUERY)) {
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
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        switch (mPager.getCurrentItem()) {
            case RESULT_ALBUM:
                final Album album = mAlbumResults.get((int) info.id);
                menu.setHeaderTitle(album.toString());
                setContextForObject(album);
                break;
            case RESULT_ARTIST:
                final Artist artist = mArtistResults.get((int) info.id);
                menu.setHeaderTitle(artist.toString());
                setContextForObject(artist);
                break;
            case RESULT_MUSIC:
                final Music music = mSongResults.get((int) info.id);
                final MenuItem gotoAlbumItem = menu
                        .add(Menu.NONE, GOTO_ALBUM, 0, R.string.goToAlbum);
                gotoAlbumItem.setOnMenuItemClickListener(this);
                menu.setHeaderTitle(music.toString());
                setContextForObject(music);
                break;
            default:
                throw new UnsupportedOperationException(UNKNOWN_ITEM_ERROR);
        }

        final MenuItem addItem = menu.add(Menu.NONE, ADD, 0, getString(mAddString));
        final MenuItem addAndReplaceItem = menu.add(Menu.NONE, ADD_REPLACE, 0,
                R.string.addAndReplace);
        final MenuItem addReplacePlayItem = menu.add(Menu.NONE, ADD_REPLACE_PLAY,
                0, R.string.addAndReplacePlay);
        final MenuItem addAndPlayItem = menu.add(Menu.NONE, ADD_PLAY, 0, R.string.addAndPlay);

        addItem.setOnMenuItemClickListener(this);
        addAndReplaceItem.setOnMenuItemClickListener(this);
        addReplacePlayItem.setOnMenuItemClickListener(this);
        addAndPlayItem.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mpd_searchmenu, menu);
        ToolbarHelper.manuallySetupSearchView(this,
                (SearchView) menu.findItem(R.id.menu_search).getActionView());
        return true;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        final Object selectedItem = parent.getAdapter().getItem(position);
        if (selectedItem instanceof Music) {
            add((Music) selectedItem, false, false);
        } else if (selectedItem instanceof Artist) {
            final Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra(Artist.EXTRA, (Parcelable) selectedItem);
            startActivityForResult(intent, -1);
        } else if (selectedItem instanceof Album) {
             final Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra(Album.EXTRA, (Parcelable) selectedItem);
            startActivityForResult(intent, -1);
        }
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final View itemFocus = (View) info.targetView.getParent();
        final View currentFocus = getCurrentFocus();
        final boolean isConsumed;

        if (itemFocus == null || currentFocus != null && !itemFocus.equals(currentFocus)) {
            isConsumed = false;
            Log.w(TAG, "Ignoring menu item press due to view change.");
        } else {
            isConsumed = true;
            final List<? extends Item<?>> targetArray;

            switch (mPager.getCurrentItem()) {
                case RESULT_ALBUM:
                    targetArray = mAlbumResults;
                    break;
                case RESULT_ARTIST:
                    targetArray = mArtistResults;
                    break;
                case RESULT_MUSIC:
                    targetArray = mSongResults;
                    break;
                default:
                    throw new UnsupportedOperationException(UNKNOWN_ITEM_ERROR);
            }
            final Object selectedItem = targetArray.get((int) info.id);
            if (item.getItemId() == GOTO_ALBUM) {
                if (selectedItem instanceof Music) {
                    final Music music = (Music) selectedItem;
                    final Intent intent = new Intent(this, SimpleLibraryActivity.class);
                    final Parcelable artist = Artist.byName(music.getAlbumArtistOrArtist());
                    intent.putExtra(Artist.EXTRA, artist);
                    intent.putExtra(Album.EXTRA, music.getAlbum());
                    startActivityForResult(intent, -1);
                }
            }
            else {
                mApp.getAsyncHelper().execAsync(new Runnable() {
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
                            default:
                                break;
                        }
                        add(selectedItem, replace, play);
                    }
                });
            }
        }

        return isConsumed;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean handled = true;

        switch (item.getItemId()) {
            case R.id.menu_search:
                onSearchRequested();
                break;
            case android.R.id.home:
                final Intent intent = new Intent(this, MainMenuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            default:
                handled = false;
                break;
        }

        return handled;
    }

    @Override
    public void onTabReselected(final ActionBar.Tab tab, final FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(final ActionBar.Tab tab, final FragmentTransaction ft) {
        mPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(final ActionBar.Tab tab, final FragmentTransaction ft) {
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
     * This updates a specific ListView for search results.
     *
     * @param listView      The ListView to update with search results.
     * @param resultList    The List of results to enter into the ListView.
     * @param noResultsView The View to hide if there are no results.
     */
    private void update(final ListView listView, final List<? extends Item<?>> resultList,
            final View noResultsView) {
        final ListAdapter separatedListAdapter = new SeparatedListAdapter(this,
                R.layout.search_list_item,
                new SearchResultDataBinder(),
                resultList);

        listView.setAdapter(separatedListAdapter);

        try {
            listView.setEmptyView(noResultsView);
            mLoadingView.setVisibility(View.GONE);
        } catch (final RuntimeException e) {
            Log.e(TAG, "Failed to update items.", e);
        }
    }

    /**
     * Update the view from the items list if items is set.
     */
    public void updateFromItems() {
        update(mListArtists, mArtistResults, mNoResultArtistsView);
        update(mListAlbums, mAlbumResults, mNoResultAlbumsView);
        update(mListSongs, mSongResults, mNoResultSongsView);
    }

    public void updateList() {
        mApp.getAsyncHelper().execAsync(this, UPDATE_ITEMS_TOKEN, new Runnable() {
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

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {

            final View v;
            switch (position) {
                case RESULT_ALBUM:
                    v = mListAlbumsFrame;
                    break;
                case RESULT_ARTIST:
                    v = mListArtistsFrame;
                    break;
                case RESULT_MUSIC:
                    v = mListSongsFrame;
                    break;
                default:
                    throw new UnsupportedOperationException(UNKNOWN_ITEM_ERROR);
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
