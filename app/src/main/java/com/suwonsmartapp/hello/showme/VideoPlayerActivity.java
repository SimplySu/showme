package com.suwonsmartapp.hello.showme;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.suwonsmartapp.hello.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

    private String smiPathname = "";                // smi file pathname
    private File smiFile;                           // smi file
    private boolean useSmi;                         // true if we will use smi file

    private String srtPathname = "";                // srt file pathname
    private File srtFile;                           // srt file
    private boolean useSrt;                         // true if we will use srt file

    private BufferedReader in;
    private String s;
    private String text = null;

    private ArrayList<VideoPlayerSUB> parsedSmi;
    private String t1, t2;
    private long timeSMI = -1;
    private boolean smiStart = false;
    private int countSmi;

    private ArrayList<VideoPlayerSUB> parsedSrt;
    private long timeSRTstart = -1;
    private long timeSRTend = -1;
    private long timeStartHour;
    private long timeStartMinute;
    private long timeStartSecond;
    private long timeStartMillisecond;
    private long timeEndHour;
    private long timeEndMinute;
    private long timeEndSecond;
    private long timeEndMillisecond;
    private boolean srtStart = false;
    private int countSrt;

    private VideoView mVV_show;                     // video screen
    private TextView mVV_subtitle;                  // subtitle

    private int volume_Max = 0;
    private int volume_Current = 0;
    private AudioManager audioManager;

    public static final int RESULT_OK = 0x0fff;
    public static final int REQUEST_CODE_AUDIO = 0x0001;
    public static final int REQUEST_CODE_AUDIO_PLAYER = 0x0002;
    public static final int REQUEST_CODE_VIDEO = 0x0010;
    public static final int REQUEST_CODE_VIDEO_PLAYER = 0x0020;
    public static final int REQUEST_CODE_IMAGE = 0x0100;
    public static final int REQUEST_CODE_IMAGE_PLAYER = 0x0200;
    private Bundle extraVideoPlayerService;
    private Intent intentVideoPlayerService;

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

        setupVideoScreen();
        setupSMI();             // prepare SMI if it exists
        setupSRT();             // prepare SRT if it exists

        MediaController mController = new MediaController(this);
        mController.setAnchorView(mVV_show);
        mVV_show.setMediaController(mController);
        mVV_show.setOnPreparedListener(this);                       // ready listener
        mVV_show.setOnCompletionListener(this);                     // complete listener for next

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        volume_Max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume_Current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        mSB_volume.setMax(volume_Max);
//        mSB_volume.setProgress(volume_Current);
//        mSB_volume.setOnSeekBarChangeListener(this);
    }

    private void setupVideoScreen() {
        videoFileInfo = mVideoFileInfoList.get(mCurrentPosition);
        fullPathname = videoFileInfo.getMediaData();
        int i = fullPathname.lastIndexOf('/');
        int j = fullPathname.length();
        requestedPathname = fullPathname.substring(0, i);          // get requested pathname
        requestedFilename = fullPathname.substring(i + 1, j);      // and filename

        smiPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".smi";
        smiFile = new File(smiPathname);
        useSmi = smiFile.isFile() && smiFile.canRead();

        if (!useSmi) {
            srtPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".srt";
            srtFile = new File(srtPathname);
            useSrt = srtFile.isFile() && srtFile.canRead();
        }

        mVV_show = (VideoView) findViewById(R.id.vv_show);
        mVV_subtitle = (TextView)findViewById(R.id.vv_subtitle);
        mVV_subtitle.setText("");

        mVV_show.setVideoPath(fullPathname);                        // setting video path
        mVV_show.requestFocus();                                    // set focus
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mVV_show.seekTo(0);
        mVV_show.start();                   // auto start

        if (useSmi) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(300);
                            smiHandler.sendMessage(smiHandler.obtainMessage());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
        }

        if (useSrt) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(300);
                            srtHandler.sendMessage(srtHandler.obtainMessage());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (mCurrentPosition >= mVideoFileInfoList.size()) {
            finish();           // playing completed
        } else {
            mCurrentPosition++;                 // next movie
            setupVideoScreen();                 // prepare next movie screen
            setupSMI();
            setupSRT();

            mVV_show.seekTo(0);
            mVV_show.start();                   // auto start

            if (useSmi) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            while (true) {
                                Thread.sleep(300);
                                smiHandler.sendMessage(smiHandler.obtainMessage());
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }).start();
            }

            if (useSrt) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            while (true) {
                                Thread.sleep(300);
                                srtHandler.sendMessage(srtHandler.obtainMessage());
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }).start();
            }
        }
    }

    @Override
    protected void onDestroy() {

        extraVideoPlayerService = new Bundle();
        intentVideoPlayerService = new Intent();
        extraVideoPlayerService.putInt("CurrentPosition", mCurrentPosition);
        intentVideoPlayerService.putExtras(extraVideoPlayerService);
        this.setResult(RESULT_OK, intentVideoPlayerService);

        super.onDestroy();
    }

    // SMI file structure:
    //
    // <SYNC Start=370000>
    // Message line 1
    // Message line 2

    private void setupSMI() {
        if (useSmi) {
            parsedSmi = new ArrayList<>();

            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(smiFile.toString())), "MS949"));
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("<SYNC")) {
                        smiStart = true;
                        if (timeSMI != -1) {
                            parsedSmi.add(new VideoPlayerSUB(timeSMI, text));
                        }
                        timeSMI = Integer.parseInt(s.substring(s.indexOf("=") + 1, s.indexOf(">")));
                        text = s.substring(s.indexOf(">") + 1, s.length());
                        text = text.substring(text.indexOf(">") + 1, text.length());
                    } else {
                        if (smiStart) {
                            text += s;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Handler smiHandler = new Handler() {
        public void handleMessage(Message msg) {
            countSmi = getSmiSyncIndex(mVV_show.getCurrentPosition());
            mVV_subtitle.setText(Html.fromHtml(parsedSmi.get(countSmi).getText()));
        }
    };

    public int getSmiSyncIndex(long playTime) {
        int l = 0, m, h = parsedSmi.size();

        while(l <= h) {
            m = (l + h) / 2;
            if(parsedSmi.get(m).getTime() <= playTime && playTime < parsedSmi.get(m + 1).getTime()) {
                return m;
            }
            if(playTime > parsedSmi.get(m + 1).getTime()) {
                l = m + 1;
            } else {
                h = m - 1;
            }
        }
        return 0;
    }

    // SRT file structure:
    //
    // 123
    // 00:00:00.000 --> 00:00:00.000
    // <i> Message line 1 </i>
    // <i> Message line 2 </i>

    private void setupSRT() {
        if (useSrt) {
            parsedSrt = new ArrayList<>();

            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srtFile.toString())), "MS949"));
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("-->")) {
                        srtStart = true;
                        if (timeSRTstart != -1) {
                            parsedSrt.add(new VideoPlayerSUB(timeSRTstart, text));
                        }
                        t1 = s.substring(0, s.lastIndexOf(" --> "));
                        timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')));
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1, t1.lastIndexOf(':')));
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1, t1.indexOf(',')));
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf(',') + 1, t1.length()));
                        timeSRTstart = ((timeStartHour * 60 + timeStartMinute) * 60 + timeStartSecond) * 1000 + timeStartMillisecond;

                        t2 = s.substring(s.lastIndexOf(" --> ") + 5, s.length());
                        timeEndHour = Integer.parseInt(t2.substring(0, t2.indexOf(':')));
                        timeEndMinute = Integer.parseInt(t2.substring(t2.indexOf(':') + 1, t2.lastIndexOf(':')));
                        timeEndSecond = Integer.parseInt(t2.substring(t2.lastIndexOf(':') + 1, t2.indexOf(',')));
                        timeEndMillisecond = Integer.parseInt(t2.substring(t2.lastIndexOf(',') + 1, t2.length()));
                        timeSRTend = ((timeEndHour * 60 + timeEndMinute) * 60 + timeEndSecond) * 1000 + timeEndMillisecond;

                        showLog(timeStartHour + ":" + timeStartMinute + ":" + timeStartSecond + "," + timeStartMillisecond + " -> " +
                                timeEndHour + ":" + timeEndMinute + ":" + timeEndSecond + "," + timeEndMillisecond);

                        text = "";      // clear text line for getting new text
                    } else if (srtStart) {
                        if (s.contains("<i>")) {
                            text = text + s.substring(s.indexOf("<i>") + 3, s.lastIndexOf("</i>"));
                        } else {
                            text = text + s.substring(0, s.length());
                            srtStart = false;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Handler srtHandler = new Handler() {
        public void handleMessage(Message msg) {
            countSrt = getSrtSyncIndex(mVV_show.getCurrentPosition());
            mVV_subtitle.setText(Html.fromHtml(parsedSrt.get(countSrt).getText()));
        }
    };

    public int getSrtSyncIndex(long playTime) {
        int l = 0, m, h = parsedSrt.size();

        while(l <= h) {
            m = (l + h) / 2;
            if(parsedSrt.get(m).getTime() <= playTime && playTime < parsedSrt.get(m + 1).getTime()) {
                return m;
            }
            if(playTime > parsedSrt.get(m + 1).getTime()) {
                l = m + 1;
            } else {
                h = m - 1;
            }
        }
        return 0;
    }
}
