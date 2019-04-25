package com.toly1994.tolymusic.app.utils;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.ChooseSongListBaseAdapter;
import com.toly1994.tolymusic.adapter.ChooseTimingBaseAdapter;
import com.toly1994.tolymusic.app.domain.SongList;
import com.toly1994.tolymusic.itf.OnBaseClickListener;

import java.util.ArrayList;
import java.util.List;

public class DialogUtils {
	/**
	 * 显示歌单列表
	 * @param context
	 * @param songList 歌单列表内容
	 * @param itemClickListener 列表视图点击监听
	 * @return
	 */
	public static PopupWindow showSongListPopupWindows(final Activity context,
			final List<SongList> songList,
			final OnBaseClickListener itemClickListener) {
		// 创建界面
		View conentView = View.inflate(context, R.layout.dialog_songlist, null);
		ListView lv_dialog_songlist = (ListView) conentView
				.findViewById(R.id.lv_dialog_songlist);
		// 判断播放列表是否没有内容
		if (songList != null && songList.size() == 0) {
			TextView tv_dont_have_songlist = (TextView) conentView
					.findViewById(R.id.tv_dont_have_songlist);
			tv_dont_have_songlist.setVisibility(View.VISIBLE);
		}
		final ChooseSongListBaseAdapter songListAdaoter = new ChooseSongListBaseAdapter(
				context, songList);
		lv_dialog_songlist.setAdapter(songListAdaoter);
		// 显示界面
		final PopupWindow popupWindow = new PopupWindow(conentView,
				DensityUtils.dp2px(context, 300), DensityUtils.dp2px(context,
						400));
		ColorDrawable dw = new ColorDrawable(context.getResources().getColor(
				R.color.main_gray_background));
		popupWindow.setBackgroundDrawable(dw);
		popupWindow.showAtLocation(conentView, Gravity.CENTER, 0, 0);
		backgroundAlpha(context, 0.6f);
		popupWindow.setOutsideTouchable(true);
		popupWindow.setFocusable(false);
		popupWindow.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				backgroundAlpha(context, 1f);
			}
		});
		popupWindow.update();

		lv_dialog_songlist.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				itemClickListener.onClick(view, position);
				popupWindow.dismiss();
			}
		});
		return popupWindow;
	}

	/**
	 * 显示按时停止播放的时间值
	 */
	public static PopupWindow showTiming(final Activity context,
			final OnBaseClickListener itemClickListener, int checkPostion) {
		@SuppressWarnings("serial")
		List<String> timingList = new ArrayList<String>(7) {
			{
				add("未开启");
				add("10分钟后");
				add("20分钟后");
				add("30分钟后");
				add("45分钟后");
				add("60分钟后");
				add("90分钟后");
			}
		};
		View conentView = View.inflate(context, R.layout.dialog_timing, null);
		// 显示界面
		final PopupWindow popupWindow = new PopupWindow(conentView,
				DensityUtils.dp2px(context, 300), DensityUtils.dp2px(context,
						400));
		ListView lv_dialog_timing = (ListView) conentView
				.findViewById(R.id.lv_dialog_timing);
		final ChooseTimingBaseAdapter timingAdaoter = new ChooseTimingBaseAdapter(
				context, timingList, checkPostion);
		lv_dialog_timing.setAdapter(timingAdaoter);
		ColorDrawable dw = new ColorDrawable(context.getResources().getColor(
				R.color.main_gray_background));
		popupWindow.setBackgroundDrawable(dw);
		popupWindow.showAtLocation(conentView, Gravity.CENTER, 0, 0);
		backgroundAlpha(context, 0.6f);
		popupWindow.setOutsideTouchable(true);
		popupWindow.setFocusable(false);
		popupWindow.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				backgroundAlpha(context, 1f);
			}
		});
		popupWindow.update();
		// 点击监听
		lv_dialog_timing.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				itemClickListener.onClick(view, position);
				timingAdaoter.setCheckItem(position);
				popupWindow.dismiss();
			}
		});
		return popupWindow;
	}

	/**
	 * 设置添加屏幕的背景透明度
	 * 
	 * @param bgAlpha
	 */
	public static void backgroundAlpha(Activity context, float bgAlpha) {
		WindowManager.LayoutParams lp = context.getWindow().getAttributes();
		lp.alpha = bgAlpha; // 0.0-1.0
		context.getWindow()
				.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		context.getWindow().setAttributes(lp);
	}
}
