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

import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.PlaylistFile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.io.IOException;

public class PlaylistsFragment extends BrowseFragment {

    public static final int DELETE = 102;

    public static final int EDIT = 101;

    private static final String TAG = "PlaylistsFragment";

    public PlaylistsFragment() {
        super(R.string.addPlaylist, R.string.playlistAdded, null);
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        try {
            mApp.oMPDAsyncHelper.oMPD.add((PlaylistFile) item, replace, play);
            if (isAdded()) {
                Tools.notifyUser(mIrAdded, item);
            }

        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
    }

    @Override
    protected void asyncUpdate() {
        try {
            mItems = mApp.oMPDAsyncHelper.oMPD.getPlaylists(true);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingPlaylists;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuItem editItem = menu
                .add(Menu.NONE, EDIT, 0, R.string.editPlaylist);
        editItem.setOnMenuItemClickListener(this);
        final MenuItem addAndReplaceItem = menu.add(Menu.NONE, DELETE, 0,
                R.string.deletePlaylist);
        addAndReplaceItem.setOnMenuItemClickListener(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        ((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(
                new StoredPlaylistFragment().init(mItems.get(position).getName()),
                "stored_playlist");
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case EDIT:
                final Intent intent = new Intent(getActivity(), PlaylistEditActivity.class);
                intent.putExtra("playlist", mItems.get((int) info.id).getName());
                startActivity(intent);
                return true;

            case DELETE:
                final String playlist = mItems.get((int) info.id).getName();

                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.deletePlaylist);
                builder.setMessage(
                        getResources().getString(R.string.deletePlaylistPrompt, playlist));

                final OnClickListener oDialogClickListener = new DialogClickListener(
                        (int) info.id);
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

    class DialogClickListener implements OnClickListener {

        private final int mItemIndex;

        DialogClickListener(final int itemIndex) {
            super();
            mItemIndex = itemIndex;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                final String playlist = mItems.get(mItemIndex).getName();
                try {
                    mApp.oMPDAsyncHelper.oMPD.getPlaylist().removePlaylist(playlist);
                    if (isAdded()) {
                        Tools.notifyUser(R.string.playlistDeleted, playlist);
                    }
                    mItems.remove(mItemIndex);
                } catch (final IOException | MPDException e) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            }
        }
    }
}
