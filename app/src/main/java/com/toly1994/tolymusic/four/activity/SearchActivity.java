package com.toly1994.tolymusic.four.activity;

import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.MusicApplication;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;
import com.toly1994.tolymusic.fragment.*;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements
        SearchResultFragment.ExchangeCallBack,
        SearchHomeFragment.ExchangeCallBack,
        SearchAllResultFragment.ExchangeCallBack {
    private SharedPreferences sp;
    private ProgressBar pb_load;
    private SearchResultFragment srFragment;
    private SearchHomeFragment shFragment;
    private String query;
    private List<Object> searchResult;
    private List<?> detailResult;
    private FragmentManager manager;
    private Toolbar bar_home;
    private SearchView action_search;
    private boolean isDelete;
    public static BaseMoreMenuClickListenerImpl impl;
    public static Activity context;
    private MusicDeleteReceiver deleteReceiver;

    public void setProgressBarVisibility(int visiblity) {
        pb_load.setVisibility(visiblity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        context = SearchActivity.this;
        shFragment = new SearchHomeFragment();
        // 加载历史查询记录页面
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.rl_search, shFragment).commit();
        setViewComponent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MusicApplication.launchActivity = SearchActivity.class;
        if (action_search != null) {
            action_search.setQueryHint(getString(R.string.action_search_all_hint));
        }
        // 注册广播
        if (deleteReceiver == null) {
            deleteReceiver = new MusicDeleteReceiver();
            IntentFilter filter = new IntentFilter(HomeActivity.INTENT_MUSIC_DELETE);
            registerReceiver(deleteReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        if (deleteReceiver != null) {
            unregisterReceiver(deleteReceiver);
            deleteReceiver = null;
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
        } else {
            finish();
        }
    }

    private void setViewComponent() {
        manager = getSupportFragmentManager();
        bar_home = findViewById(R.id.bar_home);
        pb_load = findViewById(R.id.pb_load);
        sp = getSharedPreferences("search_history", Context.MODE_PRIVATE);
        // 设置action bar
        setActionBar();
    }

    private void setActionBar() {
        setSupportActionBar(bar_home);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    /**
     * 根据关键字搜索相关的歌曲,专辑,歌手
     *
     * @param key
     */
    private void SearchInfoByKey(final String key) {
        // 结果集合
        searchResult = new ArrayList<>();
        new Thread(() -> {
            /*--检索结果--*/
            // 获取数据集合
            List<Song> songList = HomeActivity.getSongs();
            List<Artist> artistList = ArtistFragment.getArtistList();
            List<Album> albumList = AlbumsFragment.getAlbumsList();
            // 在歌曲集合中检索
            for (int index = 0; index < songList.size(); index++) {
                Song item = songList.get(index);
                // 歌曲名,歌曲中的专辑歌手包含关键字都被检索
                if (item.getTitle().contains(key)
                        || item.getAlbum().getAlbumName().contains(key)
                        || item.getAlbum().getArtist().getSingerName()
                        .contains(key)) {
                    searchResult.add(item);
                }
            }
            // 在歌手中检索
            for (int index = 0; index < artistList.size(); index++) {
                Artist item = artistList.get(index);
                // 歌手名包含关键字都被检索
                if (item.getSingerName().contains(key)) {
                    searchResult.add(item);
                }
            }
            // 在专辑中检索
            for (int index = 0; index < albumList.size(); index++) {
                Album item = albumList.get(index);
                // 专辑名,专辑歌手包含关键字都被检索
                if (item.getAlbumName().contains(key)
                        || item.getArtist().getSingerName().contains(key)) {
                    searchResult.add(item);
                }
            }
            runOnUiThread(() -> {
                srFragment = new SearchResultFragment();
                manager.beginTransaction().replace(R.id.rl_search, srFragment).commit();
                setProgressBarVisibility(View.GONE);
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.single_search, menu);
        // 获取search View
        action_search = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        action_search.setIconified(false); // 默认展开
        action_search.setQueryHint(getString(R.string.action_search_all_hint));
        action_search.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.equals("")) {
                    SearchActivity.this.query = newText.trim();
                    // 显示搜索进度条
                    setProgressBarVisibility(View.VISIBLE);
                    // 保存搜索记录
                    boolean has = sp.getAll().containsValue(query);
                    if (!has) {
                        int size = sp.getAll().size();
                        Editor edit = sp.edit();
                        edit.putString(query + size, query);
                        edit.apply();
                    }
                    // 开始搜索
                    SearchInfoByKey(query);
                } else {
                    // 空搜索,回主页
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.rl_search, shFragment).commit();
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
                int count = manager.getBackStackEntryCount();
                if (count != 0) {
                    manager.popBackStack();
                    if (isDelete) {
                        action_search.setQuery(query + " ", false);
                        isDelete = false;
                    }
                } else {
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 歌曲删除的广播接收
     *
     * @author lbRoNG
     */
    private class MusicDeleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (query != null) {
                isDelete = true;
                List<Song> deleteList = intent.getParcelableArrayListExtra("deletelist");
                // 歌曲集合处理
                HomeActivity.getSongs().removeAll(deleteList);
                // 歌手集合处理
                List<Artist> artistList = ArtistFragment.getArtistList();
                for (Iterator<Artist> it = artistList.listIterator(); it.hasNext(); ) {
                    Artist tempArtist = it.next();
                    tempArtist.getInfo().getSongs().removeAll(deleteList);
                    if (tempArtist.getInfo().getSongs().size() == 0) {
                        it.remove();
                        artistList.remove(tempArtist);
                    }
                }
                // 专辑集合的处理
                List<Album> albumList = AlbumsFragment.getAlbumsList();
                for (Iterator<Album> it = albumList.listIterator(); it.hasNext(); ) {
                    Album tempAlbum = it.next();
                    tempAlbum.getSongs().removeAll(deleteList);
                    if (tempAlbum.getSongs().size() == 0) {
                        it.remove();
                        albumList.remove(tempAlbum);
                    }
                }
                // 重新搜索
                Fragment nowFragemnt = getSupportFragmentManager().findFragmentById(R.id.rl_search);
                if (nowFragemnt instanceof SearchResultFragment) {
                    action_search.setQuery(query, false);
                }
            }
        }
    }

    // 与fragment通信的接口实现方法
    @Override
    public void goToAllResult(int dataType) {
        if (dataType == SearchResultFragment.TYPE_SONG) {
            detailResult = srFragment.getSongs();
        } else if (dataType == SearchResultFragment.TYPE_ARTIST) {
            detailResult = srFragment.getArtists();
        } else if (dataType == SearchResultFragment.TYPE_ALBUM) {
            detailResult = srFragment.getAlbums();
        }
        // 设置搜索框不可编辑
        manager.beginTransaction()
                .replace(R.id.rl_search, new SearchAllResultFragment())
                .addToBackStack("ALLRESULT_STACK").commit();
    }

    @Override
    public void startSearchByKey(String key) {
        this.query = key;
        action_search.setQuery(query, false);
    }

    @Override
    public List<Object> getData() {
        return searchResult;
    }

    @Override
    public String getSearchKey() {
        return query;
    }

    @Override
    public List<?> getDetailResult() {
        return detailResult;
    }
}
