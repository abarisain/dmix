package com.namelessdev.mpdroid.adapters;

import java.util.List;

import android.content.Context;
import android.view.View;

public interface SeparatedListDataBinder {
	public void onDataBind(Context context, View targetView, List<Object> items, Object item, int position);
	public boolean isEnabled(int position, List<Object> items, Object item);
}
