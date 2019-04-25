package com.toly1994.tolymusic.four.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.toly1994.tolymusic.app.MusicApplication;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.adapter.MoreMenuBaseAdapter;
import com.toly1994.tolymusic.app.domain.Album;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.DeleteInnerUI;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.fragment.ArtistFragment;
import com.toly1994.tolymusic.itf.OnBaseClickListener;
import com.toly1994.tolymusic.itf.OnMoreMenuItemClickListener;
import com.toly1994.tolymusic.itf.impl.BaseMoreMenuClickListenerImpl;
import com.toly1994.tolymusic.four.service.PlayingService;
import com.toly1994.tolymusic.app.utils.DensityUtils;
import com.toly1994.tolymusic.app.utils.ImageUtils;
import com.toly1994.tolymusic.app.utils.MTextUtils;
import com.toly1994.tolymusic.app.utils.MoreMenuUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
@SuppressWarnings("serial")
public class ArtistHomeActivity extends AppCompatActivity {
	private RecyclerView artist_home_songs,artist_home_albums;
	private AlbumAsArtistAdapter albumAdapter;
	private SongAsArtistAdapter songAdapter;
	private List<Album> albums = new ArrayList<>();
	private List<Song> songs = new ArrayList<>();
	private int pos;
	private Toolbar bar_home;
	private BaseMoreMenuClickListenerImpl impl,impl2;
	private String searchKey = "";
	private int themeRgb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_artist_home);
		setViewComponent();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		MusicApplication.launchActivity = ArtistHomeActivity.class;
	}
	
	@Override
	public void onBackPressed() {
		if(impl!=null && impl.popupWindowIsShow()){
			impl.popupWindowDismiss();
		}else if(MoreMenuUtils.chooseSonglistPopupWindow!=null
				&& MoreMenuUtils.chooseSonglistPopupWindow.isShowing()){
			MoreMenuUtils.chooseSonglistPopupWindow.dismiss();
		}else{
			finish();
		}
	}

	private void setViewComponent() {
		bar_home = (Toolbar) findViewById(R.id.bar_home);
		artist_home_songs = (RecyclerView) findViewById(R.id.artist_home_songs);
		artist_home_albums = (RecyclerView) findViewById(R.id.artist_home_albums);
		// 设置横向滚动的专辑列表
		LinearLayoutManager horizontalManager = new LinearLayoutManager(this,
				LinearLayoutManager.HORIZONTAL, false);
		artist_home_albums.setLayoutManager(horizontalManager);
		albumAdapter = new AlbumAsArtistAdapter();
		artist_home_albums.setAdapter(albumAdapter);
		albumAdapter.setOnItemClickListener(new OnBaseClickListener() {
					@Override
					public void onClick(View view, int position) {
						Intent intent = new Intent(ArtistHomeActivity.this,AlbumHomeActivity.class);
						Album item = albums.get(position);
						intent.putExtra("albumItem", item);
						intent.putExtra("artistAsAlbum", item.getArtist()
								.getSingerName());
						ArtistHomeActivity.this.startActivity(intent);
					}
				});
		// 设置歌曲列表
		artist_home_songs.setLayoutManager(new LinearLayoutManager(this));
		songAdapter = new SongAsArtistAdapter();
		artist_home_songs.setAdapter(songAdapter);
		songAdapter.setOnItemClickListener(new OnBaseClickListener() {
			@Override
			public void onClick(View view, int position) {
				MoreMenuUtils.playSongIntent(ArtistHomeActivity.this, (ArrayList<Song>)songs, songs.get(position));
			}
		});
		// 获取提供菜单选项的内容适配器
		MoreMenuBaseAdapter songMenuAdapter = new MoreMenuBaseAdapter(this,MoreMenuUtils.moreMenusAsSong);
		// 把内容给点击事件的实现类,完成点击跳出对应菜单
		impl = new BaseMoreMenuClickListenerImpl(this,songMenuAdapter);
		// 给菜单内的选项添加点击事件
		impl.setOnItemClickListener(new OnMoreMenuItemClickListener() {
			@Override
			public void onClick(View fromView, View clickView, int fromViewPositon,
					int clickViewPosition) {
				// 处理菜单点击选项
				final Song clickItem = songs.get(fromViewPositon);
				// 处理菜单点击选项
				switch(clickViewPosition){
					case 0:
						MoreMenuUtils.playSongIntent(ArtistHomeActivity.this, (ArrayList<Song>)songs, clickItem);
						break;
					case 1:
						// 下一首播放
						if(PlayingService.playList != null && PlayingService.playingSong != null){
							MoreMenuUtils.swapMusicUnderPlayingSong(clickItem);
							Toast.makeText(ArtistHomeActivity.this, "播放队列已更新", Toast.LENGTH_SHORT).show();
						}else{
							Toast.makeText(ArtistHomeActivity.this, "没有播放队列", Toast.LENGTH_SHORT).show();
						}
						break;
					case 2:
						// 添加进播放队列
						if(MoreMenuUtils.addSongsToPlayList(new ArrayList<Song>(){{add(clickItem);}})){
							Toast.makeText(ArtistHomeActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
						}else{
							Toast.makeText(ArtistHomeActivity.this, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
						}
						break;
					case 3:
						// 添加歌曲到歌单
						addSongToSonglist(fromViewPositon);
						break;
					case 4:
						Toast.makeText(ArtistHomeActivity.this, "已是当前歌手", Toast.LENGTH_SHORT).show();
						break;
					case 5:
						// 设置为铃声
						if(MoreMenuUtils.setVoice(ArtistHomeActivity.this, clickItem.getUrl())){
							Toast.makeText(ArtistHomeActivity.this, "设置铃声成功", Toast.LENGTH_SHORT).show();
						}else{
							Toast.makeText(ArtistHomeActivity.this, "设置铃声失败", Toast.LENGTH_SHORT).show();
						}
						break;
					case 6:
						if(!songs.contains(PlayingService.playingSong)){
							int deleteCount = MoreMenuUtils.deleteMp3(ArtistHomeActivity.this,new ArrayList<Song>(){{
								add(clickItem);
							}});
							if(deleteCount != 0){
								// 刷新列表
								songs.remove(clickItem);
								songAdapter.notifyDataSetChanged();
								// 获取被删除的歌曲属于哪个专辑
								int index = albums.indexOf(clickItem.getAlbum());
								Album deleteAlbum = albums.get(index);
								deleteAlbum.getSongs().remove(clickItem);
								if(deleteAlbum.getSongs().size() == 0){
									albums.remove(deleteAlbum);
								}
								albumAdapter.notifyDataSetChanged();
								if(songs.size() == 0){
									setNotInfo();
								}
								// 发送广播
								MoreMenuUtils.deleteIntent(ArtistHomeActivity.this, DeleteInnerUI.OtherUI, new ArrayList<Song>(){{
									add(clickItem);
								}});
								Toast.makeText(ArtistHomeActivity.this, "已删除"+deleteCount+"首歌曲", Toast.LENGTH_SHORT).show();
							}else{
								Toast.makeText(ArtistHomeActivity.this, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
							}
						}else{
							Toast.makeText(ArtistHomeActivity.this, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
						}
						break;
				}
				// 隐藏菜单
				impl.popupWindowDismiss();
			}
		});
		// 设置菜单按钮的点击事件
		songAdapter.setOnMoreMenuClickListener(impl);
		
		// 获取提供菜单选项的内容适配器
		MoreMenuBaseAdapter albumMenuAdapter = new MoreMenuBaseAdapter(this,MoreMenuUtils.moreMenusAsAlbum);
		// 把内容给点击事件的实现类,完成点击跳出对应菜单
		impl2 = new BaseMoreMenuClickListenerImpl(this,albumMenuAdapter);
		// 给菜单内的选项添加点击事件
		impl2.setOnItemClickListener(new OnMoreMenuItemClickListener() {
			@Override
			public void onClick(View fromView, View clickView, int fromViewPositon,
					int clickViewPosition) {
				ArrayList<Song> albumsSong = albums.get(fromViewPositon).getSongs();
				// 处理菜单点击选项
				switch(clickViewPosition){
				case 0:
					// 播放
					MoreMenuUtils.playSongIntent(ArtistHomeActivity.this, albumsSong, albumsSong.get(0));
					break;
				case 1:
					// 添加到播放队列
					if(MoreMenuUtils.addSongsToPlayList(albumsSong)){
						Toast.makeText(ArtistHomeActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
					}else{
						Toast.makeText(ArtistHomeActivity.this, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
					}
					break;
				case 2:
					// 添加专辑的全部歌曲到歌单
					MoreMenuUtils.addSongToSonglist(ArtistHomeActivity.this, albumsSong);
					break;
				case 3:
					// 歌手作品
					Toast.makeText(ArtistHomeActivity.this, "已是当前歌手", Toast.LENGTH_SHORT).show();
					break;
				case 4:
					// 删除
					if(!albumsSong.contains(PlayingService.playingSong)){
						int deleteCount = MoreMenuUtils.deleteMp3(ArtistHomeActivity.this,albumsSong);
						if(deleteCount != 0){
							// 删除集合刷新列表
							albums.remove(fromViewPositon);
							albumAdapter.notifyDataSetChanged();
							songs.removeAll(albumsSong);
							songAdapter.notifyDataSetChanged();
							if(songs.size() == 0){
								setNotInfo();
							}
							// 发送广播
							MoreMenuUtils.deleteIntent(ArtistHomeActivity.this, DeleteInnerUI.OtherUI, albumsSong);
							Toast.makeText(ArtistHomeActivity.this, "已删除"+deleteCount+"首歌曲", Toast.LENGTH_SHORT).show();
						}else{
							Toast.makeText(ArtistHomeActivity.this, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
						}
					}else{
						Toast.makeText(ArtistHomeActivity.this, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
					}
					break;
				}
				// 隐藏菜单
				impl2.popupWindowDismiss();
			}
		});
		
		albumAdapter.setOnMoreMenuClickListener(impl2);
		
		// 设置菜单按钮的点击事件
		getSendDataAndSetData();
	}
	
	/**
	 * 设置没有歌曲信息时候的界面
	 */
	private void setNotInfo() {
		findViewById(R.id.tv_all_album).setVisibility(View.GONE);
		findViewById(R.id.tv_all_song).setVisibility(View.GONE);
		artist_home_songs.setVisibility(View.GONE);
		artist_home_albums.setVisibility(View.GONE);
		findViewById(R.id.vs_not_info).setVisibility(View.VISIBLE);
	}
	
	private void addSongToSonglist(final int fromViewPositon){
		List<Song> allsongs = HomeActivity.getSongs();
		final Song temp = allsongs.get(allsongs.indexOf(songs.get(fromViewPositon)));
		MoreMenuUtils.addSongToSonglist(this, new ArrayList<Song>(){{add(temp);}});
	}
	
	private void getSendDataAndSetData() {
		pos = getIntent().getIntExtra("artistItemPos", -1);
		if (pos != -1) {
			Artist artist = (Artist) ArtistFragment.getArtistSortList()
					.get(pos);
			albums = artist.getInfo().getAlbums();
			songs = artist.getInfo().getSongs();
			setActionBar(artist);
		}
	}

	private void setActionBar(Artist artist) {
		bar_home.setTitle(artist.getSingerName());
		bar_home.setBackgroundColor(themeRgb = artist.getCoverRgb());
		getWindow().setStatusBarColor(ImageUtils.colorBurn(artist.getCoverRgb()));
		setSupportActionBar(bar_home);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	private class AlbumAsArtistAdapter extends
			RecyclerView.Adapter<AlbumAsArtistAdapter.AlbumAsArtistHolder> {
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

		public class AlbumAsArtistHolder extends RecyclerView.ViewHolder {
			private View itemView;
			private ImageView iv_album,iv_more;
			private TextView tv_album_name;

			public AlbumAsArtistHolder(View itemView) {
				super(itemView);
				this.itemView = itemView;
				tv_album_name = (TextView) itemView
						.findViewById(R.id.tv_album_name);
				iv_album = (ImageView) itemView.findViewById(R.id.iv_album);
				iv_more = (ImageView) itemView.findViewById(R.id.iv_more);
			}

			public View getItemView() {
				return itemView;
			}

		}

		@Override
		public int getItemCount() {
			return albums.size();
		}

		@Override
		public void onBindViewHolder(AlbumAsArtistHolder holder, final int pos) {
			Album item = albums.get(pos);
			Bitmap cover = item.getCover();
			if (cover == null) {
				cover = ImageUtils.getArtwork(ArtistHomeActivity.this,songs.get(0)
						.getTitle(), songs.get(0).getSongId(), item
						.getAlbumId(), true);
				item.setCover(cover);
			}
			holder.tv_album_name.setText(MTextUtils.setTextColorByKey(
					item.getAlbumName(), searchKey, themeRgb));
			holder.iv_album.setImageBitmap(cover);

			holder.getItemView().setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mOnItemClickListener != null) {
						mOnItemClickListener.onClick(v, pos);
					}
				}
			});
			
			holder.iv_more.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mOnMoreMenuClickListener != null) {
						mOnMoreMenuClickListener.onClick(v, pos);
					}
				}
			});
		}

		@Override
		public AlbumAsArtistHolder onCreateViewHolder(ViewGroup parent, int type) {
			View rootView = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.album_small_item, parent, false);
			return new AlbumAsArtistHolder(rootView);
		}
	}

	private class SongAsArtistAdapter extends
			RecyclerView.Adapter<SongAsArtistAdapter.SongAsArtistHolder> {
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

		class SongAsArtistHolder extends RecyclerView.ViewHolder {
			private View itemView, view_line;
			private TextView tv_music_name, tv_music_albums;
			private ImageView iv_music_icon,iv_more;

			public SongAsArtistHolder(View itemView) {
				super(itemView);
				this.itemView = itemView;
				tv_music_name = (TextView) itemView.findViewById(R.id.tv_music_name);
				tv_music_albums = (TextView) itemView.findViewById(R.id.tv_music_albums);
				iv_music_icon = (ImageView) itemView.findViewById(R.id.iv_music_icon);
				iv_more = (ImageView) itemView.findViewById(R.id.iv_more);
				view_line = (View) itemView.findViewById(R.id.view_line);
			}

			public View getItemView() {
				return itemView;
			}

		}

		@Override
		public int getItemCount() {
			return songs.size();
		}

		@Override
		public void onBindViewHolder(SongAsArtistHolder holder, final int pos) {
			Song item = songs.get(pos);
			Bitmap cover = item.getAlbum().getCover();
			if (cover == null) {
				cover = ImageUtils.getArtwork(ArtistHomeActivity.this, item.getTitle(),
						item.getSongId(), item.getAlbum().getAlbumId(), true);
				item.getAlbum().setCover(cover);
			}
			int widthAndHeight = DensityUtils.dp2px(ArtistHomeActivity.this,
					40);
			RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
					widthAndHeight, widthAndHeight);
			param.addRule(RelativeLayout.CENTER_VERTICAL);
			holder.iv_music_icon.setLayoutParams(param);
			holder.iv_music_icon.setImageBitmap(cover);
			holder.tv_music_name.setText(MTextUtils.setTextColorByKey(
					item.getTitle(), searchKey, themeRgb));
			holder.tv_music_albums.setText(item.getAlbum().getAlbumName());

			if (pos == songs.size() - 1) {
				holder.view_line.setVisibility(View.INVISIBLE);
			}

			holder.getItemView().setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mOnItemClickListener != null) {
						mOnItemClickListener.onClick(v, pos);
					}
				}
			});
			
			holder.iv_more.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mOnMoreMenuClickListener != null) {
						mOnMoreMenuClickListener.onClick(v, pos);
					}
				}
			});
		}

		@Override
		public SongAsArtistHolder onCreateViewHolder(ViewGroup parent, int type) {
			View rootView = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.music_item, parent, false);
			return new SongAsArtistHolder(rootView);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search_menu, menu);
		// 获取search View
		SearchView action_search = (SearchView) menu.findItem(
				R.id.action_search).getActionView();
		action_search.setQueryHint(getString(R.string.action_search_part_hint));
		action_search.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String key) {
				return true;
			}

			@Override
			public boolean onQueryTextChange(String key) {
				if(key != null && key.length() > 0){
					for(int i = 0;i < songs.size();i++){
						Song item = songs.get(i);
						if(item.getTitle().toLowerCase(Locale.ENGLISH)
								.contains(key.toLowerCase(Locale.ENGLISH))){
							searchKey = key;
							songAdapter.notifyItemChanged(i);
							artist_home_songs.scrollToPosition(i);
							break;
						}
					}
					for(int i = 0;i < albums.size();i++){
						Album item = albums.get(i);
						if(item.getAlbumName().toLowerCase(Locale.ENGLISH)
								.contains(key.toLowerCase(Locale.ENGLISH))){
							searchKey = key;
							albumAdapter.notifyItemChanged(i);
							artist_home_albums.scrollToPosition(i);
							break;
						}
					}
				}else{
					searchKey = "";
					songAdapter.notifyDataSetChanged();
					albumAdapter.notifyDataSetChanged();
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
		case R.id.action_search:
			Intent search_intent = new Intent(this, SearchActivity.class);
			startActivity(search_intent);
			break;
		case android.R.id.home:
			finish();
			break;
		case R.id.action_play:
			MoreMenuUtils.playSongIntent(this, (ArrayList<Song>)songs, songs.get(0));
			break;
		case R.id.action_random_album:
			getSharedPreferences("playconfig", Context.MODE_PRIVATE).edit().putInt(
					"play_method_id", PlayMusicActivity.PLAY_METHOD_RANDOM).apply();
			MoreMenuUtils.playSongIntent(this, (ArrayList<Song>)songs, songs.get(0));
			break;
		case R.id.action_add_list:
			if(MoreMenuUtils.addSongsToPlayList(songs)){
				Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(this, "播放队列已存在该歌曲", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.action_add_songlist:
			MoreMenuUtils.addSongToSonglist(this, (ArrayList<Song>)songs);
			break;
		case R.id.action_about_artist:
			// 歌手作品
			Toast.makeText(this, "已是当前歌手", Toast.LENGTH_SHORT).show();
			break;
		case R.id.action_delete:
			if(!songs.contains(PlayingService.playingSong)){
				int deleteCount = MoreMenuUtils.deleteMp3(this,(ArrayList<Song>)songs);
				if(deleteCount != 0){
					// 发送广播
					MoreMenuUtils.deleteIntent(this, DeleteInnerUI.OtherUI, (ArrayList<Song>)songs);
					Toast.makeText(this, "已删除"+deleteCount+"首歌曲", Toast.LENGTH_SHORT).show();
					finish();
				}else{
					Toast.makeText(this, "未知错误,删除失败", Toast.LENGTH_SHORT).show();
				}
			}else{
				Toast.makeText(this, "该歌曲正在播放,无法删除", Toast.LENGTH_SHORT).show();
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
