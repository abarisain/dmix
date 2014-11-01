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
import android.view.View;

import java.util.List;

public interface ArrayDataBinder {

    AbstractViewHolder findInnerViews(View targetView);

    int getLayoutId();

    boolean isEnabled(int position, List<? extends Item> items, Object item);

    void onDataBind(Context context, View targetView, AbstractViewHolder viewHolder,
            List<? extends Item> items, Object item, int position);

    View onLayoutInflation(Context context, View targetView, List<? extends Item> items);
}
