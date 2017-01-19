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

package com.namelessdev.mpdroid.fragments;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.commandresponse.StreamResponse;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.PlaylistFile;
import com.anpmech.mpd.item.Stream;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.playlists.Playlist;
import com.namelessdev.mpdroid.playlists.PlaylistEntry;
import com.namelessdev.mpdroid.playlists.Playlists;
import com.namelessdev.mpdroid.tools.StreamFetcher;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This fragment is used to display a list of Streams in a special playlist on the connected MPD
 * server.
 *
 * This fragment is used to specifically store {@link Stream} URLs with using the URL fragment as
 * a name place holder. These streams are stored in the {@link Stream#PLAYLIST_NAME} playlist.
 */
public class StreamsFragment extends BrowseFragment<Stream> {

    private static final int DELETE = 102;

    private static final int EDIT = 101;

    /**
     * The class log identifier.
     */
    private static final String TAG = "StreamsFragment";

    /**
     * This list shows the list of streams prior to ordering.
     */
    private final List<Stream> mUnordered = new ArrayList<>();

    public StreamsFragment() {
        super(R.string.addStream, R.string.streamAdded);
    }

    @Override
    protected void add(final Stream item, final boolean replace, final boolean play) {
        try {
            mApp.getMPD().addStream(
                    StreamFetcher.instance().get(item.getUrl(), item.getName()), replace, play);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add stream.", e);
        }
    }

    @Override
    protected void add(final Stream item, final PlaylistFile playlist) {
    }

    private void addEdit() {
        addEdit(-1, null);
    }

    /*
     * StreamUrlToAdd is set when coming from the browser with
     * "android.intent.action.VIEW"
     */
    public void addEdit(final int idx, final CharSequence streamUrlToAdd) {
        final LayoutInflater factory = LayoutInflater.from(getActivity());
        final View view = factory.inflate(R.layout.stream_dialog, null);
        final EditText nameEdit = (EditText) view.findViewById(R.id.name_edit);
        final EditText urlEdit = (EditText) view.findViewById(R.id.url_edit);
        final int streamTitle =  idx < 0 ? R.string.addStream : R.string.editStream;

        if (idx >= 0 && idx < mUnordered.size()) {
            final Stream stream = mUnordered.get(idx);
            if (null != nameEdit) {
                nameEdit.setText(stream.getName());
            }
            if (null != urlEdit) {
                urlEdit.setText(stream.getUrl());
            }
        } else if (streamUrlToAdd != null && urlEdit != null) {
            urlEdit.setText(streamUrlToAdd);
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(streamTitle)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new AddEditOnClickListener(nameEdit, urlEdit, idx, streamUrlToAdd))
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (streamUrlToAdd != null) {
                            getActivity().finish();
                        }
                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialog) {
                        if (streamUrlToAdd != null) {
                            getActivity().finish();
                        }
                    }
                }).show();
    }

    @Override
    protected void asyncUpdate() {
        StreamResponse streamResponse = new StreamResponse();

        /** Many users have playlist support disabled, no need for an exception. */
        if (mApp.getMPD().isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS)) {
            try {
                streamResponse = mApp.getMPD().getSavedStreams();
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to retrieve saved streams.", e);
            }
        } else {
            Log.w(TAG, "Streams fragment can't load streams, playlist support not enabled.");
        }

        final List<Stream> streams = new ArrayList<>(streamResponse);
        mUnordered.clear();
        mUnordered.addAll(streams);
        Collections.sort(streams);
        replaceItems(streams);
    }

    @Override
    protected Artist getArtist(final Stream item) {
        return null;
    }

    /**
     * This method returns the default string resource.
     *
     * @return The default string resource.
     */
    @Override
    @StringRes
    public int getDefaultTitle() {
        return R.string.streams;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.streams_list;
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingStreams;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(mList);
        updateList();
        getActivity().setTitle(R.string.streams);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        if (info.id >= 0L && info.id < (long) mItems.size()) {
            final MenuItem editItem = menu.add(Menu.NONE, EDIT, 0, R.string.editStream);
            editItem.setOnMenuItemClickListener(this);
            final MenuItem addAndReplaceItem =
                    menu.add(Menu.NONE, DELETE, 0, R.string.deleteStream);
            addAndReplaceItem.setOnMenuItemClickListener(this);
        }

        menu.setGroupEnabled(PLAYLIST_ADD_GROUP, false);
    }

    @Override
    protected void onCreateToolbarMenu() {
        super.onCreateToolbarMenu();
        mToolbar.inflateMenu(R.menu.mpd_streamsmenu);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        view.findViewById(R.id.streamAddButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                addEdit();
            }
        });

        return view;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        addAdapterItem(parent, position);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        // This is the index in relation to the ordered items.
        final int infoId = (int) ((AdapterContextMenuInfo) item.getMenuInfo()).id;
        final Stream stream = mItems.get(infoId);
        // Index should be used for the index given on the server.
        final int index = mUnordered.indexOf(stream);
        boolean clicked = true;

        switch (item.getItemId()) {
            case EDIT:
                addEdit(index, null);
                break;
            case DELETE:
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final OnClickListener oDialogClickListener
                        = new DeleteDialogClickListener(getContext(), index, mUnordered);

                builder.setTitle(R.string.deleteStream);
                builder.setMessage(
                        getResources().getString(R.string.deleteStreamPrompt, stream.getName()));
                builder.setNegativeButton(android.R.string.no, Tools.NOOP_CLICK_LISTENER);
                builder.setPositiveButton(R.string.deleteStream, oDialogClickListener);
                try {
                    builder.show();
                } catch (final BadTokenException ignored) {
                    // Can't display it. Don't care.
                }
                break;
            default:
                clicked = super.onMenuItemClick(item);
                break;
        }
        return clicked;
    }

    @Override
    protected boolean onToolbarMenuItemClick(final MenuItem item) {
        final boolean clicked;

        switch (item.getItemId()) {
            case R.id.add:
                addEdit();
                clicked = true;
                break;
            default:
                clicked = super.onToolbarMenuItemClick(item);
                break;
        }

        return clicked;
    }

    /**
     * Called when a stored playlist has been modified, renamed, created or deleted.
     */
    @Override
    public void storedPlaylistChanged() {
        super.storedPlaylistChanged();

        updateList();
    }

    /**
     * The click listener used when {@link DialogInterface#BUTTON_POSITIVE} is clicked when a
     * stream is to be deleted.
     */
    private static final class DeleteDialogClickListener implements OnClickListener {

        /**
         * The MPDApplication instance.
         */
        private final MPDApplication mApp;

        /**
         * The index of the stream to be removed.
         */
        private final int mItemIndex;

        /**
         * The name of the stream to be removed.
         */
        private final String mStreamName;

        /**
         * Sole constructor.
         *
         * @param context   The context used to get the {@link MPDApplication} instance.
         * @param itemIndex The index of the item to remove.
         * @param streams   The current list of streams.
         */
        private DeleteDialogClickListener(final Context context, final int itemIndex,
                final List<Stream> streams) {
            super();

            mApp = (MPDApplication) context.getApplicationContext();
            mItemIndex = itemIndex;
            mStreamName = streams.get(itemIndex).getName();
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                try {
                    mApp.getMPD().removeSavedStream(mItemIndex);
                    Tools.notifyUser(R.string.streamDeleted, mStreamName);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to removed a saved stream.", e);
                }
            }
        }
    }

    private final class AddEditOnClickListener implements OnClickListener {

        private final int mIndex;

        private final EditText mNameEdit;

        private final CharSequence mStreamUrlToAdd;

        private final EditText mUrlEdit;

        private AddEditOnClickListener(final EditText nameEdit, final EditText urlEdit,
                final int index, final CharSequence streamUrlToAdd) {
            super();

            mNameEdit = nameEdit;
            mUrlEdit = urlEdit;
            mIndex = index;
            mStreamUrlToAdd = streamUrlToAdd;
        }

        /**
         * Checks the TextView for a getText string, if it exists, trims and returns it.
         *
         * @param textView The TextView to check for a getText() string.
         * @return A trimmed getText string.
         */
        private String getText(final TextView textView) {
            final String result;

            if (textView == null) {
                result = null;
            } else {
                result = textView.getText().toString().trim();
            }

            return result;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            final String name = getText(mNameEdit);
            String url = getText(mUrlEdit);

            // if URL is a playlist, use first entry as stream URL
            final Playlist playlist = Playlists.create(url);
            if (playlist != null) {
                final List<PlaylistEntry> playlistEntries = playlist.getEntries();
                if (playlistEntries != null && playlistEntries.size() > 0) {
                    url = playlistEntries.get(0).getUrl();
                }
            }

            mApp.addConnectionLock(this);

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(url)) {
                if (mIndex >= 0 && mIndex < mItems.size()) {
                    final MPD mpd = mApp.getMPD();

                    try {
                        mpd.removeSavedStream(mIndex);
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to edit a saved stream.", e);
                    }
                }

                try {
                    mApp.getMPD().saveStream(url, name);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to save stream.", e);
                }

                if (mStreamUrlToAdd != null) {
                    Toast.makeText(getActivity(), R.string.streamSaved, Toast.LENGTH_SHORT).show();
                }
            }

            mApp.removeConnectionLock(this);
            if (mStreamUrlToAdd != null) {
                getActivity().finish();
            }
        }
    }
}
