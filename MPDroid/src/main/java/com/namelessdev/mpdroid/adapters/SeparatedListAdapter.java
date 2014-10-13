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
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collections;
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

    private final SeparatedListDataBinder mBinder; // The content -> view 'binding'

    private final Context mContext;

    private final LayoutInflater mInflater;

    private final List<?> mItems; // Content

    private final int mSeparatorLayoutId;

    private int mViewId = -1; // The view to be displayed

    public SeparatedListAdapter(final Context context, @LayoutRes final int viewId,
            @LayoutRes final int separatorViewId, final SeparatedListDataBinder binder,
            final List<?> items) {
        super();
        mViewId = viewId;
        mBinder = binder;
        mItems = Collections.unmodifiableList(items);
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSeparatorLayoutId = separatorViewId;
    }

    public SeparatedListAdapter(
            final Context context, @LayoutRes final int viewId,
            final SeparatedListDataBinder binder, final List<?> items) {
        this(context, viewId, R.layout.list_separator, binder, items);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(final int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return (long) position;
    }

    @Override
    public int getItemViewType(final int position) {
        final int viewType;

        if (mItems.get(position) instanceof String) {
            viewType = TYPE_SEPARATOR;
        } else {
            viewType = TYPE_CONTENT;
        }

        return viewType;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final int itemType = getItemViewType(position);
        final View view;

        if (convertView == null) {
            if (itemType == TYPE_SEPARATOR) {
                view = mInflater.inflate(mSeparatorLayoutId, parent, false);
            } else {
                view = mInflater.inflate(mViewId, parent, false);
            }
        } else {
            view = convertView;
        }

        if (itemType == TYPE_SEPARATOR) {
            final CharSequence separator = (CharSequence) mItems.get(position);

            ((TextView) view.findViewById(R.id.separator_title)).setText(separator);
        } else {
            mBinder.onDataBind(mContext, view, mItems, mItems.get(position), position);
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public boolean isEnabled(final int position) {
        if (getItemViewType(position) == TYPE_SEPARATOR) {
            return false;
        }
        return mBinder.isEnabled(position, mItems, getItem(position));
    }

}
