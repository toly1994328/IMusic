package com.toly1994.tolymusic.app.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.four.receiver.MusicWidgetProvider;
import com.toly1994.tolymusic.four.receiver.StartAppReceiver;

import java.util.List;


public class SystemUtils {
    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param mContext
     * @param serviceName 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public static boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    /**
     * 判断是否双击
     */
    private static long[] mHits = new long[2];

    public static void finishAsDouble(Activity nowUI) {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
            nowUI.finish();
        } else {
            Toast.makeText(nowUI, "再按一次退出LMusic", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新widget到默认状态
     *
     * @param context
     */
    public static void updateWidgetToDefault(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MusicWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_small_style);
            PendingIntent startAppIntent = PendingIntent
                    .getBroadcast(context, 0, new Intent(StartAppReceiver.START_APP), 0);
            rv.setOnClickPendingIntent(R.id.iv_cover, startAppIntent);
            rv.setOnClickPendingIntent(R.id.tv_song_name, startAppIntent);
            rv.setOnClickPendingIntent(R.id.tv_artist_name, startAppIntent);
            rv.setImageViewResource(R.id.iv_cover, R.drawable.default_music_icon);
            rv.setTextViewText(R.id.tv_song_name, "..");
            rv.setTextViewText(R.id.tv_artist_name, "..");
            rv.setImageViewResource(R.id.iv_pause_music, R.drawable.play_music);
            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
    }
}
