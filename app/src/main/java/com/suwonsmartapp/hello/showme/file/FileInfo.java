package com.suwonsmartapp.hello.showme.file;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class FileInfo implements Parcelable {

    private File file;          // FileManagerActivity에서만 사용할 수 있음.
    private String title;       // Parcelable을 상속받았기 때문에 Serializable이 아니면 전송이 안됨.
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
        dest.writeString(title);        // Parcel은 File 데이터를 전달할 수 없음.
        dest.writeLong(size);           // 때문에 File 데이터는 제외하고 전달함.
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
