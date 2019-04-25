package com.toly1994.tolymusic.itf.impl;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.OnMoreMenuItemClickListener;


/**
 * 实现接口,点击更多菜单按钮弹出菜单
 * @author 1bRoNG
 */
public class BaseMoreMenuClickListenerImpl implements OnBaseClickListener {
	private View fromView;
	private int fromViewPosition;
	private View conentView;
	private Context context;
	private ListView lv_more_menu;
	private ListAdapter adapter;
	private PopupWindow popupWindow;
	private OnMoreMenuItemClickListener mOnItemClickListener = null;

	public BaseMoreMenuClickListenerImpl(Context context, ListAdapter adapter) {
		this.context = context;
		this.adapter = adapter;
		setViewCompotent();
	}
	
	/**
	 * 对外提供接口,实现对菜单选项的点击
	 * @param listener
	 */
	public void setOnItemClickListener(
			OnMoreMenuItemClickListener listener) {
		this.mOnItemClickListener = listener;
	}

	public void setViewCompotent() {
		conentView = View.inflate(context, R.layout.more_menu, null);
		lv_more_menu = (ListView) conentView.findViewById(R.id.lv_more_menu);
		lv_more_menu.setAdapter(adapter);
		lv_more_menu.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(mOnItemClickListener!=null){
					mOnItemClickListener.onClick(fromView,view,fromViewPosition, position);
				}
			}
		});
	}
	/**
	 * @param view 被点击的view
	 * @param position 被点击view在列表中的位置
	 */
	@Override
	public void onClick(View view, int position) {
		fromView = view;
		fromViewPosition = position;
		popupWindow = new PopupWindow(conentView,
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		// 设置背景,setOutsideTouchable才能生效
		ColorDrawable dw = new ColorDrawable(-00000);
		popupWindow.setBackgroundDrawable(dw);
		int[] location = new int[2];
		view.getLocationInWindow(location);
		popupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP, location[0], location[1]);
		popupWindow.setOutsideTouchable(true);
		popupWindow.setFocusable(false);
		popupWindow.update();
	}
	
	public boolean popupWindowIsShow(){
		return popupWindow!=null?popupWindow.isShowing():false;
	}
	
	public void popupWindowDismiss(){
		if(popupWindow!=null){
			popupWindow.dismiss();
		}
	}
}
