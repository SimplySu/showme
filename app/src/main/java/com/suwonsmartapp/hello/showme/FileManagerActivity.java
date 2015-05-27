package com.suwonsmartapp.hello.showme;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class FileManagerActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener {

    private static final String TAG = FileManagerActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    public static final String sPathRoot = "/";
    public static final String sPathSdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String sPathMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
    public static final String sPathMovie = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
    public static final String sPathDCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
    public static final String sPathPicture = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    public static final String sPathDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();;
    public static final String sPathDocument = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();

    private ListView mListView;

    // we will use default simple adapter
    private ArrayList<Map<String, String>> mTitleList;
    private SimpleAdapter mAdapter;

    // history management : push current path (mCurrentPath) before going to the next screen
    // mCurrentPath = next screen path
    private Stack<String> mFileStack;

    // current full path
    private String mCurrentPath = "";

    private TextView mTvCurrentPath;

    private static final int ActivityForAudio = 0x0001;
    private static final int ActivityForVideo = 0x0010;
    private static final int ActivityForImage = 0x0100;
    private static final int ActivityForReserve = 0x1000;

    private String externalSdCard = null;
    private File fileCur = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mListView = (ListView) findViewById(R.id.lv_filetree);
        mTvCurrentPath = (TextView) findViewById(R.id.tv_currentPath);

        mFileStack = new Stack<>();

        setupHome();    // display categorized initial screen
    }

    private void setupHome() {
        findExtSd();    // setup path name on externalSdCard if secondary SD card exists

        Map<String, String> root = new HashMap<>();
        root.put("title", "루트");
        root.put("path", sPathRoot);

        Map<String, String> sdcard = new HashMap<>();
        sdcard.put("title", "SD Card");
        sdcard.put("path", sPathSdcard);

        Map<String, String> extsdcard = new HashMap<>();
        if (externalSdCard != null) {
            extsdcard.put("title", "확장 메모리");
            extsdcard.put("path", externalSdCard);
        }

        Map<String, String> music = new HashMap<>();
        music.put("title", "음악");
        music.put("path", sPathMusic);

        Map<String, String> movie = new HashMap<>();
        movie.put("title", "동영상");
        movie.put("path", sPathMovie);

        Map<String, String> dcim = new HashMap<>();
        dcim.put("title", "사진");
        dcim.put("path", sPathDCIM);

        Map<String, String> picture = new HashMap<>();
        picture.put("title", "그림");
        picture.put("path", sPathPicture);

        Map<String, String> download = new HashMap<>();
        download.put("title", "다운로드");
        download.put("path", sPathDownload);

        Map<String, String> document = new HashMap<>();
        document.put("title", "문서");
        document.put("path", sPathDocument);

        // Pair<String, String> root = new Pair<>("루트", sPathRoot);
        // Pair<String, String> sdcard = new Pair<>("SD Card", sPathSdcard);
        // Pair<String, String> extsdcard = new Pair<>("확장 메모리", sPathExtSdcard);
        // Pair<String, String> music = new Pair<>("음악", sPathMusic);
        // Pair<String, String> movie = new Pair<>("동영상", sPathMovie);
        // Pair<String, String> dcim = new Pair<>("사진", sPathDCIM);
        // Pair<String, String> picture = new Pair<>("그림", sPathPicture);
        // Pair<String, String> download = new Pair<>("다운로드", sPathDownload);
        // Pair<String, String> document = new Pair<>("문서", sPathDocument);

        mTitleList = new ArrayList<>();
        mTitleList.add(root);
        mTitleList.add(sdcard);
        if (externalSdCard != null) {
            mTitleList.add(extsdcard);
        }
        mTitleList.add(music);
        mTitleList.add(movie);
        mTitleList.add(dcim);
        mTitleList.add(picture);
        mTitleList.add(download);
        mTitleList.add(document);

        mAdapter = new SimpleAdapter(getApplicationContext(),
                mTitleList,
                android.R.layout.simple_list_item_2,
                new String[] {"title", "path"},
                new int[] {android.R.id.text1, android.R.id.text2});

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    private void findExtSd() {
        for( String sPathCur : Arrays.asList("external_SD", "sdcard1", "ext_card", "external_sd", "ext_sd", "external", "extSdCard", "externalSdCard")) {
            fileCur = new File( "/storage/", sPathCur);
            if( fileCur.isDirectory() && fileCur.canWrite()) {
                externalSdCard = fileCur.getAbsolutePath();
                break;
            }
        }

        if (externalSdCard == null) {
            for( String sPathCur : Arrays.asList("ext_card", "external_sd", "ext_sd", "external", "extSdCard", "externalSdCard", "external_SD", "sdcard1")) {
                fileCur = new File( "/mnt/", sPathCur);
                if( fileCur.isDirectory() && fileCur.canWrite()) {
                    externalSdCard = fileCur.getAbsolutePath();
                    break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = mListView.getAdapter().getItem(position);

        if (item instanceof Map) {
            Map mapData = (Map) item;
            String path = (String) mapData.get("path");

            mFileStack.push("");
            setCurrentPath(path);

            showFileList(path);
        } else if (item instanceof File) {
            // whenever click directory, go to the directory inside
            File fileData = (File) item;
            if (fileData.isDirectory()) {

                // insert path on the history
                mFileStack.push(mCurrentPath);
                setCurrentPath(fileData.getAbsolutePath());

                showFileList(fileData.getAbsolutePath());
            } else {
                switch (getMimeType(fileData)) {
                    case "audio":
                        Intent iAudio = new Intent(this, AudioFileListActivity.class);
                        iAudio.setData(Uri.fromFile(fileData));
                        startActivityForResult(iAudio, ActivityForAudio);
                        break;

                    case "video":
                        Intent iVideo = new Intent(this, VideoFileListActivity.class);
                        iVideo.setData(Uri.fromFile(fileData));
                        startActivityForResult(iVideo, ActivityForVideo);
                        break;

                    case "image":
                        Intent iImage = new Intent(this, ImageFileListActivity.class);
                        iImage.setData(Uri.fromFile(fileData));
                        startActivityForResult(iImage, ActivityForImage);
                        break;

                    default:
                        try {
                            if (mimeType(fileData.getAbsolutePath()) != null) {
                                // if the extension is not audio, video, or image, use chooser for user selection
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(fileData), mimeType(fileData.getAbsolutePath()));
                                startActivity(Intent.createChooser(intent, "파일선택..."));
                            } else {
                                showLog("실행할 수 없습니다.");
                            }
                        } catch (ActivityNotFoundException e) {
                            showToast("실행할 앱이 없습니다.");
                            break;
                        }
                }
            }
        }
    }

    private String getMimeType(File fileData) {
        String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "xmf", "rtx", "ota", "imy", "ts"};
        String[] video = {"avi", "mkv", "mp4", "wmv", "asf", "mov", "mpg", "flv", "tp", "3gp", "m4v", "rmvb", "webm"};
        String[] image = {"jpg", "gif", "png", "bmp", "tif", "tiff", "webp"};

        int i = fileData.getAbsolutePath().lastIndexOf('.');
        int j = fileData.getAbsolutePath().length();
        String extension = fileData.getAbsolutePath().substring(i + 1, j);
        String mimeType = extension.toLowerCase();

        for (int x = 0; x < audio.length; x++) {
            if (mimeType.equals(audio[x])) {
                return "audio";
            }
        }

        for (int y = 0; y < video.length; y++) {
            if (mimeType.equals(video[y])) {
                return "video";
            }
        }

        for (int z = 0; z < image.length; z++) {
            if (mimeType.equals(image[z])) {
                return "image";
            }
        }
        return "";
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case ActivityForAudio:
                if (resultCode == RESULT_OK) {
                    intent.getExtras().getInt("data");
                }
                break;

            case ActivityForVideo:
                if (resultCode == RESULT_OK) {
                    intent.getExtras().getInt("data");
                }
                break;

            case ActivityForImage:
                if (resultCode == RESULT_OK) {
                    intent.getExtras().getInt("data");
                }
                break;
        }
    }

    private void showFileList(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();

        if (files == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(FileManagerActivity.this);
            builder.setTitle("오류")
                    .setMessage("파일/폴더를 열 수 없습니다!")
                    .setPositiveButton("확인", null)
                    .show();
            String pathBack = mFileStack.pop();
            setCurrentPath(pathBack);
            return;
        }

        List<File> fileList = new ArrayList<>();

        // for (int i = 0; i < files.length; i++) {
        // File f = files[i];
        // }
        for (File f : files) {
            if (f != null) {
                fileList.add(f);
            }
        }

        Collections.sort(fileList);
        Collections.sort(fileList, mFolderAscComparator);

        FileAdapter fileAdapter = new FileAdapter(getApplicationContext(), fileList);
        mListView.setAdapter(fileAdapter);
    }

    Comparator<File> mDescComparator = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            String left = lhs.getName();
            String right = rhs.getName();
            return right.compareTo(left);
        }
    };

    // left, right
    // file, file = return 0 : not change
    // file, directory = return 1 : change
    // directory, file = return -1 : change
    // directory, directory = return 0 : not change

    Comparator<File> mFolderAscComparator = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            if (!lhs.isDirectory() && rhs.isDirectory()) {
                return 1;
            } else if (lhs.isDirectory() && !rhs.isDirectory()) {
                return -1;
            }
            return 0;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // if the stack is empty (no more backward), kill this process
            if (!mFileStack.empty()) {
                // if something is in the stack, go back
                String prevPath = mFileStack.pop();
                setCurrentPath(prevPath);
                if (prevPath.equals("")) {
                    setupHome();
                } else {
                    showFileList(prevPath);
                }
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setCurrentPath(String path) {
        mCurrentPath = path;
        mTvCurrentPath.setText(mCurrentPath);
    }

    public static String mimeType(String url) {
        String type = null;
        String ext = url.substring(url.lastIndexOf('.'));
        String extension = MimeTypeMap.getFileExtensionFromUrl(ext);

        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type;
    }
}
