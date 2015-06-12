package com.suwonsmartapp.hello.showme.video;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.suwonsmartapp.hello.R;
import com.suwonsmartapp.hello.showme.detect.character.CodeDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class VideoPlayerActivity extends Activity implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private final boolean nowDEBUG = true;
    private final String TAG = VideoPlayerActivity.class.getSimpleName();
    private void showLog(String msg) { if (nowDEBUG) { Log.d(TAG, msg);}}
    private void showToast(String toast_msg) { if (nowDEBUG) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }}

    private final String ENCODING = "EUC-KR";       // default encoding method
    private final int BUF_LENGTH = 256 * 8 * 4;     // enough size
    private final int sleepTime = 300;              // 1000 means 1 second

    private int mCurrentPosition;                   // current playing pointer
    private ArrayList<VideoFileInfo> mVideoFileInfoList;    // video file media_player_icon_information list

    private VideoView mVV_show;                     // video screen
    private TextView mTV_subtitle;                  // text view subtitle
    private ImageView mIV_subtitle;                 // image view subtitle

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
        setupASS();             // prepare ASS/SSA if it exists
        setupSUB();             // prepare SUB if it exists
        setupSRT();             // prepare SRT if it exists
        setupSMI();             // prepare SMI if it exists
        addOneMoreLine();                   // add dummy subtitle for the safety

        MediaController mController = new MediaController(this);
        mController.setAnchorView(mVV_show);
        mVV_show.setMediaController(mController);
        mVV_show.setOnPreparedListener(this);                       // ready listener
        mVV_show.setOnCompletionListener(this);                     // complete listener for next

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_Max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume_Current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        mSB_volume.setMax(volume_Max);
//        mSB_volume.setProgress(volume_Current);
//        mSB_volume.setOnSeekBarChangeListener(this);
    }

    private String fullPathname = "";              // full path + filename

    private void setupVideoScreen() {
        VideoFileInfo videoFileInfo = mVideoFileInfoList.get(mCurrentPosition);
        fullPathname = videoFileInfo.getMediaData();
        String requestedPathname = fullPathname.substring(0, fullPathname.lastIndexOf('/'));
        String requestedFilename = fullPathname.substring(fullPathname.lastIndexOf('/') + 1, fullPathname.length());

        mVV_show = (VideoView) findViewById(R.id.vv_show);
        mTV_subtitle = (TextView)findViewById(R.id.vv_subtitle);
        mIV_subtitle = (ImageView)findViewById(R.id.iv_subtitle);
        mTV_subtitle.setText("");

        detectSubtitle();               // see if there exist .smi .srt .ass .ssa file

        mVV_show.setVideoPath(fullPathname);                        // setting video path
        mVV_show.requestFocus();                                    // set focus
    }

    private ArrayList<VideoPlayerTextSubtitle> parsedTextSubtitle;
    private ArrayList<VideoPlayerGraphicSubtitle> parsedGraphicSubtitle;

    private File subFile;                           // sub file

    private String subFilename = "";                // sub filename string
    private File idxFile;                           // idx file

    private byte [] subtitleFile;                     // buffer for reading all of .sub file

    private boolean useSmi = false;                 // true if we will use smi file
    private boolean useSrt = false;                 // true if we will use srt file
    private boolean useAss = false;                 // true if we will use ass file
    private boolean useSsa = false;                 // true if we will use ssa file
    private boolean useSub = false;                 // true if we will use idx/sub file

    // see if .smi, .srt, .ass, .ssa, .idx, and .sub file exists.
    private void detectSubtitle() {
        String subPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".smi";
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

        if ((!useSmi) && (!useSrt) && (!useAss)) {
            String idxPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".idx";
            idxFile = new File(idxPathname);
            useSub = idxFile.isFile() && idxFile.canRead();

            if (useSub) {
                subPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".sub";
                subFilename = fullPathname.substring(fullPathname.lastIndexOf("/") + 1, fullPathname.lastIndexOf(".")) + ".sub";
                        subFile = new File(subPathname);
                useSub = subFile.isFile() && subFile.canRead();
            } else {
                useSub = false;     // because we have .idx only without .sub
            }
        }

        if (useSmi || useSrt || useAss || useSsa) {
            mIV_subtitle.setVisibility(View.GONE);
            mTV_subtitle.setVisibility(View.VISIBLE);
            parsedTextSubtitle = new ArrayList<>();
        }

        if (useSub) {
            mIV_subtitle.setVisibility(View.VISIBLE);
            mTV_subtitle.setVisibility(View.GONE);
            parsedGraphicSubtitle = new ArrayList<>();

            subtitleFile = new byte[(int) subFile.length()];

            FileInputStream f = null;
            try { f = new FileInputStream(subFile); }
            catch (FileNotFoundException e) { e.printStackTrace(); }

            FileChannel ch = null;
            if (f != null) { ch = f.getChannel(); }
            MappedByteBuffer mb = null;
            try { if (ch != null) { mb = ch.map(FileChannel.MapMode.READ_ONLY, 0L, ch.size()); } }
            catch (IOException e) { e.printStackTrace(); }

            if (mb != null) {
                int fPtr = 0;
                while(mb.remaining() > 0) {
                    mb.get(subtitleFile, fPtr, Math.min(mb.remaining(), 0x0800));
    //                showLog("current position : " + fPtr + "data : " + subtitleFile[fPtr + 0x16] + ", " + subtitleFile[fPtr + 0x1d] + ", " + subtitleFile[fPtr + 0x1e]);
                    fPtr = fPtr + 0x0800;
                }
            }
        }
    }

    private int countSub;

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

//        if (useSub) {
//            int[] timearray = new int[]{66633, 71304, 73139, 77177, 77944, 79679, 81414, 83016, 84517, 86219, 87153, 88521, 89789, 90623};
//            // create bitmap testing code
//            for (int i : timearray) {
//                countSub = getSubSyncIndexGraphic(i);
//                int pos = parsedGraphicSubtitle.get(countSub).getFilepos();
//                mIV_subtitle.setImageBitmap(getBitmapSubtitle(pos));
//            }
//        }

        mVV_show.seekTo(0);
        mVV_show.start();                   // auto start

        if (useSmi || useSrt || useAss || useSsa) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(300);
                            textHandler.sendMessage(textHandler.obtainMessage());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
        }

        if (useSub) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(sleepTime);
                            idxHandler.sendMessage(idxHandler.obtainMessage());
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
        setupASS();                         // prepare ASS/SSA if it exists
        setupSUB();                         // prepare SUB if it exists
        setupSRT();                         // prepare SRT if it exists
        setupSMI();                         // prepare SMI if it exists
        addOneMoreLine();                   // add dummy subtitle for the safety

        mVV_show.seekTo(0);
        mVV_show.start();                   // auto start

        if (useSmi || useSrt || useAss || useSsa) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(300);
                            textHandler.sendMessage(textHandler.obtainMessage());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
        }

        if (useSub) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(sleepTime);
                            idxHandler.sendMessage(idxHandler.obtainMessage());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onDestroy() {

        Bundle extraVideoPlayerService = new Bundle();
        Intent intentVideoPlayerService = new Intent();
        extraVideoPlayerService.putInt("CurrentPosition", mCurrentPosition);
        intentVideoPlayerService.putExtras(extraVideoPlayerService);
        this.setResult(RESULT_OK, intentVideoPlayerService);

        super.onDestroy();
    }

