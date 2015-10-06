The "Real" Swipe to Refresh
==============

![Swipe to Refresh in Action](http://i.imgflip.com/8meh7.gif)

I really don't like the stock SwipeRefreshLayout interaction. It moves the content, cancels the gesture after an arbitrary
timeout, and is buggy. So I've modified it to be more like the swipe to refresh we all have become familiar with (think Gmail app).

Repo Structure
=========
- **swipetorefresh/** - This is the library. Add it to your java Android app as a gradle dependency.
- **sample/** - This is the sample app. Reference it to see how to use the widget.
- **csharp/** - Contains a sample Xamarin.Android app and C# bindings for the library.

License
=======
This project is licensed under the [MIT license](LICENSE.md)
