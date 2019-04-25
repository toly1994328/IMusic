package com.toly1994.tolymusic.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.toly1994.tolymusic.R;

import java.util.List;


/**
 * 选择歌单的list view内容适配器
 * 
 * @author 1bRoNG
 */
public class ChooseTimingBaseAdapter extends BaseAdapter {
	private Context context;
	private int checkPostion = -1;
	private List<String> timingList;

	public void setCheckItem(int checkPostion) {
		this.checkPostion = checkPostion;
		notifyDataSetChanged();
	}

	public ChooseTimingBaseAdapter(Context context, List<String> timingList,
			int checkPostion) {
		this.timingList = timingList;
		this.context = context;
		this.checkPostion = checkPostion;
	}

	@Override
	public int getCount() {
		return timingList.size();
	}

	@Override
	public Object getItem(int position) {
		return timingList.get(position);
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
			convertView = View.inflate(context, R.layout.timing_check_item,
					null);
			holder.tv_timing = (TextView) convertView
					.findViewById(R.id.tv_timing);
			holder.ck_choose = (CheckBox) convertView
					.findViewById(R.id.ck_choose);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		String str = timingList.get(position);
		holder.tv_timing.setText(str);
		if (checkPostion == position) {
			holder.ck_choose.setChecked(true);
		}
		return convertView;
	}

	public final class ViewHolder {
		TextView tv_timing;
		CheckBox ck_choose;
	}
}
