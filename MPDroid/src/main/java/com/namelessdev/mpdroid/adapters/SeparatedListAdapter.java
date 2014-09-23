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

import com.namelessdev.mpdroid.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * List adapter which uses an object list. If the object is an instance of
 * String, the list will display a separator. If the object is of any other
 * type, the binder will be called. The binder should do what getView does when
 * you extend BaseAdapter (except that you never inflate the view yourself)
 * There are many other implementations of this list on the internet, this one
 * has a lot of restrictions (which makes it simpler), but handles the
 * separators so that you always get the right line number when you select a
 * line. The separator needs to have a TextView named "separator_title".
 */

public class SeparatedListAdapter extends BaseAdapter {

    private static final int TYPE_CONTENT = 0;

    private static final int TYPE_SEPARATOR = 1;

    private SeparatedListDataBinder mBinder; // The content -> view 'binding'

    private Context mContext;

    private LayoutInflater mInflater;

    private List<?> mItems; // Content

    private int mSeparatorLayoutId;

    private int mViewId = -1; // The view to be displayed

    public SeparatedListAdapter(Context context, int viewId, int separatorViewId,
            SeparatedListDataBinder binder,
            List<?> items) {
        init(context, viewId, separatorViewId, binder, items);
    }

    public SeparatedListAdapter(Context context, int viewId, SeparatedListDataBinder binder,
            List<?> items) {
        init(context, viewId, R.layout.list_separator, binder, items);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    public int getCount() {
        return mItems.size();
    }

    public Object getItem(int position) {
        return mItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof String) {
            return TYPE_SEPARATOR;
        }
        return TYPE_CONTENT;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        int itemType = getItemViewType(position);

        if (convertView == null) {
            if (itemType == TYPE_SEPARATOR) {
                convertView = mInflater.inflate(mSeparatorLayoutId, parent, false);
            } else {
                convertView = mInflater.inflate(mViewId, parent, false);
            }
        }

        if (itemType == TYPE_SEPARATOR) {
            ((TextView) convertView.findViewById(R.id.separator_title)).setText((String) mItems
                    .get(position));
        } else {
            mBinder.onDataBind(mContext, convertView, mItems, mItems.get(position), position);
        }

        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    private void init(Context context, int viewId, int separatorViewId,
            SeparatedListDataBinder binder, List<?> items) {
        this.mViewId = viewId;
        this.mBinder = binder;
        this.mItems = items;
        this.mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSeparatorLayoutId = separatorViewId;
    }

    @Override
    public boolean isEnabled(int position) {
        if (getItemViewType(position) == TYPE_SEPARATOR) {
            return false;
        }
        return mBinder.isEnabled(position, mItems, getItem(position));
    }

}
