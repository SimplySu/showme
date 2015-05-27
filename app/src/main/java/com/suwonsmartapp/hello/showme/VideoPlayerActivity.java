package com.suwonsmartapp.hello.showme;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.suwonsmartapp.hello.R;

import java.util.ArrayList;

public class VideoPlayerActivity extends AppCompatActivity implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        SeekBar.OnSeekBarChangeListener, View.OnClickListener {

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

    private SeekBar mSB_volume;
    private Button mBTN_play;
    private Button mBTN_pause;
    private Button mBTN_stop;

    private int volume_Max = 0;
    private int volume_Current = 0;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_players);
        showLog("onCreate");

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = getIntent();
        mCurrentPosition = intent.getIntExtra("currentPosition", -1);
        mVideoFileInfoList = intent.getParcelableArrayListExtra("videoInfoList");

        videoFileInfo = mVideoFileInfoList.get(mCurrentPosition);
        fullPathname = videoFileInfo.getMediaData();
        int i = fullPathname.lastIndexOf('/');
        int j = fullPathname.length();
        requestedPathname = fullPathname.substring(0, i);          // get requested pathname
        requestedFilename = fullPathname.substring(i + 1, j);      // and filename

        // 위젯 셋팅
        mVV_show = (VideoView) findViewById(R.id.vv_show);
        mSB_volume = (SeekBar) findViewById(R.id.sb_volume);
        mBTN_play = (Button) findViewById(R.id.btn_play);
        mBTN_pause = (Button) findViewById(R.id.btn_pause);
        mBTN_stop = (Button) findViewById(R.id.btn_stop);

        // VideoView에 미디어 컨트롤러 추가
        MediaController mController = new MediaController(this);
        mVV_show.setMediaController(mController);

        // VideoView에 경로 지정
        mVV_show.setVideoPath(fullPathname);
        // VideoView에 포커스하도록 지정
        mVV_show.requestFocus();

        // 동영상 재생 준비 완료, 재생 완료 리스너
        mVV_show.setOnPreparedListener(this);
        mVV_show.setOnCompletionListener(this);

        // 볼륨 조절 셋팅
        volume_Max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume_Current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mSB_volume.setMax(volume_Max);
        mSB_volume.setProgress(volume_Current);
        mSB_volume.setOnSeekBarChangeListener(this);

        // 버튼 리스터 셋팅
        mBTN_play.setOnClickListener(this);
        mBTN_pause.setOnClickListener(this);
        mBTN_stop.setOnClickListener(this);
        // 컨트롤러를 사용하는 경우 버튼으로 MediaPlayer를 제어할 필요는 없다.

    }


    //====================================================
    // 동영상 재생 관련 상속 메소드
    //====================================================
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        showToast("동영상 재생 준비가 완료되었습니다.");
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        showToast("동영상 재생이 끝났습니다.");
    }


    //====================================================
    // 볼륨 관련 상속 메소드
    //====================================================
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}


    //====================================================
    // 버튼 이벤트 상속 메소드
    //====================================================
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_play :
                mVV_show.seekTo(0);
                mVV_show.start();
                break;
            case R.id.btn_pause :
                mVV_show.pause();
                break;
            case R.id.btn_stop :
                mVV_show.stopPlayback();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
