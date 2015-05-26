package com.suwonsmartapp.hello.showme;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class AudioImageCache {

    private static AudioImageCache audioImageCache;

    // Cache get max available VM memory,
    // exceeding this amount will throw an OutOfMemory exception.
    // Stored in kilobytes as LruCache takes an int in its constructor.
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    final int cacheSize = maxMemory / 8;

    private LruCache<String, Bitmap> mMemoryCache;

    private AudioImageCache() {
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static AudioImageCache getInstance(){
        if(audioImageCache == null){
            synchronized(AudioImageCache.class){
                if(audioImageCache == null){
                    audioImageCache = new AudioImageCache();
                }
            }
        }
        return audioImageCache;
    }

    public LruCache<String, Bitmap> getmMemoryCache() {
        return mMemoryCache;
    }
}
