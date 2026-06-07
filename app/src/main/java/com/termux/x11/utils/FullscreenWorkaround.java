package com.termux.x11.utils;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.termux.x11.MainActivity;
import com.termux.x11.Prefs;

public class FullscreenWorkaround {
    // For more information, see https://issuetracker.google.com/issues/36911528
    // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.

    public static void assistActivity(Activity activity) {
        new FullscreenWorkaround(activity);
    }

    private final Activity mActivity;
    private int usableHeightPrevious;

    private FullscreenWorkaround(Activity activity) {
        mActivity = activity;
        FrameLayout content = activity.findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnGlobalLayoutListener(this::possiblyResizeChildOfContent);
    }

    private void possiblyResizeChildOfContent() {
        Prefs p = MainActivity.getPrefs();
        if (
                !mActivity.hasWindowFocus() ||
                !((mActivity.getWindow().getAttributes().flags & FLAG_FULLSCREEN) == FLAG_FULLSCREEN) ||
                !p.Reseed.get() || !p.fullscreen.get() || SamsungDexUtils.checkDeXEnabled(mActivity)
        )
            return;

        ViewGroup contentRoot = mActivity.findViewById(android.R.id.content);
        if (contentRoot == null || contentRoot.getChildCount() == 0) {
            return;
        }
        View contentChild = contentRoot.getChildAt(0);
        ViewGroup.LayoutParams layoutParams = contentChild.getLayoutParams();
        if (layoutParams == null) {
            return;
        }

        int usableHeightNow = computeUsableHeight(contentChild);
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = contentChild.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
            if (heightDifference > (usableHeightSansKeyboard / 4)) {
                // keyboard probably just became visible
                layoutParams.height = usableHeightSansKeyboard - heightDifference;
            } else {
                // keyboard probably just became hidden
                layoutParams.height = usableHeightSansKeyboard;
            }
            contentChild.requestLayout();
            usableHeightPrevious = usableHeightNow;
        }
    }

    private int computeUsableHeight(View v) {
        Rect r = new Rect();
        v.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);
    }
}
