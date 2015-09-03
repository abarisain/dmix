/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.namelessdev.mpdroid.cover;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.io.InputStream;

public class CoverBitmapDrawable extends BitmapDrawable {

    public CoverBitmapDrawable(final Resources resources, final Iterable<Bitmap> bitmaps) {
        super(resources, bitmaps.iterator().next());
    }

    public CoverBitmapDrawable(final Resources resources, final InputStream is) {
        super(resources, is);
    }

    public CoverBitmapDrawable(final Resources resources, final String filePath) {
        super(resources, filePath);
    }
}
