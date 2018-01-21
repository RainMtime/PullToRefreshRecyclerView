package com.example.chunyu.pulltorefreshrecyclerview.view;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.chunyu.pulltorefreshrecyclerview.R;
import com.example.chunyu.pulltorefreshrecyclerview.Utils.DateUtils;

/**
 * Created by 人间一小雨 on 2018/1/21 下午12:31
 * Email: 746431278@qq.com
 */

public class PullToRefreshRecyclerView extends LinearLayout {

    private OnRefreshListener mOnRefreshListener;


    private OnPullEventListener mOnPullEventListener;
    private boolean mShowViewWhileRefreshing = true;

    private static final int SMOOTH_ANIMATION_DURATION_SHORT = 300;

    private static final int SMOOTH_ANIMATION_DURATION_LONG = 600;
    private static final int SCROLL_ANIMATION_DURATION = 600;

    private final PullToRefreshOptions mOptions = new PullToRefreshOptions();

    private RecyclerViewAdapterWrapper mRecyclerViewAdapterWrapper;


    private final RecyclerView.AdapterDataObserver dataObserver = new DataObserver();

    private PullToRefreshRecyclerView.LoadLayout mLoadingView;


    private static final String TAG = "PullToRefresh";
    private final boolean DEBUG_LOG = false;

    private RecyclerView mRecyclerView;
    private RefreshHeaderLoadingLayout mHeaderRefreshLoadingView;
    private static final float FRICTION = 2.0f;

    private long mLastUpdateTime;

    private float mLastMotionX;
    private float mLastMotionY;
    private float mInitialMotionY;

    private int mPaddingLeft;
    private int mPaddingRight;
    private int mPaddingTop;
    private int mPaddingBottom;

    private int mHeaderHeight;
    private boolean mFilterTouchEvents = true;


    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;

    private boolean mIsBeingDragged = false;

    private int mState = PULL_TO_REFRESH;

    private static final int PULL_TO_REFRESH = 0x0;
    private static final int RELEASE_TO_REFRESH = 0x1;
    private static final int REFRESHING = 0x2;
    private static final int MANUAL_REFRESHING = 0x3;

    private boolean mDisableScrollingWhileRefreshing = true;

    private int mTouchSlop;
    private PullToRefreshRecyclerView.Mode mMode = Mode.PULL_DOWN_TO_REFRESH;

    private DefaultEmptyView mNoDataEmptyView;

    private boolean mEnableLoadMore = false;

    private OnLoadMoreListener mOnLoadMoreListener;

    private static final int TYPE_LOADING_VIEW = Integer.MIN_VALUE;

    private static final int TYPE_EMPTY_VIEW = Integer.MIN_VALUE + 1;