/*
    .smi file example

    <SAMI>
    <HEAD>
    <Title>Delta.of.Venus.1995.x264.DTS.CD1-MoMo</Title>
    <Style TYPE="text/css">
    <!--
    P {margin-left:2pt; margin-right:2pt; margin-bottom:1pt; margin-top:1pt;
    text-align:center; font-size:22pt; font-family: Arial, Sans-serif;
    font-weight:bold; color:white;}
    .KRCC {Name:Korean; lang:ko-KR; SAMIType:CC;}
    #STDPrn {Name:Standard Print;}
    #VLargePrn {Name:32pt (VLarge Print); font-size:32pt;}
    #LargePrn {Name:28pt (Large Print); font-size:28pt;}
    #MediumPrn {Name:24pt (Medium Print); font-size:24pt;}
    #BSmallPrn {Name:18pt (BSmall Print); font-size:18pt;}
    #SmallPrn {Name:12pt (Small Print); font-size:12pt;}
    -->
    </Style>
    </HEAD>
    <BODY>
    <!--
    서브변환 : 조이데이(goowoo5@korea.com)
    Special Thanks 메가무비동호회
    -->
    <SYNC Start=36536><P Class=KRCC>
    <span style=color:black;filter:glow(color=dodgerblue);height:1>
    DELTA OF VENUS</span><br><font color=#46b8ff>
    델타비너스
    <SYNC Start=41496><P Class=KRCC>&nbsp;
    <SYNC Start=42675><P Class=KRCC><font color=skyblue>
    파리, 1940년 1월 6일
    <SYNC Start=48204><P Class=KRCC>&nbsp;
    <SYNC Start=48281><P Class=KRCC>
    또, 그녀는 밤새 글을 썼다
    ..........................................................
    ..........................................................
    ..........................................................
    <SYNC Start=3137960><P Class=KRCC>&nbsp;
    <SYNC Start=3139803><P Class=KRCC>
    원하는 게 나이겠지
    <SYNC Start=3143295><P Class=KRCC>&nbsp;
    </BODY>
    </SAMI>
*/

    // SMI file structure:
    //
    // <SYNC Start=370000>
    // Message line 1
    // Message line 2

    private BufferedReader in;
    private String s;
    private String text = null;
    private long timeSUB = -1;
    private boolean subStart = false;

    private void setupSMI() {
        if (useSmi) {
            try { in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString()))); }
            catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("<SYNC")) {
                        subStart = true;
                        if (timeSUB != -1) {
                            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(timeSUB, text));
                        }
                        timeSUB = Integer.parseInt(s.substring(s.indexOf("=") + 1, s.indexOf(">")).trim());
                        text = s.substring(s.indexOf(">") + 1, s.length());     // get rid of <SYNC...
                        text = text.substring(text.indexOf(">") + 1, text.length());

                    } else {
                        if (subStart) {
                            if (s.toLowerCase().contains("<br>")) {
                                if (text.equals("")) {
                                    text = text + s.substring(0, s.indexOf("<"));
                                    if (s.substring(s.indexOf(">"), s.length()).length() > 1) {
                                        text = text + "<br />" + s.substring(s.lastIndexOf(">") + 1, s.length());
                                    }
                                } else {
                                    text = text + "<br />" + s.substring(0, s.indexOf("<"));
                                    if (s.substring(s.indexOf(">"), s.length()).length() > 1) {
                                        text = text + "<br />" + s.substring(s.lastIndexOf(">") + 1, s.length());
                                    }
                                }
                            } else {
                                if (text.equals("")) {
                                    text = text + s;
                                    subStart = false;       // we allow just maximum two lines
                                } else {
                                    text = text + "<br />" + s;
                                    subStart = false;       // we allow just maximum two lines
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }

            try { in.close(); }
            catch (IOException e) { e.printStackTrace(); }

        if (parsedTextSubtitle.size() <= 1) {
            useSmi = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

/*
    .srt file example

    1
    00:00:00,025 --> 00:00:03,070
    >> Welcome to Android Fundamentals.

    2
    00:00:03,070 --> 00:00:05,720
    Getting started on Android is straightforward.

    3
    00:00:05,720 --> 00:00:09,210
    Simply installing Android Studio and creating a new project is enough to

    ..........................................................
    ..........................................................
    ..........................................................

    23
    00:01:01,300 --> 00:01:03,270
    Katherine, why don't you get us started.

    24
    00:01:03,270 --> 00:01:03,770
    >> Sure.
*/

    // SRT file structure:
    //
    // 123
    // 00:00:00.000 --> 00:00:00.000
    // <i> Message line 1 </i>
    // <i> Message line 2 </i>

    private String t1, t2;

    private long timeSUBstart = -1;
    private long timeStartHour;
    private long timeStartMinute;
    private long timeStartSecond;
    private long timeStartMillisecond;

    private void setupSRT() {
        if (useSrt) {
            try { in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString()))); }
            catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("-->")) {
                        subStart = true;
                        if (timeSUBstart != -1) {
                            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(timeSUBstart, text));
                        }
                        t1 = s.substring(0, s.lastIndexOf(" --> "));
                        timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1, t1.lastIndexOf(':')).trim());
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1, t1.indexOf(',')).trim());
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf(',') + 1, t1.length()).trim());
                        timeSUBstart = ((timeStartHour * 60 + timeStartMinute) * 60 + timeStartSecond) * 1000 + timeStartMillisecond;

                        t2 = s.substring(s.lastIndexOf(" --> ") + 5, s.length());
                        long timeEndHour = Integer.parseInt(t2.substring(0, t2.indexOf(':')).trim());
                        long timeEndMinute = Integer.parseInt(t2.substring(t2.indexOf(':') + 1, t2.lastIndexOf(':')).trim());
                        long timeEndSecond = Integer.parseInt(t2.substring(t2.lastIndexOf(':') + 1, t2.indexOf(',')).trim());
                        long timeEndMillisecond = Integer.parseInt(t2.substring(t2.lastIndexOf(',') + 1, t2.length()).trim());
                        long timeSUBend = ((timeEndHour * 60 + timeEndMinute) * 60 + timeEndSecond) * 1000 + timeEndMillisecond;

