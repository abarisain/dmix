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

import org.a0z.mpd.Item;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamsFragment extends BrowseFragment {
    class DeleteDialogClickListener implements OnClickListener {
        private final int itemIndex;

        DeleteDialogClickListener(int itemIndex) {
            this.itemIndex = itemIndex;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
                case AlertDialog.BUTTON_POSITIVE:
                    try {
                        app.oMPDAsyncHelper.oMPD.removeSavedStream(streams.get(itemIndex).getPos());
                        String name = items.get(itemIndex).getName();
                        Tools.notifyUser(R.string.streamDeleted, name);
                        items.remove(itemIndex);
                        streams.remove(itemIndex);
                        updateFromItems();
                    } catch (MPDServerException e) {
                    }
                    break;
            }
        }
    }

    private static class Stream extends Item {
        private String name = null;
        private String url = null;
        private int pos = -1;

        public Stream(String name, String url, int pos) {
            this.name = name;
            this.url = url;
            this.pos = pos;
        }

        @Override
        public String getName() {
            return name;
        }
        public String getUrl() {
            return url;
        }
        public int getPos() { return pos; }
        public void setPos(int p) { pos=p; }

        @Override
        public boolean equals(Object other) {
            return this==other || (null!=other && other instanceof Stream && null!=url && url.equals(((Stream) other).url));
        }

        @Override
        public int hashCode() {
            return null==url ? 0 : url.hashCode();
        }
    }

    ArrayList<Stream> streams = new ArrayList<Stream>();
    public static final int EDIT = 101;
    public static final int DELETE = 102;
    private static final String FILE_NAME = "streams.xml";

    private static final String SERVER_FILE_NAME = "streams.xml.gz";

    public StreamsFragment() {
        super(R.string.addStream, R.string.streamAdded, null);
    }

    @Override
    protected void add(Item item, boolean replace, boolean play) {
        try {
            final Stream s = (Stream) item;
            app.oMPDAsyncHelper.oMPD.addStream(StreamFetcher.instance().get(s.getUrl(), s.getName()),
                    replace, play);
            Tools.notifyUser(irAdded, item);
        } catch (MPDServerException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void add(Item item, String playlist) {
    }

    public void addEdit() {
        addEdit(-1, null);
    }

    /*
     * StreamUrlToAdd is set when coming from the browser with
     * "android.intent.action.VIEW"
     */
    public void addEdit(final int idx, final String streamUrlToAdd) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View view = factory.inflate(R.layout.stream_dialog, null);
        final EditText nameEdit = (EditText) view.findViewById(R.id.name_edit);
        final EditText urlEdit = (EditText) view.findViewById(R.id.url_edit);
        final int index = idx;
        if (index >= 0 && index < streams.size()) {
            Stream s = streams.get(idx);
            if (null != nameEdit) {
                nameEdit.setText(s.getName());
            }
            if (null != urlEdit) {
                urlEdit.setText(s.getUrl());
            }
        } else if (streamUrlToAdd != null && urlEdit != null) {
            urlEdit.setText(streamUrlToAdd);
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(idx < 0 ? R.string.addStream : R.string.editStream)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText nameEdit = (EditText) view.findViewById(R.id.name_edit);
                        EditText urlEdit = (EditText) view.findViewById(R.id.url_edit);
                        String name = null == nameEdit ? null : nameEdit.getText().toString()
                                .trim();
                        String url = null == urlEdit ? null : urlEdit.getText().toString().trim();
                        if (null != name && name.length() > 0 && null != url && url.length() > 0) {
                            if (index >= 0 && index < streams.size()) {
                                try {
                                    int removedPos=streams.get(idx).getPos();
                                    app.oMPDAsyncHelper.oMPD.editSavedStream(url, name, removedPos);
                                    streams.remove(idx);
                                    for (Stream stream : streams) {
                                        if (stream.getPos()>removedPos) {
                                            stream.setPos(stream.getPos()-1);
                                        }
                                    }
                                    streams.add(new Stream(url, name, streams.size()));
                                } catch (MPDServerException e) {
                                }
                            } else {
                                try {
                                    app.oMPDAsyncHelper.oMPD.saveStream(url, name);
                                    streams.add(new Stream(url, name, streams.size()));
                                } catch (MPDServerException e) {
                                }
                            }
                            Collections.sort(streams);
                            items = streams;
                            if (streamUrlToAdd == null) {
                                UpdateList();
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
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (streamUrlToAdd != null) {
                            getActivity().finish();
                        }
                    }
                }).setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
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
    public int getLoadingText() {
        return R.string.loadingStreams;
    }

    private void loadStreams() {
        streams = new ArrayList<Stream>();

        // Load streams stored in MPD Streams playlist...
        List<Music> mpdStreams = null;
        try {
            mpdStreams = app.oMPDAsyncHelper.oMPD.getSavedStreams();
        } catch (MPDServerException e) {
        }

        if (null!=mpdStreams) {
            for (Music stream : mpdStreams) {
                streams.add(new Stream(stream.getName(), stream.getFullpath(), stream.getSongId()));
            }
        }

        // Load any OLD MPDroid streams, and also save these to MPD...
        ArrayList<Stream> oldStreams=loadOldStreams();
        if (null!=oldStreams) {
            for (Stream stream : streams) {
                if (!streams.contains(stream)) {
                    try {
                        app.oMPDAsyncHelper.oMPD.saveStream(stream.url, stream.name);
                        stream.setPos(streams.size());
                        streams.add(stream);
                    } catch (MPDServerException e) {
                    }
                }
            }
        }
        Collections.sort(streams);
        items = streams;
    }

    private ArrayList<Stream> loadOldStreams() {
        ArrayList<Stream> oldStreams=null;
        try {
            InputStream in = app.openFileInput(FILE_NAME);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(in, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("stream")) {
                        if (null==oldStreams) {
                            oldStreams=new ArrayList<Stream>();
                        }
                        oldStreams.add(new Stream(xpp.getAttributeValue("", "name"), xpp
                                      .getAttributeValue("", "url"), -1));
                    }
                }
                eventType = xpp.next();
            }
            in.close();
            // Now remove file - all streams will be added to MPD...
            app.deleteFile(FILE_NAME);
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            Log.e("Streams", "Error while loading streams", e);
        }
        return oldStreams;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(list);
        UpdateList();
        getActivity().setTitle(getResources().getString(R.string.streams));
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        if (info.id >= 0 && info.id < streams.size()) {
            Stream s = streams.get((int) info.id);
            android.view.MenuItem editItem = menu.add(ContextMenu.NONE, EDIT, 0,
                    R.string.editStream);
            editItem.setOnMenuItemClickListener(this);
            android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, DELETE, 0,
                    R.string.deleteStream);
            addAndReplaceItem.setOnMenuItemClickListener(this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_streamsmenu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
    }

    @Override
    public boolean onMenuItemClick(android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case EDIT:
                addEdit((int) info.id, null);
                break;
            case DELETE:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.deleteStream));
                builder.setMessage(
                        getResources().getString(R.string.deleteStreamPrompt,
                        items.get((int) info.id).getName()));

                DeleteDialogClickListener oDialogClickListener = new DeleteDialogClickListener(
                        (int) info.id);
                builder.setNegativeButton(getResources().getString(android.R.string.no),
                        oDialogClickListener);
                builder.setPositiveButton(getResources().getString(R.string.deleteStream),
                        oDialogClickListener);
                try {
                    builder.show();
                } catch (BadTokenException e) {
                    // Can't display it. Don't care.
                }
                break;
            default:
                return super.onMenuItemClick(item);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                addEdit();
                return true;
            default:
                return false;
        }
    }
}
