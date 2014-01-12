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

package com.namelessdev.mpdroid.cover;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.io.InputStream;

public class CoverBitmapDrawable extends BitmapDrawable {

    public CoverBitmapDrawable(Resources resources, Bitmap bitmap) {
        super(resources, bitmap);
    }

    public CoverBitmapDrawable(Resources resources, InputStream is) {
        super(resources, is);
    }

    public CoverBitmapDrawable(Resources resources, String string) {
        super(resources, string);
    }
}
