package com.xzw.zoomscrollview;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * @author xzwszl 2015-05-08
 *
 */

public class ZoomScrollView extends ScrollView{
	
	private View mHeaderContainer;
	
	private View mContentView;
	private Rect mRect;
	
	private final int PULL_UP = 1;
	private final int PULL_DOWN = 2;
	private final int NORMAL = 0;    //scrollview NORMAL
	
	private final float FACTION = 2.5f;
	
	private int mCurrentMode = NORMAL;
	
	private float mTouchSlop;
	private float mInitY;
	private float mLastY;
	
	
	private int mHeaderHeight = 0;
	
	private Runnable mSmoothToTop;
	private Runnable mSmoothToEnd;
	
//	private 
	 
	public ZoomScrollView(Context context) {
		super(context);
		init(context);
	}
	
	public ZoomScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public ZoomScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context) {
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mHeaderHeight = -1;
		mRect = new Rect();
	}

	public void setHeaderContainer(View view) {
		mHeaderContainer = view;
	}
	
	@Override
	protected void onFinishInflate() {
		
		mContentView = getChildAt(0);
		
		if (mContentView != null && mHeaderContainer == null) {
			mHeaderContainer = ((ViewGroup) mContentView).getChildAt(0);
		}
		super.onFinishInflate();
	}
	
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
		
		if (mHeaderHeight == -1 && mHeaderContainer != null) {
			mHeaderHeight = mHeaderContainer.getHeight();
		}
		
		if (mContentView != null) {
			mRect.set(mContentView.getLeft(), mContentView.getTop(), mContentView.getRight(), mContentView.getBottom());
		}
	}
	
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		
		// get down event
		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() == 0) {
			mLastY = mInitY = event.getY();
		}
		return super.onInterceptTouchEvent(event);
	}

	//move to identify pulldown | pullup | normal
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		int delta = 0;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			
			mLastY = mInitY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			
				
			float y = event.getY();
			
			delta = (int) (y - mLastY);
			
			if (mCurrentMode == NORMAL) {
				
				if (Math.abs(delta) >= mTouchSlop) {
					
					if (delta >= 1.0f && canPullDown()) {        //pulldown
						mInitY = mLastY;
						mCurrentMode = PULL_DOWN;
					} else if (delta <= -1.0f && canPullUp()) {
						mInitY = mLastY;
						mCurrentMode = PULL_UP;
					}
				}
			} 
			mLastY = y;
			//pullDown
			if (mCurrentMode == PULL_DOWN) {
				if (pullDown(y - mInitY))
					return true;
				
			} else if (mCurrentMode == PULL_UP) {   //pullUp
				if(pullUp(y - mInitY)) return true;
			}	
			break;
		case MotionEvent.ACTION_UP:
			
			if (mCurrentMode == PULL_DOWN) {
				scrollBackToTop();
			} else if (mCurrentMode == PULL_UP) {
				scrollBackToEnd();
			}
				mCurrentMode = NORMAL;
			break;	
		default:
			break;
		}

		return super.onTouchEvent(event);
	}

	private boolean canPullDown() {
		return getScrollY() == 0 && mHeaderContainer != null && mHeaderContainer.getHeight() >= mHeaderHeight;
	}
	
	
	private boolean canPullUp() {
		
		if (mContentView != null) {
			return mContentView.getHeight() <= getHeight() + getScrollY();
		}
		
		return  false;
	}
	
	private boolean pullDown(float dY) {
		
		if (dY <= 0.0f) {           
			mCurrentMode = NORMAL;
 			return false;
		}
		
		
		if (mSmoothToTop != null) {
			((SmoothToTopRunnable)mSmoothToTop).stopAnimation();
		}
		
		
		LinearLayout.LayoutParams layoutParams = (android.widget.LinearLayout.LayoutParams) mHeaderContainer.getLayoutParams();
		
		int newValue = (int) (dY / (FACTION * layoutParams.height / mHeaderHeight));
		layoutParams.height = (int) (mHeaderHeight + newValue);
		mHeaderContainer.setLayoutParams(layoutParams);
		
		return true;
	}
	
	private boolean pullUp(float dY) {
		
		if (dY >= 0.0f) {          
			mCurrentMode = NORMAL;
 			return false;
		}
		
		if (mSmoothToEnd != null) ((SmoothToTopRunnable) mSmoothToEnd).stopAnimation();
		int newValue = (int) (dY / FACTION);
		mContentView.layout(mRect.left, mRect.top + newValue, mRect.right, mRect.bottom + newValue);
		return true;
	}
	
	
	private void scrollBackToTop() {
		
		if (mSmoothToTop == null) {
			mSmoothToTop = new SmoothToTopRunnable(mHeaderContainer);
		}
		((SmoothToTopRunnable) mSmoothToTop).stopAnimation();
		((SmoothToTopRunnable) mSmoothToTop).startAnimation(mHeaderContainer.getHeight(),mHeaderHeight);
	}
	
	private void scrollBackToEnd() {
		
		if (mSmoothToEnd == null) {
			mSmoothToEnd = new SmoothToEndRunnable(mContentView);
		}
		((SmoothToTopRunnable) mSmoothToEnd).stopAnimation();
		((SmoothToTopRunnable) mSmoothToEnd).startAnimation(mContentView.getTop(),mRect.top);
	}
	
	class SmoothToTopRunnable implements Runnable {

		public int curY;
		public int targetY;
		public long startTime;
	    public boolean isFinished;	
		public static final long DURATION = 250L;
		public View view;
		
		public SmoothToTopRunnable (View view) {
			this.view = view;
			
		}
		@Override
		public void run() {
			
			if(!isFinished) {
				long time = SystemClock.currentThreadTimeMillis();
				float factor = interpolator.getInterpolation((float)(time - startTime) / DURATION);
				
				ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
				if (factor <= 0.0f) {
					if (layoutParams.height != targetY) {
						
						layoutParams.height = targetY;
						view.setLayoutParams(layoutParams);
					}
					isFinished = true;
				} else {
					layoutParams.height = (int) (targetY +  (curY - targetY) * factor);
					view.setLayoutParams(layoutParams);
					post(this);
				}
			}
		}
		
		public void startAnimation(int curY,int targetY) {
			this.curY = curY;
			this.targetY = targetY;
			startTime = SystemClock.currentThreadTimeMillis();
			isFinished = false;
			post(this);
		}
		
		public void stopAnimation() {
			
			removeCallbacks(this);
			isFinished = true;
			
		}
	}

	private Interpolator interpolator = new Interpolator() {
		
		@Override
		public float getInterpolation(float input) {
			
			return (float) Math.pow((1.0f - input), 9);
		}
	};
	
	class SmoothToEndRunnable extends SmoothToTopRunnable {

		public SmoothToEndRunnable(View view) {
			super(view);
		}
		
		@Override
		public void run() {
			
			if(!isFinished) {
				long time = SystemClock.currentThreadTimeMillis();
				float factor = interpolator.getInterpolation((float)(time - startTime) / DURATION);
				
				int value = (int) ((curY - targetY) * factor);
				
				if (factor <= 0.0f) {
					if (value != 0) {
						view.layout(mRect.left, mRect.top, mRect.right, mRect.bottom);
					}
					isFinished = true;
				} else {
					view.layout(mRect.left, mRect.top + value, mRect.right, mRect.bottom + value);
					post(this);
				}
			}
		}
		
	}
}

