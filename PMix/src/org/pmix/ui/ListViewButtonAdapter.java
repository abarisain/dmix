package org.pmix.ui;

import java.util.List;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

interface PlusListener
{
	void OnAdd(CharSequence sSelected, int iPosition);
}

class ListViewPlusView extends ViewGroup
{
	private class ButtonListener implements Button.OnClickListener {
		public PlusListener mRedirect;
		public void onClick(View v) {
			if(mRedirect != null)
				mRedirect.OnAdd(mText.getText(), mPosition);
		}
	}
	TextView mText;
	ImageButton mButton;
	ButtonListener mButtonListener;
	PlusListener mParentListener;
	int mPosition;
	final int mButtonWidth = 35;
	
	public ListViewPlusView(Context context) {
		this(context, null, 0);
	}
	public ListViewPlusView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public ListViewPlusView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mText = new TextView(context, attrs, defStyle);
		mText.setPadding(0, 4, 0, 4);
		mButton = new ImageButton(context, attrs, defStyle);
		mButtonListener = new ButtonListener();
		mButton.setOnClickListener(mButtonListener);
		mButton.setImageResource(android.R.drawable.ic_input_add);
		mButton.setBackgroundResource(android.R.drawable.btn_default_small);
		// ic_menu_add (circle with '') 
		// ic_input_add (green '' sign)
		// ic_input_delete (green '-' sign)
		// btn_default
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
		addView(mText);
		addView(mButton);
	}
	
	void setText(CharSequence s) {
		/*
		mText.setEllipsize(TextUtils.TruncateAt.END);
		*/
		mText.setSingleLine();
		mText.setHorizontallyScrolling(false);
		mText.setText(s);
	}
	
	void setPosition(int iPosition)
	{
		mPosition = iPosition;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int left = getPaddingLeft();
		int top = getPaddingTop();
		int right = r-l-getPaddingRight()-getPaddingLeft();
		int bottom = b-t-getPaddingBottom();
		mText.layout(left, top, right-mButtonWidth, bottom);
		mButton.layout(right-mButtonWidth, top, right, bottom);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int iTextWidthSpec = MeasureSpec.makeMeasureSpec(widthMode, widthSize-mButtonWidth);
        int iButtonWidthSpec = MeasureSpec.makeMeasureSpec(widthMode, mButtonWidth);
        
		mText.measure(iTextWidthSpec, heightMeasureSpec);
		mButton.measure(iButtonWidthSpec, heightMeasureSpec);
		int width = mText.getMeasuredWidth() + getPaddingLeft();
		int height = mText.getMeasuredHeight() + getPaddingTop() + getPaddingBottom();
		setMeasuredDimension(width + mButtonWidth, height);
	}
	public void SetPlusListener(PlusListener Listener) {
		mParentListener = Listener;
		mButtonListener.mRedirect = mParentListener;
	}
}

public class ListViewButtonAdapter<T> extends ArrayAdapter<T> {
	private final Context m_Context;
	private PlusListener m_PlusListener;
	
	public ListViewButtonAdapter(Context context, int textViewResourceId) {
		this(context, 0, textViewResourceId, (T[])null);
	}
	public ListViewButtonAdapter(Context context, int resource, int textViewResourceId) {
		this(context, resource, textViewResourceId, (T[])null);
	}
	public ListViewButtonAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
		super(context, resource, textViewResourceId, objects);
		m_Context = context;
	}
	public ListViewButtonAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
		super(context, resource, textViewResourceId, objects);
		m_Context = context;
	}
	public ListViewButtonAdapter(Context context, int textViewResourceId, List<T> objects) {
		this(context, 0, textViewResourceId, objects);
	}
	public ListViewButtonAdapter(Context context, int textViewResourceId, T[] objects) {
		this(context, 0, textViewResourceId, objects);
	}
	
	public void SetPlusListener(PlusListener Listener) {
		m_PlusListener = Listener;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ListViewPlusView v;
		if(convertView != null)
		{
			assert(convertView instanceof ListViewPlusView);
			v = (ListViewPlusView)convertView;
		}
		else
		{
			v = new ListViewPlusView(m_Context);
		}
		v.setText(getItem(position).toString());
		v.setPosition(position);
		v.setPadding(10, 6, 20, 6);
		v.SetPlusListener(m_PlusListener);
		return v;
	}
}
