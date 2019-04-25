package com.toly1994.tolymusic.four.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.toly1994.tolymusic.four.service.PlayingService;


/**
 * 定时停止播放
 * @author lbRoNG
 */
public class TimingStopMusicReceiver extends BroadcastReceiver {
	public static final String ACTION_MEDIA_BUTTON = "com.toly1994.tolymusic.ACTION_ALARM_STOP_MUSIC";
	@Override
	public void onReceive(Context context, Intent intent) {
		if(ACTION_MEDIA_BUTTON.equals(intent.getAction())){
			// 发送暂停广播给服务
			Intent stopIntent = new Intent(context,PlayingService.class);
			stopIntent.putExtra("action", PlayingService.INTENT_PAUSE_MUSIC);
			context.startService(stopIntent);
		}
	}

}
