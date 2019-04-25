package com.toly1994.tolymusic.four.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.graphics.Palette;
import android.view.KeyEvent;
import android.widget.Toast;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.four.activity.HomeActivity;
import com.toly1994.tolymusic.four.activity.PlayMusicActivity;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.four.receiver.MediaControlReceiver;
import com.toly1994.tolymusic.four.receiver.StartAppReceiver;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.SystemUtils;

import java.util.*;

@SuppressWarnings("deprecation")
public class PlayingService extends Service {
    // 携带播放列表的播放请求
    public static final String INTENT_START_MUSIC = "INTENT_START_MUSIC";
    // 播放列表内的歌曲播放的请求,不携带播放列表
    public static final String INTENT_PLAYLIST_START_MUSIC = "INTENT_PLAYLIST_START_MUSIC";
    // 播放列表内切换歌曲,但是没有进行播放
    public static final String INTENT_PLAYLIST_CHANGE_MUSIC = "INTENT_PLAYLIST_CHANGE_MUSIC";
    // 暂停播放
    public static final String INTENT_PAUSE_MUSIC = "INTENT_PAUSE_MUSIC";
    // 停止播放
    public static final String INTENT_STOP_MUSIC = "INTENT_STOP_MUSIC";
    // 上一首
    public static final String INTENT_LAST_MUSIC = "INTENT_LAST_MUSIC";
    // 顺序播放的下一首
    public static final String INTENT_NEXT_MUSIC = "INTENT_NEXT_MUSIC";
    // 随机播放的下一首
    public static final String INTENT_NEXT_RANDOM_MUSIC = "INTENT_NEXT_RANDOM_MUSIC";
    // 列表循环的下一首
    public static final String INTENT_NEXT_LOOP_MUSIC = "INTENT_NEXT_LOOP_MUSIC";
    // 清除播放列表
    public static final String CLEAN_PLAYLSIT = "CLEAN_PLAYLSIT";
    // 服务执行相关请求完成发出的通知
    public static final String OPERATE_FINISH = "com.toly1994.tolymusic.service.OPERATE_FINISH";
    // notification action 标识
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_LAST = "ACTION_LAST";
    public static final String ACTION_STOP = "ACTION_STOP";
    private ActivityMediaControlReceiver mediaControlReceiver;
    private NoisyAudioStreamReceiver nosiyReceiver;
    private MusicDeleteReceiver deleteReceiver;
    private AudioManager am;
    private long oldSystemTime;
    private Timer timer;
    private MediaSession mSession;
    private MediaController mController;
    private SharedPreferences playConfig;
    private boolean isFousLossTransient, isFousLossForever; // 记录焦点是否丢失
    public static List<Song> playList = new ArrayList<>();
    public static List<Song> playListOfRandom = new ArrayList<>();
    public static Song playingSong;
    public static MediaPlayer mediaPlayer;
    public static int pauseProgress; // 当前歌曲的进度

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mSession.release();
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 获取播放信息配置文件
        playConfig = getSharedPreferences("playconfig", Context.MODE_PRIVATE);
        // 注册媒体按键事件Receiver来监听媒体按钮按下动作
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ComponentName component = new ComponentName(this, MediaControlReceiver.class);
        am.registerMediaButtonEventReceiver(component);
        // 请求焦点,音乐流,永久获取(只有播放一小段才暂时获取,那么播放完成后就要放弃焦点 abandonAudioFocus)
        am.requestAudioFocus(new OnAudioFocusChangeListenerImpl(),
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        // 注册广播接受按钮事件
        mediaControlReceiver = new ActivityMediaControlReceiver();
        IntentFilter filter = new IntentFilter(MediaControlReceiver.ACTION_MEDIA_BUTTON);
        registerReceiver(mediaControlReceiver, filter);
        // 注册扬声器变化的接受者
        nosiyReceiver = new NoisyAudioStreamReceiver();
        IntentFilter filter2 = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(nosiyReceiver, filter2);

        // 注册广播
        if (deleteReceiver == null) {
            deleteReceiver = new MusicDeleteReceiver();
            IntentFilter filter3 = new IntentFilter(HomeActivity.INTENT_MUSIC_DELETE);
            registerReceiver(deleteReceiver, filter3);
        }
        // 初始化notification
        initMediaSessions();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            Intent finishIntent = new Intent(OPERATE_FINISH);
            if (INTENT_START_MUSIC.equals(action)) {
                // 接收数据
                receiveData(intent);
                // 清空进度
                pauseProgress = 0;
                // 播放音乐
                startRandomMusic(playingSong.getUrl());
                finishIntent.putExtra("intent_type", INTENT_START_MUSIC);
                sendBroadcast(finishIntent);
                // 设置notification类型
                intent.setAction(ACTION_PLAY);
                handleIntent(intent);
            } else if (INTENT_PLAYLIST_START_MUSIC.equals(action)) {
                // 记录收到intent的时间
                oldSystemTime = System.currentTimeMillis();
                // 延迟规定时间后再看oldSystemTime有没有变化,没有就代表用户不在频繁发intent切歌
                // 如果有旧的延迟任务,撤销
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                // 开启新的延迟任务
                judgeIsQuickChange(intent);
            } else if (INTENT_PLAYLIST_CHANGE_MUSIC.equals(action)) {
                // 设置notification类型
                intent.setAction(ACTION_PAUSE);
                handleIntent(intent);
            } else if (INTENT_PAUSE_MUSIC.equals(action)) {
                pauseMusic();
                finishIntent.putExtra("intent_type", INTENT_PAUSE_MUSIC);
                sendBroadcast(finishIntent);
                // 设置notification类型
                intent.setAction(ACTION_PAUSE);
                handleIntent(intent);
            } else if (INTENT_STOP_MUSIC.equals(action)) {
                stopMusic();
                finishIntent.putExtra("intent_type", INTENT_STOP_MUSIC);
                sendBroadcast(finishIntent);
                // 清空当前播放的音乐
                playingSong = null;
                // 设置notification类型
                intent.setAction(ACTION_STOP);
                handleIntent(intent);
            } else if (INTENT_LAST_MUSIC.equals(action)) {
                playLastMusicAsPlayMethod();
            } else if (INTENT_NEXT_MUSIC.equals(action)) {
                playNextMusicAsPlayMethod(false);
            } else if (CLEAN_PLAYLSIT.equals(action)) {
                cleanPlayList();
                finishIntent.putExtra("intent_type", CLEAN_PLAYLSIT);
                sendBroadcast(finishIntent);
                // 设置notification类型
                intent.setAction(ACTION_PAUSE);
                handleIntent(intent);
            } else {
                handleIntent(intent);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // 移除notification
        cancelNotification();
        // 更新widget
        SystemUtils.updateWidgetToDefault(this);
        // 关闭服务
        stopSelf();

    }

    @Override
    public void onDestroy() {
        mSession.release();

        if (mediaControlReceiver != null) {
            unregisterReceiver(mediaControlReceiver);
            mediaControlReceiver = null;
        }

        if (nosiyReceiver != null) {
            unregisterReceiver(nosiyReceiver);
            nosiyReceiver = null;
        }

        if (deleteReceiver != null) {
            unregisterReceiver(deleteReceiver);
            deleteReceiver = null;
        }
        super.onDestroy();
    }

    /**
     * 解析notification的action行为
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_LAST)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    /**
     * 初始化notification
     */
    private void initMediaSessions() {
        mSession = new MediaSession(getApplicationContext(), "LMusic session");
        mSession.setActive(true);
        mSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        // 设置回调控制
        mController = new MediaController(getApplicationContext(), mSession.getSessionToken());
        // 定义按钮点击回调
        mSession.setCallback(new MediaSession.Callback() {
            Intent finishIntent = new Intent(OPERATE_FINISH);

            @Override
            public void onPlay() {
                super.onPlay();
                if (!mediaPlayer.isPlaying()) {
                    startRandomMusic(playingSong.getUrl());
                    finishIntent.putExtra("intent_type", INTENT_PLAYLIST_START_MUSIC);
                    finishIntent.putExtra("is_notification_send", true);
                    sendBroadcast(finishIntent);
                }
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "暂停", ACTION_PAUSE));
            }

            @Override
            public void onPause() {
                super.onPause();
                if (mediaPlayer.isPlaying()) {
                    pauseMusic();
                    finishIntent.putExtra("intent_type", INTENT_PAUSE_MUSIC);
                    sendBroadcast(finishIntent);
                }
                buildNotification(generateAction(android.R.drawable.ic_media_play, "播放", ACTION_PLAY));
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                int nowPlayMethodId = playConfig.getInt("play_method_id", 0);
                switch (nowPlayMethodId) {
                    case PlayMusicActivity.PLAY_METHOD_ORDER:
                        nextMusic();
                        break;
                    case PlayMusicActivity.PLAY_METHOD_LOOP:
                        nextMusicForLoop();
                        break;
                    case PlayMusicActivity.PLAY_METHOD_SIGLE_LOOP:
                        nextMusicForLoop();
                        break;
                    case PlayMusicActivity.PLAY_METHOD_RANDOM:
                        nextMusicForRandom();
                        break;
                }
                finishIntent.putExtra("intent_type", INTENT_NEXT_MUSIC);
                sendBroadcast(finishIntent);
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "暂停", ACTION_PAUSE));

            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                int nowPlayMethodId = playConfig.getInt("play_method_id", 0);
                if (nowPlayMethodId == PlayMusicActivity.PLAY_METHOD_RANDOM) {
                    lastMusic(true);
                } else {
                    lastMusic(false);
                }
                finishIntent.putExtra("intent_type", INTENT_LAST_MUSIC);
                sendBroadcast(finishIntent);
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "暂停", ACTION_PAUSE));
            }

            @Override
            public void onStop() {
                super.onStop();
                pauseMusic();
                finishIntent.putExtra("intent_type", INTENT_PAUSE_MUSIC);
                sendBroadcast(finishIntent);
                cancelNotification();
            }
        });
    }

    /**
     * 创建Notification
     *
     * @param action
     */
    private void buildNotification(Notification.Action action) {
        // 获取封面
        Bitmap cover = playingSong.getAlbum().getCover();
        if (cover == null) {
            cover = ImageUtils.getArtwork(getApplicationContext(),
                    playingSong.getTitle(), playingSong.getSongId(),
                    playingSong.getAlbum().getAlbumId(), true);
            playingSong.getAlbum().setCover(cover);
        }
        // 设置锁屏显示专辑
        mSession.setMetadata(new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, playingSong.getAlbum().getCover())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, playingSong.getAlbum().getArtist().getSingerName())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, playingSong.getAlbum().getAlbumName())
                .putString(MediaMetadata.METADATA_KEY_TITLE, playingSong.getTitle()).build());
        Notification.MediaStyle style = new Notification.MediaStyle();
        // 设置点击Intent
        Intent clickIntent = new Intent(StartAppReceiver.START_APP);
        PendingIntent pendingClickIntent = PendingIntent.getBroadcast(this, 2, clickIntent, 0);
        // 设置删除Intent
        Intent deleteIntent = new Intent(this, PlayingService.class);
        deleteIntent.setAction(ACTION_STOP);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(this, 1, deleteIntent, 0);
        // 设置Notification
        Palette.Swatch vibrant = Palette.generate(playingSong.getAlbum().getCover()).getVibrantSwatch();
        int colorRgb = getResources().getColor(R.color.titleBackground);
        if (vibrant != null) {
            colorRgb = vibrant.getRgb();
            playingSong.getAlbum().setCoverRgb(colorRgb);
        }
        Notification.Builder builder = new Notification.Builder(this)
                .setLargeIcon(playingSong.getAlbum().getCover())
                .setSmallIcon(R.drawable.default_music_icon)
                .setColor(playingSong.getAlbum().getCoverRgb())
                .setContentTitle(playingSong.getTitle())
                .setContentText(playingSong.getAlbum().getArtist().getSingerName()
                        + " - " + playingSong.getAlbum().getAlbumName())
                .setContentIntent(pendingClickIntent)
                .setDeleteIntent(pendingDeleteIntent)
                .setShowWhen(false)
                .setStyle(style.setMediaSession(mSession.getSessionToken()).setShowActionsInCompactView(1, 2))
                .addAction(generateAction(android.R.drawable.ic_media_previous, "上一首", ACTION_LAST))
                .addAction(action)
                .addAction(generateAction(android.R.drawable.ic_media_next, "下一首", ACTION_NEXT));
        // 显示Notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * 设置Notification的action
     *
     * @param icon
     * @param title
     * @param intentAction
     * @return
     */
    private Notification.Action generateAction(int icon, String title,
                                               String intentAction) {
        Intent intent = new Intent(getApplicationContext(), PlayingService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent)
                .build();
    }

    private void cancelNotification() {
        // 移除notification
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
        // 保存下最后播放的歌曲和进度
        if (playingSong != null) {
            SharedPreferences playConfig = getSharedPreferences("playconfig",
                    Context.MODE_PRIVATE);
            Editor edit = playConfig.edit();
            edit.putString("last_playing_song", playingSong.getTitle());
            edit.putLong("last_playing_song_id", playingSong.getSongId());
            edit.putLong("last_playing_song_duration",
                    playingSong.getDuration());
            edit.putInt("last_playing_Progress", pauseProgress);
            edit.apply();
        }
    }

    /**
     * 获取intent携带的数据 设置当前播放的歌曲和播放列表
     *
     * @param intent
     */
    private void receiveData(Intent intent) {
        // 获取需要播放的歌曲
        playingSong = intent.getParcelableExtra("playing_song");
        // 获取当前播放歌曲所在的播放列表
        List<Song> tempList = intent
                .getParcelableArrayListExtra("playing_list");
        // 播放列表为空或者旧的播放列表和新传入的播放列表不相同,则更新
        if (playList.size() == 0 || !tempList.equals(playList)) {
            playList = tempList;
            playList = getCompleteData(playList);
            // 获取默认的随机播放的播放列表
            playListOfRandom = intent
                    .getParcelableArrayListExtra("playing_list");
            playListOfRandom = getCompleteData(playListOfRandom);
            Collections.shuffle(playListOfRandom);
        }
        // 获取完整实例
        playingSong = playList.get(playList.indexOf(playingSong));
    }

    /**
     * 打乱随机播放列表的顺序
     */
    public static List<Song> shufflePlayList() {
        Collections.shuffle(playListOfRandom);
        return playListOfRandom;
    }

    /**
     * 整理出完整的数据集合
     *
     * @param notCompleteData
     * @return
     */
    private List<Song> getCompleteData(List<Song> notCompleteData) {
        List<Song> tempList = new ArrayList<>();
        // 取出实体完整的集合
        List<Song> allSongs = HomeActivity.getSongs();
        for (Song item : notCompleteData) {
            item = allSongs.get(allSongs.indexOf(item));
            if (!tempList.contains(item)) {
                tempList.add(item);
            }
        }
        // 清理无用数据
        allSongs = null;
        return tempList;
    }

    /**
     * 处理一下用户快速滚动切歌,少于300毫秒的滚动,认为只是快速切到想切的歌,期间被切换的歌曲不做播放处理
     *
     * @param intent
     */
    private void judgeIsQuickChange(final Intent intent) {
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // 当前时候延迟了一秒,所以新的系统时间减去旧得必须大于300毫秒才能确定用户不是频繁切歌
                long newSystemTime = System.currentTimeMillis();
                if (newSystemTime - oldSystemTime >= 300) {
                    Intent finishIntent = new Intent(OPERATE_FINISH);
                    playingSong = playList.get(playList.indexOf(intent.getParcelableExtra("playing_song")));
                    startRandomMusic(playingSong.getUrl());
                    finishIntent.putExtra("intent_type", INTENT_PLAYLIST_START_MUSIC);
                    sendBroadcast(finishIntent);
                    // 设置notification类型
                    intent.setAction(ACTION_PLAY);
                    handleIntent(intent);
                    timer.cancel();
                    timer = null;
                }
            }
        };
        timer.schedule(task, 300);
    }

    /**
     * 根据播放方式播放下一曲歌曲
     *
     * @param isAuto 是否歌曲播放完成后的自动切换
     */
    private void playNextMusicAsPlayMethod(boolean isAuto) {
        // 获取播放信息配置文件
        Intent finishIntent = new Intent(OPERATE_FINISH);
        int nowPlayMethodId = playConfig.getInt("play_method_id", 0);
        switch (nowPlayMethodId) {
            case PlayMusicActivity.PLAY_METHOD_ORDER:
                if (nextMusic()) {
                    finishIntent.putExtra("intent_type", INTENT_NEXT_MUSIC);
                } else {
                    if (isAuto) {
                        pauseMusic();
                        finishIntent.putExtra("intent_type", INTENT_PAUSE_MUSIC);
                        sendBroadcast(finishIntent);
                        buildNotification(generateAction(android.R.drawable.ic_media_pause, "播放", ACTION_PAUSE));
                    } else {
                        Toast.makeText(getApplicationContext(), "已播放到最后一首", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                break;
            case PlayMusicActivity.PLAY_METHOD_LOOP:
                nextMusicForLoop();
                break;
            case PlayMusicActivity.PLAY_METHOD_SIGLE_LOOP:
                if (isAuto) {
                    startRandomMusic(playingSong.getUrl());
                    finishIntent.putExtra("intent_type", INTENT_PLAYLIST_START_MUSIC);
                    sendBroadcast(finishIntent);
                    buildNotification(generateAction(android.R.drawable.ic_media_pause, "暂停", ACTION_PAUSE));
                    return;
                } else {
                    nextMusicForLoop();
                }
                break;
            case PlayMusicActivity.PLAY_METHOD_RANDOM:
                nextMusicForRandom();
                break;
        }
        finishIntent.putExtra("intent_type", INTENT_NEXT_MUSIC);
        sendBroadcast(finishIntent);
        buildNotification(generateAction(android.R.drawable.ic_media_pause, "暂停", ACTION_PAUSE));
    }

    /**
     * 根据播放方式播放上一曲歌曲
     */
    private void playLastMusicAsPlayMethod() {
        // 获取播放信息配置文件
        Intent finishIntent = new Intent(OPERATE_FINISH);
        int nowPlayMethodId = playConfig.getInt("play_method_id", 0);
        if (nowPlayMethodId == PlayMusicActivity.PLAY_METHOD_RANDOM) {
            lastMusic(true);
        } else {
            lastMusic(false);
        }
        finishIntent.putExtra("intent_type", INTENT_LAST_MUSIC);
        sendBroadcast(finishIntent);
        buildNotification(generateAction(android.R.drawable.ic_media_pause, "暂停", ACTION_PAUSE));
    }


    /**
     * 任意一首歌重新播放
     *
     * @param path
     */
    private void startRandomMusic(String path) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            pauseProgress = 0;
        }
        startMusic(path);
    }

    /**
     * 根据歌曲url播放歌曲,并添加播放完毕的监听,播放完毕发送广播
     *
     * @param path
     */
    private void startMusic(String path) {
        try {
            if (isFousLossForever) {
                // 请求焦点,音乐流,永久获取
                am.requestAudioFocus(new OnAudioFocusChangeListenerImpl(),
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                isFousLossForever = false;
            }
            mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(path));
            mediaPlayer.start();
            if (pauseProgress != 0) {
                mediaPlayer.seekTo(pauseProgress);
            }
            // 设置播放完毕的监听
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNextMusicAsPlayMethod(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清空播放列表
     */
    private void cleanPlayList() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            // 暂停当前歌曲
            mediaPlayer.pause();
        }
        // 移除播放列表
        playList.clear();
        playListOfRandom.clear();
        playingSong = null;
        // 进度清空
        pauseProgress = 0;
    }

    /**
     * 暂停播放
     */
    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pauseProgress = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    /**
     * 停止播放
     */
    private void stopMusic() {
        if (mediaPlayer != null) {
            pauseProgress = 0;
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 按播放列表顺序的下一首切换,切换到最后一首停止播放
     */
    private boolean nextMusic() {
        if (mediaPlayer != null && playList != null) {
            int index = playList.indexOf(playingSong);
            pauseProgress = 0;
            if (index != playList.size() - 1) {
                playingSong = playList.get(index + 1);
                mediaPlayer.stop();
                startMusic(playingSong.getUrl());
            } else {
                return false;
            }
        }
        return true;
    }


    /**
     * 在播放列表随机切换下一首
     */
    private void nextMusicForRandom() {
        if (mediaPlayer != null && playList != null) {
            // 停止播放重置进度
            mediaPlayer.stop();
            pauseProgress = 0;
            // 找出当前播放的歌曲在随机列表的哪个位置
            int index = playListOfRandom.indexOf(playingSong);
            index = (index == playListOfRandom.size() - 1) ? 0 : index + 1;
            // 获取新的要播放的歌曲
            playingSong = playListOfRandom.get(index);
            startMusic(playingSong.getUrl());
        }
    }


    /**
     * 按播放列表顺序的下一首切换,切换到最后一首循环到第一首
     */
    private void nextMusicForLoop() {
        if (mediaPlayer != null && playList != null) {
            // 停止播放重置进度
            mediaPlayer.stop();
            pauseProgress = 0;
            // 寻找下一首歌曲的逻辑
            int index = playList.indexOf(playingSong);
            index = (index == playList.size() - 1) ? 0 : index + 1;
            playingSong = playList.get(index);
            startMusic(playingSong.getUrl());
        }
    }


    /**
     * 上一首的操作只有是否随机,到第一首后的上一首操作会切换到最后一首
     */
    private void lastMusic(boolean isRandom) {
        if (mediaPlayer != null && playList != null) {
            // 停止播放重置进度
            pauseProgress = 0;
            mediaPlayer.stop();
            // 寻找上一首歌曲的逻辑
            int index = 0;
            List<Song> temp = (isRandom) ? playListOfRandom : playList;
            index = temp.indexOf(playingSong);
            index = (index == 0) ? temp.size() - 1 : index - 1;
            playingSong = temp.get(index);
            startMusic(playingSong.getUrl());
            temp = null;
        }
    }


    /**
     * 歌曲删除的广播接收
     *
     * @author lbRoNG
     */
    private class MusicDeleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<Song> deleteList = intent
                    .getParcelableArrayListExtra("deletelist");
            if (deleteList != null) {
                playList.removeAll(deleteList);
                playListOfRandom.removeAll(deleteList);
            }
        }
    }

    /**
     * 焦点改变监听
     *
     * @author lbRoNG
     */
    private class OnAudioFocusChangeListenerImpl implements
            OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Intent finishIntent = new Intent(OPERATE_FINISH);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // 你的焦点会短暂失去，但是你可以与新的使用者共同使用音频焦点
                    // 降低音量
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, 30, 0);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // 暂停播放
                    // 你会短暂的失去音频焦点，你可以暂停音乐，但不要释放资源，因为你一会就可以夺回焦点并继续使用
                    if (mediaPlayer.isPlaying()) {
                        isFousLossTransient = true;
                        // 暂停音乐
                        pauseMusic();
                        // 发送暂停音乐的广播
                        finishIntent.putExtra("intent_type", INTENT_PAUSE_MUSIC);
                        sendBroadcast(finishIntent);
                        // 更新notification
                        handleIntent(new Intent(ACTION_PAUSE));

                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mediaPlayer.isPlaying()) {
                        isFousLossForever = true;
                        // 暂停音乐
                        pauseMusic();
                        // 发送暂停音乐的广播
                        finishIntent.putExtra("intent_type", INTENT_PAUSE_MUSIC);
                        sendBroadcast(finishIntent);
                        // 更新notification
                        handleIntent(new Intent(ACTION_PAUSE));
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    // 恢复播放
                    if (isFousLossTransient && playingSong != null) {
                        // 设置音量
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, 60, 0);
                        startRandomMusic(playingSong.getUrl());
                        finishIntent.putExtra("intent_type", INTENT_START_MUSIC);
                        sendBroadcast(finishIntent);
                        isFousLossTransient = false;
                        // 更新notification
                        handleIntent(new Intent(ACTION_PLAY));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 切换扬声器的监听
     *
     * @author lbRoNG
     */
    private class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent
                    .getAction())) {
                // 暂停播放
                pauseMusic();
                Intent finishIntent = new Intent(OPERATE_FINISH);
                finishIntent.putExtra("intent_type", INTENT_PAUSE_MUSIC);
                sendBroadcast(finishIntent);
                // 更新notification
                handleIntent(new Intent(ACTION_PAUSE));
            }
        }
    }

    /**
     * 媒体按钮监听
     *
     * @author lbRoNG
     */
    private class ActivityMediaControlReceiver extends BroadcastReceiver {
        long[] mHits = new long[2]; // 判断双击事件的数组

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent finishIntent = new Intent(OPERATE_FINISH);
            if (MediaControlReceiver.ACTION_MEDIA_BUTTON.equals(intent
                    .getAction())) {
                KeyEvent event = (KeyEvent) intent
                        .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        // src 拷贝的源数组
                        // srcPos 从源数组的那个位置开始拷贝.
                        // dst 目标数组
                        // dstPos 从目标数组的那个位子开始写数据
                        // length 拷贝的元素的个数
                        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                        if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                            // 双击表示切换下一曲
                            SharedPreferences playConfig = getSharedPreferences(
                                    "playconfig", Context.MODE_PRIVATE);
                            // 从配置文件中获取播放方式
                            int nowPlayMethodId = playConfig.getInt(
                                    "play_method_id", 0);
                            if (nowPlayMethodId == PlayMusicActivity.PLAY_METHOD_RANDOM) {
                                nextMusicForRandom();
                            } else if (nowPlayMethodId == PlayMusicActivity.PLAY_METHOD_LOOP) {
                                nextMusicForLoop();
                            } else {
                                nextMusic();
                            }
                            finishIntent.putExtra("intent_type", INTENT_NEXT_MUSIC);
                        }
                        // 发送广播
                        sendBroadcast(finishIntent);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
