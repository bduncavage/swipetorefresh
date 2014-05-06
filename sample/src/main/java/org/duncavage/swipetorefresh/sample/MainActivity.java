package org.duncavage.swipetorefresh.sample;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.duncavage.swipetorefresh.widget.SwipeRefreshLayout;

public class MainActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener{
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Handler mHandler;

    private final Runnable mStopRefresh = new Runnable() {
        @Override
        public void run() {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mHandler = new Handler();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setActionBarSwipeIndicatorLayout(R.layout.swipe_indicator, R.id.text);
        mSwipeRefreshLayout.setActionBarSwipeIndicatorText(R.string.swipe_to_refresh);
        mSwipeRefreshLayout.setActionBarSwipeIndicatorRefreshingText(R.string.loading);
        mSwipeRefreshLayout.setColorScheme(
                R.color.refreshing_color1,
                R.color.refreshing_color2,
                R.color.refreshing_color3,
                R.color.refreshing_color4
        );
    }

    @Override
    public void onRefresh() {
        mHandler.postDelayed(mStopRefresh, 5000);
    }
}
