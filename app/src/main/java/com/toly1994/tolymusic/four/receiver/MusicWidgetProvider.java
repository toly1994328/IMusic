package com.toly1994.tolymusic.four.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.RemoteViews;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.app.utils.ImageUtils;

public class MusicWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "MusicWidgetProvider";
    private Song playingSong;
    public static String WIDGET_LAST_ACTION = "com.toly1994.tolymusic.WIDGET_LAST_ACTION";
    public static String WIDGET_NEXT_ACTION = "com.toly1994.tolymusic.WIDGET_NEXT_ACTION";
    public static String WIDGET_PLAYORPASUSE_ACTION = "com.toly1994.tolymusic.WIDGET_PLAYORPASUSE_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        playingSong = PlayingService.playingSong;

        Log.e(TAG, "onReceive: " + intent.getAction());

        if (playingSong != null) {
            if (intent.getAction().equals(WIDGET_LAST_ACTION)) {
                // 点击widget上一首
                Intent send = new Intent(context, PlayingService.class);
                send.putExtra("action", PlayingService.INTENT_LAST_MUSIC);
                context.startService(send);
            } else if (intent.getAction().equals(WIDGET_PLAYORPASUSE_ACTION)) {
                // 点击widget播放
                Intent send = new Intent(context, PlayingService.class);
                if (PlayingService.mediaPlayer.isPlaying()) {
                    send.putExtra("action", PlayingService.INTENT_PAUSE_MUSIC);
                } else {
                    send.putExtra("action", PlayingService.INTENT_PLAYLIST_START_MUSIC);
                    send.putExtra("playing_song", playingSong);
                }
                context.startService(send);
            } else if (intent.getAction().equals(WIDGET_NEXT_ACTION)) {
                // 点击widget下一首
                Intent send = new Intent(context, PlayingService.class);
                send.putExtra("action", PlayingService.INTENT_NEXT_MUSIC);
                context.startService(send);
            }
        }

        // 接收到服务传来的intent就更新widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, MusicWidgetProvider.class));
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.e(TAG, "onUpdate: ");

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgeManger, int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_small_style);

        ComponentName component = new ComponentName(
                "com.toly1994.tolymusic",//项目包名
                "com.toly1994.tolymusic.receiver.MusicWidgetProvider"//广播接收者全类名
        );

        //上一曲
        Intent intentPrev = new Intent(WIDGET_LAST_ACTION);
        intentPrev.setComponent(component);
        views.setOnClickPendingIntent(R.id.iv_last_music,
                PendingIntent.getBroadcast(context, 0, intentPrev, 0));

        //暂停/开始
        Intent intentCtrl = new Intent(WIDGET_PLAYORPASUSE_ACTION);
        intentCtrl.setComponent(component);
        views.setOnClickPendingIntent(R.id.iv_pause_music,
                PendingIntent.getBroadcast(context, 0, intentCtrl, 0));

        //下一曲
        Intent intentNext = new Intent(WIDGET_NEXT_ACTION);
        intentNext.setComponent(component);
        views.setOnClickPendingIntent(R.id.iv_next_music,
                PendingIntent.getBroadcast(context, 0, intentNext, 0));

        //跳到App
        Intent intentStartApp = new Intent(StartAppReceiver.START_APP);
        ComponentName componentApp = new ComponentName(
                "com.toly1994.tolymusic",//项目包名
                "com.toly1994.tolymusic.receiver.StartAppReceiver"//广播接收者全类名
        );
        intentStartApp.setComponent(componentApp);
        PendingIntent startAppIntent =
                PendingIntent.getBroadcast(context, 0, intentStartApp, 0);

        views.setOnClickPendingIntent(R.id.iv_cover, startAppIntent);
        views.setOnClickPendingIntent(R.id.tv_song_name, startAppIntent);
        views.setOnClickPendingIntent(R.id.tv_artist_name, startAppIntent);


        // 设置正在播放的歌曲信息
        playingSong = PlayingService.playingSong;


        if (playingSong != null) {
            Bitmap cover = playingSong.getAlbum().getCover();
            if (cover == null) {
                cover = ImageUtils.getArtwork(context, playingSong.getTitle(),
                        playingSong.getSongId(), playingSong.getAlbum().getAlbumId(), true);
            }
            views.setImageViewBitmap(R.id.iv_cover, cover);
            views.setTextViewText(R.id.tv_song_name, playingSong.getTitle());
            views.setTextViewText(R.id.tv_artist_name, playingSong.getAlbum().getArtist().getSingerName());
            // 设置按钮状态
            int drawableID = (PlayingService.mediaPlayer != null && PlayingService.mediaPlayer.isPlaying()) ?
                    R.drawable.pause_music : R.drawable.play_music;
            views.setImageViewResource(R.id.iv_pause_music, drawableID);
        } else {
            views.setImageViewResource(R.id.iv_cover, R.drawable.default_music_icon);
            views.setTextViewText(R.id.tv_song_name, "..");
            views.setTextViewText(R.id.tv_artist_name, "..");
            views.setImageViewResource(R.id.iv_pause_music, R.drawable.play_music);
        }
        appWidgeManger.updateAppWidget(appWidgetId, views);
    }

    /**
     * 这个是刚开始添加的挂件的时候触发方法
     *
     * @param context
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.e(TAG, "onEnabled: ");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.e(TAG, "onDeleted: ");

    }
}
