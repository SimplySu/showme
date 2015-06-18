package com.suwonsmartapp.hello.showme.audio;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AudioPlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = AudioPlayerActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }
    private static final String HOME = "com.suwonsmartapp.hello.showme.";

    private static final int MSG_GET_MP = 1;
    private static final int MSG_NEXT_MP = 2;

    private TextView mTvAudioPlayerTitle;
    private ImageView mIvAudioPlayerPicture;
    private SeekBar mSbAudioPlayerSeekbar;
    private TextView mTvAudioPlayerTimeRight;
    private TextView mTvAudioPlayerTimeLeft;

    private ImageButton mIbAudioPlayerPlay;
    private ImageButton mIbAudioPlayerForward;
    private ImageButton mIbAudioPlayerBackward;
    private ImageButton mIbAudioPlayerRevert;

    private static MediaPlayer mMediaPlayer;

    private double TimeLeft = 0;
    private double TimeRight = 0;

    private int mCurrentPosition = 0;
    private ArrayList<AudioFileInfo> mAudioFileInfoList;
    private AudioFileInfo mPlayAudioFileInfo;

    private boolean mIsReceiverRegistered;

    public static final int RESULT_OK = 0x0fff;
    public static final int REQUEST_CODE_AUDIO = 0x0001;
    public static final int REQUEST_CODE_AUDIO_PLAYER = 0x0002;
    public static final int REQUEST_CODE_VIDEO = 0x0010;
    public static final int REQUEST_CODE_VIDEO_PLAYER = 0x0020;
    public static final int REQUEST_CODE_IMAGE = 0x0100;
    public static final int REQUEST_CODE_IMAGE_PLAYER = 0x0200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_main_player);

//        showLog("onCreate");

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setupViews();

        Intent intent = getIntent();
        mAudioFileInfoList = intent.getParcelableArrayListExtra("songInfoList");
        mCurrentPosition = intent.getIntExtra("currentPosition", -1);
        mPlayAudioFileInfo = mAudioFileInfoList.get(mCurrentPosition);

