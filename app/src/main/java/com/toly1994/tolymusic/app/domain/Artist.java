package com.toly1994.tolymusic.app.domain;

import android.os.Parcel;
import android.os.Parcelable;
import com.github.stuxuhai.jpinyin.PinyinHelper;

import java.util.Locale;

public class Artist implements Comparable<Artist>,Parcelable {
	private int coverRgb;
	private long artistId;
	private String singerName;
	private ArtistMusicInfo info;
	
	private Artist() {}

	public Artist(long artistId, String singerName, ArtistMusicInfo info) {
		super();
		this.artistId = artistId;
		this.singerName = singerName;
		this.info = info;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o.getClass() == Artist.class) {
			Artist other = (Artist) o;
			return this.singerName
					.trim()
					.toLowerCase(Locale.ENGLISH)
					.equals(other.getSingerName().trim()
							.toLowerCase(Locale.ENGLISH));
		}
		return false;
	}

	@Override
	public int compareTo(Artist another) {
		String aLetter = PinyinHelper.getShortPinyin(singerName).toUpperCase(
				Locale.ENGLISH);
		String bLetter = PinyinHelper.getShortPinyin(another.getSingerName())
				.toUpperCase(Locale.ENGLISH);
		return aLetter.compareTo(bLetter);
	}

	@Override
	public int hashCode() {
		return (singerName.hashCode() + Long.valueOf(artistId).hashCode()) * 2;
	}

	public ArtistMusicInfo getInfo() {
		return info;
	}

	public void setInfo(ArtistMusicInfo info) {
		this.info = info;
	}

	public long getArtistId() {
		return artistId;
	}

	public String getSingerName() {
		return singerName;
	}
	
	public int getCoverRgb() {
		return coverRgb;
	}

	public void setCoverRgb(int coverRgb) {
		this.coverRgb = coverRgb;
	}

	/**
	 * 序列化实体类
	 */
	public static final Parcelable.Creator<Artist> CREATOR = new Creator<Artist>() {
		public Artist createFromParcel(Parcel source) {
			Artist artist = new Artist();
			artist.singerName = source.readString();
			artist.artistId = source.readLong();
			artist.coverRgb = source.readInt();
			// artist.info = (ArtistMusicInfo)
			// source.readValue(Artist.class.getClassLoader());
			return artist;
		}

		public Artist[] newArray(int size) {
			return new Artist[size];
		}
	};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(singerName);
		dest.writeLong(artistId);
		dest.writeInt(coverRgb);
		// dest.writeValue(info);
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
