
package com.suwonsmartapp.hello.showme.video;

import android.content.Context;
import android.database.Cursor;
import android.widget.CursorAdapter;
import android.widget.ImageView;

/**
 * Created by junsuk on 2015-04-28.
 */
public abstract class VideoAsyncBitmapAdapter extends CursorAdapter {

    private VideoAsyncBitmapLoader mVideoAsyncBitmapLoader;

    public VideoAsyncBitmapAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);

        mVideoAsyncBitmapLoader = new VideoAsyncBitmapLoader(context);
        mVideoAsyncBitmapLoader.setBitmapLoadListener(getAsyncBitmapLoadListener());
    }

    protected abstract VideoAsyncBitmapLoader.BitmapLoadListener getAsyncBitmapLoadListener();

    protected void loadImage(int position, ImageView view) {
        mVideoAsyncBitmapLoader.loadBitmap(position, view);
    }

}
