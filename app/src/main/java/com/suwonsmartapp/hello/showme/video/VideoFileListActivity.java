
package com.suwonsmartapp.hello.showme.video;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;
import com.suwonsmartapp.hello.showme.audio.AudioFileListActivity;
import com.suwonsmartapp.hello.showme.file.FileAdapter;
import com.suwonsmartapp.hello.showme.file.FileInfo;
import com.suwonsmartapp.hello.showme.file.FileLists;

import java.io.File;
import java.util.ArrayList;

public class VideoFileListActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener {

    private static final String TAG = AudioFileListActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private ArrayList<FileInfo> movieList;
    private final int MODEall = 0;
    private final int MODEaudio = 1;
    private final int MODEimage = 2;
    private final int MODEvideo = 3;

    // 인텐트를 통해 받은 경로명과 파일명.
    private String requestedPathname = "";
    private String requestedFilename = "";

    // 비디오가 아닌 자막파일을 클릭했을 경우 처리를 위해.
    private String requestedExternsion = "";

    private boolean flagSubTitle = false;
    private String filenameWithoutExt = "";

    // -1은 파일이 특정되지 않았음을 나타냄. (초기값)
    private static int mCurrentPosition = -1;

    private FileAdapter mAdapter;
    private ListView mMovieListView;

    // 파일 매니저를 통해 건네받은 파일명.
    private String value;

    public static final int RESULT_OK = 0x0fff;
    public static final int REQUEST_CODE_AUDIO = 0x0001;
    public static final int REQUEST_CODE_AUDIO_PLAYER = 0x0002;
    public static final int REQUEST_CODE_VIDEO = 0x0010;
    public static final int REQUEST_CODE_VIDEO_PLAYER = 0x0020;
    public static final int REQUEST_CODE_IMAGE = 0x0100;
    public static final int REQUEST_CODE_IMAGE_PLAYER = 0x0200;
    private Bundle extraVideoService;
    private Intent intentVideoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_filelist);

        // 화면을 세로모드로 고정함.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 인텐트를 통해 경로명과 파일명을 읽음.
        readIntent();

        // 자막 파일을 지정한 경우 해당하는 영화 파일을 찾아서 실행함.
        flagSubTitle = (requestedExternsion.equals("smi")) || (requestedExternsion.equals("idx")) ||
                (requestedExternsion.equals("srt")) || (requestedExternsion.equals("sub")) ||
                (requestedExternsion.equals("ass")) || (requestedExternsion.equals("ssa"));

        // 실행할 영화 파일만을 추출함.
        prepareTitleToPlay();

        mMovieListView = (ListView) findViewById(R.id.lv_movies);
        mAdapter = new FileAdapter(getApplicationContext(), movieList);
        mMovieListView.setAdapter(mAdapter);
        mMovieListView.setOnItemClickListener(this);

        // 특정 파일을 지정한 경우 여기부터 실행함.
        mCurrentPosition = searchTitleIndex();
        mMovieListView.smoothScrollToPosition(mCurrentPosition);

        // 지정한 위치를 세팅함.
        mMovieListView.setSelection(mCurrentPosition);

        Intent initialIntent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);
        initialIntent.putParcelableArrayListExtra("videoInfoList", movieList);
        startActivityForResult(initialIntent, REQUEST_CODE_VIDEO_PLAYER);
    }

    // 인텐트를 통해 경로명과 파일명을 읽음.
    private void readIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if(intent.hasExtra("FilePath")) {
                value = intent.getStringExtra("FilePath");
                showLog(value);
            } else {
                showToast(getString(R.string.msg_wrong_file));
                finish();
            }
            requestedPathname = value.substring(0, value.lastIndexOf('/'));
            requestedFilename = value.substring(value.lastIndexOf('/') + 1, value.length());
            requestedExternsion = requestedFilename.substring(requestedFilename.lastIndexOf('.') + 1,
                                    requestedFilename.length()).toLowerCase();
            filenameWithoutExt = requestedFilename.substring(0, requestedFilename.lastIndexOf('.'));
        }
    }

    // 실행 가능한 영화 파일만 리스트로 만듬.
    private void prepareTitleToPlay() {
        movieList = new FileLists().getFileList(requestedPathname, MODEvideo);
        if (movieList == null) {
            showToast(getString(R.string.msg_no_movie));          // 재생할 파일이 없음.
        }
    }

    // 지정한 파일이 재생 가능한지 검사함.
    private int searchTitleIndex() {
        // 자막 파일을 클릭한 경우
        if (flagSubTitle) {
            for (int i = 0; i < movieList.size(); i++) {
                FileInfo fileInfo = movieList.get(i);
                File f = fileInfo.getFile();
                String temp = f.getName();
                // 확장자를 제외한 파일명을 검사함.
                if (filenameWithoutExt.equals(temp.substring(0, temp.lastIndexOf('.')))) {
                    return i;          // 일치하는 인덱스를 리턴함.
                }
            }
            return 0;                  // 일치하는 파일이 없으면 처음부터 재생함.

        // 영화 파일을 클릭한 경우
        } else {
            for (int i = 0; i < movieList.size(); i++) {
                FileInfo fileInfo = movieList.get(i);
                File f = fileInfo.getFile();
                // 확장자를 포함한 모든 파일명을 검사함.
                if (requestedFilename.equals(f.getName())) {
                    return i;          // 일치하는 인덱스를 리턴함.
                }
            }
            return 0;                  // 일치하는 파일이 없으면 처음부터 재생함.
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCurrentPosition = position;
        Intent initialIntent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);
        initialIntent.putParcelableArrayListExtra("videoInfoList", movieList);
        startActivityForResult(initialIntent, REQUEST_CODE_VIDEO_PLAYER);
    }

    @Override
    protected void onDestroy() {
        extraVideoService = new Bundle();
        intentVideoService = new Intent();
        extraVideoService.putInt("CurrentPosition", mCurrentPosition);
        intentVideoService.putExtras(extraVideoService);
        this.setResult(RESULT_OK, intentVideoService);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_VIDEO_PLAYER) {
            if (resultCode == RESULT_OK) {
                mCurrentPosition = data.getExtras().getInt("CurrentPosition");
            }
        }
    }
}
