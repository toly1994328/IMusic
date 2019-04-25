package com.toly1994.tolymusic.app.domain;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class ArtistMusicInfo implements Parcelable{
	private ArrayList<Song> songs;
	private ArrayList<Album> albums;

	public ArtistMusicInfo() {
		songs = new ArrayList<>();
		albums = new ArrayList<>();
	}

	public ArtistMusicInfo(ArrayList<Song> songs, ArrayList<Album> albums) {
		super();
		this.songs = songs;
		this.albums = albums;
	}

	public ArrayList<Song> getSongs() {
		return songs == null ? new ArrayList<Song>() : songs;
	}

	public ArrayList<Album> getAlbums() {
		return albums == null ? new ArrayList<Album>() : albums;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * 序列化实体类
	 */
	public static final Parcelable.Creator<ArtistMusicInfo> CREATOR = new Creator<ArtistMusicInfo>() {
		@SuppressWarnings("unchecked")
		public ArtistMusicInfo createFromParcel(Parcel source) {
			ArtistMusicInfo info = new ArtistMusicInfo();
			info.songs = source.readArrayList(ArtistMusicInfo.class
					.getClassLoader());
			info.albums = source.readArrayList(ArtistMusicInfo.class
					.getClassLoader());
			return info;
		}

		public ArtistMusicInfo[] newArray(int size) {
			return new ArtistMusicInfo[size];
		}
	};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(songs);
		dest.writeList(albums);
	}
}
