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
import com.namelessdev.mpdroid.tools.StreamFetcher;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;
import org.a0z.mpd.item.Stream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamsFragment extends BrowseFragment {

    public static final int DELETE = 102;

    public static final int EDIT = 101;

    private static final String FILE_NAME = "streams.xml";

    private static final String SERVER_FILE_NAME = "streams.xml.gz";

    private static final String TAG = "StreamsFragment";

    ArrayList<Stream> mStreams = new ArrayList<>();

    public StreamsFragment() {
        super(R.string.addStream, R.string.streamAdded, null);
    }

    @Override
    protected void add(final Item item, final boolean replace, final boolean play) {
        try {
            final Stream stream = (Stream) item;
            mApp.oMPDAsyncHelper.oMPD.addStream(
                    StreamFetcher.instance().get(stream.getUrl(), stream.getName()),
                    replace, play);
            Tools.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add stream.", e);
        }
    }

    @Override
    protected void add(final Item item, final String playlist) {
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
        final int index = idx;
        if (index >= 0 && index < mStreams.size()) {
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
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final String name = null == nameEdit ? null : nameEdit.getText().toString()
                                .trim();
                        final String url = null == urlEdit ? null
                                : urlEdit.getText().toString().trim();
                        if (null != name && !name.isEmpty() && null != url && !url.isEmpty()) {
                            if (index >= 0 && index < mStreams.size()) {
                                final int removedPos = mStreams.get(idx).getPos();
                                try {
                                    mApp.oMPDAsyncHelper.oMPD
                                            .editSavedStream(url, name, removedPos);
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
                                    mApp.oMPDAsyncHelper.oMPD.saveStream(url, name);
                                } catch (final IOException | MPDException e) {
                                    Log.e(TAG, "Failed to save stream.", e);
                                }
                                mStreams.add(new Stream(url, name, mStreams.size()));
                            }
                            Collections.sort(mStreams);
                            mItems = mStreams;
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
        loadStreams();
    }

    @Override
    @StringRes
    public int getLoadingText() {
        return R.string.loadingStreams;
    }

    private List<Stream> loadOldStreams() {
        AbstractList<Stream> oldStreams = null;
        try {
            final InputStream in = mApp.openFileInput(FILE_NAME);
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(in, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("stream".equals(xpp.getName())) {
                        if (null == oldStreams) {
                            oldStreams = new ArrayList<>();
                        }
                        oldStreams.add(new Stream(xpp.getAttributeValue("", "name"), xpp
                                .getAttributeValue("", "url"), -1));
                    }
                }
                eventType = xpp.next();
            }
            in.close();
            // Now remove file - all streams will be added to MPD...
            mApp.deleteFile(FILE_NAME);
        } catch (final FileNotFoundException ignored) {
        } catch (final Exception e) {
            Log.e(TAG, "Error while loading streams", e);
        }
        return oldStreams;
    }

    private void loadStreams() {
        mStreams = new ArrayList<>();

        // Load streams stored in MPD Streams playlist...
        List<Music> mpdStreams = null;
        int iterator = 0;

        /** Many users have playlist support disabled, no need for an exception. */
        if (mApp.oMPDAsyncHelper.oMPD.isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS)) {
            try {
                mpdStreams = mApp.oMPDAsyncHelper.oMPD.getSavedStreams();
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to retrieve saved streams.", e);
            }
        } else {
            Log.w(TAG, "Streams fragment can't load streams, playlist support not enabled.");
            mpdStreams = Collections.emptyList();
        }

        if (null != mpdStreams) {
            for (final Music stream : mpdStreams) {
                mStreams.add(new Stream(stream.getName(), stream.getFullPath(), iterator));
                iterator++;
            }
        }

        // Load any OLD MPDroid streams, and also save these to MPD...
        final List<Stream> oldStreams = loadOldStreams();
        if (null != oldStreams) {
            for (final Stream stream : mStreams) {
                if (!mStreams.contains(stream)) {
                    try {
                        mApp.oMPDAsyncHelper.oMPD.saveStream(stream.getUrl(), stream.getName());
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to save a stream.", e);
                    }

                    stream.setPos(mStreams.size());
                    mStreams.add(stream);
                }
            }
        }
        Collections.sort(mStreams);
        mItems = mStreams;
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
            final Stream stream = mStreams.get((int) info.id);
            final MenuItem editItem = menu.add(Menu.NONE, EDIT, 0,
                    R.string.editStream);
            editItem.setOnMenuItemClickListener(this);
            final MenuItem addAndReplaceItem = menu.add(Menu.NONE, DELETE, 0,
                    R.string.deleteStream);
            addAndReplaceItem.setOnMenuItemClickListener(this);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_streamsmenu, menu);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case EDIT:
                addEdit((int) info.id, null);
                break;
            case DELETE:
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.deleteStream);
                builder.setMessage(
                        getResources().getString(R.string.deleteStreamPrompt,
                                mItems.get((int) info.id).getName()));

                final OnClickListener oDialogClickListener
                        = new DeleteDialogClickListener(
                        (int) info.id);
                builder.setNegativeButton(android.R.string.no, oDialogClickListener);
                builder.setPositiveButton(R.string.deleteStream, oDialogClickListener);
                try {
                    builder.show();
                } catch (final BadTokenException e) {
                    // Can't display it. Don't care.
                }
                break;
            default:
                return super.onMenuItemClick(item);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                addEdit();
                return true;
            default:
                return false;
        }
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
                    mApp.oMPDAsyncHelper.oMPD.removeSavedStream(mStreams.get(mItemIndex).getPos());
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
