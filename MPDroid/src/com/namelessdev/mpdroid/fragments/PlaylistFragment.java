package com.namelessdev.mpdroid.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.*;
import android.view.View.OnClickListener;
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
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper.CoverRetrievers;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.tools.Tools;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import java.util.*;

public class PlaylistFragment extends ListFragment implements StatusChangeListener, OnMenuItemClickListener {
    private ArrayList<HashMap<String, Object>> songlist;
    private List<Music> musics;

    private MPDApplication app;
    private DragSortListView list;
    private ActionMode actionMode;
    private SearchView searchView;
    private String filter = null;
    private PopupMenu popupMenu;
    private Integer popupSongID;

    private int lastPlayingID = -1;

    public static final int MAIN = 0;
    public static final int CLEAR = 1;
    public static final int MANAGER = 3;
    public static final int SAVE = 4;
    public static final int EDIT = 2;

    public PlaylistFragment() {
        super();
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = (MPDApplication) getActivity().getApplication();
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
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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
        DragSortController controller = new DragSortController(list);
        controller.setDragHandleId(R.id.icon);
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
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
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
                                positions[j] = ((Integer) ((HashMap<String, Object>) adapter.getItem(i)).get("songid")).intValue();
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
                                positions[j] = ((Integer) ((HashMap<String, Object>) adapter.getItem(i)).get("songid")).intValue();
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
        if ((getActivity() instanceof MainMenuActivity && app.isLightNowPlayingThemeSelected()) ||
                (!(getActivity() instanceof MainMenuActivity) && app.isLightThemeSelected())) {
            list.setCacheColorHint(getResources().getColor(android.R.color.background_light));
        } else {
            list.setCacheColorHint(getResources().getColor(R.color.nowplaying_background));
        }
    }

    protected void update() {
        update(true);
    }