//                        showLog(timeStartHour + ":" + timeStartMinute + ":" + timeStartSecond + "," + timeStartMillisecond + " -> " +
//                                timeEndHour + ":" + timeEndMinute + ":" + timeEndSecond + "," + timeEndMillisecond);

                        text = "";      // clear text line for getting new text
                    } else if (subStart) {
                        if (s.equals("")) {
                            subStart = false;
                        } else {
                            if (s.contains("<i>")) {
                                text = text + s.substring(s.indexOf("<i>") + 3, s.lastIndexOf("</i>"));
                            } else {
                                if (text.equals("")) {
                                    text = text + s;
                                } else {
                                    text = text + "<br />" + s;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }

            try { in.close(); }
            catch (IOException e) { e.printStackTrace(); }

        if (parsedTextSubtitle.size() <= 1) {
            useSrt = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

/*
    .ass file example

    [Script Info]
    ; Script generated by Aegisub 2.1.8
    ; http://www.aegisub.org/
    Title: Default Aegisub file
    ScriptType: v4.00+
    WrapStyle: 0
    PlayResX: 640
    PlayResY: 480
    ScaledBorderAndShadow: yes
    Video Aspect Ratio: 0
    Video Zoom: 6
    Video Position: 0
    Last Style Storage: Default

    [V4+ Styles]
    Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
    Style: Default,Arial,25,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,1,2,10,10,10,1

    [Events]
    Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
    Comment: 0,0:00:00.00,0:00:00.00,Default,,0000,0000,0000,,This line has some Arabic text to check for basic support.
    Dialogue: 0,0:00:00.00,0:00:02.00,Default,,0000,0000,0000,,هل تعمل اللغة العربية؟
    Comment: 0,0:00:02.00,0:00:02.00,Default,,0000,0000,0000,,These are typeset words with diacritics. They also demonstrate some "ligatures".
    Dialogue: 0,0:00:02.00,0:00:04.00,Default,,0000,0000,0000,,{\pos(323,84)\frz345.949}نَصٌّ
    Dialogue: 0,0:00:02.00,0:00:04.00,Default,,0000,0000,0000,,{\frx28\fry322\fscx251.25\fscy308.75\pos(318,177)}تجريبيّ
    Dialogue: 0,0:00:02.00,0:00:04.00,Default,,0000,0000,0000,,{\frz25.57\fscx368.75\fscy352.5\pos(346,305)}عَرَبيّ
    Dialogue: 0,0:00:02.00,0:00:04.00,Default,,0000,0000,0000,,{\pos(319,430)\fscx260\fscy258.75\clip(305,327,353,460)}مقطوع
    Comment: 0,0:00:04.00,0:00:04.00,Default,,0000,0000,0000,,This line test a common bug. Using tags on a word in an RTL line usually messes up its order.
    Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0000,0000,0000,,{\frz328.582\fscx357.5\fscy445\frx58\fry6\pos(292,250)}{\1c&H0000FF&}هذا {\1c&H00FF00&}نص {\1c&HFF0000&}ملون
    Comment: 0,0:00:06.00,0:00:06.00,Default,,0000,0000,0000,,Numbers in a RTL line. They should be displayed as 1432 and 2011.
    Dialogue: 0,0:00:06.00,0:00:08.00,Default,,0000,0000,0000,,كُتب هذا الملف عام 1432 هـ الموافق 2011 م
    Comment: 0,0:00:08.00,0:00:08.00,Default,,0000,0000,0000,,Another common problem: punctuations at the beginning or the end of a line are sometimes rendered at the wrong end of the line.
    Dialogue: 0,0:00:08.00,0:00:10.00,Default,,0000,0000,0000,,- علامات الترقيم تعمل!
    Comment: 0,0:00:10.00,0:00:10.00,Default,,0000,0000,0000,,A LTR word in a punctuated RTL context and vice-versa.
    Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0000,0000,0000,,- اللغة الرسمية في Germany هي الألمانية.\N- The official language in مصر is Arabic.
    Comment: 0,0:00:12.00,0:00:12.00,Default,,0000,0000,0000,,Bidi karaoke has never worked before. Say hello to kickass-libass!!
    Dialogue: 0,0:00:12.00,0:00:18.00,Default,,0000,0000,0000,,{\k59}كا{\k58}ر{\k54}يو{\k59}كي {\k63}La{\k55}tin {\k52}و{\k58}ع{\k54}ر{\k53}بي!
*/

    // ASS/SSA file structure:
    //
    // Dialog: Layer, Start, End, Style, Name, MarginalL, MarginalR, MarginalV, Effect, Text
    // Dialog: 0,0:00:15.76, ,0:00:16.21, ... ..Text     or
    // Dialog: 0,0:00:15.76, ,0:00:16.21, ... ..{ ... }Text

    private void setupASS() {
        if (useAss || useSsa) {
            if (useAss) {
                try { in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString()))); }
                catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }
            }

            if (useSsa) {
                try { in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subFile.toString())), detectEncoding(subFile.toString()))); }
                catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }

                useAss = true;      // treat SSA same as ASS
                useSsa = false;
            }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("Dialogue:")) {
                        subStart = true;
                        if (timeSUBstart != -1) {
                            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(timeSUBstart, text));
                            showLog("time : " + timeSUBstart + ", text : " + text);
                        }
                        t2 = s.substring(s.indexOf(',') + 1, s.length());
                        t1 = t2.substring(0, t2.indexOf(','));
                        timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1, t1.lastIndexOf(':')).trim());
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1, t1.indexOf('.')).trim());
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf('.') + 1, t1.length()).trim()) * 10;
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
            } catch (IOException e) { e.printStackTrace(); }

            try { in.close(); }
            catch (IOException e) { e.printStackTrace(); }

        if (parsedTextSubtitle.size() <= 1) {
            useAss = false;     // if we have just one line, ignore this subtitle
            useSsa = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

/*
    .idx file example

    # VobSub index file, v7 (do not modify this line!)
    #
    # To repair desyncronization, you can insert gaps this way:
    # (it usually happens after vob id changes)
    #
    #	 delay: [sign]hh:mm:ss:ms
    #
    # Where:
    #	 [sign]: +, - (optional)
    #	 hh: hours (0 <= hh)
    #	 mm/ss: minutes/seconds (0 <= mm/ss <= 59)
    #	 ms: milliseconds (0 <= ms <= 999)
    #
    #	 Note: You can't position a sub before the previous with a negative value.
    #
    # You can also modify timestamps or delete a few subs you don't like.
    # Just make sure they stay in increasing order.


    # Settings

    # Original frame size
    size: 720x480

    # Origin, relative to the upper-left corner, can be overloaded by aligment
    org: 0, 0

    # Image scaling (hor,ver), origin is at the upper-left corner or at the alignment coord (x, y)
    scale: 100%, 100%

    # Alpha blending
    alpha: 100%

    # Smoothing for very blocky images (use OLD for no filtering)
    smooth: OFF

    # In millisecs
    fadein/out: 50, 50

    # Force subtitle placement relative to (org.x, org.y)
    align: OFF at LEFT TOP

    # For correcting non-progressive desync. (in millisecs or hh:mm:ss:ms)
    # Note: Not effective in DirectVobSub, use "delay: ... " instead.
    time offset: 0

    # ON: displays only forced subtitles, OFF: shows everything
    forced subs: OFF

    # The original palette of the DVD
    palette: e83f07, e19120, f3c71b, f8ff18, 9bd22a, 54a530, 12eb12, 15bef6, 0300e3, 4c0353, c12262, ffffff, b3b3b3, 808080, 4e4e4e, 000000

    # Custom colors (transp idxs and the four colors)
    custom colors: OFF, tridx: 1000, colors: ffffff, ffffff, 808080, 000000

    # Language index in use
    langidx: 0

    # Chinese
    id: zh, index: 0
    # Decomment next line to activate alternative name in DirectVobSub / Windows Media Player 6.x
    # alt: Chinese
    # Vob/Cell ID: 1, 2 (PTS: 65931)
    timestamp: 00:01:06:633, filepos: 000000000
    timestamp: 00:01:11:304, filepos: 000004000
    timestamp: 00:01:13:139, filepos: 000005800
    timestamp: 00:01:17:177, filepos: 000008000
        .............................
    timestamp: 00:12:49:468, filepos: 0001de800
    timestamp: 00:12:51:937, filepos: 0001e1800
    timestamp: 00:12:53:572, filepos: 0001e4000
    # Vob/Cell ID: 1, 3 (PTS: 780346)
    timestamp: 00:13:01:447, filepos: 0001e5800
    timestamp: 00:13:02:615, filepos: 0001e7000
    timestamp: 00:13:04:016, filepos: 0001e9800
        .............................
    timestamp: 01:38:21:662, filepos: 000f04000
    timestamp: 01:38:23:297, filepos: 000f08000
    timestamp: 01:38:30:070, filepos: 000f0a800

    # English
    id: en, index: 1
    # Decomment next line to activate alternative name in DirectVobSub / Windows Media Player 6.x
    # alt: English
    # Vob/Cell ID: 1, 2 (PTS: 65931)
    timestamp: 00:01:06:633, filepos: 000002000
    timestamp: 00:01:11:304, filepos: 000003800
    timestamp: 00:01:13:139, filepos: 000006000
        .............................
        .............................
    timestamp: 01:38:21:662, filepos: 000f05000
    timestamp: 01:38:23:297, filepos: 000f09000
    timestamp: 01:38:30:070, filepos: 000f0b000

    # Chinese
    id: zh, index: 2
    # Decomment next line to activate alternative name in DirectVobSub / Windows Media Player 6.x
    # alt: Chinese
    # Vob/Cell ID: 1, 2 (PTS: 65931)
    timestamp: 00:01:06:633, filepos: 000001000
    timestamp: 00:01:11:304, filepos: 000003000
    timestamp: 00:01:13:139, filepos: 000004800
        .............................
        .............................
    timestamp: 01:38:21:662, filepos: 000f04800
    timestamp: 01:38:23:297, filepos: 000f07000
    timestamp: 01:38:30:070, filepos: 000f0a000
*/

    private boolean stopFlag = false;
    private int vob_ID;
    private int cell_ID;
    private int pts;
    private int filePOS;
    private long savedTimeSub = -1;
    private int sizeCx = 0;
    private int sizeCy = 0;
    private int[] palette = new int[16];            // save palette information on .idx file
    private boolean customColors;
    private int tridx;
    private int [] color = new int [4];
    private int langIdx;                            // language index number
    private int index = 0;                          // current index number, we will show langIdx == index
    private boolean fLanguageMatch = false;         // if langIdx == index, this flag will be set to true

    private void setupSUB() {
        if (useSub) {
            try { in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(idxFile.toString())), detectEncoding(idxFile.toString()))); }
            catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }

            try {
                while (((s = in.readLine()) != null) && (stopFlag == false)) {
                    if (s.contains("# Vob/Cell ID: ")) {
                        vob_ID = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.indexOf(",")).trim());

                        s = s.substring(s.indexOf(",") + 1, s.length());    // git rid of vob_ID portion
                        cell_ID = Integer.parseInt(s.substring(0, s.indexOf("(")).trim());

                        pts = Integer.parseInt(s.substring(s.indexOf("PTS:") + 4, s.lastIndexOf(")")).trim());

                    } else if (s.toLowerCase().contains("timestamp:")) {
                        if (fLanguageMatch == true) {
                            t1 = s.substring(s.toLowerCase().indexOf("timestamp:") + 10, s.indexOf(",")).trim();
                            t2 = s.substring(s.toLowerCase().indexOf("filepos:") + 8, s.length()).trim();

                            timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                            t1 = t1.substring(t1.indexOf(":") + 1, t1.length()).trim();     // get rid of hour
                            timeStartMinute = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                            t1 = t1.substring(t1.indexOf(":") + 1, t1.length()).trim();     // get rid of minute
                            timeStartSecond = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                            t1 = t1.substring(t1.indexOf(":") + 1, t1.length()).trim();     // get rid of second
                            timeStartMillisecond = Integer.parseInt(t1.substring(0, t1.length()).trim());
                            timeSUB = ((timeStartHour * 60 + timeStartMinute) * 60 + timeStartSecond) * 1000 + timeStartMillisecond;

                            filePOS = Integer.parseInt(t2.trim(), 16);

                            if (savedTimeSub == -1) {
                                parsedGraphicSubtitle.add(new VideoPlayerGraphicSubtitle(timeSUB, filePOS));
                                savedTimeSub = timeSUB;
                                stopFlag = false;
                            } else if (timeSUB > savedTimeSub) {
                                parsedGraphicSubtitle.add(new VideoPlayerGraphicSubtitle(timeSUB, filePOS));
                                savedTimeSub = timeSUB;
                            } else {
                                stopFlag = true;
                            }
                        }

                    } else if (s.toLowerCase().contains("size:")) {
                        sizeCx = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.toLowerCase().indexOf("x")).trim());
                        sizeCy = Integer.parseInt(s.substring(s.toLowerCase().indexOf("x") + 1, s.length()).trim());

                    } else if (s.toLowerCase().contains("index:")) {
                        index = Integer.parseInt(s.substring(s.toLowerCase().lastIndexOf(":") + 1, s.length()).trim());
                        if (langIdx == index) {
                            fLanguageMatch = true;      // we will save time stamp and position
                        } else {
                            fLanguageMatch = false;     // ignore this frame
                        }

                    } else if (s.toLowerCase().contains("palette:")) {
                        s = s.substring(s.toLowerCase().indexOf("palette:") + 8, s.length()).trim();
                        s = s + ","; // for the consistency
                        for (int i = 0; i < 16; i++) {
                            palette[i] = (Integer.parseInt(s.substring(0, s.indexOf(",")).trim(), 16)) & 0x00ffffff;
                            s = s.substring(s.indexOf(",") + 1, s.length());
                        }

                    } else if (s.toLowerCase().contains("custom colors:")) {
                        String cColors = s.substring(s.indexOf("custom colors:") + 17, s.indexOf("custom colors:") + 18).toLowerCase().trim();
                        customColors = !cColors.equals("f");        // ON : true, OFF : false
                        tridx = Integer.parseInt(s.substring(s.indexOf("tridx:") + 6, s.indexOf(", colors:")).trim(), 16);
                        s = s.substring(s.indexOf(", colors:") + 9, s.length());
                        s = s + ","; // for the consistency
                        for (int i = 0; i < 4; i++) {
                            color[i] = (Integer.parseInt(s.substring(0, s.indexOf(",")).trim(), 16)) & 0x00ffffff;
                            s = s.substring(s.indexOf(",") + 1, s.length());
                        }
                    } else if (s.toLowerCase().contains("langidx:")) {
                        langIdx = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.length()).trim());
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }

            try { in.close(); }
            catch (IOException e) { e.printStackTrace(); }

