package com.toly1994.tolymusic.four.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.MusicApplication;
import com.toly1994.tolymusic.app.db.SongListDBAdapter;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.domain.SongList;
import com.toly1994.tolymusic.app.utils.DensityUtils;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MTextUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.fragment.SongListFragment;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.OnMoreMenuItemClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SongListHomeActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TextView tv_song_count, tv_time_count;
    private ImageView iv_cover;
    private RecyclerView rv_songlist_home_songs;
    private RelativeLayout rl_message;
    private BaseMoreMenuClickListenerImpl impl;
    private String searchKey = ""; // 搜索关键字
    private List<Song> songsAsList;
    private SongsAdapter songAdapter;
    private SongList songList;
    private int themeRgb = Color.parseColor("#76C2AF");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songlist_home);
        setViewComponent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MusicApplication.launchActivity = SongListHomeActivity.class;
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
        tv_song_count = (TextView) findViewById(R.id.tv_song_count);
        tv_time_count = (TextView) findViewById(R.id.tv_time_count);
        iv_cover = (ImageView) findViewById(R.id.iv_cover);
        rv_songlist_home_songs = (RecyclerView) findViewById(R.id.rv_songlist_home_songs);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        rl_message = (RelativeLayout) findViewById(R.id.rl_message);
        // 获取数据
        getSendDataAndSetData();
        // 设置数据
        songAdapter = new SongsAdapter();
        rv_songlist_home_songs.setLayoutManager(new LinearLayoutManager(this));
        rv_songlist_home_songs.setAdapter(songAdapter);

        songAdapter.setOnItemClickListener(new OnBaseClickListener() {
            @Override
            public void onClick(View view, int position) {
                MoreMenuUtils.playSongIntent(SongListHomeActivity.this, (ArrayList<Song>) songsAsList, songsAsList.get(position));
            }
        });

        // 获取提供菜单选项的内容适配器
        MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(this, MoreMenuUtils.moreMenusAsSong);
        // 把内容给点击事件的实现类,完成点击跳出对应菜单
        impl = new BaseMoreMenuClickListenerImpl(this, mmAdapter);
        // 给菜单内的选项添加点击事件
        impl.setOnItemClickListener(new OnMoreMenuItemClickListener() {
            @SuppressWarnings("serial")
            @Override
            public void onClick(View fromView, View clickView, final int fromViewPositon,
                                int clickViewPosition) {
                final Song clickItem = songsAsList.get(fromViewPositon);
                // 处理菜单点击选项
                switch (clickViewPosition) {
                    case 0:
                        MoreMenuUtils.playSongIntent(SongListHomeActivity.this, (ArrayList<Song>) songsAsList, clickItem);
                        break;
                    case 1:
                        // 下一首播放
                        if (PlayingService.playList != null && PlayingService.playingSong != null) {
                            MoreMenuUtils.swapMusicUnderPlayingSong(clickItem);
                            Toast.makeText(SongListHomeActivity.this, "播放队列已更新", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SongListHomeActivity.this, "没有播放队列", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        // 添加进播放队列
                        if (MoreMenuUtils.addSongsToPlayList(new ArrayList<Song>() {{
                            add(clickItem);
                        }})) {
                            Toast.makeText(SongListHomeActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SongListHomeActivity.this, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3:
                        // 添加歌曲到歌单
                        MoreMenuUtils.addSongToSonglist(SongListHomeActivity.this, new ArrayList<Song>() {{
                            add((Song) songsAsList.get(fromViewPositon));
                        }});
                        break;
                    case 4:
                        Song complete = HomeActivity.getSongs().get(HomeActivity.getSongs().indexOf(clickItem));
                        MoreMenuUtils.aboutArtist(SongListHomeActivity.this, complete.getAlbum().getArtist());
                        break;
                    case 5:
                        // 设置为铃声
                        if (MoreMenuUtils.setVoice(SongListHomeActivity.this, clickItem.getUrl())) {
                            Toast.makeText(SongListHomeActivity.this, "设置铃声成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SongListHomeActivity.this, "设置铃声失败", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 6:
                        // 删除播放列表内的歌曲
                        SongListDBAdapter db = new SongListDBAdapter(SongListHomeActivity.this).open();
                        long songListID = db.getSonglistID4Name(songList.getListName());
                        boolean result = db.deleteItem(SongListDBAdapter.DATABASE_TABLE_SONG,
                                SongListDBAdapter.KEY_FROM_LIST + "=" + songListID + " and " +
                                        SongListDBAdapter.KEY_SONG_ONLY_ID + "=" + clickItem.getSongId() + " and " +
                                        SongListDBAdapter.KEY_SONG_NAME + "= '" + clickItem.getTitle() + "'");
                        db.close();
                        if (result) {
                            // 刷新列表
                            songsAsList.remove(clickItem);
                            songAdapter.notifyDataSetChanged();
                            // 刷新头部信息
                            tv_song_count.setText(songsAsList.size() + "首歌曲");
                            long timeCount = 0;
                            for (int i = 0; i < songsAsList.size(); i++) {
                                timeCount = songsAsList.get(i).getDuration() + timeCount;
                            }
                            // 转换时间
                            tv_time_count.setText(MTextUtils.long2Minute(timeCount) + "时长");
                            // 发送广播
                            Intent send = new Intent(SongListFragment.SONGLIST_CHANGE_INTENT);
                            send.putExtra("actiontype", SongListFragment.SONGLIST_DELETE);
                            send.putExtra("position", getIntent().getIntExtra("position", -1));
                            send.putExtra("deleteItem", clickItem);
                            sendBroadcast(send);
                            Toast.makeText(SongListHomeActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SongListHomeActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                // 隐藏菜单
                impl.popupWindowDismiss();
            }
        });
        // 设置菜单按钮的点击事件
        songAdapter.setOnMoreMenuClickListener(impl);
    }

    /**
     * 获取数据并设置数据
     *
     * @return 返回专辑的歌曲集合
     */
    @SuppressWarnings("deprecation")
    private void getSendDataAndSetData() {
        songsAsList = new ArrayList<>();
        songList = getIntent().getParcelableExtra("songlistItem");
        Bitmap cover = null;
        long timeCount = 0;
        if (songList != null) {
            // 传递的Song对象没有包含album对象(占用内存大),根据Song的title找寻出完整的Song实例
            List<Song> temp = songList.getSongs();
            if (temp != null && temp.size() != 0) {
                List<Song> tempAll = HomeActivity.getSongs();
                for (int i = 0; i < temp.size(); i++) {
                    Song item = temp.get(i);
                    if (tempAll.contains(item)) {
                        item = tempAll.get(tempAll.indexOf(item));
                        songsAsList.add(item);
                    }
                }
                cover = ImageUtils.getArtwork(this, songsAsList.get(0)
                                .getTitle(), songsAsList.get(0).getSongId(),
                        songsAsList.get(0).getAlbum().getAlbumId(), true);
            } else {
                cover = BitmapFactory.decodeResource(getResources(),
                        R.drawable.default_music_icon);
            }
            // 获取屏幕宽度,设置控件位置
            int windowWidth = getWindowManager().getDefaultDisplay().getWidth();
            iv_cover.setLayoutParams(new RelativeLayout.LayoutParams(
                    windowWidth, windowWidth));
            RelativeLayout.LayoutParams marinParams = new RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, DensityUtils.dp2px(this, 30));
            marinParams.setMargins(0, windowWidth - windowWidth / 4, 0, 0);
            rl_message.setLayoutParams(marinParams);
            // 设置标题封面的数据
            iv_cover.setImageBitmap(cover);
            tv_song_count.setText(temp.size() + "首歌曲");
            for (int i = 0; i < songsAsList.size(); i++) {
                timeCount = songsAsList.get(i).getDuration() + timeCount;
            }
            // 转换时间
            tv_time_count.setText(MTextUtils.long2Minute(timeCount) + "时长");
            setActionBar(songList);
        }
    }

    /**
     * 设置actionbar
     *
     * @param songList
     */
    private void setActionBar(SongList songList) {
        toolbar.setTitle(songList.getListName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * 删除歌单
     */
    private boolean deleteSonglist() {
        SongListDBAdapter db = new SongListDBAdapter(this).open();
        long songListID = db.getSonglistID4Name(songList.getListName());
        // 删除数据库内的播放列表
        boolean result = db.deleteItem(SongListDBAdapter.DATABASE_TABLE_LIST,
                SongListDBAdapter.KEY_LIST_NAME + "='" + songList.getListName() + "'");
        if (result) {
            // 删除数据库内的播放列表关联的音乐
            db.deleteItem(SongListDBAdapter.DATABASE_TABLE_SONG,
                    SongListDBAdapter.KEY_FROM_LIST + "=" + songListID);
        }
        db.close();
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.songlist_menu, menu);
        // 获取search View
        SearchView action_search = (SearchView) menu.findItem(
                R.id.action_search).getActionView();
        action_search.setQueryHint(getString(R.string.action_search_part_hint));
        action_search.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String key) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String key) {
                if (key != null && key.length() > 0) {
                    if (songsAsList.size() != 0) {
                        for (int i = 0; i < songsAsList.size(); i++) {
                            Song item = songsAsList.get(i);
                            if (item.getTitle().toLowerCase(Locale.ENGLISH)
                                    .contains(key.toLowerCase(Locale.ENGLISH))) {
                                searchKey = key;
                                songAdapter.notifyItemChanged(i);
                                rv_songlist_home_songs.scrollToPosition(i);
                                break;
                            }
                        }
                    }
                } else {
                    searchKey = "";
                    songAdapter.notifyDataSetChanged();
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
                MoreMenuUtils.playSongIntent(this, (ArrayList<Song>) songsAsList, songsAsList.get(0));
                break;
            case R.id.action_random_all:
                getSharedPreferences("playconfig", Context.MODE_PRIVATE).edit().putInt(
                        "play_method_id", PlayMusicActivity.PLAY_METHOD_RANDOM).apply();
                MoreMenuUtils.playSongIntent(this, (ArrayList<Song>) songsAsList, songsAsList.get(0));
                break;
            case R.id.action_add_list:
                if (MoreMenuUtils.addSongsToPlayList(songsAsList)) {
                    Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "播放队列已存在该歌单歌曲", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_delete:
                // 删除播放列表
                if (deleteSonglist()) {
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                    Intent response = new Intent();
                    response.putExtra("position", getIntent().getIntExtra("position", -1));
                    setResult(RESULT_OK, response);
                    finish();
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class SongsAdapter extends
            RecyclerView.Adapter<SongsAdapter.SongsHolder> {
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

        class SongsHolder extends RecyclerView.ViewHolder {
            private View itemView, view_line;
            private TextView tv_music_name, tv_music_albums;
            private ImageView iv_music_icon, iv_more;

            public SongsHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                tv_music_name = (TextView) itemView
                        .findViewById(R.id.tv_music_name);
                tv_music_albums = (TextView) itemView
                        .findViewById(R.id.tv_music_albums);
                iv_music_icon = (ImageView) itemView
                        .findViewById(R.id.iv_music_icon);
                iv_more = (ImageView) itemView.findViewById(R.id.iv_more);
                view_line = (View) itemView.findViewById(R.id.view_line);
            }

            public View getItemView() {
                return itemView;
            }

        }

        @Override
        public int getItemCount() {
            return songsAsList.size();
        }

        @Override
        public void onBindViewHolder(SongsHolder holder, final int pos) {
            Song item = songsAsList.get(pos);
            Bitmap cover = item.getAlbum().getCover();
            if (cover == null) {
                cover = ImageUtils.getArtwork(SongListHomeActivity.this, item.getTitle(),
                        item.getSongId(), item.getAlbum().getAlbumId(), true);
                item.getAlbum().setCover(cover);
            }
            holder.tv_music_name.setText(MTextUtils.setTextColorByKey(
                    item.getTitle(), searchKey, themeRgb));
            holder.iv_music_icon.setImageBitmap(cover);
            holder.tv_music_albums.setText(item.getAlbum().getAlbumName());

            if (pos == songsAsList.size() - 1) {
                holder.view_line.setVisibility(View.INVISIBLE);
            }

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
        public SongsHolder onCreateViewHolder(ViewGroup parent, int type) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.music_item, parent, false);
            return new SongsHolder(rootView);
        }
    }
}