    protected void update(boolean forcePlayingIDRefresh) {
        try {
            MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
            songlist = new ArrayList<HashMap<String, Object>>();
            musics = playlist.getMusicList();
            if (lastPlayingID == -1 || forcePlayingIDRefresh)
                lastPlayingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
            // The position in the songlist of the currently played song
            int listPlayingID = -1;
            String tmpArtist = null;
            String tmpAlbum = null;
            String tmpAlbumArtist = null;
            String tmpTitle = null;
            for (Music m : musics) {
                if (m == null) {
                    continue;
                }
                tmpArtist = m.getArtist();
                tmpAlbum = m.getAlbum();
                tmpAlbumArtist = m.getAlbumArtist();
                tmpTitle = m.getTitle();
                if (filter != null) {
                    if (tmpArtist == null)
                        tmpArtist = "";
                    if (tmpAlbum == null)
                        tmpAlbum = "";
                    if (tmpAlbumArtist == null)
                        tmpAlbumArtist = "";
                    if (tmpTitle == null)
                        tmpTitle = "";
                    if (!tmpArtist.toLowerCase(Locale.getDefault()).contains(filter) &&
                            !tmpAlbum.toLowerCase(Locale.getDefault()).contains(filter) &&
                            !tmpAlbumArtist.toLowerCase(Locale.getDefault()).contains(filter) &&
                            !tmpTitle.toLowerCase(Locale.getDefault()).contains(filter)) {
                        continue;
                    }
                }
                HashMap<String, Object> item = new HashMap<String, Object>();
                item.put("songid", m.getSongId());
                item.put("_artist", tmpArtist);
                item.put("_album", tmpAlbum);
                if (m.isStream()) {
                    if (m.haveTitle()) {
                        item.put("title", tmpTitle);
                        if (Tools.isStringEmptyOrNull(m.getName())) {
                            item.put("artist", tmpArtist);
                        } else if (Tools.isStringEmptyOrNull(tmpArtist)) {
                            item.put("artist", m.getName());
                        } else {
                            item.put("artist", tmpArtist + " - " + m.getName());
                        }
                    } else {
                        item.put("title", m.getName());
                    }
                } else {
                    if (Tools.isStringEmptyOrNull(tmpAlbum)) {
                        item.put("artist", tmpArtist);
                    } else {
                        item.put("artist", tmpArtist + " - " + tmpAlbum);
                    }
                    item.put("title", tmpTitle);
                }

                if (m.getSongId() == lastPlayingID) {
                    item.put("play", android.R.drawable.ic_media_play);
                    // Lie a little. Scroll to the previous song than the one playing. That way it shows that there are other songs before
                    // it
                    listPlayingID = songlist.size() - 1;
                } else {
                    item.put("play", 0);
                }
                songlist.add(item);
            }

            final int finalListPlayingID = listPlayingID;

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    SimpleAdapter songs = new QueueAdapter(getActivity(), songlist, R.layout.playlist_queue_item, new String[]{
                            "play",
                            "title", "artist"}, new int[]{R.id.picture, android.R.id.text1, android.R.id.text2});

                    setListAdapter(songs);
                    if (actionMode != null)
                        actionMode.finish();

                    // Only scroll if there is a valid song to scroll to. 0 is a valid song but does not require scroll anyway.
                    // Also, only scroll if it's the first update. You don't want your playlist to scroll itself while you are looking at
                    // other
                    // stuff.
                    if (finalListPlayingID > 0)
                        setSelection(finalListPlayingID);
                }
            });

        } catch (MPDServerException e) {
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
                    Tools.notifyUser(getResources().getString(R.string.playlistCleared), getActivity());
                    ((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLM_EditPL:
                i = new Intent(getActivity(), PlaylistEditActivity.class);
                startActivity(i);
                return true;
            case R.id.PLM_Save:
                final EditText input = new EditText(getActivity());
                new AlertDialog.Builder(getActivity())
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

        MPDApplication app = (MPDApplication) getActivity().getApplication(); // Play selected Song

        @SuppressWarnings("unchecked")
        final Integer song = (Integer) ((HashMap<String, Object>) l.getAdapter().getItem(position)).get("songid");
        try {
            app.oMPDAsyncHelper.oMPD.skipToId(song);
        } catch (MPDServerException e) {
        }

    }

    public void scrollToNowPlaying() {
        for (HashMap<String, Object> song : songlist) {
            try {
                if (((Integer) song.get("songid")).intValue() == ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD
                        .getStatus()
                        .getSongId()) {
                    getListView().requestFocusFromTouch();
                    getListView().setSelection(songlist.indexOf(song));
                }
            } catch (MPDServerException e) {
            }
        }
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
        // Mark running track...
        for (HashMap<String, Object> song : songlist) {
            if (((Integer) song.get("songid")).intValue() == mpdStatus.getSongId())
                song.put("play", android.R.drawable.ic_media_play);
            else
                song.put("play", 0);

        }
        final SimpleAdapter adapter = (SimpleAdapter) getListAdapter();
        if (adapter != null)
            adapter.notifyDataSetChanged();

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
            HashMap<String, Object> itemFrom = songlist.get(from);
            Integer songID = (Integer) itemFrom.get("songid");
            try {
                app.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to);
            } catch (MPDServerException e) {
            }
        }
    };

    private OnClickListener itemMenuButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            popupSongID = (Integer) v.getTag();
            popupMenu = new PopupMenu(getActivity(), v);
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
                    Tools.notifyUser("Song moved to next in list", getActivity());
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLCX_moveFirst:
                try { // Move song to first in playlist
                    app.oMPDAsyncHelper.oMPD.getPlaylist().move(popupSongID, 0);
                    Tools.notifyUser("Song moved to first in list", getActivity());
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLCX_moveLast:
                try { // Move song to last in playlist
                    MPDStatus status = app.oMPDAsyncHelper.oMPD.getStatus();
                    app.oMPDAsyncHelper.oMPD.getPlaylist().move(popupSongID, status.getPlaylistLength() - 1);
                    Tools.notifyUser("Song moved to last in list", getActivity());
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.PLCX_removeFromPlaylist:
                try {
                    app.oMPDAsyncHelper.oMPD.getPlaylist().removeById(popupSongID);
                    Tools.notifyUser(getResources().getString(R.string.deletedSongFromPlaylist), getActivity());
                } catch (MPDServerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            default:
                return true;
        }
    }

    private class QueueAdapter extends SimpleAdapter {

        private List<CoverRetrievers> enabledRetrievers;
        private MPDApplication app;
        private SharedPreferences settings;
        private boolean lightTheme;

        public QueueAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);

            enabledRetrievers = null;

            app = (MPDApplication) getActivity().getApplication();
            settings = PreferenceManager.getDefaultSharedPreferences(app);
            lightTheme = app.isLightNowPlayingThemeSelected();
            if (settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true)) {
                enabledRetrievers = new ArrayList<CoverAsyncHelper.CoverRetrievers>();
                enabledRetrievers.add(CoverRetrievers.CACHE);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            view.findViewById(R.id.icon).setVisibility(filter == null ? View.VISIBLE : View.GONE);
            final View menuButton = view.findViewById(R.id.menu);
            if (convertView == null) {
                menuButton.setOnClickListener(itemMenuButtonListener);
                if (enabledRetrievers == null)
                    view.findViewById(R.id.cover).setVisibility(View.GONE);
            }

            final Map<String, ?> item = (Map<String, ?>) getItem(position);
            menuButton.setTag(item.get("songid"));
            if (enabledRetrievers != null) {
                final ImageView albumCover = (ImageView) view.findViewById(R.id.cover);
                final CoverAsyncHelper coverHelper = new CoverAsyncHelper(app, settings);
                coverHelper.setCoverRetrievers(enabledRetrievers);
                final int height = albumCover.getHeight();
                // If the list is not displayed yet, the height is 0. This is a problem, so set a fallback one.
                coverHelper.setCoverMaxSize(height == 0 ? 128 : height);
                final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(getActivity(), albumCover, lightTheme);
                final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) albumCover
                        .getTag(R.id.AlbumCoverDownloadListener);
                if (oldAcd != null) {
                    oldAcd.detach();
                }

                albumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
                coverHelper.addCoverDownloadListener(acd);
                coverHelper.downloadCover((String) item.get("_artist"), (String) item.get("_album"), null, null);
            }
            return view;
        }
    }

}
