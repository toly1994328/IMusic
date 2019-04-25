package com.toly1994.tolymusic.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.DeleteInnerUI;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.app.utils.SongsSortByLettersUtils;
import com.toly1994.tolymusic.app.utils.SystemUtils;
import com.toly1994.tolymusic.four.activity.HomeActivity;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.OnMoreMenuItemClickListener;
import com.toly1994.tolymusic.itf.SortByLetterString;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 歌曲fragment
 *
 * @author lbRoNG
 */
public class SongsFragment extends Fragment {
    private final static int LETTER_VIEW = 1;    // Recyclerview样式标识
    private final static int SONG_VIEW = 2;
    private Song playingSong;     // 正在播放的音�?
    private ExecutorService pool; // 线程�?
    private View view;
    private RecyclerView rv_songs;
    private SongsAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SongsSearchFinishReceiver mReceiver;
    private OperateFinishReceiver finishReceiver;
    private BaseMoreMenuClickListenerImpl impl;
    private static List<Object> songsAsSort = new ArrayList<>();
    public static List<Song> songsAstSortAndNotLetter = new ArrayList<>();
    //更新封面的item位置
    private static int COVER_LAST_UPDATE_POSITION = 9;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mReceiver == null) {
            mReceiver = new SongsSearchFinishReceiver();
            getActivity().registerReceiver(mReceiver,
                    new IntentFilter(HomeActivity.LOADER_SEARCH_FINISH));
            System.out.println("SONGS_RES");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_songs, null);
        }
        setViewComponent();
        return view;
    }

    @Override
    public void onStart() {
        // 更新正在播放的音乐
        updatePlayingFlagAsList();
        // 注册服务对音乐的操作完成的广播
        if (finishReceiver == null) {
            finishReceiver = new OperateFinishReceiver();
            getActivity().registerReceiver(finishReceiver,
                    new IntentFilter(PlayingService.OPERATE_FINISH));
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 初始化线程池
        pool = Executors.newCachedThreadPool();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(
                (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                if (impl.popupWindowIsShow()) {
                    impl.popupWindowDismiss();
                } else if (MoreMenuUtils.chooseSonglistPopupWindow != null
                        && MoreMenuUtils.chooseSonglistPopupWindow.isShowing()) {
                    MoreMenuUtils.chooseSonglistPopupWindow.dismiss();
                } else {
                    SystemUtils.finishAsDouble(getActivity());
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onPause() {
        // 关闭线程
        pool.shutdownNow();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (finishReceiver != null) {
            getActivity().unregisterReceiver(finishReceiver);
            finishReceiver = null;
        }
        // 反注册广播
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }

    // 该界面正处于用户可见状态
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    @SuppressWarnings("deprecation")
    private void setViewComponent() {
        // 获取配置文件,找到上一次最后播放的歌曲
        playingSong = HomeActivity.playingSong;
        // 拿到RecyclerView
        rv_songs = (RecyclerView) view.findViewById(R.id.rv_songs);
        // 设置LinearLayoutManager
        mLayoutManager = new LinearLayoutManager(getActivity());
        rv_songs.setLayoutManager(mLayoutManager);
        // 设置ItemAnimator
        rv_songs.setItemAnimator(new DefaultItemAnimator());
        // 设置固定大小
        rv_songs.setHasFixedSize(true);
        // 初始化自定义的适配器
        mAdapter = new SongsAdapter();
        rv_songs.setAdapter(mAdapter);
        // 设置点击监听
        mAdapter.setOnItemClickListener(new OnBaseClickListener() {
            // 点击事件的事件
            @Override
            public void onClick(View v, int position) {
                if (v instanceof RelativeLayout) {
                    clickToPlayMusic(position);
                } else if (v instanceof LinearLayout) {
                }
            }
        });

        // 设置每个item的更多菜单点�?
        // 获取提供菜单选项的内容�?配器
        MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(getActivity(), MoreMenuUtils.moreMenusAsSong);
        // 把内容给点击事件的实现类,完成点击跳出对应菜单
        impl = new BaseMoreMenuClickListenerImpl(getActivity(), mmAdapter);
        // 给菜单内的�?项添加点击事�?
        impl.setOnItemClickListener(new OnMoreMenuItemClickListener() {
            @SuppressWarnings("serial")
            @Override
            public void onClick(View fromView, View clickView, final int fromViewPositon,
                                int clickViewPosition) {
                final Song clickItem = (Song) songsAsSort.get(fromViewPositon);
                // 处理菜单点击选项
                switch (clickViewPosition) {
                    case 0:
                        // 播放
                        clickToPlayMusic(fromViewPositon);
                        break;
                    case 1:
                        // 下一首播�?
                        if (PlayingService.playList != null && PlayingService.playingSong != null) {
                            MoreMenuUtils.swapMusicUnderPlayingSong(clickItem);
                            Toast.makeText(getActivity(), "播放队列已更新", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "没有播放队列", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        // 添加进播放队�?
                        if (MoreMenuUtils.addSongsToPlayList(new ArrayList<Song>() {{
                            add(clickItem);
                        }})) {
                            Toast.makeText(getActivity(), "添加成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3:
                        // 添加歌曲到歌�?
                        MoreMenuUtils.addSongToSonglist(getActivity(), new ArrayList<Song>() {
                            {
                                add(clickItem);
                            }
                        });
                        break;
                    case 4:
                        // 歌手的作�?
                        Artist item = clickItem.getAlbum().getArtist();
                        MoreMenuUtils.aboutArtist(getActivity(), item);
                        break;
                    case 5:
                        // 设置为铃�?
                        if (MoreMenuUtils.setVoice(getActivity(), clickItem.getUrl())) {
                            Toast.makeText(getActivity(), "设置铃声成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "设置铃声失败", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 6:
                        // 如果删除歌曲是当前播放的歌曲,提示用户
                        if (!clickItem.equals(PlayingService.playingSong)) {
                            // 删除歌曲
                            int deleteCount = MoreMenuUtils.deleteMp3(getActivity(), new ArrayList<Song>() {{
                                add(clickItem);
                            }});
                            if (deleteCount != 0) {
                                // 刷新列表
                                MoreMenuUtils.deleteArtistAndRefresh(mAdapter, songsAsSort, clickItem, fromViewPositon);
                                songsAstSortAndNotLetter.remove(clickItem);
                                // 发�?广播通知刷新列表,删除有关该歌曲的全部信息
                                MoreMenuUtils.deleteIntent(getActivity(), DeleteInnerUI.SongsFragment,
                                        new ArrayList<Song>() {{
                                            add(clickItem);
                                        }});
                                Toast.makeText(getActivity(), "已删除" + deleteCount + "首歌曲", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "未知错误,删除失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                // 隐藏菜单
                impl.popupWindowDismiss();
            }
        });
        // 设置菜单按钮的点击事�?
        mAdapter.setOnMoreMenuClickListener(impl);

        // 设置滚动监听,当界面停留的时�?再加载屏幕内条目的图�?
        rv_songs.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView,
                                             int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 获取第一个显示的位置
                    int firstVisibleItem = ((LinearLayoutManager) mLayoutManager)
                            .findFirstVisibleItemPosition();
                    // 获取�?���?��显示的位�?
                    int lastVisibleItem = ((LinearLayoutManager) mLayoutManager)
                            .findLastVisibleItemPosition();
                    for (int position = firstVisibleItem; position <= lastVisibleItem; position++) {
                        if (!(songsAsSort.size() == 0)) {
                            setMusicCover(position);
                        }
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // 获取�?���?��显示的位�?
                int lastVisibleItem = ((LinearLayoutManager) mLayoutManager)
                        .findLastVisibleItemPosition();
                if (lastVisibleItem > COVER_LAST_UPDATE_POSITION) {
                    // 滑动速度在指定�?度以下再加载图片,滑动速度快代表用户只是在找指定的�?不必加载图片
                    if (Math.abs(dy) < 100 && Math.abs(dy) > 5) {
                        setMusicCover(lastVisibleItem);
                        COVER_LAST_UPDATE_POSITION = lastVisibleItem;
                    }
                }
            }
        });
    }

    /**
     * 点击item播放歌曲
     *
     * @param position
     */
    private void clickToPlayMusic(int position) {
        Song temp = (Song) songsAsSort.get(position);
        // 判断是否为同�?��点击,不是第一次点�?只有点击不是正在播放的歌曲才执行相关操作
        if (!temp.equals(playingSong)) {
            if (playingSong != null) {
                // 取消旧的播放状�?标记
                mAdapter.notifyItemChanged(songsAsSort.indexOf(playingSong));
            }
            // 标记当前歌曲为播放状�?
            playingSong = temp;
            // 播放歌曲
            MoreMenuUtils.playSongIntent(getActivity(), (ArrayList<Song>) songsAstSortAndNotLetter, playingSong);
        }
        // 更新新的播放状�?标记
        mAdapter.notifyItemChanged(position);
    }

    /**
     * 更新列表中正在播放的音乐标记
     */
    private void updatePlayingFlagAsList() {
        // 更新正在播放的歌�?
        playingSong = PlayingService.playingSong;
        // 刷新列表内正在播放的歌曲标记
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 设置封面
     *
     * @param position
     */
    private void setMusicCover(final int position) {
        if (mAdapter.getItemViewType(position) == SONG_VIEW) {
            final Song songItem = (Song) songsAsSort.get(position);
            if (songItem.getAlbum().getCover() == null) {
                // 提交任务给线程池处理
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap cover = ImageUtils.getArtwork(getActivity(),
                                songItem.getTitle(), songItem.getSongId(),
                                songItem.getAlbum().getAlbumId(), true);
                        if (cover != null) {
                            songItem.getAlbum().setCover(cover);
                        } else {
                            songItem.getAlbum().setCover(
                                    BitmapFactory.decodeResource(
                                            getResources(),
                                            R.drawable.default_music_icon));
                        }
                        getActivity().runOnUiThread(() -> mAdapter.notifyItemChanged(position));
                    }
                });
            }
        }
    }

    /**
     * 歌曲操作完毕的广播监�?
     *
     * @author lbRoNG
     */
    private class OperateFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            playingSong = PlayingService.playingSong;
            String intent_type = intent.getStringExtra("intent_type");
            // 服务内对歌曲的操作完�?
            if (PlayingService.INTENT_NEXT_MUSIC.equals(intent_type)) {
                updatePlayingFlagAsList();
            } else if (PlayingService.CLEAN_PLAYLSIT.equals(intent_type)
                    || PlayingService.INTENT_STOP_MUSIC.equals(intent_type)) {
                // 清除播放列表上的正在播放的标�?
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * 本地广播接收�?接受音乐搜索完成的个广播
     *
     * @author lbRoNG
     */
    private class SongsSearchFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(HomeActivity.LOADER_SEARCH_FINISH)
                    && !DeleteInnerUI.SongsFragment.equals(((DeleteInnerUI) intent.getSerializableExtra("innerUI")))) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final List<Song> oldList = HomeActivity.getSongs();
                        songsAsSort = SongsSortByLettersUtils.getInfoByLetter(
                                oldList, new SortByLetterString() {
                                    // 返回排序依据字符�?
                                    @Override
                                    public String sortSign(int position) {
                                        return oldList.get(position)
                                                .getTitle();
                                    }
                                });
                        // 清除旧数�?
                        songsAstSortAndNotLetter.clear();
                        // 整理出不包含字母的排序过的集�?
                        for (int i = 0; i < songsAsSort.size(); i++) {
                            Object obj = songsAsSort.get(i);
                            if (obj instanceof Song) {
                                songsAstSortAndNotLetter.add((Song) obj);
                            }
                        }
                        getActivity().runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                    }
                }).start();
            }
        }
    }

    /**
     * 数据适配�?
     *
     * @author lbRoNG
     */
    private class SongsAdapter extends
            RecyclerView.Adapter<SongsAdapter.ViewHolder> {
        // 点击事件监听器接�?
        private OnBaseClickListener mOnItemClickListener = null;
        private OnBaseClickListener mOnMoreMenuClickListener = null;

        // 对外提供的设置监听器方法
        public void setOnItemClickListener(
                OnBaseClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        // 对外提供的设置监听器方法
        public void setOnMoreMenuClickListener(
                OnBaseClickListener listener) {
            this.mOnMoreMenuClickListener = listener;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private View itemView, view_line;
            private TextView tv_music_name, tv_music_albums, tv_letter;
            private ImageView iv_music_icon, iv_more, iv_playing;

            public ViewHolder(View v, int type) {
                super(v);
                this.itemView = v;
                if (type == LETTER_VIEW) {
                    tv_letter = (TextView) v.findViewById(R.id.tv_letter);
                } else {
                    tv_music_name = (TextView) v.findViewById(R.id.tv_music_name);
                    tv_music_albums = (TextView) v.findViewById(R.id.tv_music_albums);
                    iv_music_icon = (ImageView) v.findViewById(R.id.iv_music_icon);
                    iv_playing = (ImageView) v.findViewById(R.id.iv_playing);
                    iv_more = (ImageView) v.findViewById(R.id.iv_more);
                    view_line = (View) v.findViewById(R.id.view_line);
                }
            }

            public View getItemView() {
                return itemView;
            }
        }

        @Override
        public SongsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
            View view = null;
            if (viewType == LETTER_VIEW) {
                view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.letter_item, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.music_item, parent, false);
            }
            ViewHolder holder = new ViewHolder(view, viewType);
            return holder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            int type = getItemViewType(position);
            if (type == LETTER_VIEW) {
                holder.tv_letter.setText(songsAsSort.get(position) + "");
            } else {
                // 默认加载10首歌曲的封面
                if (position < 10) {
                    setMusicCover(position);
                }
                final Song item = (Song) songsAsSort.get(position);
                holder.tv_music_name.setText(item.getTitle());
                holder.tv_music_albums.setText(item.getAlbum().getArtist()
                        .getSingerName()
                        + " | " + item.getAlbum().getAlbumName());
                holder.iv_music_icon.setImageBitmap(item.getAlbum().getCover());
                // 正在播放的歌曲背景改变
                if (playingSong != null && item.equals(playingSong)) {
                    holder.iv_playing.setVisibility(View.VISIBLE);
                } else {
                    holder.iv_playing.setVisibility(View.GONE);
                }
                holder.iv_more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnMoreMenuClickListener != null) {
                            mOnMoreMenuClickListener.onClick(v, position);
                        }
                    }
                });

                // 分割线是否显示
                if (position + 1 < songsAsSort.size()) {
                    Object obj = songsAsSort.get(position + 1);
                    if (obj instanceof String) {
                        holder.view_line.setVisibility(View.INVISIBLE);
                    } else {
                        holder.view_line.setVisibility(View.VISIBLE);
                    }
                } else if (position + 1 == songsAsSort.size()) {
                    holder.view_line.setVisibility(View.INVISIBLE);
                }

            }
            // 处理点击事件
            holder.getItemView().setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, position);
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            Object obj = songsAsSort.get(position);
            if (obj instanceof String) {
                return LETTER_VIEW;
            } else if (obj instanceof Song) {
                return SONG_VIEW;
            }
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            return songsAsSort.size();
        }
    }
}
