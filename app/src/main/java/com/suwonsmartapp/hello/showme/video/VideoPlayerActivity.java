package com.suwonsmartapp.hello.showme.video;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
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
import com.suwonsmartapp.hello.showme.file.FileInfo;

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
    private final int BUF_LENGTH = 256 * 8 * 4;     // enough size (maybe 3 frame is enough)
    private final int sleepTime = 300;              // 1000 means 1 second

    private int mCurrentPosition;                   // current playing pointer
    private ArrayList<FileInfo> mVideoFileInfoList;    // video file media_player_icon_information list

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

        // 타이틀바를 지우고 전체 스크린을 사용함.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.video_player_activity);

        // 화면을 가로모드로 고정함.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 인텐트를 통해 경로명과 파일명을 읽음.
        Intent intent = getIntent();
        if (intent != null) {
            mCurrentPosition = intent.getIntExtra("currentPosition", -1);
            mVideoFileInfoList = intent.getParcelableArrayListExtra("videoInfoList");
        } else {
            showToast(getString(R.string.msg_wrong_file));
            finish();
        }

        setupVideoScreen();     // 자막 파일이 존재하는지 검사함
        setupASS();             // ASS/SSA 자막이 존재하면 이를 준비함
        setupSUB();             // SUB 자막이 존재하면 이를 준비함
        setupSRT();             // SRT 자막이 존재하면 이를 준비함
        setupSMI();             // SMI 자막이 존재하면 이를 준비함
        addOneMoreLine();       // 에러 방지를 위해 자막 1 라인을 추가함.

        MediaController mController = new MediaController(this);
        mController.setAnchorView(mVV_show);
        mVV_show.setMediaController(mController);
        mVV_show.setOnPreparedListener(this);
        mVV_show.setOnCompletionListener(this);
    }

    private String fullPathname = "";

    private void setupVideoScreen() {
        FileInfo videoFileInfo = mVideoFileInfoList.get(mCurrentPosition);
        fullPathname = videoFileInfo.getTitle();
        String requestedPathname = fullPathname.substring(0, fullPathname.lastIndexOf('/'));
        String requestedFilename = fullPathname.substring(fullPathname.lastIndexOf('/') + 1, fullPathname.length());

        mVV_show = (VideoView) findViewById(R.id.vv_show);
        mTV_subtitle = (TextView)findViewById(R.id.vv_subtitle);
        mIV_subtitle = (ImageView)findViewById(R.id.iv_subtitle);
        mTV_subtitle.setText("");

        detectSubtitle();               // .smi .srt .ass .ssa 자막이 존재하는지 검사함.

        mVV_show.setVideoPath(fullPathname);
        mVV_show.requestFocus();
    }

    private ArrayList<VideoPlayerTextSubtitle> parsedTextSubtitle;
    private ArrayList<VideoPlayerGraphicSubtitle> parsedGraphicSubtitle;

    private File subFile;                           // sub 파일
    private String subFilename = "";                // sub 파일명
    private File idxFile;                           // idx 파일
    private byte [] subtitleFile;                   // .sub 파일을 모두 읽을 버퍼

    private boolean useSmi = false;                 // smi 파일을 사용할 경우 true
    private boolean useSrt = false;                 // srt 파일을 사용할 경우 true
    private boolean useAss = false;                 // ass 파일을 사용할 경우 true
    private boolean useSsa = false;                 // ssa 파일을 사용할 경우 true
    private boolean useSub = false;                 // sub/idx 파일을 사용할 경우 true

    // .smi, .srt, .ass, .ssa, .idx, 및 .sub 파일이 존재하는지 검사함.
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
                subFilename = fullPathname.substring(fullPathname.lastIndexOf("/") + 1,
                        fullPathname.lastIndexOf(".")) + ".sub";
                subFile = new File(subPathname);
                useSub = subFile.isFile() && subFile.canRead();
            } else {
                useSub = false;     // .sub는 없고 .idx만 있을 경우 자막을 무시함.
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
                    fPtr = fPtr + 0x0800;           // 모든 프레임은 0x0800 바이트임.
                }
            }
        }
    }

    private int countSub;

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mVV_show.seekTo(0);
        mVV_show.start();       // 준비가 되면 자동 시작.

        // 텍스트 기반의 자막일 경우.
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

        // 그래픽 기반의 자막일 경우.
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
        mCurrentPosition++;                 // 다음 영화
        if (mCurrentPosition == mVideoFileInfoList.size()) {
            mCurrentPosition = 0;           // 끝나면 처음부터 다시 시작함.
        }

        setupVideoScreen();     // 자막 파일이 존재하는지 검사함
        setupASS();             // ASS/SSA 자막이 존재하면 이를 준비함
        setupSUB();             // SUB 자막이 존재하면 이를 준비함
        setupSRT();             // SRT 자막이 존재하면 이를 준비함
        setupSMI();             // SMI 자막이 존재하면 이를 준비함
        addOneMoreLine();       // 에러 방지를 위해 자막 1 라인을 추가함.

        mVV_show.seekTo(0);
        mVV_show.start();       // 준비가 되면 자동 시작.

        // 텍스트 기반의 자막일 경우.
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

        // 그래픽 기반의 자막일 경우.
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
    .smi 자막 파일의 예

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

    // SMI 파일 구조:
    //
    // <SYNC Start=370000>
    // 메시지(자막) 라인 1
    // 메시지(자막) 라인 2

    private BufferedReader in;
    private String s;
    private String text = null;
    private long timeSUB = -1;
    private boolean subStart = false;

    private void setupSMI() {
        if (useSmi) {
            try { in = new BufferedReader(new InputStreamReader(new FileInputStream(
                    new File(subFile.toString())), detectEncoding(subFile.toString()))); }
            catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }

            try {
                while ((s = in.readLine()) != null) {
                    if (s.contains("<SYNC")) {
                        subStart = true;
                        if (timeSUB != -1) {
                            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(timeSUB, text));
                        }
                        timeSUB = Integer.parseInt(s.substring(s.indexOf("=") + 1,
                                s.indexOf(">")).trim());
                        // <SYNC... 를 제거함.
                        text = s.substring(s.indexOf(">") + 1, s.length());
                        text = text.substring(text.indexOf(">") + 1, text.length());

                    } else {
                        if (subStart) {
                            if (s.toLowerCase().contains("<br>")) {
                                if (text.equals("")) {
                                    text = text + s.substring(0, s.indexOf("<"));
                                    if (s.substring(s.indexOf(">"), s.length()).length() > 1) {
                                        text = text + "<br />" + s.substring(s.lastIndexOf(">") + 1,
                                                s.length());
                                    }
                                } else {
                                    text = text + "<br />" + s.substring(0, s.indexOf("<"));
                                    if (s.substring(s.indexOf(">"), s.length()).length() > 1) {
                                        text = text + "<br />" + s.substring(s.lastIndexOf(">") + 1,
                                                s.length());
                                    }
                                }
                            } else {
                                if (text.equals("")) {
                                    text = text + s;
                                    subStart = false;       // 최대 2라인까지
                                } else {
                                    text = text + "<br />" + s;
                                    subStart = false;       // 최대 2라인까지
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }

            try { in.close(); }
            catch (IOException e) { e.printStackTrace(); }

        if (parsedTextSubtitle.size() <= 1) {
            useSmi = false;     // 한줄자리 자막은 무시함. (구간을 구할 수 없어서)
            }
        }
    }

/*
    .srt 자막 파일의 예

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

    // SRT 파일 구조:
    //
    // 123
    // 00:00:00.000 --> 00:00:00.000
    // <i> 메시지(자막) 라인 1 </i>
    // <i> 메시지(자막) 라인 2 </i>

    private String t1, t2;

    private long timeSUBstart = -1;
    private long timeStartHour;
    private long timeStartMinute;
    private long timeStartSecond;
    private long timeStartMillisecond;

    private void setupSRT() {
        if (useSrt) {
            try { in = new BufferedReader(new InputStreamReader(new FileInputStream(
                    new File(subFile.toString())), detectEncoding(subFile.toString()))); }
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
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1,
                                t1.lastIndexOf(':')).trim());
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1,
                                t1.indexOf(',')).trim());
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf(',') + 1,
                                t1.length()).trim());
                        timeSUBstart = ((timeStartHour * 60 + timeStartMinute) * 60
                                + timeStartSecond) * 1000 + timeStartMillisecond;

                        t2 = s.substring(s.lastIndexOf(" --> ") + 5, s.length());
                        long timeEndHour = Integer.parseInt(t2.substring(0, t2.indexOf(':')).trim());
                        long timeEndMinute = Integer.parseInt(t2.substring(t2.indexOf(':') + 1,
                                t2.lastIndexOf(':')).trim());
                        long timeEndSecond = Integer.parseInt(t2.substring(t2.lastIndexOf(':') + 1,
                                t2.indexOf(',')).trim());
                        long timeEndMillisecond = Integer.parseInt(t2.substring(t2.lastIndexOf(',') + 1,
                                t2.length()).trim());
                        long timeSUBend = ((timeEndHour * 60 + timeEndMinute) * 60
                                + timeEndSecond) * 1000 + timeEndMillisecond;

                        text = "";      // 새로운 텍스트를 받기 위해 초기화.

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
            useSrt = false;     // 한줄자리 자막은 무시함. (구간을 구할 수 없어서)
            }
        }
    }

/*
    .ass 자막 파일의 예

    [Script Info]
    ; Script generated by Aegisub 3.2.2
    ; http://www.aegisub.org/
    ScriptType: v4.00+
    PlayResX: 1280
    PlayResY: 720
    Timer: 100.0000
    YCbCr Matrix: TV.601

    [Aegisub Project Garbage]
    Last Style Storage: Default
    Audio File: 150510 HKT48 no Goboten ep48.mp4
    Video File: 150510 HKT48 no Goboten ep48.mp4
    Video AR Mode: 4
    Video AR Value: 1.777778
    Video Zoom Percent: 0.500000
    Scroll Position: 620
    Active Line: 648
    Video Position: 44954

    [V4+ Styles]
    Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
    Style: Default,맑은 고딕,50,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,-1,0,0,0,100,100,0,0,1,2,2,2,10,10,10,1
    Style: 주석,맑은 고딕,30,&H005390F4,&H000000FF,&H00000000,&H00000000,-1,0,0,0,100,100,0,0,1,2,2,2,10,10,10,1

    [Events]
    Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
    Dialogue: 0,0:00:00.41,0:00:02.45,Default,,0,0,0,,오늘로 방송회수
    Dialogue: 0,0:00:02.45,0:00:03.81,Default,,0,0,0,,48
    Dialogue: 0,0:00:03.81,0:00:08.80,Default,,0,0,0,,HKT48의 고보텐!
    Dialogue: 0,0:00:18.11,0:00:18.72,Default,,0,0,0,,자 시작되었습니다
    Dialogue: 0,0:00:18.72,0:00:23.43,Default,,0,0,0,,HKT48의 고보텐 MC인 파라슈트 부대예요\N잘 부탁드려요
    Dialogue: 0,0:00:25.86,0:00:28.71,Default,,0,0,0,,그리고 HKT48 멤버 여러분이에요
    Dialogue: 0,0:00:28.71,0:00:32.47,Default,,0,0,0,,잘 부탁드려요
*/

    // ASS/SSA 파일 구조:
    //
    // Dialog: Layer, Start, End, Style, Name, MarginalL, MarginalR, MarginalV, Effect, Text
    // Dialog: 0,0:00:15.76, ,0:00:16.21, ... ..Text     or
    // Dialog: 0,0:00:15.76, ,0:00:16.21, ... ..{ ... }Text

    private void setupASS() {
        if (useAss || useSsa) {
            if (useAss) {
                try { in = new BufferedReader(new InputStreamReader(new FileInputStream(
                        new File(subFile.toString())), detectEncoding(subFile.toString()))); }
                catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }
            }

            if (useSsa) {
                try { in = new BufferedReader(new InputStreamReader(new FileInputStream(
                        new File(subFile.toString())), detectEncoding(subFile.toString()))); }
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
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1,
                                t1.lastIndexOf(':')).trim());
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1,
                                t1.indexOf('.')).trim());
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf('.') + 1,
                                t1.length()).trim()) * 10;
                        timeSUBstart = ((timeStartHour * 60 + timeStartMinute) * 60
                                + timeStartSecond) * 1000 + timeStartMillisecond;

                        t1 = t2.substring(t2.lastIndexOf(",,") + 2, t2.length());
                        if (t1.equals("")) {
                            text = " ";     // if text is empty, just put one space
                        } else {
                            if (t1.substring(0, 1).equals("{")) {
                                // { ... } 내용은 무시함.
                                t1 = t2.substring(t2.lastIndexOf("}") + 1, t2.length());
                            }
                            text = t1;      // 텍스트를 얻었음.
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
            useAss = false;     // 한줄자리 자막은 무시함. (구간을 구할 수 없어서)
            useSsa = false;
            }
        }
    }

