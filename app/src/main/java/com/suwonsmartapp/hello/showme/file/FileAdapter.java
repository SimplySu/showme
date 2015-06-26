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

    // 파일명과 아이콘이 쌍으로 되어 있음. (예, mp3 = file_music_mp3)
    private String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "xmf", "rtx", "ota",
                                "imy", "ts", "cue", "wma", "ape"};
    private int[] audioR = {R.drawable.file_music_mp3, R.drawable.file_music_ogg, R.drawable.file_music_wav,
            R.drawable.file_music_flac, R.drawable.file_music_mid, R.drawable.file_music_m4a,
            R.drawable.file_music_xmf, R.drawable.file_music_rtx, R.drawable.file_music_ota,
            R.drawable.file_music_imy, R.drawable.file_music_ts, R.drawable.file_music_cue,
            R.drawable.file_music_wma, R.drawable.file_music_ape};

    // we should append .ass and .ssa
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
            holder = (ViewHolder) convertView.getTag();
        }

        // 현재 위치와 데이터를 가져옴.
        FileInfo fi = (FileInfo) getItem(position);
        File file = fi.getFile();
        holder.fileName.setText(file.getName());

        if (file.isDirectory()) {
            holder.fileIcon.setImageResource(R.drawable.file_folder);
        } else {
            // 확장자가 이미 알고 있는 형태이면 해당 아이콘을 가져옴.
            if (!seeIfKnownType(file, holder.fileIcon)) {
                // 모르는 형태이면 미리 정의된 아이콘을 표시함.
                holder.fileIcon.setImageResource(determineMimeType(file.getName()));
            }
        }

        // 디렉토리인 경우 읽기 가능하면 흰색, 불가능하면 회색으로 표시.
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
            // 파일 크기는 바이트 혹은 킬로바이트 단위로 표시함.
            long size = file.length() / 1024;
            if (size == 0) {
                holder.fileSize.setText(String.valueOf(file.length()) + "b");
            } else {
                holder.fileSize.setText(mDecimalFormat.format(size) + "k");
            }

            // 파일 색은 현재 액세스하는 파일이면 청색, 아니면 흰색.
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

        // 최종 업데이트된 날자를 표시함.
        holder.modified.setText(mDateFormat.format(new Date(file.lastModified())));

        // 완성된 뷰를 리턴함.
        return convertView;
    }

    // 현재 엑세스하는 위치를 저장함.
    public void setmCurrentPosition(int position) {
        this.mCurrentPosition = position;
    }

    // 확장자를 검사하여 이미 알고 있는 형태이면 해당 아이콘(thumbnail)을 가져옴.
    private boolean seeIfKnownType(File file, ImageView v) {
        // 확장자가 없으면 아무런 해당사항이 없음.
        int result = file.getName().lastIndexOf('.');
        if ((result == -1) || (result == 0)) {
            return false;
        }

        // 파일명으로부터 확장자만을 추출함.
        int length = file.getName().length();
        String ext = file.getName().substring(result + 1, length).toLowerCase();

        // 오디오 파일인지 검사함.
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

        // 그림 파일인지 검사함.
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

        // 비디오 파일인지 검사함.
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

        // 세 경우가 아니면 관계없는 파일이므로 정해진 아이콘을 표시함.
        return false;
    }

    // 파일명의 확장자를 보고 마임 타입을 결정함.
    private int determineMimeType(String file) {
        int result = file.lastIndexOf('.');
        if ((result == -1) || (result == 0)) {
            return R.drawable.file_system_;
        }

        // 도트(.)를 제외한 확장자명을 가지고 옴.
        int length = file.length();
        String ext = file.substring(result + 1, length);

        // 확장자가 없으면 시스템 파일로 간주함.
        if (ext.equals("")) {
            return R.drawable.file_system_;
        }

        // 확장자를 소문자로 변환함.
        String extension = ext.toLowerCase();

        // 오디오 파일인가?
        for (int i = 0; i < audio.length; i++) {
            if (extension.equals(audio[i])) {
                return audioR[i];
            }
        }

        // 비디오 파일인가?
        for (int i = 0; i < video.length; i++) {
            if (extension.equals(video[i])) {
                return videoR[i];
            }
        }

        // 그림 파일인가?
        for (int i = 0; i < image.length; i++) {
            if (extension.equals(image[i])) {
                return imageR[i];
            }
        }

        // 숫자 확장자인가?
        for (int i = 0; i < number.length; i++) {
            if (extension.equals(number[i])) {
                return numberR[i];
            }
        }

        // 시스템 파일인가?
        for (int i = 0; i < system.length; i++) {
            if (extension.equals(system[i])) {
                return systemR[i];
            }
        }

        // 패키지 파일인가?
        for (int i = 0; i < other.length; i++) {
            if (extension.equals(other[i])) {
                return otherR[i];
            }
        }

        // 확장자가 세글자 이하인가?
        if (extension.length() < 3) {
            return R.drawable.file_other_;
        }

        // 문서 파일인가?
        String ext3char = extension.substring(0, 3);
        for (int i = 0; i < document.length; i++) {
            if (ext3char.equals(document[i])) {
                return documentR[i];
            }
        }

        // 아무 것에도 속하지 않는 모르는 파일임.
        return R.drawable.file_other_;
    }

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
            return null;
        }

        // 데이터베이스가 잘 안읽힐 경우 정보 검색이 안되서 섬네일을 표시할 수 없음.
        if (gc == 0) {
            return null;
        }

        // 만족하는 파일이 한 개밖에 없어야 정상임.
        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        // 옵션값이 1이면 섬네일의 확대/축소를 하지 않음.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;   // 1 = 확대/축소 없음, 2^n = 점점 작은 섬네일

        // 해당 오디오 파일의 Uri 값을 가져옴.
        Uri audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        if (audioUri == null) {
            return null;
        }

        // Uri 주소로부터 저장된 섬네일을 읽어옴.
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, audioUri);
        } catch (RuntimeException e) {
            return null;
        }
        byte data[] = retriever.getEmbeddedPicture();

        // 저장된 섬네일이 없을 수도 있음.
        if (data == null) {
            return null;
        }

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
            return null;
        }

        // 데이터베이스가 잘 안읽힐 경우 정보 검색이 안되서 섬네일을 표시할 수 없음.
        if (gc != 0) {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_id");
            long id = cursor.getLong(index);

            options.inSampleSize = 1;   // 1 = 확대/축소 없음, 2^n = 점점 작은 섬네일

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
            return null;
        }

        // 데이터베이스가 잘 안읽힐 경우 정보 검색이 안되서 섬네일을 표시할 수 없음.
        if (gc == 0) {
            return null;
        }

        // 만족하는 파일이 한 개밖에 없어야 정상임.
        cursor.moveToFirst();
        int index = cursor.getColumnIndex("_id");
        long id = cursor.getLong(index);

        // 옵션값이 1이면 섬네일의 확대/축소를 하지 않음.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;   // 1 = 확대/축소 없음, 2^n = 점점 작은 섬네일

        // 섬네일이 있는 경우 이를 읽어옴.
        Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                context.getContentResolver(), id,
                MediaStore.Video.Thumbnails.MINI_KIND, options);

        // 섬네일이 없으면 비디오 파일로부터 새로 만듬.
        if (thumbnail == null) {
            options.inSampleSize = 8;
            thumbnail = BitmapFactory.decodeFile(file, options);
        }
        return thumbnail;
    }

    // ViewHolder에 데이터를 저장하는 패턴
    static class ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileSize;
        TextView modified;
    }
}
