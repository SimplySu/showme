package com.suwonsmartapp.hello.showme;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.suwonsmartapp.hello.R;

import java.util.ArrayList;

public class AudioListAdapter extends BaseAdapter {

    private static final String TAG = AudioListAdapter.class.getSimpleName();
    private LayoutInflater inflater;
    private Context mContext;
    private ViewHolder viewHolder;

    private final ArrayList<AudioFileInfo> mAudioFileInfoList;

    private int mCurrentPosition;

    static class ViewHolder {
        ImageView ivAlbumIcon;
        TextView tvTitle;
        TextView tvArtist;
    }

    public AudioListAdapter(Context context, ArrayList<AudioFileInfo> audioFileInfoList, int currentPosition) {
        mContext = context;
        mAudioFileInfoList = audioFileInfoList;
        mCurrentPosition = currentPosition;
    }

    @Override
    public int getCount() {
        return mAudioFileInfoList.size();
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
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null){
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.audio_file_list, null);
            ImageView albumPic = (ImageView) view.findViewById(R.id.album_picture);
            TextView music = (TextView) view.findViewById(R.id.audio_adapter_title);
            TextView singer = (TextView) view.findViewById(R.id.audio_adapter_artist);

            viewHolder = new ViewHolder();          // construct view holder
            viewHolder.tvTitle = music;             // set music title
            viewHolder.ivAlbumIcon = albumPic;      // set album icon
            viewHolder.tvArtist = singer;           // set music artist
            view.setTag(viewHolder);                // setup one view

        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        AudioFileInfo playSong = mAudioFileInfoList.get(position);

        int extensionPosition = playSong.getDisplayName().lastIndexOf('.');
        int extensionLength = playSong.getDisplayName().length();
        String extension = playSong.getDisplayName().substring(extensionPosition, extensionLength);

        viewHolder.tvTitle.setText(playSong.getTitle() + extension);    // title.ext
        viewHolder.tvArtist.setText(playSong.getArtist());              // artist

        int albumId = playSong.getAlbumId();

        // attach a bitmap image
        AudioLoadBitmap audioLoadBitmap = new AudioLoadBitmap(mContext);
        audioLoadBitmap.loadBitmap(albumId, viewHolder.ivAlbumIcon);

        if (mCurrentPosition == position) {     // currently playing
            viewHolder.tvTitle.setTextColor(Color.parseColor("#ff5652f1"));
        } else {
            viewHolder.tvTitle.setTextColor(Color.WHITE);
        }
        return view;
    }

    public void setmCurrentPosition(int position) {
        this.mCurrentPosition = position;
    }
}