/*
    .idx 인텍스 파일의 예

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
    palette: e83f07, e19120, f3c71b, f8ff18, 9bd22a, 54a530, 12eb12, 15bef6, 0300e3, 4c0353,
                                                     c12262, ffffff, b3b3b3, 808080, 4e4e4e, 000000

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
    private int[] palette = new int[16];            // .idx 파일에 있는 파렛트 정보 저장
    private boolean customColors;
    private int tridx;
    private int [] color = new int [4];
    private int langIdx;                            // 언어 정보(language index)
    private int index = 0;                          // 현재 인덱스 (langIdx == index) 인 경우만 사용
    private boolean fLanguageMatch = false;         // langIdx == index 인 경우 true

    private void setupSUB() {
        if (useSub) {
            try { in = new BufferedReader(new InputStreamReader(new FileInputStream(
                    new File(idxFile.toString())), detectEncoding(idxFile.toString()))); }
            catch (UnsupportedEncodingException | FileNotFoundException e) { e.printStackTrace(); }

            try {
                while (((s = in.readLine()) != null) && (!stopFlag)) {
                    if (s.contains("# Vob/Cell ID: ")) {
                        vob_ID = Integer.parseInt(s.substring(s.indexOf(":") + 1,
                                s.indexOf(",")).trim());

                        s = s.substring(s.indexOf(",") + 1, s.length());    // vob_ID 무시.
                        cell_ID = Integer.parseInt(s.substring(0, s.indexOf("(")).trim());

                        pts = Integer.parseInt(s.substring(s.indexOf("PTS:") + 4,
                                s.lastIndexOf(")")).trim());

                    } else if (s.toLowerCase().contains("timestamp:")) {
                        if (fLanguageMatch) {
                            t1 = s.substring(s.toLowerCase().indexOf("timestamp:") + 10,
                                    s.indexOf(",")).trim();
                            t2 = s.substring(s.toLowerCase().indexOf("filepos:") + 8,
                                    s.length()).trim();

                            timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                            t1 = t1.substring(t1.indexOf(":") + 1, t1.length()).trim(); // 시
                            timeStartMinute = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                            t1 = t1.substring(t1.indexOf(":") + 1, t1.length()).trim(); // 분
                            timeStartSecond = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                            t1 = t1.substring(t1.indexOf(":") + 1, t1.length()).trim(); // 초
                            timeStartMillisecond = Integer.parseInt(t1.substring(0, t1.length()).trim());
                            timeSUB = ((timeStartHour * 60 + timeStartMinute) * 60
                                    + timeStartSecond) * 1000 + timeStartMillisecond;

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
                        sizeCx = Integer.parseInt(s.substring(s.indexOf(":") + 1,
                                s.toLowerCase().indexOf("x")).trim());
                        sizeCy = Integer.parseInt(s.substring(s.toLowerCase().indexOf("x") + 1,
                                s.length()).trim());

                        pixels = new int [ sizeCx * sizeCy];        // 픽셀 버퍼를 준비함.

                    } else if (s.toLowerCase().contains("index:")) {
                        index = Integer.parseInt(s.substring(s.toLowerCase().lastIndexOf(":") + 1,
                                s.length()).trim());
                        fLanguageMatch = langIdx == index;

                    } else if (s.toLowerCase().contains("palette:")) {
                        s = s.substring(s.toLowerCase().indexOf("palette:") + 8, s.length()).trim();
                        s = s + ","; // for the consistency
                        for (int i = 0; i < 16; i++) {
                            palette[i] = (Integer.parseInt(s.substring(0, s.indexOf(",")).trim(), 16)) & 0x00ffffff;
                            s = s.substring(s.indexOf(",") + 1, s.length());
                        }

                    } else if (s.toLowerCase().contains("custom colors:")) {
                        String cColors = s.substring(s.indexOf("custom colors:") + 17,
                                s.indexOf("custom colors:") + 18).toLowerCase().trim();
                        customColors = !cColors.equals("f");        // ON : true, OFF : false
                        tridx = Integer.parseInt(s.substring(s.indexOf("tridx:") + 6,
                                s.indexOf(", colors:")).trim(), 16);
                        s = s.substring(s.indexOf(", colors:") + 9, s.length());
                        s = s + ","; // 일관성 유지를 위하여
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

            if (parsedGraphicSubtitle.size() <= 1) {
                useSub = false;     // 한줄자리 자막은 무시함. (구간을 구할 수 없어서)
            }
        }
    }

/*
    패킷 헤더 규칙(.sub 파일) :

    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 00 | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 0a | 0b | 0c | 0d | 0e | 0f | 주소
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 00 | 00 | 01 | ba |    |    |    |    |    |    |    |    |    |    | 00 | 00 | 데이터
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+

    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 1a | 1b | 1c | 1d | 1e | 1f | 주소
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 01 | bd |    |    |    |(*1)| pq |(*2)|    |    |    |    |    | wx | yz | WX | 데이터
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+

    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 | 29 | 2a | 2b | 2c | 2d | 2e | 2f | 주소
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    | YZ |    |    |    |    |    |    |    |    |    |    |    |    |    |    |    | 데이터
    +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+

    buf[0x00] 는 반드시 00 00 01 ba (고정값) 이어야 함.
    buf[0x0e] 는 반드시 00 00 01 bd (고정값) 이어야 함.
    (*1) buf[0x15] & 0x80 는 반드시 true 이어야 함.
    (*2) (buf[0x17] & 0xf0) 는 반드시 0x20 이어야 함.
    (buf[buf[0x16] + 0x17] & 0xe0) 는 반드시 0x20 이어야 함.
    (buf[buf[0x16] + 0x17] & 0x1f) 는 지원하는 언어값이 들어 있어야 함.

    buf[16] = [pq] = 통상 0x05 (최초 블럭은  0x08 가능, 이 경우 옵션 정보가 3 바이트 더 있음)
    packet size = (buf[buf[0x16] + 0x18] << 8) + buf[buf[0x16] + 0x19] = 통상 wxyz(16진수값)
    data size = (buf[buf[0x16] + 0x1a] << 8) + buf[buf[0x16] + 0x1b] = 통상 WXYZ(16진수값)

    예를 들어,
    buf[0x16] = 05(pq), buf[0x1d] = 07(wx), buf[0x1e] = 9a(yz), buf[0x1f] = 07(WX), buf[0x20] = 7b(YZ)
    라고 하면, 패킷 크기 = 0x079a 바이트, 데이터 크기 = 0x077b 바이트.
    따라서, 정보 길이 = 패킷 크기 - 데이터 크기 = 0x079a - 0x077b = 0x1f 바이트.

    또 다른 예를 들면,
    buf[0x16] = 08(pq), buf[0x20] = 0b(wx), buf[0x21] = 70(yz), buf[0x22] = 0b(WX), buf[0x23] = 51(YZ)
    라고 하면, 패킷 크기 = 0x0b70 바이트, 데이터 크기 = 0x0b51 바이트
    따라서, 정보 길이 = 패킷 크기 - 데이터 크기 = 0x0b70 - 0x0b51 = 0x1f 바이트.
*/

    private int subIDXpointer;
    private byte [] buf = new byte[0x0800];           // 1 패킷 유닛
    private byte [] pData = new byte[BUF_LENGTH];     // 데이터 버퍼

    private int packetSize = 0;         // 패킷 크기 > 데이터 크기 (반드시 유지해야 함)
    private int dataSize = 0;
    private int savedSize = 0;

    final int IDmarkA = 0x00;           // 구분자 A = 00 00 01 ba
    final int IDmarkB = 0x0e;           // 구분자 B = 00 00 01 bd
    final int PESmark = 0x15;           // 7 번째 비트
    final int OPTIONS = 0x16;           // 옵션 바이트 길이
    final int ID_LANG = 0x17;           // ID 및 언어 코드
    final int PACKET_SIZE = 0x18;       // 패킷 크기
    final int DATA_SIZE = 0x1a;         // 데이터 크기

    // SUB 자막의 비트맵을 생성하여 돌려줌.
    private Bitmap getBitmapSubtitle(int filePos) {
        Bitmap graphic;
        subIDXpointer = filePos;     // 파일 위치 저장

        // .sub 데이터는 subtitleFile[0] 에서 subtitleFile[subtitleFile.length] 에 담겨 있음.
        System.arraycopy(subtitleFile, subIDXpointer, buf, 0, buf.length);
        subIDXpointer = subIDXpointer + buf.length;

        // .sub 파일 규칙 검사:
        if ((unsigned(buf[IDmarkA]) != 0x00) || (unsigned(buf[IDmarkA + 1]) != 0x00) ||
                (unsigned(buf[IDmarkA + 2]) != 0x01) || (unsigned(buf[IDmarkA + 3]) != 0xba)) {
            return null; }
        if ((unsigned(buf[IDmarkB]) != 0x00) || (unsigned(buf[IDmarkB + 1]) != 0x00) ||
                (unsigned(buf[IDmarkB + 2]) != 0x01) || (unsigned(buf[IDmarkB + 3]) != 0xbd)) {
            return null; }
        if ((unsigned(buf[PESmark]) & 0x80) == 0x00) { return null; }
        if ((unsigned(buf[ID_LANG]) & 0xf0) != 0x20) { return null; }
        // 상위 3 비트 = 인덱스 (0x20 => 0x01)
        if (((unsigned(buf[unsigned(buf[OPTIONS]) + ID_LANG])) & 0xe0) != 0x20) { return null; }
        // 하위 5 비트 = 언어
        if ((unsigned(buf[unsigned(buf[OPTIONS]) + ID_LANG]) & 0x1f) != langIdx) { return null; }

        // 패킷 헤더 정보를 읽음.
        packetSize = ((unsigned(buf[unsigned(buf[OPTIONS]) + PACKET_SIZE])) << 8) +
                unsigned(buf[unsigned(buf[OPTIONS]) + PACKET_SIZE + 1]);
        dataSize = ((unsigned(buf[unsigned(buf[OPTIONS]) + DATA_SIZE])) << 8) +
                unsigned(buf[unsigned(buf[OPTIONS]) + DATA_SIZE + 1]);

        if (packetSize > BUF_LENGTH) { return null; }

        // 버퍼 내용에서 불필요한 부분을 지우고 축약함.
        condenseBuffer();

        if (packetSize > savedSize) {
            savedSize = packetSize;         // 얼마나 많은 데이터가 오는지 확인하기 위한 코드.
        }

        // BYTE 어레이를 INT 어레이로 복사하는 메소드.
//        IntBuffer intBuf = ByteBuffer.wrap(nbuf).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
//        int [] ibuf = new int [intBuf.remaining()];
//        intBuf.get(ibuf);

        getPacketInfo();        // 패킷 정보를 얻음.

        getBitmapData();        // 자막의 비트맵 데이터를 생성함.

        // 비트맵을 2배로 확대하여 출력함.
        graphic = Bitmap.createBitmap(pixels, 0, sizeCx, sizeCx, sizeCy, Bitmap.Config.ARGB_8888);
        return Bitmap.createScaledBitmap(graphic, sizeCx * 2, sizeCy * 2, true);
    }

