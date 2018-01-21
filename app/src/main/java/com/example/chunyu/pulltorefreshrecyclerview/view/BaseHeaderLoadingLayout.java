package com.example.chunyu.pulltorefreshrecyclerview.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by 人间一小雨 on 2018/1/21 下午9:25
 * Email: 746431278@qq.com
 */

public abstract class BaseHeaderLoadingLayout extends FrameLayout {


    public BaseHeaderLoadingLayout(@NonNull Context context) {
        super(context);
    }

    public BaseHeaderLoadingLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseHeaderLoadingLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract void reset();

    public abstract void releaseToRefresh();

    public abstract void setPullLabel(String var1);

    public abstract void refreshing();

    public abstract void setSubVisibleWhenRefreshing(boolean var1);

    public abstract void setRefreshingLabel(String var1);

    public abstract void setReleaseLabel(String var1);

    public abstract void pullToRefresh();

    public abstract void setTextSize(float var1);

    public abstract void setTextColor(ColorStateList var1);

    public abstract void setTextColor(int var1);

    public abstract void setSubTextSize(float var1);

    public abstract void setSubTextColor(ColorStateList var1);

    public abstract void setSubTextColor(int var1);

    public abstract void setSubHeaderText(CharSequence var1);

    public abstract void setLoadingDrawable(Drawable var1);

    public abstract void setPullDrawable(Drawable var1);

    public abstract void setReleaseDrawable(Drawable var1);

    public abstract void setPullAnimationEnabled(boolean var1);

    public abstract void setDividerVisible(boolean var1);

    public abstract void onPullY(float var1);
}
