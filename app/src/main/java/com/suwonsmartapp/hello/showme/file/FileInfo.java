package com.suwonsmartapp.hello.showme.file;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class FileInfo implements Parcelable {

    private File file;
    private String title;
    private Long size;
    private Long modified;

    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public Long getModified() { return modified; }
    public void setModified(Long modified) { this.modified = modified; }

    public static Creator getCREATOR() { return CREATOR; }

    public FileInfo() { }
    public FileInfo(Parcel in) { readFromParcel(in); }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeLong(size);
        dest.writeLong(modified);
    }

    private void readFromParcel(Parcel in){
        title = in.readString();
        size = in.readLong();
        modified = in.readLong();
    }

    public static final Creator CREATOR = new Creator() {
        public FileInfo createFromParcel(Parcel in) {
            return new FileInfo(in);
        }

        @Override
        public FileInfo[] newArray(int size) {
            return new FileInfo[0];
        }
    };
}
