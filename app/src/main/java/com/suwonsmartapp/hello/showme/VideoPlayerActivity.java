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

import org.mozilla.universalchardet.UniversalDetector;

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
    private static final String ENCODING = "EUC-KR";

    private int mCurrentPosition;                   // current playing pointer
    private ArrayList<VideoFileInfo> mVideoFileInfoList;    // video file media_player_icon_information list
    private VideoFileInfo videoFileInfo;                    // video file info getting by cursor
    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent
    private String fullPathname = "";              // full path + filename

    private String subPathname = "";                // smi file pathname
    private File subFile;                           // smi file
    private boolean useSmi = false;                 // true if we will use smi file
    private boolean useSrt = false;                 // true if we will use srt file
    private boolean useAss = false;                 // true if we will use ass file
    private boolean useSsa = false;                 // true if we will use ssa file

    private BufferedReader in;
    private String s;
    private String text = null;

    private ArrayList<VideoPlayerSubtitle> parsedSub;
    private long timeSUB = -1;
    private boolean subStart = false;
    private int countSub;
    private long maxRunningTime = 0L;

    private long timeSUBstart = -1;
    private long timeSUBend = -1;
    private String t1, t2;
    private long timeStartHour;
    private long timeStartMinute;
    private long timeStartSecond;
    private long timeStartMillisecond;
    private long timeEndHour;
    private long timeEndMinute;
    private long timeEndSecond;
    private long timeEndMillisecond;

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
        if (intent != null) {
            mCurrentPosition = intent.getIntExtra("currentPosition", -1);
            mVideoFileInfoList = intent.getParcelableArrayListExtra("videoInfoList");
        } else {
            showToast("잘못된 파일입니다.");
            finish();
        }

        setupVideoScreen();     // see if subtitle exists ?
        setupSMI();             // prepare SMI if it exists
        setupSRT();             // prepare SRT if it exists
        setupASS();             // prepare ASS/SSA if it exists
        addOneMoreLine();                   // add dummy subtitle for the safety

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

        detectSubtitle();               // see if there exist .smi .srt .ass .ssa file

        mVV_show = (VideoView) findViewById(R.id.vv_show);
        mVV_subtitle = (TextView)findViewById(R.id.vv_subtitle);
        mVV_subtitle.setText("");

        mVV_show.setVideoPath(fullPathname);                        // setting video path
        mVV_show.requestFocus();                                    // set focus
    }

    private void detectSubtitle() {
        subPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".smi";
        subFile = new File(subPathname);
        useSmi = subFile.isFile() && subFile.canRead();

        if (!useSmi) {
            subPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".srt";
            subFile = new File(subPathname);
            useSrt = subFile.isFile() && subFile.canRead();
        }

        if ((!useSmi) && (!useSrt)) {
            subPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".ass";
            subFile = new File(subPathname);
            useAss = subFile.isFile() && subFile.canRead();

            if (!useAss) {
                subPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".ssa";
                subFile = new File(subPathname);
                useSsa = subFile.isFile() && subFile.canRead();
            }
        }

        if (useSmi || useSrt || useAss || useSsa) {
            parsedSub = new ArrayList<>();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mVV_show.seekTo(0);
        mVV_show.start();                   // auto start

        if (useSmi || useSrt || useAss || useSsa) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(300);
                            subHandler.sendMessage(subHandler.obtainMessage());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mCurrentPosition++;                 // next movie
        if (mCurrentPosition == mVideoFileInfoList.size()) {
            mCurrentPosition = 0;               // wrap around
        }

        setupVideoScreen();                 // prepare next movie screen
        setupSMI();
        setupSRT();
        setupASS();
        addOneMoreLine();                   // add dummy subtitle for the safety

        mVV_show.seekTo(0);
        mVV_show.start();                   // auto start

        if (useSmi || useSrt || useAss || useSsa) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(300);
                            subHandler.sendMessage(subHandler.obtainMessage());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
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
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString())));
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("<SYNC")) {
                        subStart = true;
                        if (timeSUB != -1) {
                            parsedSub.add(new VideoPlayerSubtitle(timeSUB, text));
                        }
                        timeSUB = Integer.parseInt(s.substring(s.indexOf("=") + 1, s.indexOf(">")));
                        text = s.substring(s.indexOf(">") + 1, s.length());
                        text = text.substring(text.indexOf(">") + 1, text.length());
                    } else {
                        if (subStart) {
                            text = text + s;
                            subStart = false;
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

        if (parsedSub.size() <= 1) {
            useSmi = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

    // SRT file structure:
    //
    // 123
    // 00:00:00.000 --> 00:00:00.000
    // <i> Message line 1 </i>
    // <i> Message line 2 </i>

    private void setupSRT() {
        if (useSrt) {
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString())));
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("-->")) {
                        subStart = true;
                        if (timeSUBstart != -1) {
                            parsedSub.add(new VideoPlayerSubtitle(timeSUBstart, text));
                        }
                        t1 = s.substring(0, s.lastIndexOf(" --> "));
                        timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')));
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1, t1.lastIndexOf(':')));
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1, t1.indexOf(',')));
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf(',') + 1, t1.length()));
                        timeSUBstart = ((timeStartHour * 60 + timeStartMinute) * 60 + timeStartSecond) * 1000 + timeStartMillisecond;

                        t2 = s.substring(s.lastIndexOf(" --> ") + 5, s.length());
                        timeEndHour = Integer.parseInt(t2.substring(0, t2.indexOf(':')));
                        timeEndMinute = Integer.parseInt(t2.substring(t2.indexOf(':') + 1, t2.lastIndexOf(':')));
                        timeEndSecond = Integer.parseInt(t2.substring(t2.lastIndexOf(':') + 1, t2.indexOf(',')));
                        timeEndMillisecond = Integer.parseInt(t2.substring(t2.lastIndexOf(',') + 1, t2.length()));
                        timeSUBend = ((timeEndHour * 60 + timeEndMinute) * 60 + timeEndSecond) * 1000 + timeEndMillisecond;

