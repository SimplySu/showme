
package com.suwonsmartapp.hello.showme.video;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class VideoAsyncBitmapLoader {

    private static final int MAX_CACHE_SIZE = 10;

    private VideoImageCache mVideoImageCache;

    private BitmapLoadListener mBitmapLoadListener;

    private Context mContext;

    private ColorDrawable mTransparentColorDrawable;

    private TransitionDrawable mTransitionDrawable;

    public interface BitmapLoadListener {
        Bitmap getBitmap(int position);
    }

    public void setBitmapLoadListener(BitmapLoadListener listener) {
        mBitmapLoadListener = listener;
    }

    public VideoAsyncBitmapLoader(Context context) {
        mContext = context;
//        mVideoImageCache = ImageCacheFactory.getInstance().createMemoryCache("memoryCache", MAX_CACHE_SIZE);
        mVideoImageCache = new VideoMemoryImageCache(MAX_CACHE_SIZE);

        mTransparentColorDrawable = new ColorDrawable(Color.TRANSPARENT);
    }

    public void loadBitmap(int position, ImageView imageView) {
        final String imageKey = String.valueOf(position);

        final Bitmap bitmap = getBitmapFromCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            if (cancelTask(position, imageView)) {
                final AsyncBitmapLoaderTask task = new AsyncBitmapLoaderTask(position, imageView);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), null, task);
                imageView.setImageDrawable(asyncDrawable);

                task.execute(position);
            }
        }
    }

    private void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null) {
            mVideoImageCache.addBitmap(key, bitmap);
        }
    }

    private Bitmap getBitmapFromCache(String key) {
        return mVideoImageCache.getBitmap(key);
    }

    class AsyncBitmapLoaderTask extends AsyncTask<Integer, Void, Bitmap> {
        private int position = -1;

        private final WeakReference<ImageView> mImageViewReference;

        public AsyncBitmapLoaderTask(int position, ImageView imageView) {
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            if (mBitmapLoadListener == null) {
                throw new NullPointerException("BitmapLoadListener is null");
            }

            position = params[0];
            Log.d("VideoAsyncBitmapLoader", "position : " + position);

            final Bitmap bitmap = mBitmapLoadListener.getBitmap(position);

            String key = String.valueOf(position);
            addBitmapToCache(key, bitmap);

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (mImageViewReference != null && bitmap != null) {
                BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
                Drawable[] drawables = new Drawable[] { mTransparentColorDrawable, bitmapDrawable};
                mTransitionDrawable = new TransitionDrawable(drawables);

                final ImageView imageView = mImageViewReference.get();
                if (imageView != null) {
                    // get task from ImageView
                    final AsyncBitmapLoaderTask bitmapLoaderTask = getAsyncBitmapLoaderTask(imageView);

                    // setup bitmap if the same task
                    if (this == bitmapLoaderTask && imageView != null) {
                        mTransitionDrawable.startTransition(500);
                        imageView.setImageDrawable(mTransitionDrawable);
                    }
                }
            }
        }

    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<AsyncBitmapLoaderTask> asyncBitmapLoaderTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, AsyncBitmapLoaderTask asyncBitmapLoaderTask) {
            super(res, bitmap);
            this.asyncBitmapLoaderTaskReference = new WeakReference<AsyncBitmapLoaderTask>(asyncBitmapLoaderTask);
        }

        public AsyncBitmapLoaderTask getAsyncBitmapLoaderTask() {
            return asyncBitmapLoaderTaskReference.get();
        }
    }

    private static AsyncBitmapLoaderTask getAsyncBitmapLoaderTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getAsyncBitmapLoaderTask();
            }
        }
        return null;
    }

    public static boolean cancelTask(int position, ImageView imageView) {
        final AsyncBitmapLoaderTask task = getAsyncBitmapLoaderTask(imageView);

        if (task != null) {
            final int taskPosition = task.position;
            if (taskPosition == -1 || taskPosition != position) {
                // cancel previous task
                task.cancel(true);
                Log.d("VideoAsyncBitmapLoader", "cancel : " + position);
            } else {
                // do not execute if the same task
                Log.d("VideoAsyncBitmapLoader", "false");
                return false;
            }
        }
        // execute new task
        return true;
    }

    public void destroy() {
        if (mVideoImageCache != null) {
            mVideoImageCache.clear();
            mVideoImageCache = null;
        }
    }
}