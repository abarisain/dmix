package com.namelessdev.mpdroid.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.*;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SearchView.OnQueryTextListener;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.models.AbstractPlaylistMusic;
import com.namelessdev.mpdroid.models.PlaylistSong;
import com.namelessdev.mpdroid.models.PlaylistStream;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.views.holders.PlayQueueViewHolder;
import org.a0z.mpd.*;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.util.Log.e;

public class PlaylistFragment extends ListFragment implements StatusChangeListener, OnMenuItemClickListener {
    private ArrayList<AbstractPlaylistMusic> songlist;
    private MPDApplication app;
    private DragSortListView list;
    private ActionMode actionMode;
    private SearchView searchView;
    private String filter = null;
    private PopupMenu popupMenu;
    private Integer popupSongID;
    private DragSortController controller;
    private FragmentActivity activity;
    private static final boolean DEBUG = false;
    private int lastPlayingID = -1;

    public PlaylistFragment() {
        super();
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.activity = getActivity();
        app = (MPDApplication) activity.getApplication();
        refreshListColorCacheHint();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.playlist_activity, container, false);
        searchView = (SearchView) view.findViewById(R.id.search);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                // Hide the keyboard and give focus to the list
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                list.requestFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter = newText;
                if ("".equals(newText))
                    filter = null;
                if (filter != null)
                    filter = filter.toLowerCase();
                list.setDragEnabled(filter == null);
                update(false);
                return false;
            }
        });
        list = (DragSortListView) view.findViewById(android.R.id.list);
        list.requestFocus();
        list.setDropListener(onDrop);
        controller = new DragSortController(list);
        controller.setDragHandleId(R.id.cover);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setDragInitMode(1);

        list.setFloatViewManager(controller);
        list.setOnTouchListener(controller);
        list.setDragEnabled(true);

        refreshListColorCacheHint();
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public boolean onPrepareActionMode(ActionMode mode, android.view.Menu menu) {
                actionMode = mode;
                controller.setSortEnabled(false);
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                controller.setSortEnabled(true);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, android.view.Menu menu) {
                final android.view.MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.mpd_queuemenu, menu);
                return true;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) {

                final SparseBooleanArray checkedItems = list.getCheckedItemPositions();
                final int count = list.getCount();
                final ListAdapter adapter = list.getAdapter();
                int j = 0;
                final int positions[];

                switch (item.getItemId()) {

                    case R.id.menu_delete:
                        positions = new int[list.getCheckedItemCount()];
                        for (int i = 0; i < count && j < positions.length; i++) {
                            if (checkedItems.get(i)) {
                                positions[j] = ((AbstractPlaylistMusic) adapter.getItem(i)).getSongId();
                                j++;
                            }
                        }

                        app.oMPDAsyncHelper.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    app.oMPDAsyncHelper.oMPD.getPlaylist().removeById(positions);
                                } catch (MPDServerException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        mode.finish();
                        return true;
                    case R.id.menu_crop:
                        positions = new int[list.getCount() - list.getCheckedItemCount()];
                        for (int i = 0; i < count && j < positions.length; i++) {
                            if (!checkedItems.get(i)) {
                                positions[j] = ((AbstractPlaylistMusic) adapter.getItem(i)).getSongId();
                                j++;
                            }
                        }

                        app.oMPDAsyncHelper.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    app.oMPDAsyncHelper.oMPD.getPlaylist().removeById(positions);
                                } catch (MPDServerException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                final int selectCount = list.getCheckedItemCount();
                if (selectCount == 0)
                    mode.finish();
                if (selectCount == 1) {
                    mode.setTitle(R.string.actionSongSelected);
                } else {
                    mode.setTitle(getString(R.string.actionSongsSelected, selectCount));
                }
            }
        });

        return view;
    }

    private void refreshListColorCacheHint() {
        if (app == null || list == null)
            return;
        if (app.isLightThemeSelected()) {
            list.setCacheColorHint(getResources().getColor(android.R.color.background_light));
        } else {
            list.setCacheColorHint(getResources().getColor(R.color.nowplaying_background));
        }
    }

    public void update() {
        update(true);
    }

    protected synchronized void update(boolean forcePlayingIDRefresh) {
        try {
            MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
            final ArrayList<AbstractPlaylistMusic> newSonglist = new ArrayList<AbstractPlaylistMusic>();
            List<Music> musics = playlist.getMusicList();
            if (lastPlayingID == -1 || forcePlayingIDRefresh)
                lastPlayingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
            // The position in the songlist of the currently played song
            int listPlayingID = -1;
            // Copy list to avoid concurrent exception
            for (Music m : new ArrayList<Music>(musics)) {
                if (m == null) {
                    continue;
                }

                AbstractPlaylistMusic item = m.isStream() ? new PlaylistStream(m) : new PlaylistSong(m);

                if (filter != null) {
                    if (!(item.getAlbumArtist() != null ? item.getAlbumArtist() : "").toLowerCase(Locale.getDefault()).contains(filter) &&
                            !(item.getAlbum() != null ? item.getAlbum() : "").toLowerCase(Locale.getDefault()).contains(filter) &&
                            !(item.getTitle() != null ? item.getTitle() : "").toLowerCase(Locale.getDefault()).contains(filter)) {
                        continue;
                    }
                }

                if (item.getSongId() == lastPlayingID) {
                    item.setCurrentSongIconRefID(android.R.drawable.ic_media_play);
                    // Lie a little. Scroll to the previous song than the one playing. That way it shows that there are other songs before
                    // it
                    listPlayingID = newSonglist.size() - 1;
                } else {
                    item.setCurrentSongIconRefID(0);
                }
                newSonglist.add(item);

                final int finalListPlayingID = listPlayingID;

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        ArrayAdapter songs = new QueueAdapter(activity, newSonglist, R.layout.playlist_queue_item);
                        setListAdapter(songs);
                        songlist = newSonglist;
                        songs.notifyDataSetChanged();
                        if (actionMode != null)
                            actionMode.finish();

                        // Only scroll if there is a valid song to scroll to. 0 is a valid song but does not require scroll anyway.
                        // Also, only scroll if it's the first update. You don't want your playlist to scroll itself while you are looking at
                        // other
                        // stuff.
                        if (finalListPlayingID > 0 && getView() != null)
                            setSelection(finalListPlayingID);
                    }
                });
            }

        } catch (MPDServerException e) {
            e(PlaylistFragment.class.getSimpleName(), "Playlist update failure : " + e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        new Thread(new Runnable() {
            public void run() {
                update();
            }
        }).start();
    }

    @Override
    public void onPause() {
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
        super.onPause();
    }

    /*
     * Create Menu for Playlist View
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_playlistmenu, menu);
        menu.removeItem(R.id.PLM_EditPL);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu actions...
        Intent i;
        switch (item.getItemId()) {
            case R.id.PLM_Clear:
                try {
                    app.oMPDAsyncHelper.oMPD.getPlaylist().clear();
                    songlist.clear();
                    if (isAdded()) {
                        Tools.notifyUser(getResources().getString(R.string.playlistCleared), activity);
                    }
                    ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLM_EditPL:
                i = new Intent(activity, PlaylistEditActivity.class);
                startActivity(i);
                return true;
            case R.id.PLM_Save:
                final EditText input = new EditText(activity);
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.playlistName)
                        .setMessage(R.string.newPlaylistPrompt)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                final String name = input.getText().toString().trim();
                                if (null != name && name.length() > 0) {
                                    app.oMPDAsyncHelper.execAsync(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                app.oMPDAsyncHelper.oMPD.getPlaylist().savePlaylist(name);
                                            } catch (MPDServerException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            default:
                return false;
        }

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        MPDApplication app = (MPDApplication) activity.getApplication(); // Play selected Song

        @SuppressWarnings("unchecked")
        final Integer song = ((AbstractPlaylistMusic) l.getAdapter().getItem(position)).getSongId();
        try {
            app.oMPDAsyncHelper.oMPD.skipToId(song);
        } catch (MPDServerException e) {
        }

    }

    public void scrollToNowPlaying() {

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    MPD mpd = ((MPDApplication) activity.getApplication()).oMPDAsyncHelper.oMPD;
                    return mpd.getStatus().getSongPos();
                } catch (MPDServerException e) {
                    e(PlaylistFragment.class.getSimpleName(), "Cannot find the current playing song position : " + e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Integer songIndex) {
                if (songIndex != null) {

                    if (activity instanceof MainMenuActivity) {
                        ((MainMenuActivity) activity).switchMode(MainMenuActivity.DisplayMode.MODE_QUEUE);
                    }

                    getListView().requestFocusFromTouch();
                    getListView().setSelection(songIndex);
                    getListView().clearFocus();
                } else {
                    Log.d(PlaylistFragment.class.getSimpleName(), "Missing list item : " + songIndex);
                }
            }
        }.execute();
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        // TODO Auto-generated method stub

    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
        update();

    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        if (songlist != null) {
            // Mark running track...
            for (AbstractPlaylistMusic song : songlist) {
                int newPlay;
                if ((song.getSongId()) == mpdStatus.getSongId()) {
                    newPlay = android.R.drawable.ic_media_play;
                } else {
                    newPlay = 0;
                }
                if (song.getCurrentSongIconRefID() != newPlay) {
                    song.setCurrentSongIconRefID(newPlay);
                    refreshPlaylistItemView(song);
                }
            }
        }
    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void repeatChanged(boolean repeating) {
        // TODO Auto-generated method stub

    }

    @Override
    public void randomChanged(boolean random) {
        // TODO Auto-generated method stub

    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        // TODO Auto-generated method stub

    }

    @Override
    public void libraryStateChanged(boolean updating) {
        // TODO Auto-generated method stub

    }

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        public void drop(int from, int to) {
            if (from == to || filter != null) {
                return;
            }
            AbstractPlaylistMusic itemFrom = songlist.get(from);
            Integer songID = itemFrom.getSongId();
            try {
                app.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to);
            } catch (MPDServerException e) {
            }
        }
    };

    private View.OnClickListener itemMenuButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            popupSongID = (Integer) v.getTag();
            popupMenu = new PopupMenu(activity, v);
            popupMenu.getMenuInflater().inflate(R.menu.mpd_playlistcnxmenu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(PlaylistFragment.this);
            popupMenu.show();
        }
    };

    @Override
    public boolean onMenuItemClick(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.PLCX_SkipToHere:
                // skip to selected Song
                try {
                    app.oMPDAsyncHelper.oMPD.skipToId(popupSongID);
                } catch (MPDServerException e) {
                }
                return true;
            case R.id.PLCX_playNext:
                try { // Move song to next in playlist
                    MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
                    if (popupSongID < status.getSongPos()) {
                        app.oMPDAsyncHelper.oMPD.getPlaylist().move(popupSongID, status.getSongPos());
                    } else {
                        app.oMPDAsyncHelper.oMPD.getPlaylist().move(popupSongID, status.getSongPos() + 1);
                    }
                    Tools.notifyUser("Song moved to next in list", activity);
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLCX_moveFirst:
                try { // Move song to first in playlist
                    app.oMPDAsyncHelper.oMPD.getPlaylist().move(popupSongID, 0);
                    Tools.notifyUser("Song moved to first in list", activity);
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLCX_moveLast:
                try { // Move song to last in playlist
                    MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
                    app.oMPDAsyncHelper.oMPD.getPlaylist().move(popupSongID, status.getPlaylistLength() - 1);
                    Tools.notifyUser("Song moved to last in list", activity);
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLCX_removeFromPlaylist:
                try {
                    app.oMPDAsyncHelper.oMPD.getPlaylist().removeById(popupSongID);
                    if (isAdded()) {
                        Tools.notifyUser(getResources().getString(R.string.deletedSongFromPlaylist), activity);
                    }
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLCX_removeAlbumFromPlaylist:
                try {
                    if (DEBUG)
                        Log.d(PlaylistFragment.class.getSimpleName(), "Remove Album " + popupSongID);
                    app.oMPDAsyncHelper.oMPD.getPlaylist().removeAlbumById(popupSongID);
                    if (isAdded()) {
                        Tools.notifyUser(getResources().getString(R.string.deletedSongFromPlaylist), activity);
                    }
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            default:
                return true;
        }
    }

    private class QueueAdapter extends ArrayAdapter {

        private MPDApplication app;
        private SharedPreferences settings;
        private boolean lightTheme;

        public QueueAdapter(Context context, List<?> data, int resource) {
            super(context, resource, data);

            app = (MPDApplication) activity.getApplication();
            settings = PreferenceManager.getDefaultSharedPreferences(app);
            lightTheme = app.isLightThemeSelected();
        }

        @SuppressWarnings("unchecked")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PlayQueueViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new PlayQueueViewHolder();
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.playlist_queue_item, null);
                viewHolder.artist = (TextView) convertView.findViewById(android.R.id.text2);
                viewHolder.title = (TextView) convertView.findViewById(android.R.id.text1);
                viewHolder.play = (ImageView) convertView.findViewById(R.id.picture);
                viewHolder.cover = (ImageView) convertView.findViewById(R.id.cover);
                viewHolder.coverHelper = new CoverAsyncHelper(app, settings);
                final int height = viewHolder.cover.getHeight();
                // If the list is not displayed yet, the height is 0. This is a problem, so set a fallback one.
                viewHolder.coverHelper.setCoverMaxSize(height == 0 ? 128 : height);
                final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(activity, viewHolder.cover, lightTheme);
                final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) viewHolder.cover
                        .getTag(R.id.AlbumCoverDownloadListener);
                if (oldAcd != null) {
                    oldAcd.detach();
                }
                viewHolder.cover.setTag(R.id.AlbumCoverDownloadListener, acd);
                viewHolder.cover.setTag(R.id.CoverAsyncHelper, viewHolder.coverHelper);
                viewHolder.coverHelper.addCoverDownloadListener(acd);
                viewHolder.menuButton = convertView.findViewById(R.id.menu);
                viewHolder.menuButton.setOnClickListener(itemMenuButtonListener);
                viewHolder.icon = convertView.findViewById(R.id.icon);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (PlayQueueViewHolder) convertView.getTag();
            }

            AbstractPlaylistMusic music = (AbstractPlaylistMusic) getItem(position);

            viewHolder.artist.setText(music.getPlaylistSubLine());
            viewHolder.title.setText(music.getPlayListMainLine());
            viewHolder.icon.setVisibility(filter == null ? View.VISIBLE : View.GONE);
            viewHolder.menuButton.setTag(music.getSongId());
            viewHolder.play.setImageResource(music.getCurrentSongIconRefID());

            if (music.isForceCoverRefresh() || viewHolder.cover.getTag() == null || !viewHolder.cover.getTag().equals(music.getAlbumInfo().getKey())) {
                if (!music.isForceCoverRefresh()) {
                    viewHolder.cover.setImageResource(lightTheme ? R.drawable.no_cover_art_light : R.drawable.no_cover_art);
                }
                music.setForceCoverRefresh(false);
                viewHolder.coverHelper.downloadCover(music.getAlbumInfo(), false);
            }
            return convertView;
        }
    }

    private void refreshPlaylistItemView(final AbstractPlaylistMusic... playlistSongs) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                int start = list.getFirstVisiblePosition();
                for (int i = start, j = list.getLastVisiblePosition(); i <= j; i++) {
                    AbstractPlaylistMusic playlistMusic = (AbstractPlaylistMusic) list.getAdapter().getItem(i);
                    for (AbstractPlaylistMusic song : playlistSongs) {
                        if (playlistMusic.getSongId() == song.getSongId()) {
                            View view = list.getChildAt(i - start);
                            list.getAdapter().getView(i, view, list);
                        }
                    }
                }
            }
        }.execute();
    }

    public void updateCover(AlbumInfo albumInfo) {

        List<AbstractPlaylistMusic> musicsToBeUpdated = new ArrayList<AbstractPlaylistMusic>();

        for (AbstractPlaylistMusic playlistMusic : songlist) {
            if (playlistMusic.getAlbumInfo().equals(albumInfo)) {
                playlistMusic.setForceCoverRefresh(true);
                musicsToBeUpdated.add(playlistMusic);
            }
        }
        refreshPlaylistItemView(musicsToBeUpdated.toArray(new AbstractPlaylistMusic[musicsToBeUpdated.size()]));
    }
}