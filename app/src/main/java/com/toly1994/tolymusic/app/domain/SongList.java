package com.toly1994.tolymusic.app.domain;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class SongList implements Parcelable{
	private String listName;
	private Bitmap cover;
	private List<Song> songs = new ArrayList<>();
	
	private SongList(){};

	public SongList(String listName, List<Song> songs) {
		this.listName = listName;
		this.songs = songs;
	}
	
	public Bitmap getCover() {
		return cover;
	}

	public void setCover(Bitmap cover) {
		this.cover = cover;
	}
	
	public String getListName() {
		return listName;
	}

	public void setListName(String listName) {
		this.listName = listName;
	}

	public List<Song> getSongs() {
		return songs;
	}

	public void setSongs(List<Song> songs) {
		this.songs = songs;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof SongList){
			SongList item = (SongList)o;
			return item.getListName().equals(listName);
		}
		return false;
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	public static final Parcelable.Creator<SongList> CREATOR = new Creator<SongList>() {
		@SuppressWarnings("unchecked")
		public SongList createFromParcel(Parcel source) {
			SongList item = new SongList();
			item.listName = source.readString();
			item.songs = source.readArrayList(SongList.class.getClassLoader());
			return item;
		}

		public SongList[] newArray(int size) {
			return new SongList[size];
		}
	};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(listName);
		dest.writeList(songs);
	}
}
