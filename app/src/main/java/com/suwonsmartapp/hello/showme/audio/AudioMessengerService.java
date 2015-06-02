package com.suwonsmartapp.hello.showme.audio;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;

import java.io.IOException;
import java.util.ArrayList;

public class AudioMessengerService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private static final String TAG = AudioMessengerService.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }
    private static final String HOME = "com.suwonsmartapp.hello.showme.";

    // Command to the service to display a message
    public static final int MSG_GET_MP = 1;
    public static final int MSG_NEXT_MP = 2;
    public static final int MSG_GET_MP_IN_LIST = 3;
    private static final int MSG_STOP_SERVICE = 4;
    private static final int MSG_SONG_CHANGE = 5;

    private static final int NOTI_MUSIC_SERVICE = 100;

    private MediaPlayer mMediaPlayer;
    private NotificationManagerCompat mNotiManager;

    // Target we publish for clients to send messages to IncomingHandler
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    final Messenger mMPMessenger = new Messenger(new AudioPlayerActivity.MusicHandler());
    final Messenger mMPMessengerToList = new Messenger(new AudioFileListActivity.MusicHandler());

    private ArrayList<AudioFileInfo> mAudioFileInfoList;
    private AudioFileInfo mPlayAudioFileInfo;
    private int mCurrentPosition;

    private boolean mIsCloseReceiverRegistered;
    private RemoteViews mRemoteViews;

    private NotificationCompat.Builder mNotiBuilder;

    public static final int RESULT_OK = 0x0fff;
    public static final int REQUEST_CODE_AUDIO = 0x0001;
    public static final int REQUEST_CODE_AUDIO_PLAYER = 0x0002;
    public static final int REQUEST_CODE_VIDEO = 0x0010;
    public static final int REQUEST_CODE_VIDEO_PLAYER = 0x0020;
    public static final int REQUEST_CODE_IMAGE = 0x0100;
    public static final int REQUEST_CODE_IMAGE_PLAYER = 0x0200;
    private Bundle extraAudioService;
    private Intent intentAudioService;

    // Handler of incoming messages from clients
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_MP:
                    sendMessageToPlayerActivity();
                    break;

                case MSG_NEXT_MP:

                case MSG_GET_MP_IN_LIST:
                    sendMessageArgsToSongListActivity();
                    break;

                case MSG_STOP_SERVICE:
                    stopMusicService();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        showLog("onCreate");

        mNotiManager = NotificationManagerCompat.from(getApplicationContext());

        registerCallReceiverService();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    private boolean doRestart = false;
    private boolean setSongList = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showLog("onStartCommand");

        if (!setSongList) {
            mAudioFileInfoList = intent.getParcelableArrayListExtra("songInfoList");
            setSongList = true;
        }

        int tmpPosition = intent.getIntExtra("currentPosition", -1);

        if (mMediaPlayer.isPlaying() || isPaused) {
            if (mCurrentPosition == tmpPosition) {
                doRestart = true;
                sendMessageToPlayerActivity();
                return super.onStartCommand(intent, flags, startId);
            } else {
                mCurrentPosition = tmpPosition;
            }
        } else {
            mCurrentPosition = intent.getIntExtra("currentPosition", -1);
        }

        doRestart = false;
        mPlayAudioFileInfo = mAudioFileInfoList.get(mCurrentPosition);
        mPlayAudioFileInfo.setAlbumArt(getAlbumArt(mPlayAudioFileInfo.getAlbumId()));

        showMusicNotification();

        playMusic();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        showLog("onPrepared");

        if (!doRestart) {
            mp.start();
            if (mp.isPlaying()) {
                sendMessageToPlayerActivity();
            }
        }
    }

    // When binding to the service,
    // we return an interface to our messenger for sending messages to the service
    @Override
    public IBinder onBind(Intent intent) {
        showLog("onBind");

        if (intent == null) {
            return mMessenger.getBinder();
        }

        return mMessenger.getBinder();
    }

    @Override
    public void onRebind(Intent intent) { // new client 가 접속했을 때 불린다고?
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        setNextMusic();
    }

    private void setPreviousMusic() {
        showLog("setPreviousMusic");

        if (mCurrentPosition > 0 && mCurrentPosition < mAudioFileInfoList.size()) {
            mCurrentPosition  -= 1;
            setMusic();
        } else if (mCurrentPosition == 0) {                     // wrap around
            mCurrentPosition = mAudioFileInfoList.size() - 1;
            setMusic();
        }
    }

    private void setNextMusic() {
        showLog("setNextMusic");

        if (mCurrentPosition >= 0 && mCurrentPosition < mAudioFileInfoList.size()) {
            mCurrentPosition  += 1;
            setMusic();
        } else if (mCurrentPosition >= mAudioFileInfoList.size()) {     // wrap around
            mCurrentPosition = 0;
            setMusic();
        }
    }

    private void setMusic() {
        showLog("setMusic : " + String.valueOf(mCurrentPosition));

        if (mCurrentPosition >= 0 && mCurrentPosition < mAudioFileInfoList.size()){
            mPlayAudioFileInfo = mAudioFileInfoList.get(mCurrentPosition);
            mPlayAudioFileInfo.setAlbumArt(getAlbumArt(mPlayAudioFileInfo.getAlbumId()));
            playMusic();

            sendMessageToPlayerActivity();

            // broadcast that we've changed title
            Intent intentPlay = new Intent(HOME + "AudioPlayerActivity.songChanged");
            intentPlay.putExtra("currentPosition", mCurrentPosition);
            sendBroadcast(intentPlay);
        } else if (mCurrentPosition < 0 || mCurrentPosition >= mAudioFileInfoList.size()){
                mCurrentPosition = 0;       // wrap around
                mPlayAudioFileInfo = mAudioFileInfoList.get(mCurrentPosition);
                mPlayAudioFileInfo.setAlbumArt(getAlbumArt(mPlayAudioFileInfo.getAlbumId()));
                playMusic();

                sendMessageToPlayerActivity();

                // broadcast that we've changed title
                Intent intentPlay = new Intent(HOME + "AudioPlayerActivity.songChanged");
                intentPlay.putExtra("currentPosition", mCurrentPosition);
                sendBroadcast(intentPlay);
        }
    }

    private void playMusic() {
        showLog("playMusic");

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setDataSource(getApplicationContext(), mPlayAudioFileInfo.getSongUri());
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        showLog("onDestroy");

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        mNotiManager.cancel(NOTI_MUSIC_SERVICE);
        unregisterCallReceiverService();

//        extraAudioService = new Bundle();
//        intentAudioService = new Intent();
//        extraAudioService.putInt("CurrentPosition", 0);
//        intentAudioService.putExtras(extraAudioService);
//        this.setResult(RESULT_OK, intentAudioService);

        super.onDestroy();
    }

    private void sendMessageToPlayerActivity() {
        showLog("sendMessageToPlayerActivity");

        Message msg = Message.obtain(null, AudioMessengerService.MSG_GET_MP, 0, 0);
        msg.obj = mMediaPlayer;
        try {
            mMPMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageArgsToSongListActivity() {
        showLog("sendMessageArgsToSongListActivity");

        Message msg = Message.obtain(null, AudioMessengerService.MSG_GET_MP_IN_LIST, 0, 0);
        msg.obj = mMediaPlayer;
        msg.arg1 = mCurrentPosition;

        try {
            mMPMessengerToList.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageStopService() {
        showLog("sendMessageStopService");

        Message msg = Message.obtain(null, AudioMessengerService.MSG_STOP_SERVICE, 0, 0);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public static boolean isPaused = false;

    public static void pause(MediaPlayer mp) {
        mp.pause();
        isPaused = true;
    }

    public static void restart(MediaPlayer mp) {
        mp.start();
        isPaused = false;
    }

    private static int sTimer = 1000 * 60 * 5;      // 5 minutes
    private static int sSec = 0;

    // service will be terminated after certain times (now 5 minutes)
    private static Thread mPausedThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                if (sSec != sTimer) {
                    sSec++;
                }
                mPausedThread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    private void showMusicNotification() {
        showLog("showMusicNotification");

        Intent intent = new Intent(getApplicationContext(), AudioFileListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), NOTI_MUSIC_SERVICE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotiBuilder = new NotificationCompat.Builder(getApplicationContext());
        mNotiBuilder.setSmallIcon(R.drawable.audio_play_button);

        mNotiBuilder.setContentIntent(pendingIntent);
        mNotiBuilder.setOngoing(true);

        mRemoteViews = new RemoteViews(this.getPackageName(), R.layout.audio_service_player);

        setNotificationUI();

        Intent intentClose = new Intent(HOME + "AudioMessengerService.Close");
        PendingIntent pendingIntentClose = PendingIntent.getBroadcast(this, NOTI_MUSIC_SERVICE, intentClose, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent intentPlay = new Intent(HOME + "AudioMessengerService.Play");
        PendingIntent pendingIntentPlay = PendingIntent.getBroadcast(this, NOTI_MUSIC_SERVICE, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent intentPrevious = new Intent(HOME + "AudioMessengerService.Previous");
        PendingIntent pendingIntentPrevious = PendingIntent.getBroadcast(this, NOTI_MUSIC_SERVICE, intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent intentNext = new Intent(HOME + "AudioMessengerService.Next");
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, NOTI_MUSIC_SERVICE, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        mRemoteViews.setOnClickPendingIntent(R.id.ib_messenger_stop, pendingIntentClose);
        mRemoteViews.setOnClickPendingIntent(R.id.ib_messenger_play, pendingIntentPlay);
        mRemoteViews.setOnClickPendingIntent(R.id.ib_messenger_previous, pendingIntentPrevious);
        mRemoteViews.setOnClickPendingIntent(R.id.ib_messenger_next, pendingIntentNext);

        mNotiBuilder.setContent(mRemoteViews);
        mNotiManager.notify(NOTI_MUSIC_SERVICE, mNotiBuilder.build());
    }

    private void setNotificationUI() {
        showLog("setNotificationUI");

        if (mPlayAudioFileInfo.getAlbumArt() != null) {
            if (mPlayAudioFileInfo.getTitle().toLowerCase().lastIndexOf(".mp3") == -1) {
                mRemoteViews.setImageViewResource(R.id.messenger_album_picture, R.drawable.audio_music_small);
            } else {
                mRemoteViews.setBitmap(R.id.messenger_album_picture, "setImageBitmap", mPlayAudioFileInfo.getAlbumArt());
            }
        } else {
            mRemoteViews.setImageViewResource(R.id.messenger_album_picture, R.drawable.audio_music_small);
        }

        mRemoteViews.setTextViewText(R.id.messenger_title, mPlayAudioFileInfo.getTitle() + " - " + mPlayAudioFileInfo.getArtist());
    }

    private BroadcastReceiver mButtonBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if ((HOME + "AudioMessengerService.Close").equals(action)) {
                stopMusicService();
            } else {
                if ((HOME + "AudioMessengerService.Play").equals(action)) {
                    if (isPaused) {
                        restart(mMediaPlayer);
                        mNotiBuilder.setSmallIcon(R.drawable.audio_play_button);
                        mRemoteViews.setImageViewResource(R.id.ib_messenger_play, android.R.drawable.ic_media_pause);
                    } else {
                        pause(mMediaPlayer);
                        mNotiBuilder.setSmallIcon(R.drawable.audio_pause_button);
                        mRemoteViews.setImageViewResource(R.id.ib_messenger_play, android.R.drawable.ic_media_play);
                    }
                } else if ((HOME + "AudioMessengerService.Previous").equals(action)) {
                    setPreviousMusic();
                } else if ((HOME + "AudioMessengerService.Next").equals(action)) {
                    setNextMusic();
                }
                setNotificationUI();
                mNotiBuilder.setContent(mRemoteViews);
                mNotiManager.notify(NOTI_MUSIC_SERVICE, mNotiBuilder.build());
            }
        }
    };

    private void registerCallReceiverService(){
        showLog("registerCallReceiverService");

        if(!mIsCloseReceiverRegistered){
            IntentFilter filter = new IntentFilter();
            filter.addAction(HOME + "AudioMessengerService.Close");
            filter.addAction(HOME + "AudioMessengerService.Play");
            filter.addAction(HOME + "AudioMessengerService.Previous");
            filter.addAction(HOME + "AudioMessengerService.Next");
            registerReceiver(mButtonBroadcastReceiver, filter);

            mIsCloseReceiverRegistered = true;
        }
    }

    private void unregisterCallReceiverService(){
        showLog("unregisterCallReceiverService");

        if(mIsCloseReceiverRegistered){
            unregisterReceiver(mButtonBroadcastReceiver);
            mIsCloseReceiverRegistered = false;
        }
    }

    private Bitmap getAlbumArt(int albumId) {
        showLog("getAlbumArt");

        return AudioPlayerAlbumImage.getArtworkQuick(getApplicationContext(), albumId, 300, 300);
    }

    private void stopMusicService() {
        showLog("stopMusicService");

        Intent playerActivity = new Intent(HOME + "AudioPlayerActivity.STOP");
        sendBroadcast(playerActivity);
        Intent songListActivity = new Intent(HOME + "AudioFileListActivity.STOP");
        sendBroadcast(songListActivity);

        boolean result = stopService(new Intent(getApplicationContext(), AudioMessengerService.class));
    }
}
