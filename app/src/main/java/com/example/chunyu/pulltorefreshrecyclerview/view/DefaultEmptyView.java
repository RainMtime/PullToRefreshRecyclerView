package com.example.chunyu.pulltorefreshrecyclerview.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chunyu.pulltorefreshrecyclerview.R;

/**
 * Created by 人间一小雨 on 2018/1/21 下午9:42
 * Email: 746431278@qq.com
 */

public class DefaultEmptyView extends FrameLayout {

    private ImageView mEmptyIconView;
    private TextView mEmptyMsgView;

    private CharSequence mDefaultMsg;

    public DefaultEmptyView(Context context) {
        super(context);
        init();
    }

    public DefaultEmptyView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public DefaultEmptyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.pulltorefresh_default_empty_view, this);
        mEmptyIconView = (ImageView) findViewById(R.id.empty_icon);
        mEmptyMsgView = (TextView) findViewById(R.id.empty_msg);
    }

    public void setIcon(int resId) {
        mEmptyIconView.setImageResource(resId);
    }

    public void setMessage(int resId) {
        setMessage(getResources().getText(resId));
    }

    public void setMessage(CharSequence msg) {
        mEmptyMsgView.setText(TextUtils.isEmpty(msg) ? mDefaultMsg : msg);
    }

    public void setDefaultMessage(int resId) {
        setDefaultMessage(getResources().getText(resId));
    }

    public void setDefaultMessage(CharSequence msg) {
        mDefaultMsg = msg;
        mEmptyMsgView.setText(msg);
    }
}

