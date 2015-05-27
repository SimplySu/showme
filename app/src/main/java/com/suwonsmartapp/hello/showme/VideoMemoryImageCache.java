package com.suwonsmartapp.hello.showme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import java.io.File;

/**
 * Created by sol on 2015-04-07.
 */
public class VideoMemoryImageCache implements VideoImageCache {
    private LruCache<String, Bitmap> lruCache;

    public VideoMemoryImageCache(int maxCount) {
        lruCache = new LruCache<String, Bitmap>(maxCount);
    }

    @Override
    public void addBitmap(String key, Bitmap bitmap) {
        if (bitmap == null)
            return;
        lruCache.put(key, bitmap);
    }

    @Override
    public void addBitmap(String key, File bitmapFile) {
        if (bitmapFile == null)
            return;
        if (!bitmapFile.exists())
            return;

        Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
        lruCache.put(key, bitmap);
    }

    @Override
    public Bitmap getBitmap(String key) {
        return lruCache.get(key);
    }

    @Override
    public void clear() {
        lruCache.evictAll();
    }

}
