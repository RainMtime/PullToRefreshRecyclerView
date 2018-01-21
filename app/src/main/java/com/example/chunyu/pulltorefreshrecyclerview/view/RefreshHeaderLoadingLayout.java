package com.example.chunyu.pulltorefreshrecyclerview.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chunyu.pulltorefreshrecyclerview.R;

/**
 * Created by 人间一小雨 on 2018/1/21 下午9:22
 * Email: 746431278@qq.com
 */

public class RefreshHeaderLoadingLayout extends BaseHeaderLoadingLayout {


    private ViewGroup mRootView;

    private ImageView mPullHeaderImage;

    private TextView mPullHeaderTips;

    private Drawable mPullDrawable;

    private AnimationDrawable mRefreshDrawable;

    public RefreshHeaderLoadingLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mRootView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header_layout, this);
        mPullHeaderImage = (ImageView) mRootView.findViewById(R.id.radio_pull_header_image);
        mPullHeaderTips = (TextView) mRootView.findViewById(R.id.radio_pull_header_tips);
        mRefreshDrawable = (AnimationDrawable) context.getResources().getDrawable(R.drawable.pull_to_refresh_anim);
        mPullDrawable = context.getResources().getDrawable(R.drawable.bg_refresh_step01);
    }

    @Override
    public void reset() {
        mPullHeaderImage.setImageDrawable(mPullDrawable);
        mRefreshDrawable.stop();
    }

    @Override
    public void setPullLabel(String pullLabel) {
        mPullHeaderTips.setText(pullLabel);
    }

    @Override
    public void refreshing() {
        mPullHeaderImage.setImageDrawable(mRefreshDrawable);
        mRefreshDrawable.start();
        mPullHeaderTips.setText("正在刷新");
    }

    @Override
    public void releaseToRefresh() {

    }

    @Override
    public void setSubVisibleWhenRefreshing(boolean var1) {

    }

    @Override
    public void setRefreshingLabel(String var1) {

    }

    @Override
    public void setReleaseLabel(String var1) {

    }

    @Override
    public void pullToRefresh() {
        mPullHeaderImage.setImageDrawable(mPullDrawable);
    }

    @Override
    public void setTextSize(float var1) {

    }

    @Override
    public void setTextColor(ColorStateList var1) {

    }

    @Override
    public void setTextColor(int var1) {

    }

    @Override
    public void setSubTextSize(float var1) {

    }

    @Override
    public void setSubTextColor(ColorStateList var1) {

    }

    @Override
    public void setSubTextColor(int var1) {

    }

    @Override
    public void setSubHeaderText(CharSequence var1) {

    }

    @Override
    public void setLoadingDrawable(Drawable var1) {

    }

    @Override
    public void setPullDrawable(Drawable var1) {

    }

    @Override
    public void setReleaseDrawable(Drawable var1) {

    }

    @Override
    public void setPullAnimationEnabled(boolean var1) {

    }

    @Override
    public void setDividerVisible(boolean var1) {

    }

    @Override
    public void onPullY(float var1) {

    }
}
