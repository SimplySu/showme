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

    private ListView mLvMusicList;                          // music list view
    private FileAdapter mFileAdapter;                       // audio list adapter

    private static int mCurrentPosition = -1;               // -1 means we didn't specify title
    private static MediaPlayer mMediaPlayer;                // media player member variable
    private static final int MSG_GET_MP_IN_LIST = 3;

    private LinearLayout mLlMiniMiniPlayer;             // for visibility setting
    private ImageView mIvAlbum;                         // media_player_icon_android
    private TextView mTvSongTitle;                      // title
    private TextView mTvMiniPlayerStartTime;            // time left
    private TextView mTvMiniPlayerFinalTime;            // time right

    private SeekBar mSbMiniPlayer;                      // seekbar
    private Button mBtnPrevious;                        // previous
    private Button mBtnPlay;                            // play or stop
    private Button mBtnNext;                            // next

    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent

    private int TimeLeft;                           // progressing time
    private int TimeRight;                          // music duration

    private String value;                           // filename passed by file manager

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
        setContentView(R.layout.audio_list_and_mini_player);

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        readIntent();                       // get pathname and filename

        prepareTitleToPlay();               // setup titles for playing
        setupViews();                       // setup view

        mFileAdapter = new FileAdapter(getApplicationContext(), musicList);
        mLvMusicList = (ListView) findViewById(R.id.lv_music_list);
        mLvMusicList.setAdapter(mFileAdapter);
        mLvMusicList.setOnItemClickListener(this);      // handle if user selected title directly

        // connect event handler on the seekbar
        mSbMiniPlayer = (SeekBar) findViewById(R.id.mini_audio_player_seekbar);
        mSbMiniPlayer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mMediaPlayer.seekTo(progress);      // change progress if user changed seekbar
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        registerCallReceiver();

        mCurrentPosition = searchTitleIndex();      // search title index which was specified by user
        updateTitleListView();
        mLvMusicList.smoothScrollToPosition(mCurrentPosition);

        Intent initialIntent = new Intent(getApplicationContext(), AudioPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);       // current title position
        initialIntent.putParcelableArrayListExtra("songInfoList", musicList);
        startActivity(initialIntent);
    }

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
        mLlMiniMiniPlayer = (LinearLayout) findViewById(R.id.mini_audio_player);
        mIvAlbum = (ImageView) findViewById(R.id.mini_audio_player_icon);
        mTvSongTitle = (TextView) findViewById(R.id.mini_audio_player_title);
        mTvSongTitle.setSelected(true);                 // set focus for marque

        mTvMiniPlayerStartTime = (TextView) findViewById(R.id.mini_audio_player_time_left);
        mTvMiniPlayerFinalTime = (TextView) findViewById(R.id.mini_audio_player_time_right);

        mBtnPrevious = (Button) findViewById(R.id.btn_previous_song);
        mBtnPrevious.setOnClickListener(this);      // setup listener for previous button

        mBtnPlay = (Button) findViewById(R.id.btn_play_song);
        mBtnPlay.setOnClickListener(this);          // setup listener for play button

        mBtnNext = (Button) findViewById(R.id.btn_next_song);
        mBtnNext.setOnClickListener(this);          // setup listener for next button
    }

    // search matched title with specified by user
    private int searchTitleIndex() {
        for (int i = 0; i < musicList.size(); i++) {
            FileInfo fileInfo = musicList.get(i);    // read image file
            File f = fileInfo.getFile();
            if (requestedFilename.equals(f.getName())) {
                return i;          // return matched index
            }
        }
        return 0;                  // default is the first picture
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAudioPlayerServiceRunning()) {
            setMiniPlayer();
        }
    }

    @Override
    protected void onDestroy() {
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
        mCurrentPosition = position;        // save position when user clicked current view
        updateTitleListView();

        Intent intent = new Intent(getApplicationContext(), AudioPlayerActivity.class);
        intent.putExtra("currentPosition", mCurrentPosition);       // current title position
        intent.putParcelableArrayListExtra("songInfoList", musicList);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_previous_song:
                previous();                 // play the previous title
                break;

            case R.id.btn_next_song:
                next();                     // play the next title
                break;

            case R.id.btn_play_song:
                restartOrPause();           // restart or pause playing
                break;
        }
    }

    private void prepareTitleToPlay() {
        musicList = new FileLists().getFileList(requestedPathname, MODEaudio);
        if (musicList == null) {
            showToast(getString(R.string.msg_no_music));          // no music found
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

            // get audio player icon information
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
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) TimeLeft)))
                    );

                    setupMiniPlayer();

                    TimeRight = mMediaPlayer.getDuration();
                    mSbMiniPlayer.setMax(TimeRight);
                    mTvMiniPlayerFinalTime.setText(String.format("%02d : %02d",
                                    TimeUnit.MILLISECONDS.toMinutes((long) TimeRight),
                                    TimeUnit.MILLISECONDS.toSeconds((long) TimeRight) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) TimeRight)))
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
                case MSG_GET_MP_IN_LIST:    // get audio player media_player_icon_information from service
                    mMediaPlayer = (MediaPlayer) msg.obj;
                    mCurrentPosition = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    // return true if audio player service is running
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
        Bitmap albumArt = FileAdapter.getAudioThumbnail(getApplicationContext(), playSong.getTitle());

        if (albumArt != null) {
            if (playSong.getTitle().toLowerCase().lastIndexOf(".mp3") == -1) {
                mIvAlbum.setImageResource(R.drawable.audio_music_small);    // draw default icon
            } else {
                mIvAlbum.setImageBitmap(albumArt);      // album icon found
            }
        } else {
            mIvAlbum.setImageResource(R.drawable.audio_music_small);    // draw default icon
        }

        mLlMiniMiniPlayer.setVisibility(View.VISIBLE);
        mTvSongTitle.setText(playSong.getTitle());

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
