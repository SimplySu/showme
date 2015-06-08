package com.suwonsmartapp.hello.showme.video;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
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
import java.util.ArrayList;

public class VideoPlayerActivity extends Activity implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private static final String ENCODING = "EUC-KR";    // default encoding method
    private static final int BUF_LENGTH = 256 * 8 * 3;

    private int mCurrentPosition;                   // current playing pointer
    private ArrayList<VideoFileInfo> mVideoFileInfoList;    // video file media_player_icon_information list
    private VideoFileInfo videoFileInfo;                    // video file info getting by cursor
    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent
    private String fullPathname = "";              // full path + filename

    private String subPathname = "";                // sub file pathname
    private File subFile;                           // sub file
    private String subFilename = "";                // sub filename string
    private String idxPathname = "";                // idx file pathname
    private File idxFile;                           // idx file
    private boolean useSmi = false;                 // true if we will use smi file
    private boolean useSrt = false;                 // true if we will use srt file
    private boolean useAss = false;                 // true if we will use ass file
    private boolean useSsa = false;                 // true if we will use ssa file
    private boolean useSub = false;                 // true if we will use idx/sub file

    private BufferedReader in;
    private String s;
    private String text = null;
    private Bitmap graphic;
    Canvas canvas;
    Paint p;
    private int rgbReserved;

    private ArrayList<VideoPlayerTextSubtitle> parsedTextSubtitle;
    private ArrayList<VideoPlayerGraphicSubtitle> parsedGraphicSubtitle;
    private long timeSUB = -1;
    private long savedTimeSub = -1;
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
    private int vob_ID;
    private int cell_ID;
    private int pts;
    private long filePOS;
    private int savedSize = 0;
    private boolean stopFlag = false;
    private int savedIndex;

    private FileInputStream accessFile;
    private byte[] buf = new byte[BUF_LENGTH];    // buffer for sub data reading, minimum 0x1800
    private int[] palette = new int[16];            // save palette informatin on .idx file
    private int tridx;
    private boolean customColors;
    private int [] color = new int [4];
    private int pixel;
    private int numberOfRead;
    private int currentFilePointer = 0;

    private int packetSize = 0;
    private int dataSize = 0;
    private int dataPointer = 0;
    private int hsize = 0;
    private int ptr = 0;
    private int nLang = 0;
    private int savedDataSize = 0;
    private int sizeCx = 0;
    private int sizeCy = 0;
    private int index = 0;

    private boolean fForced = true;
    private int delay = 0;
    private int t = 0;
    private boolean fBreak = false;
    private int nextCtrlBlk;
    private int pal;
    private int tr;
    private Rect rect = new Rect(0,0,0,0);
    private int dataIndex;
    private int [] nOffset = new int [2];
    private int [] palPal = new int [4];
    private int [] palTr = new int [4];
    private int x;
    private int y;
    private int end0;
    private int end1;
    private int nPlane = 0;
    private int fAligned = 1;
    private int offset;

    private VideoView mVV_show;                     // video screen
    private TextView mVV_subtitle;                  // text view subtitle
    private ImageView mIV_subtitle;                 // image view subtitle

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
        mIV_subtitle = (ImageView)findViewById(R.id.iv_subtitle);
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

        if ((!useSmi) && (!useSrt) && (!useAss)) {
            idxPathname = fullPathname.substring(0, fullPathname.lastIndexOf(".")) + ".idx";
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
            parsedTextSubtitle = new ArrayList<>();
        }

        if (useSub) {
            parsedGraphicSubtitle = new ArrayList<>();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        int [] timearray = new int [] {71304, 73139, 77177, 77944, 79679, 81414};
        // create bitmap testing code
        for (int i : timearray) {
            countSub = getSubSyncIndexGraphic(i);
//            mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));
            getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos());
        }

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
                            Thread.sleep(300);
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
                            Thread.sleep(500);
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
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (parsedTextSubtitle.size() <= 1) {
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
                            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(timeSUBstart, text));
                        }
                        t1 = s.substring(0, s.lastIndexOf(" --> "));
                        timeStartHour = Integer.parseInt(t1.substring(0, t1.indexOf(':')).trim());
                        timeStartMinute = Integer.parseInt(t1.substring(t1.indexOf(':') + 1, t1.lastIndexOf(':')).trim());
                        timeStartSecond = Integer.parseInt(t1.substring(t1.lastIndexOf(':') + 1, t1.indexOf(',')).trim());
                        timeStartMillisecond = Integer.parseInt(t1.substring(t1.lastIndexOf(',') + 1, t1.length()).trim());
                        timeSUBstart = ((timeStartHour * 60 + timeStartMinute) * 60 + timeStartSecond) * 1000 + timeStartMillisecond;

                        t2 = s.substring(s.lastIndexOf(" --> ") + 5, s.length());
                        timeEndHour = Integer.parseInt(t2.substring(0, t2.indexOf(':')).trim());
                        timeEndMinute = Integer.parseInt(t2.substring(t2.indexOf(':') + 1, t2.lastIndexOf(':')).trim());
                        timeEndSecond = Integer.parseInt(t2.substring(t2.lastIndexOf(':') + 1, t2.indexOf(',')).trim());
                        timeEndMillisecond = Integer.parseInt(t2.substring(t2.lastIndexOf(',') + 1, t2.length()).trim());
                        timeSUBend = ((timeEndHour * 60 + timeEndMinute) * 60 + timeEndSecond) * 1000 + timeEndMillisecond;

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
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (parsedTextSubtitle.size() <= 1) {
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
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

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

    private void setupSUB() {
        if (useSub) {
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(idxFile.toString())), detectEncoding(idxFile.toString())));
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                while (((s = in.readLine()) != null) && (stopFlag == false) && (index == 0)) {
                    if (s.contains("# Vob/Cell ID: ")) {
                        vob_ID = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.indexOf(",")).trim());

                        s = s.substring(s.indexOf(",") + 1, s.length());    // git rid of vob_ID portion
                        cell_ID = Integer.parseInt(s.substring(0, s.indexOf("(")).trim());

                        pts = Integer.parseInt(s.substring(s.indexOf("PTS:") + 4, s.lastIndexOf(")")).trim());

                    } else if (s.toLowerCase().contains("timestamp:")) {

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

                        filePOS = Long.parseLong(t2.trim(), 16);

                        if (savedTimeSub == -1) {
                            parsedGraphicSubtitle.add(new VideoPlayerGraphicSubtitle(timeSUB, filePOS));
                            showLog("index : " + index + ", time stamp : " + timeSUB + ", file pos : " + filePOS);
                            savedTimeSub = timeSUB;
                            stopFlag = false;
                        } else if (timeSUB > savedTimeSub) {
                            parsedGraphicSubtitle.add(new VideoPlayerGraphicSubtitle(timeSUB, filePOS));
//                            showLog("index : " + index + ", time stamp : " + timeSUB + ", file pos : " + filePOS);
                            savedTimeSub = timeSUB;
                        } else {
                            stopFlag = true;
                        }

                    } else if (s.toLowerCase().contains("size:")) {
                        sizeCx = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.toLowerCase().indexOf("x")).trim());
                        sizeCy = Integer.parseInt(s.substring(s.toLowerCase().indexOf("x") + 1, s.length()).trim());
