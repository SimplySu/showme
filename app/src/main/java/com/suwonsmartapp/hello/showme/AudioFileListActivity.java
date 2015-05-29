package com.suwonsmartapp.hello.showme;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AudioFileListActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = AudioFileListActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }
    private static final String HOME = "com.suwonsmartapp.hello.showme.";

    private AudioFileInfo audioFileInfo;                    // audio file info getting by cursor
    private boolean mIsReceiverRegistered;

    private ArrayList<AudioFileInfo> mAudioFileInfoList;    // audio file media_player_icon_information list
    private ListView mLvMusicList;                          // music list view
    private AudioListAdapter mAudioListAdapter;             // audio list adapter
    private Cursor mCursor;                                 // cursor for media store searching

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_list_and_mini_player);
        showLog("onCreate");

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        readIntent();                       // get pathname and filename

        setupViews();                       // setup view
        prepareTitleToPlay();               // setup titles for playing

        mAudioListAdapter = new AudioListAdapter(getApplicationContext(), mAudioFileInfoList, mCurrentPosition);
        mLvMusicList = (ListView) findViewById(R.id.lv_music_list);
        mLvMusicList.setAdapter(mAudioListAdapter);
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
        initialIntent.putParcelableArrayListExtra("songInfoList", mAudioFileInfoList);
        startActivity(initialIntent);
    }

    private void readIntent() {
        Intent intent = getIntent();
        if(intent.hasExtra("FilePath")) {
            value = intent.getStringExtra("FilePath");
            showLog(value);
        } else {
            showToast("잘못된 파일입니다.");
            finish();
        }

        requestedPathname = value.substring(0, value.lastIndexOf('/'));
        requestedFilename = value.substring(value.lastIndexOf('/') + 1, value.length());
    }

    private void setupViews() {
        showLog("setupViews");
        mAudioFileInfoList = new ArrayList<>();         // create audio file lists

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
        for (int i = 0; i < mAudioFileInfoList.size(); i++) {
            AudioFileInfo audioFileInfo = mAudioFileInfoList.get(i);    // read audio file
            if (requestedFilename.equals(audioFileInfo.getDisplayName())) {
                return i;          // return matched index
            }
        }
        return 0;                  // default is the first title
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLog("onResume - isAudioPlayerServiceRunning : " + String.valueOf(isAudioPlayerServiceRunning()));

        if (isAudioPlayerServiceRunning()) {
            setMiniPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        showLog("onDestroy  - mBoundMessenger : " + String.valueOf(mBoundMessenger));

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
        showLog("onItemClick");
        mCurrentPosition = position;        // save position when user clicked current view
        updateTitleListView();

        Intent intent = new Intent(getApplicationContext(), AudioPlayerActivity.class);
        intent.putExtra("currentPosition", mCurrentPosition);       // current title position
        intent.putParcelableArrayListExtra("songInfoList", mAudioFileInfoList);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        showLog("onClick");
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
        showLog("prepareTitleToPlay");

        // query : syncronized processing (can be slow)
        // loader : asyncronized processing

        String[] projection = {
                MediaStore.Audio.Media._ID,                 // album ID
                MediaStore.Audio.Media.ARTIST,              // artist
                MediaStore.Audio.Media.TITLE,               // title
                MediaStore.Audio.Media.DATA,                // full pathname
                MediaStore.Audio.Media.DISPLAY_NAME,        // filename
                MediaStore.Audio.Media.DURATION,            // play time
                MediaStore.Audio.Media.ALBUM_ID,            // album ID
                MediaStore.MediaColumns.DATA
        };

        String selection = MediaStore.Audio.Media.DATA + " like ?";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        mCursor = getContentResolver()
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,    // The content URI of the words table
                        projection,                 // The columns to return for each row
                        selection,                  //  selection criteria
                        new String[] {requestedPathname + "/%"},        // Selection criteria
                        sortOrder);                 // The sort order for the returned rows

        showLog("query result : " + String.valueOf(mCursor));

        mAudioFileInfoList = new ArrayList<>();     // initialize info list

        if (mCursor != null) {
            mCursor.moveToFirst();              // from the start of data base

            showLog("searched file count : " + String.valueOf(mCursor.getCount()));

            for (int i = 0; i < mCursor.getCount(); i++) {
                mCursor.moveToPosition(i);      // get next row of data base

                if (isDirectoryMatch()) {      // select matched directory only
                    audioFileInfo = new AudioFileInfo();
                    audioFileInfo.setId(mCursor.getLong(0));                // music ID
                    audioFileInfo.setArtist(mCursor.getString(1));          // artist
                    audioFileInfo.setTitle(mCursor.getString(2));           // title
                    audioFileInfo.setMediaData(mCursor.getString(3));       // full path of the music
                    audioFileInfo.setDisplayName(mCursor.getString(4));     // brief music name to show
                    audioFileInfo.setDuration(mCursor.getLong(5));          // playing time
                    audioFileInfo.setAlbumId(mCursor.getInt(6));            // album ID
                    audioFileInfo.setColumnsData(mCursor.getString(7));

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioFileInfo.getId());
                    audioFileInfo.setSongUri(contentUri);                   // get music media_player_icon_android

                    mAudioFileInfoList.add(audioFileInfo);                  // register music on the play list
                }
            }
        } else {
            showToast("음악 파일이 없습니다.");          // no music found
        }
    }

    // return true if current file's directory is matching with user selection,
    // return false if it is not.
    // we will include subdirectories also.
    private boolean isDirectoryMatch() {
        String fullPath = mCursor.getString(3);         // get full path name
        int i = fullPath.lastIndexOf('/');              // search last slash
        int j = fullPath.length();                      // get fullpath's length
        String pathname = fullPath.substring(0, i);     // get pathname only
        String filename = fullPath.substring(i + 1, j); // get filename only

        showLog(filename);

        int k = requestedPathname.length();             // get requested path length
        int l = pathname.length();                      // get current pathname length
        if (l < k) {                                    // if current pathname is shorter than requested
            return false;                               // we don't need to compare it
        }

        String s = pathname.substring(0, k);            // compare just we requested for subdirectory
        return s.equals(requestedPathname);             // see if this directory is matching ?
    }

    private void setMiniPlayer() {
        showLog("setMiniPlayer");
        Intent service = new Intent(getApplicationContext(), AudioMessengerService.class);
        service.putExtra("activity", AudioFileListActivity.class.getSimpleName());
        bindService(service, mServiceConnection, Context.BIND_ADJUST_WITH_ACTIVITY);

        mMusicHandler.postDelayed(mMusicThread, 100);
    }

    private Messenger mMsgOfListAndService;
    private boolean mBoundMessenger;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            showLog("onServiceConnected");
            mMsgOfListAndService = new Messenger(service);
            mBoundMessenger = true;

            // get audio player media_player_icon_information
            Message msg = Message.obtain(null, AudioMessengerService.MSG_GET_MP_IN_LIST, 0, 0);
            try {
                mMsgOfListAndService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            showLog("onServiceDisconnected");
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
                showLog("isAudioPlayerServiceRunning : true");
                return true;
            }
        }
        showLog("isAudioPlayerServiceRunning : false");
        return false;
    }

    private void setupMiniPlayer() {
        if (mCurrentPosition >=0 && mCurrentPosition < mAudioFileInfoList.size()) {
            changeMiniPlayerUI();
        } else if (mCurrentPosition < 0) {
            mCurrentPosition = 0;
            changeMiniPlayerUI();
        } else if (mCurrentPosition >= mAudioFileInfoList.size()) {
            mCurrentPosition = mAudioFileInfoList.size() - 1;
            changeMiniPlayerUI();
        }
    }

    private void changeMiniPlayerUI() {
        AudioFileInfo playSong = mAudioFileInfoList.get(mCurrentPosition);

        // get album media_player_icon_android bitmap image from the media store
        Bitmap albumArt = AudioPlayerAlbumImage.getArtworkQuick(getApplicationContext(), playSong.getAlbumId(), 150, 150);

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
        showLog("updateTitleListView");
        mAudioListAdapter.setmCurrentPosition(mCurrentPosition);
        mAudioListAdapter.notifyDataSetChanged();
    }

    private void restartOrPause() {
        showLog("restartOrPause");
        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Play");
        sendBroadcast(songListActivity);
    }

    private void previous() {
        showLog("previous");
        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Previous");
        sendBroadcast(songListActivity);
    }

    private void next() {
        showLog("next");
        Intent songListActivity = new Intent(HOME + "AudioMessengerService.Next");
        sendBroadcast(songListActivity);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showLog("onReceive : BroadcastReceiver");
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
                mAudioListAdapter.setmCurrentPosition(mCurrentPosition);
                mAudioListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void registerCallReceiver(){
        showLog("registerCallReceiver");
        if(!mIsReceiverRegistered){
            IntentFilter filter = new IntentFilter();
            filter.addAction(HOME + "AudioFileListActivity.STOP");
            filter.addAction(HOME + "AudioPlayerActivity.songChanged");
            registerReceiver(mBroadcastReceiver, filter);
            mIsReceiverRegistered = true;
        }
    }

    private void unregisterCallReceiver(){
        showLog("unregisterCallReceiver");
        if(mIsReceiverRegistered){
            unregisterReceiver(mBroadcastReceiver);
            mIsReceiverRegistered = false;
        }
    }
}
