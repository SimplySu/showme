package com.suwonsmartapp.hello.showme.file;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.suwonsmartapp.hello.R;
import com.suwonsmartapp.hello.showme.audio.AudioFileListActivity;
import com.suwonsmartapp.hello.showme.image.ImageFileListActivity;
import com.suwonsmartapp.hello.showme.video.VideoFileListActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class FileManagerActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String VERSION = "v1.0.00 : 2015.6.29";    // 초기 버전 넘버
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
    private FileManagerAdapter mRootAdapter;
    private ArrayList<FileManagerInfo> mRootList;

    // 히스토리 관리 : 디렉토리 안으로 들어가기 전에 현재 경로를 PUSH함.
    // mCurrentPath = 다음 표시할 경로 화면.
    private Stack<String> mFileStack;

    // 현재 경로명.
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

    // 루트에서는 파일/폴더 지우기가 안되므로 초기치를 false로 함.
    private boolean fAllowded = false;
    private FileAdapter fileAdapter;

    private ArrayList<FileInfo> fileList;
    private final int MODEall = 0;
    private final int MODEaudio = 1;
    private final int MODEimage = 2;
    private final int MODEvideo = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_manager_main);

        // 화면을 세로모드로 고정함.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mListView = (ListView) findViewById(R.id.lv_filetree);
        mTvCurrentPath = (TextView) findViewById(R.id.tv_currentPath);

        // 리소스로 AdView를 검색하고 요청을 로드함.
        AdView adView = (AdView) this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // 경로명을 저장할 스택을 초기화함.
        mFileStack = new Stack<>();

        // 루트 화면을 표시함.
        setupHome();

        // 짧게 클릭하면 해당 폴더로 들어가거나, 파일인 경우 실행함.
        mListView.setOnItemClickListener(this);
        // 길게 클릭하면 해당 파일/폴더를 삭제함.
        mListView.setOnItemLongClickListener(this);
    }

    // 초기 화면을 표시함.
    private void setupHome() {
        fAllowded = false;      // 초기화면에서는 삭제가 안됨.
        findExtSd();    // 확장 SD 카드가 존재하면 그 경로을 파악해야 함.

        FileManagerInfo root = new FileManagerInfo();
        root.setIconName(R.drawable.icon_root);
        root.setFolderName(getString(R.string.root));
        root.setFolderPath(sPathRoot);

        FileManagerInfo sdcard = new FileManagerInfo();
        sdcard.setIconName(R.drawable.icon_sdcard);
        sdcard.setFolderName(getString(R.string.sdcard));
        sdcard.setFolderPath(sPathSdcard);

        FileManagerInfo extsdcard = new FileManagerInfo();
        if (externalSdCard != null) {
            extsdcard.setIconName(R.drawable.icon_extsd);
            extsdcard.setFolderName(getString(R.string.extsdcard));
            extsdcard.setFolderPath(externalSdCard);
        }

        FileManagerInfo music = new FileManagerInfo();
        music.setIconName(R.drawable.icon_music);
        music.setFolderName(getString(R.string.music));
        music.setFolderPath(sPathMusic);

        FileManagerInfo movie = new FileManagerInfo();
        movie.setIconName(R.drawable.icon_movie);
        movie.setFolderName(getString(R.string.movie));
        movie.setFolderPath(sPathMovie);

        FileManagerInfo dcim = new FileManagerInfo();
        dcim.setIconName(R.drawable.icon_picture);
        dcim.setFolderName(getString(R.string.picture));
        dcim.setFolderPath(sPathDCIM);

        FileManagerInfo picture = new FileManagerInfo();
        picture.setIconName(R.drawable.icon_image);
        picture.setFolderName(getString(R.string.image));
        picture.setFolderPath(sPathPicture);

        FileManagerInfo download = new FileManagerInfo();
        download.setIconName(R.drawable.icon_download);
        download.setFolderName(getString(R.string.download));
        download.setFolderPath(sPathDownload);

        FileManagerInfo document = new FileManagerInfo();
        document.setIconName(R.drawable.icon_document);
        document.setFolderName(getString(R.string.document));
        document.setFolderPath(sPathDocument);

        mRootList = new ArrayList<>();
        mRootList.add(root);
        mRootList.add(sdcard);
        if (externalSdCard != null) {       // 확장 SD 카드가 존재하는 경우에만
            mRootList.add(extsdcard);       // 이 메뉴가 표시되도록 함.
        }
        mRootList.add(music);
        mRootList.add(movie);
        mRootList.add(dcim);
        mRootList.add(picture);
        mRootList.add(download);
        mRootList.add(document);

        // 초기화면을 표시할 어댑터에 리스트를 전달함.
        mRootAdapter = new FileManagerAdapter(getApplicationContext(), mRootList);
        mListView.setAdapter(mRootAdapter);
    }

    // 확장 SD 카드 판별 : 지정된 디렉토리가 존재하면 확장 SD 카드가 존재할 가능성이 있고,
    // 그 파일이 디렉토리면서 쓰기 가능하면 확장 SD 카드임.
    private void findExtSd() {
        for( String sPathCur : Arrays.asList("external_SD", "sdcard1", "ext_card", "external_sd",
                "ext_sd", "external", "extSdCard", "externalSdCard")) {
            fileCur = new File("/storage/", sPathCur);
            if( fileCur.isDirectory() && fileCur.canWrite()) {
                externalSdCard = fileCur.getAbsolutePath();
                break;
            }
        }

        // mnt인 경우 리눅스에서는 액세스 가능하지만 안드로이드에서는 엑세스 불가능함.
        if (externalSdCard == null) {
            for( String sPathCur : Arrays.asList("ext_card", "external_sd", "ext_sd", "external",
                    "extSdCard", "externalSdCard", "external_SD", "sdcard1")) {
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
        // 옵션 메뉴를 정의할 수 있으나 지금은 지원하지 않음.
        getMenuInflater().inflate(R.menu.menu_file_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 액션바 아이템을 다룰 수 있지만 현재는 지원하지 않음.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = mListView.getAdapter().getItem(position);

        // 초기화면인 경우
        if (item instanceof FileManagerInfo) {
            FileManagerInfo foldername = (FileManagerInfo) item;
            String path = foldername.getFolderPath();

            mFileStack.push("");
            setCurrentPath(path);
            showFileList(path);

        } else if (item instanceof FileInfo) {
            FileInfo fileData = (FileInfo) item;

            // 디렉토리인 경우 해당 디렉토리로 들어감.
            if (fileData.getFile().isDirectory()) {
                mFileStack.push(mCurrentPath);
                setCurrentPath(fileData.getFile().getAbsolutePath());
                showFileList(fileData.getFile().getAbsolutePath());

            // 파일인 경우, 오디오, 그림, 비디오 파일이면 이를 실행함.
            } else {
                switch (getMimeType(fileData.getFile())) {
                    case "audio":
                        Intent iAudio = new Intent(getApplicationContext(), AudioFileListActivity.class);
                        iAudio.putExtra("FilePath", fileData.getTitle());
                        startActivityForResult(iAudio, REQUEST_CODE_AUDIO);
                        break;

                    case "video":
                        Intent iVideo = new Intent(getApplicationContext(), VideoFileListActivity.class);
                        iVideo.putExtra("FilePath", fileData.getTitle());
                        startActivityForResult(iVideo, REQUEST_CODE_VIDEO);
                        break;

                    case "title":
                        Intent iTitle = new Intent(getApplicationContext(), VideoFileListActivity.class);
                        iTitle.putExtra("FilePath", fileData.getTitle());
                        startActivityForResult(iTitle, REQUEST_CODE_VIDEO);
                        break;

                    case "image":
                        Intent iImage = new Intent(getApplicationContext(), ImageFileListActivity.class);
                        iImage.putExtra("FilePath", fileData.getTitle());
                        startActivityForResult(iImage, REQUEST_CODE_IMAGE);
                        break;

                    // 오디오, 그림, 비디오가 아닌 경우 설치된 APP을 통해 실행함.
                    default:
                        try {
                            if (mimeType(fileData.getFile().getAbsolutePath()) != null) {
                                // if the extension is not audio, video, or image, use chooser for user selection
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(fileData.getFile()), mimeType(fileData.getFile().getAbsolutePath()));
                                startActivity(Intent.createChooser(intent, "Select file..."));
                            } else {
                                showLog(getString(R.string.msg_cant_execute));
                            }
                        } catch (ActivityNotFoundException e) {
                            showToast(getString(R.string.msg_no_app));
                            break;
                        }
                }
            }
        }
    }

    // 파일의 확장자를 보고 실행할 수 있는 파일인지 구분함.
    private String getMimeType(File file) {
        String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "wma"};
        String[] video = {"avi", "mkv", "mp4", "wmv", "asf", "mov", "mpg", "flv", "tp", "3gp", "m4v", "rmvb", "webm"};
        String[] image = {"jpg", "gif", "png", "bmp", "jpeg", "webp"};
        String[] title = {"smi", "srt", "sub", "idx", "ass", "ssa"};

        int i = file.getAbsolutePath().lastIndexOf('.');
        int j = file.getAbsolutePath().length();
        String extension = file.getAbsolutePath().substring(i + 1, j);
        String mimeType = extension.toLowerCase();

        // 오디오의 경우 .cue, .ape 파일은 실행할 수 없음.
        for (String anAudio : audio) {
            if (mimeType.equals(anAudio)) {
                return "audio";
            }
        }

        for (String aVideo : video) {
            if (mimeType.equals(aVideo)) {
                return "video";
            }
        }

        // 자막을 클릭한 경우 이에 해당하는 비디오를 찾아 실행함.
        for (String aTitle : title) {
            if (mimeType.equals(aTitle)) {
                return "title";
            }
        }

        // 현재 tiff 파일은 표시할 수 없음.
        for (String anImage : image) {
            if (mimeType.equals(anImage)) {
                return "image";
            }
        }

        // 모르는 파일인 경우 null을 리턴함.
        return "";
    }

    // 지정된 디렉토리에 있는 파일들을 화면에 표시해 줌.
    private void showFileList(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();

        // 만일 표시할 파일이 없거나 읽지 못하는 폴더인 경우 에러메시지를 표시함.
        if (files == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(FileManagerActivity.this);
            builder.setTitle(getString(R.string.msg_error))
                    .setMessage(getString(R.string.msg_cant_open))
                    .setPositiveButton(getString(R.string.msg_confirm), null)
                    .show();
            String pathBack = mFileStack.pop();
            setCurrentPath(pathBack);
            return;
        }

        // 현재 경로의 모든 파일을 읽어 어댑터에 전달함.
        fileList = new FileLists().getFileList(path, MODEall);
        fileAdapter = new FileAdapter(getApplicationContext(), fileList);
        mListView.setAdapter(fileAdapter);

        // 지금부터 파일/폴더를 삭제할 수 있음.
        fAllowded = true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // BACK 키를 눌러 돌아가는 경우 스택이 비었으면 프로세스를 종료함.
            if (!mFileStack.empty()) {
                // 스택에 무언가 있으면 이를 표시함.
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

    // 현재 경로명을 저장하고 텍스트 뷰에 표시함.
    private void setCurrentPath(String path) {
        mCurrentPath = path;
        mTvCurrentPath.setText(mCurrentPath);
    }

    // 안드로이드에서 제공하는 마임타입 구별법.
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

    // 오디오, 그림, 비디오 실행이 끝나고 다음 실행을 위해 현재의 위치를 되돌려줌.
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

    // 롱클릭에 의해 파일/폴더 삭제 기능을 수행함.
    // 파일/폴더 생성 및 변경 기능은 현재 지원하지 않음.
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        Object item = mListView.getAdapter().getItem(position);
        FileInfo fileData = (FileInfo) item;
        String ap = fileData.getFile().getAbsolutePath();
        String f = ap.substring(ap.lastIndexOf("/") + 1, ap.length());

        builder.setTitle(getString(R.string.msg_confirm_delete) + " : " + f);
        builder.setMessage(getString(R.string.msg_are_you_sure));

        builder.setPositiveButton(getString(R.string.msg_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (fAllowded) {            // 현재 삭제 기능을 수행할 수 있는지?
                    Object item = mListView.getAdapter().getItem(position);
                    if (item instanceof FileInfo) {
                        FileInfo fileData = (FileInfo) item;

                            String absPath = fileData.getFile().getAbsolutePath();
                            if (fileData.getFile().isDirectory()) {
                                // 디렉토리 안의 모든 파일을 지우고 폴더도 지움.
                                boolean wellDeleted = deleteDir(absPath);
                                fileList.remove(position);
                                fileAdapter.notifyDataSetChanged();

                                // 잘 지워졌으면 메시지를 표시해 줌.
                                if (wellDeleted) {
                                    showToast(getString(R.string.msg_del_dir));
                                } else {
                                    showToast(getString(R.string.msg_cant_del_dir));
                                }
                            } else {
                                // 파일만 지움.
                                File file = new File(absPath);
                                boolean deleted = file.delete();

                                // 잘 지워졌으면 메시지를 표시해 줌.
                                if (deleted) {
                                    fileList.remove(position);
                                    fileAdapter.notifyDataSetChanged();
                                    showToast(getString(R.string.msg_del_file));
                                } else {
                                    showToast(getString(R.string.msg_cant_del_file));
                                }
                            }
                    }
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getString(R.string.msg_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    // 재귀호출을 통해 폴더내 모든 파일을 지움.
    public boolean deleteDir(String aPath) {
        File file = new File(aPath);
        if (file.exists()) {
            File[] childFileList = file.listFiles();
            for (File childFile : childFileList) {

                // 폴더 안에 폴더가 있을 경우 자신을 재귀호출함.
                if (childFile.isDirectory()) {
                    deleteDir(childFile.getAbsolutePath());
                }
                else {
                    childFile.delete();
                }
            }
            file.delete();
            return true;
        } else {
            return false;
        }
    }
}