//        SeekBar seekVolumn = (SeekBar) findViewById(R.id.SeekBar_Volumn);
//        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
//        int nMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        int nCurrentVolumn = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        seekVolumn.setMax(nMax); seekVolumn.setProgress(nCurrentVolumn);
//        seekVolumn.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//            }
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//            }
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
//            }
//        });

        // connect event handler on the seekbar
        mSbAudioPlayerSeekbar = (SeekBar) findViewById(R.id.audio_player_seekbar);
        mSbAudioPlayerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mMediaPlayer.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        registerCallReceiver();
    }

    private void setupViews() {
//        showLog("setupViews");

        mIvAudioPlayerPicture = (ImageView) findViewById(R.id.audio_player_picture);
        mTvAudioPlayerTitle = (TextView) findViewById(R.id.audio_player_title);
        mTvAudioPlayerTimeLeft = (TextView) findViewById(R.id.auido_player_time_left);
        mTvAudioPlayerTimeRight = (TextView) findViewById(R.id.audio_player_time_right);

        mIbAudioPlayerBackward = (ImageButton) findViewById(R.id.audio_player_backward);
        mIbAudioPlayerBackward.setOnClickListener(this);

        mIbAudioPlayerPlay = (ImageButton) findViewById(R.id.audio_player_play);
        mIbAudioPlayerPlay.setOnClickListener(this);

        mIbAudioPlayerForward = (ImageButton) findViewById(R.id.audio_player_forward);
        mIbAudioPlayerForward.setOnClickListener(this);

        mIbAudioPlayerRevert = (ImageButton) findViewById(R.id.audio_player_showlist);
        mIbAudioPlayerRevert.setOnClickListener(this);

        mMediaPlayer = new MediaPlayer();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        showLog("onStart");

        // setup index of current song on the list and audio file list
        Intent serviceIPC = new Intent(getApplicationContext(), AudioMessengerService.class);
        serviceIPC.putExtra("currentPosition", mCurrentPosition);
        serviceIPC.putParcelableArrayListExtra("songInfoList", mAudioFileInfoList);
        startService(serviceIPC);

        bindService(serviceIPC, mConnectionMessenger, Context.BIND_ADJUST_WITH_ACTIVITY);

        if (AudioMessengerService.isPaused) {
            mIbAudioPlayerPlay.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        showLog("onResume");

//        For volume control...
//        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        showLog("onDestroy");

        if (mBoundMessenger) {
            unbindService(mConnectionMessenger);
            mBoundMessenger = false;
        }

        if (mMusicThread != null && mMusicThread.isAlive()) {
            mMusicThread.interrupt();
        }
        unregisterCallReceiver();
    }

    // Messenger for communicating with the service
    private static Messenger mServiceMessenger;
    // Flag indicating whether we have called bind on the service
    private boolean mBoundMessenger;

    private ServiceConnection mConnectionMessenger = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mServiceMessenger = new Messenger(service);
            mBoundMessenger = true;
            setMusicUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            mBoundMessenger = false;
        }
    };

    private void setMusicUI() {
//        showLog("setMusicUI");

        mTvAudioPlayerTitle.setText(mPlayAudioFileInfo.getTitle() + " - " + mPlayAudioFileInfo.getArtist());
        mPlayAudioFileInfo.setAlbumArt(getAlbumArt(mPlayAudioFileInfo.getAlbumId()));

        if (mPlayAudioFileInfo.getAlbumArt() != null) {
            if (mPlayAudioFileInfo.getTitle().toLowerCase().lastIndexOf(".mp3") == -1) {
                mIvAudioPlayerPicture.setImageResource(R.drawable.audio_music_large);
            } else {
                mIvAudioPlayerPicture.setImageBitmap(mPlayAudioFileInfo.getAlbumArt());
            }
        } else {
            mIvAudioPlayerPicture.setImageResource(R.drawable.audio_music_large);
        }

        if (mMediaPlayer != null) {
            mMusicThread = getmMusicThread();
            mMusicHandler.postDelayed(mMusicThread, 100);
        }
    }

    private Bitmap getAlbumArt(int albumId) {
//        showLog("getAlbumArt");

        return AudioPlayerAlbumImage.getArtworkQuick(getApplicationContext(), albumId, 300, 300);
    }

    private final MusicHandler mMusicHandler = new MusicHandler();

    public static class MusicHandler extends Handler {

        public MusicHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_MP:
                    // get media player info from the service
                    mMediaPlayer = (MediaPlayer) msg.obj;
                    break;

                case MSG_NEXT_MP:
                    // play next song when we finish current playing
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private Thread mMusicThread;

    private Thread getmMusicThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
            synchronized (mMediaPlayer) {
                if (mMediaPlayer != null) {
                    try {
                        if (AudioMessengerService.isPaused) {
                            mIbAudioPlayerPlay.setImageResource(android.R.drawable.ic_media_play);
                        } else {
                            mIbAudioPlayerPlay.setImageResource(android.R.drawable.ic_media_pause);
                        }

                        TimeLeft = mMediaPlayer.getCurrentPosition();
                        mTvAudioPlayerTimeLeft.setText(String.format("%02d : %02d",
                                        TimeUnit.MILLISECONDS.toMinutes((long) TimeLeft),
                                        TimeUnit.MILLISECONDS.toSeconds((long) TimeLeft) -
                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) TimeLeft)))
                        );

                        TimeRight = mMediaPlayer.getDuration();
                        mSbAudioPlayerSeekbar.setMax((int) TimeRight);
                        mTvAudioPlayerTimeRight.setText(String.format("%02d : %02d",
                                        TimeUnit.MILLISECONDS.toMinutes((long) TimeRight),
                                        TimeUnit.MILLISECONDS.toSeconds((long) TimeRight) -
                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) TimeRight)))
                        );

                        mSbAudioPlayerSeekbar.setProgress((int) TimeLeft);
                        mMusicHandler.postDelayed(mMusicThread, 100);

                    } catch (IllegalStateException e) {
                        // Thrown when an action is attempted at a time when the VM is not in the correct state
                        showLog("IllegalStateException");
                    }
                }
            }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.audio_player_play:
                restartOrPause();
                break;

            case R.id.audio_player_forward:
                next();
                break;

            case R.id.audio_player_backward:
                previous();
                break;

            case R.id.audio_player_showlist:
                backToList();
                break;
        }
    }

    private void restartOrPause() {
//        showLog("restartOrPause");

        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Play");
        sendBroadcast(songListActivity);
    }

    private void previous() {
//        showLog("previous");

        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Previous");
        sendBroadcast(songListActivity);
    }

    private void next() {
//        showLog("next");

        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Next");
        sendBroadcast(songListActivity);
    }

    private void backToList() {
//        showLog("backToList");

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private BroadcastReceiver mBRPlayer = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            showLog("Broadcast Receiver : " + intent.getAction());

            String action = intent.getAction();
            if ((HOME + "AudioPlayerActivity.STOP").equals(action)) {
                if (mBoundMessenger) {
                    unbindService(mConnectionMessenger);
                    mBoundMessenger = false;
                }
                finish();
            } else if ((HOME + "AudioPlayerActivity.songChanged").equals(action)) {
                mCurrentPosition = intent.getIntExtra("currentPosition", -1);
                mPlayAudioFileInfo = mAudioFileInfoList.get(mCurrentPosition);
                setMusicUI();
            }
        }
    };

    private void registerCallReceiver() {
//        showLog("registerCallReceiver");

        if(!mIsReceiverRegistered){
            IntentFilter filter = new IntentFilter();
            filter.addAction(HOME + "AudioPlayerActivity.STOP");
            filter.addAction(HOME + "AudioPlayerActivity.songChanged");
            registerReceiver(mBRPlayer, filter);

            mIsReceiverRegistered = true;
        }
    }

    private void unregisterCallReceiver() {
//        showLog("unregisterCallReceiver");

        if(mIsReceiverRegistered){
            unregisterReceiver(mBRPlayer);
            mIsReceiverRegistered = false;
        }
    }
}
