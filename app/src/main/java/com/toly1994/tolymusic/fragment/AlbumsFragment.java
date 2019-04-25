package com.toly1994.tolymusic.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.toly1994.tolymusic.four.activity.AlbumHomeActivity;
import com.toly1994.tolymusic.four.activity.HomeActivity;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.DeleteInnerUI;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.OnMoreMenuItemClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.app.utils.SystemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 专辑fragment
 *
 * @author lbRoNG
 */
public class AlbumsFragment extends Fragment {
    private static List<Album> albums = new ArrayList<>();// 专辑列表

    private View view;
    private RecyclerView rv_albums;
    private AlbumAdapter albumAdapter;
    private AlbumsSearchFinishReceiver mReceiver;
    private BaseMoreMenuClickListenerImpl impl;
    private ExecutorService pool;
    private static int COVER_LAST_UPDATE_POSITION = 5;

    public static List<Album> getAlbumsList() {
        return albums;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mReceiver == null) {
            mReceiver = new AlbumsSearchFinishReceiver();
            getActivity().registerReceiver(mReceiver,
                    new IntentFilter(HomeActivity.LOADER_SEARCH_FINISH));
            System.out.println("ALBUM_RES");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_albums, null);
        }
        setViewComponent();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 初始化线程池
        pool = Executors.newCachedThreadPool();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP
                    && keyCode == KeyEvent.KEYCODE_BACK) {
                if (impl != null && impl.popupWindowIsShow()) {
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
        super.onPause();
        pool.shutdownNow();
    }

    @Override
    public void onDestroy() {
        // 反注册广播
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private void setViewComponent() {
        rv_albums = view.findViewById(R.id.rv_albums);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                2, StaggeredGridLayoutManager.VERTICAL);
        rv_albums.setLayoutManager(layoutManager);
        albumAdapter = new AlbumAdapter();
        rv_albums.setAdapter(albumAdapter);

        albumAdapter.setOnItemClickListener(new OnBaseClickListener() {
            @Override
            public void onClick(View view, int position) {
                Album item = albums.get(position);
                Intent intent = new Intent(getActivity(),
                        AlbumHomeActivity.class);
                // Album实现的Parcelable没有写入Artist对象(占内存),所以单独传入歌手
                intent.putExtra("albumItem", item);
                intent.putExtra("artistAsAlbum", item.getArtist()
                        .getSingerName());
                getActivity().startActivity(intent);
            }
        });

        // 设置滚动监听,当界面停留的时候再加载屏幕内条目的图片
        rv_albums.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView,
                                             int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int[] firstViewble = new int[2];
                    int[] lastViewble = new int[2];
                    // 获取第一个显示的位置
                    firstViewble = layoutManager.findFirstVisibleItemPositions(firstViewble);
                    // 获取最后一个显示的位置
                    lastViewble = layoutManager.findLastVisibleItemPositions(lastViewble);
                    for (int position = firstViewble[0]; position <= lastViewble[1]; position++) {
                        if (!(albums.size() == 0)) {
                            setAlbumsCover(position);
                        }
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // 获取最后一个显示的位置
                int[] lastViewble = new int[2];
                lastViewble = layoutManager.findLastVisibleItemPositions(lastViewble);
                if (lastViewble[0] > COVER_LAST_UPDATE_POSITION) {
                    // 滑动速度在指定速度以下再加载图片,滑动速度快代表用户只是在找指定的歌,不必加载图片
                    if (Math.abs(dy) < 100 && Math.abs(dy) > 5) {
                        setAlbumsCover(lastViewble[0]);
                        setAlbumsCover(lastViewble[1]);
                        COVER_LAST_UPDATE_POSITION = lastViewble[1];
                    }
                }
            }
        });

        // 设置每个item的更多菜单点击
        // 获取提供菜单选项的内容适配器
        MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(getActivity(), MoreMenuUtils.moreMenusAsAlbum);
        // 把内容给点击事件的实现类,完成点击跳出对应菜单
        impl = new BaseMoreMenuClickListenerImpl(getActivity(), mmAdapter);
        // 给菜单内的选项添加点击事件
        impl.setOnItemClickListener(new OnMoreMenuItemClickListener() {
            @Override
            public void onClick(View fromView, View clickView, int fromViewPositon,
                                int clickViewPosition) {
                Album item = albums.get(fromViewPositon);
                ArrayList<Song> albumSongs = item.getSongs();
                switch (clickViewPosition) {
                    case 0:
                        // 播放
                        MoreMenuUtils.playSongIntent(getActivity(), albumSongs, albumSongs.get(0));
                        break;
                    case 1:
                        // 添加到播放队列
                        if (MoreMenuUtils.addSongsToPlayList(albumSongs)) {
                            Toast.makeText(getActivity(), "添加成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        // 添加专辑的全部歌曲到歌单
                        MoreMenuUtils.addSongToSonglist(getActivity(), albumSongs);
                        break;
                    case 3:
                        // 歌手作品
                        MoreMenuUtils.aboutArtist(getActivity(), item.getArtist());
                        break;
                    case 4:
                        // 删除
                        if (!albumSongs.contains(PlayingService.playingSong)) {
                            int deleteCount = MoreMenuUtils.deleteMp3(getActivity(), albumSongs);
                            if (deleteCount != 0) {
                                // 删除集合刷新列表
                                albums.remove(fromViewPositon);
                                albumAdapter.notifyItemRemoved(fromViewPositon);
                                albumAdapter.notifyItemRangeChanged(fromViewPositon, albumAdapter.getItemCount());
                                // 发送广播
                                MoreMenuUtils.deleteIntent(getActivity(), DeleteInnerUI.AlbumsFragment, albumSongs);
                                Toast.makeText(getActivity(), "已删除" + deleteCount + "首歌曲", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "未知错误,删除失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                impl.popupWindowDismiss();
            }
        });
        albumAdapter.setOnMoreMenuClickListener(impl);

    }

    // 该界面正处于用户可见状态
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    /**
     * 异步更新专辑封面
     *
     * @param position
     */
    private void setAlbumsCover(final int position) {
        final Album item = albums.get(position);
        final int rgb = Color.parseColor("#4BAD97");
        if (item.getCover() == null || item.getCoverRgb() == rgb) {
            pool.submit(new Runnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    Bitmap cover = null;
                    // 刷新封面
                    if (item.getCover() == null) {
                        cover = ImageUtils.getArtwork(getActivity(), item
                                        .getSongs().get(0).getTitle(),
                                item.getSongs().get(0).getSongId(),
                                item.getAlbumId(), true);
                        if (cover == null) {
                            cover = BitmapFactory.decodeResource(
                                    getResources(), R.drawable.default_music_icon);
                        }
                        item.setCover(cover);
                    }
                    // 刷新背景
                    if (item.getCoverRgb() == rgb) {
                        Palette palette = Palette.generate(item.getCover());
                        Palette.Swatch vibrant = palette.getVibrantSwatch();
                        if (vibrant != null) {
                            item.setCoverRgb(vibrant.getRgb());
                        } else {
                            item.setCoverRgb(getResources().getColor(R.color.titleBackground));
                        }
                    }
                    getActivity().runOnUiThread(() -> albumAdapter.notifyDataSetChanged());
                }
            });
        }
    }

    /**
     * 本地广播接收者,接受音乐搜索完成的个广播
     *
     * @author lbRoNG
     */
    private class AlbumsSearchFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(HomeActivity.LOADER_SEARCH_FINISH)
                    && !DeleteInnerUI.AlbumsFragment.equals(((DeleteInnerUI) intent.getSerializableExtra("innerUI")))) {
                // 清除旧数据
                albums.clear();
                final List<Song> mSongs = HomeActivity.getSongs();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (Song item : mSongs) {
                            // 专辑分类
                            Album album = item.getAlbum();
                            if (albums.contains(album)) {
                                int index = albums.indexOf(album);
                                album = albums.get(index);
                            } else {
                                albums.add(album);
                            }
                            // 添加歌曲到专辑列表里
                            album.getSongs().add(item);
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                albumAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();
            }
        }
    }

    private class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumHolder> {
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

        public class AlbumHolder extends RecyclerView.ViewHolder {
            private ImageView iv_album, iv_more;
            private TextView tv_album_name, tv_singer_name;
            private RelativeLayout rl_content_text;

            public AlbumHolder(View itemView) {
                super(itemView);

                tv_album_name = itemView.findViewById(R.id.tv_album_name);
                tv_singer_name = itemView.findViewById(R.id.tv_singer_name);
                iv_album = itemView.findViewById(R.id.iv_album);
                iv_more = itemView.findViewById(R.id.iv_more);
                rl_content_text = itemView.findViewById(R.id.rl_content_text);
            }

        }

        @Override
        public int getItemCount() {
            return albums.size();
        }

        @Override
        public void onBindViewHolder(AlbumAdapter.AlbumHolder holder, final int pos) {
            if (pos < 6) {
                setAlbumsCover(pos);
            }
            Album item = albums.get(pos);
            holder.tv_album_name.setText(item.getAlbumName());
            holder.tv_singer_name.setText(item.getArtist().getSingerName());
            holder.iv_album.setImageBitmap(item.getCover());
            holder.rl_content_text.setBackgroundColor(item.getCoverRgb());

            holder.itemView.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, pos);
                }
            });

            holder.iv_more.setOnClickListener(v -> {
                if (mOnMoreMenuClickListener != null) {
                    mOnMoreMenuClickListener.onClick(v, pos);
                }
            });
        }

        @Override
        public AlbumAdapter.AlbumHolder onCreateViewHolder(
                ViewGroup parent, int type) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.album_item, parent, false);
            return new AlbumHolder(itemView);
        }
    }
}