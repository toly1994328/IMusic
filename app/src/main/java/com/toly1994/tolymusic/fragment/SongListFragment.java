package com.toly1994.tolymusic.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.db.SongListDBAdapter;
import com.toly1994.tolymusic.app.db.dao.SongListDao;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.domain.SongList;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.app.utils.SystemUtils;
import com.toly1994.tolymusic.four.activity.AddSongListActivity;
import com.toly1994.tolymusic.four.activity.HomeActivity;
import com.toly1994.tolymusic.four.activity.SongListHomeActivity;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.OnMoreMenuItemClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SongListFragment extends Fragment {
    public final static String SONGLIST_CHANGE_INTENT = "SONGLIST_CHANGE_INTENT";
    public final static int SONGLIST_INSTER = 1111;
    public final static int SONGLIST_DELETE = 1112;
    public final static int SONGLIST_ADD_REQUEST = 1100;
    public final static int SONGLIST_EDIT_REQUEST = 1101;
    public final static int SONGLIST_DETAILS_REQUEST = 1102;
    private final static int TYPE_SIMPLE = 1;
    private final static int TYPE_MORE = 2;
    private static int COVER_LAST_UPDATE_POSITION = 5;
    private ImageView iv_add_songlist;
    private RecyclerView rv_songlist;
    private View view;
    private SongListAdapter slAdapter;
    private ExecutorService pool;
    public static List<SongList> songlist = new ArrayList<>();
    private SongsSearchFinishReceiver mReceiver;
    private SongListChangeReceiver changeReceiver;
    private BaseMoreMenuClickListenerImpl impl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化线程池
        pool = Executors.newCachedThreadPool();
        // 注册歌单改变的广播
        if (changeReceiver == null) {
            changeReceiver = new SongListChangeReceiver();
            getActivity().registerReceiver(changeReceiver,
                    new IntentFilter(SongListFragment.SONGLIST_CHANGE_INTENT));
        }
        // 注册本地歌曲检索完成的广播
        if (mReceiver == null) {
            mReceiver = new SongsSearchFinishReceiver();
            getActivity().registerReceiver(mReceiver,
                    new IntentFilter(HomeActivity.LOADER_SEARCH_FINISH));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_songlist, null);
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
        pool.shutdownNow();
        // 注销广播
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if (changeReceiver != null) {
            getActivity().unregisterReceiver(changeReceiver);
            changeReceiver = null;
        }
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    public void setViewComponent() {
        iv_add_songlist = view.findViewById(R.id.iv_add_songlist);
        rv_songlist = view.findViewById(R.id.rv_songlist);
        final StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rv_songlist.setLayoutManager(sglm);
        rv_songlist.setItemAnimator(new DefaultItemAnimator());
        slAdapter = new SongListAdapter();
        rv_songlist.setAdapter(slAdapter);

        iv_add_songlist.setOnClickListener(v ->
                startActivityForResult(new Intent(getActivity(), AddSongListActivity.class),
                        SONGLIST_ADD_REQUEST));

        slAdapter.setOnItemClickListener((view, position) -> {
            SongList clickItem = songlist.get(position);
            if (clickItem.getSongs().size() != 0) {
                Intent send = new Intent(getActivity(), SongListHomeActivity.class);
                send.putExtra("songlistItem", clickItem);
                send.putExtra("position", position);
                startActivityForResult(send, SONGLIST_DETAILS_REQUEST);
            } else {
                Toast.makeText(getActivity(), "没有歌曲的歌单", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置滚动监听,当界面停留的时候再加载屏幕内条目的图片
        rv_songlist.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int[] firstViewble = new int[2];
                    int[] lastViewble = new int[2];
                    // 获取第一个显示的位置
                    firstViewble = sglm.findFirstVisibleItemPositions(firstViewble);
                    // 获取最后一个显示的位置
                    lastViewble = sglm.findLastVisibleItemPositions(lastViewble);
                    for (int position = firstViewble[0]; position <= lastViewble[1]; position++) {
                        if (!(songlist.size() == 0)) {
                            setMusicCover(position);
                        }
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // 获取最后一个显示的位置
                int[] lastViewble = new int[2];
                lastViewble = sglm.findLastVisibleItemPositions(lastViewble);
                if (lastViewble[0] > COVER_LAST_UPDATE_POSITION) {
                    // 滑动速度在指定速度以下再加载图片,滑动速度快代表用户只是在找指定的歌,不必加载图片
                    if (Math.abs(dy) < 100 && Math.abs(dy) > 5) {
                        setMusicCover(lastViewble[0]);
                        setMusicCover(lastViewble[1]);
                        COVER_LAST_UPDATE_POSITION = lastViewble[1];
                    }
                }
            }
        });

        // 获取提供菜单选项的内容适配器
        MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(getActivity(), MoreMenuUtils.moreMenusAsSongList);
        // 把内容给点击事件的实现类,完成点击跳出对应菜单
        impl = new BaseMoreMenuClickListenerImpl(getActivity(), mmAdapter);
        // 给菜单内的选项添加点击事件
        impl.setOnItemClickListener(new OnMoreMenuItemClickListener() {
            @Override
            public void onClick(View fromView, View clickView, int fromViewPositon,
                                int clickViewPosition) {
                SongList clickItem = songlist.get(fromViewPositon);
                ArrayList<Song> listSongs = (ArrayList<Song>) clickItem.getSongs();
                switch (clickViewPosition) {
                    case 0:
                        MoreMenuUtils.playSongIntent(getActivity(), listSongs, listSongs.get(0));
                        break;
                    case 1:
                        // 添加进播放队列
                        if (MoreMenuUtils.addSongsToPlayList(listSongs)) {
                            Toast.makeText(getActivity(), "添加成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        // 编辑
                        Intent send = new Intent(getActivity(), AddSongListActivity.class);
                        send.putExtra("edit_songlist", songlist.get(fromViewPositon));
                        send.putExtra("edit_songlist_position", fromViewPositon);
                        startActivityForResult(send, SONGLIST_EDIT_REQUEST);
                        break;
                    case 3:
                        // 删除
                        deleteSongList(fromViewPositon);
                        break;
                }
                impl.popupWindowDismiss();
            }
        });
        // 设置菜单按钮的点击事件
        slAdapter.setOnMoreMenuClickListener(impl);
        // 设置按钮颜色
        GradientDrawable sd = (GradientDrawable) (((RippleDrawable)
                iv_add_songlist.getBackground()).getDrawable(0));
        sd.setColor(Color.parseColor("#76C2AF"));
    }

    /**
     * 设置封面
     *
     * @param position
     */
    private void setMusicCover(final int position) {
        if (slAdapter.getItemViewType(position) == TYPE_MORE) {
            final SongList item = songlist.get(position);
            if (item.getCover() == null) {
                // 提交任务给线程池处理
                pool.submit(() -> {
                    Bitmap cover = getCover(item.getSongs().get(0));
                    if (cover != null) {
                        item.setCover(cover);
                    } else {
                        item.setCover(BitmapFactory.decodeResource(
                                getResources(),
                                R.drawable.default_music_icon));
                    }
                    getActivity().runOnUiThread(() -> slAdapter.notifyDataSetChanged());
                });
            }
        }
    }

    private Bitmap getCover(Song item) {
        // 这里是为了处理特殊情况——>添加歌单界面返回的songlist实体内的song实体没有包含album
        if (item.getAlbum() == null) {
            List<Song> temp = HomeActivity.getSongs();
            item = temp.get(temp.indexOf(item));
        }
        Bitmap cover = item.getAlbum().getCover();
        if (cover == null) {
            cover = ImageUtils.getArtwork(getActivity(),
                    item.getTitle(), item.getSongId(),
                    item.getAlbum().getAlbumId(), true);
            item.getAlbum().setCover(cover);
        }
        return cover;
    }

    /**
     * 添加歌曲到歌单
     *
     * @param items
     * @param songListName
     */
    private void addSongToSongList(final List<Song> items, final String songListName) {
        pool.submit(() -> {
            // 根据歌单名获取到歌单实例
            if (songlist != null && songlist.size() != 0) {
                final int position = songlist.indexOf(new SongList(songListName, null));
                SongList temp = songlist.get(position);
                List<Song> songsAsList = temp.getSongs();
                if (songlist.contains(temp)) {
                    SongListDBAdapter db = null;
                    try {
                        db = new SongListDBAdapter(getActivity()).open();
                        for (int i = 0; i < items.size(); i++) {
                            Song item = items.get(i);
                            if (!songsAsList.contains(item)) {
                                // 数据库数据处理
                                long result = db.insertSongItem(item.getTitle(), item.getSongId(),
                                        db.getSonglistID4Name(songListName));
                                if (result != -1) {
                                    // 集合数据处理
                                    songsAsList.add(0, item);
                                    getActivity().runOnUiThread(() -> {
                                        slAdapter.notifyItemChanged(position);
                                        Toast.makeText(getActivity(), "添加成功", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } else {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getActivity(), "歌单已包含该歌曲", Toast.LENGTH_SHORT).show());
                            }
                        }
                    } finally {
                        db.close();
                    }
                }
            }
        });
    }

    /**
     * 根据列表的位置删除对应的歌单
     *
     * @param position 实例在列表中的位置
     */
    private void deleteSongList(int position) {
        SongList item = songlist.get(position);
        SongListDBAdapter db = new SongListDBAdapter(getActivity()).open();
        long songListID = db.getSonglistID4Name(item.getListName());
        // 删除数据库内的播放列表
        db.deleteItem(SongListDBAdapter.DATABASE_TABLE_LIST,
                SongListDBAdapter.KEY_LIST_NAME + "='" + item.getListName() + "'");
        // 删除数据库内的播放列表关联的音乐
        db.deleteItem(SongListDBAdapter.DATABASE_TABLE_SONG,
                SongListDBAdapter.KEY_FROM_LIST + "=" + songListID);
        db.close();
        //刷新列表
        songlist.remove(position);
        slAdapter.notifyItemRemoved(position);
        // 删除一个之后,position之后的item索引都改变了,所以从position开始刷新之后的item
        slAdapter.notifyItemRangeChanged(position, slAdapter.getItemCount());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SONGLIST_ADD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                SongList item = data.getParcelableExtra("finish_add_songlistitem");
                if (item != null) {
                    songlist.add(0, item);
                    slAdapter.notifyItemInserted(0);
                    // 增加数据后,数据顺序改变了,刷新新增数据后的条目
                    slAdapter.notifyItemRangeChanged(1, slAdapter.getItemCount());
                    rv_songlist.scrollToPosition(0);
                    Toast.makeText(getActivity(), "添加成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "添加失败", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == SONGLIST_EDIT_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                SongList item = data.getParcelableExtra("finish_add_songlistitem");
                int position = data.getIntExtra("edit_songlist_position", -1);
                if (item != null && position != -1) {
                    SongList original = songlist.get(position);
                    original.setListName(item.getListName());
                    original.setSongs(item.getSongs());
                    // 刷新适配器
                    slAdapter.notifyItemChanged(position);
                    Toast.makeText(getActivity(), "编辑成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "歌单内容无改变", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == SONGLIST_DETAILS_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                int position = data.getIntExtra("position", -1);
                slAdapter.notifyItemRemoved(position);
                slAdapter.notifyItemRangeChanged(position, slAdapter.getItemCount());
            }
        }
    }

    /**
     * 本地广播接收者,接受音乐搜索完成的个广播
     *
     * @author lbRoNG
     */
    private class SongsSearchFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(HomeActivity.LOADER_SEARCH_FINISH)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 清除旧数据
                        songlist.clear();
                        songlist = SongListDao.getSongList(getActivity());
                        getActivity().runOnUiThread(() -> {
                            if (slAdapter != null) {
                                slAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();
            }
        }
    }

    /**
     * 歌单改变的广播
     *
     * @author lbRoNG
     */
    private class SongListChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(SONGLIST_CHANGE_INTENT)) {
                int tag = intent.getIntExtra("actiontype", -1);
                switch (tag) {
                    case SONGLIST_INSTER:
                        List<Song> items = intent.getParcelableArrayListExtra("songlist");
                        String songlistName = intent.getStringExtra("songlistname");
                        addSongToSongList(items, songlistName);
                        break;
                    case SONGLIST_DELETE:
                        int position = intent.getIntExtra("position", -1);
                        Song deleteItem = intent.getParcelableExtra("deleteItem");
                        SongList deleteList = songlist.get(position);
                        deleteList.getSongs().remove(deleteItem);
                        slAdapter.notifyItemChanged(position);
                        break;
                }
                // 移除粘性广播
                getActivity().removeStickyBroadcast(intent);
            }
        }
    }

    private class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongListHolder> {
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

        public class SongListHolder extends RecyclerView.ViewHolder {
            private ImageView iv_cover, iv_more;
            private TextView tv_songlist_name, tv_songlist_size;

            public SongListHolder(View itemView, int type) {
                super(itemView);
                if (type == TYPE_MORE) {
                    iv_cover = itemView.findViewById(R.id.iv_cover);
                }
                tv_songlist_name = itemView.findViewById(R.id.tv_songlist_name);
                tv_songlist_size = itemView.findViewById(R.id.tv_songlist_size);
                iv_more = itemView.findViewById(R.id.iv_more);
            }

        }

        @Override
        public int getItemCount() {
            return songlist.size();
        }

        @Override
        public int getItemViewType(int position) {
            SongList item = songlist.get(position);
            int size = item.getSongs().size();
            if (size >= 0 && size < 4) {
                return TYPE_SIMPLE;
            } else {
                return TYPE_MORE;
            }
        }

        @Override
        public void onBindViewHolder(SongListAdapter.SongListHolder holder,
                                     final int pos) {
            SongList item = songlist.get(pos);
            int type = getItemViewType(pos);
            if (type == TYPE_MORE) {
                if (pos < 6) {
                    setMusicCover(pos);
                }
                holder.iv_cover.setImageBitmap(item.getCover());
            }
            holder.tv_songlist_name.setText(item.getListName());
            holder.tv_songlist_size.setText(item.getSongs().size() + "首");

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
        public SongListAdapter.SongListHolder onCreateViewHolder(
                ViewGroup parent, int type) {
            View itemView = null;
            if (type == TYPE_SIMPLE) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.songlist_item_two, parent, false);
            } else {
                itemView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.songlist_item_one, parent, false);
            }
            return new SongListHolder(itemView, type);
        }
    }
}