//            showLog("saved " + parsedGraphicSubtitle.size() + " subtitles");
//            showLog("example: " + parsedGraphicSubtitle.get(0).getTime() + ", " + parsedGraphicSubtitle.get(1).getTime() + ", " +
//                            parsedGraphicSubtitle.get(2).getTime() + ", " + parsedGraphicSubtitle.get(3).getTime() + ", " +
//                            parsedGraphicSubtitle.get(4).getTime() + ", " + parsedGraphicSubtitle.get(5).getTime() + ", " +
//                            parsedGraphicSubtitle.get(6).getTime() + ", " + parsedGraphicSubtitle.get(7).getTime() + ", " +
//                            parsedGraphicSubtitle.get(8).getTime() + ", " + parsedGraphicSubtitle.get(9).getTime() + ", " +
//                            parsedGraphicSubtitle.get(10).getTime() + ", " + parsedGraphicSubtitle.get(11).getTime() + ", " +
//                            parsedGraphicSubtitle.get(12).getTime() + ", " + parsedGraphicSubtitle.get(13).getTime());

            if (parsedGraphicSubtitle.size() <= 1) {
                useSub = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

/*
    packet header rule (.sub file) :

    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 00 | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 0a | 0b | 0c | 0d | 0e | 0f | address
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 00 | 00 | 01 | ba |    |    |    |    |    |    |    |    |    |    | 00 | 00 | data
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+

    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 1a | 1b | 1c | 1d | 1e | 1f | address
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 01 | bd |    |    |    |(*1)| pq |(*2)|    |    |    |    |    | wx | yz | WX | data
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+

    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 | 29 | 2a | 2b | 2c | 2d | 2e | 2f | address
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | YZ |    |    |    |    |    |    |    |    |    |    |    |    |    |    |    | data
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+

    buf[0x00] should be 00 00 01 ba (fixed value)
    buf[0x0e] should be 00 00 01 bd (fixed value)
    (*1) buf[0x15] & 0x80 should be true
    (*2) (buf[0x17] & 0xf0) should be 0x20
    (buf[buf[0x16] + 0x17] & 0xe0) should be 0x20
    (buf[buf[0x16] + 0x17] & 0x1f) should be supported number of Language

    buf[16] = [pq] = normally 0x05 (very first block can contains 0x08, it means there are optional information 3 bytes)
    packet size = (buf[buf[0x16] + 0x18] << 8) + buf[buf[0x16] + 0x19] = normally wxyz(hexa)
    data size = (buf[buf[0x16] + 0x1a] << 8) + buf[buf[0x16] + 0x1b] = normally WXYZ(hexa)

    for example,
    buf[0x16] = 05(pq), buf[0x1d] = 07(wx), buf[0x1e] = 9a(yz), buf[0x1f] = 07(WX), buf[0x20] = 7b(YZ)
    then, packet size = 0x079a bytes, data size = 0x077b bytes
    thus, information length = packet size - data size = 0x079a - 0x077b = 0x1f bytes

    for example,
    buf[0x16] = 08(pq), buf[0x20] = 0b(wx), buf[0x21] = 70(yz), buf[0x22] = 0b(WX), buf[0x23] = 51(YZ)
    then, packet size = 0x0b70 bytes, data size = 0x0b51 bytes
    thus, information length = packet size - data size = 0x0b70 - 0x0b51 = 0x1f bytes
*/

    private int subIDXpointer;
    private byte [] buf = new byte[0x0800];             // one unit
    private byte [] pData = new byte[BUF_LENGTH];     // real data buffer

    private int packetSize = 0;         // packet Size > dataSize (we should keep it)
    private int dataSize = 0;
    private int savedSize = 0;

    final int IDmarkA = 0x00;           // indicator A = 00 00 01 ba
    final int IDmarkB = 0x0e;           // indicator B = 00 00 01 bd
    final int PESmark = 0x15;           // bit 7
    final int OPTIONS = 0x16;           // option byte length
    final int ID_LANG = 0x17;           // ID and language code
    final int PACKET_SIZE = 0x18;       // packet size
    final int DATA_SIZE = 0x1a;         // data size

    private Bitmap getBitmapSubtitle(int filePos) {
        Bitmap graphic = null;
        subIDXpointer = filePos;     // save file position

        // .sub data contains on subtitleFile[0] to subtitleFile[subtitleFile.length]
        System.arraycopy(subtitleFile, subIDXpointer, buf, 0, buf.length);
        subIDXpointer = subIDXpointer + buf.length;

        // check .sub file rule:
        if ((unsigned(buf[IDmarkA]) != 0x00) || (unsigned(buf[IDmarkA + 1]) != 0x00) || (unsigned(buf[IDmarkA + 2]) != 0x01) || (unsigned(buf[IDmarkA + 3]) != 0xba)) { return null; }
        if ((unsigned(buf[IDmarkB]) != 0x00) || (unsigned(buf[IDmarkB + 1]) != 0x00) || (unsigned(buf[IDmarkB + 2]) != 0x01) || (unsigned(buf[IDmarkB + 3]) != 0xbd)) { return null; }
        if ((unsigned(buf[PESmark]) & 0x80) == 0x00) { return null; }
        if ((unsigned(buf[ID_LANG]) & 0xf0) != 0x20) { return null; }
        if (((unsigned(buf[unsigned(buf[OPTIONS]) + ID_LANG])) & 0xe0) != 0x20) { return null; }        // upper 3 bits = index (0x20 => 0x01)
        if ((unsigned(buf[unsigned(buf[OPTIONS]) + ID_LANG]) & 0x1f) != langIdx) { return null; }     // lower 5 bits = language

        // read packet header information
//        showLog("data 1d~20 = " + buf[buf[0x16] + 0x18] + ", " + buf[buf[0x16] + 0x19] + ", " + buf[buf[0x16] + 0x1a] + ", " + buf[buf[0x16] + 0x1b]);
        packetSize = ((unsigned(buf[unsigned(buf[OPTIONS]) + PACKET_SIZE])) << 8) + unsigned(buf[unsigned(buf[OPTIONS]) + PACKET_SIZE + 1]);
        dataSize = ((unsigned(buf[unsigned(buf[OPTIONS]) + DATA_SIZE])) << 8) + unsigned(buf[unsigned(buf[OPTIONS]) + DATA_SIZE + 1]);

        if (packetSize > BUF_LENGTH) { return null; }

        condenseBuffer();

        if (packetSize > savedSize) {
            savedSize = packetSize;         // we want to see how much data comming
            showLog("file pos and packet size = " + filePos + ", " + packetSize);
        }

        // copy BYTE array into INT array
//        IntBuffer intBuf = ByteBuffer.wrap(nbuf).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
//        int [] ibuf = new int [intBuf.remaining()];
//        intBuf.get(ibuf);

        getPacketInfo();        // collect rendering information

        getBitmapData();        // collect bitmap data from subtitle packet

//        trimSubImage();         // cut off meaningless area

//        graphic = Bitmap.createBitmap(pixels, 0, sizeCx, sizeCx, sizeCy, Bitmap.Config.ARGB_8888);
        graphic = Bitmap.createBitmap(pixels, 0, sizeCx, sizeCx, sizeCy, Bitmap.Config.ARGB_8888);
        Bitmap newGraphic = Bitmap.createScaledBitmap(graphic, sizeCx * 2, sizeCy * 2, true);
        return newGraphic;
    }

/*
    we have four blocks of buffer, one buffer contains 0x0800 bytes.
    each buffer have packet header (length is vary)
    if we treat two more blocks of data, we should get rid of this packet header from the second block.
    packet header length of the second block to fourth block is 0x18 bytes.
    if the packet exceeds four blocks (0x0800 * 4 bytes), we just ignore it because we can't display it by time limitation.

    buf[0]
    +-----------------------+       +-----------------------+
    |   packet header 1     |       | pkt size  & data size | <-- 4 bytes
    +-----------------------+       +-----------------------+
    |                       |       |                       |
    |   data block 1        |       |   data block 1        | <-- 0x0800 - hsize bytes
    |                       |       |                       |
    +-----------------------+       +-----------------------+
    |   packet header 2     |       |                       |
    +-----------------------+       |   data block 2        | <-- 0x0800 - (hsize=0x18 + buf[0x16]) bytes
    |                       |  ==>  |                       |
    |   data block 2        |       +-----------------------+
    |                       |       +-----------------------+
    +-----------------------+       |                       |
    +-----------------------+       |   data block n        |
    |   packet header n     |       |                       |
    +-----------------------+       +-----------------------+
    |                       |
    |   data block n        |
    |                       |
    +-----------------------+


For example : data length = 0x0b26
    +-----------------------+       +-----------------------+
    |    header 0x1d byte   |       |    header 0x18 byte   |
    +-----------------------+       +-----------------------+
    |                       |       |                       |
    |    data block 1       |   +   |    data block n       |
    |    0x07e3 bytes       |       |         x bytes       |  x = 0x0b26 - 0x0800 + 0x1d + 0x18
    |                       |       |                       |    = 0x035b bytes
    +-----------------------+       +-----------------------+
           1st packet                      nth packet
                                  (buf[0x16]+0x17 == lang | 0x20)
*/

    private int srcIndex;
    private int dstIndex;
    private int sizeLeft;
    private int copyLength;
    private int hsize;
    private int nLang;

    private void condenseBuffer() {
        hsize = unsigned(buf[OPTIONS]) + PACKET_SIZE;
        nLang = unsigned(buf[buf[OPTIONS] + ID_LANG]) & 0x1f;

        srcIndex = hsize;
        dstIndex = 0;
        sizeLeft = packetSize;
        copyLength = Math.min(sizeLeft, 0x0800 - hsize);
        System.arraycopy(buf, srcIndex, pData, dstIndex, copyLength);
        sizeLeft = sizeLeft - copyLength;
        dstIndex = dstIndex + copyLength;

        if (sizeLeft != 0) {
            while (true) {
                System.arraycopy(subtitleFile, subIDXpointer, buf, 0, buf.length);
                subIDXpointer = subIDXpointer + buf.length;

                if (unsigned(buf[buf[OPTIONS] + ID_LANG]) == (langIdx | 0x20)) { break; }
            }

            hsize = unsigned(buf[OPTIONS]) + PACKET_SIZE;
            srcIndex = hsize;
            copyLength = Math.min (sizeLeft, 0x0800 - hsize);
            System.arraycopy(buf, srcIndex, pData, dstIndex, copyLength);
            sizeLeft = sizeLeft - copyLength;
            dstIndex = dstIndex + copyLength;
        }
    }

/*
    Sub-Pictures
    The typical arrangement of data in the 53220 byte buffer for sub-pictures is
            +------+-------+-------+----------+
            | (1)  |      (2)      |    (3)   |
            | SPUH | PXDtf | PXDbf | SP_DCSQT |
            +------+-------+-------+----------+
    However, the only requirement is that the header (SPUH) be first, all other areas are reached by pointers.


    (1) SPUH (Sub-Picture Unit Header)
    2 words (small endian - least significant byte last)
            offset	name	    contents
                0	SPDSZ	    the size of the total sub-picture data (which may span packets)
                2	SP_DCSQTA	offset within the Sub-Picture Unit to the SP_DCSQT


    (2) PXDtf and PXDbf

    PiXel Data
    These are the rle compressed pixel data for the top field (lines 1, 3, 5, etc) and
    the bottom field (lines 2, 4, 6, etc) respectively
    Individual pixels may have one of four values, commonly referred to as background (0), pattern (1), emphasis 1 (2),
    and emphasis 2 (3).
    Each coded value indicates the number of pixels having the same code value, and can be in one of four forms,
    depending on the number of identical pixels
            range	bits	format
            1-3	    4	    n n c c
            4-15	8	    0 0 n n n n c c
            16-63	12	    0 0 0 0 n n n n n n c c
            64-255	16	    0 0 0 0 0 0 n n n n n n n n c c
    One special case, encoding a count of zero using the 16-bit format indicates the same pixel value until the end of the line.
    If, at the end of a line, the bit count is not a multiple of 8, four fill bits of 0 are added.


    (3) SP_DCSQT (Sub-Picture Display Control SeQuence Table)
    This area contains blocks (SP_DCSQ) of commands to the decoder. Each SP_DCSQ begins with a 2 word header
            offset	name	        contents
                0	SP_DCSQ_STM	    delay to wait before executing these commands.
                                    The units are 90KHz clock (same as PTM) divided by 1024 - see conversion aids
                2	SP_NXT_DCSQ_SA	offset within the Sub-Picture Unit to the next SP_DCSQ.
                                    If this is the last SP_DCSQ, it points to itself.

    (4) Commands

    There are eight commands available for Sub-Pictures.
    The first SP_DCSQ should contain, as a minimum, SET_COLOR, SET_CONTR, SET_DAREA, and SET_DSPXA.

    FF - CMD_END - ends one SP_DCSQ

    00 - FSTA_DSP - Forced Start Display, no arguments

    01 - STA_DSP - Start Display, no arguments

    02 - STP_DSP - Stop Display, no arguments

    03 - SET_COLOR - provides four indices into the CLUT for the current PGC to associate with the four pixel values.
                     One nibble per pixel value for a total of 2 bytes.
        e2 e1   p b

    04 - SET_CONTR - directly provides the four contrast (alpha blend) values to associate with the four pixel values.
                     One nibble per pixel value for a total of 2 bytes. 0x0 = transparent, 0xF = opaque
        e2 e1   p b

    05 - SET_DAREA - defines the display area, each pair (X and Y) of values is 3 bytes wide,
                     for a total of 6 bytes, and has the form
        sx sx   sx ex   ex ex   sy sy   sy ey   ey ey
            sx = starting X coordinate
            ex = ending X coordinate
            sy = starting Y coordinate
            ey = ending Y coordinate

    06 - SET_DSPXA - defines the pixel data addresses.
                     First a 2-byte offset to the top field data, followed by a 2-byte offset to the bottom field data,
                     for a total of 4 bytes.

    07 - CHG_COLCON - allows for changing the COLor and CONtrast within one or more areas of the display.
                  This command contains a series of parameters, arranged in a hierarchy.
                  Following the command byte is a 2-byte value for the total size of the parameter area, including the size word.
                  The parameter sequence begins with a LN_CTLI, which defines a vertically bounded area of the display.
                  The LN_CTLI may include from one to fifteen PX_CTLI parameters, which define a starting horizontal position
                  and new color and contrast value to apply from that column on towards the right to the next PX_CTLI or
                  the right side of the display.

         LN_CTLI, 4 bytes, special value of 0f ff ff ff signifies the end of the parameter area
                  (this termination code MUST be present as the last parameter)
                    0 s   s s   n t   t t
                        sss = csln, the starting (top-most) line number for this area
                        n = number of PX_CTLI to follow
                        ttt = ctln, the terminating (bottom-most) line number for this area

         PX_CTLI, 6 bytes, defines a starting column and new color and contrast values
                    bytes 0 and 1 - starting column number
                    bytes 2 and 3 - new color values, as per SET_COLOR
                    bytes 4 and 5 - new contrast values, as per SET_CONTR


    Converting frames and time to SP_DCSQ_STM values

    The direct method of converting time to delay values is to multiply time in seconds by 90000/1024 and truncate the value.
    Rounding up will cause the display to occur one frame late.
*/

    private boolean fForced = true;
    private int t;
    private int pal;
    private int tr;
    private int delay;
    private Rect rect = new Rect(0,0,0,0);

    private int [] nOffset = new int [2];
    private int [] palPal = new int [4];
    private int [] palTr = new int [4];

    private int dataIndex;
    private int nextCtrlBlk;
    private boolean fBreak;

    final int FORCED_START_DISPLAY = 0;         // forced start displaying
    final int START_DISPLAY = 1;                // start displaying (at last, we will have this command)
    final int STOP_DISPLAY = 2;                 // stop displaying
    final int GET_PALETTE = 3;                  // next 2 bytes palette data
    final int GET_TRIDX = 4;                    // next 2 bytes IDX
    final int GET_RECTANGLE = 5;                // next 6 bytes (x1, y1, x2, y2) = (left, top, right, bottom)
    final int GET_PLANE_OFFSET = 6;             // next 4 bytes (plane 0 : 2 bytes, plane 1 : 2 bytes) - interace mode
    final int END_OF_CONTROL_BLOCK = 255;       // end of control block, other codes will be ignored, stop analyzing

    private void getPacketInfo() {
        dataIndex = dataSize;       // before packet start, we will have 4 bytes of t(2 bytes) and next control block(2 bytes)

        t = ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]));
        dataIndex = dataIndex + 2;

        nextCtrlBlk = ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]));
        dataIndex = dataIndex + 2;

        // we should note that : dataSize < nextCtrlBlk < packetSize
        fBreak = false;

        do {
            switch (unsigned(pData[dataIndex])) {
                case FORCED_START_DISPLAY:      // forced start displaying
                    dataIndex++;
                    fForced = true;
                    break;

                case START_DISPLAY:      // start displaying
                    dataIndex++;
                    fForced = false;
                    break;

                case STOP_DISPLAY:      // stop displaying
                    dataIndex++;
                    delay = 1024 * t / 90;
                    break;

                case GET_PALETTE:      // get palette
                    dataIndex++;
                    pal = ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]));
                    dataIndex = dataIndex + 2;

