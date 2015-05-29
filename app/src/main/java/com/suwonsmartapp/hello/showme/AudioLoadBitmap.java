package com.suwonsmartapp.hello.showme;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.ImageView;

import com.suwonsmartapp.hello.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class AudioLoadBitmap {

    private static final String TAG = AudioLoadBitmap.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }

    private Context mContext;
    private AudioImageCache mAudioImageCache;

    public AudioLoadBitmap(Context mContext) {
        this.mContext = mContext;
        mAudioImageCache = AudioImageCache.getInstance();
    }

    public void loadBitmap(int albumId, ImageView imageView) {
        final String imageKey = String.valueOf(albumId);
        final Bitmap bitmap = getBitmapFromMemoryCache(imageKey);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            if (cancelPotentialWork(albumId, imageView)) {
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);

                // binding AsyncDrawable instance and imageView
                final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), null, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(albumId);
            }
        }
    }

    // this method will be used to show temporarily image before task finishing
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<> (bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static boolean cancelPotentialWork(int albumId, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final int currentAlbumId = bitmapWorkerTask.getmAlbumId();
            // If bitmapData is not yet set or it differs from the new data
            if (currentAlbumId != albumId) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    // to get task related with imageView
    // return WorkerTask if imageView and drawable of imageView is not null
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) { //drawable 이 null일 경우 false 반환
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public void addBitmapToMemoryCache(String albumIdKey, Bitmap bitmap) {
        showLog("addBitmapToMemoryCache");

        if (getBitmapFromMemoryCache(albumIdKey) == null) {
            mAudioImageCache.getmMemoryCache().put(albumIdKey, bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCache(String albumIdKey) {
        return mAudioImageCache.getmMemoryCache().get(albumIdKey);
    }


    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {

        public int getmAlbumId() {
            return mAlbumId;
        }

        private int mAlbumId;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Dynamic Loading
        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            mAlbumId = params[0];

            // decode bitmap
            Bitmap bitmap = getArtworkQuick(mAlbumId, 50, 50);

            // save cache
            if (bitmap != null) {
                addBitmapToMemoryCache(String.valueOf(mAlbumId), bitmap);
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (bitmap != null) {
                    if (this == bitmapWorkerTask && imageView != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                } else {
                    if (this == bitmapWorkerTask && imageView != null) {
                        imageView.setImageResource(R.drawable.audio_music_small);
                    }
                }
            }
        }
    }

    private Bitmap getArtworkQuick(int album_id, int w, int h) {
        BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");

        w -= 2;
        h -= 2;
        Uri uri = ContentUris.withAppendedId(artworkUri, album_id);
        if (uri != null) {
            ParcelFileDescriptor fd = null;
            try {
                fd = mContext.getContentResolver().openFileDescriptor(uri, "r");
                int sampleSize = 1;

                // Compute the closest power-of-two scale factor
                // and pass that to sBitmapOptionsCache.inSampleSize, which will
                // result in faster decoding and better quality
                sBitmapOptionsCache.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null,
                        sBitmapOptionsCache);

                int nextWidth = sBitmapOptionsCache.outWidth >> 1;
                int nextHeight = sBitmapOptionsCache.outHeight >> 1;
                while (nextWidth > w && nextHeight > h) {
                    sampleSize <<= 1;
                    nextWidth >>= 1;
                    nextHeight >>= 1;
                }

                sBitmapOptionsCache.inSampleSize = sampleSize;
                sBitmapOptionsCache.inJustDecodeBounds = false;
                Bitmap b = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null,
                        sBitmapOptionsCache);

                if (b != null) {
                    // finally rescale to exactly the size we need
                    if (sBitmapOptionsCache.outWidth != w || sBitmapOptionsCache.outHeight != h) {
                        Bitmap tmp = Bitmap.createScaledBitmap(b, w, h, true);
                        b.recycle();
                        b = tmp;
                    }
                }
                return b;
            } catch (FileNotFoundException e) {
            } finally {
                try {
                    if (fd != null)
                        fd.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
