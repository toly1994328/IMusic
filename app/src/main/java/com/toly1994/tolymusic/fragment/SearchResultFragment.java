package com.toly1994.tolymusic.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.widget.SearchResultItem;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchResultFragment extends Fragment {
    private View view;
    private String query;
    private RelativeLayout rl_not_search;
    private List<Object> result;
    private List<Song> songs;
    private List<Artist> artists;
    private List<Album> albums;
    private SearchResultItem songItem, artistItem, albumItem;
    private ExchangeCallBack callBack;
    public final static int TYPE_SONG = 11;
    public final static int TYPE_ARTIST = 12;
    public final static int TYPE_ALBUM = 13;

    public interface ExchangeCallBack {
        void goToAllResult(int type);

        List<Object> getData();

        String getSearchKey();
    }

    public List<Song> getSongs() {
        return songs;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public List<Album> getAlbums() {
        return albums;
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
            view = View.inflate(getActivity(), R.layout.fragment_search_result,
                    null);
        }
        setViewCompontent();
        return view;
    }

    private void setViewCompontent() {
        songItem = view.findViewById(R.id.sr_one);
        artistItem = view.findViewById(R.id.sr_two);
        albumItem = view.findViewById(R.id.sr_three);
        // 初始化容器
        songs = new ArrayList<>();
        artists = new ArrayList<>();
        albums = new ArrayList<>();
        // 获取数据
        result = callBack.getData();
        query = callBack.getSearchKey();
        if (result != null && result.size() == 0) {
            rl_not_search = view.findViewById(R.id.rl_not_search);
            rl_not_search.setVisibility(View.VISIBLE);
        } else {
            // 遍历集合,分组数据
            for (int i = 0; i < result.size(); i++) {
                Object obj = result.get(i);
                if (obj instanceof Song) {
                    songs.add((Song) obj);
                } else if (obj instanceof Artist) {
                    artists.add((Artist) obj);
                } else if (obj instanceof Album) {
                    albums.add((Album) obj);
                }
            }
            // 整理数据,确定有多少类型的数据需要显示
            settleResult();
        }
        // 设置点击监听
        songItem.setOnSongItemClickListener((view, position) ->
                MoreMenuUtils.playSongIntent(
                        getActivity(), (ArrayList<Song>) songs, songs.get(position)));

        artistItem.setOnSongItemClickListener((view, position) ->
                MoreMenuUtils.aboutArtist(getActivity(), artists.get(position)));

        albumItem.setOnSongItemClickListener((view, position) ->
                MoreMenuUtils.aboutAlbumIntent(getActivity(), albums.get(position)));

        // 设置菜单
        songItem.setMoreMenu(songs, MoreMenuUtils.moreMenusAsSong);
        artistItem.setMoreMenu(artists, MoreMenuUtils.moreMenusAsArtist);
        albumItem.setMoreMenu(albums, MoreMenuUtils.moreMenusAsAlbum);
    }

    /**
     * 分析整理数据,用以确定该显示多少内容
     */
    private void settleResult() {
        for (int i = 1; i < songs.size() + 1; i++) {
            Song data = songs.get(i - 1);
            Bitmap cover = data.getAlbum().getCover();
            if (cover == null) {
                cover = ImageUtils.getArtwork(getActivity(), data.getTitle(),
                        data.getSongId(), data.getAlbum().getAlbumId(), true);
                // 把封面保存下来
                data.getAlbum().setCover(cover);
            }
            String desc = data.getAlbum().getArtist().getSingerName() + " | "
                    + data.getAlbum().getAlbumName();
            settleResult(songItem, TYPE_SONG, i,
                    getResources().getString(R.string.title_song),
                    getResources().getString(R.string.show_all_song),
                    data.getTitle(), desc, cover);
        }
        for (int i = 1; i < artists.size() + 1; i++) {
            Artist data = artists.get(i - 1);
            String desc = data.getInfo().getAlbums().size() + "张专辑" + " | "
                    + data.getInfo().getSongs().size() + "首歌曲";
            settleResult(artistItem, TYPE_ARTIST, i,
                    getResources().getString(R.string.title_artist),
                    getResources().getString(R.string.show_all_artist),
                    data.getSingerName(), desc, BitmapFactory.decodeResource(
                            getResources(), R.drawable.default_music_icon));
        }
        for (int i = 1; i < albums.size() + 1; i++) {
            Album data = albums.get(i - 1);
            Bitmap cover = data.getCover();
            if (cover == null) {
                cover = ImageUtils.getArtwork(getActivity(), data.getSongs()
                                .get(0).getTitle(), data.getSongs().get(0).getSongId(),
                        data.getAlbumId(), true);
                data.setCover(cover);
            }
            settleResult(albumItem, TYPE_ALBUM, i,
                    getResources().getString(R.string.title_album),
                    getResources().getString(R.string.show_all_album),
                    data.getAlbumName(), data.getArtist().getSingerName(),
                    cover);
        }
    }

    private void settleResult(SearchResultItem root, final int type, int pos,
                              String title, String more, String text, String desc, Bitmap cover) {
        if (!root.isShown()) {
            root.setVisibility(View.VISIBLE);
        }
        if (pos == 1) {
            // 设置标题
            root.setTitle(title);
        } else if (pos > 3) {
            // 结果大于3,显示更多
            root.showOrHideMore(View.VISIBLE);
            root.setMoreTitle(more);
            root.setOnMoreBtnClickListener(v -> callBack.goToAllResult(type));
            return;
        }
        root.setItem(pos, text, desc, cover, query);
    }
}
