package com.suwonsmartapp.hello.showme;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ImageFileInfo implements Parcelable {

    private Long id;
    private String title;
    private String data;
    private String displayName;
    private Long size;
    private String uriData;
    private Uri imageUri;

    public ImageFileInfo() { }
    public ImageFileInfo(Parcel in) { readFromParcel(in); }
    public Uri getImageUri() { return imageUri; }
    public void setImageUri(Uri imageUri) { this.imageUri = imageUri; }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getUriData() { return uriData; }
    public void setUriData(String uriData) { this.uriData = uriData; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(data);
        dest.writeString(displayName);
        dest.writeLong(size);
        dest.writeString(uriData);
        dest.writeParcelable(imageUri, CONTENTS_FILE_DESCRIPTOR);
    }

    private void readFromParcel(Parcel in){
        id = in.readLong();
        title = in.readString();
        data = in.readString();
        displayName = in.readString();
        size = in.readLong();
        uriData = in.readString();
        imageUri = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ImageFileInfo createFromParcel(Parcel in) {
            return new ImageFileInfo(in);
        }

        @Override
        public ImageFileInfo[] newArray(int size) {
            return new ImageFileInfo[0];
        }
    };
}
