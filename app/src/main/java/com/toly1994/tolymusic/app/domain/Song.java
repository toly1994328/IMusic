package com.toly1994.tolymusic.app.domain;

import android.os.Parcel;
import android.os.Parcelable;
import com.github.stuxuhai.jpinyin.PinyinHelper;

import java.util.Locale;

public class Song implements Comparable<Song>, Parcelable {
	private long songId;
	private Album album;
	private String title;
	private long duration;
	private String url;

	public Song(Album album, String title, long songId, String url, long duration) {
		super();
		this.album = album;
		this.title = title;
		this.duration = duration;
		this.songId = songId;
		this.url = url;
	}

	public Song() {}

	@Override
	public int compareTo(Song another) {
		String aLetter = PinyinHelper.getShortPinyin(title).toUpperCase(
				Locale.ENGLISH);
		String bLetter = PinyinHelper.getShortPinyin(another.getTitle())
				.toUpperCase(Locale.ENGLISH);
		return aLetter.compareTo(bLetter);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Song){
			Song item = (Song)obj;
			return item.getTitle().equals(title) 
					&& item.getSongId()==songId 
					&& item.getDuration() == duration
					&& item.getUrl().equals(url) ;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return super.toString();
	}

	public Album getAlbum() {
		return album;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public long getDuration() {
		return duration;
	}

	public long getSongId() {
		return songId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * 序列化实体类
	 */
	public static final Parcelable.Creator<Song> CREATOR = new Creator<Song>() {
		public Song createFromParcel(Parcel source) {
			Song song = new Song();
			song.title = source.readString();
			song.duration = source.readLong();
			song.songId = source.readLong();
			song.url = source.readString();
			return song;
		}

		public Song[] newArray(int size) {
			return new Song[size];
		}
	};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeLong(duration);
		dest.writeLong(songId);
		dest.writeString(url);
	}
}
