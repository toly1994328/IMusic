package com.toly1994.tolymusic.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.itf.OnBaseClickListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


public class SearchHomeFragment extends Fragment {
    private View view;
    private RecyclerView rv_search;
    private SearchAdapter sAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<String> keyList;
    private SharedPreferences sp;
    private LinearLayout ll_clean_key;
    private TextView tv_more_title;
    private ExchangeCallBack callBack;

    public interface ExchangeCallBack {
        void startSearchByKey(String key);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            callBack = (ExchangeCallBack) context;
        } catch (ClassCastException e) {
            throw new RuntimeException("父Activity必须实现必要的接口");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_search_home, null);
        }
        setViewComponent();
        return view;
    }

    private void setViewComponent() {
        // 默认弹出输入法
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        sp = getActivity().getSharedPreferences("search_history",
                Context.MODE_PRIVATE);

        ll_clean_key = view.findViewById(R.id.ll_clean_key);
        // 设置清除浏览记录按钮样式
        tv_more_title = view.findViewById(R.id.tv_more_title);
        tv_more_title.setText(getResources().getString(
                R.string.clean_search_key));
        // 设置RecyclerView
        rv_search = view.findViewById(R.id.rv_search);
        mLayoutManager = new LinearLayoutManager(getActivity());
        rv_search.setLayoutManager(mLayoutManager);
        rv_search.setItemAnimator(new DefaultItemAnimator());
        rv_search.setHasFixedSize(true);
        // 设置数据
        keyList = new ArrayList<>();
        Iterator<?> it = sp.getAll().entrySet().iterator();
        while (it.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, ?> entry = (Entry<String, ?>) it.next();
            String key = (String) entry.getValue();
            keyList.add(key);
        }

        sAdapter = new SearchAdapter();
        rv_search.setAdapter(sAdapter);
        sAdapter.setOnItemClickListener((view, position) -> {
            // 执行搜索
            TextView tv = view.findViewById(R.id.tv_search_key);
            String key = tv.getText().toString();
            callBack.startSearchByKey(key);
        });

        // 清除搜索记录
        ll_clean_key.setOnClickListener(v -> {
            keyList.clear();
            sAdapter.notifyDataSetChanged();
            sp.edit().clear().commit();
            ll_clean_key.setVisibility(View.GONE);
        });

        // 如果没有搜索记录,隐藏清除搜索记录的功能布局
        if (keyList.size() == 0) {
            ll_clean_key.setVisibility(View.GONE);
        }
    }

    private class SearchAdapter extends
            RecyclerView.Adapter<SearchAdapter.ViewHolder> {

        // 点击事件监听器接口
        private OnBaseClickListener mOnItemClickListener = null;

        // 对外提供的设置监听器方法
        public void setOnItemClickListener(
                OnBaseClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_search_key;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_search_key = itemView.findViewById(R.id.tv_search_key);
            }

        }

        @Override
        public int getItemCount() {
            return keyList.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            String key = keyList.get(position);
            holder.tv_search_key.setText(key);
            holder.itemView.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, position);
                }
            });
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.history_search_item, parent, false);
            return new ViewHolder(view);
        }
    }
}
