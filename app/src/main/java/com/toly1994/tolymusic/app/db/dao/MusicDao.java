package com.toly1994.tolymusic.app.db.dao;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

/**
 * 系统音乐数据库相关操作
 * @author lbRoNG
 */
public abstract class MusicDao {
	public static final int SONG_LOADER_ID = 1;
	private static final Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;;
	private Activity context;
	private LoaderManager manager;
	private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks;

	public MusicDao(Activity context) {
		this.context = context;
	}

	/**
	 * 根据类型读取音乐
	 */
	public void getMusic4Type(int loderId) {
		manager = context.getLoaderManager();
		// loaderCallbacks实现
		loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
			@Override
			public void onLoaderReset(Loader<Cursor> loader) {
				onResetLoader(loader);
			}

			// cursor会自动关闭
			@Override
			public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
				onFinishedLoader(loader,data,manager);
			}

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				return onLoaderCreate(contentUri,id,args);
			}
		};
		
		// 初始化loader
		manager.initLoader(loderId, null, loaderCallbacks);
	}

	public abstract Loader<Cursor> onLoaderCreate(Uri contentUri,int id, Bundle args);

	public abstract void onFinishedLoader(Loader<Cursor> loader, Cursor data,LoaderManager manager);

	public abstract void onResetLoader(Loader<Cursor> loader);
}
