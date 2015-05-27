package com.suwonsmartapp.hello.showme;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class VideoFileInfo implements Parcelable {

    private Long id;
    private String artist;
    private String title;
    private String mediaData;
    private String displayName;
    private Long duration;
    private String columnsData;

    private Uri videoUri;
    private Bitmap albumArt;

    public VideoFileInfo() { }
    public VideoFileInfo(Parcel in) { readFromParcel(in); }
    public Bitmap getAlbumArt() { return albumArt; }
    public void setAlbumArt(Bitmap albumArt) { this.albumArt = albumArt; }
    public Uri getVideoUri() { return videoUri; }
    public void setVideoUri(Uri songUri) { this.videoUri = songUri; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMediaData() { return mediaData; }
    public void setMediaData(String mediaData) { this.mediaData = mediaData; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public String getColumnsData() { return columnsData; }
    public void setColumnsData(String columnsData) { this.columnsData = columnsData; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(artist);
        dest.writeString(title);
        dest.writeString(mediaData);
        dest.writeString(displayName);
        dest.writeLong(duration);
        dest.writeString(columnsData);
        dest.writeParcelable(albumArt, CONTENTS_FILE_DESCRIPTOR);
        dest.writeParcelable(videoUri, CONTENTS_FILE_DESCRIPTOR);
    }

    private void readFromParcel(Parcel in) {
        id = in.readLong();
        artist = in.readString();
        title = in.readString();
        mediaData = in.readString();
        displayName = in.readString();
        duration = in.readLong();
        columnsData = in.readString();
        albumArt = in.readParcelable(ClassLoader.getSystemClassLoader());
        videoUri = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public VideoFileInfo createFromParcel(Parcel in) {
            return new VideoFileInfo(in);
        }

        @Override
        public VideoFileInfo[] newArray(int size) {
            return new VideoFileInfo[0];
        }
    };
}
