package com.toly1994.tolymusic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.four.activity.SearchActivity;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;
import com.toly1994.tolymusic.itf.impl.MoreMenuImpl;

import java.util.ArrayList;
import java.util.List;

public class MusicItem extends RelativeLayout{
	private Context context;
	private TextView tv_music_name, tv_music_albums;
	private View view_line;
	private ImageView iv_music_icon, iv_more;

	public MusicItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		initView();

		TypedArray array = context.obtainStyledAttributes(attrs,
				R.styleable.MusicItem);
		String musicName = array.getString(R.styleable.MusicItem_musicName);
		String musicAlbums = array.getString(R.styleable.MusicItem_musicAlbums);
		boolean showLine = array.getBoolean(R.styleable.MusicItem_showLine,
				false);
		tv_music_name.setText(musicName);
		tv_music_albums.setText(musicAlbums);
		if (!showLine) {
			view_line.setVisibility(View.INVISIBLE);
		}
		array.recycle();
	}

	private void initView() {
		View.inflate(context, R.layout.music_item, this);
		tv_music_name = (TextView) findViewById(R.id.tv_music_name);
		tv_music_albums = (TextView) findViewById(R.id.tv_music_albums);
		view_line = (View) findViewById(R.id.view_line);
		iv_music_icon = (ImageView) findViewById(R.id.iv_music_icon);
		iv_more = (ImageView) findViewById(R.id.iv_more);
	}

	/**
	 * 设置更多按钮菜单
	 * @param fromViewposition 在列表中的位置
	 */
	public void setOnMoreMenuClickListener(final List<?> data,
			List<Integer> menus, final int fromViewposition) {
		// 获取提供菜单选项的内容适配器
		MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(context, menus);
		// 把内容给点击事件的实现类,完成点击跳出对应菜单
		final BaseMoreMenuClickListenerImpl impl = new BaseMoreMenuClickListenerImpl(context, mmAdapter);
		// 给菜单内的选项添加点击事件
		impl.setOnItemClickListener((fromView, clickView, fromViewPositon, clickViewPosition) -> {
			Object type = data.get(0);
			if(type instanceof Song){
				MoreMenuImpl.MoreMenuAsSong(context, (ArrayList<Song>)data,
						null, fromViewPositon, clickViewPosition);
			}else if(type instanceof Album){
				MoreMenuImpl.MoreMenuAsAlbum(context, (ArrayList<Album>)data,
						null, fromViewPositon, clickViewPosition);
			}else{
				MoreMenuImpl.MoreMenuAsArtist(context, (ArrayList<Artist>)data,
						null, fromViewPositon, clickViewPosition);
			}
			// 隐藏菜单
			impl.popupWindowDismiss();
		});
		
		// 设置菜单按钮的点击事件
		iv_more.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchActivity.impl = impl;
				impl.onClick(v, fromViewposition);
			}
		});
	}

	public void setText(String text) {
		tv_music_name.setText(text);
	}

	public void setText(SpannableStringBuilder text) {
		tv_music_name.setText(text);
	}

	public void setDesc(String desc) {
		tv_music_albums.setText(desc);
	}

	public void setDesc(SpannableStringBuilder desc) {
		tv_music_albums.setText(desc);
	}

	public void setImage(Bitmap bm) {
		iv_music_icon.setImageBitmap(bm);
	}
}
