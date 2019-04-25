package com.toly1994.tolymusic.four.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 耳机按钮监听
 * 必须是静态注册的接收器才能接受到,所以借此接受者作为中间人,本地再发送一次广播到本地动态注册的广播,否则无法调用方法控制音乐
 * @author lbRoNG
 */
public class MediaControlReceiver extends BroadcastReceiver {
	public static final String ACTION_MEDIA_BUTTON = "com.toly1994.tolymusic.ACTION_MEDIA_BUTTON";
	@Override
	public void onReceive(Context context, Intent intent) {
		if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())){
			Intent send = new Intent(ACTION_MEDIA_BUTTON);
			send.putExtras(intent.getExtras());
			context.sendBroadcast(send);                                                              
		}
	}

}
