package com.toly1994.tolymusic.four.activity;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.MusicApplication;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.utils.DialogUtils;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.four.receiver.TimingStopMusicReceiver;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.fragment.PlayingSongControlFragment;
import com.toly1994.tolymusic.fragment.PlayingSongCoverFragemnt;
import com.toly1994.tolymusic.fragment.PlayingSongMessageFragment;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("deprecation")
public class PlayMusicActivity extends AppCompatActivity {
    private final int SLIDE_LAST = 1; // 上一首滑动方式
    private final int SLIDE_NEXT = 2; // 下一首滑动方式
    private int themeRgb; // 主题颜色
    private Song playingSong; // 正在播放的歌曲
    private int windowWidth;  // 屏幕宽度
    private static int COVER_LAST_UPDATE_POSITION = 9; // 播放列表封面刷新的最后一个位置的记录值
    private static int checkPosition = 0; // 定时播放的时间选择所在的位置
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private Fragment nowFragmentType; // 当前是歌曲控制fragemnt还是歌曲信息fragemnt
    private Toolbar bar_home;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private RecyclerView rv_play_songs;
    private ViewPager vp_playing_songs;
    private BaseMoreMenuClickListenerImpl impl;
    private PopupWindow chooseTimingPopupWindow;
    private ExecutorService pool;
    private PlaySongsAdapter playListAdapter;
    private PlayingSongPagerAdapter pagerAdatper;
    private LinearLayout rl_playing_song;
    private ImageView btn_play;
    private FragmentManager manager;
    private SharedPreferences playConfig;
    private MediaPlayer player;
    private OperateFinishReceiver finishReceiver;
    private boolean defaultUI;         // 是否处于缺省界面
    private boolean isChangePagerAutoPlay = true;  // 控制切换pager是否执行播放操作
    private boolean firstCome = true;  // 防止按钮执行动画后重绘
    private int nowPlayMethodId;       // 当前所使用的播放方式的id
    public static final int PLAY_METHOD_ORDER = 0;
    public static final int PLAY_METHOD_LOOP = 1;
    public static final int PLAY_METHOD_SIGLE_LOOP = 2;
    public static final int PLAY_METHOD_RANDOM = 3;
    // 播放方式的id数组
    public static final int[] playMethodIds = new int[]{
            R.string.play_method_order, R.string.play_method_loop,
            R.string.play_method_single_loop, R.string.play_method_random
    };
    // 播放列表 -> 侧滑菜单的播放列表,顺序是不跟随随机播放而改变顺序的
    private List<Song> playSongListAsSliding;
    // 播放列表 -> pager主页的播放列表,顺序是跟随随机播放而改变顺序的
    private List<Song> playSongListAsPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playmusic);
        // 设置音频流
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // 获取播放信息配置文件
        playConfig = getSharedPreferences("playconfig", Context.MODE_PRIVATE);
        // 从配置文件中获取播放方式
        nowPlayMethodId = playConfig.getInt("play_method_id", 0);
        // 获取MediaPlayer实例
        player = PlayingService.mediaPlayer;
        // 获取侧滑菜单的播放列表
        playSongListAsSliding = PlayingService.playList;
        // 获取当前pager的播放列表
        playSongListAsPager = (nowPlayMethodId == PLAY_METHOD_RANDOM) ? PlayingService.playListOfRandom
                : PlayingService.playList;
        // 获取当前播放的歌曲
        playingSong = playSongListAsSliding.get(playSongListAsSliding.indexOf(PlayingService.playingSong));
        // 设置界面
        setViewComponent();
    }

    @Override
    public void onStart() {
        MusicApplication.launchActivity = PlayMusicActivity.class;
        if (finishReceiver == null) {
            finishReceiver = new OperateFinishReceiver();
            IntentFilter filter = new IntentFilter(PlayingService.OPERATE_FINISH);
            registerReceiver(finishReceiver, filter);
        }
        pool = Executors.newCachedThreadPool();
        super.onStart();
    }

    @Override
    public void onStop() {
        // 关闭线程池
        pool.shutdownNow();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (finishReceiver != null) {
            unregisterReceiver(finishReceiver);
            finishReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (impl != null && impl.popupWindowIsShow()) {
            impl.popupWindowDismiss();
        } else if (MoreMenuUtils.chooseSonglistPopupWindow != null
                && MoreMenuUtils.chooseSonglistPopupWindow.isShowing()) {
            MoreMenuUtils.chooseSonglistPopupWindow.dismiss();
        } else if (chooseTimingPopupWindow != null
                && chooseTimingPopupWindow.isShowing()) {
            chooseTimingPopupWindow.dismiss();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.play_music_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (!defaultUI) {
            switch (id) {
                case R.id.action_add_songlist:
                    // 添加歌曲到播放列表
                    MoreMenuUtils.addSongToSonglist(this, new ArrayList<Song>() {{
                        add(playingSong);
                    }});
                    break;
                case R.id.action_search:
                    // 搜索音乐
                    Intent search_intent = new Intent(this, SearchActivity.class);
                    startActivity(search_intent);
                    break;
                case R.id.action_equalizer:
                    // 打开AudioFx均衡器
                    startEqualizer();
                    break;
                case R.id.more_menu_about_artist:
                    // 来自此歌手的作品
                    Artist aboutAritst = playingSong.getAlbum().getArtist();
                    MoreMenuUtils.aboutArtist(this, aboutAritst);
                    break;
                case R.id.action_delete_music_for_playlist:
                    int index = playSongListAsSliding.indexOf(playingSong);
                    // 从播放列表中删除歌曲
                    PlayingService.playList.remove(playingSong);
                    PlayingService.playListOfRandom.remove(playingSong);
                    playListAdapter.notifyDataSetChanged();
                    pagerAdatper.notifyDataSetChanged();
                    // 播放下一首
                    int position = (index == playSongListAsSliding.size() - 1) ? 0 : index + 1;
                    vp_playing_songs.setCurrentItem(position);
                    break;
                case R.id.action_add_playlist_to_songlist:
                    // 添加播放列表到歌单
                    MoreMenuUtils.addSongToSonglist(this, (ArrayList<Song>) playSongListAsSliding);
                    break;
                case R.id.action_clean_playlist:
                    // 发送清除播放列表的广播
                    Intent stopIntent = new Intent(this, PlayingService.class);
                    stopIntent.putExtra("action", PlayingService.CLEAN_PLAYLSIT);
                    startService(stopIntent);
                    break;
                case R.id.action_playing_for_timing:
                    // 定时停止播放
                    chooseTimingPopupWindow = DialogUtils.showTiming(
                            this, (view, position1) -> {
                                // 更新标识
                                checkPosition = position1;
                                // 开始设置
                                if (position1 == 0) {
                                    if (alarmManager != null) {
                                        alarmManager.cancel(alarmIntent);
                                        alarmManager = null;
                                    }
                                } else {
                                    stopMusicForTiming();
                                }
                            }, checkPosition);
                    break;
            }
        } else {
            if (id == R.id.action_search) {
                Intent search_intent = new Intent(this, SearchActivity.class);
                startActivity(search_intent);
            } else {
                Toast.makeText(this, "没有正在播放的歌曲,无效操作~", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * 定时停止播放
     */
    private void stopMusicForTiming() {
        long[] times = new long[]{0, 600000, 1200000, 1800000, 2700000, 36000000, 5400000};
        if (alarmManager == null) {
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(TimingStopMusicReceiver.ACTION_MEDIA_BUTTON), 0);
        }
        long triggerAtMillis = System.currentTimeMillis() + times[checkPosition];
        alarmManager.set(AlarmManager.RTC, triggerAtMillis, alarmIntent);
    }

    private void setViewComponent() {
        // 初始化控件
        bar_home = findViewById(R.id.bar_home);
        drawer = findViewById(R.id.drawer);
        rv_play_songs = findViewById(R.id.rv_play_songs);
        vp_playing_songs = findViewById(R.id.vp_playing_songs);

        btn_play = findViewById(R.id.btn_play);
        rl_playing_song = findViewById(R.id.rl_playing_song);


        windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        // 获取管理者
        manager = getSupportFragmentManager();
        // 计算出播放按钮的位置
        ViewTreeObserver viewObserver = rl_playing_song.getViewTreeObserver();
        viewObserver.addOnPreDrawListener(() -> {
            if (firstCome) {
                if (!player.isPlaying()) {
                    int heightAndWidth = btn_play.getMeasuredHeight();
                    // 计算位置
                    btn_play.setX(windowWidth - (heightAndWidth + heightAndWidth / 3));
                    btn_play.setY(windowWidth + (bar_home.getHeight() - (heightAndWidth / 2)));
                } else {
                    // 缩小按钮
                    btn_play.setScaleX(0.6f);
                    btn_play.setScaleY(0.6f);
                    int[] location = new int[2];
                    rl_playing_song.getLocationInWindow(location);
                    btn_play.setX(location[0]);
                    btn_play.setY(location[1] - rl_playing_song.getHeight());
                    // 屏蔽点击功能
                    btn_play.setClickable(false);
                }
                firstCome = false;
            }
            return true;
        });
        // 设置toolbar 和滑动菜单
        setToolbarAndSlidingMenu();
        // 显示内容区域
        if (!player.isPlaying()) {
            nowFragmentType = new PlayingSongMessageFragment(playingSong);
        } else {
            nowFragmentType = PlayingSongControlFragment.getInstance(playingSong);
        }
        manager.beginTransaction().add(R.id.rl_song_message, nowFragmentType).commit();
        // 设置主题颜色和歌曲内容
        setSongMessageBgColor(playingSong.getAlbum().getCover());
        // 设置歌曲信息
        if (playSongListAsSliding != null && playSongListAsSliding.size() > 0) {
            // 设置播放列表
            final LinearLayoutManager mLayoutManager = new LinearLayoutManager(
                    this);
            rv_play_songs.setLayoutManager(mLayoutManager);
            playListAdapter = new PlaySongsAdapter();
            rv_play_songs.setAdapter(playListAdapter);
            // 当前播放音乐在播放列表的位置
            int position = playSongListAsPager.indexOf(playingSong);
            // 获取完数据,初始化viewpager
            pagerAdatper = new PlayingSongPagerAdapter(getSupportFragmentManager());
            pagerAdatper.setData(playSongListAsPager);
            vp_playing_songs.setAdapter(pagerAdatper);
            vp_playing_songs.setCurrentItem(position);
            vp_playing_songs.setLayoutParams(new RelativeLayout.LayoutParams(
                    windowWidth, windowWidth));
            // 滚动封面选中监听
            vp_playing_songs.setOnPageChangeListener(new OnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    if (isChangePagerAutoPlay && playingSong != null) {
                        // 归零进度
                        PlayingService.pauseProgress = 0;
                        // 更新信息
                        updateAllPlayingSongMessage(position);
                        // 播放音乐
                        if (nowFragmentType instanceof PlayingSongControlFragment) {
                            inPlayListStartMusicIntent();
                        }
                    }
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {
                }

                @Override
                public void onPageScrollStateChanged(int arg0) {
                }
            });

            // 设置滚动监听,当界面停留的时候再加载屏幕内条目的图片
            rv_play_songs.setOnScrollListener(new OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView,
                                                 int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        // 获取第一个显示的位置
                        int firstVisibleItem = mLayoutManager
                                .findFirstVisibleItemPosition();
                        // 获取最后一个显示的位置
                        int lastVisibleItem = mLayoutManager
                                .findLastVisibleItemPosition();
                        for (int position = firstVisibleItem; position <= lastVisibleItem; position++) {
                            if (!(playSongListAsSliding.size() == 0)) {
                                setMusicCover(position);
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    // 获取最后一个显示的位置
                    int lastVisibleItem = mLayoutManager
                            .findLastVisibleItemPosition();
                    if (lastVisibleItem > COVER_LAST_UPDATE_POSITION) {
                        // 滑动速度在指定速度以下再加载图片,滑动速度快代表用户只是在找指定的歌,不必加载图片
                        if (Math.abs(dy) < 100 && Math.abs(dy) > 5) {
                            setMusicCover(lastVisibleItem);
                            COVER_LAST_UPDATE_POSITION = lastVisibleItem;
                        }
                    }
                }
            });

            // 播放列表点击监听
            playListAdapter.setOnItemClickListener((view, position1) -> {
                // 设置相关歌曲的歌手和专辑信息
                setAboutPlayingSongMessage(position1);
                // 判断是否随机播放,是随机播放要找到随机列表中对应的歌曲位置
                position1 = playSongListAsPager.indexOf(playSongListAsSliding.get(position1));
                // 通知pager显示对应的信息
                vp_playing_songs.setCurrentItem(position1);
            });

            // 获取提供菜单选项的内容适配器
            MoreMenuBaseAdapter mplayListAdapter = new MoreMenuBaseAdapter(this,
                    MoreMenuUtils.moreMenusAsPlayList);
            // 把内容给点击事件的实现类,完成点击跳出对应菜单
            impl = new BaseMoreMenuClickListenerImpl(this, mplayListAdapter);
            // 给菜单内的选项添加点击事件
            impl.setOnItemClickListener(
                    (fromView, clickView, fromViewPositon, clickViewPosition) -> {
                        final Song clickSongItem = playSongListAsSliding.get(fromViewPositon);
                        // 处理菜单点击选项
                        switch (clickViewPosition) {
                            case 0:
                                // 下一首播放
                                if (playingSong != null) {
                                    MoreMenuUtils.swapMusicUnderPlayingSong(clickSongItem);
                                    playListAdapter.notifyDataSetChanged();
                                    Toast.makeText(PlayMusicActivity.this, "播放队列已更新", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(PlayMusicActivity.this, "没有播放队列", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 1:
                                // 添加歌曲到歌单
                                MoreMenuUtils.addSongToSonglist(PlayMusicActivity.this
                                        , new ArrayList<Song>() {{
                                            add(clickSongItem);
                                        }});
                                break;
                            case 2:
                                // 关于歌手
                                MoreMenuUtils.aboutArtist(PlayMusicActivity.this, clickSongItem.getAlbum().getArtist());
                                break;
                            case 3:
                                // 设置为铃声
                                if (MoreMenuUtils.setVoice(PlayMusicActivity.this, clickSongItem.getUrl())) {
                                    Toast.makeText(PlayMusicActivity.this, "设置铃声成功", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(PlayMusicActivity.this, "设置铃声失败", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 4:
                                // 从播放列表中删除歌曲
                                PlayingService.playList.remove(clickSongItem);
                                PlayingService.playListOfRandom.remove(clickSongItem);
                                playListAdapter.notifyDataSetChanged();
                                pagerAdatper.notifyDataSetChanged();
                                // 如果当前删除的音乐正在播放,切换到下一首
                                if (clickSongItem.equals(playingSong)) {
                                    int index = playSongListAsSliding.indexOf(playingSong);
                                    index = (index == playSongListAsSliding.size() - 1) ? 0 : index + 1;
                                    vp_playing_songs.setCurrentItem(index);
                                } else {
                                    // 切换pager不切换当前播放的歌曲,只是集合顺序改变的刷新操作
                                    int index = playSongListAsPager.indexOf(playingSong);
                                    isChangePagerAutoPlay = false;
                                    vp_playing_songs.setCurrentItem(index, false);
                                    isChangePagerAutoPlay = true;
                                }
                                break;
                        }
                        // 隐藏菜单
                        impl.popupWindowDismiss();
                    });
            // 设置菜单按钮的点击事件
            playListAdapter.setOnMoreMenuClickListener(impl);
        }
    }

    /**
     * 启动内置均衡器,没有则提示
     */
    private void startEqualizer() {
        try {
            Intent intent = this.getPackageManager().getLaunchIntentForPackage(
                    "org.cyanogenmod.audiofx");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "没有安装AudioFX", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 在已知播放列表里切歌的Intent,无需重复传入播放列表的数据给服务
     */
    private void inPlayListStartMusicIntent() {
        
        Intent send = new Intent(PlayMusicActivity.this, PlayingService.class);
        send.putExtra("action", PlayingService.INTENT_PLAYLIST_START_MUSIC);
        send.putExtra("playing_song", playingSong);
        startService(send);
    }

    /**
     * 点击播放按钮,显示控制界面并播放当前歌曲
     *
     * @param v
     */
    public void btnPlayMusic(View v) {
        inPlayListStartMusicIntent();
        playButtonAnim();
    }

    /**
     * 播放歌曲的动画
     */
    private void playButtonAnim() {
        // 计算平移坐标
        int[] location = new int[2];
        rl_playing_song.getLocationInWindow(location);
        // 设置动画
        ObjectAnimator one = ObjectAnimator.ofFloat(btn_play, "scaleX", 1f, 0.6f);
        ObjectAnimator two = ObjectAnimator.ofFloat(btn_play, "scaleY", 1f, 0.6f);
        ObjectAnimator three = ObjectAnimator.ofFloat(btn_play, "translationX", location[0]);
        ObjectAnimator four = ObjectAnimator.ofFloat(btn_play, "translationY", location[1] - rl_playing_song.getHeight() );
        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(1000);
        animSet.playTogether(one, two, three, four);
        animSet.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // 处理歌曲信息区域的背景和信息
                setShapeDrawableBgColor(btn_play, getResources().getColor(R.color.titleBackground));
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        animSet.start();
        // 显示歌曲控制区域
        FragmentTransaction tran = manager.beginTransaction();
        tran.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        nowFragmentType = PlayingSongControlFragment.getInstance(playingSong);
        tran.replace(R.id.rl_song_message, nowFragmentType).commit();
        // 设置按钮不可用
        btn_play.setClickable(false);
    }

    /**
     * 实现和PlayingSongControlFragment通信接口的方法
     * 暂停歌曲的动画与设置
     */
    private void pauseMusicAnim() {
        // 计算位置
        int heightAndWidth = btn_play.getMeasuredHeight();
        // 设置动画
        ObjectAnimator one = ObjectAnimator.ofFloat(btn_play, "scaleX", 0.6f, 1f);
        ObjectAnimator two = ObjectAnimator.ofFloat(btn_play, "scaleY", 0.6f, 1f);
        ObjectAnimator three = ObjectAnimator.ofFloat(btn_play, "translationX", windowWidth - (heightAndWidth + heightAndWidth / 3));
        ObjectAnimator four = ObjectAnimator.ofFloat(btn_play, "translationY", windowWidth + (bar_home.getHeight() - (heightAndWidth / 2)));
        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(1000);
        animSet.playTogether(one, two, three, four);
        animSet.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // 处理歌曲信息区域的背景和信息
                setShapeDrawableBgColor(btn_play, themeRgb);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        animSet.start();
        // 切换歌曲控制台
        FragmentTransaction tran = manager.beginTransaction();
        tran.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        nowFragmentType = new PlayingSongMessageFragment(playingSong);
        tran.replace(R.id.rl_song_message, nowFragmentType).commit();
        // 设置按钮不可用
        btn_play.setClickable(true);
    }

    /**
     * 设置没有播放歌曲时候的界面信息
     */
    private void setDefaultNotPlayingSongMessage() {
        // 设置缺省标记
        defaultUI = true;
        // 切换到暂停播放的状态
        pauseMusicAnim();
        // 设置封面
        pagerAdatper.setData(new ArrayList<Song>(1) {{
            add(null);
        }});
        pagerAdatper.notifyDataSetChanged();
        vp_playing_songs.setCurrentItem(0, false);
        // 刷新清空播放列表
        playListAdapter.notifyDataSetChanged();
        // 设置主题颜色
        setSongMessageBgColor(BitmapFactory.decodeResource(getResources(), R.drawable.default_music_icon));
    }

    /**
     * 设置shape图片资源的背景色
     *
     * @param itemView
     * @param rgb
     */
    private void setShapeDrawableBgColor(View itemView, int rgb) {
        RippleDrawable rd = (RippleDrawable) itemView.getBackground();
        GradientDrawable sd = (GradientDrawable) rd.getDrawable(0);
        sd.setColor(rgb);
    }

    /**
     * 更换了正在播放的歌曲,切换全部相关的信息
     *
     * @param newPosition
     */
    public void updateAllPlayingSongMessage(int newPosition) {
        // 设置相关歌曲的歌手和专辑信息
        setAboutPlayingSongMessage(newPosition);
        // 设置主题颜色和歌曲内容
        setSongMessageBgColor(playingSong.getAlbum().getCover());
    }

    /**
     * 设置正在播放或者选中歌曲相关歌手专辑信息的区域
     * 并在此更新正在播放的歌曲实例
     *
     * @param newPosition
     */
    private void setAboutPlayingSongMessage(int newPosition) {
        int oldPosition = playSongListAsPager.indexOf(playingSong);
        // 取消旧的正在播放的歌曲标记
        playListAdapter.notifyItemChanged(playSongListAsSliding.indexOf(playingSong));
        // 更新正在播放的歌曲实例
        playingSong = playSongListAsPager.get(newPosition);
        PlayingService.playingSong = playingSong;
        // 添加新的正在播放的标记
        playListAdapter.notifyItemChanged(newPosition);
        // 设置信息区域的切换动画
        if (nowFragmentType instanceof PlayingSongMessageFragment) {
            // 滑动方式
            int sildeMethod = 0;
            // 判断是下一首还是上一首的操作
            if (oldPosition > newPosition) {
                sildeMethod = SLIDE_LAST;
            } else if (oldPosition < newPosition) {
                sildeMethod = SLIDE_NEXT;
            }
            FragmentTransaction tran = manager.beginTransaction();
            // 设置歌曲相关专辑和歌手信息
            if (sildeMethod == SLIDE_LAST) {
                tran.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            } else if (sildeMethod == SLIDE_NEXT) {
                tran.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            }
            tran.replace(R.id.rl_song_message, new PlayingSongMessageFragment(playingSong)).commit();
            // 发送广播
            startService(new Intent(PlayMusicActivity.this, PlayingService.class)
                    .putExtra("action", PlayingService.INTENT_PLAYLIST_CHANGE_MUSIC));
        }
    }

    /**
     * 跟新播放列表内的歌曲封面
     */
    private void setMusicCover(final int position) {
        final Song songItem = playSongListAsSliding.get(position);
        if (songItem.getAlbum().getCover() == null) {
            // 提交任务给线程池处理
            pool.submit(() -> {
                Bitmap cover = ImageUtils.getArtwork(
                        PlayMusicActivity.this, songItem.getTitle(),
                        songItem.getSongId(), songItem.getAlbum()
                                .getAlbumId(), true);
                if (cover != null) {
                    songItem.getAlbum().setCover(cover);
                } else {
                    songItem.getAlbum().setCover(
                            BitmapFactory.decodeResource(getResources(),
                                    R.drawable.default_music_icon));
                }
                runOnUiThread(() -> playListAdapter.notifyItemChanged(position));
            });
        }
    }


    /**
     * 设置toobar和drawer侧滑菜单
     */
    private void setToolbarAndSlidingMenu() {
        // tool bar设置
        setSupportActionBar(bar_home);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // 设置滑动菜单
        mDrawerToggle = new ActionBarDrawerToggle(this, drawer, bar_home,
                R.string.slide_open, R.string.slide_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // 滑动到当前播放的歌曲
                int index = playSongListAsSliding.indexOf(playingSong);
                rv_play_songs.scrollToPosition(index);
                // 隐藏播放按钮
                float fromScale = 1f;
                if (player.isPlaying()) {
                    fromScale = 0.6f;
                }
                PropertyValuesHolder one = PropertyValuesHolder.ofFloat("scaleX", fromScale, 0.0f);
                PropertyValuesHolder two = PropertyValuesHolder.ofFloat("scaleY", fromScale, 0.0f);
                ObjectAnimator.ofPropertyValuesHolder(btn_play, one, two).setDuration(400).start();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                float toScale = 1f;
                if (player.isPlaying()) {
                    toScale = 0.6f;
                }
                PropertyValuesHolder one = PropertyValuesHolder.ofFloat("scaleX", 0.0f, toScale);
                PropertyValuesHolder two = PropertyValuesHolder.ofFloat("scaleY", 0.0f, toScale);
                ObjectAnimator.ofPropertyValuesHolder(btn_play, one, two).setDuration(400).start();
            }

        };
        mDrawerToggle.syncState();
        drawer.setDrawerListener(mDrawerToggle);
        // 显示滑动菜单内的播放方式
        final RelativeLayout rl_play_method = (RelativeLayout) findViewById(R.id.rl_play_method);
        final TextView tv_play_method = (TextView) rl_play_method.findViewById(R.id.tv_play_method);
        final ImageView iv_change_method = (ImageView) rl_play_method.findViewById(R.id.iv_change_method);
        final String beforeStr = getString(R.string.play_method) + " : ";
        if (nowPlayMethodId != -1) {
            tv_play_method.setText(beforeStr + getString(playMethodIds[nowPlayMethodId]));
        } else {
            tv_play_method.setText(beforeStr + getString(playMethodIds[0]));
        }
        // 监听点击,切换播放方式
        class ChangeMethodClickListener implements OnClickListener {
            @Override
            public void onClick(View v) {
                // 标识为改变播放方式
                isChangePagerAutoPlay = false;
                // 计算出下一个播放方式的ID
                if (++nowPlayMethodId == 4) {
                    nowPlayMethodId = 0;
                }
                // 设置播放方式
                tv_play_method.setText(beforeStr + getString(playMethodIds[nowPlayMethodId]));
                tv_play_method.setTag(nowPlayMethodId);
                // 是随机播放就刷新pager数据列表,切换到随机列表
                if (playingSong != null) {
                    int nowPlayingSongPosition = 0;
                    playSongListAsPager = (nowPlayMethodId == PLAY_METHOD_RANDOM) ? PlayingService.shufflePlayList()
                            : PlayingService.playList;
                    pagerAdatper.setData(playSongListAsPager);
                    nowPlayingSongPosition = playSongListAsPager.indexOf(playingSong);
                    // 刷新封面pager
                    pagerAdatper.notifyDataSetChanged();
                    // 滑动到当前播放的歌曲封面,取消滑动的平滑动画
                    vp_playing_songs.setCurrentItem(nowPlayingSongPosition, false);
                }
                // 滑动完毕后重置标识
                isChangePagerAutoPlay = true;
                // 保存播放方式
                playConfig.edit().putInt("play_method_id", nowPlayMethodId).apply();
            }
        }
        ChangeMethodClickListener changeMethodListener = new ChangeMethodClickListener();
        rl_play_method.setOnClickListener(changeMethodListener);
        iv_change_method.setOnClickListener(changeMethodListener);
    }

    /**
     * 通过封面获取主题颜色,设置相关背景统一风格
     *
     * @param bitmap
     */
    private void setSongMessageBgColor(final Bitmap bitmap) {
        Palette.Swatch vibrant = Palette.generate(bitmap).getVibrantSwatch();
        int defaultRgb = getResources().getColor(R.color.titleBackground);
        if (vibrant == null) {
            // 分析不出颜色,默认颜色
            themeRgb = defaultRgb;
        } else {
            themeRgb = vibrant.getRgb();
        }
        // toolbar颜色
        bar_home.setBackgroundColor(themeRgb);
        // 状态栏颜色
        getWindow().setStatusBarColor(ImageUtils.colorBurn(themeRgb));
        if (nowFragmentType instanceof PlayingSongMessageFragment) {
            // 不是播放状态的,按钮颜色跟随主题改变
            setShapeDrawableBgColor(btn_play, themeRgb);
        } else {
            setShapeDrawableBgColor(btn_play, defaultRgb);
        }
    }

    /**
     * 接收播放服务操作(播放)完成的广播
     *
     * @author lbRoNG
     */
    private class OperateFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intent_type = intent.getStringExtra("intent_type");
            // 更新标识
            player = PlayingService.mediaPlayer;
            // 不是INTENT_START_MUSIC就根据当前播放列表获取当前播放的歌曲
            // INTENT_START_MUSIC表示传入新的播放列表,需要更新列表后才能获取得到当前播放的歌曲
            if (!PlayingService.INTENT_START_MUSIC.equals(intent_type)) {
                // playingSong属于Song实例,通过intent传递的实例不包含album和artist信息,通过完整列表获取到完整的实例
                playingSong = playSongListAsSliding.get(playSongListAsSliding.indexOf(PlayingService.playingSong));
            }
            // 服务内对歌曲的操作完成
            if (PlayingService.INTENT_START_MUSIC.equals(intent_type)) {
                finishSetMessageOfNewPlayList();
            } else if (PlayingService.INTENT_PLAYLIST_START_MUSIC.equals(intent_type)) {
                // 判断是否是notification的操作
                if (intent.getBooleanExtra("is_notifacation_send", false)) {
                    playButtonAnim();
                }
                finishSetMessage();
            } else if (PlayingService.INTENT_PAUSE_MUSIC.equals(intent_type)) {
                pauseMusicAnim();
            } else if (PlayingService.INTENT_STOP_MUSIC.equals(intent_type)) {
                finish();
            } else if (PlayingService.INTENT_LAST_MUSIC.equals(intent_type)) {
                nextOrLastUpdateMessage();
            } else if (PlayingService.INTENT_NEXT_MUSIC.equals(intent_type)) {
                nextOrLastUpdateMessage();
            } else if (PlayingService.CLEAN_PLAYLSIT.equals(intent_type)) {
                setDefaultNotPlayingSongMessage();
            }
        }

        /**
         * 切换歌曲的逻辑处理
         */
        private void nextOrLastUpdateMessage() {
            // 切换pager
            int index = playSongListAsPager.indexOf(playingSong);
            isChangePagerAutoPlay = false;
            vp_playing_songs.setCurrentItem(index);
            isChangePagerAutoPlay = true;
            // 设置控制台
            if (nowFragmentType instanceof PlayingSongMessageFragment) {
                playButtonAnim();
            }
            finishSetMessage();
        }

        /**
         * 播放列表内切换歌曲设置界面
         */
        private void finishSetMessage() {
            // 刷新播放列表
            playListAdapter.notifyDataSetChanged();
            // 设置主题颜色
            setSongMessageBgColor(playingSong.getAlbum().getCover());
        }

        /**
         * 新的播放列表传入设置界面
         */
        private void finishSetMessageOfNewPlayList() {
            // 获取侧滑菜单的播放列表
            playSongListAsSliding = PlayingService.playList;
            // 获取当前pager的播放列表
            playSongListAsPager = (nowPlayMethodId == PLAY_METHOD_RANDOM) ? PlayingService.playListOfRandom
                    : PlayingService.playList;
            pagerAdatper.setData(playSongListAsPager);
            // 更新当前播放歌曲
            playingSong = playSongListAsSliding.get(playSongListAsSliding.indexOf(PlayingService.playingSong));
            // 刷新播放列表
            playListAdapter.notifyDataSetChanged();
            // 切换pager
            int index = playSongListAsPager.indexOf(playingSong);
            isChangePagerAutoPlay = false;
            vp_playing_songs.setCurrentItem(index);
            isChangePagerAutoPlay = true;
            finishSetMessage();
        }
    }

    /**
     * 封面pager内容适配器
     *
     * @author lbRoNG
     */
    private class PlayingSongPagerAdapter extends FragmentStatePagerAdapter {
        private List<Song> tempData;

        public void setData(List<Song> source) {
            tempData = source;
        }

        public PlayingSongPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tempData.get(position).getTitle();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return tempData.size();
        }

        @Override
        public Fragment getItem(int position) {
            return new PlayingSongCoverFragemnt(playSongListAsPager.get(position));
        }
    }

    /**
     * 播放列表recycleview内容适配器
     *
     * @author lbRoNG
     */
    private class PlaySongsAdapter extends
            RecyclerView.Adapter<PlaySongsAdapter.ViewHolder> {
        // 点击事件监听器接口
        private OnBaseClickListener mOnItemClickListener = null;
        private OnBaseClickListener mOnMoreMenuClickListener = null;

        // 对外提供的设置监听器方法
        public void setOnItemClickListener(OnBaseClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        // 对外提供的设置监听器方法
        public void setOnMoreMenuClickListener(OnBaseClickListener listener) {
            this.mOnMoreMenuClickListener = listener;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private View itemView;
            private TextView tv_music_name, tv_music_albums;
            private ImageView iv_music_icon, iv_more, iv_playing;

            public ViewHolder(View v, int type) {
                super(v);
                this.itemView = v;
                tv_music_name = (TextView) v.findViewById(R.id.tv_music_name);
                tv_music_albums = (TextView) v
                        .findViewById(R.id.tv_music_albums);
                iv_music_icon = (ImageView) v.findViewById(R.id.iv_music_icon);
                iv_more = (ImageView) v.findViewById(R.id.iv_more);
                iv_playing = (ImageView) v.findViewById(R.id.iv_playing);
            }

            public View getItemView() {
                return itemView;
            }
        }

        @Override
        public PlaySongsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.music_item, parent, false);
            ViewHolder holder = new ViewHolder(view, viewType);
            return holder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            // 默认加载前10首歌曲的封面
            if (position < 10) {
                setMusicCover(position);
            }
            final Song item = playSongListAsSliding.get(position);
            holder.tv_music_name.setText(item.getTitle());
            holder.tv_music_albums.setText(item.getAlbum().getArtist()
                    .getSingerName()
                    + " | " + item.getAlbum().getAlbumName());
            holder.iv_music_icon.setImageBitmap(item.getAlbum().getCover());
            if (playingSong != null && item.equals(playingSong)) {
                holder.iv_playing.setVisibility(View.VISIBLE);
            } else {
                holder.iv_playing.setVisibility(View.GONE);
            }
            holder.iv_more.setOnClickListener(v -> {
                if (mOnMoreMenuClickListener != null) {
                    mOnMoreMenuClickListener.onClick(v, position);
                }
            });
            // 处理点击事件
            holder.getItemView().setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return playSongListAsSliding.size();
        }
    }

}
