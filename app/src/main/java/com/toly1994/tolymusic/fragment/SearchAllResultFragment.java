package com.toly1994.tolymusic.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;
import com.toly1994.tolymusic.itf.impl.MoreMenuImpl;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchAllResultFragment extends Fragment {
    private View view;
    private RecyclerView rv_all_result;
    private AllResultAdapter resultAdapter;
    private ExchangeCallBack callBack;
    private List<?> detailResult;
    private BaseMoreMenuClickListenerImpl impl;
    private List<Integer> moreMenus = null;

    public interface ExchangeCallBack {
        List<?> getDetailResult();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            callBack = (ExchangeCallBack) context;
        } catch (ClassCastException e) {
            throw new RuntimeException("父Activity必须实现必要的接口");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_search_all_result,
                    null);
        }
        setViewCompontent();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (impl.popupWindowIsShow()) {
                        impl.popupWindowDismiss();
                    } else if (MoreMenuUtils.chooseSonglistPopupWindow != null
                            && MoreMenuUtils.chooseSonglistPopupWindow.isShowing()) {
                        MoreMenuUtils.chooseSonglistPopupWindow.dismiss();
                    } else {
                        getActivity().finish();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void setViewCompontent() {
        // 获取数据
        detailResult = callBack.getDetailResult();
        rv_all_result = view.findViewById(R.id.rv_all_result);
        rv_all_result.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv_all_result.setItemAnimator(new DefaultItemAnimator());
        resultAdapter = new AllResultAdapter();
        resultAdapter.setOnItemClickListener((view, position) -> {
            Object obj = detailResult.get(position);
            if (obj instanceof Song) {
                MoreMenuUtils.playSongIntent(getActivity(), (ArrayList<Song>) detailResult, (Song) obj);
            } else if (obj instanceof Album) {
                MoreMenuUtils.aboutAlbumIntent(getActivity(), (Album) obj);
            } else {
                MoreMenuUtils.aboutArtist(getActivity(), (Artist) obj);
            }
        });
        rv_all_result.setAdapter(resultAdapter);

        // 设置每个item的更多菜单点击
        Object obj = detailResult.get(0);
        if (obj instanceof Song) {
            moreMenus = MoreMenuUtils.moreMenusAsSong;
        } else if (obj instanceof Album) {
            moreMenus = MoreMenuUtils.moreMenusAsAlbum;
        } else {
            moreMenus = MoreMenuUtils.moreMenusAsArtist;
        }
        // 获取提供菜单选项的内容适配器
        MoreMenuBaseAdapter mmAdapter = new MoreMenuBaseAdapter(getActivity(), moreMenus);
        // 把内容给点击事件的实现类,完成点击跳出对应菜单
        impl = new BaseMoreMenuClickListenerImpl(getActivity(), mmAdapter);
        // 给菜单内的选项添加点击事件
        impl.setOnItemClickListener(
                (fromView, clickView, fromViewPositon, clickViewPosition) -> {
                    if (moreMenus.size() == 7) {
                        MoreMenuImpl.MoreMenuAsSong(
                                getActivity(), (ArrayList<Song>) detailResult,
                                resultAdapter, fromViewPositon, clickViewPosition);
                    } else if (moreMenus.size() == 5) {
                        MoreMenuImpl.MoreMenuAsAlbum(
                                getActivity(), (ArrayList<Album>) detailResult,
                                resultAdapter, fromViewPositon, clickViewPosition);
                    } else {
                        MoreMenuImpl.MoreMenuAsArtist(
                                getActivity(), (ArrayList<Artist>) detailResult,
                                resultAdapter, fromViewPositon, clickViewPosition);
                    }
                    // 隐藏菜单
                    impl.popupWindowDismiss();
                });
        // 设置菜单按钮的点击事件
        resultAdapter.setOnMoreMenuClickListener(impl);

    }

    private class AllResultAdapter extends
            RecyclerView.Adapter<AllResultAdapter.AllResultHolder> {

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

        public class AllResultHolder extends RecyclerView.ViewHolder {
            private View itemView;
            private TextView tv_music_name, tv_music_albums;
            private ImageView iv_music_icon, iv_more;

            public AllResultHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                tv_music_name = itemView.findViewById(R.id.tv_music_name);
                tv_music_albums = itemView.findViewById(R.id.tv_music_albums);
                iv_music_icon = itemView.findViewById(R.id.iv_music_icon);
                iv_more = itemView.findViewById(R.id.iv_more);
            }

            public View getItemView() {
                return itemView;
            }
        }

        @Override
        public int getItemCount() {
            return detailResult != null ? detailResult.size() : 0;
        }

        @Override
        public void onBindViewHolder(AllResultHolder holder, final int position) {
            Object obj = detailResult.get(position);
            String title = null, desc = null;
            Bitmap bm = null;
            int type = getItemViewType(position);
            if (type == SearchResultFragment.TYPE_SONG) {
                Song data = (Song) obj;
                title = data.getTitle();
                desc = data.getAlbum().getArtist().getSingerName() + " | "
                        + data.getAlbum().getAlbumName();
                bm = data.getAlbum().getCover();
                if (bm == null) {
                    bm = ImageUtils.getArtwork(getActivity(), data.getTitle(),
                            data.getSongId(), data.getAlbum().getAlbumId(), true);
                    // 保存封面
                    data.getAlbum().setCover(bm);
                }

            } else if (type == SearchResultFragment.TYPE_ARTIST) {
                Artist data = (Artist) obj;
                title = data.getSingerName();
                desc = data.getInfo().getAlbums().size() + "张专辑" + " | "
                        + data.getInfo().getSongs().size() + "首歌曲";
                bm = BitmapFactory.decodeResource(getResources(),
                        R.drawable.default_music_icon);
            } else if (type == SearchResultFragment.TYPE_ALBUM) {
                Album data = (Album) obj;
                title = data.getAlbumName();
                desc = data.getArtist().getSingerName();
                bm = data.getCover();
                if (bm == null) {
                    bm = ImageUtils.getArtwork(getActivity(), data.getSongs()
                                    .get(0).getTitle(), data.getSongs().get(0).getSongId(),
                            data.getAlbumId(), true);
                    data.setCover(bm);
                }
            }
            holder.tv_music_name.setText(title);
            holder.tv_music_albums.setText(desc);
            holder.iv_music_icon.setImageBitmap(bm);

            holder.getItemView().setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, position);
                }
            });

            holder.iv_more.setOnClickListener(v -> {
                if (mOnMoreMenuClickListener != null) {
                    mOnMoreMenuClickListener.onClick(v, position);
                }
            });

        }

        @Override
        public AllResultHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.music_item, parent, false);
            AllResultHolder holder = new AllResultHolder(view);
            return holder;
        }

        @Override
        public int getItemViewType(int position) {
            Object obj = detailResult.get(position);
            if (obj instanceof Song) {
                return SearchResultFragment.TYPE_SONG;
            } else if (obj instanceof Artist) {
                return SearchResultFragment.TYPE_ARTIST;
            } else if (obj instanceof Album) {
                return SearchResultFragment.TYPE_ALBUM;
            } else {
                return 0;
            }
        }
    }
}
