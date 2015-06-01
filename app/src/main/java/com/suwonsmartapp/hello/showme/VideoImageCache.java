package com.suwonsmartapp.hello.showme;

import android.graphics.Bitmap;

import java.io.File;

public interface VideoImageCache {
    public void addBitmap(String key, Bitmap bitmap);
    public void addBitmap(String key, File bitmapFile);
    public Bitmap getBitmap(String key);
    public void clear();
}
