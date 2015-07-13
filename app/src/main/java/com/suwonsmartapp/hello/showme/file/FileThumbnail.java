package com.suwonsmartapp.hello.showme.file;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;

public class FileThumbnail {
    // 컨텍스트와 파일명으로부터 미디어스토어를 검색하여 오디오 파일의 아이콘을 가지고 옴.
    public static Bitmap getAudioThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[]{ file },
                "_display_name ASC");

        int gc;     // getCount가 에러를 리턴할 것에 대비하여.
        try {
            gc = cursor.getCount();
        } catch (NullPointerException e) {
            cursor.close();
            return null;
        }

        // 데이터베이스가 잘 안읽힐 경우 정보 검색이 안되서 섬네일을 표시할 수 없음.
        if (gc == 0) {
            cursor.close();
            return null;
        }

        // 만족하는 파일이 한 개밖에 없어야 정상임.
        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        // 옵션값이 1이면 섬네일의 확대/축소를 하지 않음.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;   // 1 = 확대/축소 없음, 2^n = 점점 작은 섬네일

        // 해당 오디오 파일의 Uri 값을 가져옴.
        Uri audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        if (audioUri == null) {
            cursor.close();
            return null;
        }

        // Uri 주소로부터 저장된 섬네일을 읽어옴.
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, audioUri);
        } catch (RuntimeException e) {
            cursor.close();
            return null;
        }
        byte data[] = retriever.getEmbeddedPicture();

        // 저장된 섬네일이 없을 수도 있음.
        if (data == null) {
            cursor.close();
            return null;
        }

        // 임시로 사용한 커서는 메모리 해제를 위해 닫아야 함.
        cursor.close();
        // 섬네일을 찾았을 경우 이를 리턴함.
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    // 컨텍스트와 파일명으로부터 미디어스토어를 검색하여 그림 파일의 아이콘을 가지고 옴.
    public static Bitmap getImageThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[] { file },
                "_display_name ASC");

        // 섬네일이 없을 경우 만들기 위해 초기화함.
        Bitmap thumbnail = null;
        BitmapFactory.Options options = new BitmapFactory.Options();

        int gc;     // getCount가 에러를 리턴할 것에 대비하여.
        try {
            gc = cursor.getCount();
        } catch (NullPointerException e) {
            cursor.close();
            return null;
        }

        // 데이터베이스가 잘 안읽힐 경우 정보 검색이 안되서 섬네일을 표시할 수 없음.
        if (gc != 0) {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_id");
            long id = cursor.getLong(index);

            options.inSampleSize = 8;   // 1 = 확대/축소 없음, 2^n = 점점 작은 섬네일

            // 섬네일이 있는 경우 이를 읽어옴.
            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    context.getContentResolver(), id,
                    MediaStore.Images.Thumbnails.MINI_KIND, options);
        }

        // 섬네일이 없으면 이미지 파일로부터 새로 만듬.
        if (thumbnail == null) {
            options.inSampleSize = 8;
            thumbnail = BitmapFactory.decodeFile(file, options);
        }

        // 임시로 사용한 커서는 메모리 해제를 위해 닫아야 함.
        cursor.close();
        // 섬네일을 찾았을 경우 이를 리턴함.
        return thumbnail;
    }

    // 컨텍스트와 파일명으로부터 미디어스토어를 검색하여 비디오 파일의 아이콘을 가지고 옴.
    public static Bitmap getVideoThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[] { file },
                "_display_name ASC");

        int gc;     // getCount가 에러를 리턴할 것에 대비하여.
        try {
            gc = cursor.getCount();
        } catch (NullPointerException e) {
            cursor.close();
            return null;
        }

        // 데이터베이스가 잘 안읽힐 경우 정보 검색이 안되서 섬네일을 표시할 수 없음.
        if (gc == 0) {
            cursor.close();
            return null;
        }

        // 만족하는 파일이 한 개밖에 없어야 정상임.
        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        // 옵션값이 1이면 섬네일의 확대/축소를 하지 않음.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;   // 1 = 확대/축소 없음, 2^n = 점점 작은 섬네일

        // 섬네일이 있는 경우 이를 읽어옴.
        Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                context.getContentResolver(), id,
                MediaStore.Video.Thumbnails.MINI_KIND, options);

        // 섬네일이 없으면 비디오 파일로부터 새로 만듬.
        if (thumbnail == null) {
            options.inSampleSize = 8;
            thumbnail = BitmapFactory.decodeFile(file, options);
        }

        // 임시로 사용한 커서는 메모리 해제를 위해 닫아야 함.
        cursor.close();
        // 섬네일을 찾았을 경우 이를 리턴함.
        return thumbnail;
    }
}
