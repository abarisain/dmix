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

package com.namelessdev.mpdroid.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.namelessdev.mpdroid.R;

public class FixedRatioRelativeLayout extends RelativeLayout {
    private static final String FIXED_HEIGHT = "height";
    private static final String FIXED_WIDTH = "width";
    private static final int FIXED_HEIGHT_INT = 1;
    private static final int FIXED_WIDTH_INT = 2;

    private int fixedSide = FIXED_HEIGHT_INT;

    public FixedRatioRelativeLayout(Context context) {
        super(context);
    }

    public FixedRatioRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttrs(context, attrs);
    }

    public FixedRatioRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readAttrs(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (fixedSide == FIXED_HEIGHT_INT) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }

    private void readAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FixedRatioRelativeLayout);
        CharSequence s = a.getString(R.styleable.FixedRatioRelativeLayout_fixedSide);
        if (s != null) {
            final String fixString = s.toString();
            if (fixString.equals(FIXED_HEIGHT)) {
                fixedSide = FIXED_HEIGHT_INT;
            } else if (fixString.equals(FIXED_WIDTH)) {
                fixedSide = FIXED_WIDTH_INT;
            }
        }
        a.recycle();
    }

}
