package com.toly1994.tolymusic.app.domain;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Locale;

public class Album implements Parcelable{
	private long albumId;
	private Bitmap cover;
	private String albumName;
	private Artist artist;
	private int coverRgb;
	private ArrayList<Song> songs;
	
	private Album(){}

	public Album(long albumId, String albumName, Artist artist,
			ArrayList<Song> songs) {
		super();
		this.albumId = albumId;
		this.albumName = albumName;
		this.artist = artist;
		this.coverRgb = Color.parseColor("#4BAD97");
		if (songs == null) {
			this.songs = new ArrayList<>();
		} else {
			this.songs = songs;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o.getClass() == Album.class) {
			Album other = (Album) o;
			return this.albumName
					.trim()
					.toLowerCase(Locale.ENGLISH)
					.equals(other.getAlbumName().trim()
							.toLowerCase(Locale.ENGLISH))
					&& this.artist.equals(other.getArtist());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (Long.valueOf(albumId).hashCode() + albumName.hashCode() + artist
				.hashCode()) * 2;
	}

	public Artist getArtist() {
		return artist;
	}

	public long getAlbumId() {
		return albumId;
	}
	
	public int getCoverRgb() {
		return coverRgb;
	}

	public void setCoverRgb(int coverRgb) {
		this.coverRgb = coverRgb;
	}

	public Bitmap getCover() {
		return cover;
	}

	public void setCover(Bitmap cover) {
		this.cover = cover;
	}

	public String getAlbumName() {
		return albumName;
	}

	public ArrayList<Song> getSongs() {
		return songs;
	}

	public void setSongs(ArrayList<Song> songs) {
		this.songs = songs;
	}

	/**
	 * 序列化实体类
	 */
	public static final Parcelable.Creator<Album> CREATOR = new Creator<Album>() {
		@SuppressWarnings("unchecked")
		public Album createFromParcel(Parcel source) {
			Album album = new Album();
			album.albumName = source.readString();
			album.albumId = source.readLong();
			album.songs = source.readArrayList(Album.class.getClassLoader());
			return album;
		}

		public Album[] newArray(int size) {
			return new Album[size];
		}
	};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(albumName);
		dest.writeLong(albumId);
		dest.writeList(songs);
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
