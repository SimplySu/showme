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
import com.suwonsmartapp.hello.showme.video.VideoAsyncBitmapLoader;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileListAdapter extends BaseAdapter {

    private VideoAsyncBitmapLoader mAudioAsyncBitmapLoader;
    private VideoAsyncBitmapLoader mImageAsyncBitmapLoader;
    private VideoAsyncBitmapLoader mVideoAsyncBitmapLoader;

    private List<File> mData;
    private Context mContext;
    private Cursor mCursor;

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

    public FileListAdapter(Context context, List<File> data) {
        mContext = context;
        mData = data;

        mAudioAsyncBitmapLoader = new VideoAsyncBitmapLoader(context);
        mAudioAsyncBitmapLoader.setBitmapLoadListener(new VideoAsyncBitmapLoader.BitmapLoadListener() {
            @Override
            public Bitmap getBitmap(int position) {
                Cursor cursor = (Cursor) getItem(position);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;   // 1 = no sample, 2^n = smaller

                Uri audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0));
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(mContext, audioUri);
                byte data[] = retriever.getEmbeddedPicture();
                Bitmap thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
                return thumbnail;
            }
        });

        mImageAsyncBitmapLoader = new VideoAsyncBitmapLoader(context);
        mImageAsyncBitmapLoader.setBitmapLoadListener(new VideoAsyncBitmapLoader.BitmapLoadListener() {
            @Override
            public Bitmap getBitmap(int position) {
                Cursor cursor = (Cursor) getItem(position);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;   // 1 = no sample, 2^n = smaller
                Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                        mContext.getContentResolver(), cursor.getLong(0),
                        MediaStore.Images.Thumbnails.MINI_KIND, options);
                return thumbnail;
            }
        });

        mVideoAsyncBitmapLoader = new VideoAsyncBitmapLoader(context);
        mVideoAsyncBitmapLoader.setBitmapLoadListener(new VideoAsyncBitmapLoader.BitmapLoadListener() {
            @Override
            public Bitmap getBitmap(int position) {
                Cursor cursor = (Cursor) getItem(position);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;   // 1 = no sample, 2^n = smaller
                Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                        mContext.getContentResolver(), cursor.getLong(0),
                        MediaStore.Video.Thumbnails.MINI_KIND, options);
                return thumbnail;
            }
        });

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
        File file = (File) getItem(position);
        String currentFilename = file.getName();
        holder.fileName.setText(currentFilename);

        if (file.isDirectory()) {
            holder.fileIcon.setImageResource(R.drawable.file_folder);
        } else {
            if (!seeIfKnownType(currentFilename, position, holder.fileIcon)) {
                holder.fileIcon.setImageResource(determineMimeType(currentFilename));
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
        }

        holder.modified.setText(mDateFormat.format(new Date(file.lastModified())));

        // return completed View
        return convertView;
    }

    private boolean seeIfKnownType(String file, int pos, ImageView v) {

        int result = file.lastIndexOf('.');
        if (result == -1) {
            return false;
        }

        int length = file.length();
        String ext = file.substring(result + 1, length).toLowerCase();

        for (int i = 0; i < audio.length; i++) {
            if (ext.equals(audio[i])) {
                mAudioAsyncBitmapLoader.loadBitmap(pos, v);
                return true;
            }
        }

        for (int i = 0; i < image.length; i++) {
            if (ext.equals(image[i])) {
                mImageAsyncBitmapLoader.loadBitmap(pos, v);
                return true;
            }
        }

        for (int i = 0; i < video.length; i++) {
            if (ext.equals(video[i])) {
                mVideoAsyncBitmapLoader.loadBitmap(pos, v);
                return true;
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

    // ViewHolder pattern
    static class ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileSize;
        TextView modified;
    }
}
