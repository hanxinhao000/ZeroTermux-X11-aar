package com.termux.x11.utils;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

import android.graphics.Rect;
import android.widget.FrameLayout;
import android.view.View;
import android.app.Activity;

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

        View viewById = mActivity.findViewById(android.R.id.content);
        if (viewById != null && viewById instanceof FrameLayout) {
            View childAt = ((FrameLayout) viewById).getChildAt(0);
            if (childAt != null && childAt instanceof FrameLayout) {
                FrameLayout content = (FrameLayout) childAt;
                FrameLayout.LayoutParams frameLayoutParams = (FrameLayout.LayoutParams) content.getLayoutParams();

                int usableHeightNow = computeUsableHeight(content);
                if (usableHeightNow != usableHeightPrevious) {
                    int usableHeightSansKeyboard = content.getRootView().getHeight();
                    int heightDifference = usableHeightSansKeyboard - usableHeightNow;
                    if (heightDifference > (usableHeightSansKeyboard/4)) {
                        // keyboard probably just became visible
                        frameLayoutParams.height = usableHeightSansKeyboard - heightDifference;
                    } else {
                        // keyboard probably just became hidden
                        frameLayoutParams.height = usableHeightSansKeyboard;
                    }
                    content.requestLayout();
                    usableHeightPrevious = usableHeightNow;
                }
            }
        }
    }

    private int computeUsableHeight(View v) {
        Rect r = new Rect();
        v.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);
    }
}