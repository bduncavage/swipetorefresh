/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.duncavage.swipetorefresh.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;


/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and progress
 * animation, call setEnabled(false) on the view.
 *
 * <p> This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.</p>
 */
public class SwipeRefreshLayout extends ViewGroup {
    private static final float ACCELERATE_INTERPOLATION_FACTOR = 1.5f;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final float PROGRESS_BAR_HEIGHT = 4;
    private static final float MAX_SWIPE_DISTANCE_FACTOR = .6f;
    private static final int REFRESH_TRIGGER_DISTANCE = 120;

    private SwipeProgressBar mProgressBar; //the thing that shows progress is going
    private View mTarget; //the content that gets pulled down
    private OnRefreshListener mListener;
    private MotionEvent mDownEvent;
    private boolean mRefreshing = false;
    private boolean mHasRefreshed = false;
    private int mTouchSlop;
    private float mDistanceToTriggerSync = -1;
    private int mMediumAnimationDuration;
    private float mFromPercentage = 0;
    private float mCurrPercentage = 0;
    private int mProgressBarHeight;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private final AccelerateInterpolator mAccelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.enabled
    };

    private ActionBarSwipeIndicator mActionBarSwipeIndicator;

    private Animation mShrinkTrigger = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            float percent = mFromPercentage + ((0 - mFromPercentage) * interpolatedTime);
            mProgressBar.setTriggerPercentage(percent);
        }
    };

    private final AnimationListener mShrinkAnimationListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            mCurrPercentage = 0;
        }
    };

    // Cancel the refresh gesture and animate everything back to its original state.
    private final Runnable mCancel = new Runnable() {

        @Override
        public void run() {
            // Timeout fired since the user last moved their finger; animate the
            // trigger to 0 and put the target back at its original position
            if (mProgressBar != null) {
                mFromPercentage = mCurrPercentage;
                mShrinkTrigger.setDuration(mMediumAnimationDuration);
                mShrinkTrigger.setAnimationListener(mShrinkAnimationListener);
                mShrinkTrigger.reset();
                mShrinkTrigger.setInterpolator(mDecelerateInterpolator);
                startAnimation(mShrinkTrigger);
            }
            if (mActionBarSwipeIndicator != null && !isRefreshing()) {
                mActionBarSwipeIndicator.hide();
            }
        }

    };

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     * @param context
     */
    public SwipeRefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     * @param context
     * @param attrs
     */
    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mActionBarSwipeIndicator = new ActionBarSwipeIndicator(context);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mProgressBar = new SwipeProgressBar(this);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mProgressBarHeight = (int) (metrics.density * PROGRESS_BAR_HEIGHT);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mAccelerateInterpolator = new AccelerateInterpolator(ACCELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mCancel);
        mActionBarSwipeIndicator.onAttachedToWindow((ViewGroup)getRootView());
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mCancel);
        mActionBarSwipeIndicator.onDetatchedFromWindow();
    }

    /**
     * Set the resource id of the string to display in the action bar indicator
     * overlay.
     */
    public void setActionBarSwipeIndicatorText(int resId) {
        if (mActionBarSwipeIndicator != null) {
            mActionBarSwipeIndicator.setSwipeToRefreshText(resId);
        }
    }

    /**
     * Set the resource id of the string to display in the action bar indicator
     * overlay while content is refreshing.
     */
    public void setActionBarSwipeIndicatorRefreshingText(int resId) {
        if (mActionBarSwipeIndicator != null) {
            mActionBarSwipeIndicator.setRefreshingText(resId);
        }
    }

    /**
     * Set the color of the text that is displayed while content is refreshing.
     */
    public void setActionBarSwipeIndicatorRefreshingTextColor(int color) {
        if (mActionBarSwipeIndicator != null) {
            mActionBarSwipeIndicator.setRefreshingTextColor(color);
        }
    }

    /**
     * Set the background color of the action bar swipe indicator overlay view.
     */
    public void setActionBarSwipeIndicatorBackgroundColor(int color) {
        if (mActionBarSwipeIndicator != null) {
            mActionBarSwipeIndicator.setBackgroundColor(color);
        }
    }

    /**
     * Set a custom to layout to be used for the action bar swipe indicator.
     */
    public void setActionBarSwipeIndicatorLayout(int resId, int textViewId) {
        if (mActionBarSwipeIndicator != null) {
            mActionBarSwipeIndicator.setCustomLayout(resId, textViewId);
        }
    }

    /**
     * Set text color of the action bar swipe indicator overlay view.
     */
    public void setActionBarSwipeIndicatorTextColor(int color) {
        if (mActionBarSwipeIndicator != null) {
            mActionBarSwipeIndicator.setSwipeToRefreshTextColor(color);
        }
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    private void setTriggerPercentage(float percent) {
        if (percent == 0f) {
            // No-op. A null trigger means it's uninitialized, and setting it to zero-percent
            // means we're trying to reset state, so there's nothing to reset in this case.
            mCurrPercentage = 0;
            return;
        }
        mActionBarSwipeIndicator.show();
        mCurrPercentage = percent;
        mProgressBar.setTriggerPercentage(percent);
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (mRefreshing != refreshing) {
            ensureTarget();
            mCurrPercentage = 0;
            mRefreshing = refreshing;
            if (mRefreshing) {
                mProgressBar.start();
            } else {
                mProgressBar.stop();
            }
            mActionBarSwipeIndicator.setRefreshing(refreshing);
        }
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     *
     * @param colorRes1 Color resource.
     * @param colorRes2 Color resource.
     * @param colorRes3 Color resource.
     * @param colorRes4 Color resource.
     */
    public void setColorScheme(int colorRes1, int colorRes2, int colorRes3, int colorRes4) {
        ensureTarget();
        final Resources res = getResources();
        final int color1 = res.getColor(colorRes1);
        final int color2 = res.getColor(colorRes2);
        final int color3 = res.getColor(colorRes3);
        final int color4 = res.getColor(colorRes4);
        mProgressBar.setColorScheme(color1, color2, color3,color4);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     *         progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid out yet.
        if (mTarget == null) {
            if (getChildCount() > 1 && !isInEditMode()) {
                throw new IllegalStateException(
                        "SwipeRefreshLayout can host only one direct child");
            }
            mTarget = getChildAt(0);
        }
        if (mDistanceToTriggerSync == -1) {
            if (getParent() != null && ((View)getParent()).getHeight() > 0) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDistanceToTriggerSync = (int) Math.min(
                        ((View) getParent()) .getHeight() * MAX_SWIPE_DISTANCE_FACTOR,
                        REFRESH_TRIGGER_DISTANCE * metrics.density);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mProgressBar.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width =  getMeasuredWidth();
        final int height = getMeasuredHeight();
        mProgressBar.setBounds(0, 0, width, mProgressBarHeight);
        if (getChildCount() == 0) {
            return;
        }
        final View child = getChildAt(0);
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() > 1 && !isInEditMode()) {
            throw new IllegalStateException("SwipeRefreshLayout can host only one direct child");
        }
        if (getChildCount() > 0) {
            getChildAt(0).measure(
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                            MeasureSpec.EXACTLY));
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();
        boolean handled = false;
        if (isEnabled() && !canChildScrollUp() && !isRefreshing()) {
            handled = onTouchEvent(ev);
        }
        return !handled ? super.onInterceptTouchEvent(ev) : handled;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        boolean handled = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCurrPercentage = 0;
                mDownEvent = MotionEvent.obtain(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mHasRefreshed || isRefreshing()) {
                    // We've already triggered a refresh once during this gesture.
                    return false;
                }
                if (mDownEvent != null) {
                    final float eventY = event.getY();
                    float yDiff = eventY - mDownEvent.getY();
                    if (yDiff > mTouchSlop) {
                        // User velocity passed min velocity; trigger a refresh
                        if (yDiff > mDistanceToTriggerSync) {
                            // User movement passed distance; trigger a refresh
                            startRefresh();
                            mHasRefreshed = handled = true;
                            break;
                        } else {
                            // Just track the user's movement
                            setTriggerPercentage(
                                    mAccelerateInterpolator.getInterpolation(
                                            yDiff / mDistanceToTriggerSync));
                            updatePositionTimeout();
                            handled = true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDownEvent != null) {
                    mDownEvent.recycle();
                    mDownEvent = null;
                }
                removeCallbacks(mCancel);
                if (mHasRefreshed) {
                    // If we've already refreshed, just unset the flag.
                    // No need to run the cancel runnable;
                    mHasRefreshed = false;
                } else {
                    mCancel.run();
                }
                break;
        }
        return handled;
    }

    private void startRefresh() {
        if (isRefreshing()) {
            // Already refreshing, no need to do anything.
            // Notifying the listener is not a good idea.
            return;
        }
        removeCallbacks(mCancel);
        setRefreshing(true);
        mListener.onRefresh();
    }

    private void updatePositionTimeout() {
        removeCallbacks(mCancel);
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }

    /**
     * Simple AnimationListener to avoid having to implement unneeded methods in
     * AnimationListeners.
     */
    private class BaseAnimationListener implements AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}