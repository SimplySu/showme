package com.suwonsmartapp.hello.showme.audio;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class AudioFileInfo implements Parcelable {

    private Long id;
    private String artist;
    private String title;
    private String mediaData;
    private String displayName;
    private Long duration;
    private Integer albumId;
    private String columnsData;

    private Uri songUri;
    private Bitmap albumArt;

    public AudioFileInfo() { }
    public AudioFileInfo(Parcel in) {
        readFromParcel(in);
    }
    public Bitmap getAlbumArt() {
        return albumArt;
    }
    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }
    public Uri getSongUri() {
        return songUri;
    }
    public void setSongUri(Uri songUri) {
        this.songUri = songUri;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getMediaData() {
        return mediaData;
    }
    public void setMediaData(String mediaData) {
        this.mediaData = mediaData;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public long getDuration() {
        return duration;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }
    public int getAlbumId() {
        return albumId;
    }
    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }
    public String getColumnsData() {
        return columnsData;
    }
    public void setColumnsData(String columnsData) {
        this.columnsData = columnsData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeInt(albumId);
        dest.writeParcelable(albumArt, CONTENTS_FILE_DESCRIPTOR);
        dest.writeParcelable(songUri, CONTENTS_FILE_DESCRIPTOR);
    }

    private void readFromParcel(Parcel in){
        id = in.readLong();
        title = in.readString();
        artist = in.readString();
        albumId = in.readInt();
        albumArt = in.readParcelable(ClassLoader.getSystemClassLoader());
        songUri = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AudioFileInfo createFromParcel(Parcel in) {
            return new AudioFileInfo(in);
        }

        @Override
        public AudioFileInfo[] newArray(int size) {
            return new AudioFileInfo[0];
        }
    };
}