// the following code does not work correctly, so we will ignore this command(get palette).
// instead, we will make another color palette named myColor.
//                    int p;
//                    p = (pal >> 12) & 0x0f;
//                    myColor[0] = palette[p] & 0xff000000;   // make transparent
//                    p = (pal >> 8) & 0x0f;
//                    myColor[1] = palette[p];
//                    p = (pal >> 4) & 0x0f;
//                    myColor[2] = palette[p];
//                    p = pal & 0x0f;
//                    myColor[3] = palette[p];

                    break;

                case GET_TRIDX:      // get tridx data
                    dataIndex++;
                    if ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]) != 0) {
                        tr = (unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]);
                    }
                    dataIndex = dataIndex + 2;
                    break;

                case GET_RECTANGLE:      // get rectangle
                    dataIndex++;
                    int left = (unsigned(pData[dataIndex]) << 4) + (unsigned(pData[dataIndex + 1]) >> 4);
                    int top = (unsigned(pData[dataIndex + 3]) << 4) + (unsigned(pData[dataIndex + 4]) >> 4);
                    int right = ((unsigned(pData[dataIndex + 1]) & 0x0f) << 8) + (unsigned(pData[dataIndex + 2]) + 1);
                    int bottom = ((unsigned(pData[dataIndex + 4]) & 0x0f) << 8) + (unsigned(pData[dataIndex + 5]) + 1);
                    rect = new Rect(left, top, right, bottom);
