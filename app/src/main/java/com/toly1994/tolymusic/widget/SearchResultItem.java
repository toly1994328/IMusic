package com.toly1994.tolymusic.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.app.utils.MTextUtils;

import java.util.List;

public class SearchResultItem extends RelativeLayout {
	private TextView tv_title, tv_more_title;
	private MusicItem mi_one, mi_two, mi_three;
	private LinearLayout ll_more;

	public SearchResultItem(Context context) {
		super(context);
		initView(context);
	}

	public SearchResultItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	public void setTitle(String title) {
		tv_title.setText(title);
	}

	public void setMoreTitle(String title) {
		tv_more_title.setText(title);
	}

	public void showOrHideMore(int visibility) {
		ll_more.setVisibility(visibility);
	}

	public void setOnMoreBtnClickListener(
			OnClickListener listener) {
		ll_more.setOnClickListener(listener);
	}
	
	public void setOnSongItemClickListener(
			final OnBaseClickListener listener) {
		mi_one.setOnClickListener(v -> listener.onClick(v, 0));
		mi_two.setOnClickListener(v -> listener.onClick(v, 1));
		mi_three.setOnClickListener(v -> listener.onClick(v, 2));
	}
	
	public void setMoreMenu(List<?> data,List<Integer> menus){
		mi_one.setOnMoreMenuClickListener(data,menus,0);
		mi_two.setOnMoreMenuClickListener(data,menus,1);
		mi_three.setOnMoreMenuClickListener(data,menus,2);
	}

	/**
	 * 设置数据
	 * @param position
	 * @param title
	 * @param desc
	 * @param bm
	 */
	public void setItem(int position, String title, String desc, Bitmap bm,
			String key) {
		MusicItem temp = null;
		if (position == 1) {
			temp = mi_one;
		} else if (position == 2) {
			temp = mi_two;
		} else if (position == 3) {
			temp = mi_three;
		} else {
			throw new RuntimeException("positon传入参数错误,1or2or3");
		}
		temp.setVisibility(View.VISIBLE);
		temp.setText(MTextUtils.setTextColorByKey(title, key, getResources().getColor(R.color.titleBackground)));
		temp.setDesc(MTextUtils.setTextColorByKey(desc, key, getResources().getColor(R.color.titleBackground)));
		temp.setImage(bm);
	}

	private void initView(Context context) {
		View.inflate(context, R.layout.search_result_item, this);
		ll_more = (LinearLayout) findViewById(R.id.ll_more);
		tv_title = (TextView) findViewById(R.id.tv_title);
		tv_more_title = (TextView) findViewById(R.id.tv_more_title);
		mi_one = (MusicItem) findViewById(R.id.mi_one);
		mi_two = (MusicItem) findViewById(R.id.mi_two);
		mi_three = (MusicItem) findViewById(R.id.mi_three);
	}
}
