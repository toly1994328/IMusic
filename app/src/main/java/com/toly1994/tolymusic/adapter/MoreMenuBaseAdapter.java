package com.toly1994.tolymusic.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.toly1994.tolymusic.R;

import java.util.List;


/**
 * 更多按钮菜单的list view内容适配器
 * @author 1bRoNG
 */
public class MoreMenuBaseAdapter extends BaseAdapter {
	private Context context;
	private List<Integer> moreMenus;

	public MoreMenuBaseAdapter(Context context, List<Integer> moreMenus) {
		this.context = context;
		this.moreMenus = moreMenus;
	}

	@Override
	public int getCount() {
		return moreMenus.size();
	}

	@Override
	public Object getItem(int position) {
		return moreMenus.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = View.inflate(context, R.layout.more_menu_item, null);
			holder.tv_content = (TextView) convertView
					.findViewById(R.id.tv_content);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		String str = context.getResources().getString(moreMenus.get(position));
		holder.tv_content.setText(str);
		return convertView;
	}

	public final class ViewHolder {
		TextView tv_content;
	}
}
