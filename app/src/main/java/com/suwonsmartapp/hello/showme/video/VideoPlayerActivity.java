package com.suwonsmartapp.hello.showme.video;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class VideoPlayerActivity extends Activity implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }
    private static final String ENCODING = "EUC-KR";
    private static final int BUF_LENGTH = 256 * 16 * 2;

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
    private long savedSize = 0L;
    private boolean stopFlag = false;

    private FileInputStream accessFile;
    private byte[] buf = new byte[BUF_LENGTH];    // buffer for sub data reading, minimum 0x2000
    private int numberOfRead;
    private int currentFilePointer = 0;

    private int packetSize = 0;
    private int dataSize = 0;
    private int hsize = 0;
    private int ptr = 0;
    private int nLang = 0;
    private int savedDataSize = 0;
    private int sizeCx = 0;
    private int sizeCy = 0;

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
        mVV_show.seekTo(0);
        mVV_show.start();                   // auto start

        // test code
//        countSub = getSubSyncIndexGraphic(0);
//        mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));
//
//        countSub = getSubSyncIndexGraphic(1);
//        mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));
//
//        countSub = getSubSyncIndexGraphic(2);
//        mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));
//
//        countSub = getSubSyncIndexGraphic(3);
//        mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));
//
//        countSub = getSubSyncIndexGraphic(4);
//        mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));



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

    private void setupSUB() {
        if (useSub) {
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(idxFile.toString())), detectEncoding(idxFile.toString())));
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                while (((s = in.readLine()) != null) || (!stopFlag)) {
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
                            savedTimeSub = timeSUB;
                            stopFlag = false;
                        } else if (timeSUB > savedTimeSub) {
                            parsedGraphicSubtitle.add(new VideoPlayerGraphicSubtitle(timeSUB, filePOS));
                            savedTimeSub = timeSUB;
                        } else {
                            stopFlag = true;
                        }
                    } else if (s.toLowerCase().contains("size:")) {
                        sizeCx = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.toLowerCase().indexOf("x")).trim());
                        sizeCy = Integer.parseInt(s.substring(s.toLowerCase().indexOf("x") + 1, s.length()).trim());
                        showLog("screen size (x, y) = (" + sizeCx + ", " + sizeCy + ")");
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

            if (parsedGraphicSubtitle.size() <= 1) {
                useSub = false;     // if we have just one line, ignore this subtitle
            }
        }
    }

    // subtitle file is (File) subFile
    // purpose is to make bitmap file and put it into image view.

    private Bitmap getBitmapSubtitle(long filePos) {
        try {
            accessFile = new FileInputStream(new File(subFile.toString()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            accessFile.skip(filePos);
            numberOfRead = accessFile.read(buf, 0, buf.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            accessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // buf[0x00] should be 00 00 01 ba
        // buf[0x0e] should be 00 00 01 bd
        // buf[0x15] & 0x80 should be true
        // (buf[0x17] & 0xf0) should be 0x20
        // (buf[buf[0x16] + 0x17] & 0xe0) should be 0x20
        // (buf[buf[0x16] + 0x17] & 0x1f) should be supported number of Language

        // packetSize 와 0x800 - hsize 중에서 적은 것이 size
        // buf[hsize] 부터 size 만큼 copy
        // buf[buf[0x16] + 0x17] == (nLang | 0x20) 이면 stop

        int packetSize = (buf[buf[0x16] + 0x18] << 8) + buf[buf[0x16] + 0x19];
        int dataSize = (buf[buf[0x16] + 0x1a] << 8) + buf[buf[0x16] + 0x1b];
        int hsize = 0x18 + buf[0x16];
        int ptr = buf[hsize];
        int nLang = buf[buf[0x16] + 0x17] & 0x1f;

//        showToast("packet size : " + packetSize);
//        showToast("data size : " + dataSize);
//        showToast("hsize : " + hsize);
//        showToast("ptr : " + ptr);
//        showToast("nLang : " + nLang);

        byte [] nbuf = new byte[BUF_LENGTH / 2];
        System.arraycopy(buf, hsize + 6, nbuf, 0, packetSize);

        IntBuffer intBuf = ByteBuffer.wrap(nbuf).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        int [] ibuf = new int [intBuf.remaining()];
        intBuf.get(ibuf);

        graphic = Bitmap.createBitmap(ibuf, 0, sizeCx, sizeCx, sizeCy, Bitmap.Config.ARGB_8888);
//        graphic = Bitmap.createBitmap(ibuf, 0, sizeCx, sizeCx, sizeCy, Bitmap.Config.RGB_565);
        return graphic;
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
                mIV_subtitle.setImageBitmap(getBitmapSubtitle(parsedGraphicSubtitle.get(countSub).getFilepos()));
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
