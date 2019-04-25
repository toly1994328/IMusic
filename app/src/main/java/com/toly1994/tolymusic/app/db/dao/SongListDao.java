package com.toly1994.tolymusic.app.db.dao;

import android.content.Context;
import android.database.Cursor;
import com.toly1994.tolymusic.four.activity.HomeActivity;
import com.toly1994.tolymusic.app.db.SongListDBAdapter;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.domain.SongList;

import java.util.ArrayList;
import java.util.List;

public class SongListDao {
	public static List<SongList> getSongList(Context context) {
		List<SongList> songlist = new ArrayList<>();
		SongListDBAdapter db = new SongListDBAdapter(context).open();
		Cursor listCursor = db
				.getAllItem(SongListDBAdapter.DATABASE_TABLE_LIST);
		while (listCursor != null && listCursor.moveToNext()) {
			List<Song> tempList = new ArrayList<>();
			// 获取到歌单名
			String name = listCursor.getString(listCursor
					.getColumnIndex(SongListDBAdapter.KEY_LIST_NAME));
			// 通过歌单名检索属于该歌单的歌曲
			Cursor songCursor = db.getAllSongs4ListName(name);
			while (songCursor != null && songCursor.moveToNext()) {
				String songName = songCursor.getString(songCursor
						.getColumnIndex(SongListDBAdapter.KEY_SONG_NAME));
				long songId = songCursor.getLong(songCursor
						.getColumnIndex(SongListDBAdapter.KEY_SONG_ONLY_ID));
				// 通过数据库的歌名个歌曲ID检索出Song实体
				Song tempSong = getSong4NameAndID(songName, songId);
				if (tempSong != null) {
					tempList.add(tempSong);
				}
			}
			songlist.add(new SongList(name, tempList));
		}
		return songlist;
	}

	public static Song getSong4NameAndID(String name, long id) {
		List<Song> temp = HomeActivity.getSongs();
		for (Song item : temp) {
			if (item.getTitle().equals(name) && item.getSongId() == id) {
				return item;
			}
		}
		return null;
	}
}
