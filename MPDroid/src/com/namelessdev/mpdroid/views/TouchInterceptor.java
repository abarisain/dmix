package com.namelessdev.mpdroid.views;

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.namelessdev.mpdroid.R;

//Straight out of Google ! Oh the joy of open source ... :)
public class TouchInterceptor extends ListView {

	private ImageView mDragView;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;
	private int mDragPos; // which item is being dragged
	private int mFirstDragPos; // where was the dragged item originally
	private int mDragPoint; // at what offset inside the item did the user grab it
	private int mCoordOffset; // the difference between screen coordinates and coordinates in this view
	private DragListener mDragListener;
	private DropListener mDropListener;
	private RemoveListener mRemoveListener;
	private int mUpperBound;
	private int mLowerBound;
	private int mHeight;
	private GestureDetector mGestureDetector;
	private static final int FLING = 0;
	private static final int SLIDE = 1;
	private int mRemoveMode = -1;
	private Rect mTempRect = new Rect();
	private Bitmap mDragBitmap;
	private final int mTouchSlop;
	private int mItemHeightNormal;
	private int mItemHeightExpanded;
	private int mItemHeightHalf;
	private boolean mDraggingEnabled;

	public TouchInterceptor(Context context, AttributeSet attrs) {
		super(context, attrs);
		mRemoveMode = -1;
		mDraggingEnabled = true;
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		Resources res = getResources();
		mItemHeightNormal = res.getDimensionPixelSize(R.dimen.normal_height);
		mItemHeightHalf = mItemHeightNormal / 2;
		mItemHeightExpanded = res.getDimensionPixelSize(R.dimen.expanded_height);
	}
	
