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

package com.namelessdev.mpdroid.views;

import com.namelessdev.mpdroid.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class FixedRatioRelativeLayout extends RelativeLayout {

    private static final String FIXED_HEIGHT = "height";

    private static final int FIXED_HEIGHT_INT = 1;

    private int mFixedSide = FIXED_HEIGHT_INT;

    private static final String FIXED_WIDTH = "width";

    private static final int FIXED_WIDTH_INT = 2;

    public FixedRatioRelativeLayout(final Context context) {
        super(context);
    }

    public FixedRatioRelativeLayout(final Context context, @AttrRes final AttributeSet attrs) {
        super(context, attrs);
        readAttrs(context, attrs);
    }

    public FixedRatioRelativeLayout(final Context context, @AttrRes final AttributeSet attrs,
            @StyleRes final int defStyle) {
        super(context, attrs, defStyle);
        readAttrs(context, attrs);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (mFixedSide == FIXED_HEIGHT_INT) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }

    private void readAttrs(final Context context, final AttributeSet attrs) {
        final TypedArray relativeLayout =
                context.obtainStyledAttributes(attrs, R.styleable.FixedRatioRelativeLayout);
        final String fixed =
                relativeLayout.getString(R.styleable.FixedRatioRelativeLayout_fixedSide);

        if (fixed != null) {
            if (fixed.equals(FIXED_HEIGHT)) {
                mFixedSide = FIXED_HEIGHT_INT;
            } else if (fixed.equals(FIXED_WIDTH)) {
                mFixedSide = FIXED_WIDTH_INT;
            }
        }
        relativeLayout.recycle();
    }

}
