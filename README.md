# PullToRefreshRecyclerView [![](https://img.shields.io/badge/version-1.0.0-blue.svg)]()  [![](https://img.shields.io/badge/License-MIT-blue.svg)]()

# 概述
一个有下拉刷新能力的RecyclerView，交互效果类似与PullToRefreshListView

#特点如下
- 支持下拉刷新效果如下图
- 支持加载更多效果
- 支持adapter中data为empty的时候，加载更多效果

#效果图如下：
- 下拉刷新效果：
![pull01](https://lh3.googleusercontent.com/-vLpZeu04yy4/Wm7FhsnDPLI/AAAAAAAAEy8/tyRGnsIJKaE8pNignzAiKwzWn_jdEO13gCHMYCw/I/pull01.gif)


- 加载更多效果： 
![pull02](https://lh3.googleusercontent.com/-OlunKjTqP6E/Wm7FjJUoSQI/AAAAAAAAEzA/6u7z7xE5XTYqqVlYTUjG_-l4bfXdUzs9QCHMYCw/I/pull02.gif)


- 空界面效果：
![pull3](https://lh3.googleusercontent.com/-YtbnsbuVI2w/Wm7FAW_dJwI/AAAAAAAAExk/4GyfPabWmH8j_InLMY5VJKtqx1lSDpwdACHMYCw/I/pull3.gif)


#使用方法：
- xml

```
    <com.example.chunyu.pulltorefreshrecyclerview.view.PullToRefreshRecyclerView
        android:id="@+id/pulltorefreshrecyclerview"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    </com.example.chunyu.pulltorefreshrecyclerview.view.PullToRefreshRecyclerView>
    
```


- 简单的demo代码如下：
```

  //设置下拉刷新模式（还有一种DISABLE）模式，此种模式不具备下拉刷新能力
        mPullToRefreshRecyclerView.setMode(PullToRefreshRecyclerView.Mode.PULL_DOWN_TO_REFRESH);
        //设置是否支持加载更多的能力
        mPullToRefreshRecyclerView.setEnableLoadMore(true);

        //当设置能加载更多，可以设置加载更多监听
        mPullToRefreshRecyclerView.setOnLoadMoreListener(new PullToRefreshRecyclerView.OnLoadMoreListener() {
            @Override
            public boolean onLoadMore(PullToRefreshRecyclerView.EventSource source) {
                //onLoadMore 动作就是当内容滑倒底部的时候，进行加载更多时候进行调用
                //一般这里放上加载更多的请求，下面进行模拟这个动作。
                                mPullToRefreshRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.addData(mockDatas());
                        mPullToRefreshRecyclerView.setLoadMoreComplete(true);
                    }
                }, 1000);
                return false;
            }
        });

        //当设置的MODE 为PULL_DOWN_TO_REFRESH 的时候，可以设置加载更多监听
        mPullToRefreshRecyclerView.setOnRefreshListener(new PullToRefreshRecyclerView.OnRefreshListener() {
            @Override
            public void onRefresh(PullToRefreshRecyclerView refreshView) {
                //onRefresh 中动作就是当下拉超过距离的阀值，准备刷新的时候调用
                //一般这里放置刷新请求的动作，下面进行模拟这个动作。
                mPullToRefreshRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.setData(mockDatas());
                        mPullToRefreshRecyclerView.setRefreshComplete(true);
                    }
                }, 1000);
            }

            @Override
            public void onRefreshComplete(PullToRefreshRecyclerView refreshView) {

            }
        });

```

#实现原理：

- 下拉效果，主要是利用LinearLayout的设置-padding的效果，把头部的刷新view 给隐藏了起来。然后就处理下拉事件，改变状态即可
- 加载更多效果，主要是把LoadingView 当作Adapter中的一个item进行处理
- 空界面的效果，当adapter里面的Data为空的时候，返回的size = 1，其实就是空View（这个size控制的动作，无需外部控制，内部已经控制好了，外部业务逻辑写adapter，就按照官方的规则写就行啦）。


# Demo参考：
这个项目有个app的module，里面有个简单的实例（就是的效果图的示例）



