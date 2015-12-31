/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

import com.anpmech.mpd.item.Item;
import com.crashlytics.android.Crashlytics;

import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;

public abstract class BrowseFragment<T extends Item<T>> extends BrowseFragmentBase<T> {

    protected BrowseFragment(@StringRes final int rAdd, @StringRes final int rAdded) {
        super(rAdd, rAdded);
    }

    @Override
    protected void reportTrackFailure(final View parent, final Adapter adapter) {
        /** Temporary, I want to find out exactly what's null. */
        Crashlytics.log(Log.ERROR, TAG, trackFailureString(parent, adapter));
    }
}
