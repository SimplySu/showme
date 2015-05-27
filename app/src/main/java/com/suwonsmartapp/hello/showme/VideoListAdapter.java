package com.suwonsmartapp.hello.showme;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.suwonsmartapp.hello.R;

public class VideoListAdapter extends VideoAsyncBitmapAdapter {

    private static final String TAG = VideoListAdapter.class.getSimpleName();
    private LayoutInflater inflater;
    private Context mContext;
    private ViewHolder viewHolder;
    private Cursor mCursor;

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.mCurrentPosition = currentPosition;
    }

    private int mCurrentPosition = -1;

    static class ViewHolder {
        ImageView ivAlbumIcon;
        TextView tvTitle;
        TextView tvArtist;
    }

    public VideoListAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);

        mContext = context;
        mCursor = c;

    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null){
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.video_file_list, null);
            ImageView albumPic = (ImageView) view.findViewById(R.id.video_picture);
            TextView music = (TextView) view.findViewById(R.id.video_adapter_title);
            TextView singer = (TextView) view.findViewById(R.id.video_adapter_artist);

            viewHolder = new ViewHolder();          // construct view holder
            viewHolder.tvTitle = music;             // set music title
            viewHolder.ivAlbumIcon = albumPic;      // set album icon
            viewHolder.tvArtist = singer;           // set music artist
            view.setTag(viewHolder);                // setup one view

        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        mCursor.moveToPosition(position);
        VideoFileInfo playVideo = new VideoFileInfo();
        playVideo.setId(mCursor.getLong(0));                // video ID
        playVideo.setArtist(mCursor.getString(1));          // artist
        playVideo.setTitle(mCursor.getString(2));           // title
        playVideo.setMediaData(mCursor.getString(3));       // full path of the video
        playVideo.setDisplayName(mCursor.getString(4));     // brief video name to show
        playVideo.setDuration(mCursor.getLong(5));          // playing time
        playVideo.setColumnsData(mCursor.getString(6));     // URI

        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, playVideo.getId());
        playVideo.setVideoUri(contentUri);                   // get music icon

        int extensionPosition = playVideo.getDisplayName().lastIndexOf('.');
        int extensionLength = playVideo.getDisplayName().length();
        String extension = playVideo.getDisplayName().substring(extensionPosition, extensionLength);

        viewHolder.tvTitle.setText(playVideo.getTitle() + extension);    // title.ext
        viewHolder.tvArtist.setText(playVideo.getArtist());              // artist

        // attach a bitmap image
        loadImage(position, viewHolder.ivAlbumIcon);

        if (mCurrentPosition == position) {     // currently playing
            viewHolder.tvTitle.setTextColor(Color.parseColor("#ff5652f1"));
        } else {
            viewHolder.tvTitle.setTextColor(Color.WHITE);
        }
        return view;
    }

    @Override
    protected VideoAsyncBitmapLoader.BitmapLoadListener getAsyncBitmapLoadListener() {
        VideoAsyncBitmapLoader.BitmapLoadListener loadListener = new VideoAsyncBitmapLoader.BitmapLoadListener() {
            @Override
            public Bitmap getBitmap(int position) {
                long id = getItemId(position);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;

                Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                        mContext.getContentResolver(), id, MediaStore.Video.Thumbnails.MICRO_KIND,
                        options);

                return thumbnail;
            }
        };
        return loadListener;
    }
}
