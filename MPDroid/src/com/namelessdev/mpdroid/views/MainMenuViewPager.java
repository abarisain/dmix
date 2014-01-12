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
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MainMenuViewPager extends ViewPager {

    public MainMenuViewPager(Context context) {
        super(context);
    }

    public MainMenuViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // If a touch event is intercepted on page 1, don't do anything.
        // This will enable scoll on blank areas, cover art, but not on
        // buttons/seekbars
        // If we're on page 1, all that's displayed is a list that plays nicely
        // with ViewPager
        if (getCurrentItem() == 0)
            return false;
        return super.onInterceptTouchEvent(event);
    }
}
