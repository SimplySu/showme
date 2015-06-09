package com.suwonsmartapp.hello.showme.file;

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
import com.suwonsmartapp.hello.showme.audio.AudioFileListActivity;
import com.suwonsmartapp.hello.showme.image.ImageFileListActivity;
import com.suwonsmartapp.hello.showme.video.VideoFileListActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    public static final String sPathDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    public static final String sPathDocument = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();

    private ListView mListView;
    private FileManagerAdapter mFileManagerAdapter;

    // we will use default simple adapter
    private ArrayList<FileManagerInfo> mTitleList;
    private SimpleAdapter mAdapter;

    // history management : push current path (mCurrentPath) before going to the next screen
    // mCurrentPath = next screen path
    private Stack<String> mFileStack;

    // current full path
    private String mCurrentPath = "";

    private TextView mTvCurrentPath;

    private String externalSdCard = null;
    private File fileCur = null;

    private int mCurrentPosition;

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
        setContentView(R.layout.file_manager_main);
        showLog("onCreate");

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mListView = (ListView) findViewById(R.id.lv_filetree);
        mTvCurrentPath = (TextView) findViewById(R.id.tv_currentPath);

        mFileStack = new Stack<>();

        setupHome();    // display categorized initial screen
    }

    private void setupHome() {
        findExtSd();    // setup path name on externalSdCard if secondary SD card exists

        FileManagerInfo root = new FileManagerInfo();
        root.setIconName(R.drawable.icon_root);
        root.setFolderName("루트");
        root.setFolderPath(sPathRoot);

        FileManagerInfo sdcard = new FileManagerInfo();
        sdcard.setIconName(R.drawable.icon_sdcard);
        sdcard.setFolderName("내부 메모리");
        sdcard.setFolderPath(sPathSdcard);

        FileManagerInfo extsdcard = new FileManagerInfo();
        if (externalSdCard != null) {
            extsdcard.setIconName(R.drawable.icon_extsd);
            extsdcard.setFolderName("확장 메모리");
            extsdcard.setFolderPath(externalSdCard);
        }

        FileManagerInfo music = new FileManagerInfo();
        music.setIconName(R.drawable.icon_music);
        music.setFolderName("음악");
        music.setFolderPath(sPathMusic);

        FileManagerInfo movie = new FileManagerInfo();
        movie.setIconName(R.drawable.icon_movie);
        movie.setFolderName("동영상");
        movie.setFolderPath(sPathMovie);

        FileManagerInfo dcim = new FileManagerInfo();
        dcim.setIconName(R.drawable.icon_picture);
        dcim.setFolderName("사진");
        dcim.setFolderPath(sPathDCIM);

        FileManagerInfo picture = new FileManagerInfo();
        picture.setIconName(R.drawable.icon_image);
        picture.setFolderName("그림");
        picture.setFolderPath(sPathPicture);

        FileManagerInfo download = new FileManagerInfo();
        download.setIconName(R.drawable.icon_download);
        download.setFolderName("다운로드");
        download.setFolderPath(sPathDownload);

        FileManagerInfo document = new FileManagerInfo();
        document.setIconName(R.drawable.icon_document);
        document.setFolderName("문서");
        document.setFolderPath(sPathDocument);

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

        mFileManagerAdapter = new FileManagerAdapter(getApplicationContext(), mTitleList);
        mListView.setAdapter(mFileManagerAdapter);
        mListView.setOnItemClickListener(this);      // handle if user selected title directly
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

        if (item instanceof FileManagerInfo) {
            FileManagerInfo foldername = (FileManagerInfo) item;
            String path = foldername.getFolderPath();

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
                        Intent iAudio = new Intent(getApplicationContext(), AudioFileListActivity.class);
                        iAudio.putExtra("FilePath", fileData.toString());
                        startActivityForResult(iAudio, REQUEST_CODE_AUDIO);
                        break;

                    case "video":
                        Intent iVideo = new Intent(getApplicationContext(), VideoFileListActivity.class);
                        iVideo.putExtra("FilePath", fileData.toString());
                        startActivityForResult(iVideo, REQUEST_CODE_VIDEO);
                        break;

                    case "title":
                        Intent iTitle = new Intent(getApplicationContext(), VideoFileListActivity.class);
                        iTitle.putExtra("FilePath", fileData.toString());
                        startActivityForResult(iTitle, REQUEST_CODE_VIDEO);
                        break;

                    case "image":
                        Intent iImage = new Intent(getApplicationContext(), ImageFileListActivity.class);
                        iImage.putExtra("FilePath", fileData.toString());
                        startActivityForResult(iImage, REQUEST_CODE_IMAGE);
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
        String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "xmf", "rtx", "ota", "imy", "ts", "wma"};
        String[] video = {"avi", "mkv", "mp4", "wmv", "asf", "mov", "mpg", "flv", "tp", "3gp", "m4v", "rmvb", "webm"};
        String[] image = {"jpg", "gif", "png", "bmp", "tif", "tiff", "jpeg", "webp"};
        String[] title = {"smi", "srt", "sub", "ass", "ssa"};

        int i = fileData.getAbsolutePath().lastIndexOf('.');
        int j = fileData.getAbsolutePath().length();
        String extension = fileData.getAbsolutePath().substring(i + 1, j);
        String mimeType = extension.toLowerCase();

        for (String anAudio : audio) {
            if (mimeType.equals(anAudio)) {
                return "audio";                     // currently .cue and .ape are not supported
            }
        }

        for (String aVideo : video) {
            if (mimeType.equals(aVideo)) {
                return "video";
            }
        }

        for (String aTitle : title) {
            if (mimeType.equals(aTitle)) {
                return "title";     // if user designate subtitle, we should find the corresponding movie file.
            }
        }

        for (String anImage : image) {
            if (mimeType.equals(anImage)) {
                return "image";
            }
        }
        return "";
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

        FileListAdapter fileListAdapter = new FileListAdapter(getApplicationContext(), fileList);
        mListView.setAdapter(fileListAdapter);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_AUDIO:
                if (resultCode == RESULT_OK) {
                    mCurrentPosition = data.getExtras().getInt("CurrentPosition");
                }
                break;

            case REQUEST_CODE_VIDEO:
                if (resultCode == RESULT_OK) {
                    mCurrentPosition = data.getExtras().getInt("CurrentPosition");
                }
                break;

            case REQUEST_CODE_IMAGE:
                if (resultCode == RESULT_OK) {
                    mCurrentPosition = data.getExtras().getInt("CurrentPosition");
                }
                break;
        }
    }
}