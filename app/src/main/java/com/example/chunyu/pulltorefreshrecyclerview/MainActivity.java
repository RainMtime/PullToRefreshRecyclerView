package com.example.chunyu.pulltorefreshrecyclerview;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.chunyu.pulltorefreshrecyclerview.view.PullToRefreshRecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private PullToRefreshRecyclerView mPullToRefreshRecyclerView;

    private DemoAdapter mAdapter;

    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAdapter();
    }


    private void initView() {
        mPullToRefreshRecyclerView = (PullToRefreshRecyclerView) findViewById(R.id.pulltorefreshrecyclerview);
        mPullToRefreshRecyclerView.setEnableLoadMore(true);
        mPullToRefreshRecyclerView.setOnLoadMoreListener(new PullToRefreshRecyclerView.OnLoadMoreListener() {
            @Override
            public boolean onLoadMore(PullToRefreshRecyclerView.EventSource source) {
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
        mPullToRefreshRecyclerView.setOnRefreshListener(new PullToRefreshRecyclerView.OnRefreshListener() {
            @Override
            public void onRefresh(PullToRefreshRecyclerView refreshView) {
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
    }

    private void initAdapter() {
        mAdapter = new DemoAdapter(this);
        mAdapter.setData(mockDatas());
        mPullToRefreshRecyclerView.setAdapter(mAdapter);
    }

    private ArrayList<String> mockDatas() {

        int start = count * 10;
        ArrayList<String> datas = new ArrayList<>();
        for (int i = start; i < start + 10; i++) {
            datas.add("我是第" + i + "个元素");
        }
        count++;
        return datas;
    }

    public static final class DemoAdapter extends RecyclerView.Adapter {

        private Context mContext;
        private ArrayList<String> mDatas = new ArrayList<>();

        public DemoAdapter(Context context) {
            mContext = context;
        }

        public void setData(ArrayList<String> datas) {
            if (datas != null) {
                mDatas.clear();
                mDatas.addAll(datas);
                notifyDataSetChanged();
            }
        }

        public void addData(ArrayList<String> datas) {
            if (datas != null) {
                mDatas.addAll(datas);
                notifyDataSetChanged();
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DemoItemVH(LayoutInflater.from(mContext).inflate(R.layout.recyclerview_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            String str = mDatas.get(position);
            ((DemoItemVH) holder).renderView(str);
        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }
    }

    public static final class DemoItemVH extends RecyclerView.ViewHolder {
        Button mbtn;

        public DemoItemVH(View itemView) {
            super(itemView);
            mbtn = (Button) itemView.findViewById(R.id.btn);
        }


        public void renderView(String str) {
            mbtn.setText(str);
        }
    }
}
