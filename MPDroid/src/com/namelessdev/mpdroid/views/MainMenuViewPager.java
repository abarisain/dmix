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
		// This will enable scoll on blank areas, cover art, but not on buttons/seekbars
		// If we're on page 1, all that's displayed is a list that plays nicely with ViewPager
		if (getCurrentItem() == 0)
			return false;
		return super.onInterceptTouchEvent(event);
	}
}