//                    showLog("rect(" + left + ", " + top + " ~ " + right + ", " + bottom + ")");
                    dataIndex = dataIndex + 6;

                case GET_PLANE_OFFSET:      // get offset of top line (plane 0) and bottom line (plane 1)
                    dataIndex++;
                    nOffset[0] = (unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]);
                    dataIndex = dataIndex + 2;

                    nOffset[1] = (unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]);
                    dataIndex = dataIndex + 2;
                    break;

                case END_OF_CONTROL_BLOCK:      // end of control block
                    dataIndex++;
                    fBreak = true;
                    continue;

                default:        // skip this control block
                    dataIndex++;
                    fBreak = true;
                    break;
            }
        } while (!fBreak);

        for (int i = 0; i < 4; i++) {
            palPal[i] = ((pal >> (i << 2)) & 0x0f);
            palTr[i] = ((tr >> (i << 2)) & 0x0f);
        }
    }

    // buf[0]
    // +-----------------------+
    // |   packet header       | (packet header length = hsize)
    // +-----------------------+ <-----+ <-----+ buf[hsize]
    // |                       |       |       |
    // |                       |       |       |
    // |   subtitle data       |       | data  |
    // |                       |       | size  | packet
    // |                       |       |       | size
    // |                       |       |       |
    // +-----------------------+ <-----+       |
    // |   information         |               |   (information length = packet size - data size)
    // +-----------------------+         <-----+

    private int x;
    private int y;
    private int [] pixels = new int [740 * 480];
    private int y1;
    private int y2;

    private int dataPointer;

    private int nPlane;             // from the first line
    private int fAligned = 1;       // from the high nibble

    private int end0;
    private int end1;

    private void getBitmapData() {
        dataPointer = dataSize;

        nPlane = 0;         // from the first line
        fAligned = 1;       // from the high nibble

        end0 = nOffset[1];
        end1 = dataPointer;

        if (nOffset[0] > nOffset[1]) {
            end1 = nOffset[0];
            end0 = dataPointer;
        }

        x = rect.left;
        y = rect.top;
//        y1 = rect.top;
//        y2 = rect.bottom / 2;

        while ((nPlane == 0 && nOffset[0] < end0) || (nPlane == 1 && nOffset[1] < end1)) {
            int code;

            if((code = getNibble()) >= 0x4
                    || (code = (code << 4) | getNibble()) >= 0x10
                    || (code = (code << 4) | getNibble()) >= 0x40
                    || (code = (code << 4) | getNibble()) >= 0x100) {
                drawPixels(code >> 2, code & 3);
                if((x += code >> 2) < rect.right) continue;
            }

            drawPixels(rect.right - x, code & 3);

            if (fAligned == 0) { getNibble(); }        // align to byte

            x = rect.left;              // initialize x pointer
            y++;
            nPlane = 1 - nPlane;        // go to the second line
        }

        rect.bottom = Math.min(y, rect.bottom);
        trimSubImage();
    }

    // if alpha == 0xff, it is not transparent (white)
    //
    // for the reference,
    // transparent white(0x00ffffff) = grey(0x00808080) = black(0x00000000)
    // non-transparent white = 0xffffffff, grey = 0xff808080, black = 0xff000000
    //
    // if we change grey to 0x00808080, grey will be gone ! (i.e., black/white only)
    // if we change grey to 0xffffffff, grey will be shown by white ! (i.e., black/white also)

    private int tBLACK = 0x00000000 & 0xffffffff;       // transparent black
    private int nBLACK = 0xff000000 & 0xffffffff;       // black
    private int tGREY = 0x00808080 & 0xffffffff;        // transparent grey
    private int nGREY = 0xff808080 & 0xffffffff;        // grey
    private int tWHITE = 0x00ffffff & 0xffffffff;       // transparent white
    private int nWHITE = 0xffffffff & 0xffffffff;       // white

    // int [] myColor = {tWHITE, nWHITE, nGREY, nBLACK};         // original color
    // int [] myColor = {tBLACK, nWHITE, nBLACK, nBLACK};        // my modified color
    // int [] myColor = {tBLACK, tWHITE, nWHITE, nWHITE};        // reverse color
    private int [] myColor = {tBLACK, nWHITE, nBLACK, nBLACK};      // modified color palette for android phone

    private int rgbReserved;        // will be used for trim image
    private int ptr = 0;

    private void drawPixels(int length, int colorid) {
        if ((length <= 0) || (x + length < rect.left) || (x >= rect.right) || (y < rect.top) || (y >= rect.bottom)) {
            return;
        }

        if (x < rect.left) {
            x = rect.left;
        }

        if (x + length >= rect.right) {
            length = rect.right - x;
        }

        ptr = rect.width() * (y - rect.top) + (x - rect.left);

        int c;
        // original code : support 16 colors, but it does not work for android
//        if (!customColors) {
//            c = palette[palPal[colorid]];
//            rgbReserved = (palTr[colorid] << 4) | palTr[colorid];
//        } else {
//            c = color[colorid];
//        }

        c = myColor[colorid & 0x03];

        while (length-- > 0) {
            pixels[ptr] = c;            // put palette data
            ptr++;                      // by length
        }
    }

    private void trimSubImage() {
        rect.left = rect.width();
        rect.top = rect.height();
        rect.right = 0;
        rect.bottom = 0;

        for (int j = 0, y = rect.height(); j < y; j++) {
            for (int i = 0, x = rect.width(); i < x; i++, ptr++) {
                if (rgbReserved != 0) {
                    if (rect.top > j) { rect.top = j; }
                    if (rect.bottom < j) { rect.bottom = j; }
                    if (rect.left < i) { rect.left = i; }
                    if (rect.right < i) { rect.right = i; }
                }
            }
        }

        if ((rect.left > rect.right) || (rect.top > rect.bottom)) { return; }

        rect.right += 1;
        rect.bottom += 1;

        rect.left &= 0;
        rect.top &= 0;
        rect.right &= rect.width();
        rect.bottom &= rect.height();

        int w = rect.width();
        int h = rect.height();
        int offset = rect.top * rect.width() + rect.left;

        rect.left += 1;
        rect.top += 1;
        rect.right += 1;
        rect.bottom += 1;

        for (h = rect.height(); h < 0; h--) {
            pixels[offset] = 0;
            offset += w;
        }
    }

    private int offset;

    // offset moves 0.5 bytes (4 bits) because of fAligned value.
    private int getNibble() {
        offset = nOffset[nPlane];
        int result = (unsigned(pData[offset]) >> (fAligned << 2)) & 0x0f;

        if (fAligned == 1) {
            fAligned = 0;
        } else if (fAligned == 0) {
            fAligned = 1;
        }

//        offset = offset + fAligned;
        nOffset[nPlane] += fAligned;
        return result;
    }

    private long maxRunningTime = 0L;

    public Handler textHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mVV_show.getCurrentPosition() <= maxRunningTime) {
                countSub = getSubSyncIndexText(mVV_show.getCurrentPosition());
                mTV_subtitle.setText(Html.fromHtml(parsedTextSubtitle.get(countSub).getText()));
            }
        }
    };

    // if get(index) <= playTime < get(index+1), then return index
    //
    //                      <=    playTime    <
    //    +-----------------+-----------------+--------------------+
    //    0             get(index)      get(index + 1)         get(size)
    //
    public int getSubSyncIndexText(long playTime) {
        int lowLimitT = 0;
        int highLimitT = parsedTextSubtitle.size();
        int indexPointerT;

        while(lowLimitT <= highLimitT) {
            indexPointerT = (lowLimitT + highLimitT) / 2;
            if((parsedTextSubtitle.get(indexPointerT).getTime() <= playTime) && (playTime < parsedTextSubtitle.get(indexPointerT + 1).getTime())) {
                return indexPointerT;
            }
            if(playTime >= parsedTextSubtitle.get(indexPointerT + 1).getTime()) {
                lowLimitT = indexPointerT + 1;
            } else {
                highLimitT = indexPointerT - 1;
            }
        }
        return 0;
    }

    private int savedCountSub = -1;

    public Handler idxHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mVV_show.getCurrentPosition() <= maxRunningTime) {
                countSub = getSubSyncIndexGraphic(mVV_show.getCurrentPosition());
                if (countSub != savedCountSub) {
                    savedCountSub = countSub;
                    mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));
                }
            }
        }
    };

    public int getSubSyncIndexGraphic(long playTime) {
        int lowLimitG = 0;
        int highLimitG = parsedGraphicSubtitle.size();
        int indexPointerG;

        while(lowLimitG <= highLimitG) {
            indexPointerG = (lowLimitG + highLimitG) / 2;
            if((parsedGraphicSubtitle.get(indexPointerG).getTime() <= playTime) && (playTime < parsedGraphicSubtitle.get(indexPointerG + 1).getTime())) {
                return indexPointerG;
            }
            if(playTime >= parsedGraphicSubtitle.get(indexPointerG + 1).getTime()) {
                lowLimitG = indexPointerG + 1;
            } else {
                highLimitG = indexPointerG - 1;
            }
        }
        return 0;
    }

    private void addOneMoreLine() {
        if (useSmi || useSrt || useAss || useSsa) {
            int h = parsedTextSubtitle.size() - 1;
            maxRunningTime = parsedTextSubtitle.get(h).getTime();
            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(maxRunningTime + 500, "The End"));
        } else if (useSub) {
            int h = parsedGraphicSubtitle.size() - 1;
            maxRunningTime = parsedGraphicSubtitle.get(h).getTime();
            parsedGraphicSubtitle.add(new VideoPlayerGraphicSubtitle(maxRunningTime + 500, 0));
        }
    }

    private String detectEncoding(String filename) {
        int numberOfRead;
        FileInputStream fis = null;

        try { fis = new FileInputStream(filename); }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        CodeDetector detector = new CodeDetector(null);

        try {
            if (fis != null) {
                while ((numberOfRead = fis.read(pData)) > 0 && !detector.isDone()) {
                    detector.handleData(pData, 0, numberOfRead);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        detector.dataEnd();

        String encoding = detector.getDetectedCharset();
        if (encoding == null) {
            encoding = ENCODING;        // set default encoding method
        }

        detector.reset();
        return encoding;
    }

    // if we convert byte data to integer, java recognize it into signed integer.
    // thus, we need to cut off unused bits by (& 0xff) operation.
    //
    // (byte [])  0000 0000 0000 0000 0000 0000 1111 1111 (255)
    //    int     1111 1111 1111 1111 1111 1111 1111 1111 (-1)
    //  (& 0xff)  0000 0000 0000 0000 0000 0000 1111 1111 (255)
    private int unsigned(byte chr) {
        return (int) (chr  & 0xff);
    }
}