//                        showLog("screen size (x, y) = (" + sizeCx + ", " + sizeCy + ")");

                    } else if (s.toLowerCase().contains("index:")) {
                        index = Integer.parseInt(s.substring(s.toLowerCase().lastIndexOf(":") + 1, s.length()).trim());
//                        showLog("index : " + index);

                    } else if (s.toLowerCase().contains("palette:")) {
                        s = s.substring(s.toLowerCase().indexOf("palette:") + 8, s.length()).trim();
                        s = s + ","; // for the consistency
                        for (int i = 0; i < 16; i++) {
                            palette[i] = Integer.parseInt(s.substring(0, s.indexOf(",")).trim(), 16);
                            s = s.substring(s.indexOf(",") + 1, s.length());
                        }

                    } else if (s.toLowerCase().contains("custom colors:")) {
                        String cColors = s.substring(s.indexOf("custom colors:") + 17, s.indexOf("custom colors:") + 18).toLowerCase().trim();
                        customColors = true;        // ON
                        if (cColors.equals("f")) {
                            customColors = false;   // OFF
                        }
                        tridx = Integer.parseInt(s.substring(s.indexOf("tridx:") + 6, s.indexOf(", colors:")).trim(), 16);
                        s = s.substring(s.indexOf(", colors:") + 9, s.length());
                        s = s + ","; // for the consistency
                        for (int i = 0; i < 4; i++) {
                            color[i] = Integer.parseInt(s.substring(0, s.indexOf(",")).trim(), 16);
                            s = s.substring(s.indexOf(",") + 1, s.length());
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

            try {
                accessFile = new FileInputStream(new File(subFile.toString()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            showLog("saved " + parsedGraphicSubtitle.size() + " subtitles");
            if (parsedGraphicSubtitle.size() <= 1) {
                useSub = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

    // packet header rule (.sub file) :
    //
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    // | 00 | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 0a | 0b | 0c | 0d | 0e | 0f | address
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    // | 00 | 00 | 01 | ba |    |    |    |    |    |    |    |    |    |    | 00 | 00 | data
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    //
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    // | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 1a | 1b | 1c | 1d | 1e | 1f | address
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    // | 01 | bd |    |    |    |(*1)| pq |(*2)|    |    |    |    |    | wx | yz | WX | data
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    //
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    // | 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 | 29 | 2a | 2b | 2c | 2d | 2e | 2f | address
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    // | YZ |    |    |    |    |    |    |    |    |    |    |    |    |    |    |    | data
    // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
    //
    // buf[0x00] should be 00 00 01 ba (fixed value)
    // buf[0x0e] should be 00 00 01 bd (fixed value)
    // (*1) buf[0x15] & 0x80 should be true
    // (*2) (buf[0x17] & 0xf0) should be 0x20
    // (buf[buf[0x16] + 0x17] & 0xe0) should be 0x20
    // (buf[buf[0x16] + 0x17] & 0x1f) should be supported number of Language
    //
    // buf[16] = [pq] = normally 0x05 (very first block can contains 0x08, it means there are optional information 3 bytes)
    // packet size = (buf[buf[0x16] + 0x18] << 8) + buf[buf[0x16] + 0x19] = normally wxyz(hexa)
    // data size = (buf[buf[0x16] + 0x1a] << 8) + buf[buf[0x16] + 0x1b] = normally WXYZ(hexa)
    //
    // for example,
    // buf[0x16] = 05(pq), buf[0x1d] = 07(wx), buf[0x1e] = 9a(yz), buf[0x1f] = 07(WX), buf[0x20] = 7b(YZ)
    // then, packet size = 0x079a bytes, data size = 0x077b bytes
    // thus, information length = packet size - data size = 0x079a - 0x077b = 0x1f bytes

    // for example,
    // buf[0x16] = 08(pq), buf[0x20] = 0b(wx), buf[0x21] = 70(yz), buf[0x22] = 0b(WX), buf[0x23] = 51(YZ)
    // then, packet size = 0x0b70 bytes, data size = 0x0b51 bytes
    // thus, information length = packet size - data size = 0x0b70 - 0x0b51 = 0x1f bytes

    private boolean getBitmapSubtitle(long filePos) {
        try {
            accessFile.skip(filePos);
            numberOfRead = accessFile.read(buf, 0, buf.length);
//            showLog("read " + numberOfRead + " bytes for filePOS : " + filePos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ((buf[0x00] != 0) || (buf[0x01] != 0) || (buf[0x02] != 1) || (buf[0x03] != -70)) { return false; }
        if ((buf[0x0e] != 0) || (buf[0x0f] != 0) || (buf[0x10] != 1) || (buf[0x11] != -67)) { return false; }
        if (buf[0x16] == 0) { return false; }

        packetSize = (buf[buf[0x16] + 0x18] << 8) + buf[buf[0x16] + 0x19];
        dataSize = (buf[buf[0x16] + 0x1a] << 8) + buf[buf[0x16] + 0x1b];
        hsize = 0x18 + buf[0x16];
        if ((buf[0x15] & 0x80) != 0x00) {
            hsize = hsize + 4;
        }
        ptr = buf[hsize];
        nLang = buf[buf[0x16] + 0x17] & 0x1f;

        // if packetSize exceeds 0x0800 bytes, we should squeeze information area.
//        System.arraycopy(buf, 0x0800 + 0x18, buf, 0x0800, packetSize - 0x0800 + hsize);
//        System.arraycopy(buf, hsize, buf, 0, packetSize);

        if (packetSize > savedSize) {
            savedSize = packetSize;         // we want to see how much data comming
            showLog("packet size = " + packetSize);
        }

        if (packetSize > BUF_LENGTH) {
//            showLog("file pos : " + filePos);
//            showLog("data : " + buf[0x16] + " " + buf[0x18] + " " + buf[0x19] + " " + buf[0x1a] + " " + buf[0x1b]);
            return false;
        }

        // packet data = from buf[hsize] length packetSize
        condenseBuffer();

        // copy BYTE array into INT array
//        IntBuffer intBuf = ByteBuffer.wrap(nbuf).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
//        int [] ibuf = new int [intBuf.remaining()];
//        intBuf.get(ibuf);

        Canvas canvas = new Canvas();

        getPacketInfo();        // collect rendering information

        getBitmapData();        // collect bitmap data from subtitle packet

//        graphic = Bitmap.createBitmap(colorbuf, 0, rect.right, rect.right, rect.bottom, Bitmap.Config.ARGB_4444);
        return true;
    }

    // we have four blocks of buffer, one buffer contains 0x0800 bytes.
    // each buffer have packet header (length is vary)
    // if we treat two more blocks of data, we should get rid of this packet header from the second block.
    // packet header length of the second block to fourth block is 0x18 bytes.
    // if the packet exceeds four blocks (0x0800 * 4 bytes), we just ignore it because we can't display it by time limitation.

    // buf[0]
    // +-----------------------+       +-----------------------+
    // |   packet header 1     |       |   packet header 1     | <-- hsize bytes
    // +-----------------------+       +-----------------------+
    // |                       |       |                       |
    // |   data block 1        |       |   data block 1        | <-- 0x0800 - hsize bytes
    // |                       |       |                       |
    // +-----------------------+       +-----------------------+
    // |   packet header 2     |       |                       |
    // +-----------------------+       |   data block 2        | <-- 0x0800 - 0x18 bytes
    // |                       |  ==>  |                       |
    // |   data block 2        |       +-----------------------+
    // |                       |       |                       |
    // +-----------------------+       |   data block 3        |   maximum real data will be
    // |   packet header 3     |       |                       |   0x0800 * 3 - hsize - 0x18 * 2 bytes
    // +-----------------------+       +-----------------------+
    // |                       |
    // |   data block 3        |
    // |                       |
    // +-----------------------+

    private void condenseBuffer() {
        if (packetSize < (0x0800 - hsize)) {
            // done.
        } else if (packetSize < (0x1000 - (hsize + 0x18))) {
            // data continues to the second block
            for (int i = 0x0800 + 0x18, j = 0x0800; i < packetSize - 0x0800 + hsize + 0x18; i++, j++) {
                buf[j] = buf[i];
            }
        } else if (packetSize < (0x1800 - (hsize + 0x18 * 2))) {
            // data continues to the third block : if data burst, we will be dead.
            for (int i = 0x0800 + 0x18, j = 0x0800; i < 0x1000 - 0x18; i++, j++) {
                buf[j] = buf[i];
            }
            for (int i = 0x1000 + 0x18, j = 0x1000 - 0x18; i < packetSize - 0x1000 + hsize + 0x18 * 2; i++, j++) {
                buf[j] = buf[i];
            }
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

    private void getBitmapData() {
        dataPointer = dataSize + hsize - 4;

        int nPlane = 0;
        int fAligned = 1;

        int end0 = nOffset[1] + hsize - 4;
        int end1 = dataPointer;

        if (nOffset[0] > nOffset[1]) {
            end1 = nOffset[0] + hsize - 4;
            end0 = dataPointer;
        }

        x = rect.left;
        y = rect.top;
        offset = nOffset[nPlane] + hsize - 4;

        while ((((nPlane == 0) && (nOffset[0] + hsize - 4 < end0)) || ((nPlane == 1) && (nOffset[1] + hsize - 4 < end1)))) {

            int a = getNibble();
            int b = getNibble();
            int c = getNibble();
            int d = getNibble();

            int code = a;
            if (a >= 0x04) {
                drawPixels(code >> 2, code & 3);
            }

            code = code << 4;
            code = code | b;
            if (b >= 0x10) {
                drawPixels(code >> 2, code & 3);
            }

            code = code << 4;
            code = code | c;
            if (c >= 0x40) {
                drawPixels(code >> 2, code & 3);
            }

            code = code << 4;
            code = code | d;
            if (d >= 0x100) {
                drawPixels(code >> 2, code & 3);
            }

            drawPixels(rect.right - x, code & 3);

            x = rect.left;
            y++;
            nPlane = 1 - nPlane;

            nOffset[0] = nOffset[0] + 2;
            nOffset[1] = nOffset[1] + 2;
        }

        if (fAligned == 1) {
            getNibble();        // align to byte
        }

        rect.bottom = Math.min(y, rect.bottom);
    }

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

        int ptr = rect.width() * (y - rect.top) + (x - rect.left);

        int c;
        if (!customColors) {
            c = palette[palPal[colorid]];
            rgbReserved = (palTr[colorid] << 4) | palTr[colorid];
        } else {
            c = color[colorid];
        }

        while (length-- > 0) {
//            colorbuf[ptr] = c;            // delete temporarily
            ptr++;
        }
    }

//    private void trimSubImage() {
//        for (int j = 0, cy = rect.height(); j < cy; j++) {
//            for (int i = 0, cx = rect.width(); i < cx; i++, ptr++) {
//                if (rgbReserved != 0) {
//                    if (rect.top > j) {
//                        rect.top = j;
//                    }
//
//                    if (rect.bottom < j) {
//                        rect.bottom = j;
//                    }
//
//                    if (rect.left < i) {
//                        rect.left = i;
//                    }
//
//                    if (rect.right < i) {
//                        rect.right = i;
//                    }
//                }
//            }
//        }
//
//        if ((rect.left > rect.right) || (rect.top > rect.bottom)) {
//            return;
//        }
//
//        // code for PC
//
//    }

    private int getNibble() {
        int result = (buf[offset] >> (fAligned << 2)) & 0x0f;

        if (fAligned == 1) {
            fAligned = 0;
        } else if (fAligned == 0) {
            fAligned = 1;
        }

        offset = offset + fAligned;
        return result;
    }

    private void getPacketInfo() {
        dataIndex = dataSize + hsize - 4;
        t = ((buf[dataIndex] << 8) | buf[dataIndex + 1]);
//        showLog("t = " + buf[dataIndex] + ", " + buf[dataIndex + 1]);
        dataIndex = dataIndex + 2;

        nextCtrlBlk = ((buf[dataIndex] << 8) | buf[dataIndex + 1]);
//        showLog("nextCtrlBlk = " + buf[dataIndex] + ", " + buf[dataIndex + 1]);
        dataIndex = dataIndex + 2;

        // we should note that : dataSize < nextCtrlBlk < packetSize

        do {
//            showLog("parsing data = " + buf[dataIndex] + ", " + buf[dataIndex + 1] + ", " +
//                    buf[dataIndex + 2] + ", " + buf[dataIndex + 3] + ", " + buf[dataIndex + 4] +
//                    ", " + buf[dataIndex + 5] + ", " + buf[dataIndex + 6]);

            switch (buf[dataIndex++]) {
                case 0x00:      // forced start displaying
                    fForced = true;
                    break;

                case 0x01:      // start displaying
                    fForced = false;
                    break;

                case 0x02:      // stop displaying
                    delay = 1024 * t / 90;
                    break;

                case 0x03:      // get palette
                    pal = ((buf[dataIndex] << 8) | buf[dataIndex + 1]);
                    dataIndex = dataIndex + 2;
                    break;

                case 0x04:      // get tridx data
                    if ((buf[dataIndex] << 8 | buf[dataIndex + 1]) != 0) {
                        tr = ((buf[dataIndex] << 8) | buf[dataIndex + 1]);
                    }
                    dataIndex = dataIndex + 2;
                    break;

                case 0x05:      // get rectangle
                    int left = (buf[dataIndex] << 4) + (buf[dataIndex + 1] >> 4);
                    int top = (buf[dataIndex + 3] << 4) + (buf[dataIndex + 4] >> 4);
                    int right = ((buf[dataIndex + 1] & 0x0f) << 8) + (buf[dataIndex + 2] + 1);
                    int bottom = ((buf[dataIndex + 4] & 0x0f) << 8) + (buf[dataIndex + 5] + 1);
                    rect = new Rect(left, top, right, bottom);
                    dataIndex = dataIndex + 6;

                case 0x06:      // get offset
                    nOffset[0] = (buf[dataIndex] << 8) + (buf[dataIndex + 1]);
                    dataIndex = dataIndex + 2;
                    nOffset[1] = (buf[dataIndex] << 8) + (buf[dataIndex + 1]);
                    dataIndex = dataIndex + 2;
                    break;

                case (byte) 0xff:      // end of control block
                    fBreak = true;
                    continue;

                default:        // skip this control block
                    fBreak = true;
                    break;
            }
        } while (!fBreak);

        for (int i = 0; i < 4; i++) {
            palPal[i] = ((pal >> (i << 2)) & 0x0f);
            palTr[i] = ((tr >> (i << 2)) & 0x0f);
        }
    }

    Handler textHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mVV_show.getCurrentPosition() <= maxRunningTime) {
                countSub = getSubSyncIndexText(mVV_show.getCurrentPosition());
                mVV_subtitle.setText(Html.fromHtml(parsedTextSubtitle.get(countSub).getText()));
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
        int lowLimit = 0;
        int highLimit = parsedTextSubtitle.size();
        int indexPointer;

        while(lowLimit <= highLimit) {
            indexPointer = (lowLimit + highLimit) / 2;
            if((parsedTextSubtitle.get(indexPointer).getTime() <= playTime) && (playTime < parsedTextSubtitle.get(indexPointer + 1).getTime())) {
                return indexPointer;
            }
            if(playTime >= parsedTextSubtitle.get(indexPointer + 1).getTime()) {
                lowLimit = indexPointer + 1;
            } else {
                highLimit = indexPointer - 1;
            }
        }
        return 0;
    }

    Handler idxHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mVV_show.getCurrentPosition() <= maxRunningTime) {
                countSub = getSubSyncIndexGraphic(mVV_show.getCurrentPosition());
                mIV_subtitle.setImageBitmap(null);
                getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos());
//                mIV_subtitle.setImageBitmap(getBitmapSubtitle(recordPos));
//                showLog("current running time : " + countSub + "position : " + recordPos);
            }
        }
    };

    public int getSubSyncIndexGraphic(long playTime) {
        int lowLimit = 0;
        int highLimit = parsedGraphicSubtitle.size();
        int indexPointer;

        while(lowLimit <= highLimit) {
            indexPointer = (lowLimit + highLimit) / 2;
            if((parsedGraphicSubtitle.get(indexPointer).getTime() <= playTime) && (playTime < parsedGraphicSubtitle.get(indexPointer + 1).getTime())) {
                return indexPointer;
            }
            if(playTime >= parsedGraphicSubtitle.get(indexPointer + 1).getTime()) {
                lowLimit = indexPointer + 1;
            } else {
                highLimit = indexPointer - 1;
            }
        }
        return 0;
    }

    private String detectEncoding(String filename) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        CodeDetector detector = new CodeDetector(null);

        try {
            while ((numberOfRead = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, numberOfRead);
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
            int h = parsedTextSubtitle.size() - 1;
            maxRunningTime = parsedTextSubtitle.get(h).getTime();
            parsedTextSubtitle.add(new VideoPlayerTextSubtitle(maxRunningTime + 500, "The End"));
        }
    }
}