/*
    버퍼를 축약하는 방법:

    버퍼는 4 블럭이 준비되어 있고 한 버퍼는 0x0800 (2,048) 바이트이다.
    각 버퍼는 패킷 헤더를 가지고 있고 그 길이는 가변이다.
    만약 데이터가 2 블럭 이상 오면 최소한 두 번째 블럭의 헤더는 제거해야 한다.
    두 번째 블럭의 패킷 헤더 정보는 통상 0x18 바이트이다.
    만약 데이터가 4 블럭(0x0800 * 4 바이트)을 초과한다면, 표시 시간의 제약으로 인해
    그 자막은 무시하는 것이 바람직하다.

    buf[0]
    +-----------------------+       +-----------------------+
    |   첫번째 패킷 헤더    |       | 패킷크기 및 데이터크기| <-- 4 바이트
    +-----------------------+       +-----------------------+
    |                       |       |                       |
    |   첫번째 데이터블럭   |       |   첫번재 데이터블럭   | <-- 0x0800 - hsize 바이트
    |                       |       |                       |
    +-----------------------+       +-----------------------+
    |   두번째 패킷 헤더    |       |                       |
    +-----------------------+       |   두번째 데이터블럭   | <-- 0x0800 - (hsize=0x18 + buf[0x16]) 바이트
    |                       |  ==>  |                       |
    |   두번째 데이터블럭   |       +-----------------------+
    |                       |       +-----------------------+
    +-----------------------+       |                       |
    +-----------------------+       |   N번째 데이터블럭    |
    |   N번째 패킷 헤더     |       |                       |
    +-----------------------+       +-----------------------+
    |                       |
    |   N번째 데이터 블럭   |
    |                       |
    +-----------------------+


예를 들면 : 데이터 길이 = 0x0b26 라고 하면
    +-----------------------+       +-----------------------+
    |    헤더 0x1d 바이트   |       |    헤더 0x18 바이트   |
    +-----------------------+       +-----------------------+
    |                       |       |                       |
    |    첫번째 데이터블럭  |   +   |    두번째 데이터블럭  |
    |    0x07e3 바이트      |       |         X 바이트      |  X = 0x0b26 - 0x0800 + 0x1d + 0x18
    |                       |       |                       |    = 0x035b 바이트
    +-----------------------+       +-----------------------+
           첫번째 패킷                      두번째 패킷
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

    private boolean fForced = true;
    private int t;
    private int pal;
    private int tr;
    private int delay;
    private Rect rect = new Rect(0,0,0,0);      // rect = (left, top, right, bottom)

    private int [] nOffset = new int [2];       // nOffset[0] = nPlane 0, 인터레이스 모드 짝수라인,
                                                // nOffset[1] = nPlane 0, 인터레이스 모드 홀수라인.
    private int [] palPal = new int [4];        // 새로운 파렛트, 무시함.
    private int [] palTr = new int [4];

    private int dataIndex;
    private int nextCtrlBlk;                    // 다음 컨트롤 블럭 시작 주소, 무시함.
    private boolean fBreak;

    final int FORCED_START_DISPLAY = 0; // 강제 표시 시작
    final int START_DISPLAY = 1;        // 표시 시작 (맨 끝에 이 명령어가 온다)
    final int STOP_DISPLAY = 2;         // 표시 종료
    final int GET_PALETTE = 3;          // 다음 2 바이트가 파렛트 데이터
    final int GET_TRIDX = 4;            // 다음 2 바이트가 IDX
    final int GET_RECTANGLE = 5;        // 다음 6 바이트가 (x1, y1, x2, y2) = (left, top, right, bottom)
    final int GET_PLANE_OFFSET = 6;     // 다음 4 바이트가 (plane 0 : 2 바이트, plane 1 : 2 바이트) - 인터레이스 모드
    final int END_OF_CONTROL_BLOCK = 255;// 컨트롤 블럭의 끝, 다른 코드는 무시함.

    private void getPacketInfo() {
        dataIndex = dataSize;       // 패킷이 시작될 때 t(2 바이트) 와 다음 콘트롤 블럭(2 바이트)이 옴.

        t = ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]));
        dataIndex = dataIndex + 2;

        nextCtrlBlk = ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]));
        dataIndex = dataIndex + 2;

        // 반드시 dataSize < nextCtrlBlk < packetSize 이어야 함.
        fBreak = false;

        do {
            switch (unsigned(pData[dataIndex])) {
                case FORCED_START_DISPLAY:      // 강제 표시 시작
                    dataIndex++;
                    fForced = true;
                    break;

                case START_DISPLAY:             // 표시 시작
                    dataIndex++;
                    fForced = false;
                    break;

                case STOP_DISPLAY:              // 표시 종료
                    dataIndex++;
                    delay = 1024 * t / 90;
                    break;

                case GET_PALETTE:               // 파렛트 데이터
                    dataIndex++;
                    pal = ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]));
                    dataIndex = dataIndex + 2;
                    break;

                case GET_TRIDX:                 // tridx 데이터
                    dataIndex++;
                    if ((unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]) != 0) {
                        tr = (unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]);
                    }
                    dataIndex = dataIndex + 2;
                    break;

                case GET_RECTANGLE:             // rectangle (표시할 4각형 영역) 데이터
                    dataIndex++;
                    int left = (unsigned(pData[dataIndex]) << 4) +
                            (unsigned(pData[dataIndex + 1]) >> 4);
                    int top = (unsigned(pData[dataIndex + 3]) << 4) +
                            (unsigned(pData[dataIndex + 4]) >> 4);
                    int right = ((unsigned(pData[dataIndex + 1]) & 0x0f) << 8) +
                            (unsigned(pData[dataIndex + 2]) + 1);
                    int bottom = ((unsigned(pData[dataIndex + 4]) & 0x0f) << 8) +
                            (unsigned(pData[dataIndex + 5]) + 1);
                    rect = new Rect(left, top, right, bottom);
                    dataIndex = dataIndex + 6;

                case GET_PLANE_OFFSET:      // (plane 0) 와 (plane 1) 의 시작 위치
                    dataIndex++;
                    nOffset[0] = (unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]);
                    dataIndex = dataIndex + 2;

                    nOffset[1] = (unsigned(pData[dataIndex]) << 8) + unsigned(pData[dataIndex + 1]);
                    dataIndex = dataIndex + 2;
                    break;

                case END_OF_CONTROL_BLOCK:      // 컨트롤 블럭의 끝
                    dataIndex++;
                    fBreak = true;
                    continue;

                default:                        // 다른 명령어는 무시함.
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
    // |       패킷 헤더       | (패킷 헤더 길이 = hsize)
    // +-----------------------+ <-----+ <-----+ buf[hsize]
    // |                       |       |       |
    // |                       |       |       |
    // |      자막 데이터      |       | 데이터|
    // |                       |       | 크기  | 패킷
    // |                       |       |       | 크기
    // |                       |       |       |
    // +-----------------------+ <-----+       |
    // |         정보          |               |   (정보 길이 = 패킷 크기 - 데이터 크기)
    // +-----------------------+         <-----+

    private int x;
    private int y;
    private int [] pixels;

    private int dataPointer;

    private int nPlane;             // 첫 라인부터(인터레이스 모드의 상위 라인)
    private int fAligned = 1;       // 상위 4 비트 부터

    private int end0;
    private int end1;

    private void getBitmapData() {
        dataPointer = dataSize;

        nPlane = 0;         // 첫 라인부터
        fAligned = 1;       // 상위 4비트 부터

        end0 = nOffset[1];
        end1 = dataPointer;

        if (nOffset[0] > nOffset[1]) {
            end1 = nOffset[0];
            end0 = dataPointer;
        }

        x = rect.left;
        y = rect.top;

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

            if (fAligned == 0) { getNibble(); }        // 바이트로 맞추기 위해서

            x = rect.left;              // x 좌표 초기화
            y++;
            nPlane = 1 - nPlane;        // 두번째 라인으로
        }

        rect.bottom = Math.min(y, rect.bottom);
//        trimSubImage();               // 자막 비트맵 다듬기
    }

    // alpha == 0xff 라면, 불투명을 의미함(흰색)
    //
    // 참고로,
    // 투명 흰색(0x00ffffff) = 투명 회색(0x00808080) = 투명 검정색(0x00000000)
    // 불투명 흰색 = 0xffffffff, 불투명 회색 = 0xff808080, 불투명 검정색 = 0xff000000
    //
    // 회색을 0x00808080 로 한다면, 회색은 더 이상 표시되지 않는다 ! (즉, 흑/백 전용)
    // 회색을 0xffffffff 로 한다면, 회색은 흰색으로 표시된다 ! (즉, 역시 흑/백)

    private int tBLACK = 0x00000000 & 0xffffffff;       // 투명 검정색
    private int nBLACK = 0xff000000 & 0xffffffff;       // 불투명 검정색
    private int tGREY = 0x00808080 & 0xffffffff;        // 투명 회색
    private int nGREY = 0xff808080 & 0xffffffff;        // 불투명 회색
    private int tWHITE = 0x00ffffff & 0xffffffff;       // 투명 흰색
    private int nWHITE = 0xffffffff & 0xffffffff;       // 불투명 흰색

    // int [] myColor = {tWHITE, nWHITE, nGREY, nBLACK};         // 원래 색상
    // int [] myColor = {tBLACK, nWHITE, nBLACK, nBLACK};        // 변경한 색상
    // int [] myColor = {tBLACK, tWHITE, nWHITE, nWHITE};        // 반전된 색상
    private int [] myColor = {tBLACK, nWHITE, nBLACK, nBLACK};   // 안드로이드 폰을 위해 변경

    private int rgbReserved;        // 비트맵 다듬기에서 사용됨.
    private int ptr = 0;

    private void drawPixels(int length, int colorid) {
        if ((length <= 0) || (x + length < rect.left) ||
                (x >= rect.right) || (y < rect.top) || (y >= rect.bottom)) {
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
        // 원래 코드는 16 컬러를 지원하지만 안드로이드에서는 동작하지 않음.
//        if (!customColors) {
//            c = palette[palPal[colorid]];
//            rgbReserved = (palTr[colorid] << 4) | palTr[colorid];
//        } else {
//            c = color[colorid];
//        }

        c = myColor[colorid & 0x03];

        while (length-- > 0) {
            pixels[ptr] = c;            // 파렛트 데이터를 넣음
            ptr++;                      // 길이만큼
        }
    }

    // 비트맵 데이터를 다듬는 메소드.
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

    // 오프셋(offset)은 0.5 바이트(4비트) 단위로 움직임.
    private int getNibble() {
        offset = nOffset[nPlane];
        int result = (unsigned(pData[offset]) >> (fAligned << 2)) & 0x0f;

        if (fAligned == 1) {
            fAligned = 0;
        } else if (fAligned == 0) {
            fAligned = 1;
        }

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

    // get(index) <= playTime < get(index+1) 이면, 인덱스를 리턴함
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
            if((parsedTextSubtitle.get(indexPointerT).getTime() <= playTime) &&
                    (playTime < parsedTextSubtitle.get(indexPointerT + 1).getTime())) {
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
                    mIV_subtitle.setImageBitmap(getBitmapSubtitle(
                            parsedGraphicSubtitle.get(countSub).getFilepos()));
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
            if((parsedGraphicSubtitle.get(indexPointerG).getTime() <= playTime) &&
                    (playTime < parsedGraphicSubtitle.get(indexPointerG + 1).getTime())) {
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

    // 자막 데이터 끝에 한 라인을 추가함. (안정성을 위해)
    private void addOneMoreLine() {
        if (useSmi || useSrt || useAss || useSsa) {
            int h = parsedTextSubtitle.size() - 1;
            maxRunningTime = parsedTextSubtitle.get(h).getTime();
            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(maxRunningTime + 500, getString(R.string.msg_end)));
        } else if (useSub) {
            int h = parsedGraphicSubtitle.size() - 1;
            maxRunningTime = parsedGraphicSubtitle.get(h).getTime();
            parsedGraphicSubtitle.add(new VideoPlayerGraphicSubtitle(maxRunningTime + 500, 0));
        }
    }

    // 자막 데이터에 사용된 유니코드를 분석하여 리턴함. (대부분의 언어 지원)
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
            encoding = ENCODING;        // 분석 불가, 기본 언어 세팅
        }

        detector.reset();
        return encoding;
    }

    // 바이트 데이터를 정수로 변환할 경우 java가 부호붙은 정수로 인식하기 때문에
    // 이를 부호없는 정수로 바꾸어 주어야 한다.
    // 이를 위해 (& 0xff) 를 행한다.
    //
    // (byte [])  0000 0000 0000 0000 0000 0000 1111 1111 (255)
    //    int     1111 1111 1111 1111 1111 1111 1111 1111 (-1)
    //  (& 0xff)  0000 0000 0000 0000 0000 0000 1111 1111 (255)
    private int unsigned(byte chr) {
        return (int) (chr  & 0xff);
    }




//    // 추후 FFmpeg 을 지원하기 위한 wrapper 예제.
//    private static native void openFile();
//    private static native void drawFrame(Bitmap bitmap);
//    private static native void drawFrameAt(Bitmap bitmap, int secs);
//
//    private Bitmap mBitmap;
//    private int mSecs = 0;
//
//    static {
//        System.loadLibrary("ffmpegutils");
//    }
//
//    private void mpeg() {
//        mBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888);
//        openFile();
//        drawFrame(mBitmap);
//        mIV_subtitle.setImageBitmap(mBitmap);
//    }

}
