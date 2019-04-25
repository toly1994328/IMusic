package com.toly1994.tolymusic.itf.impl;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;
import com.toly1994.tolymusic.four.activity.SearchActivity;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.DeleteInnerUI;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;

import java.util.ArrayList;

/**
 * 通用的更多菜单实现
 * @author lbRoNG
 */
public class MoreMenuImpl{
	@SuppressWarnings("serial")
	public static void MoreMenuAsSong(Context context,ArrayList<Song> data,
			RecyclerView.Adapter<?> mAdapter,int fromViewPositon,int clickViewPosition) {
		final Song clickItem = (Song) data.get(fromViewPositon);
		// 处理菜单点击选项
		switch(clickViewPosition){
			case 0:
				// 播放
				MoreMenuUtils.playSongIntent(context, data, clickItem);
				break;
			case 1:
				// 下一首播放
				if(PlayingService.playList != null && PlayingService.playingSong != null){
					MoreMenuUtils.swapMusicUnderPlayingSong(clickItem);
					Toast.makeText(context, "播放队列已更新", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(context, "没有播放队列", Toast.LENGTH_SHORT).show();
				}
				break;
			case 2:
				// 添加进播放队列
				if(MoreMenuUtils.addSongsToPlayList(new ArrayList<Song>(){{add(clickItem);}})){
					Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(context, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
				}
				break;
			case 3:
				// 添加歌曲到歌单
				MoreMenuUtils.addSongToSonglist(SearchActivity.context, new ArrayList<Song>() {
					{
						add(clickItem);
					}
				});
				break;
			case 4:
				// 歌手的作品
				MoreMenuUtils.aboutArtist(context,clickItem.getAlbum().getArtist());
				break;
			case 5:
				// 设置为铃声
				if(MoreMenuUtils.setVoice(context, clickItem.getUrl())){
					Toast.makeText(context, "设置铃声成功", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(context, "设置铃声失败", Toast.LENGTH_SHORT).show();
				}
				break;
			case 6:
				// 如果删除歌曲是当前播放的歌曲,提示用户
				if(!clickItem.equals(PlayingService.playingSong)){
					// 删除歌曲
					int deleteCount = MoreMenuUtils.deleteMp3(context,new ArrayList<Song>(){{add(clickItem);}});
					if(deleteCount != 0){
						// 刷新列表
						if(mAdapter != null){
							data.remove(fromViewPositon);
							mAdapter.notifyDataSetChanged();
						}
						// 发送广播通知刷新列表,删除有关该歌曲的全部信息
						MoreMenuUtils.deleteIntent(context, DeleteInnerUI.OtherUI,
								new ArrayList<Song>(){{add(clickItem);}});
						Toast.makeText(context, "已删除"+deleteCount+"首歌曲", Toast.LENGTH_SHORT).show();
					}else{
						Toast.makeText(context, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
					}
				}else{
					Toast.makeText(context, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
				}
				break;
		}
	}
	
	public static void MoreMenuAsAlbum(Context context,ArrayList<Album> data,
			RecyclerView.Adapter<?> mAdapter,int fromViewPositon,int clickViewPosition) {
		Album albumItem = data.get(fromViewPositon);
		ArrayList<Song> albumSongs = albumItem.getSongs();
		// 处理菜单点击选项
		switch(clickViewPosition){
		case 0:
			// 播放
			MoreMenuUtils.playSongIntent(context, albumSongs, albumSongs.get(0));
			break;
		case 1:
			// 添加到播放队列
			if(MoreMenuUtils.addSongsToPlayList(albumSongs)){
				Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(context, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
			}
			break;
		case 2:
			// 添加专辑的全部歌曲到歌单
			MoreMenuUtils.addSongToSonglist(SearchActivity.context, albumSongs);
			break;
		case 3:
			// 歌手作品
			MoreMenuUtils.aboutArtist(context,albumItem.getArtist());
			break;
		case 4:
			// 删除
			if(!albumSongs.contains(PlayingService.playingSong)){
				int deleteCount = MoreMenuUtils.deleteMp3(context,albumSongs);
				if(deleteCount != 0){
					if(mAdapter != null){
						data.remove(fromViewPositon);
						mAdapter.notifyDataSetChanged();
					}
					// 发送广播
					MoreMenuUtils.deleteIntent(context, DeleteInnerUI.OtherUI, albumSongs);
					Toast.makeText(context, "已删除"+deleteCount+"首歌曲", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(context, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
				}
			}else{
				Toast.makeText(context, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}
	
	public static void MoreMenuAsArtist(Context context,ArrayList<Artist> data,
			RecyclerView.Adapter<?> mAdapter,int fromViewPositon,int clickViewPosition) {
		Artist artistItem = data.get(fromViewPositon);
		ArrayList<Song> artistSongs = artistItem.getInfo().getSongs();
		// 处理菜单点击选项
		switch (clickViewPosition) {
		case 0:
			// 播放
			MoreMenuUtils.playSongIntent(context, artistSongs, artistSongs.get(0));
			break;
		case 1:
			if(MoreMenuUtils.addSongsToPlayList(artistSongs)){
				Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(context, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
			}
			break;
		case 2:
			// 添加到歌单
			MoreMenuUtils.addSongToSonglist(SearchActivity.context,artistSongs);
			break;
		case 3:
			// 删除
			if(!artistSongs.contains(PlayingService.playingSong)){
				int deleteCount = MoreMenuUtils.deleteMp3(context,artistSongs);
				if(deleteCount != 0){
					if(mAdapter != null){
						data.remove(fromViewPositon);
						mAdapter.notifyDataSetChanged();
					}
					// 发送广播
					MoreMenuUtils.deleteIntent(context, DeleteInnerUI.OtherUI, artistSongs);
					Toast.makeText(context, "已删除"+deleteCount+"首歌曲", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(context, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
				}
			}else{
				Toast.makeText(context, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}
}
