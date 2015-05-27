
package com.suwonsmartapp.hello.showme;

import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;

import java.net.URLDecoder;
import java.util.ArrayList;

public class VideoFileListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private static final String TAG = AudioFileListActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent

    private VideoFileInfo videoFileInfo;                    // video file info getting by cursor
    private ArrayList<VideoFileInfo> mVideoFileInfoList;    // video file media_player_icon_information list
    private Cursor mCursor;                                 // cursor for media store searching
    private static int mCurrentPosition = -1;               // -1 means we didn't specify file

    private VideoListAdapter mAdapter;

    private ListView mMovieListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
        showLog("onCreate");

        mVideoFileInfoList = new ArrayList<>();

        // fix the screen for portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent = getIntent();                // get user's intent
        if (intent != null) {                       // see if there was any contents
            String value = URLDecoder.decode(intent.getDataString());   // convert filename to UTF-8

            if (TextUtils.isEmpty(value) == false) {
                int i = value.lastIndexOf('/');
                int j = value.length();
                requestedPathname = value.substring(7, i);          // get requested pathname
                requestedFilename = value.substring(i + 1, j);      // and filename
            }
        }

        prepareTitleToPlay();               // setup titles for playing

        mMovieListView = (ListView) findViewById(R.id.lv_movies);
        mAdapter = new VideoListAdapter(getApplicationContext(), mCursor, true);
        mMovieListView.setAdapter(mAdapter);
        mMovieListView.setOnItemClickListener(this);

        mCurrentPosition = searchTitleIndex();      // search title index which was specified by user
        mAdapter.setCurrentPosition(mCurrentPosition);
        mMovieListView.smoothScrollToPosition(mCurrentPosition);

        // 로더 초기화
        getSupportLoaderManager().initLoader(0, null, this);

        Intent initialIntent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);       // current title position
        initialIntent.putParcelableArrayListExtra("videoInfoList", mVideoFileInfoList);
        startActivity(initialIntent);
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
                    videoFileInfo.setVideoUri(contentUri);                   // get music media_player_icon_android

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
        for (int i = 0; i < mVideoFileInfoList.size(); i++) {
            VideoFileInfo videoFileInfo = mVideoFileInfoList.get(i);    // read audio file
            if (requestedFilename.equals(videoFileInfo.getDisplayName())) {
                return i;          // return matched index
            }
        }
        return 0;                  // default is the first title
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // 커서 로더 생성
        // 모든 Video 데이터 취득
        return new CursorLoader(getApplicationContext(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // 화면 갱신
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 로더 파괴
        getSupportLoaderManager().destroyLoader(0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCurrentPosition = position;
        Intent initialIntent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
        initialIntent.putExtra("currentPosition", mCurrentPosition);       // current title position
        initialIntent.putParcelableArrayListExtra("videoInfoList", mVideoFileInfoList);
        startActivity(initialIntent);
    }
}
