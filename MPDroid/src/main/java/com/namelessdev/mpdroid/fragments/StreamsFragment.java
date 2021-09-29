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

import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Stream;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

public class StreamsFragment extends BrowseFragment {

    private static final String TAG = "StreamsFragment";

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

    /*
     * StreamUrlToAdd is set when coming from the browser with
     * "android.intent.action.VIEW"
     */
    public void addEdit(final CharSequence streamUrlToAdd) {
        final LayoutInflater factory = LayoutInflater.from(getActivity());
        final View view = factory.inflate(R.layout.stream_dialog, null);
        final ToggleButton addReplaceEdit = (ToggleButton) view.findViewById(R.id.add_replace_edit);
        final EditText urlEdit = (EditText) view.findViewById(R.id.url_edit);
        addReplaceEdit.setChecked(Boolean.TRUE);
        if (streamUrlToAdd != null && urlEdit != null) {
            urlEdit.setText(streamUrlToAdd);
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.addStream)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final Boolean addReplace = null == addReplaceEdit ? null
                                : addReplaceEdit.isChecked();
                        final String url = null == urlEdit ? null
                                : urlEdit.getText().toString().trim();
                        if (null != url && !url.isEmpty()) {
                            try {
                                if(addReplace)
                                    mApp.oMPDAsyncHelper.oMPD.addStream(url, true, true);
                                else
                                    mApp.oMPDAsyncHelper.oMPD.addStream(url, false, false);
                            } catch (final IOException | MPDException e) {
                                Log.e(TAG, "Failed to save stream.", e);
                            }

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
        //        loadStreams();
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

    //    @Override
    //    public boolean onMenuItemClick(final MenuItem item) {
    //        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    //        switch (item.getItemId()) {
    //            case EDIT:
    //                addEdit((int) info.id, null);
    //                break;
    //            case DELETE:
    //                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    //                builder.setTitle(R.string.deleteStream);
    //                builder.setMessage(
    //                        getResources().getString(R.string.deleteStreamPrompt,
    //                                mItems.get((int) info.id).getName()));
    //
    //                final OnClickListener oDialogClickListener
    //                        = new DeleteDialogClickListener(
    //                        (int) info.id);
    //                builder.setNegativeButton(android.R.string.no, oDialogClickListener);
    //                builder.setPositiveButton(R.string.deleteStream, oDialogClickListener);
    //                try {
    //                    builder.show();
    //                } catch (final BadTokenException e) {
    //                    // Can't display it. Don't care.
    //                }
    //                break;
    //            default:
    //                return super.onMenuItemClick(item);
    //        }
    //        return false;
    //    }

    //    @Override
    //    public boolean onOptionsItemSelected(final MenuItem item) {
    //        switch (item.getItemId()) {
    //            case R.id.add:
    //                addEdit();
    //                return true;
    //            default:
    //                return false;
    //        }
    //    }

    //    class DeleteDialogClickListener implements OnClickListener {
    //
    //        private final int mItemIndex;
    //
    //        DeleteDialogClickListener(final int itemIndex) {
    //            super();
    //            mItemIndex = itemIndex;
    //        }
    //
    //        @Override
    //        public void onClick(final DialogInterface dialog, final int which) {
    //            if (which == DialogInterface.BUTTON_POSITIVE) {
    //                try {
    //                    mApp.oMPDAsyncHelper.oMPD.removeSavedStream(mStreams.get(mItemIndex).getPos());
    //                } catch (final IOException | MPDException e) {
    //                    Log.e(TAG, "Failed to removed a saved stream.", e);
    //                }
    //
    //                final String name = mItems.get(mItemIndex).getName();
    //                Tools.notifyUser(R.string.streamDeleted, name);
    //                mItems.remove(mItemIndex);
    //                mStreams.remove(mItemIndex);
    //                updateFromItems();
    //            }
    //        }
    //    }
}
