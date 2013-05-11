package com.namelessdev.mpdroid.adapters;

import java.util.List;

import org.a0z.mpd.Item;

import android.content.Context;
import android.view.View;

import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;

public interface ArrayIndexerDataBinder {
	public View onLayoutInflation(Context context, View targetView, List<? extends Item> items);
	public void onDataBind(Context context, View targetView, AbstractViewHolder viewHolder, List<? extends Item> items, Object item, int position);
	public AbstractViewHolder findInnerViews(View targetView);
	public boolean isEnabled(int position, List<? extends Item> items, Object item);
	public int getLayoutId();
}
