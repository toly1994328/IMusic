package com.toly1994.tolymusic.app.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.toly1994.tolymusic.*;
import com.toly1994.tolymusic.four.activity.AlbumHomeActivity;
import com.toly1994.tolymusic.four.activity.ArtistHomeActivity;
import com.toly1994.tolymusic.four.activity.HomeActivity;
import com.toly1994.tolymusic.app.db.dao.SongListDao;
import com.toly1994.tolymusic.app.domain.*;
import com.toly1994.tolymusic.fragment.ArtistFragment;
import com.toly1994.tolymusic.fragment.SongListFragment;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.four.service.PlayingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class MoreMenuUtils {
	// 歌单列表
	public static PopupWindow chooseSonglistPopupWindow;
	// 歌曲的菜单列表
	public final static List<Integer> moreMenusAsSong = new ArrayList<Integer>() {
		{
			add( R.string.more_menu_play);
			add(R.string.more_menu_play_next);
			add(R.string.more_menu_add_list);
			add(R.string.more_menu_add_songlist);
			add(R.string.more_menu_about_artist);
			add(R.string.more_menu_set_ringo);
			add(R.string.more_menu_delete);
		}
	};
	// 专辑的菜单列表
	public final static List<Integer> moreMenusAsAlbum = new ArrayList<Integer>() {
		{
			add(R.string.more_menu_play);
			add(R.string.more_menu_add_list);
			add(R.string.more_menu_add_songlist);
			add(R.string.more_menu_about_artist);
			add(R.string.more_menu_delete);
		}
	};
	// 歌手的菜单列表
	public final static List<Integer> moreMenusAsArtist = new ArrayList<Integer>() {
		{
			add(R.string.more_menu_play);
			add(R.string.more_menu_add_list);
			add(R.string.more_menu_add_songlist);
			add(R.string.more_menu_delete);
		}
	};
	// 歌单的菜单列表
	public final static List<Integer> moreMenusAsSongList = new ArrayList<Integer>() {
		{
			add(R.string.more_menu_play);
			add(R.string.more_menu_add_list);
			add(R.string.more_menu_edit);
			add(R.string.more_menu_delete);
		}
	};

	// 播放列表的菜单列表
	public final static List<Integer> moreMenusAsPlayList = new ArrayList<Integer>() {
		{
			add(R.string.more_menu_play_next);
			add(R.string.more_menu_add_songlist);
			add(R.string.more_menu_about_artist);
			add(R.string.more_menu_set_ringo);
			add(R.string.more_menu_delete);
		}
	};

	/**
	 * 跳转到选中歌手的作品主页
	 * @param context
	 * @param item
	 */
	public static void aboutArtist(Context context, Artist item) {
		Intent intent = new Intent(context, ArtistHomeActivity.class);
		ArrayList<Object> singers = ArtistFragment.getArtistSortList();
		if (singers != null && singers.size() != 0) {
			intent.putExtra("artistItemPos", singers.indexOf(item));
			context.startActivity(intent);
		} else {
			Toast.makeText(context, "找不到相关歌手信息", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 播放歌曲的intent
	 * @param playList 播放队列
	 * @param playingSong 要播放的第一首音乐
	 */
	public static void playSongIntent(Context context, ArrayList<Song> playList, Song playingSong){
		Intent send = new Intent(context,PlayingService.class);
		send.putExtra("action", PlayingService.INTENT_START_MUSIC);
		send.putParcelableArrayListExtra("playing_list", playList);
		send.putExtra("playing_song", playingSong);
		context.startService(send);
	}
	
	/**
	 * 打开专辑的intent
	 * @param item
	 */
	public static void aboutAlbumIntent(Context context,Album item){
		Intent intent = new Intent(context,AlbumHomeActivity.class);
		intent.putExtra("albumItem", item);
		intent.putExtra("artistAsAlbum", item.getArtist().getSingerName());
		context.startActivity(intent);
	}
	
	/**
	 * 添加歌曲到播放队列
	 * @param songs
	 * @return
	 */
	public static boolean addSongsToPlayList(List<Song> songs){
		boolean result = false;
		// 添加到播放队列
		for(Song temp : songs){
			if(!PlayingService.playList.contains(temp)){
				// 添加到播放列表
				PlayingService.playList.add(temp);
				// 随机队列也要添加
				PlayingService.playListOfRandom.add(temp);
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * 添加歌曲到播放列表
	 * @param context
	 * @param item
	 * @return
	 */
	public static void addSongToSonglist(final Activity context,
			final ArrayList<Song> item) {
		new AsyncTask<Integer, Integer, List<SongList>>() {
			@Override
			protected List<SongList> doInBackground(Integer... params) {
				return SongListDao.getSongList(context);
			}

			@Override
			protected void onPostExecute(final List<SongList> result) {
				chooseSonglistPopupWindow = DialogUtils
						.showSongListPopupWindows(context, result,
								new OnBaseClickListener() {
									@Override
									public void onClick(View view, int position) {
										// 发送广播通知添加操作
										Intent send = new Intent(SongListFragment.SONGLIST_CHANGE_INTENT);
										send.putExtra("actiontype", SongListFragment.SONGLIST_INSTER);
										send.putExtra("songlist", item);
										send.putExtra("songlistname", result.get(position).getListName());
										context.sendBroadcast(send);
									}
								});
			}
		}.execute(0);
	}
	
	/**
	 * 播放到下一首
	 * @param willSwapSong 要播放到下一首的歌曲位置
	 */
	public static void swapMusicUnderPlayingSong(Song willSwapSong){
		// 获取要交换的歌曲
		int fromViewPositon = PlayingService.playList.indexOf(willSwapSong);
		// 判断要交换的歌曲是否在播放队列中已存在
		if(PlayingService.playList.contains(willSwapSong)){
			// 存在就删除
			PlayingService.playList.remove(fromViewPositon);
		}
		// 获取当前播放的歌曲在新列表的位置
		int playingSongPostion = PlayingService.playList.indexOf(PlayingService.playingSong);
		// 添加要交换的歌曲到当前播放歌曲的下一个位置
		PlayingService.playList.add(playingSongPostion+1,willSwapSong);
	}
	
	/**
	 * 将歌曲设置为铃声
	 * @param context
	 * @param path
	 */
	public static boolean setVoice(Context context, String path) {
		ContentValues cv = new ContentValues();
		Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
		// 查询音乐文件在媒体库是否存在
		Cursor cursor = context.getContentResolver().query(uri, null,
				MediaStore.MediaColumns.DATA + "=?", new String[] { path },
				null);
		if (cursor.moveToFirst() && cursor.getCount() > 0) {
			String _id = cursor.getString(0);
			cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
			cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
			cv.put(MediaStore.Audio.Media.IS_ALARM, false);
			cv.put(MediaStore.Audio.Media.IS_MUSIC, false);

			// 把需要设为铃声的歌曲更新铃声库
			context.getContentResolver().update(uri, cv,
					MediaStore.MediaColumns.DATA + "=?", new String[] { path });
			Uri newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));

			RingtoneManager.setActualDefaultRingtoneUri(context,
					RingtoneManager.TYPE_RINGTONE, newUri);
			return true;
		}
		return false;
	}
	
	/**
	 * 从sd卡删除音乐文件
	 * @param item
	 */
	public static int deleteMp3(Context context,ArrayList<Song> item) {
		int deleteCount = 0;
		for(Song delete : item){
			// sdcard中删除MP3文件
			File file = new File(delete.getUrl());
			if (file.exists()) {
				// 删除数据库中的记录
				context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
			            MediaStore.Audio.Media._ID + "=" + delete.getSongId(), 
			            null);
				file.delete();
				deleteCount++;
			}
		}
		return deleteCount;
	}
	
	/**
	 * 删除音乐的广播
	 * @param context
	 * @param innerUI 在哪个UI内执行的操作
	 * @param deleteList 要删除的歌曲列表
	 */
	public static void deleteIntent(Context context, DeleteInnerUI innerUI, ArrayList<Song> deleteList){
		Intent deleteIntent = new Intent(HomeActivity.INTENT_MUSIC_DELETE);
		deleteIntent.putExtra("deletelist", deleteList);
		deleteIntent.putExtra("innerUI", innerUI);
		context.sendBroadcast(deleteIntent);
	}
	
	/**
	 * 删除歌手条目并且刷新列表
	 * @param listAsSort 包含字母排序的集合
	 * @param clickItem 被点击的实例
	 * @param fromViewPositon 实例在列表的位置
	 */
	public static void deleteArtistAndRefresh(RecyclerView.Adapter<?> mAdapter,List<Object> listAsSort
			,Object clickItem,int fromViewPositon){
		// 获取被点击实例的上一个位置的类型
		Object lastObj = listAsSort.get(fromViewPositon-1);
		if(lastObj instanceof String){
			// 上一个类型是字母
			// 再判断下一个实例是不是不存在或者也是字母,这样就能判断被点击的实例已是该字母开头的最后一项,删除字母
			if(listAsSort.size() == fromViewPositon + 1){
				listAsSort.remove(fromViewPositon-1);
				mAdapter.notifyItemRemoved(fromViewPositon-1);
			}else{
				Object nextObj = listAsSort.get(fromViewPositon+1);
				if(nextObj instanceof String){
					listAsSort.remove(fromViewPositon-1);
					mAdapter.notifyItemRemoved(fromViewPositon-1);
				}
			}
		}
		// 删除集合数据并刷新
		listAsSort.remove(clickItem);
		mAdapter.notifyItemRemoved(fromViewPositon);
		mAdapter.notifyItemRangeChanged(fromViewPositon-1, mAdapter.getItemCount());
	}
}
