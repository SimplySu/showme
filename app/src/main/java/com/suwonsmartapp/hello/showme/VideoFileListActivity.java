
package com.suwonsmartapp.hello.showme;

import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;

import java.util.ArrayList;

public class VideoFileListActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener {

    private static final String TAG = AudioFileListActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent
    private String requestedExternsion = "";        // specified extension by user from intent
    private boolean flagSubTitle = false;           // default is filename is not subtitle
    private String filenameWithoutExt = "";         // filename without extension for subtitle

    private VideoFileInfo videoFileInfo;                    // video file info getting by cursor
    private ArrayList<VideoFileInfo> mVideoFileInfoList;    // video file media_player_icon_information list
    private Cursor mCursor;                                 // cursor for media store searching
    private static int mCurrentPosition = -1;               // -1 means we didn't specify file

    private VideoListAdapter mAdapter;

    private ListView mMovieListView;

    private String value;                                   // filename passed by file manager

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
        showLog("onCreate");

        mVideoFileInfoList = new ArrayList<>();

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        readIntent();                       // get pathname and filename

        flagSubTitle = (requestedExternsion.equals("smi")) || (requestedExternsion.equals("srt")) || (requestedExternsion.equals("sub"));

        prepareTitleToPlay();               // setup titles for playing

        mMovieListView = (ListView) findViewById(R.id.lv_movies);
        mAdapter = new VideoListAdapter(getApplicationContext(), mCursor, true);
        mMovieListView.setAdapter(mAdapter);
        mMovieListView.setOnItemClickListener(this);

        mCurrentPosition = searchTitleIndex();      // search title index which was specified by user
        mAdapter.setCurrentPosition(mCurrentPosition);
        mMovieListView.smoothScrollToPosition(mCurrentPosition);

        Intent initialIntent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);       // current title position
        initialIntent.putParcelableArrayListExtra("videoInfoList", mVideoFileInfoList);
        startActivityForResult(initialIntent, REQUEST_CODE_VIDEO_PLAYER);
    }

    private void readIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if(intent.hasExtra("FilePath")) {
                value = intent.getStringExtra("FilePath");
                showLog(value);
            } else {
                showToast("잘못된 파일입니다.");
                finish();
            }
            requestedPathname = value.substring(0, value.lastIndexOf('/'));
            requestedFilename = value.substring(value.lastIndexOf('/') + 1, value.length());
            requestedExternsion = requestedFilename.substring(requestedFilename.lastIndexOf('.') + 1, requestedFilename.length()).toLowerCase();
            filenameWithoutExt = requestedFilename.substring(0, requestedFilename.lastIndexOf('.'));
        }
    }

    private void prepareTitleToPlay() {
        showLog("prepareTitleToPlay");

        // query : syncronized processing (can be slow)
        // loader : asyncronized processing

        String[] projection = {
                MediaStore.Video.Media._ID,                 // album ID
                MediaStore.Video.Media.ARTIST,              // artist
                MediaStore.Video.Media.TITLE,               // title
                MediaStore.Video.Media.DATA,                // full pathname
                MediaStore.Video.Media.DISPLAY_NAME,        // filename
                MediaStore.Video.Media.DURATION,            // play time
                MediaStore.MediaColumns.DATA
        };

        String selection = MediaStore.Video.Media.DATA + " like ?";
        String sortOrder = MediaStore.Video.Media.TITLE + " ASC";

        mCursor = getContentResolver()
                .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,    // The content URI of the words table
                        projection,                 // The columns to return for each row
                        selection,                  //  selection criteria
                        new String[] {requestedPathname + "/%"},        // Selection criteria
                        sortOrder);                 // The sort order for the returned rows

        showLog("query result : " + String.valueOf(mCursor));

        mVideoFileInfoList = new ArrayList<>();     // initialize info list

        if (mCursor != null) {
            mCursor.moveToFirst();              // from the start of data base

            showLog("searched file count : " + String.valueOf(mCursor.getCount()));

            for (int i = 0; i < mCursor.getCount(); i++) {
                mCursor.moveToPosition(i);      // get next row of data base

                if (isDirectoryMatch()) {      // select matched directory only
                    videoFileInfo = new VideoFileInfo();
                    videoFileInfo.setId(mCursor.getLong(0));                // video ID
                    videoFileInfo.setArtist(mCursor.getString(1));          // artist
                    videoFileInfo.setTitle(mCursor.getString(2));           // title
                    videoFileInfo.setMediaData(mCursor.getString(3));       // full path of the video
                    videoFileInfo.setDisplayName(mCursor.getString(4));     // brief video name to show
                    videoFileInfo.setDuration(mCursor.getLong(5));          // playing time
                    videoFileInfo.setColumnsData(mCursor.getString(6));     // URI

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoFileInfo.getId());
                    videoFileInfo.setVideoUri(contentUri);                   // get video icon

                    mVideoFileInfoList.add(videoFileInfo);                  // register music on the play list
                }
            }
        } else {
            showToast("영상 파일이 없습니다.");          // no video found
        }
    }

    // return true if current file's directory is matching with user selection,
    // return false if it is not.
    // we will include subdirectories also.
    private boolean isDirectoryMatch() {
        String fullPath = mCursor.getString(3);         // get full path name
        int i = fullPath.lastIndexOf('/');              // search last slash
        int j = fullPath.length();                      // get fullpath's length
        String pathname = fullPath.substring(0, i);     // get pathname only
        String filename = fullPath.substring(i + 1, j); // get filename only

        showLog(filename);

        int k = requestedPathname.length();             // get requested path length
        int l = pathname.length();                      // get current pathname length
        if (l < k) {                                    // if current pathname is shorter than requested
            return false;                               // we don't need to compare it
        }

        String s = pathname.substring(0, k);            // compare just we requested for subdirectory
        return s.equals(requestedPathname);             // see if this directory is matching ?
    }

    // search matched title with specified by user
    private int searchTitleIndex() {
        if (flagSubTitle) {
            for (int i = 0; i < mVideoFileInfoList.size(); i++) {
                VideoFileInfo videoFileInfo = mVideoFileInfoList.get(i);    // read audio file
                String temp = videoFileInfo.getDisplayName();
                if (filenameWithoutExt.equals(temp.substring(0, temp.lastIndexOf('.')))) {
                    return i;          // return matched index
                }
            }
            return 0;                  // default is the first title
        } else {
            for (int i = 0; i < mVideoFileInfoList.size(); i++) {
                VideoFileInfo videoFileInfo = mVideoFileInfoList.get(i);    // read audio file
                if (requestedFilename.equals(videoFileInfo.getDisplayName())) {
                    return i;          // return matched index
                }
            }
            return 0;                  // default is the first title
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCurrentPosition = position;
        Intent initialIntent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);       // current title position
        initialIntent.putParcelableArrayListExtra("videoInfoList", mVideoFileInfoList);
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
