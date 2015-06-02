package com.suwonsmartapp.hello.showme.audio;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;
import java.io.IOException;

public class AudioPlayerAlbumImage {

    public static Bitmap getArtworkQuick(Context context, int album_id, int w, int h) {

        BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();

        // Most robust way to fetch album art in Android.
        // It can get the album art(album)id) from media store.
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");

        w -= 2;
        h -= 2;
        Uri uri = ContentUris.withAppendedId(artworkUri, album_id);

        if (uri != null) {
            ParcelFileDescriptor fd = null;
            try {
                fd = context.getContentResolver().openFileDescriptor(uri, "r");
                int sampleSize = 1;

                // Compute the closest power-of-two scale factor
                // and pass that to sBitmapOptionsCache.inSampleSize, which will
                // result in faster decoding and better quality
                sBitmapOptionsCache.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);

                int nextWidth = sBitmapOptionsCache.outWidth;
                int nextHeight = sBitmapOptionsCache.outHeight;

                while (nextWidth > w && nextHeight > h) {
                    sampleSize <<= 1;
                    nextWidth >>= 1;
                    nextHeight >>= 1;
                }

                sBitmapOptionsCache.inSampleSize = sampleSize;
                sBitmapOptionsCache.inJustDecodeBounds = false;
                Bitmap b = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);

                if (b != null) {
                    // finally rescale to exactly the size we need
                    if (sBitmapOptionsCache.outWidth != w || sBitmapOptionsCache.outHeight != h) {
                        Bitmap tmp = Bitmap.createScaledBitmap(b, w, h, true);
                        b.recycle();
                        b = tmp;
                    }
                }
                return b;   // return bitmap data of the album
            } catch (FileNotFoundException e) {
            } finally {
                try {
                    if (fd != null)
                        fd.close();
                } catch (IOException e) {
                }
            }
        }
        return null;    // if url = null, nothing to return
    }
}
