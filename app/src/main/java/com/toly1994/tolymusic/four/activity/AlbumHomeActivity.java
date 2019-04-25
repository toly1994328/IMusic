package com.toly1994.tolymusic.four.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.*;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.MusicApplication;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.DeleteInnerUI;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MTextUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlbumHomeActivity extends AppCompatActivity {
    private RecyclerView rv_album_home_songs;
    private ImageView iv_album_home_cover;
    private TextView tv_album_home_song_count, tv_album_home_title,
            tv_album_home_time_count;
    private BaseAdapter mAdapter;
    private List<Song> songs;
    private Toolbar bar_home;
    private BaseMoreMenuClickListenerImpl impl;
    private String searchKey = "";
    private int themeRgb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_home);
        setViewComponent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MusicApplication.launchActivity = AlbumHomeActivity.class;
    }

    @Override
    public void onBackPressed() {
        if (impl != null && impl.popupWindowIsShow()) {
            impl.popupWindowDismiss();
        } else if (MoreMenuUtils.chooseSonglistPopupWindow != null
                && MoreMenuUtils.chooseSonglistPopupWindow.isShowing()) {
            MoreMenuUtils.chooseSonglistPopupWindow.dismiss();
        } else {
            finish();
        }
    }

    private void setViewComponent() {
        bar_home = (Toolbar) findViewById(R.id.bar_home);
        iv_album_home_cover = (ImageView) findViewById(R.id.iv_album_home_cover);
        tv_album_home_title = (TextView) findViewById(R.id.tv_album_home_title);
        tv_album_home_song_count = (TextView) findViewById(R.id.tv_album_home_song_count);
        tv_album_home_time_count = (TextView) findViewById(R.id.tv_album_home_time_count);
        rv_album_home_songs = (RecyclerView) findViewById(R.id.rv_album_home_songs);

        rv_album_home_songs.setLayoutManager(new LinearLayoutManager(this));
        rv_album_home_songs.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new BaseAdapter();
        rv_album_home_songs.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener((view, position) ->
                MoreMenuUtils.playSongIntent(AlbumHomeActivity.this, (ArrayList<Song>) songs, songs.get(position)));

        // 获取提供菜单选项的内容适配器
        MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(this, MoreMenuUtils.moreMenusAsSong);
        // 把内容给点击事件的实现类,完成点击跳出对应菜单
        impl = new BaseMoreMenuClickListenerImpl(this, mmAdapter);
        // 给菜单内的选项添加点击事件
        impl.setOnItemClickListener((fromView, clickView, fromViewPositon, clickViewPosition) -> {
            final Song clickItem = songs.get(fromViewPositon);
            // 处理菜单点击选项
            switch (clickViewPosition) {
                case 0:
                    MoreMenuUtils.playSongIntent(AlbumHomeActivity.this, (ArrayList<Song>) songs, clickItem);
                    break;
                case 1:
                    // 下一首播放
                    if (PlayingService.playList != null && PlayingService.playingSong != null) {
                        MoreMenuUtils.swapMusicUnderPlayingSong(clickItem);
                        Toast.makeText(AlbumHomeActivity.this, "播放队列已更新", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AlbumHomeActivity.this, "没有播放队列", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    // 添加进播放队列
                    if (MoreMenuUtils.addSongsToPlayList(new ArrayList<Song>() {{
                        add(clickItem);
                    }})) {
                        Toast.makeText(AlbumHomeActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AlbumHomeActivity.this, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 3:
                    // 添加歌曲到歌单
                    addSongToSonglist(fromViewPositon);
                    break;
                case 4:
                    aboutArtist();
                    break;
                case 5:
                    // 设置为铃声
                    if (MoreMenuUtils.setVoice(AlbumHomeActivity.this, clickItem.getUrl())) {
                        Toast.makeText(AlbumHomeActivity.this, "设置铃声成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AlbumHomeActivity.this, "设置铃声失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 6:
                    if (!clickItem.equals(PlayingService.playingSong)) {
                        int deleteCount = MoreMenuUtils.deleteMp3(AlbumHomeActivity.this, new ArrayList<Song>() {{
                            add(clickItem);
                        }});
                        if (deleteCount != 0) {
                            // 刷新列表
                            songs.remove(clickItem);
                            mAdapter.notifyDataSetChanged();
                            // 刷新头部信息
                            tv_album_home_song_count.setText(songs.size() + "首歌曲");
                            long timeCount = 0;
                            for (int i = 0; i < songs.size(); i++) {
                                timeCount = songs.get(i).getDuration() + timeCount;
                            }
                            // 转换时间
                            tv_album_home_time_count.setText(MTextUtils.long2Minute(timeCount) + "时长");
                            if (songs.size() == 0) {
                                rv_album_home_songs.setVisibility(View.GONE);
                                findViewById(R.id.vs_not_info).setVisibility(View.VISIBLE);
                            }
                            // 发送广播
                            MoreMenuUtils.deleteIntent(AlbumHomeActivity.this, DeleteInnerUI.OtherUI, new ArrayList<Song>() {{
                                add(clickItem);
                            }});
                            Toast.makeText(AlbumHomeActivity.this, "已删除" + deleteCount + "首歌曲", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AlbumHomeActivity.this, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AlbumHomeActivity.this, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
            // 隐藏菜单
            impl.popupWindowDismiss();
        });
        // 设置菜单按钮的点击事件
        mAdapter.setOnMoreMenuClickListener(impl);

        // 获取数据并设置该页面的数据
        songs = getSendDataAndSetData();
    }

    private void aboutArtist() {
        Song complete = HomeActivity.getSongs().get(HomeActivity.getSongs().indexOf(songs.get(0)));
        MoreMenuUtils.aboutArtist(this, complete.getAlbum().getArtist());
    }

    @SuppressWarnings("serial")
    private void addSongToSonglist(final int fromViewPositon) {
        MoreMenuUtils.addSongToSonglist(this
                , new ArrayList<Song>() {{
                    add((Song) songs.get(fromViewPositon));
                }});
    }

    /**
     * 获取数据并设置数据
     *
     * @return 返回专辑的歌曲集合
     */
    public List<Song> getSendDataAndSetData() {
        List<Song> songs = new ArrayList<>();
        Album album = getIntent().getParcelableExtra("albumItem");
        if (album != null) {
            songs = album.getSongs();
            Bitmap cover = ImageUtils.getArtwork(this, songs.get(0).getTitle(),
                    songs.get(0).getSongId(), album.getAlbumId(), true);
            // 设置标题封面的数据
            iv_album_home_cover.setImageBitmap(cover);
            tv_album_home_title.setText(album.getAlbumName());
            tv_album_home_song_count.setText(album.getSongs().size() + "首歌曲");
            long timeCount = 0;
            for (int i = 0; i < songs.size(); i++) {
                timeCount = songs.get(i).getDuration() + timeCount;
            }
            // 转换时间
            tv_album_home_time_count.setText(MTextUtils.long2Minute(timeCount) + "时长");
            setActionBar(album, cover);
        }
        return songs;
    }

    /**
     * 设置actionbar
     *
     * @param album
     * @param cover
     */
    private void setActionBar(Album album, Bitmap cover) {
        String singerName = getIntent().getStringExtra("artistAsAlbum");
        bar_home.setTitle(singerName);
        setTollBarColor(cover);
        setSupportActionBar(bar_home);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @SuppressWarnings("deprecation")
    private void setTollBarColor(final Bitmap bitmap) {
        // Palette的部分
        Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
            /**
             * 提取完之后的回调方法
             */
            @Override
            public void onGenerated(Palette palette) {
                Palette.Swatch vibrant = palette.getVibrantSwatch();
                if (vibrant == null) {
                    // 分析不出颜色,默认颜色
                    themeRgb = getResources().getColor(R.color.titleBackground);
                } else {
                    themeRgb = vibrant.getRgb();
                }
                bar_home.setBackgroundColor(themeRgb);
                getWindow().setStatusBarColor(ImageUtils.colorBurn(themeRgb));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        // 获取search View
        SearchView action_search = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        action_search.setQueryHint(getString(R.string.action_search_part_hint));
        action_search.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String key) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String key) {
                if (key != null && key.length() > 0) {
                    if (songs.size() != 0) {
                        for (int i = 0; i < songs.size(); i++) {
                            Song item = songs.get(i);
                            if (item.getTitle().toLowerCase(Locale.ENGLISH)
                                    .contains(key.toLowerCase(Locale.ENGLISH))) {
                                searchKey = key;
                                mAdapter.notifyItemChanged(i);
                                rv_album_home_songs.scrollToPosition(i);
                                break;
                            }
                        }
                    }
                } else {
                    searchKey = "";
                    mAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_play:
                MoreMenuUtils.playSongIntent(this, (ArrayList<Song>) songs, songs.get(0));
                break;
            case R.id.action_random_album:
                getSharedPreferences("playconfig", Context.MODE_PRIVATE).edit().putInt(
                        "play_method_id", PlayMusicActivity.PLAY_METHOD_RANDOM).apply();
                MoreMenuUtils.playSongIntent(this, (ArrayList<Song>) songs, songs.get(0));
                break;
            case R.id.action_add_list:
                if (MoreMenuUtils.addSongsToPlayList(songs)) {
                    Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_add_songlist:
                MoreMenuUtils.addSongToSonglist(this, (ArrayList<Song>) songs);
                break;
            case R.id.action_about_artist:
                // 歌手作品
                aboutArtist();
                break;
            case R.id.action_delete:
                if (!songs.contains(PlayingService.playingSong)) {
                    int deleteCount = MoreMenuUtils.deleteMp3(this, (ArrayList<Song>) songs);
                    if (deleteCount != 0) {
                        // 发送广播
                        MoreMenuUtils.deleteIntent(this, DeleteInnerUI.OtherUI, (ArrayList<Song>) songs);
                        Toast.makeText(this, "已删除" + deleteCount + "首歌曲", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class BaseAdapter extends
            RecyclerView.Adapter<BaseAdapter.BaseHolder> {
        // 点击事件监听器接口
        private OnBaseClickListener mOnItemClickListener = null;
        private OnBaseClickListener mOnMoreMenuClickListener = null;

        // 对外提供的设置监听器方法
        public void setOnItemClickListener(OnBaseClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        // 对外提供的设置监听器方法
        public void setOnMoreMenuClickListener(
                OnBaseClickListener listener) {
            this.mOnMoreMenuClickListener = listener;
        }

        public class BaseHolder extends RecyclerView.ViewHolder {
            public View itemView;
            public TextView tv_music_name, tv_music_time;
            private ImageView iv_more;

            public BaseHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                tv_music_name = (TextView) itemView.findViewById(R.id.tv_music_name);
                tv_music_time = (TextView) itemView.findViewById(R.id.tv_music_time);
                iv_more = (ImageView) itemView.findViewById(R.id.iv_more);
            }

            public View getItemView() {
                return itemView;
            }
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        @Override
        public void onBindViewHolder(BaseHolder holder, final int pos) {
            Song item = songs.get(pos);
            holder.tv_music_name.setText(MTextUtils.setTextColorByKey(
                    item.getTitle(), searchKey, themeRgb));
            holder.tv_music_time.setText(MTextUtils.long2Minute(item.getDuration()));
            holder.getItemView().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onClick(v, pos);
                    }
                }
            });
            holder.iv_more.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnMoreMenuClickListener != null) {
                        mOnMoreMenuClickListener.onClick(v, pos);
                    }
                }
            });
        }

        @Override
        public BaseHolder onCreateViewHolder(ViewGroup parent, int type) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.album_home_item, parent, false);
            return new BaseHolder(itemView);
        }
    }
}
