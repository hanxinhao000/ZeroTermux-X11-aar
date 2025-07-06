package com.termux.x11;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;

public class BaseLoader extends FrameLayout {
    private View mView;
    public Activity mActivity;

    public BaseLoader(Context context) {
        super(context);
        mActivity = (Activity) context;
        mView = View.inflate(context, R.layout.main_activity, null);
        addView(mView);
    }
   /* public BaseLoader(Activity activity) {
        mView = View.inflate(activity, R.layout.main_activity, null);
        mActivity = activity;
    }*/

}
