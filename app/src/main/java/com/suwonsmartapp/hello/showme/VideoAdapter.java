package com.suwonsmartapp.hello.showme;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.suwonsmartapp.hello.R;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Administrator on 2015-05-04.
 */
public class VideoAdapter extends CursorAdapter {

    // 흐름 확인용 로그 출력 메소드
    private static String TAG = "로그 / 비디오플레이어(아답터) ";
    private void show_Log(String msg) { Log.d(TAG, msg); }

    private Context mContext;
    private CustomHolder mHolder;
    private SimpleDateFormat mFormat;

    // ViewHolder 패턴
    private class CustomHolder {
        ImageView mIV_thumbimage;
        TextView mTV_title;
        TextView mTV_size;
        TextView mTV_date;
    }


    public VideoAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mContext = context;
        mFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if(v == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.activity_video_adapter, null);

            mHolder = new CustomHolder();
            mHolder.mIV_thumbimage = (ImageView)v.findViewById(R.id.iv_thumbimage);
            mHolder.mTV_title = (TextView)v.findViewById(R.id.tv_title);
            mHolder.mTV_size = (TextView)v.findViewById(R.id.tv_size);
            mHolder.mTV_date = (TextView)v.findViewById(R.id.tv_date);

            v.setTag(mHolder);
        }
        else {
            mHolder = (CustomHolder) v.getTag();
        }

        // 동영상 썸네일 이미지 셋팅
        Cursor mCursor = (Cursor)getItem(position);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                mContext.getContentResolver(), getItemId(position), MediaStore.Video.Thumbnails.MICRO_KIND,
                options);
        mHolder.mIV_thumbimage.setImageBitmap(thumbnail);
        mHolder.mIV_thumbimage.setAdjustViewBounds(true);
        mHolder.mIV_thumbimage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // 동영상 타이틀 텍스트 셋팅
        final String title = mCursor.getString(mCursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
        mHolder.mTV_title.setText(title);

        // 동영상 크기 텍스트 셋팅
        final String size = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
        double numInt = Integer.parseInt(size);
        numInt = numInt / 1024 * 1024;
        numInt = Math.floor(numInt * 100) / 100;
        mHolder.mTV_size.setText(numInt + "MB");

        // 동영상 등록일 텍스트 셋팅
        final String date = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED));
        String dateText = mFormat.format(new Date(Long.valueOf(date)));
        mHolder.mTV_date.setText(dateText);

        return v;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

    }
}


/* /// MediaColumns 인터페이스 ///
-------------------------------------
_COUNT(_count)  int  레코드 개수
_ID(_id)  long  레코드의 pk
DATA(_data)  DATA_STREAM  데이터 스트림. 파일의 경로
SIZE(_size)  long  파일 크기
DISPLAY_NAME(_display_name)  text  파일 표시명
MIME_TYPE(mime_type)  text  마임 타입
TITLE(title)  text  제목
DATE_ADDED(date_added)  long  추가 날짜. 초단위
DATE_MODIFIED (date_modified)  long  최후 갱신 날짜. 초단위
 */
