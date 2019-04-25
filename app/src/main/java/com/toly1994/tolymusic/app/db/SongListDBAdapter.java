package com.toly1994.tolymusic.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SongListDBAdapter {
	private static final String TAG = "DBAdapter";
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "SONGLISTDB";
	public static final String DATABASE_TABLE_LIST = "songlist";
	public static final String DATABASE_TABLE_SONG = "songitem";
	// 列表表的列名
	public static final String KEY_LIST_ID = "_id";
	public static final String KEY_LIST_NAME = "listname";
	// 歌曲表列表
	public static final String KEY_SONG_ID = "_id";
	public static final String KEY_SONG_NAME = "songname";
	public static final String KEY_SONG_ONLY_ID = "songid";
	public static final String KEY_FROM_LIST = "listid";

	private static final String LIST_TALBE_CREATE = "create table "
			+ DATABASE_TABLE_LIST + "( " + KEY_LIST_ID
			+ " integer primary key autoincrement, " + "" + KEY_LIST_NAME
			+ " text not null);";

	private static final String SONG_TABLE_CREATE = "create table "
			+ DATABASE_TABLE_SONG + "( " + KEY_SONG_ID
			+ " integer primary key autoincrement, " + "" + KEY_SONG_NAME
			+ " text not null," + KEY_SONG_ONLY_ID + " bigint not null,"
			+ KEY_FROM_LIST + " bigint not null);";

	private final Context context;
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public SongListDBAdapter(Context cxt) {
		this.context = cxt;
		DBHelper = new DatabaseHelper(context);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(LIST_TALBE_CREATE);
				db.execSQL(SONG_TABLE_CREATE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.wtf(TAG, "Upgrading database from version " + oldVersion
					+ "to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + LIST_TALBE_CREATE + "");
			db.execSQL("DROP TABLE IF EXISTS " + SONG_TABLE_CREATE + "");
			onCreate(db);
		}
	}

	public SongListDBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	public void createDB() {
		db = DBHelper.getWritableDatabase();
		DBHelper.close();
	}

	public void close() {
		DBHelper.close();
	}

	public long insertSongList(String name) {
		Cursor mCursor = hasSonglist(name);
		if(!mCursor.moveToFirst()){
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_LIST_NAME, name);
			return db.insert(DATABASE_TABLE_LIST, null, initialValues);
		}
		return -1;
	}

	public boolean updateSongListItem(int rowId, String name) {
		ContentValues args = new ContentValues();
		args.put(KEY_LIST_NAME, name);
		return db.update(DATABASE_TABLE_LIST, args, KEY_LIST_ID + "=" + rowId,
				null) > 0;
	}
	
	public Cursor hasSonglist(String name){
		Cursor mCursor = db
				.query(true, DATABASE_TABLE_LIST, new String[] { KEY_LIST_ID },
						KEY_LIST_NAME + "='" + name + "'", null, null, null,
						null, null);
		return mCursor;
	}

	public int getSonglistID4Name(String name) {
		Cursor mCursor = hasSonglist(name);
		if (mCursor.moveToFirst()) {
			return mCursor.getInt(mCursor.getColumnIndexOrThrow(KEY_LIST_ID));
		}
		return -1;
	}

	public long insertSongItem(String songName, long songId, long listId) {
		// 检查是否包含同一首歌曲
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_SONG_NAME, songName);
		initialValues.put(KEY_SONG_ONLY_ID, songId);
		initialValues.put(KEY_FROM_LIST, listId);
		return db.insert(DATABASE_TABLE_SONG, null, initialValues);
	}

	public boolean updateSongItem(int rowId, String songName, long songId,
			long listId) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_SONG_NAME, songName);
		initialValues.put(KEY_SONG_ONLY_ID, songId);
		initialValues.put(KEY_FROM_LIST, listId);
		return db.update(DATABASE_TABLE_SONG, initialValues, KEY_SONG_ID + "="
				+ rowId, null) > 0;
	}

	public Cursor getAllSongs4ListName(String listName) throws SQLException {
		int id = getSonglistID4Name(listName);
		Cursor mCursor = db.query(true, DATABASE_TABLE_SONG, null,
				KEY_FROM_LIST + "=" + id, null, null, null, "_id DESC", null);
		return mCursor;
	}

	public boolean deleteItem(String tableName, String where) {
		return db.delete(tableName, where, null) > 0;
	}

	public Cursor getAllItem(String tableName) {
		return db.query(tableName, null, null, null, null, null, "_id DESC");
	}
}
