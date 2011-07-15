package com.namelessdev.mpdroid;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ActionBar extends RelativeLayout implements OnClickListener {
	private LayoutInflater inflater;
	private View actionBarView;
	private View logoView;
	private View searchView;
	private View libraryView;
	private View titleView;
	private View backView;
	private Button textButtonView;
	private View bottomSeparatorView;
	private OnClickListener searchClickListener;
	private OnClickListener textClickListener;
	private OnClickListener libraryClickListener;
	private OnClickListener titleClickListener;
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
		backView = actionBarView.findViewById(R.id.actionbar_back);
		backView.setOnClickListener(this);
		backView.setVisibility(View.GONE);
        
        titleView = actionBarView.findViewById(R.id.actionbar_title);
        titleView.setOnClickListener(this);
        titleView.setVisibility(View.GONE);
		titleView.setSelected(true);
        
        searchView = actionBarView.findViewById(R.id.actionbar_search);
        searchView.setOnClickListener(this);
        searchView.setVisibility(View.GONE);
        
		libraryView = actionBarView.findViewById(R.id.actionbar_library);
		libraryView.setOnClickListener(this);
		libraryView.setVisibility(View.GONE);

		textButtonView = (Button) actionBarView.findViewById(R.id.actionbar_text);
		textButtonView.setOnClickListener(this);
		textButtonView.setVisibility(View.GONE);

		bottomSeparatorView = actionBarView.findViewById(R.id.actionbar_bottom_separator);
		bottomSeparatorView.setVisibility(View.GONE);

	}

	public void showBottomSeparator(boolean show) {
		bottomSeparatorView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void setSearchButtonParams(boolean show, OnClickListener listener, int drawable) {
		((ImageButton) searchView).setImageResource(drawable);
		setSearchButtonParams(show, listener);
	}

	public void setSearchButtonParams(boolean show, OnClickListener listener) {
		if(listener != null) {
			searchClickListener = listener;
		}
		searchView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void setLibraryButtonParams(boolean show, OnClickListener listener) {
		if (listener != null) {
			libraryClickListener = listener;
		}
		libraryView.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	public void setTextButtonParams(boolean show, int text, OnClickListener listener) {
		if (listener != null) {
			textClickListener = listener;
		}
		textButtonView.setText(text);
		textButtonView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void setBackActionEnabled(boolean enable) {
		this.enableBackAction = enable;
		backView.setVisibility(enable ? View.VISIBLE : View.GONE);
	}
	
	public void setTitle(String title) {
		((TextView) titleView).setText(title);
		titleView.setVisibility(View.VISIBLE);
	}
	
	public void setTitle(int res) {
		setTitle(getResources().getString(res));
	}
	
	public void setTitleSelected(boolean selected) {
		titleView.setSelected(selected);
	}

	public void setTitleBackgroundDrawable(int drawable) {
		titleView.setBackgroundResource(drawable);
	}

	public void setTitleLeftDrawable(int res) {
		((TextView) titleView).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(res), null, null, null);
	}

	public void setTitleRightDrawable(int res) {
		((TextView) titleView).setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(res), null);
	}

	public void setTitleClickListener(OnClickListener listener) {
		if (listener != null) {
			titleClickListener = listener;
		}
	}

	@Override
	public void onClick(View v) {
		if(v == searchView) {
			if(searchClickListener != null) {
				searchClickListener.onClick(v);
			}
		} else if (v == logoView || v == backView) {
			if(enableBackAction && context instanceof Activity) {
				((Activity) context).finish();
			}
		} else if (v == textButtonView) {
			if (textClickListener != null) {
				textClickListener.onClick(v);
			}
		} else if (v == libraryView) {
			if (libraryClickListener != null) {
				libraryClickListener.onClick(v);
			}
		} else if (v == titleView) {
			if (titleClickListener != null) {
				titleClickListener.onClick(v);
			} else {
				if (enableBackAction && context instanceof Activity) {
					((Activity) context).finish();
				}
			}
		}
	}
}
