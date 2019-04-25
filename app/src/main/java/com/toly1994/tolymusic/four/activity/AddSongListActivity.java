package com.toly1994.tolymusic.four.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.*;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.db.SongListDBAdapter;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.domain.SongList;
import com.toly1994.tolymusic.app.utils.MTextUtils;
import com.toly1994.tolymusic.fragment.SongListFragment;
import com.toly1994.tolymusic.itf.OnBaseClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddSongListActivity extends AppCompatActivity {
    private Toolbar bar_home;
    private RecyclerView rv_check_songs;
    private CheckSongsAdapter adapter;
    private List<Song> checkSongs = new ArrayList<>();
    private List<Integer> checkSongsPos = new ArrayList<>();
    private EditText et_songlist_name;
    private SearchView action_search;
    private String searchKey = "";
    private int startMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_songlist);
        setViewComponent();
    }

    private void setViewComponent() {
        et_songlist_name = findViewById(R.id.et_songlist_name);
        rv_check_songs = findViewById(R.id.rv_check_songs);
        bar_home = findViewById(R.id.bar_home);
        // data
        checkSongs = HomeActivity.getSongs();
        // list
        rv_check_songs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheckSongsAdapter();
        rv_check_songs.setAdapter(adapter);

        // 分析是添加请求还是编辑请求
        Intent receive = getIntent();
        if (receive != null) {
            SongList item = receive.getParcelableExtra("edit_songlist");
            if (item != null) {
                // 携带数据的是编辑请求
                bar_home.setTitle("编辑歌单");
                et_songlist_name.setText(item.getListName());
                // 选中已经存在的歌曲
                checkOriginalSongs(item);
                // 设置启动模式
                startMode = SongListFragment.SONGLIST_EDIT_REQUEST;
            } else {
                bar_home.setTitle("新建歌单");
                startMode = SongListFragment.SONGLIST_ADD_REQUEST;
            }
        }
        // tool bar
        bar_home.setNavigationIcon(R.drawable.icon_check);
        setSupportActionBar(bar_home);

        adapter.setOnItemClickListener((view, position) -> {
            CheckBox check_song = view.findViewById(R.id.ck_choose);
            boolean isCheck = check_song.isChecked();
            check_song.setChecked(!isCheck);
            if (!isCheck) {
                checkSongsPos.add(position);
            } else {
                checkSongsPos.remove(Integer.valueOf(position));
            }
        });
    }

    /**
     * 编辑歌单请求,原来已经存在的歌曲在列表中置顶并勾选
     *
     * @param item
     */
    private void checkOriginalSongs(SongList item) {
        List<Song> temp = item.getSongs();
        for (int i = 0; i < temp.size(); i++) {
            Song tempItem = temp.get(i);
            int index = checkSongs.indexOf(tempItem);
            if (index != -1) {
                // 传递过来的Song实体是不完整的,通过不完整的实体寻找出完整的实体并保存
                checkSongs.add(i, checkSongs.get(index));
                checkSongs.remove(tempItem);
            }
            // 记录勾选的歌曲位置
            checkSongsPos.add(i);
            // 更新列表
            adapter.notifyItemRemoved(checkSongs.indexOf(tempItem));
            adapter.notifyItemInserted(i);
        }
    }

    /**
     * 添加新的播放列表
     *
     * @return
     */
    private SongList addNewSongList() {
        SongList listItem = null;
        List<Song> tempList = new ArrayList<>();
        String name = et_songlist_name.getText().toString();
        if (!TextUtils.isEmpty(name)) {
            SongListDBAdapter db = null;
            try {
                db = new SongListDBAdapter(this).open();
                ;
                long insertId = db.insertSongList(name);
                if (insertId != -1) {
                    for (int i = 0; i < checkSongsPos.size(); i++) {
                        Song item = checkSongs.get(checkSongsPos.get(i));
                        db.insertSongItem(item.getTitle(), item.getSongId(), insertId);
                        tempList.add(item);
                    }
                    listItem = new SongList(name, tempList);
                }
            } finally {
                if (db != null) {
                    db.close();
                }
            }
        }
        return listItem;
    }

    /**
     * 编辑歌单
     *
     * @param oldSongList
     * @return
     */
    private SongList editSongList(SongList oldSongList) {
        SongList newSongList = null;
        if (oldSongList != null) {
            String newName = et_songlist_name.getText().toString();
            // 整理出当前选择的歌曲
            List<Song> newList = new ArrayList<>();
            List<Song> oldList = oldSongList.getSongs();
            for (int i = 0; i < checkSongsPos.size(); i++) {
                Song item = checkSongs.get(checkSongsPos.get(i));
                newList.add(item);
            }
            if (!TextUtils.isEmpty(newName)) {
                SongListDBAdapter db = null;
                try {
                    db = new SongListDBAdapter(this).open();
                    if (newName.equals(oldSongList.getListName())
                            && !(newList.containsAll(oldList) && oldList.containsAll(newList))) {
                        // 只改变歌单包含的歌曲
                        System.out.println("1");
                        newSongList = new SongList(newName, updateSongsAsSongList(db, newName));
                    } else if (!newName.equals(oldSongList.getListName())
                            && (newList.containsAll(oldList) && oldList.containsAll(newList))) {
                        // 只改变歌单名
                        updateNameAsSongList(db, newName);
                        newSongList = new SongList(newName, newList);
                        System.out.println("2");
                    } else if (!newName.equals(oldSongList.getListName())
                            && !(newList.containsAll(oldList) && oldList.containsAll(newList))) {
                        // 全改变
                        newSongList = new SongList(newName, updateSongsAsSongList(db, newName));
                        updateNameAsSongList(db, newName);
                        System.out.println("3");
                    }
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }
        }
        return newSongList;
    }

    private List<Song> updateSongsAsSongList(SongListDBAdapter db, String newName) {
        // 只改变歌单的歌曲数
        // 删除原本属于该歌单的歌曲,重新添加
        int insertId = db.getSonglistID4Name(newName);
        db.deleteItem(SongListDBAdapter.DATABASE_TABLE_SONG,
                SongListDBAdapter.KEY_FROM_LIST + "=" + insertId);
        List<Song> newList = new ArrayList<>();
        for (int i = 0; i < checkSongsPos.size(); i++) {
            Song item = checkSongs.get(checkSongsPos.get(i));
            db.insertSongItem(item.getTitle(), item.getSongId(), insertId);
            newList.add(item);
        }
        return newList;
    }

    private void updateNameAsSongList(SongListDBAdapter db, String newName) {
        int insertId = db.getSonglistID4Name(newName);
        db.updateSongListItem(insertId, newName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        // 获取search View
        action_search = (SearchView) menu.findItem(R.id.action_search).getActionView();
        action_search.setQueryHint(getString(R.string.action_search_part_hint));
        action_search.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String key) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String key) {
                if (key != null && key.length() > 0) {
                    if (checkSongs.size() != 0) {
                        for (int i = 0; i < checkSongs.size(); i++) {
                            Song item = checkSongs.get(i);
                            if (item.getTitle().toLowerCase(Locale.ENGLISH)
                                    .contains(key.toLowerCase(Locale.ENGLISH))) {
                                searchKey = key;
                                adapter.notifyItemChanged(i);
                                rv_check_songs.scrollToPosition(i);
                                break;
                            }
                        }
                    }
                } else {
                    searchKey = "";
                    adapter.notifyDataSetChanged();
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
                SongList temp = null;
                Intent data = new Intent();
                if (startMode == SongListFragment.SONGLIST_ADD_REQUEST) {
                    temp = addNewSongList();
                    data.putExtra("finish_add_songlistitem", temp);
                } else if (startMode == SongListFragment.SONGLIST_EDIT_REQUEST) {
                    SongList oldSongList = getIntent().getParcelableExtra("edit_songlist");
                    temp = editSongList(oldSongList);
                    data.putExtra("edit_songlist_position", getIntent().getIntExtra("edit_songlist_position", -1));
                    data.putExtra("finish_edit_songlistitem", temp);
                }
                // 回传数据
                setResult(RESULT_OK, data);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class CheckSongsAdapter extends RecyclerView.Adapter<CheckSongsAdapter.CheckSongsHolder> {
        private OnBaseClickListener mOnItemClickListener = null;

        public void setOnItemClickListener(OnBaseClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        public class CheckSongsHolder extends RecyclerView.ViewHolder {
            private View itemView;
            private TextView tv_music_name, tv_artist_name;
            private CheckBox check_song;

            public CheckSongsHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                tv_music_name = itemView.findViewById(R.id.tv_music_name);
                tv_artist_name = itemView.findViewById(R.id.tv_artist_name);
                check_song = itemView.findViewById(R.id.ck_choose);
            }

            public View getItemView() {
                return itemView;
            }

        }

        @Override
        public int getItemCount() {
            return checkSongs.size();
        }

        @Override
        public void onBindViewHolder(CheckSongsHolder holder, final int position) {
            Song item = checkSongs.get(position);
            holder.tv_music_name.setText(MTextUtils.setTextColorByKey(
                    item.getTitle(), searchKey, getResources().getColor(R.color.titleBackground)));
            holder.tv_artist_name.setText(item.getAlbum().getArtist().getSingerName());
            holder.check_song.setChecked(checkSongsPos.contains(Integer.valueOf(position)));
            holder.getItemView().setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, position);
                }
            });

        }

        @Override
        public CheckSongsHolder onCreateViewHolder(ViewGroup parent, int type) {
            View itemView = LayoutInflater.from(AddSongListActivity.this)
                    .inflate(R.layout.songlist_check_song_item, parent, false);
            return new CheckSongsHolder(itemView);
        }
    }
}
