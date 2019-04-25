package com.toly1994.tolymusic.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.domain.*;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.app.utils.SongsSortByLettersUtils;
import com.toly1994.tolymusic.app.utils.SystemUtils;
import com.toly1994.tolymusic.four.activity.ArtistHomeActivity;
import com.toly1994.tolymusic.four.activity.HomeActivity;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArtistFragment extends Fragment {
    public final static int LETTER_VIEW = 0;
    public final static int ARTIST_VIEW = 1;
    private View view;
    private RecyclerView rv_singer;
    private SingerAdapter mAdapter;
    private ArtistSearchFinishReceiver mReceiver;
    private RecyclerView.LayoutManager mLayoutManager;
    private static ArrayList<Object> mSingerBySort = new ArrayList<>();
    private static ArrayList<Artist> mSinger = new ArrayList<>();
    private BaseMoreMenuClickListenerImpl impl;

    public static ArrayList<Artist> getArtistList() {
        return mSinger;
    }

    public static ArrayList<Object> getArtistSortList() {
        return mSingerBySort;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mReceiver == null) {
            mReceiver = new ArtistSearchFinishReceiver();
            getActivity().registerReceiver(mReceiver,
                    new IntentFilter(HomeActivity.LOADER_SEARCH_FINISH));
            System.out.println("ARTIST_RES");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_singer, null);
        }
        setViewComponent();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
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
    public void onDestroy() {
        // 反注册广播
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }

    private void setViewComponent() {
        // 拿到RecyclerView
        rv_singer = (RecyclerView) view.findViewById(R.id.rv_singer);
        // 设置LinearLayoutManager
        mLayoutManager = new LinearLayoutManager(getActivity());
        rv_singer.setLayoutManager(mLayoutManager);
        // 设置ItemAnimator
        rv_singer.setItemAnimator(new DefaultItemAnimator());
        // 设置固定大小
        rv_singer.setHasFixedSize(true);
        // 初始化自定义的适配器
        mAdapter = new SingerAdapter();
        // 为mRecyclerView设置适配器
        rv_singer.setAdapter(mAdapter);
        // 设置点击监听器
        // 点击事件的实现
        mAdapter.setOnItemClickListener((v, position) -> {
            if (v instanceof RelativeLayout) {
                // 歌曲选项被点击
                Intent intent = new Intent(getActivity(),
                        ArtistHomeActivity.class);
                intent.putExtra("artistItemPos", position);
                getActivity().startActivity(intent);
            } else if (v instanceof LinearLayout) {
                // 首字母选项被点击

            }
        });

        // 设置每个item的更多菜单点击
        MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(getActivity(), MoreMenuUtils.moreMenusAsArtist);
        impl = new BaseMoreMenuClickListenerImpl(getActivity(), mmAdapter);
        impl.setOnItemClickListener((fromView, clickView, fromViewPositon, clickViewPosition) -> {
            Artist item = (Artist) mSingerBySort.get(fromViewPositon);
            ArrayList<Song> artistSongs = item.getInfo().getSongs();
            switch (clickViewPosition) {
                case 0:
                    // 播放
                    MoreMenuUtils.playSongIntent(getActivity(), artistSongs, artistSongs.get(0));
                    break;
                case 1:
                    if (MoreMenuUtils.addSongsToPlayList(artistSongs)) {
                        Toast.makeText(getActivity(), "添加成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    // 添加到歌单
                    MoreMenuUtils.addSongToSonglist(getActivity(), artistSongs);
                    break;
                case 3:
                    // 删除
                    if (!artistSongs.contains(PlayingService.playingSong)) {
                        int deleteCount = MoreMenuUtils.deleteMp3(getActivity(), artistSongs);
                        if (deleteCount != 0) {
                            // 删除集合刷新列表
                            MoreMenuUtils.deleteArtistAndRefresh(mAdapter, mSingerBySort, item, fromViewPositon);
                            mSinger.remove(item);
                            // 发送广播
                            MoreMenuUtils.deleteIntent(getActivity(), DeleteInnerUI.ArtistFragment, artistSongs);
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

        });
        mAdapter.setOnMoreMenuClickListener(impl);
    }

    /**
     * 本地广播接收者,接受音乐搜索完成的广播
     *
     * @author lbRoNG
     */
    private class ArtistSearchFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(HomeActivity.LOADER_SEARCH_FINISH)
                    && !DeleteInnerUI.ArtistFragment.equals((intent.getSerializableExtra("innerUI")))) {
                // 清除旧数据
                mSinger.clear();
                final List<Song> mSongs = HomeActivity.getSongs();
                new Thread(() -> {
                    for (Song item : mSongs) {
                        // 歌手分类
                        Artist artist = item.getAlbum().getArtist();
                        // 录入歌手
                        if (mSinger.contains(artist)) {
                            int index = mSinger.indexOf(artist);
                            artist = mSinger.get(index);
                        } else {
                            mSinger.add(artist);
                        }
                        // 整理所属同一歌手的歌曲和专辑
                        ArtistMusicInfo info = artist.getInfo();
                        info.getSongs().add(item);
                        // 如果存在同名专辑则不添加
                        ArrayList<Album> albums = info.getAlbums();
                        if (!albums.contains(item.getAlbum())) {
                            albums.add(item.getAlbum());
                        }
                        artist.setInfo(info);
                        // 设置歌手封面
                        if (artist.getCoverRgb() == 0) {
                            Song temp = artist.getInfo().getSongs().get(0);
                            Bitmap cover = ImageUtils.getArtwork(getActivity(),
                                    temp.getTitle(), temp.getSongId(),
                                    temp.getAlbum().getAlbumId(), true);
                            setCoverColor(artist, cover);
                        }
                    }
                    // 排序集合
                    mSingerBySort = SongsSortByLettersUtils
                            .getInfoByLetter(mSinger,
                                    position -> mSinger.get(position).getSingerName());
                    // 更新列表
                    getActivity().runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                }).start();
            }
        }
    }

    /**
     * 设置封面颜色,通过该歌手随意一张专辑封面图片提取
     */
    @SuppressWarnings("deprecation")
    private void setCoverColor(final Artist artist, final Bitmap bitmap) {
        if (bitmap != null) {
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
                        artist.setCoverRgb(getResources().getColor(R.color.titleBackground));
                    } else {
                        artist.setCoverRgb(vibrant.getRgb());
                    }
                }
            });
        } else {
            artist.setCoverRgb(getResources().getColor(R.color.titleBackground));
        }
    }

    private class SingerAdapter extends RecyclerView.Adapter<SingerAdapter.ViewHolder> {
        // 点击事件监听器接口
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
            private TextView tv_artist_name, tv_artist_info, tv_artist_icon,
                    tv_letter;
            private ImageView iv_more;

            public ViewHolder(View itemView, int type) {
                super(itemView);
                this.itemView = itemView;
                if (type == LETTER_VIEW) {
                    tv_letter = (TextView) itemView.findViewById(R.id.tv_letter);
                } else {
                    tv_artist_name = (TextView) itemView.findViewById(R.id.tv_artist_name);
                    tv_artist_info = (TextView) itemView.findViewById(R.id.tv_artist_info);
                    tv_artist_icon = (TextView) itemView.findViewById(R.id.tv_artist_icon);
                    iv_more = (ImageView) itemView.findViewById(R.id.iv_more);
                    view_line = (View) itemView.findViewById(R.id.view_line);
                }
            }

            public View getItemView() {
                return itemView;
            }

        }

        @Override
        public SingerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
            View view = null;
            ViewHolder holder = null;
            if (viewType == LETTER_VIEW) {
                view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.letter_item, parent, false);
                holder = new ViewHolder(view, LETTER_VIEW);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.artist_item, parent, false);
                holder = new ViewHolder(view, ARTIST_VIEW);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            int type = getItemViewType(position);
            if (type == LETTER_VIEW) {
                holder.tv_letter.setText(mSingerBySort.get(position) + "");
            } else {
                Artist artist = (Artist) mSingerBySort.get(position);
                int songCount = artist.getInfo().getSongs().size();
                int albumCount = artist.getInfo().getAlbums().size();
                holder.tv_artist_name.setText(artist.getSingerName());
                holder.tv_artist_info.setText(albumCount + "张专辑" + " | "
                        + songCount + "首歌曲");
                holder.tv_artist_icon.setBackgroundColor(artist.getCoverRgb());
                // 设置歌手封面
                String nameSub = artist.getSingerName().substring(0, 1)
                        .toUpperCase(Locale.ENGLISH);
                if (!(nameSub.toCharArray()[0] >= 'A' && nameSub.toCharArray()[0] <= 'Z')) {
                    holder.tv_artist_icon.setText(nameSub);
                } else {
                    // 歌曲名为英文
                    holder.tv_artist_icon.setText(artist.getSingerName()
                            .substring(0, 2));
                }

                holder.iv_more.setOnClickListener(v -> {
                    if (mOnMoreMenuClickListener != null) {
                        mOnMoreMenuClickListener.onClick(v, position);
                    }
                });

                // 分割线是否显示
                if (position + 1 < mSingerBySort.size()) {
                    Object obj = mSingerBySort.get(position + 1);
                    if (obj instanceof String) {
                        holder.view_line.setVisibility(View.INVISIBLE);
                    } else {
                        holder.view_line.setVisibility(View.VISIBLE);
                    }
                } else if (position + 1 == mSingerBySort.size()) {
                    holder.view_line.setVisibility(View.INVISIBLE);
                }

            }
            // 注册点击事件
            holder.getItemView().setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, position);
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            Object obj = mSingerBySort.get(position);
            if (obj instanceof String) {
                return LETTER_VIEW;
            } else if (obj instanceof Artist) {
                return ARTIST_VIEW;
            }
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            return mSingerBySort.size();
        }
    }
}
