package com.toly1994.tolymusic.four.activity;

import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.MusicApplication;
import com.toly1994.tolymusic.app.db.dao.MusicDao;
import com.toly1994.tolymusic.app.domain.*;
import com.toly1994.tolymusic.app.utils.permission.Permission;
import com.toly1994.tolymusic.app.utils.permission.PermissionActivity;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.fragment.AlbumsFragment;
import com.toly1994.tolymusic.fragment.ArtistFragment;
import com.toly1994.tolymusic.fragment.SongListFragment;
import com.toly1994.tolymusic.fragment.SongsFragment;
import com.toly1994.tolymusic.widget.ProgressView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeActivity extends PermissionActivity {
    public final static String LOADER_SEARCH_FINISH = "LOADER_SEARCH_FINISH";
    public final static String INTENT_MUSIC_DELETE = "MUSIC_DELETE";
    private static List<Song> songs; // 包含为整理的所有音乐
    private Intent intent = new Intent(LOADER_SEARCH_FINISH); //检索完成的粘性广播
    private Toolbar bar_home;
    private ViewPager vp_home;
    private TabLayout mTabLayout;
    private TextView tv_playing_window_songname, tv_playing_window_singer;
    private ImageView iv_playing_window_cover, iv_playing_window_flag, iv_playing_window_next;
    private OperateFinishReceiver finishReceiver;
    private MusicDeleteReceiver deleteReceiver;
    public static Song playingSong;
    private RelativeLayout rl_bottom_control;
    private ProgressView id_pv_pre;
    private SharedPreferences playConfig;

    public static List<Song> getSongs() {
        return songs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        applyPermissions(Permission.WRITE_EXTERNAL_STORAGE);
    }


    @Override
    protected void permissionOk(boolean isFirst) {
        playConfig = getSharedPreferences("playconfig", Context.MODE_PRIVATE);
        if (songs == null) {
            songs = new ArrayList<>();
            // 加载主程序就就搜索本地音乐
            getLocalMusic(true, null);
        }
        setViewComponent();
    }

    @Override
    public void onStart() {
        MusicApplication.launchActivity = HomeActivity.class;
        // 服务内有歌曲播放或暂停中
        if (PlayingService.mediaPlayer != null) {
            playingSong = songs.get(songs.indexOf(PlayingService.playingSong));
            updatePlayingSongMessage(playingSong);
            changeBtnState(PlayingService.mediaPlayer.isPlaying());
        }
        // 注册广播
        if (finishReceiver == null) {
            finishReceiver = new OperateFinishReceiver();
            IntentFilter filter = new IntentFilter(PlayingService.OPERATE_FINISH);
            registerReceiver(finishReceiver, filter);
        }
        // 注册广播
        if (deleteReceiver == null) {
            deleteReceiver = new MusicDeleteReceiver();
            IntentFilter filter = new IntentFilter(INTENT_MUSIC_DELETE);
            registerReceiver(deleteReceiver, filter);
        }
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if (finishReceiver != null) {
            unregisterReceiver(finishReceiver);
            finishReceiver = null;
        }
        if (deleteReceiver != null) {
            unregisterReceiver(deleteReceiver);
            deleteReceiver = null;
        }
        // 结束粘性广播
        removeStickyBroadcast(intent);

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:
                Intent search_intent = new Intent(this, SearchActivity.class);
                startActivity(search_intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 初始化操作
     */
    private void setViewComponent() {
        bar_home = findViewById(R.id.bar_home);
        vp_home = findViewById(R.id.vp_home);
        tv_playing_window_songname = findViewById(R.id.tv_playing_window_songname);
        tv_playing_window_singer = findViewById(R.id.tv_playing_window_singer);
        iv_playing_window_cover = findViewById(R.id.iv_playing_window_cover);
        iv_playing_window_flag = findViewById(R.id.iv_playing_window_flag);
        iv_playing_window_next = findViewById(R.id.iv_playing_window_next);
        rl_bottom_control = findViewById(R.id.rl_bottom_control);
        id_pv_pre = findViewById(R.id.id_pv_pre);
        mTabLayout = findViewById(R.id.id_tl_tab);

        bar_home.setTitle("IMusic"); // 标题的文字需在setSupportActionBar之前，不然会无效
        setSupportActionBar(bar_home);

        vp_home.setAdapter(new MusicPagerAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(vp_home);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        // 默认选中歌曲列表界面
        vp_home.setCurrentItem(0);

        // 设置按钮监听
        iv_playing_window_flag.setOnClickListener(v -> {
            // 获取表示并且切换按钮
            if (playingSong != null && PlayingService.playList.size() != 0) {
                // 有当前播放歌曲
                boolean isPlaying = (boolean) iv_playing_window_flag.getTag();
                changeBtnState(!isPlaying);
                // 播放或暂停当前音乐
                Intent send = new Intent(HomeActivity.this, PlayingService.class);
                String intentStr = isPlaying ? PlayingService.INTENT_PAUSE_MUSIC : PlayingService.INTENT_PLAYLIST_START_MUSIC;
                send.putExtra("action", intentStr);
                send.putExtra("playing_song", playingSong);
                startService(send);
            } else if (playingSong == null && PlayingService.playList.size() != 0) {
                getPlayListFirstPlay();
            } else {
                Toast.makeText(HomeActivity.this, "没有正在播放的歌曲和播放队列..", Toast.LENGTH_SHORT).show();
            }
        });

        //主界面下一曲
        iv_playing_window_next.setOnClickListener(v -> {
            if (playingSong != null) {
                Intent send = new Intent(HomeActivity.this, PlayingService.class);
                send.putExtra("action", PlayingService.INTENT_NEXT_MUSIC);
                startService(send);
            } else {
                Toast.makeText(HomeActivity.this, "没有正在播放的歌曲,无效操作..", Toast.LENGTH_SHORT).show();
            }
        });

        //主界面暂停/播放
        rl_bottom_control.setOnClickListener(v -> {
            // 获取表示并且切换按钮
            if (playingSong != null && PlayingService.playList.size() != 0) {
                Intent send = new Intent(HomeActivity.this, PlayMusicActivity.class);
                startActivity(send);
            } else if (playingSong == null && PlayingService.playList.size() != 0) {
                getPlayListFirstPlay();
            } else {
                Toast.makeText(HomeActivity.this, "没有正在播放的歌曲和播放队列..", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 控制seekbar更新
     */
    private MediaPlayer player;
    private Timer timer;

    private void updateMusicSeekBar() {
        player = PlayingService.mediaPlayer;
        // 清空上一次的进度
        id_pv_pre.setProgress(0);
        // 开始间隔1秒更新
        timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                int pre = (int) (player.getCurrentPosition() * 100f / player.getDuration());
                id_pv_pre.setProgress(pre);
            }
        };
        timer.schedule(task, 0, 1000);
    }



    /**
     * 获取播放列表中第一首歌曲播放
     */
    private void getPlayListFirstPlay() {
        // 改变按钮状态
        changeBtnState(true);
        // 不存在当前播放歌曲,但是播放列表长度大于等于1,点击播放就播放队列中的第一首
        int nowPlayMethodId = playConfig.getInt("play_method_id", -1);
        playingSong = (nowPlayMethodId == PlayMusicActivity.PLAY_METHOD_RANDOM) ? PlayingService.playListOfRandom.get(0)
                : PlayingService.playList.get(0);
        // 播放歌曲
        Intent send = new Intent(HomeActivity.this, PlayingService.class);
        send.putExtra("action", PlayingService.INTENT_PLAYLIST_START_MUSIC);
        send.putExtra("playing_song", playingSong);
        startService(send);
    }

    /**
     * 第一次初始化app,找到最后播放的歌曲,恢复数据
     */
    private void findLastPlayingSong() {
        String title = playConfig.getString("last_playing_song", "");
        long id = playConfig.getLong("last_playing_song_id", -1);
        long duration = playConfig.getLong("last_playing_song_duration", -1);
        if (!title.equals("") && id != -1 && duration != -1) {
            Song lastSong = new Song(null, title, id, null, duration);
            int index = songs.indexOf(lastSong);
            if (index != -1) {
                playingSong = songs.get(index);
                // 设置数据
                updatePlayingSongMessage(playingSong);
                // 设置按钮
                changeBtnState(false);
            }
        }
    }

    /**
     * 播放按钮和暂停按钮的切换
     *
     * @param isPlaying
     */
    private void changeBtnState(boolean isPlaying) {
        updateMusicSeekBar();
        if (isPlaying) {
            iv_playing_window_flag.setImageResource(R.drawable.icon_start_2);
        } else {
            iv_playing_window_flag.setImageResource(R.drawable.icon_stop_2);
        }
        iv_playing_window_flag.setTag(isPlaying);
    }

    /**
     * 更新底部小控制台的歌曲信息
     *
     * @param item
     */
    private void updatePlayingSongMessage(Song item) {
        String title = null, album = null;
        Bitmap cover = null;
        if (item != null) {
            title = item.getTitle();
            album = item.getAlbum().getArtist().getSingerName();
            cover = item.getAlbum().getCover();
            if (cover == null) {
                cover = BitmapFactory.decodeResource(
                        getResources(), R.drawable.default_music_icon);
            }
        }

        tv_playing_window_songname.setText(title);
        tv_playing_window_singer.setText(album);
        iv_playing_window_cover.setImageBitmap(cover);
    }


    /**
     * 搜索本地音乐
     *
     * @param isFirst 是否是第一次扫描
     * @param innerUI 哪个UI需要执行扫描操作,null表示程序启动
     */
    private void getLocalMusic(final boolean isFirst, final DeleteInnerUI innerUI) {
        new MusicDao(HomeActivity.this) {
            @Override
            public void onResetLoader(Loader<Cursor> loader) {
                songs.clear();
            }

            @Override
            public Loader<Cursor> onLoaderCreate(Uri contentUri, int id,
                                                 Bundle args) {
                String[] projection = new String[]{
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ARTIST_ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.IS_MUSIC};
                return new CursorLoader(HomeActivity.this, contentUri,
                        projection, null, null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            }

            @Override
            public void onFinishedLoader(Loader<Cursor> loader, Cursor data,
                                         LoaderManager manager) {
                if (data != null) {
                    // 清除旧数据
                    songs.clear();
                    // 获取所需列的索引
                    int albumIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int artistIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int titleIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int durationIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int songidIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int albumidIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                    int artistidIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
                    int dataUrlIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int isMusicIdx = data
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC);

                    while (data.moveToNext()) {
                        int isMusic = data.getInt(isMusicIdx);
                        if (isMusic != 0) {
                            String album = data.getString(albumIdx);
                            String artist = data.getString(artistIdx);
                            String title = data.getString(titleIdx);
                            String dataUrl = data.getString(dataUrlIdx);
                            long duration = data.getLong(durationIdx);
                            long songId = data.getLong(songidIdx);
                            long albumId = data.getLong(albumidIdx);
                            long artistId = data.getLong(artistidIdx);
                            Song item = new Song(new Album(albumId, album,
                                    new Artist(artistId, artist,
                                            new ArtistMusicInfo()), null),
                                    title, songId, dataUrl, duration);
                            songs.add(item);
                        }
                    }
                    // 歌曲检索完毕
                    manager.destroyLoader(SONG_LOADER_ID); // 销毁loader
                    // 发送粘性广播,保证fragment创建完成能收到广播(搜索完成可能在创建完fragment之前)
                    intent.putExtra("innerUI", innerUI);
                    HomeActivity.this.sendStickyBroadcast(intent);
                    if (isFirst) {
                        // 检索有没有播放记录
                        findLastPlayingSong();
                    }
                }
            }
        }.getMusic4Type(MusicDao.SONG_LOADER_ID);
    }

    /**
     * 歌曲删除的广播接收
     *
     * @author lbRoNG
     */
    private class MusicDeleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 重新扫描本地音乐
            getLocalMusic(false, (DeleteInnerUI) intent.getSerializableExtra("innerUI"));
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
            playingSong = PlayingService.playingSong;
            if (playingSong != null && playingSong.getAlbum() == null) {
                playingSong = songs.get(songs.indexOf(PlayingService.playingSong));
            }
            // 服务内对歌曲的操作完成
            if (PlayingService.INTENT_START_MUSIC.equals(intent_type)) {
                updatePlayingSongMessage(playingSong);
                changeBtnState(true);
            } else if (PlayingService.INTENT_PLAYLIST_START_MUSIC.equals(intent_type)) {
                updatePlayingSongMessage(playingSong);
                changeBtnState(true);
            } else if (PlayingService.INTENT_PAUSE_MUSIC.equals(intent_type)) {
                changeBtnState(false);
            } else if (PlayingService.INTENT_STOP_MUSIC.equals(intent_type)) {
                changeBtnState(false);
                updatePlayingSongMessage(playingSong);
            } else if (PlayingService.INTENT_LAST_MUSIC.equals(intent_type)) {
                changeBtnState(true);
                updatePlayingSongMessage(playingSong);
            } else if (PlayingService.INTENT_NEXT_MUSIC.equals(intent_type)) {
                changeBtnState(true);
                updatePlayingSongMessage(playingSong);
            } else if (PlayingService.CLEAN_PLAYLSIT.equals(intent_type)) {
                changeBtnState(false);
                updatePlayingSongMessage(playingSong);
            }
        }
    }

    /**
     * tab界面pager适配器
     *
     * @author lbRoNG
     */
    private class MusicPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<Fragment>() {{
            add(new SongsFragment());
            add(new ArtistFragment());
            add(new AlbumsFragment());
            add(new SongListFragment());
        }};
        private final String[] TITLES = {"歌曲", "歌手", "专辑", "歌单"};

        public MusicPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }
    }
}
