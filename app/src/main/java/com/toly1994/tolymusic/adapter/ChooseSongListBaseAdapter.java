package com.toly1994.tolymusic.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.domain.SongList;

import java.util.List;


/**
 * 选择歌单的list view内容适配器
 * @author 1bRoNG
 */
public class ChooseSongListBaseAdapter extends BaseAdapter {
	private Context context;
	private List<SongList> songList;

	public ChooseSongListBaseAdapter(Context context, List<SongList> songList) {
		this.context = context;
		this.songList = songList;
	}

	@Override
	public int getCount() {
		return songList.size();
	}

	@Override
	public Object getItem(int position) {
		return songList.get(position);
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
			convertView = View.inflate(context, R.layout.album_home_item, null);
			holder.tv_music_name = (TextView) convertView.findViewById(R.id.tv_music_name);
			holder.tv_music_time = (TextView) convertView.findViewById(R.id.tv_music_time);
			holder.iv_more = (ImageView) convertView.findViewById(R.id.iv_more);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		SongList item = songList.get(position);
		holder.iv_more.setVisibility(View.GONE);
		holder.tv_music_name.setText(item.getListName());
		holder.tv_music_time.setText(item.getSongs().size()+"首");
		return convertView;
	}

	public final class ViewHolder {
		TextView tv_music_name, tv_music_time;
		ImageView iv_more;
	}
}
