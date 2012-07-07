package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Item;

import android.content.Context;
import android.view.View;

public interface ArrayIndexerDataBinder {
	public void onDataBind(Context context, View targetView, List<? extends Item> items, Object item, int position);
	public boolean isEnabled(int position, List<? extends Item> items, Object item);
	public int getLayoutId();
}
