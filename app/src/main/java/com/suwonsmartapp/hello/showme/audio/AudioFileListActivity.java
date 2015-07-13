package com.suwonsmartapp.hello.showme.audio;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;
import com.suwonsmartapp.hello.showme.file.FileAdapter;
import com.suwonsmartapp.hello.showme.file.FileInfo;
import com.suwonsmartapp.hello.showme.file.FileLists;
import com.suwonsmartapp.hello.showme.file.FileThumbnail;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AudioFileListActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = AudioFileListActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }
    private static final String HOME = "com.suwonsmartapp.hello.showme.";

    private ArrayList<FileInfo> musicList;
    private final int MODEall = 0;
    private final int MODEaudio = 1;
    private final int MODEimage = 2;
    private final int MODEvideo = 3;

    private boolean mIsReceiverRegistered;

    private ListView mLvMusicList;
    private FileAdapter mFileAdapter;

    // -1은 파일이 특정되지 않았음을 나타냄. (초기값)
    private static int mCurrentPosition = -1;
    private static MediaPlayer mMediaPlayer;
    private static final int MSG_GET_MP_IN_LIST = 3;

    private LinearLayout mLlMiniPlayer;
    private ImageView mIvAlbum;
    private TextView mTvSongTitle;
    private TextView mTvMiniPlayerStartTime;
    private TextView mTvMiniPlayerFinalTime;

    private SeekBar mSbMiniPlayer;
    private Button mBtnPrevious;
    private Button mBtnPlay;
    private Button mBtnNext;

    // 인텐트를 통해 받은 경로명과 파일명.
    private String requestedPathname = "";
    private String requestedFilename = "";

    private int TimeLeft;
    private int TimeRight;

    // 파일 매니저를 통해 건네받은 파일명.
    private String value;

    public static final int RESULT_OK = 0x0fff;
    public static final int REQUEST_CODE_AUDIO = 0x0001;
    public static final int REQUEST_CODE_AUDIO_PLAYER = 0x0002;
    public static final int REQUEST_CODE_VIDEO = 0x0010;
    public static final int REQUEST_CODE_VIDEO_PLAYER = 0x0020;
    public static final int REQUEST_CODE_IMAGE = 0x0100;
    public static final int REQUEST_CODE_IMAGE_PLAYER = 0x0200;
    private Bundle extraAudioService;
    private Intent intentAudioService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_list_and_mini_player);

        // 화면을 세로모드로 고정함.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 인텐트를 통해 경로명과 파일명을 읽음.
        readIntent();

        // 실행할 음악 파일만을 추출함.
        prepareTitleToPlay(requestedPathname);
        setupViews();

        // 오디오 플레이어가 다시 시작되어 새로운 곡을 재생하기 위해 오디오 서비스를 멈춤.
        close();   // 브로드캐스팅을 통해 명령을 실행하므로 서비스가 실행되지 않고 있어도 문제없음.

        mFileAdapter = new FileAdapter(getApplicationContext(), musicList);
        mLvMusicList = (ListView) findViewById(R.id.lv_music_list);
        mLvMusicList.setAdapter(mFileAdapter);
        mLvMusicList.setOnItemClickListener(this);

        // 이벤트 핸들러를 씨크바에 연결함.
        mSbMiniPlayer = (SeekBar) findViewById(R.id.mini_audio_player_seekbar);
        mSbMiniPlayer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) { mMediaPlayer.seekTo(progress); }
            }
           @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        registerCallReceiver();

        // 특정 파일을 지정한 경우 여기부터 실행함.
        mCurrentPosition = searchTitleIndex(requestedFilename);
        updateTitleListView();
        mLvMusicList.smoothScrollToPosition(mCurrentPosition);

        Intent initialIntent = new Intent(getApplicationContext(), AudioPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);
        initialIntent.putParcelableArrayListExtra("songInfoList", musicList);
        startActivity(initialIntent);
    }

    // 인텐트를 통해 경로명과 파일명을 읽음.
    private void readIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("FilePath")) {
                value = intent.getStringExtra("FilePath");
                showLog(value);
            } else {
                showToast(getString(R.string.msg_wrong_file));
                finish();
            }
            requestedPathname = value.substring(0, value.lastIndexOf('/'));
            requestedFilename = value.substring(value.lastIndexOf('/') + 1, value.length());
        }
    }

    private void setupViews() {
        mLlMiniPlayer = (LinearLayout) findViewById(R.id.mini_audio_player);
        mIvAlbum = (ImageView) findViewById(R.id.mini_audio_player_icon);
        mTvSongTitle = (TextView) findViewById(R.id.mini_audio_player_title);
        mTvSongTitle.setSelected(true);

        mTvMiniPlayerStartTime = (TextView) findViewById(R.id.mini_audio_player_time_left);
        mTvMiniPlayerFinalTime = (TextView) findViewById(R.id.mini_audio_player_time_right);

        mBtnPrevious = (Button) findViewById(R.id.btn_previous_song);
        mBtnPrevious.setOnClickListener(this);

        mBtnPlay = (Button) findViewById(R.id.btn_play_song);
        mBtnPlay.setOnClickListener(this);

        mBtnNext = (Button) findViewById(R.id.btn_next_song);
        mBtnNext.setOnClickListener(this);
    }

    // 지정한 파일이 재생 가능한지 검사함.
    private int searchTitleIndex(String rf) {
        for (int i = 0; i < musicList.size(); i++) {
            FileInfo fileInfo = musicList.get(i);
            File f = fileInfo.getFile();
            if (rf.equals(f.getName())) {
                return i;          // 일치하는 인덱스를 리턴함.
            }
        }
        return 0;                  // 일치하는 파일이 없으면 처음부터 재생함.
    }

    private void close() {
        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Close");
        sendBroadcast(songListActivity);
    }

    @Override
    protected void onResume() {
        // 실행할 음악 파일만을 추출함.
        prepareTitleToPlay(requestedPathname);
        setupViews();

        mFileAdapter = new FileAdapter(getApplicationContext(), musicList);
        mLvMusicList = (ListView) findViewById(R.id.lv_music_list);
        mLvMusicList.setAdapter(mFileAdapter);
        mLvMusicList.setOnItemClickListener(this);

        registerCallReceiver();

        // 특정 파일을 지정한 경우 여기부터 실행함.
        mCurrentPosition = searchTitleIndex(requestedFilename);
        updateTitleListView();
        mLvMusicList.smoothScrollToPosition(mCurrentPosition);

        super.onResume();

        if (isAudioPlayerServiceRunning()) {
            setMiniPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        extraAudioService = new Bundle();
        intentAudioService = new Intent();
        extraAudioService.putInt("CurrentPosition", mCurrentPosition);
        intentAudioService.putExtras(extraAudioService);
        this.setResult(RESULT_OK, intentAudioService);

        super.onDestroy();

        if (mBoundMessenger) {
            unbindService(mServiceConnection);
            mBoundMessenger = false;
        }

        if (mMusicThread != null && mMusicThread.isAlive()) {
            mMusicThread.interrupt();
        }
        unregisterCallReceiver();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCurrentPosition = position;
        updateTitleListView();

        Intent initialIntent = new Intent(getApplicationContext(), AudioPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);
        initialIntent.putParcelableArrayListExtra("songInfoList", musicList);
        startActivity(initialIntent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_previous_song:
                previous();                 // 이전 곡을 재생함
                break;

            case R.id.btn_next_song:
                next();                     // 다음 곡을 재생함
                break;

            case R.id.btn_play_song:
                restartOrPause();           // 재생을 재시작하거나 멈춤
                break;
        }
    }

    // 재생 가능한 음악 파일만 리스트로 만듬.
    private void prepareTitleToPlay(String rp) {
        musicList = new FileLists().getFileList(rp, MODEaudio);
        if (musicList == null) {
            showToast(getString(R.string.msg_no_music));          // 재생할 파일이 없음.
        }
    }

    private void setMiniPlayer() {
        Intent service = new Intent(getApplicationContext(), AudioMessengerService.class);
        service.putExtra("activity", TAG);
        bindService(service, mServiceConnection, Context.BIND_ADJUST_WITH_ACTIVITY);
        mMusicHandler.postDelayed(mMusicThread, 100);
    }

    private Messenger mMsgOfListAndService;
    private boolean mBoundMessenger;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMsgOfListAndService = new Messenger(service);
            mBoundMessenger = true;

            Message msg = Message.obtain(null, AudioMessengerService.MSG_GET_MP_IN_LIST, 0, 0);
            try {
                mMsgOfListAndService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMsgOfListAndService = null;
            mBoundMessenger = false;
        }
    };

    private final Thread mMusicThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                if (mMediaPlayer != null) {
                    if (AudioMessengerService.isPaused) {   // player paused ?
                        mBtnPlay.setText("PLAY");
                    } else {
                        mBtnPlay.setText("STOP");
                    }

                    TimeLeft = mMediaPlayer.getCurrentPosition();
                    mTvMiniPlayerStartTime.setText(String.format("%02d : %02d",
                        TimeUnit.MILLISECONDS.toMinutes((long) TimeLeft),
                        TimeUnit.MILLISECONDS.toSeconds((long) TimeLeft) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                                        .toMinutes((long) TimeLeft)))
                    );

                    setupMiniPlayer();

                    TimeRight = mMediaPlayer.getDuration();
                    mSbMiniPlayer.setMax(TimeRight);
                    mTvMiniPlayerFinalTime.setText(String.format("%02d : %02d",
                                    TimeUnit.MILLISECONDS.toMinutes((long) TimeRight),
                                    TimeUnit.MILLISECONDS.toSeconds((long) TimeRight) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                                                    .toMinutes((long) TimeRight)))
                    );

                    mSbMiniPlayer.setProgress(TimeLeft);
                }
            } catch (IllegalStateException e) {
            }
            mMusicHandler.postDelayed(mMusicThread, 100);
        }
    });

    private final MusicHandler mMusicHandler = new MusicHandler();

    public static class MusicHandler extends Handler {

        public MusicHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_MP_IN_LIST:
                    mMediaPlayer = (MediaPlayer) msg.obj;
                    mCurrentPosition = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    // 오디오 플레이어가 작동중이면 true를 리턴함.
    private boolean isAudioPlayerServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AudioMessengerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void setupMiniPlayer() {
        if (mCurrentPosition >=0 && mCurrentPosition < musicList.size()) {
            changeMiniPlayerUI();
        } else if (mCurrentPosition < 0) {
            mCurrentPosition = 0;
            changeMiniPlayerUI();
        } else if (mCurrentPosition >= musicList.size()) {
            mCurrentPosition = musicList.size() - 1;
            changeMiniPlayerUI();
        }
    }

    private void changeMiniPlayerUI() {
        FileInfo playSong = musicList.get(mCurrentPosition);
        Bitmap albumArt = FileThumbnail.getAudioThumbnail(getApplicationContext(), playSong.getTitle());

        if (albumArt != null) {
            if (playSong.getTitle().toLowerCase().lastIndexOf(".mp3") == -1) {
                mIvAlbum.setImageResource(R.drawable.audio_music_small);
            } else {
                mIvAlbum.setImageBitmap(albumArt);
            }
        } else {
            mIvAlbum.setImageResource(R.drawable.audio_music_small);
        }

        mLlMiniPlayer.setVisibility(View.VISIBLE);
        String song = playSong.getTitle();
        mTvSongTitle.setText(song.substring(song.lastIndexOf("/") + 1, song.length()));

        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mBtnPlay.setText("PLAY");
        }
    }

    private void updateTitleListView() {
        mFileAdapter.setmCurrentPosition(mCurrentPosition);
        mFileAdapter.notifyDataSetChanged();
    }

    private void restartOrPause() {
        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Play");
        sendBroadcast(songListActivity);
    }

    private void previous() {
        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Previous");
        sendBroadcast(songListActivity);
    }

    private void next() {
        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Next");
        sendBroadcast(songListActivity);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ((HOME + "AudioFileListActivity.STOP").equals(action)) {
                if (mBoundMessenger) {
                    unbindService(mServiceConnection);
                    mBoundMessenger = false;
                }
                finish();
            } else if ((HOME + "AudioPlayerActivity.songChanged").equals(action)) {
                mCurrentPosition = intent.getIntExtra("currentPosition", -1);
                setupMiniPlayer();
                mFileAdapter.setmCurrentPosition(mCurrentPosition);
                mFileAdapter.notifyDataSetChanged();
            }
        }
    };

    private void registerCallReceiver(){
        if(!mIsReceiverRegistered){
            IntentFilter filter = new IntentFilter();
            filter.addAction(HOME + "AudioFileListActivity.STOP");
            filter.addAction(HOME + "AudioPlayerActivity.songChanged");
            registerReceiver(mBroadcastReceiver, filter);
            mIsReceiverRegistered = true;
        }
    }

    private void unregisterCallReceiver(){
        if(mIsReceiverRegistered){
            unregisterReceiver(mBroadcastReceiver);
            mIsReceiverRegistered = false;
        }
    }
}
