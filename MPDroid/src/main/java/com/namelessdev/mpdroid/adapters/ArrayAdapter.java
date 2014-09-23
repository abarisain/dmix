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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayAdapter extends android.widget.ArrayAdapter<Item> {

    private static final int TYPE_DEFAULT = 0;

    private Context mContext;

    private ArrayDataBinder mDataBinder = null;

    private LayoutInflater mInflater;

    private List<Item> mItems;

    @SuppressWarnings("unchecked")
    public ArrayAdapter(final Context context, final ArrayDataBinder dataBinder,
            final List<? extends Item> items) {
        super(context, 0, (List<Item>) items);
        mDataBinder = dataBinder;
        init(context, items);
    }

    @SuppressWarnings("unchecked")
    public ArrayAdapter(final Context context, final int textViewResourceId,
            final List<? extends Item> items) {
        super(context, textViewResourceId, (List<Item>) items);
        mDataBinder = null;
        init(context, items);
    }

    public ArrayDataBinder getDataBinder() {
        return mDataBinder;
    }

    @Override
    public int getItemViewType(final int position) {
        return TYPE_DEFAULT;
    }

    public View getView(final int position, View convertView, final ViewGroup parent) {
        if (mDataBinder == null) {
            return super.getView(position, convertView, parent);
        }

        // cache all inner view references with ViewHolder pattern
        final AbstractViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(mDataBinder.getLayoutId(), parent, false);
            convertView = mDataBinder.onLayoutInflation(mContext, convertView, mItems);

            // use the data binder to look up all references to inner views
            holder = mDataBinder.findInnerViews(convertView);
            convertView.setTag(holder);
        } else {
            holder = (AbstractViewHolder) convertView.getTag();
        }

        mDataBinder
                .onDataBind(mContext, convertView, holder, mItems, mItems.get(position), position);
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    protected void init(final Context context, final List<? extends Item> items) {
        if (!(items instanceof ArrayList<?>)) {
            throw new RuntimeException("Items must be contained in an ArrayList<Item>");
        }

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
        mItems = (List<Item>) items;
    }

    @Override
    public boolean isEnabled(final int position) {
        if (mDataBinder == null) {
            return super.isEnabled(position);
        }
        return mDataBinder.isEnabled(position, mItems, getItem(position));
    }

    public void setDataBinder(final ArrayDataBinder dataBinder) {
        mDataBinder = dataBinder;
    }

}
