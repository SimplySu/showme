package com.suwonsmartapp.hello.showme.file;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.suwonsmartapp.hello.R;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileAdapter extends BaseAdapter {

    private List<FileInfo> mData;
    private Context mContext;
    private int mCurrentPosition;

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyMMdd kk:mm");
    private DecimalFormat mDecimalFormat = new DecimalFormat("#,###");

    private String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "xmf", "rtx", "ota",
                                "imy", "ts", "cue", "wma", "ape"};
    private int[] audioR = {R.drawable.file_music_mp3, R.drawable.file_music_ogg, R.drawable.file_music_wav,
            R.drawable.file_music_flac, R.drawable.file_music_mid, R.drawable.file_music_m4a,
            R.drawable.file_music_xmf, R.drawable.file_music_rtx, R.drawable.file_music_ota,
            R.drawable.file_music_imy, R.drawable.file_music_ts, R.drawable.file_music_cue,
            R.drawable.file_music_wma, R.drawable.file_music_ape};

    private String[] video = {"avi", "mkv", "mp4", "wmv", "asf", "mov", "mpg", "flv", "tp", "3gp",
                                "m4v", "rmvb", "webm", "smi", "srt", "sub", "idx"};
    private int[] videoR = {R.drawable.file_movie_avi, R.drawable.file_movie_mkv, R.drawable.file_movie_mp4,
            R.drawable.file_movie_wmv, R.drawable.file_movie_asf, R.drawable.file_movie_mov,
            R.drawable.file_movie_mpg, R.drawable.file_movie_flv, R.drawable.file_movie_tp,
            R.drawable.file_movie_3gp, R.drawable.file_movie_m4v, R.drawable.file_movie_rmvb,
            R.drawable.file_movie_webm, R.drawable.file_document_smi, R.drawable.file_document_srt,
            R.drawable.file_document_sub, R.drawable.file_document_idx};

    private String[] image = {"jpg", "jpeg", "gif", "png", "bmp", "tif", "tiff", "webp"};
    private int[] imageR = {R.drawable.file_image_jpg, R.drawable.file_image_jpeg, R.drawable.file_image_gif,
            R.drawable.file_image_png, R.drawable.file_image_bmp, R.drawable.file_image_tif,
            R.drawable.file_image_tiff, R.drawable.file_image_webp};

    private String[] number = {"0", "1", "2", "3", "4", "5", "6", "7"};
    private int[] numberR = {R.drawable.file_system_0, R.drawable.file_system_1, R.drawable.file_system_2,
            R.drawable.file_system_3, R.drawable.file_system_4, R.drawable.file_system_5,
            R.drawable.file_system_6, R.drawable.file_system_7, R.drawable.file_system_8,
            R.drawable.file_system_9};

    private String[] system = {"cfg", "conf", "dat", "fil", "gz", "ico", "pem", "qc", "qcom", "rc", "sh"};
    private int[] systemR = {R.drawable.file_system_cfg, R.drawable.file_system_conf,
            R.drawable.file_system_dat, R.drawable.file_system_fil, R.drawable.file_system_gz,
            R.drawable.file_system_ico, R.drawable.file_system_pem, R.drawable.file_system_qc,
            R.drawable.file_system_qcom, R.drawable.file_system_rc, R.drawable.file_system_sh};

    private String[] other = {"apk", "bin"};
    private int[] otherR = {R.drawable.file_other_apk, R.drawable.file_other_bin};

    private String[] document = {"txt", "doc", "htm", "hwp", "pdf", "ppt", "rtf", "xls", "xlx", "xml",
            "csv", "dif", "dot", "emf", "mht", "odp", "ods", "odt", "pot", "ppa", "pps", "prn",
            "slk", "thm", "wps", "xla", "xlt", "xps", "gul"};
    private int[] documentR = {R.drawable.file_document_txt, R.drawable.file_document_doc,
            R.drawable.file_document_htm, R.drawable.file_document_hwp, R.drawable.file_document_pdf,
            R.drawable.file_document_ppt, R.drawable.file_document_rtf, R.drawable.file_document_xls,
            R.drawable.file_document_xlx, R.drawable.file_document_xml, R.drawable.file_document_csv,
            R.drawable.file_document_dif, R.drawable.file_document_dot, R.drawable.file_document_emf,
            R.drawable.file_document_mht, R.drawable.file_document_odp, R.drawable.file_document_ods,
            R.drawable.file_document_odt, R.drawable.file_document_pot, R.drawable.file_document_ppa,
            R.drawable.file_document_pps, R.drawable.file_document_prn, R.drawable.file_document_slk,
            R.drawable.file_document_thm, R.drawable.file_document_wps, R.drawable.file_document_xla,
            R.drawable.file_document_xlt, R.drawable.file_document_xps, R.drawable.file_document_gul};

    public FileAdapter(Context context, List<FileInfo> data) {
        mContext = context;
        mData = data;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // When we load the View at very first time, or setting Data at very first time
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.file_manager_filelist, null);
            ImageView fileicon = (ImageView) convertView.findViewById(R.id.file_list_icon);
            TextView filename = (TextView) convertView.findViewById(R.id.tv_filename);
            TextView filesize = (TextView) convertView.findViewById(R.id.tv_filesize);
            TextView modified = (TextView) convertView.findViewById(R.id.tv_modified);

            holder = new ViewHolder();
            holder.fileIcon = fileicon;
            holder.fileName = filename;
            holder.fileSize = filesize;
            holder.modified = modified;

            convertView.setTag(holder);
        } else {
            // reuse View, and Data
            holder = (ViewHolder) convertView.getTag();
        }

        // get current position and data
        FileInfo fi = (FileInfo) getItem(position);
        File file = fi.getFile();
        holder.fileName.setText(file.getName());

        if (file.isDirectory()) {
            holder.fileIcon.setImageResource(R.drawable.file_folder);
        } else {
            if (!seeIfKnownType(file, holder.fileIcon)) {
                holder.fileIcon.setImageResource(determineMimeType(file.getName()));
            }
        }

        // see if folder or not
        if (file.isDirectory()) {
            if (file.canRead()) {
                holder.fileSize.setText("<dir>");
                holder.fileName.setTextColor(Color.parseColor("#ffffff"));
                holder.fileSize.setTextColor(Color.parseColor("#ffffff"));
                holder.modified.setTextColor(Color.parseColor("#ffffff"));
            } else {
                holder.fileSize.setText("<dir>");
                holder.fileName.setTextColor(Color.parseColor("#c0c0c0"));
                holder.fileSize.setTextColor(Color.parseColor("#c0c0c0"));
                holder.modified.setTextColor(Color.parseColor("#c0c0c0"));
            }
        } else {
            long size = file.length() / 1024;
            if (size == 0) {
                holder.fileSize.setText(String.valueOf(file.length()) + "b");
            } else {
                holder.fileSize.setText(mDecimalFormat.format(size) + "k");
            }
            holder.fileName.setTextColor(Color.parseColor("#fffafa"));
            holder.fileSize.setTextColor(Color.parseColor("#fffafa"));
            holder.modified.setTextColor(Color.parseColor("#fffafa"));

            if (mCurrentPosition == position) {     // currently focusing ?
                holder.fileName.setTextColor(Color.parseColor("#ff5050f0"));
                holder.fileSize.setTextColor(Color.parseColor("#ff5050f0"));
                holder.modified.setTextColor(Color.parseColor("#ff5050f0"));
            } else {
                holder.fileName.setTextColor(Color.parseColor("#fffafa"));
                holder.fileSize.setTextColor(Color.parseColor("#fffafa"));
                holder.modified.setTextColor(Color.parseColor("#fffafa"));
            }
        }

        holder.modified.setText(mDateFormat.format(new Date(file.lastModified())));

        // return completed View
        return convertView;
    }

    public void setmCurrentPosition(int position) {
        this.mCurrentPosition = position;
    }

    private boolean seeIfKnownType(File file, ImageView v) {

        int result = file.getName().lastIndexOf('.');
        if ((result == -1) || (result == 0)) {
            return false;
        }

        int length = file.getName().length();
        String ext = file.getName().substring(result + 1, length).toLowerCase();

        for (int i = 0; i < 1; i++) {
            if (ext.equals(audio[i])) {
                Bitmap bm = getAudioThumbnail(mContext, file.getAbsolutePath());
                if (bm == null) {
                    return false;
                } else {
                    v.setImageBitmap(bm);
                    return true;
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            if (ext.equals(image[i])) {
                Bitmap bm = getImageThumbnail(mContext, file.getAbsolutePath());
                if (bm == null) {
                    return false;
                } else {
                    v.setImageBitmap(bm);
                    return true;
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            if (ext.equals(video[i])) {

                Bitmap bm = getVideoThumbnail(mContext, file.getAbsolutePath());
                if (bm == null) {
                    return false;
                } else {
                    v.setImageBitmap(bm);
                    return true;
                }
            }
        }

        return false;
    }

    private int determineMimeType(String file) {

        int result = file.lastIndexOf('.');
        if (result == -1) {
            return R.drawable.file_system_;
        }

        int length = file.length();
        String ext = file.substring(result + 1, length);        // get extension without dot

        if (ext.equals("")) {
            return R.drawable.file_system_;
        }

        String extension = ext.toLowerCase();    // extension lowercase character

        for (int i = 0; i < audio.length; i++) {
            if (extension.equals(audio[i])) {
                return audioR[i];
            }
        }

        for (int i = 0; i < video.length; i++) {
            if (extension.equals(video[i])) {
                return videoR[i];
            }
        }

        for (int i = 0; i < image.length; i++) {
            if (extension.equals(image[i])) {
                return imageR[i];
            }
        }

        for (int i = 0; i < number.length; i++) {
            if (extension.equals(number[i])) {
                return numberR[i];
            }
        }

        for (int i = 0; i < system.length; i++) {
            if (extension.equals(system[i])) {
                return systemR[i];
            }
        }

        for (int i = 0; i < other.length; i++) {
            if (extension.equals(other[i])) {
                return otherR[i];
            }
        }

        if (extension.length() < 3) {
            return R.drawable.file_other_;
        }

        String ext3char = extension.substring(0, 3);
        for (int i = 0; i < document.length; i++) {
            if (ext3char.equals(document[i])) {
                return documentR[i];
            }
        }

        return R.drawable.file_other_;
    }

    public static Bitmap getAudioThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[]{ file },
                "_display_name ASC");

        if (cursor.getCount() == 0) {
            return null;
        }

        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;   // 1 = no sample, 2^n = smaller

        Uri audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        if (audioUri == null) {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, audioUri);
        } catch (RuntimeException e) {
            return null;
        }
        byte data[] = retriever.getEmbeddedPicture();

        if (data == null) {
            return null;
        }

        Bitmap thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
        return thumbnail;
    }

    public static Bitmap getImageThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[] { file },
                "_display_name ASC");

        Bitmap thumbnail = null;
        BitmapFactory.Options options = new BitmapFactory.Options();

        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_id");
            long id = cursor.getLong(index);

            options.inSampleSize = 1;   // 1 = no sample, 2^n = smaller

            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    context.getContentResolver(), id,
                    MediaStore.Images.Thumbnails.MINI_KIND, options);
        }

        if (thumbnail == null) {
            options.inSampleSize = 8;
            thumbnail = BitmapFactory.decodeFile(file, options);
        }
        return thumbnail;
    }

    public static Bitmap getVideoThumbnail(Context context, String file) {
        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "_data = ?",
                new String[] { file },
                "_display_name ASC");

        if (cursor.getCount() == 0) {
            return null;
        }

        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;   // 1 = no sample, 2^n = smaller

        Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                context.getContentResolver(), id,
                MediaStore.Video.Thumbnails.MINI_KIND, options);

        if (thumbnail == null) {
            options.inSampleSize = 8;
            thumbnail = BitmapFactory.decodeFile(file, options);
        }
        return thumbnail;
    }

    // ViewHolder pattern
    static class ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileSize;
        TextView modified;
    }
}
