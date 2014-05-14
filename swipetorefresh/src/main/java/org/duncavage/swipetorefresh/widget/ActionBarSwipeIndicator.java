package org.duncavage.swipetorefresh.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.TextView;

import org.duncavage.swipetorefresh.R;

/**
 * Created by brett on 4/25/14.
 */
final class ActionBarSwipeIndicator {
    private final String TAG = "ActionBarSwipeIndicator";

    private View mHeaderView;
    private Resources mResources;
    private Context mContext;
    private boolean mIsHidingHeader;
    private int mActionBarHeight;
    private int mStatusBarHeight;
    private String mIndicatorText;
    private int mIndicatorTextColor;
    private int mBackgroundColor;
    private int mHeaderViewLayoutResId;
    private Rect mHeaderViewRect;
    private boolean mHasHoneycomb;
    private String mIndicatorRefreshingText;
    private int mIndicatorRefreshingTextColor;
    private boolean mIsRefreshing;
    private int mTextViewId;

    public ActionBarSwipeIndicator(Context context) {
        mContext = context;
        mResources = context.getResources();

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, mResources.getDisplayMetrics());
        }
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            mStatusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        if (mActionBarHeight == 0) {
            // use our fallback. not super ideal, but works in a pinch on pre-honeycomb
            mActionBarHeight = mContext.getResources().getDimensionPixelSize(R.dimen.fallback_action_bar_height);
        }

        mHasHoneycomb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public void show() {
        if (mHeaderView.getVisibility() == View.VISIBLE) return;

        mHeaderView.setVisibility(View.VISIBLE);
        if (mHasHoneycomb) {
            ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(mHeaderView, "alpha", 0f, 1f);
            ObjectAnimator transAnim = ObjectAnimator.ofFloat(mHeaderView, "translationY", -mActionBarHeight, 0f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(fadeAnim, transAnim);
            animSet.setDuration(mResources.getInteger(android.R.integer.config_shortAnimTime));
            animSet.start();
        }
    }

    public void hide() {
        if (mIsHidingHeader || mHeaderView == null) return;

        mIsHidingHeader = true;
        if (mHasHoneycomb) {
            ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(mHeaderView, "alpha", 1f, 0f);
            ObjectAnimator transAnim = ObjectAnimator.ofFloat(mHeaderView, "translationY", 0f, -mActionBarHeight);
            AnimatorSet animSet = new AnimatorSet();
            animSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    mIsHidingHeader = false;
                    // This animation can complete after we've been detached from the window.
                    // At which time we'll have nulled the header view reference.
                    if (mHeaderView != null) {
                        mHeaderView.setVisibility(View.INVISIBLE);
                        setHeaderText(mIndicatorText, mIndicatorTextColor);
                    }
                }
            });
            animSet.playTogether(fadeAnim, transAnim);
            animSet.setDuration(mResources.getInteger(android.R.integer.config_shortAnimTime));
            animSet.start();
        } else {
            // Just make the header invisible. A possible improvement could be to use old-style
            // animations on GB and lower.
            mHeaderView.setVisibility(View.INVISIBLE);
            mIsHidingHeader = false;
            setHeaderText(mIndicatorText, mIndicatorTextColor);
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (refreshing != mIsRefreshing) {
            mIsRefreshing = refreshing;
            if (mIsRefreshing) {
                setHeaderText(mIndicatorRefreshingText, mIndicatorRefreshingTextColor);
            } else {
                // hide() resets the text after the animation completes
                hide();
            }
        }
    }

    public boolean isIsRefreshing() { return mIsRefreshing; }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
    }

    public void setSwipeToRefreshText(int resId) {
        mIndicatorText = mContext.getString(resId);
    }

    public void setSwipeToRefreshTextColor(int color) {  mIndicatorTextColor = color; }

    public void setRefreshingText(int resId) { mIndicatorRefreshingText = mContext.getString(resId); }

    public void setRefreshingTextColor(int color) { mIndicatorRefreshingTextColor = color; }

    public void setCustomLayout(int resId, int textViewId) {
        mHeaderViewLayoutResId = resId;
        mTextViewId = textViewId;
        createHeaderView();
    }

    public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (mHeaderView.getVisibility() != View.VISIBLE && scrollY == 0) {
            show();
        }
    }

    public void onAttachedToWindow(ViewGroup rootView) {
        if (mHeaderView != null) return;

        mHeaderViewRect = new Rect();
        rootView.getWindowVisibleDisplayFrame(mHeaderViewRect);
        createHeaderView();
    }

    public void onDetatchedFromWindow() {
        destroyHeaderView();
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (visibleItemCount > 0 && firstVisibleItem == 0 && view.getChildAt(0).getTop() < 0) {
            hide();
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING &&
                mHeaderView.getVisibility() == View.VISIBLE) {
            hide();
        }
    }

    private void createHeaderView() {
        if (mHeaderViewRect == null) {
            return;
        }

        if (mHeaderViewLayoutResId != 0) {
            mHeaderView = View.inflate(mContext, mHeaderViewLayoutResId, null);
        } else {
            TextView tv = new TextView(mContext);
            tv.setBackgroundColor(mBackgroundColor);
            tv.setText(mIndicatorText);
            tv.setTextColor(mIndicatorTextColor);
            tv.setGravity(Gravity.CENTER);
            mHeaderView = tv;
        }
        mHeaderView.setVisibility(View.INVISIBLE);

        WindowManager.LayoutParams wlp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                mActionBarHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        wlp.x = 0;
        wlp.y = mHeaderViewRect.top == 0 ? mStatusBarHeight : mHeaderViewRect.top;
        wlp.gravity = Gravity.TOP;

        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mHeaderView, wlp);
    }

    private void destroyHeaderView() {
        if (mHeaderView != null) {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.removeViewImmediate(mHeaderView);
            mHeaderView = null;
        }
    }

    private void setHeaderText(String text, int color) {
        if (mHeaderView == null) {
            Log.w(TAG, "setHeaderText called before header view created");
            return;
        }

        TextView tv = (TextView)mHeaderView.findViewById(mTextViewId);
        if (tv == null && !(mHeaderView instanceof TextView)) {
            Log.w(TAG, "setHeaderText could not find TextView in header view");
            return;
        }
        if (mHeaderView instanceof TextView) {
            tv = (TextView)mHeaderView;
        }
        tv.setText(text);
        // Only set the color if we are using the default layout.
        // Otherwise, just let the layout determine the text color.
        if (mHeaderViewLayoutResId == 0) {
            tv.setTextColor(color);
        }
    }
}
