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
    // ���ؽ�Ʈ�� ���ϸ����κ��� �̵��� �˻��Ͽ� ����� ������ �������� ������ ��.
    public static Bitmap getAudioThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[]{ file },
                "_display_name ASC");

        int gc;     // getCount�� ������ ������ �Ϳ� ����Ͽ�.
        try {
            gc = cursor.getCount();
        } catch (NullPointerException e) {
            cursor.close();
            return null;
        }

        // �����ͺ��̽��� �� ������ ��� ���� �˻��� �ȵǼ� �������� ǥ���� �� ����.
        if (gc == 0) {
            cursor.close();
            return null;
        }

        // �����ϴ� ������ �� ���ۿ� ����� ������.
        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        // �ɼǰ��� 1�̸� �������� Ȯ��/��Ҹ� ���� ����.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;   // 1 = Ȯ��/��� ����, 2^n = ���� ���� ������

        // �ش� ����� ������ Uri ���� ������.
        Uri audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        if (audioUri == null) {
            cursor.close();
            return null;
        }

        // Uri �ּҷκ��� ����� �������� �о��.
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, audioUri);
        } catch (RuntimeException e) {
            cursor.close();
            return null;
        }
        byte data[] = retriever.getEmbeddedPicture();

        // ����� �������� ���� ���� ����.
        if (data == null) {
            cursor.close();
            return null;
        }

        // �ӽ÷� ����� Ŀ���� �޸� ������ ���� �ݾƾ� ��.
        cursor.close();
        // �������� ã���� ��� �̸� ������.
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    // ���ؽ�Ʈ�� ���ϸ����κ��� �̵��� �˻��Ͽ� �׸� ������ �������� ������ ��.
    public static Bitmap getImageThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[] { file },
                "_display_name ASC");

        // �������� ���� ��� ����� ���� �ʱ�ȭ��.
        Bitmap thumbnail = null;
        BitmapFactory.Options options = new BitmapFactory.Options();

        int gc;     // getCount�� ������ ������ �Ϳ� ����Ͽ�.
        try {
            gc = cursor.getCount();
        } catch (NullPointerException e) {
            cursor.close();
            return null;
        }

        // �����ͺ��̽��� �� ������ ��� ���� �˻��� �ȵǼ� �������� ǥ���� �� ����.
        if (gc != 0) {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_id");
            long id = cursor.getLong(index);

            options.inSampleSize = 8;   // 1 = Ȯ��/��� ����, 2^n = ���� ���� ������

            // �������� �ִ� ��� �̸� �о��.
            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    context.getContentResolver(), id,
                    MediaStore.Images.Thumbnails.MINI_KIND, options);
        }

        // �������� ������ �̹��� ���Ϸκ��� ���� ����.
        if (thumbnail == null) {
            options.inSampleSize = 8;
            thumbnail = BitmapFactory.decodeFile(file, options);
        }

        // �ӽ÷� ����� Ŀ���� �޸� ������ ���� �ݾƾ� ��.
        cursor.close();
        // �������� ã���� ��� �̸� ������.
        return thumbnail;
    }

    // ���ؽ�Ʈ�� ���ϸ����κ��� �̵��� �˻��Ͽ� ���� ������ �������� ������ ��.
    public static Bitmap getVideoThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[] { file },
                "_display_name ASC");

        int gc;     // getCount�� ������ ������ �Ϳ� ����Ͽ�.
        try {
            gc = cursor.getCount();
        } catch (NullPointerException e) {
            cursor.close();
            return null;
        }

        // �����ͺ��̽��� �� ������ ��� ���� �˻��� �ȵǼ� �������� ǥ���� �� ����.
        if (gc == 0) {
            cursor.close();
            return null;
        }

        // �����ϴ� ������ �� ���ۿ� ����� ������.
        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        // �ɼǰ��� 1�̸� �������� Ȯ��/��Ҹ� ���� ����.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;   // 1 = Ȯ��/��� ����, 2^n = ���� ���� ������

        // �������� �ִ� ��� �̸� �о��.
        Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                context.getContentResolver(), id,
                MediaStore.Video.Thumbnails.MINI_KIND, options);

        // �������� ������ ���� ���Ϸκ��� ���� ����.
        if (thumbnail == null) {
            options.inSampleSize = 8;
            thumbnail = BitmapFactory.decodeFile(file, options);
        }

        // �ӽ÷� ����� Ŀ���� �޸� ������ ���� �ݾƾ� ��.
        cursor.close();
        // �������� ã���� ��� �̸� ������.
        return thumbnail;
    }
}