//                        showLog(timeStartHour + ":" + timeStartMinute + ":" + timeStartSecond + "," + timeStartMillisecond + " -> " +
//                                timeEndHour + ":" + timeEndMinute + ":" + timeEndSecond + "," + timeEndMillisecond);

                        text = "";      // clear text line for getting new text
                    } else if (subStart) {
                        if (s.contains("<i>")) {
                            text = text + s.substring(s.indexOf("<i>") + 3, s.lastIndexOf("</i>"));
                        } else {
                            text = text + s.substring(0, s.length());
                            subStart = false;
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

        if (parsedSub.size() <= 1) {
            useSrt = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

    // ASS/SSA file structure:
    //
    // Dialog: Layer, Start, End, Style, Name, MarginalL, MarginalR, MarginalV, Effect, Text
    // Dialog: 0,0:00:15.76, ,0:00:16.21, ... ..Text     or
    // Dialog: 0,0:00:15.76, ,0:00:16.21, ... ..{ ... }Text

    private void setupASS() {
        if (useAss || useSsa) {
            if (useAss) {
                try {
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString())));
                } catch (UnsupportedEncodingException | FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            if (useSsa) {
                try {
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString())));
                    useAss = true;      // treat SSA same as ASS
                    useSsa = false;
                } catch (UnsupportedEncodingException | FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("Dialogue:")) {
                        subStart = true;
                        if (timeSUBstart != -1) {
                            parsedSub.add(new VideoPlayerSubtitle(timeSUBstart, text));
                            showLog("time : " + timeSUBstart + ", text : " + text);
                        }
                        t2 = s.substring(s.indexOf(',') + 1, s.length());
                        t1 = t2.substring(0, t2.indexOf(','));
                        timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')));
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1, t1.lastIndexOf(':')));
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1, t1.indexOf('.')));
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf('.') + 1, t1.length())) * 10;
                        timeSUBstart = ((timeStartHour * 60 + timeStartMinute) * 60 + timeStartSecond) * 1000 + timeStartMillisecond;

                        t1 = t2.substring(t2.lastIndexOf(",,") + 2, t2.length());
                        if (t1.equals("")) {
                            text = " ";     // if text is empty, just put one space
                        } else {
                            if (t1.substring(0, 1).equals("{")) {
                                t1 = t2.substring(t2.lastIndexOf("}") + 1, t2.length());    // ignore { ... }
                            }
                            text = t1;      // we've got the text
                        }
                    } else if (subStart) {
                        text = text + s.substring(0, s.length());
                        subStart = false;
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

        if (parsedSub.size() <= 1) {
            useAss = false;     // if we have just one line, ignore this subtitle
            useSsa = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

    Handler subHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mVV_show.getCurrentPosition() <= maxRunningTime) {
                countSub = getSubSyncIndex(mVV_show.getCurrentPosition());
                mVV_subtitle.setText(Html.fromHtml(parsedSub.get(countSub).getText()));
            }
        }
    };

    // if get(index) <= playTime < get(index+1), then return index
    //
    //                      <=    playTime    <
    //    +-----------------+-----------------+--------------------+
    //    0             get(index)      get(index + 1)         get(size)
    //
    public int getSubSyncIndex(long playTime) {
        int lowLimit = 0;
        int highLimit = parsedSub.size();
        int indexPointer;

        while(lowLimit <= highLimit) {
            indexPointer = (lowLimit + highLimit) / 2;
            if((parsedSub.get(indexPointer).getTime() <= playTime) && (playTime < parsedSub.get(indexPointer + 1).getTime())) {
                return indexPointer;
            }
            if(playTime >= parsedSub.get(indexPointer + 1).getTime()) {
                lowLimit = indexPointer + 1;
            } else {
                highLimit = indexPointer - 1;
            }
        }
        return 0;
    }

    private String detectEncoding(String filename) {
        byte[] buf = new byte[4096];
        String fileName = filename;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        UniversalDetector detector = new UniversalDetector(null);

        int nread;
        try {
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        detector.dataEnd();

        String encoding = detector.getDetectedCharset();
        if (encoding == null) {
            encoding = ENCODING;        // set default encoding method
        }

        detector.reset();

        return encoding;
    }

    private void addOneMoreLine() {
        if (useSmi || useSrt || useAss || useSsa) {
            int h = parsedSub.size() - 1;
            maxRunningTime = parsedSub.get(h).getTime();
            parsedSub.add(new VideoPlayerSubtitle(maxRunningTime + 500, "The End"));
        }
    }
}
