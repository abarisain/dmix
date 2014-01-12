/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Xml;
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

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.LocalCover;
import com.namelessdev.mpdroid.tools.StreamFetcher;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.Item;
import org.a0z.mpd.exception.MPDServerException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

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
                    String name = items.get(itemIndex).getName();
                    Tools.notifyUser(
                            String.format(getResources().getString(R.string.streamDeleted), name),
                            getActivity());
                    items.remove(itemIndex);
                    updateFromItems();
                    break;
            }
        }
    }

    private static class Stream extends Item {
        private String name = null;
        private String url = null;
        private boolean onServer = false;

        public Stream(String name, String url, boolean onServer) {
            this.name = name;
            this.url = url;
            this.onServer = onServer;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
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
            app.oMPDAsyncHelper.oMPD.add(StreamFetcher.instance().get(s.getUrl(), s.getName()),
                    replace, play);
            Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
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
    public void addEdit(int idx, final String streamUrlToAdd) {
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
                                streams.remove(index);
                            }
                            streams.add(new Stream(name, url, false));
                            Collections.sort(streams);
                            items = streams;
                            saveStreams();
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

    private void loadLocalStreams() {
        try {
            loadStreams(getActivity().openFileInput(FILE_NAME), false);
        } catch (FileNotFoundException e) {
        }
    }

    private void loadServerStreams() {
        HttpURLConnection connection = null;
        try {
            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            String musicPath = settings.getString("musicPath", "music/");
            String url = LocalCover.buildCoverUrl(
                    app.oMPDAsyncHelper.getConnectionSettings().sServer, musicPath, null,
                    SERVER_FILE_NAME);
            URL u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            loadStreams(new GZIPInputStream(connection.getInputStream()), true);
        } catch (IOException e) {
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
    }

    private void loadStreams() {
        streams = new ArrayList<Stream>();
        loadLocalStreams();
        loadServerStreams();
        Collections.sort(streams);
        items = streams;
    }

    private void loadStreams(InputStream in, boolean fromServer) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(in, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("stream")) {
                        streams.add(new Stream(xpp.getAttributeValue("", "name"), xpp
                                .getAttributeValue("", "url"), fromServer));
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
        }
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
            if (!s.onServer) {
                android.view.MenuItem editItem = menu.add(ContextMenu.NONE, EDIT, 0,
                        R.string.editStream);
                editItem.setOnMenuItemClickListener(this);
                android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, DELETE, 0,
                        R.string.deleteStream);
                addAndReplaceItem.setOnMenuItemClickListener(this);
            }
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
                builder.setMessage(String.format(
                        getResources().getString(R.string.deleteStreamPrompt),
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

    private void saveStreams() {
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(getActivity().openFileOutput(FILE_NAME, Context.MODE_PRIVATE),
                    "UTF-8");
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "streams");
            if (null != streams) {
                for (Stream s : streams) {
                    if (!s.onServer) {
                        serializer.startTag("", "stream");
                        serializer.attribute("", "name", s.getName());
                        serializer.attribute("", "url", s.getUrl());
                        serializer.endTag("", "stream");
                    }
                }
            }
            serializer.endTag("", "streams");
            serializer.flush();
        } catch (Exception e) {
        }
    }

}
