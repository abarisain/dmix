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
    class SearchResultsPagerAdapter extends PagerAdapter {

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return 3;
        }

        public Object instantiateItem(View collection, int position) {

            View v;
            switch (position) {
                default:
                case 0:
                    v = listArtistsFrame;
                    break;
                case 1:
                    v = listAlbumsFrame;
                    break;
                case 2:
                    v = listSongsFrame;
                    break;
            }
            if (v.getParent() == null)
                pager.addView(v);
            return v;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }

    public static final int MAIN = 0;

    public static final int PLAYLIST = 3;
    public static final int ADD = 0;
    public static final int ADD_REPLACE = 1;
    public static final int ADD_REPLACE_PLAY = 3;

    public static final int ADD_PLAY = 2;
    private ArrayList<Artist> arrayArtistsResults;
    private ArrayList<Album> arrayAlbumsResults;

    private ArrayList<Music> arraySongsResults;
    protected int iJobID = -1;
    private View listArtistsFrame = null;
    private View listAlbumsFrame = null;
    private View listSongsFrame = null;
    private ListView listArtists = null;
    private ListView listAlbums = null;
    private ListView listSongs = null;
    protected View loadingView;
    protected View noResultArtistsView;
    protected View noResultAlbumsView;
    protected View noResultSongsView;

    protected ViewPager pager;
    private Tab tabArtists;
    private Tab tabAlbums;

    private Tab tabSongs;

    private static final String TAG = "SearchActivity";

    private int addString, addedString;

    private String searchKeywords = "";

    public SearchActivity() {
        addString = R.string.addSong;
        addedString = R.string.songAdded;
        arrayArtistsResults = new ArrayList<Artist>();
        arrayAlbumsResults = new ArrayList<Album>();
        arraySongsResults = new ArrayList<Music>();
    }

    protected void add(Artist artist, Album album, boolean replace, boolean play) {
        try {
            String note;
            if (artist == null) {
                app.oMPDAsyncHelper.oMPD.add(album, replace, play);
                note = album.getArtist().getName() + " - " + album.getName();
            } else if (album == null) {
                app.oMPDAsyncHelper.oMPD.add(artist, replace, play);
                note = artist.getName();
            } else {
                return;
            }
            Tools.notifyUser(addedString, note);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    protected void add(Music music, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add(music, replace, play);
            Tools.notifyUser(R.string.songAdded, music.getTitle(), music.getName());
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    protected void add(Object object, boolean replace, boolean play) {
        setContextForObject(object);
        if (object instanceof Music) {
            add((Music) object, replace, play);
        } else if (object instanceof Artist) {
            add(((Artist) object), null, replace, play);
        } else if (object instanceof Album) {
            add(null, (Album) object, replace, play);
        }
    }

    @Override
    public void asyncExecSucceeded(int jobID) {
        if (iJobID == jobID) {
            updateFromItems();
        }
    }

    protected void asyncUpdate() {
        final String finalsearch = this.searchKeywords.toLowerCase();

        ArrayList<Music> arrayMusic = null;

        try {
            arrayMusic = (ArrayList<Music>) app.oMPDAsyncHelper.oMPD.search("any", finalsearch);
        } catch (final MPDServerException e) {
            Log.e(TAG, "MPD search failure.", e);

        }

        if (arrayMusic == null) {
            return;
        }

        arrayArtistsResults.clear();
        arrayAlbumsResults.clear();
        arraySongsResults.clear();

        String tmpValue;
        boolean valueFound;
        for (Music music : arrayMusic) {
            if (music.getTitle() != null && music.getTitle().toLowerCase().contains(finalsearch)) {
                arraySongsResults.add(music);
            }
            valueFound = false;
            Artist artist = music.getAlbumArtistAsArtist();
            if (artist == null || artist.isUnknown()) {
                artist = music.getArtistAsArtist();
            }
            if (artist != null) {
                tmpValue = artist.getName().toLowerCase();
                if (tmpValue.contains(finalsearch)) {
                    for (Artist artistItem : arrayArtistsResults) {
                        if (artistItem.getName().equalsIgnoreCase(tmpValue))
                            valueFound = true;
                    }
                    if (!valueFound)
                        arrayArtistsResults.add(artist);
                }
            }
            valueFound = false;
            Album album = music.getAlbumAsAlbum();
            if (album != null && album.getName() != null) {
                tmpValue = album.getName().toLowerCase();
                if (tmpValue.contains(finalsearch)) {
                    for (Album albumItem : arrayAlbumsResults) {
                        if (albumItem.getName().equalsIgnoreCase(tmpValue))
                            valueFound = true;
                    }
                    if (!valueFound)
                        arrayAlbumsResults.add(album);
                }
            }
        }

        Collections.sort(arrayArtistsResults);
        Collections.sort(arrayAlbumsResults);
        Collections.sort(arraySongsResults);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tabArtists.setText(getString(R.string.artists) + " (" + arrayArtistsResults.size()
                        + ")");
                tabAlbums.setText(getString(R.string.albums) + " (" + arrayAlbumsResults.size()
                        + ")");
                tabSongs.setText(getString(R.string.songs) + " (" + arraySongsResults.size() + ")");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);

        SearchResultsPagerAdapter adapter = new SearchResultsPagerAdapter();
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        tabArtists = actionBar.newTab()
                .setText(R.string.artists)
                .setTabListener(this);
        actionBar.addTab(tabArtists);

        tabAlbums = actionBar.newTab()
                .setText(R.string.albums)
                .setTabListener(this);
        actionBar.addTab(tabAlbums);

        tabSongs = actionBar.newTab()
                .setText(R.string.songs)
                .setTabListener(this);
        actionBar.addTab(tabSongs);

        listArtistsFrame = findViewById(R.id.list_artists_frame);
        noResultArtistsView = listArtistsFrame.findViewById(R.id.no_artist_result);
        listArtists = (ListView) listArtistsFrame.findViewById(android.R.id.list);
        listArtists.setOnItemClickListener(this);

        listAlbumsFrame = findViewById(R.id.list_albums_frame);
        noResultAlbumsView = listAlbumsFrame.findViewById(R.id.no_album_result);
        listAlbums = (ListView) listAlbumsFrame.findViewById(android.R.id.list);
        listAlbums.setOnItemClickListener(this);

        listSongsFrame = findViewById(R.id.list_songs_frame);
        noResultSongsView = listSongsFrame.findViewById(R.id.no_song_result);
        listSongs = (ListView) listSongsFrame.findViewById(android.R.id.list);
        listSongs.setOnItemClickListener(this);

        loadingView = findViewById(R.id.loadingLayout);
        loadingView.setVisibility(View.VISIBLE);

        final Intent queryIntent = getIntent();
        final String queryAction = queryIntent.getAction();

        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY).trim();
            final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchRecentProvider.AUTHORITY, SearchRecentProvider.MODE);
            suggestions.saveRecentQuery(searchKeywords, null);
        } else {
            return; // Bye !
        }

        setTitle(getTitle() + " : " + searchKeywords);

        registerForContextMenu(listArtists);
        registerForContextMenu(listAlbums);
        registerForContextMenu(listSongs);

        updateList();
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDestroy() {
        app.oMPDAsyncHelper.removeAsyncExecListener(this);
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final MenuItem addItem = menu.add(Menu.NONE, ADD, 0, getString(addString));
        final MenuItem addAndReplaceItem = menu.add(Menu.NONE, ADD_REPLACE, 0,
                R.string.addAndReplace);
        final MenuItem addReplacePlayItem = menu.add(Menu.NONE, ADD_REPLACE_PLAY,
                0, R.string.addAndReplacePlay);
        final MenuItem addAndPlayItem = menu.add(Menu.NONE, ADD_PLAY, 0, R.string.addAndPlay);

        switch (pager.getCurrentItem()) {
            case 0:
                final Artist artist = arrayArtistsResults.get((int) info.id);
                menu.setHeaderTitle(artist.mainText());
                setContextForObject(artist);
                break;
            case 1:
                final Album album = arrayAlbumsResults.get((int) info.id);
                menu.setHeaderTitle(album.mainText());
                setContextForObject(album);
                break;
            case 2:
                final Music music = arraySongsResults.get((int) info.id);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mpd_searchmenu, menu);
        return true;
    }

    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        Object selectedItem = adapterView.getAdapter().getItem(position);
        if (selectedItem instanceof Music) {
            add((Music) selectedItem, false, false);
        } else if (selectedItem instanceof Artist) {
            final Parcelable parcel = new ArtistParcelable((Artist) selectedItem);
            Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("artist", parcel);
            startActivityForResult(intent, -1);
        } else if (selectedItem instanceof Album) {
            final Parcelable parcel = new AlbumParcelable((Album) selectedItem);
            Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("album", parcel);
            startActivityForResult(intent, -1);
        }
    }

    @Override
    public boolean onMenuItemClick(final android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        ArrayList<?> targetArray;
        switch (pager.getCurrentItem()) {
            default:
            case 0:
                targetArray = arrayArtistsResults;
                break;
            case 1:
                targetArray = arrayAlbumsResults;
                break;
            case 2:
                targetArray = arraySongsResults;
                break;
        }
        final Object selectedItem = targetArray.get((int) info.id);
        app.oMPDAsyncHelper.execAsync(new Runnable() {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                this.onSearchRequested();
                return true;
            case android.R.id.home:
                final Intent i = new Intent(this, MainMenuActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        app.setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        app.unsetActivity(this);
    }

    /**
     * *** ActionBar TabListener methods ****
     */

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        pager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    private void setContextForObject(Object object) {
        if (object instanceof Music) {
            addString = R.string.addSong;
            addedString = R.string.songAdded;
        } else if (object instanceof Artist) {
            addString = R.string.addArtist;
            addedString = R.string.artistAdded;
        } else if (object instanceof Album) {
            addString = R.string.addAlbum;
            addedString = R.string.albumAdded;
        }
    }

    /**
     * Update the view from the items list if items is set.
     */
    public void updateFromItems() {
        if (arrayArtistsResults != null) {
            listArtists.setAdapter(new SeparatedListAdapter(this,
                    R.layout.search_list_item,
                    new SearchResultDataBinder(),
                    arrayArtistsResults));
            try {
                listArtists.setEmptyView(noResultArtistsView);
                loadingView.setVisibility(View.GONE);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to update artists from items.", e);
            }
        }
        if (arrayAlbumsResults != null) {
            listAlbums.setAdapter(new SeparatedListAdapter(this,
                    R.layout.search_list_item,
                    new SearchResultDataBinder(),
                    arrayAlbumsResults));
            try {
                listAlbums.setEmptyView(noResultAlbumsView);
                loadingView.setVisibility(View.GONE);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to update albums from items.", e);
            }
        }
        if (arraySongsResults != null) {
            listSongs.setAdapter(new SeparatedListAdapter(this,
                    R.layout.search_list_item,
                    new SearchResultDataBinder(),
                    arraySongsResults));
            try {
                listSongs.setEmptyView(noResultSongsView);
                loadingView.setVisibility(View.GONE);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to update songs from items.", e);
            }
        }
    }

    public void updateList() {
        app.oMPDAsyncHelper.addAsyncExecListener(this);
        iJobID = app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                asyncUpdate();
            }
        });
    }

}
