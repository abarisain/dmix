/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.anpmech.mpd.item.Stream;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.StreamFetcher;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
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
import java.util.ListIterator;

public class StreamsFragment extends BrowseFragment<Stream> {

    public static final int DELETE = 102;

    public static final int EDIT = 101;

    private static final String TAG = "StreamsFragment";

    private final List<Stream> mStreams = new ArrayList<>();

    public StreamsFragment() {
        super(R.string.addStream, R.string.streamAdded, null);
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

    public void addEdit() {
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

        if (idx >= 0 && idx < mStreams.size()) {
            final Stream stream = mStreams.get(idx);
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
                .setTitle(idx < 0 ? R.string.addStream : R.string.editStream)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {

                    /**
                     * Checks the TextView for a getText string, if it exists, trims and returns
                     * it.
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
                        final String name = getText(nameEdit);
                        final String url = getText(urlEdit);

                        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(url)) {
                            if (idx >= 0 && idx < mStreams.size()) {
                                final int removedPos = mStreams.get(idx).getPos();
                                try {
                                    mApp.getMPD().editSavedStream(url, name, removedPos);
                                } catch (final IOException | MPDException e) {
                                    Log.e(TAG, "Failed to edit a saved stream.", e);
                                }
                                mStreams.remove(idx);
                                for (final Stream stream : mStreams) {
                                    if (stream.getPos() > removedPos) {
                                        stream.setPos(stream.getPos() - 1);
                                    }
                                }
                                mStreams.add(new Stream(url, name, mStreams.size()));
                            } else {
                                try {
                                    mApp.getMPD().saveStream(url, name);
                                } catch (final IOException | MPDException e) {
                                    Log.e(TAG, "Failed to save stream.", e);
                                }
                                mStreams.add(new Stream(url, name, mStreams.size()));
                            }
                            Collections.sort(mStreams);
                            replaceItems(mStreams);
                            if (streamUrlToAdd == null) {
                                updateList();
                            } else {
                                Toast.makeText(getActivity(), R.string.streamSaved,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        if (streamUrlToAdd != null) {
                            getActivity().finish();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (streamUrlToAdd != null) {
                            getActivity().finish();
                        }
                    }
                }).setOnCancelListener(new OnCancelListener() {
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
        mStreams.clear();

        /** Many users have playlist support disabled, no need for an exception. */
        if (mApp.getMPD().isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS)) {
            try {
                final ListIterator<Music> iterator = mApp.getMPD().getSavedStreams().listIterator();

                while (iterator.hasNext()) {
                    final Music stream = iterator.next();

                    mStreams.add(new Stream(stream.getName(), stream.getFullPath(),
                            iterator.nextIndex()));
                }
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to retrieve saved streams.", e);
            }
        } else {
            Log.w(TAG, "Streams fragment can't load streams, playlist support not enabled.");
        }

        Collections.sort(mStreams);
        replaceItems(mStreams);
    }

    @Override
    protected Artist getArtist(final Stream item) {
        return null;
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
        if (info.id >= 0 && info.id < mStreams.size()) {
            final MenuItem editItem = menu.add(Menu.NONE, EDIT, 0, R.string.editStream);
            editItem.setOnMenuItemClickListener(this);
            final MenuItem addAndReplaceItem =
                    menu.add(Menu.NONE, DELETE, 0, R.string.deleteStream);
            addAndReplaceItem.setOnMenuItemClickListener(this);
        }
    }

    @Override
    protected void onCreateToolbarMenu() {
        super.onCreateToolbarMenu();
        mToolbar.inflateMenu(R.menu.mpd_streamsmenu);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View v = super.onCreateView(inflater, container, savedInstanceState);

        v.findViewById(R.id.streamAddButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                addEdit();
            }
        });

        return v;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final int infoId = (int) ((AdapterContextMenuInfo) item.getMenuInfo()).id;
        boolean clicked = true;

        switch (item.getItemId()) {
            case EDIT:
                addEdit(infoId, null);
                break;
            case DELETE:
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.deleteStream);
                builder.setMessage(
                        getResources().getString(R.string.deleteStreamPrompt,
                                mItems.get(infoId).getName()));

                final OnClickListener oDialogClickListener
                        = new DeleteDialogClickListener(infoId);
                builder.setNegativeButton(android.R.string.no, oDialogClickListener);
                builder.setPositiveButton(R.string.deleteStream, oDialogClickListener);
                try {
                    builder.show();
                } catch (final BadTokenException e) {
                    // Can't display it. Don't care.
                }
                break;
            default:
                clicked = super.onMenuItemClick(item);
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

    class DeleteDialogClickListener implements OnClickListener {

        private final int mItemIndex;

        DeleteDialogClickListener(final int itemIndex) {
            super();
            mItemIndex = itemIndex;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                try {
                    mApp.getMPD().removeSavedStream(mStreams.get(mItemIndex).getPos());
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to removed a saved stream.", e);
                }

                final String name = mItems.get(mItemIndex).getName();
                Tools.notifyUser(R.string.streamDeleted, name);
                mItems.remove(mItemIndex);
                mStreams.remove(mItemIndex);
                updateFromItems();
            }
        }
    }
}