    public PullToRefreshRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public PullToRefreshRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }


    public PullToRefreshRecyclerView(Context context, Mode mode) {
        super(context);
        mMode = mode;
        init(context, null);
    }


    private void init(Context context, AttributeSet attrs) {
        setOrientation(LinearLayout.VERTICAL);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mRecyclerView = createRefreshableView(context, attrs);
        addViewInternal(mRecyclerView, -1, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
        mHeaderRefreshLoadingView = new RefreshHeaderLoadingLayout(context, null);
        updateUIForMode();
        //加载更多的View
        mLoadingView = new PullToRefreshRecyclerView.LoadLayout(context, new Runnable() {
            @Override
            public void run() {
                doLoadMoreOperation(EventSource.MANUAL);
            }
        });
        // 空页面数据的View
        mNoDataEmptyView = new DefaultEmptyView(context);
        // Todo Styleables from XML
    }

    /**
     * @param iconId 空界面展示的图标
     * @param msg    空界面展示的提示语
     */
    public void setEmptyViewData(@DrawableRes int iconId, String msg) {
        if (mNoDataEmptyView != null) {
            mNoDataEmptyView.setIcon(iconId);
            mNoDataEmptyView.setMessage(msg);
        }
    }

    /**
     * @param onRefreshListener 设置下拉刷新动作的监听。
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    /**
     * @param onPullEventListener 设置下拉事件监听（开始和结束事件）
     */
    public void setOnPullEventListener(OnPullEventListener onPullEventListener) {
        mOnPullEventListener = onPullEventListener;
    }

    /**
     * @param mode 设置当前这个组件的模式
     * @see PullToRefreshRecyclerView.Mode
     */
    public final void setMode(Mode mode) {
        if (mode != mMode) {
            if (DEBUG_LOG) {
                Log.d(TAG, "Setting mode to: " + mode);
            }
            mMode = mode;
            updateUIForMode();
        }
    }

    /**
     * Note：如果在使用的时候，存在RecyclerView 嵌套RecyclerView 的情况，可以调用此方法。
     * 这样可以通过改变外层RecyclerView的TouchSlop的阀值，改善滑动体验。
     */
    public final void optimizeRecyclerviewScroll() {
        if (mRecyclerView != null) {
            mRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        }
    }

    /**
     * @param adapter 页面使用的adapter
     *                使用RadioPullToRecycleView 的时候，设置adapter，务必使用这个接口
     *                如果使用RadioPullToRecycleView.getRecyclerView().setAdapter，会导致loadmore和空View的能力失效。
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (adapter != null) {
            mRecyclerViewAdapterWrapper = new RecyclerViewAdapterWrapper(adapter);
            mRecyclerView.setAdapter(mRecyclerViewAdapterWrapper);
            adapter.registerAdapterDataObserver(dataObserver);
            dataObserver.onChanged();
        }
    }

    /**
     * @return 返回setAdapter参数中，对应的那个Adapter。
     */
    @Nullable
    public RecyclerView.Adapter getAdapter() {
        return mRecyclerView.getAdapter();
    }


    /**
     * @param onLoadMoreListener 设置加载更多的监听Listener
     */
    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        mOnLoadMoreListener = onLoadMoreListener;
    }


    /**
     * @param enableLoadMore 设置能否加载更多，默认是false
     */
    public void setEnableLoadMore(boolean enableLoadMore) {
        if (mEnableLoadMore != enableLoadMore) {
            mEnableLoadMore = enableLoadMore;
            mLoadingView.setState(mEnableLoadMore ? PullToRefreshRecyclerView.LoadLayout.LOAD_MORE_PENDING : PullToRefreshRecyclerView.LoadLayout.HIDE);
            if (mRecyclerViewAdapterWrapper != null) {
                mRecyclerViewAdapterWrapper.notifyDataSetChanged();
            }
        }
    }


    /**
     * @param hasMore 是否还有更多数据
     *                加载更多动作完成后，需要设置一下， 这个函数会改变loadmoreview对应的状态（加载态，隐藏态等）
     */
    public void setLoadMoreComplete(boolean hasMore) {
        if (hasMore) {
            mLoadingView.setState(PullToRefreshRecyclerView.LoadLayout.LOAD_MORE_PENDING);
        } else {
            mLoadingView.setState(PullToRefreshRecyclerView.LoadLayout.NO_MORE_DATA);
        }
    }

    public void setLoadMoreTextNoMore(String noMoreText) {
        mLoadingView.setNoMoreDataText(noMoreText);
    }


    protected void updateUIForMode() {
        if (this == mHeaderRefreshLoadingView.getParent()) {
            removeView(mHeaderRefreshLoadingView);
        }

        if (mMode.canPullDownToRefresh()) {
            addViewInternal(mHeaderRefreshLoadingView, 0, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        refreshHeaderLoadingViewsHeight();
    }

    private void refreshHeaderLoadingViewsHeight() {
        mHeaderHeight = 0;
        if (mMode.canPullDownToRefresh()) {
            measureView(mHeaderRefreshLoadingView);
            mHeaderHeight = mHeaderRefreshLoadingView.getMeasuredHeight();
            if (DEBUG_LOG) {
                Log.i(TAG, "mHeaderHeight:" + mHeaderHeight);
            }
        }

        // Hide Loading Views
        int left = mPaddingLeft, right = mPaddingRight;
        int top = mPaddingTop, bottom = mPaddingBottom;
        switch (mMode) {
            case DISABLED:
                setPadding(left, top, right, bottom);
                break;
            case PULL_DOWN_TO_REFRESH:
            default:
                setPadding(left, top - mHeaderHeight, right, bottom);
                break;
        }

    }
    // 暂不开启，先保留
    //    public final void setPullPadding(int left, int top, int right, int bottom) {
    //        if (mPaddingLeft != left || mPaddingTop != top || mPaddingRight != right || mPaddingBottom != bottom) {
    //            mPaddingLeft = left;
    //            mPaddingRight = right;
    //            mPaddingTop = top;
    //            mPaddingBottom = bottom;
    //            refreshHeaderLoadingViewsHeight();
    //        }
    //    }


    @NonNull
    public RecyclerView getRecyclerView() {
        return this.mRecyclerView;
    }

    private void addViewInternal(View child, int index, LayoutParams params) {
        super.addView(child, index, params);
    }


    @NonNull
    protected RecyclerView createRefreshableView(Context context, AttributeSet attrs) {
        mRecyclerView = new InnerRecycler(context);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!mEnableLoadMore || mRecyclerViewAdapterWrapper == null || mRecyclerViewAdapterWrapper.isOriginAdapterEmpty()) {
                    return;
                }

                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                int index = -1;
                if (layoutManager instanceof LinearLayoutManager) {
                    index = ((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition();
                    if (index != RecyclerView.NO_POSITION && index == mRecyclerViewAdapterWrapper.getItemCount() - 1) {
                        //mean scroll the LoadingView
                        if (null != mOnLoadMoreListener) {
                            doLoadMoreOperation(PullToRefreshRecyclerView.EventSource.AUTO);
                        }
                    }

                } else if (layoutManager instanceof GridLayoutManager) {
                    //to do Logic for GridLayoutManager
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        //TypedArray a  =context.obtainStyledAttributes();
        return mRecyclerView;
    }

    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPullToRefreshEnabled()) {
            return false;
        }

        if (mDisableScrollingWhileRefreshing && isRefreshing()) {
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }


        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (mIsBeingDragged) {
                    mLastMotionY = event.getY();
                    pullEvent();
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {
                    mLastMotionY = mInitialMotionY = event.getY();
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;
                    dispatchPullEnd();

                    if (mState == RELEASE_TO_REFRESH) {
                        if (null != mOnRefreshListener) {
                            setRefreshingInternal(true);
                            mOnRefreshListener.onRefresh(this);
                            return true;
                        } else {
                            resetHeader();
                            return true;
                        }
                    }
                    smoothScrollTo(determineReleaseScrollY());
                    return true;
                }
                break;
            }
            default:
                break;
        }


        return super.onTouchEvent(event);
    }

    protected void setRefreshingInternal(boolean doScroll) {
        mState = REFRESHING;
        if (mMode.canPullDownToRefresh()) {
            mHeaderRefreshLoadingView.refreshing();
        }

        if (doScroll) {
            if (mShowViewWhileRefreshing) {
                smoothScrollTo(mMode == Mode.PULL_DOWN_TO_REFRESH ? -mHeaderHeight : 0);
            } else {
                smoothScrollTo(0);
            }
        }
        onRefreshing();
    }

    protected void onRefreshing() {


    }


    private boolean pullEvent() {
        final int newHeight;
        final int oldHeight = getScrollY();

        int pullHeight;
        int maxHeight;

        switch (mMode) {
            case PULL_DOWN_TO_REFRESH:
            default:
                pullHeight = Math.round(Math.min(mInitialMotionY - mLastMotionY, 0) / FRICTION);
                maxHeight = mOptions.maxPullDownLimit > 0 ? max(mOptions.maxPullDownLimit, mHeaderHeight + 1) : -1;
                newHeight = maxHeight > 0 ? max(-maxHeight, pullHeight) : pullHeight;
                break;
        }

        setHeaderScroll(newHeight);

        if (newHeight != 0) {
            //// TODO: 2017/9/25  headerView/FooterView action by scale
            float scale = Math.abs(newHeight) / (float) mHeaderHeight;

            int height = mHeaderHeight;
            if (mState == PULL_TO_REFRESH && height < Math.abs(newHeight)) {
                mState = RELEASE_TO_REFRESH;
                onReleaseToRefresh();
                return true;
            } else if (mState == RELEASE_TO_REFRESH && height >= Math.abs(newHeight)) {
                mState = PULL_TO_REFRESH;
                onPullToRefresh();
                return true;
            }
        }
        return oldHeight != newHeight;
    }

    /**
     * Called when the UI needs to be updated to the 'Pull to Refresh' state
     */
    protected void onPullToRefresh() {
        switch (mMode) {
            case PULL_DOWN_TO_REFRESH:
                //mHeaderLayout.pullToRefresh();
                if (DEBUG_LOG) {
                    Log.i(TAG, "down-onPullToRefresh");
                }
                break;
            default:
                break;
        }
    }


    /**
     * Called when the UI needs to be updated to the 'Release to Refresh' state
     */
    protected void onReleaseToRefresh() {
        switch (mMode) {
            case PULL_DOWN_TO_REFRESH:
                if (DEBUG_LOG) {
                    Log.i(TAG, "Down-onReleaseToRefresh");
                }
                //mHeaderLayout.releaseToRefresh();
                break;
            default:
                break;
        }
    }

    /**
     * Mark the current Refresh as complete. Will Reset the UI and hide the Refreshing View
     *
     * @parma succeed
     */
    public void setRefreshComplete(boolean succeed) {
        if (mState != PULL_TO_REFRESH) {
            resetHeader();
            onRefreshComplete(succeed);
            // TODO: 2017/9/25 refreshListener
            if (null != mOnRefreshListener) {
                mOnRefreshListener.onRefreshComplete(this);
            }
        }
        if (succeed) {
            setLastUpdateTime(0);
        }
    }

    public void setRefreshing() {
        if (!isRefreshing()) {
            setRefreshingInternal(true);
            mState = MANUAL_REFRESHING;
            if (null != mOnRefreshListener) {
                mOnRefreshListener.onRefresh(this);
            }
        }


    }

    protected void onRefreshComplete(boolean succeed) {

    }


    private static int max(int a, int b) {
        return a > b ? a : b;
    }

    private static int min(int a, int b) {
        return a < b ? a : b;
    }

    public final void setPullLimit(int limit, Mode mode) {
        if (mode.canPullDownToRefresh()) {
            mOptions.maxPullDownLimit = limit;
        }
    }

    public final int getPullDownLimit() {
        if (mOptions == null) {
            return 0;
        }
        return mOptions.maxPullDownLimit;
    }

    public final int getPullUpLimit() {
        if (mOptions == null) {
            return 0;
        }
        return mOptions.maxPullUpLimit;
    }

    /**
     * Determine the scroll y position when release.
     *
     * @return scroll y position when release.
     */
    protected int determineReleaseScrollY() {
        return 0;
    }


    protected void resetHeader() {
        final boolean afterRefresh = isRefreshing();
        mState = PULL_TO_REFRESH;

        if (mIsBeingDragged) {
            mIsBeingDragged = false;
            dispatchPullEnd();
        }

        if (mMode.canPullDownToRefresh()) {
            mHeaderRefreshLoadingView.reset();
        }

        if (!afterRefresh) {
            smoothScrollTo(0);
        } else {
            smoothScrollToSlowly(0);
        }

    }


    protected final void smoothScrollTo(int y) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        if (getScrollY() != y) {
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(getScrollY(), y, SMOOTH_ANIMATION_DURATION_SHORT);
            post(mCurrentSmoothScrollRunnable);
        }
    }

    protected final void smoothScrollToSlowly(int y) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        if (getScrollY() != y) {
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(getScrollY(), y, SMOOTH_ANIMATION_DURATION_LONG);
            post(mCurrentSmoothScrollRunnable);
        }
    }

    private void circleSmoothScrollTo(int y) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        if (getScrollY() != y) {
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(getScrollY(), y, SCROLL_ANIMATION_DURATION, new CycleInterpolator(0.5f));
            post(mCurrentSmoothScrollRunnable);
        }
    }

    /**
     * Whether Pull-to-Refresh is enabled
     *
     * @return enabled
     */
    public final boolean isPullToRefreshEnabled() {
        return mMode != Mode.DISABLED;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!isCanPull()) {
            return false;
        }

        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsBeingDragged = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {

                // make LoadingLayout touchable while refreshing @willis
                if (touchInLoadingLayout(ev) && isRefreshing()) {
                    return false;
                }

                if (mDisableScrollingWhileRefreshing && isRefreshing()) {
                    return true;
                }

                if (isReadyForPull()) {
                    final float y = ev.getY();
                    final float dy = y - mLastMotionY;
                    final float yDiff = Math.abs(dy);
                    final float xDiff = Math.abs(ev.getX() - mLastMotionX);

                    if (yDiff > mTouchSlop && (!mFilterTouchEvents || yDiff > xDiff)) {
                        if (mMode.canPullDownToRefresh() && dy >= 1f && isReadyForPullDownToRefresh()) {
                            mLastMotionY = y;
                            mIsBeingDragged = true;
                            dispatchPullStart();
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {
                    mLastMotionY = mInitialMotionY = ev.getY();
                    mLastMotionX = ev.getX();
                    mIsBeingDragged = false;
                }
                break;
            }
            default:
                break;

        }

        return mIsBeingDragged;
    }

    private void dispatchPullStart() {
        onPullStart();
        if (mOnPullEventListener != null) {
            mOnPullEventListener.onPullStart();
        }
    }

    private void dispatchPullEnd() {
        onPullEnd();
        if (mOnPullEventListener != null) {
            mOnPullEventListener.onPullEnd();
        }
    }

    protected void onPullStart() {
        if (DEBUG_LOG) {
            Log.i(TAG, "onPullStart()");
        }
        if (mLastUpdateTime != 0) {
            setLastUpdateTime(mLastUpdateTime);
        }
    }


    protected void onPullEnd() {
        if (DEBUG_LOG) {
            Log.i(TAG, "onPullEnd");
        }
    }

    public void setLastUpdateTime(long time) {
        if (time == 0) {
            //// TODO: 2017/11/7  time maybe not real time
            time = System.currentTimeMillis();
        }
        mLastUpdateTime = time;
        setPulltoLabel(getRefreshTips(time), Mode.PULL_DOWN_TO_REFRESH);
    }

    public String getRefreshTips(long refreshTime) {
        long diff = (System.currentTimeMillis() - refreshTime) / (1000 * 60);

        if (diff < 0) {
            return getResources().getString(R.string.pull_refresh_pull_label);
        } else if (diff == 0) {
            return getResources().getString(R.string.pull_refresh_just_tips);
        } else if (diff < 60) {
            return getResources().getString(R.string.pull_refresh_min_tips, diff);
        } else if (diff < 60 * 24) {
            diff = diff / 60;
            return getResources().getString(R.string.pull_refresh_hours_tips, diff);
        } else if (diff < 60 * 24 * 3) {
            diff = diff / 24;
            return getResources().getString(R.string.pull_refresh_day_tips, diff);
        }

        return getResources().getString(R.string.pull_refresh_date_tips, DateUtils.getBirthString(refreshTime));
    }


    public void setPulltoLabel(String pulltoLabel, Mode mode) {
        if (null != mHeaderRefreshLoadingView && mode.canPullDownToRefresh()) {
            mHeaderRefreshLoadingView.setPullLabel(pulltoLabel);
        }
    }

    private boolean touchInLoadingLayout(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY() - mHeaderHeight;

        if (mHeaderRefreshLoadingView != null) {
            if (x > mHeaderRefreshLoadingView.getLeft() && x < mHeaderRefreshLoadingView.getRight() &&
                    y > mHeaderRefreshLoadingView.getTop() && y < mHeaderRefreshLoadingView.getBottom()) {
                return true;
            }
        }


        return false;
    }


    public final boolean isRefreshing() {
        return mState == REFRESHING || mState == MANUAL_REFRESHING;
    }


    public final boolean isCanPull() {
        return mMode != Mode.DISABLED;
    }


    private boolean isReadyForPull() {
        switch (mMode) {
            case PULL_DOWN_TO_REFRESH:
                return isReadyForPullDownToRefresh();
            default:
                return false;
        }
    }

    protected boolean isReadyForPullDownToRefresh() {
        //RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        return !mRecyclerView.canScrollVertically(-1);
    }

    protected boolean isReadyForPullUpToLoadMore() {
        return !mRecyclerView.canScrollVertically(1);
    }


    public enum Mode {
        DISABLED(0x0), //禁用一切能力
        PULL_DOWN_TO_REFRESH(0x1); //下拉刷新能力 ,默认行为

        private int mIntValue;

        public static PullToRefreshRecyclerView.Mode mapIntToMode(int mode) {
            switch (mode) {
                case 0x0:
                    return DISABLED;
                case 0x1:
                    return PULL_DOWN_TO_REFRESH;
                default:
                    return PULL_DOWN_TO_REFRESH;
            }
        }

        Mode(int modeInt) {
            mIntValue = modeInt;
        }

        boolean canPullDownToRefresh() {
            return this.getIntValue() == PULL_DOWN_TO_REFRESH.getIntValue();
        }


        public int getIntValue() {
            return mIntValue;
        }
    }

    // TODO: 2017/9/22 Method name!
    protected final void setHeaderScroll(int y) {
        scrollTo(0, y);
    }


    private final class SmoothScrollRunnable implements Runnable {

        static final int ANIMATION_DURATION_MS = 300;
        static final int ANIMATION_DELAY = 10;
        static final float ANIMATION_OVERSHOOT_TENSION = 0.0f;

        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;

        private int mDuration = ANIMATION_DURATION_MS;

        public SmoothScrollRunnable(int fromY, int toY) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mInterpolator = new OvershootInterpolator(ANIMATION_OVERSHOOT_TENSION);
        }

        public SmoothScrollRunnable(int fromY, int toY, int duration) {
            this(fromY, toY);
            mDuration = duration;
        }

        public SmoothScrollRunnable(int fromY, int toY, int duration, Interpolator interpolator) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mDuration = duration;
            mInterpolator = interpolator;
        }

        @Override
        public void run() {

            /**
             * Only set mStartTime if this is the first time we're starting, else actually calculate the Y delta
             */
            long currentTime = System.currentTimeMillis();
            if (mStartTime == -1) {
                mStartTime = currentTime;
            } else {

                /**
                 * We do do all calculations in long to reduce software float calculations. We use 1000 as it gives us
                 * good accuracy and small rounding errors
                 */
                long normalizedTime = (1000 * (currentTime - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY) * mInterpolator.getInterpolation(normalizedTime / 1000f));
                mCurrentY = mScrollFromY - deltaY;
                setHeaderScroll(mCurrentY);

            }

            // If we're not at the target Y, keep going...
            if (mContinueRunning && (currentTime - mStartTime < mDuration)) {
                // if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                // SDK16.postOnAnimation(PullToRefreshBase.this, this);
                // } else {
                postDelayed(this, ANIMATION_DELAY);
                // }
            }
        }

        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }

    }


    public static class PullToRefreshOptions {
        public int maxPullDownLimit;
        public int maxPullUpLimit;
    }


    public interface OnPullEventListener {

        void onPullStart();

        void onPullEnd();
    }


    public interface OnRefreshListener {
        void onRefresh(final PullToRefreshRecyclerView refreshView);

        void onRefreshComplete(final PullToRefreshRecyclerView refreshView);
    }


    public enum EventSource {
        AUTO, MANUAL
    }


    private void doLoadMoreOperation(EventSource source) {
        if (!mEnableLoadMore) {
            return;
        }

        if (!mLoadingView.checkState(PullToRefreshRecyclerView.LoadLayout.LOADING_MORE)) {
            return;
        }

        mLoadingView.setState(PullToRefreshRecyclerView.LoadLayout.LOADING_MORE);

        if (null != mOnLoadMoreListener) {
            mOnLoadMoreListener.onLoadMore(source);
        }
    }

    public class BaseLoadMoreView extends RecyclerView.ItemDecoration {

    }

    private class RecyclerViewAdapterWrapper extends RecyclerView.Adapter {

        private final RecyclerView.Adapter mAdapter;

        public RecyclerViewAdapterWrapper(@NonNull RecyclerView.Adapter adapter) {
            mAdapter = adapter;
            this.setHasStableIds(true);
        }

        public boolean isOriginAdapterEmpty() {
            return mAdapter.getItemCount() <= 0;
        }

        public RecyclerView.Adapter getAdapter() {
            return mAdapter;
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            if (!(holder instanceof EmptyViewViewHolder || holder instanceof LoadingViewViewHolder)) {
                mAdapter.onViewRecycled(holder);
            } else {
                super.onViewRecycled(holder);
            }
        }


        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            mAdapter.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            mAdapter.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
            if (!(holder instanceof EmptyViewViewHolder || holder instanceof LoadingViewViewHolder)) {
                return mAdapter.onFailedToRecycleView(holder);
            } else {
                return super.onFailedToRecycleView(holder);
            }
        }


        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            if (!(holder instanceof EmptyViewViewHolder || holder instanceof LoadingViewViewHolder)) {
                mAdapter.onViewDetachedFromWindow(holder);
            } else {
                super.onViewDetachedFromWindow(holder);
            }
        }


        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            if (!(holder instanceof EmptyViewViewHolder || holder instanceof LoadingViewViewHolder)) {
                mAdapter.onViewAttachedToWindow(holder);
            } else {
                super.onViewAttachedToWindow(holder);
            }
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_EMPTY_VIEW:
                    return createEmptyView(mNoDataEmptyView);
                case TYPE_LOADING_VIEW:
                    return createLoadingViewViewHolder(mLoadingView);
                default:
                    return mAdapter.onCreateViewHolder(parent, viewType);
            }
        }

        @NonNull
        protected PullToRefreshRecyclerView.EmptyViewViewHolder createEmptyView(View itemView) {
            removeViewFromParent(itemView);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            itemView.setLayoutParams(params);
            return new PullToRefreshRecyclerView.EmptyViewViewHolder(itemView);
        }


        @NonNull
        protected PullToRefreshRecyclerView.LoadingViewViewHolder createLoadingViewViewHolder(View itemView) {
            removeViewFromParent(itemView);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            itemView.setLayoutParams(params);
            return new PullToRefreshRecyclerView.LoadingViewViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (position < mAdapter.getItemCount()) {
                mAdapter.onBindViewHolder(viewHolder, position);
            } else {
                bindLoadingViewHolder(viewHolder);
            }
        }

        protected void bindLoadingViewHolder(RecyclerView.ViewHolder holder) {
            // do nothing
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            //无元素展示的时候显示了空View，同时也无。
            if (isOriginAdapterEmpty()) {
                return 1;
            }

            return mEnableLoadMore ? mAdapter.getItemCount() + 1 : mAdapter.getItemCount();
        }

        @Override
        public int getItemViewType(int position) {
            if (isOriginAdapterEmpty()) {
                return TYPE_EMPTY_VIEW;
            }
            if (position >= 0 && position < mAdapter.getItemCount()) {
                int type = mAdapter.getItemViewType(position);
                if (type == TYPE_LOADING_VIEW || type == TYPE_EMPTY_VIEW) {
                    throw new IllegalStateException("view Type error! place change viewType");
                }
                return type;
            }
            return TYPE_LOADING_VIEW;
        }
    }


    private static final class LoadingViewViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static final class EmptyViewViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewViewHolder(View itemView) {
            super(itemView);
        }
    }


    private class DataObserver extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            if (mRecyclerViewAdapterWrapper != null) {
                mRecyclerViewAdapterWrapper.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (mRecyclerViewAdapterWrapper != null) {
                mRecyclerViewAdapterWrapper.notifyItemRangeInserted(positionStart, itemCount);
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {

            if (mRecyclerViewAdapterWrapper != null) {
                mRecyclerViewAdapterWrapper.notifyItemRangeChanged(positionStart, itemCount);
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            if (mRecyclerViewAdapterWrapper != null) {
                mRecyclerViewAdapterWrapper.notifyItemRangeChanged(positionStart, itemCount, payload);
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {

            if (mRecyclerViewAdapterWrapper != null) {
                mRecyclerViewAdapterWrapper.notifyItemRangeRemoved(positionStart, itemCount);
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (mRecyclerViewAdapterWrapper != null) {
                mRecyclerViewAdapterWrapper.notifyItemMoved(fromPosition, toPosition);
            }
        }
    }

    public interface OnLoadMoreListener {
        boolean onLoadMore(EventSource source);
    }

    public static void removeViewFromParent(View view) {
        if (view == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
    }


    private static class InnerRecycler extends RecyclerView {
        public InnerRecycler(Context context) {
            super(context);
        }

        public InnerRecycler(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public InnerRecycler(Context context, @Nullable AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public Adapter getAdapter() {
            Adapter adapter = super.getAdapter();
            if (adapter instanceof PullToRefreshRecyclerView.RecyclerViewAdapterWrapper) {
                return ((PullToRefreshRecyclerView.RecyclerViewAdapterWrapper) adapter).getAdapter();
            }
            return adapter;
        }
    }


    public static class LoadLayout extends FrameLayout {

        public static final int HIDE = 0;
        public static final int LOADING = 1;
        public static final int LOADING_MORE = 2;
        public static final int LOAD_MORE_PENDING = 3;
        public static final int NO_MORE_DATA = 4;

        private Context mContext;

        private LinearLayout mLoadLayout;
        private ProgressBar mProgressBar;
        private TextView mTextView;
        private View mDivider;

        private int mState = -1;

        private String mLoadMoreText;
        private String mLoadingMoreText;
        private String mNoMoreDataText;
        private String mLoadingText;

        private final Runnable mOnLoadMoreClickAction;

        public LoadLayout(Context context, Runnable onLoadMoreClickAction) {
            super(context);

            mOnLoadMoreClickAction = onLoadMoreClickAction;

            mContext = context;
            initResources();
            initView();
            bindEvents();
        }

        public void setLoadMoreText(String loadManualText) {
            mLoadMoreText = loadManualText;
        }

        public void setLoadingMoreText(String loadingText) {
            mLoadingMoreText = loadingText;
        }

        public void setNoMoreDataText(String noMoreDataText) {
            mNoMoreDataText = noMoreDataText;
        }

        public void setLoadingText(String loadingText) {
            mLoadingText = loadingText;
        }

        public boolean setState(int state) {
            return updateState(state);
        }

        public int getState() {
            return mState;
        }

        public boolean checkState(int state) {
            return checkStateSwitch(mState, state);
        }

        public void setVisible(boolean visible) {
            mLoadLayout.setVisibility(visible ? VISIBLE : GONE);
        }

        public void setDividerVisible(boolean visible) {
            mDivider.setVisibility(visible ? VISIBLE : GONE);
        }

        private void initResources() {
            mLoadMoreText = getResources().getString(R.string.load_more);
            mLoadingMoreText = getResources().getString(R.string.loading_more);
            mNoMoreDataText = getResources().getString(R.string.load_more_no_data);
            mLoadingText = getResources().getString(R.string.loading);
        }

        private void initView() {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mLoadLayout = (LinearLayout) inflater.inflate(R.layout.radio_widget_pull_load_more, null);

            mProgressBar = (ProgressBar) mLoadLayout.findViewById(R.id.progress);
            mTextView = (TextView) mLoadLayout.findViewById(R.id.text);
            mDivider = mLoadLayout.findViewById(R.id.divider);

            FrameLayout.LayoutParams layoutParams = new LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            addView(mLoadLayout, layoutParams);

            updateState(HIDE);
        }

        private void bindEvents() {
            mLoadLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnLoadMoreClickAction != null) {
                        mOnLoadMoreClickAction.run();
                    }
                }
            });
        }

        private boolean updateState(int state) {
            if (!checkStateSwitch(mState, state)) {
                return false;
            }
            mState = state;
            switch (state) {
                case LOAD_MORE_PENDING:
                    mProgressBar.setVisibility(View.GONE);
                    mTextView.setText(mLoadMoreText);
                    mLoadLayout.setVisibility(View.VISIBLE);
                    break;

                case LOADING_MORE:
                    mProgressBar.setVisibility(View.VISIBLE);
                    mTextView.setText(mLoadingMoreText);
                    mLoadLayout.setVisibility(View.VISIBLE);
                    break;

                case NO_MORE_DATA:
                    mProgressBar.setVisibility(View.GONE);
                    mTextView.setText(mNoMoreDataText);
                    mLoadLayout.setVisibility(View.VISIBLE);
                    break;

                case LOADING:
                    mProgressBar.setVisibility(View.VISIBLE);
                    mTextView.setText(mLoadingText);
                    mLoadLayout.setVisibility(View.VISIBLE);
                    break;

                case HIDE:
                    mLoadLayout.setVisibility(View.GONE);
                    break;

                default:
                    break;
            }
            return true;
        }

        private boolean checkStateSwitch(int oldState, int newState) {
            if (oldState < 0) {
                return true;
            }
            switch (oldState) {
                case HIDE:
                    return true;

                case LOADING:
                    if (newState == HIDE
                            || newState == LOAD_MORE_PENDING
                            || newState == NO_MORE_DATA) {
                        return true;
                    }
                    break;

                case LOADING_MORE:
                    if (newState == HIDE
                            || newState == LOAD_MORE_PENDING
                            || newState == NO_MORE_DATA) {
                        return true;
                    }
                    break;

                case LOAD_MORE_PENDING:
                    if (newState == HIDE
                            || newState == LOADING
                            || newState == LOADING_MORE
                            || newState == NO_MORE_DATA) {
                        return true;
                    }
                    break;

                case NO_MORE_DATA:
                    if (newState == HIDE
                            || newState == LOADING
                            || newState == LOAD_MORE_PENDING) {
                        return true;
                    }
                    break;

                default:
                    break;
            }

            return false;
        }
    }
}
