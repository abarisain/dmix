package com.namelessdev.mpdroid;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ActionBar extends RelativeLayout implements OnClickListener {
	private LayoutInflater inflater;
	private View actionBarView;
	private View logoView;
	private View searchView;
	private View titleView;
	private OnClickListener searchClickListener;
	private boolean enableBackAction;
	private Context context;
	
	public ActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        actionBarView = inflater.inflate(R.layout.actionbar, null);
        addView(actionBarView);
        
        enableBackAction = false;
        logoView = actionBarView.findViewById(R.id.actionbar_logo);
        logoView.setOnClickListener(this);
        
        titleView = actionBarView.findViewById(R.id.actionbar_title);
        titleView.setOnClickListener(this);
        titleView.setVisibility(View.GONE);
        
        searchView = actionBarView.findViewById(R.id.actionbar_search);
        searchView.setOnClickListener(this);
        searchView.setVisibility(View.GONE);
        
	}

	public void setSearchButtonParams(boolean show, OnClickListener listener) {
		if(listener != null) {
			searchClickListener = listener;
		}
		searchView.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	public void setBackActionEnabled(boolean enable) {
		this.enableBackAction = enable;
	}
	
	public void setTitle(String title) {
		((TextView) titleView).setText(title);
		titleView.setVisibility(View.VISIBLE);
	}
	
	public void setTitle(int res) {
		setTitle(getResources().getString(res));
	}
	
	@Override
	public void onClick(View v) {
		if(v == searchView) {
			if(searchClickListener != null) {
				searchClickListener.onClick(v);
			}
		} else if(v == logoView || v == titleView) {
			if(enableBackAction && context instanceof Activity) {
				((Activity) context).finish();
			}
		}
	}
}
