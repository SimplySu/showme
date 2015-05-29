package com.suwonsmartapp.hello.showme;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.suwonsmartapp.hello.R;

import java.util.ArrayList;

public class VideoPlayerActivity extends Activity implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private int mCurrentPosition;                   // current playing pointer
    private ArrayList<VideoFileInfo> mVideoFileInfoList;    // video file media_player_icon_information list
    private VideoFileInfo videoFileInfo;                    // video file info getting by cursor
    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent
    private String fullPathname = "";              // full path + filename

    private VideoView mVV_show;

    private int volume_Max = 0;
    private int volume_Current = 0;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // delete title bar and use full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.video_player_activity);
        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        showLog("onCreate");

        Intent intent = getIntent();
        mCurrentPosition = intent.getIntExtra("currentPosition", -1);
        mVideoFileInfoList = intent.getParcelableArrayListExtra("videoInfoList");

        videoFileInfo = mVideoFileInfoList.get(mCurrentPosition);
        fullPathname = videoFileInfo.getMediaData();
        int i = fullPathname.lastIndexOf('/');
        int j = fullPathname.length();
        requestedPathname = fullPathname.substring(0, i);          // get requested pathname
        requestedFilename = fullPathname.substring(i + 1, j);      // and filename

        mVV_show = (VideoView) findViewById(R.id.vv_show);
        MediaController mController = new MediaController(this);
        mVV_show.setMediaController(mController);

        mVV_show.setVideoPath(fullPathname);                        // setting video path
        mVV_show.requestFocus();                                    // set focus

        mVV_show.setOnPreparedListener(this);                       // ready listener
        mVV_show.setOnCompletionListener(this);                     // complete listener for next

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        volume_Max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume_Current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        mSB_volume.setMax(volume_Max);
//        mSB_volume.setProgress(volume_Current);
//        mSB_volume.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
            mVV_show.seekTo(0);
            mVV_show.start();                   // auto start
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (mCurrentPosition >= mVideoFileInfoList.size()) {
            finish();           // playing completed
        } else {
            mCurrentPosition++;
            videoFileInfo = mVideoFileInfoList.get(mCurrentPosition);
            fullPathname = videoFileInfo.getMediaData();
            int i = fullPathname.lastIndexOf('/');
            int j = fullPathname.length();
            requestedPathname = fullPathname.substring(0, i);          // get requested pathname
            requestedFilename = fullPathname.substring(i + 1, j);      // and filename

            mVV_show.setVideoPath(fullPathname);                        // setting video path
            mVV_show.requestFocus();                                    // set focus

            mVV_show.seekTo(0);
            mVV_show.start();                   // auto start
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
