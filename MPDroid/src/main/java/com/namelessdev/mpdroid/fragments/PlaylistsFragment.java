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

package com.namelessdev.mpdroid.fragments;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.PlaylistFile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PlaylistsFragment extends BrowseFragment {
    class DialogClickListener implements OnClickListener {
        private final int itemIndex;

        DialogClickListener(int itemIndex) {
            this.itemIndex = itemIndex;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
                case AlertDialog.BUTTON_POSITIVE:
                    String playlist = items.get(itemIndex).getName();
                    try {
                        app.oMPDAsyncHelper.oMPD.getPlaylist().removePlaylist(playlist);
                        if (isAdded()) {
                            Tools.notifyUser(R.string.playlistDeleted, playlist);
                        }
                        items.remove(itemIndex);
                    } catch (final MPDServerException e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.deletePlaylist);
                        builder.setMessage(
                                getResources().getString(R.string.failedToDelete, playlist));
                        builder.setPositiveButton(android.R.string.cancel, null);

                        try {
                            builder.show();
                        } catch (final BadTokenException ignored) {
                            // Can't display it. Don't care.
                        }
                    }
                    updateFromItems();
                    break;

            }
        }
    }

    public static final int EDIT = 101;

    public static final int DELETE = 102;

    public PlaylistsFragment() {
        super(R.string.addPlaylist, R.string.playlistAdded, null);
    }

    private static final String TAG = "PlaylistsFragment";

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            app.oMPDAsyncHelper.oMPD.add((PlaylistFile) item, replace, play);
            if (isAdded()) {
                Tools.notifyUser(irAdded, item);
            }

        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(Item item, String playlist) {
    }

    @Override
    protected void asyncUpdate() {
        try {
            items = app.oMPDAsyncHelper.oMPD.getPlaylists(true);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    public int getLoadingText() {
        return R.string.loadingPlaylists;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        android.view.MenuItem editItem = menu.add(ContextMenu.NONE, EDIT, 0, R.string.editPlaylist);
        editItem.setOnMenuItemClickListener(this);
        android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, DELETE, 0,
                R.string.deletePlaylist);
        addAndReplaceItem.setOnMenuItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                new StoredPlaylistFragment().init(items.get(position).getName()),
                "stored_playlist");
    }

    @Override
    public boolean onMenuItemClick(android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case EDIT:
                Intent intent = new Intent(getActivity(), PlaylistEditActivity.class);
                intent.putExtra("playlist", items.get((int) info.id).getName());
                startActivity(intent);
                return true;

            case DELETE:
                String playlist = items.get((int) info.id).getName();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.deletePlaylist);
                builder.setMessage(
                        getResources().getString(R.string.deletePlaylistPrompt, playlist));

                DialogClickListener oDialogClickListener = new DialogClickListener((int) info.id);
                builder.setNegativeButton(android.R.string.no, oDialogClickListener);
                builder.setPositiveButton(R.string.deletePlaylist, oDialogClickListener);

                try {
                    builder.show();
                } catch (final BadTokenException ignored) {
                    // Can't display it. Don't care.
                }
                break;
            default:
                super.onMenuItemClick(item);
                break;
        }
        return false;
    }
}
