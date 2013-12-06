package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.Collections;

import org.a0z.mpd.Album;
import org.a0z.mpd.UnknownAlbum;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
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

import com.namelessdev.mpdroid.MPDroidActivities.MPDroidActivity;
import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.SearchResultDataBinder;

public class SearchActivity extends MPDroidActivity implements OnMenuItemClickListener, AsyncExecListener, OnItemClickListener, TabListener {
    public static final int MAIN = 0;
    public static final int PLAYLIST = 3;

    public static final int ADD = 0;
    public static final int ADDNREPLACE = 1;
    public static final int ADDNREPLACEPLAY = 3;
    public static final int ADDNPLAY = 2;

    private MPDApplication app;
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

    private int addString, addedString;
    private String searchKeywords = "";

    public SearchActivity() {
        addString = R.string.addSong;
        addedString = R.string.songAdded;
        arrayArtistsResults = new ArrayList<Artist>();
        arrayAlbumsResults = new ArrayList<Album>();
        arraySongsResults = new ArrayList<Music>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (MPDApplication) getApplication();

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
        noResultArtistsView = listArtistsFrame.findViewById(R.id.no_result);
        listArtists = (ListView) listArtistsFrame.findViewById(android.R.id.list);
        listArtists.setOnItemClickListener(this);

        listAlbumsFrame = findViewById(R.id.list_albums_frame);
        noResultAlbumsView = listAlbumsFrame.findViewById(R.id.no_result);
        listAlbums = (ListView) listAlbumsFrame.findViewById(android.R.id.list);
        listAlbums.setOnItemClickListener(this);

        listSongsFrame = findViewById(R.id.list_songs_frame);
        noResultSongsView = listSongsFrame.findViewById(R.id.no_result);
        listSongs = (ListView) listSongsFrame.findViewById(android.R.id.list);
        listSongs.setOnItemClickListener(this);

        loadingView = findViewById(R.id.loadingLayout);
        loadingView.setVisibility(View.VISIBLE);

        final Intent queryIntent = getIntent();
        final String queryAction = queryIntent.getAction();

        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY).trim();
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
    public void onStart() {
        super.onStart();
        MPDApplication app = (MPDApplication) getApplicationContext();
        app.setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        MPDApplication app = (MPDApplication) getApplicationContext();
        app.unsetActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mpd_searchmenu, menu);
        return true;
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

    private String getItemName(Object o) {
        if (o instanceof Music) {
            return ((Music) o).getTitle();
        } else if (o instanceof Artist) {
            return ((Artist) o).getName();
        } else if (o instanceof Album) {
            return ((Album) o).getName();
        }
        return "";
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

    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        Object selectedItem = adapterView.getAdapter().getItem(position);
        if (selectedItem instanceof Music) {
            add((Music) selectedItem, false, false);
        } else if (selectedItem instanceof Artist) {
            Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("artist", ((Artist) selectedItem));
            startActivityForResult(intent, -1);
        } else if (selectedItem instanceof Album) {
            Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("album", ((Album) selectedItem));
            startActivityForResult(intent, -1);
        }
    }

    protected void add(Object object, boolean replace, boolean play) {
        setContextForObject(object);
        if (object instanceof Music) {
            add((Music) object, replace, play);
        } else if (object instanceof Artist) {
            add(new UnknownAlbum((Artist) object), replace, play);
        } else if (object instanceof Album) {
            add((Album) object, replace, play);
        }
    }

    protected void add(Album album, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add(album, replace, play);
            String notifystr = album.getArtist().getName();
            if (notifystr == null) {
                notifystr = album.getName();
            } else {
                notifystr += " - " + album.getName();
            }
            Tools.notifyUser(String.format(getResources().getString(addedString), notifystr), this);
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    protected void add(Music music, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add(music, replace, play);
            Tools.notifyUser(String.format(getResources().getString(R.string.songAdded, music.getTitle()), music.getName()), this);
        } catch (MPDServerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        ArrayList<? extends Object> targetArray;
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
        final Object item = targetArray.get((int) info.id);
        menu.setHeaderTitle(getItemName(item));
        setContextForObject(item);
        android.view.MenuItem addItem = menu.add(ContextMenu.NONE, ADD, 0, getResources().getString(addString));
        addItem.setOnMenuItemClickListener(this);
        android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, ADDNREPLACE, 0, R.string.addAndReplace);
        addAndReplaceItem.setOnMenuItemClickListener(this);
        android.view.MenuItem addAndReplacePlayItem = menu.add(ContextMenu.NONE, ADDNREPLACEPLAY, 0, R.string.addAndReplacePlay);
        addAndReplacePlayItem.setOnMenuItemClickListener(this);
        android.view.MenuItem addAndPlayItem = menu.add(ContextMenu.NONE, ADDNPLAY, 0, R.string.addAndPlay);
        addAndPlayItem.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(final android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final MPDApplication app = (MPDApplication) getApplication();
        ArrayList<? extends Object> targetArray;
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
                add(selectedItem, replace, play);
            }
        });
        return false;
    }

    protected void asyncUpdate() {
        final String finalsearch = this.searchKeywords.toLowerCase();

        ArrayList<Music> arrayMusic = null;

        try {
            arrayMusic = (ArrayList<Music>) app.oMPDAsyncHelper.oMPD.search("any", finalsearch);
        } catch (MPDServerException e) {
            Log.e(SearchActivity.class.getSimpleName(), "MPD search failure : " + e);

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
            tmpValue = music.getArtist();
            if (tmpValue != null && tmpValue.toLowerCase().contains(finalsearch)) {
                for (Artist artistItem : arrayArtistsResults) {
                    if (artistItem.getName().equalsIgnoreCase(tmpValue))
                        valueFound = true;
                }
                if (!valueFound)
                    arrayArtistsResults.add(new Artist(tmpValue, 0));
            }
            valueFound = false;
            tmpValue = music.getAlbum();
            if (tmpValue != null && tmpValue.toLowerCase().contains(finalsearch)) {
                for (Album albumItem : arrayAlbumsResults) {
                    if (albumItem.getName().equalsIgnoreCase(tmpValue))
                        valueFound = true;
                }
                if (!valueFound)
                    arrayAlbumsResults.add(new Album(tmpValue, new Artist(music.getArtist())));
            }
        }

        Collections.sort(arrayArtistsResults);
        Collections.sort(arrayAlbumsResults);
        Collections.sort(arraySongsResults);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tabArtists.setText(getString(R.string.artists) + " (" + arrayArtistsResults.size() + ")");
                tabAlbums.setText(getString(R.string.albums) + " (" + arrayAlbumsResults.size() + ")");
                tabSongs.setText(getString(R.string.songs) + " (" + arraySongsResults.size() + ")");
            }
        });
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
            } catch (Exception e) {
                e.printStackTrace();
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
            } catch (Exception e) {
                e.printStackTrace();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void asyncExecSucceeded(int jobID) {
        if (iJobID == jobID) {
            updateFromItems();
        }
    }

    class SearchResultsPagerAdapter extends PagerAdapter {

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
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    /**
     * ***
     * ActionBar TabListener methods
     * ****
     */

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        return;
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        pager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        return;
    }

}
