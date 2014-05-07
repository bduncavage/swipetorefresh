using System;
using Android.App;
using Android.Content;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.OS;
using SwipeToRefresh.Widget;

namespace SwipeToRefresharpSample
{
    [Activity(Label = "SwipeToRefresharpSample", MainLauncher = true, Theme = "@style/AppTheme")]
    public class MainActivity : Activity
    {
        Handler handler;
        SwipeRefreshLayout swipe_refresh_layout;

        protected override void OnCreate(Bundle bundle)
        {
            handler = new Handler();

            base.OnCreate(bundle);

            // Set our view from the "main" layout resource
            SetContentView(Resource.Layout.Main);

            swipe_refresh_layout = FindViewById<SwipeRefreshLayout>(Resource.Id.swipe_refresh_layout);
            // Setup the actionbar indicator
            swipe_refresh_layout.SetActionBarSwipeIndicatorLayout(Resource.Layout.swipe_indicator, Resource.Id.text);
            swipe_refresh_layout.SetActionBarSwipeIndicatorRefreshingText(Resource.String.loading);
            swipe_refresh_layout.SetActionBarSwipeIndicatorText(Resource.String.swipe_to_refresh);
            // Setup colors
            swipe_refresh_layout.SetColorScheme(
                Resource.Color.refreshing_color1,
                Resource.Color.refreshing_color2,
                Resource.Color.refreshing_color3,
                Resource.Color.refreshing_color4);
            // Set the listener
            swipe_refresh_layout.Refresh += OnRefresh;
        }

        private void OnRefresh(object sender, EventArgs args)
        {
            handler.PostDelayed(() => {
                swipe_refresh_layout.Refreshing = false;
            }, 3000);
        }
    }
}


