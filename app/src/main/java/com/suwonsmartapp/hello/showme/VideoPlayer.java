package com.suwonsmartapp.hello.showme;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import com.suwonsmartapp.hello.R;

public class VideoPlayer extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    // 흐름 확인용 로그 출력 메소드
    private static String TAG = "로그 / 비디오플레이어 ";
    private void show_Log(String msg) { Log.d(TAG, msg); }

    private VideoView mVV_show;
    private ActionBar action_Bar;   // 액션바

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 화면을 landscape(가로) 화면으로 고정하고 싶은 경우
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_video);

        // 액션바 선언
        action_Bar = this.getSupportActionBar();
        if (action_Bar != null) {
            action_Bar.hide();
        }

        MediaController mController = new MediaController(this);
        mVV_show = (VideoView) findViewById(R.id.vv_show);
        mVV_show.setMediaController(mController);

        Uri uri = getIntent().getData();
        mVV_show.setVideoURI(uri);

        //전체화면 만들기
        //LinearLayout.LayoutParams mParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        //mVV_show.setLayoutParams(mParam);

        //종료 이벤트 리스너
        mVV_show.setOnCompletionListener(this);
        mVV_show.start();
    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        show_Log("end end");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