	private boolean isDragItem(View item){
		return (mDraggingEnabled && item.findViewById(R.id.icon) != null);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mDraggingEnabled && mRemoveListener != null && mGestureDetector == null) {
			if (mRemoveMode == FLING) {
				mGestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
						if (mDragView != null) {
							if (velocityX > 1000) {
								Rect r = mTempRect;
								mDragView.getDrawingRect(r);
								if (e2.getX() > r.right * 2 / 3) {
									// fast fling right with release near the right edge of the screen
									stopDragging();
									mRemoveListener.remove(mFirstDragPos);
									unExpandViews(true);
								}
							}
							// flinging while dragging should have no effect
							return true;
						}
						return false;
					}
				});
			}
		}
		if (mDraggingEnabled && mDragListener != null || mDropListener != null) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				int itemnum = pointToPosition(x, y);
				if (itemnum == AdapterView.INVALID_POSITION) {
					break;
				}
				ViewGroup item = (ViewGroup) getChildAt(itemnum - getFirstVisiblePosition());
				mDragPoint = y - item.getTop();
				mCoordOffset = ((int) ev.getRawY()) - y;
				View dragger = item.findViewById(R.id.icon);
				if (!isDragItem(item))
					return super.onInterceptTouchEvent(ev);

				Rect r = mTempRect;
				dragger.getDrawingRect(r);
				// The dragger icon itself is quite small, so pretend the touch area is bigger
				if (x < r.right * 2) {
					item.setDrawingCacheEnabled(true);
					// Create a copy of the drawing cache so that it does not get recycled
					// by the framework when the list tries to clean up memory
					Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
					startDragging(bitmap, y);
					mDragPos = itemnum;
					mFirstDragPos = mDragPos;
					mHeight = getHeight();
					int touchSlop = mTouchSlop;
					mUpperBound = Math.min(y - touchSlop, mHeight / 3);
					mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3);
					return false;
				}
				stopDragging();
				break;
			}
		}
		return super.onInterceptTouchEvent(ev);
	}

	/*
	 * pointToPosition() doesn't consider invisible views, but we need to, so implement a slightly different version.
	 */
	private int myPointToPosition(int x, int y) {

		if (y < 0) {
			// when dragging off the top of the screen, calculate position
			// by going back from a visible item
			int pos = myPointToPosition(x, y + mItemHeightNormal);
			if (pos > 0) {
				return pos - 1;
			}
		}

		Rect frame = mTempRect;
		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = getChildAt(i);
			child.getHitRect(frame);
			if (frame.contains(x, y)) {
				return getFirstVisiblePosition() + i;
			}
		}
		return INVALID_POSITION;
	}

	private int getItemForPosition(int y) {
		int adjustedy = y - mDragPoint - mItemHeightHalf;
		int pos = myPointToPosition(0, adjustedy);
		if (pos >= 0) {
			if (pos <= mFirstDragPos) {
				pos += 1;
			}
		} else if (adjustedy < 0) {
			// this shouldn't happen anymore now that myPointToPosition deals
			// with this situation
			pos = 0;
		}
		return pos;
	}

	private void adjustScrollBounds(int y) {
		if (y >= mHeight / 3) {
			mUpperBound = mHeight / 3;
		}
		if (y <= mHeight * 2 / 3) {
			mLowerBound = mHeight * 2 / 3;
		}
	}

	/*
	 * Restore size and visibility for all listitems
	 */
	private void unExpandViews(boolean deletion) {
		for (int i = 0;; i++) {
			View v = getChildAt(i);
			if (v == null) {
				if (deletion) {
					// HACK force update of mItemCount
					int position = getFirstVisiblePosition();
					int y = getChildAt(0).getTop();
					setAdapter(getAdapter());
					setSelectionFromTop(position, y);
					// end hack
				}
				layoutChildren(); // force children to be recreated where needed
				v = getChildAt(i);
				if (v == null) {
					break;
				}
			}
			if (isDragItem(v)){
				ViewGroup.LayoutParams params = v.getLayoutParams();
				params.height = mItemHeightNormal;
				v.setLayoutParams(params);
				v.setVisibility(View.VISIBLE);
			}	
		}
	}

	/*
	 * Adjust visibility and size to make it appear as though an item is being dragged around and other items are making room for it: If
	 * dropping the item would result in it still being in the same place, then make the dragged listitem's size normal, but make the item
	 * invisible. Otherwise, if the dragged listitem is still on screen, make it as small as possible and expand the item below the insert
	 * point. If the dragged item is not on screen, only expand the item below the current insertpoint.
	 */
	private void doExpansion() {
		int childnum = mDragPos - getFirstVisiblePosition();
		if (mDragPos > mFirstDragPos) {
			childnum++;
		}

		View first = getChildAt(mFirstDragPos - getFirstVisiblePosition());

		for (int i = 0;; i++) {
			View vv = getChildAt(i);
			if (vv == null) {
				break;
			}
			int height = mItemHeightNormal;
			int visibility = View.VISIBLE;
			if (vv.equals(first)) {
				// processing the item that is being dragged
				if (mDragPos == mFirstDragPos) {
					// hovering over the original location
					visibility = View.INVISIBLE;
				} else {
					// not hovering over it
					height = 1;
				}
			} else if (i == childnum) {
				if (mDragPos < getCount() - 1) {
					height = mItemHeightExpanded;
				}
			}
			if (isDragItem(vv)){
				ViewGroup.LayoutParams params = vv.getLayoutParams();
				params.height = height;
				vv.setLayoutParams(params);
				vv.setVisibility(visibility);
			}	
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mGestureDetector != null) {
			mGestureDetector.onTouchEvent(ev);
		}
		if (mDraggingEnabled && (mDragListener != null || mDropListener != null) && mDragView != null) {
			int action = ev.getAction();
			switch (action) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				Rect r = mTempRect;
				mDragView.getDrawingRect(r);
				stopDragging();
				if (mRemoveMode == SLIDE && ev.getX() > r.right * 3 / 4) {
					if (mRemoveListener != null) {
						mRemoveListener.remove(mFirstDragPos);
					}
					unExpandViews(true);
				} else {
					if (mDropListener != null && mDragPos >= 0 && mDragPos < getCount()) {
						mDropListener.drop(mFirstDragPos, mDragPos);
					}
					unExpandViews(false);
				}
				break;

			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				dragView(x, y);
				int itemnum = getItemForPosition(y);
				if (itemnum >= 0) {
					if (action == MotionEvent.ACTION_DOWN || itemnum != mDragPos) {
						if (mDragListener != null) {
							mDragListener.drag(mDragPos, itemnum);
						}
						mDragPos = itemnum;
						doExpansion();
					}
					int speed = 0;
					adjustScrollBounds(y);
					if (y > mLowerBound) {
						// scroll the list up a bit
						speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
					} else if (y < mUpperBound) {
						// scroll the list down a bit
						speed = y < mUpperBound / 2 ? -16 : -4;
					}
					if (speed != 0) {
						int ref = pointToPosition(0, mHeight / 2);
						if (ref == AdapterView.INVALID_POSITION) {
							// we hit a divider or an invisible view, check somewhere else
							ref = pointToPosition(0, mHeight / 2 + getDividerHeight() + 64);
						}
						View v = getChildAt(ref - getFirstVisiblePosition());
						if (v != null) {
							int pos = v.getTop();
							setSelectionFromTop(ref, pos - speed);
						}
					}
				}
				break;
			}
			return true;
		}
		return super.onTouchEvent(ev);
	}

	private void startDragging(Bitmap bm, int y) {
		stopDragging();

		mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP;
		mWindowParams.x = 0;
		mWindowParams.y = y - mDragPoint + mCoordOffset;

		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;

		Context context = getContext();
		ImageView v = new ImageView(context);
		int backGroundColor = Color.BLACK;
		v.setBackgroundColor(backGroundColor);
		v.setImageBitmap(bm);
		mDragBitmap = bm;

		mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mWindowManager.addView(v, mWindowParams);
		mDragView = v;
	}

	private void dragView(int x, int y) {
		if (mRemoveMode == SLIDE) {
			float alpha = 1.0f;
			int width = mDragView.getWidth();
			if (x > width / 2) {
				alpha = ((float) (width - x)) / (width / 2);
			}
			mWindowParams.alpha = alpha;
		}
		if (mRemoveMode == FLING) {
			mWindowParams.x = x;
		}
		mWindowParams.y = y - mDragPoint + mCoordOffset;
		mWindowManager.updateViewLayout(mDragView, mWindowParams);
	}

	private void stopDragging() {
		if (mDragView != null) {
			mDragView.setVisibility(GONE);
			WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
			wm.removeView(mDragView);
			mDragView.setImageDrawable(null);
			mDragView = null;
		}
		if (mDragBitmap != null) {
			mDragBitmap.recycle();
			mDragBitmap = null;
		}
	}

	public void setDraggingEnabled(boolean enable) {
		mDraggingEnabled = enable;
	}

	public void setDragListener(DragListener l) {
		mDragListener = l;
	}

	public void setDropListener(DropListener l) {
		mDropListener = l;
	}

	public void setRemoveListener(RemoveListener l) {
		mRemoveListener = l;
	}

	public interface DragListener {
		void drag(int from, int to);
	}

	public interface DropListener {
		void drop(int from, int to);
	}

	public interface RemoveListener {
		void remove(int which);
	}
}
