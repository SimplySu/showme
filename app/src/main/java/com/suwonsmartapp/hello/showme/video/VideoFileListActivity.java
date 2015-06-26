
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

    // ����Ʈ�� ���� ���� ��θ�� ���ϸ�.
    private String requestedPathname = "";
    private String requestedFilename = "";

    // ������ �ƴ� �ڸ������� Ŭ������ ��� ó���� ����.
    private String requestedExternsion = "";

    private boolean flagSubTitle = false;
    private String filenameWithoutExt = "";

    // -1�� ������ Ư������ �ʾ����� ��Ÿ��. (�ʱⰪ)
    private static int mCurrentPosition = -1;

    private FileAdapter mAdapter;
    private ListView mMovieListView;

    // ���� �Ŵ����� ���� �ǳ׹��� ���ϸ�.
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

        // ȭ���� ���θ��� ������.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // ����Ʈ�� ���� ��θ�� ���ϸ��� ����.
        readIntent();

        // �ڸ� ������ ������ ��� �ش��ϴ� ��ȭ ������ ã�Ƽ� ������.
        flagSubTitle = (requestedExternsion.equals("smi")) || (requestedExternsion.equals("idx")) ||
                (requestedExternsion.equals("srt")) || (requestedExternsion.equals("sub")) ||
                (requestedExternsion.equals("ass")) || (requestedExternsion.equals("ssa"));

        // ������ ��ȭ ���ϸ��� ������.
        prepareTitleToPlay();

        mMovieListView = (ListView) findViewById(R.id.lv_movies);
        mAdapter = new FileAdapter(getApplicationContext(), movieList);
        mMovieListView.setAdapter(mAdapter);
        mMovieListView.setOnItemClickListener(this);

        // Ư�� ������ ������ ��� ������� ������.
        mCurrentPosition = searchTitleIndex();
        mMovieListView.smoothScrollToPosition(mCurrentPosition);

        // ������ ��ġ�� ������.
        mMovieListView.setSelection(mCurrentPosition);

        Intent initialIntent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);
        initialIntent.putParcelableArrayListExtra("videoInfoList", movieList);
        startActivityForResult(initialIntent, REQUEST_CODE_VIDEO_PLAYER);
    }

    // ����Ʈ�� ���� ��θ�� ���ϸ��� ����.
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

    // ���� ������ ��ȭ ���ϸ� ����Ʈ�� ����.
    private void prepareTitleToPlay() {
        movieList = new FileLists().getFileList(requestedPathname, MODEvideo);
        if (movieList == null) {
            showToast(getString(R.string.msg_no_movie));          // ����� ������ ����.
        }
    }

    // ������ ������ ��� �������� �˻���.
    private int searchTitleIndex() {
        // �ڸ� ������ Ŭ���� ���
        if (flagSubTitle) {
            for (int i = 0; i < movieList.size(); i++) {
                FileInfo fileInfo = movieList.get(i);
                File f = fileInfo.getFile();
                String temp = f.getName();
                // Ȯ���ڸ� ������ ���ϸ��� �˻���.
                if (filenameWithoutExt.equals(temp.substring(0, temp.lastIndexOf('.')))) {
                    return i;          // ��ġ�ϴ� �ε����� ������.
                }
            }
            return 0;                  // ��ġ�ϴ� ������ ������ ó������ �����.

        // ��ȭ ������ Ŭ���� ���
        } else {
            for (int i = 0; i < movieList.size(); i++) {
                FileInfo fileInfo = movieList.get(i);
                File f = fileInfo.getFile();
                // Ȯ���ڸ� ������ ��� ���ϸ��� �˻���.
                if (requestedFilename.equals(f.getName())) {
                    return i;          // ��ġ�ϴ� �ε����� ������.
                }
            }
            return 0;                  // ��ġ�ϴ� ������ ������ ó������ �����.
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
