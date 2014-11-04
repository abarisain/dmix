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

package com.namelessdev.mpdroid.adapters;

import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;

import org.a0z.mpd.item.Item;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayAdapter extends android.widget.ArrayAdapter<Item> {

    private static final int TYPE_DEFAULT = 0;

    private final Context mContext;

    private final ArrayDataBinder mDataBinder;

    private final LayoutInflater mInflater;

    private final List<Item> mItems;

    public ArrayAdapter(final Context context, final ArrayDataBinder dataBinder,
            final List<? extends Item> items) {
        super(context, 0, (List<Item>) items);
        mDataBinder = dataBinder;

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
        mItems = Collections.unmodifiableList(items);

        /** Empty lists need not be of type ArrayList<?> */
        if (!items.isEmpty() && !(items instanceof ArrayList<?>)) {
            throw new UnsupportedOperationException(
                    "Items must be contained in an ArrayList<Item>");
        }
    }

    public ArrayAdapter(final Context context, @LayoutRes final int textViewResourceId,
            final List<? extends Item> items) {
        super(context, textViewResourceId, (List<Item>) items);
        mDataBinder = null;

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
        mItems = Collections.unmodifiableList(items);

        /** Empty lists need not be of type ArrayList<?> */
        if (!items.isEmpty() && !(items instanceof ArrayList<?>)) {
            throw new UnsupportedOperationException(
                    "Items must be contained in an ArrayList<Item>");
        }
    }

    public ArrayDataBinder getDataBinder() {
        return mDataBinder;
    }

    @Override
    public int getItemViewType(final int position) {
        return TYPE_DEFAULT;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View resultView;

        if (mDataBinder == null) {
            resultView = super.getView(position, convertView, parent);
        } else {
            // cache all inner view references with ViewHolder pattern
            final AbstractViewHolder holder;

            if (convertView == null) {
                resultView = mInflater.inflate(mDataBinder.getLayoutId(), parent, false);
                resultView = mDataBinder.onLayoutInflation(mContext, resultView, mItems);

                // use the data binder to look up all references to inner views
                holder = mDataBinder.findInnerViews(resultView);
                resultView.setTag(holder);
            } else {
                resultView = convertView;
                holder = (AbstractViewHolder) resultView.getTag();
            }

            mDataBinder.onDataBind(mContext, resultView, holder, mItems, mItems.get(position),
                    position);
        }
        return resultView;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEnabled(final int position) {
        final boolean isEnabled;

        if (mDataBinder == null) {
            isEnabled = super.isEnabled(position);
        } else {
            isEnabled = mDataBinder.isEnabled(position, mItems, getItem(position));
        }

        return isEnabled;
    }
}
