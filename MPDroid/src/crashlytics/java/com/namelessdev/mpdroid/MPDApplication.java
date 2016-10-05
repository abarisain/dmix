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

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class MPDApplication extends MPDApplicationBase {

    protected static MPDApplication sInstance;

    public static MPDApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        sInstance = this;

        super.onCreate();

        Fabric.with(sInstance, new Crashlytics());
    }
}
