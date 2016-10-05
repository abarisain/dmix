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

package com.namelessdev.mpdroid;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import android.support.v4.app.Fragment;

public class MPDApplication extends MPDApplicationBase {

    protected static MPDApplication sInstance;

    /**
     * This is a RefWatcher for this Application instance.
     */
    private RefWatcher mRefWatcher;

    public static MPDApplication getInstance() {
        return sInstance;
    }

    /**
     * This is the {@link RefWatcher} used to watch {@link Fragment}s.
     *
     * @return A RefWatcher.
     */
    public static RefWatcher getRefWatcher() {
        return sInstance.mRefWatcher;
    }

    @Override
    public void onCreate() {
        sInstance = this;

        super.onCreate();

        mRefWatcher = LeakCanary.install(this);
    }
}
