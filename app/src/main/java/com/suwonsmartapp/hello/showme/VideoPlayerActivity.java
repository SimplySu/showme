package com.suwonsmartapp.hello.showme;

import android.content.ActivityNotFoundException;
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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;

public class VideoPlayerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static String TAG = "VideoPlayerActivity ";
    private void showLog(String msg) { Log.d(TAG, msg); }
    void showToast(CharSequence toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private ListView mLvVideoList;
    private VideoAdapter mVideoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoplay);

        // fix the screen for landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mLvVideoList  = (ListView) findViewById(R.id.lv_VidoeList);

        // setting video adapter
        mVideoAdapter = new VideoAdapter(getApplicationContext(), null, true);
        mLvVideoList.setAdapter(mVideoAdapter);

        // setup click event listener
        mLvVideoList.setOnItemClickListener(this);
        mLvVideoList.setOnItemLongClickListener(this);

        // initialize loader
        getSupportLoaderManager().initLoader(0, null, this);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getApplicationContext(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mVideoAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mVideoAdapter.swapCursor(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSupportLoaderManager().destroyLoader(0);
    }

    // 리스트뷰 클릭 이벤트 메소드 (액티비티 바로 실행)
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor mCursor = (Cursor)mVideoAdapter.getItem(i);
        String mData = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));

        Intent mIntent = new Intent(getApplicationContext(), VideoPlayer.class);
        mIntent.setData(Uri.parse(mData));
        startActivity(mIntent);

    }

    // 리스트뷰 롱클릭 이벤트 메소드 (액티비티 선택 실행)
    // 액티비티 호출할때 액티비티 포 리절트로 호출하고
    // 돌려줄때 셋리절트로 돌려준다.
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor mCursor = (Cursor)mVideoAdapter.getItem(i);
        String mData = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));

        try {
            Intent mIntent = new Intent(Intent.ACTION_VIEW);
            mIntent.setDataAndType(Uri.parse(mData), "video/*");
            startActivity(Intent.createChooser(mIntent, "실행할 앱을 선택하세요."));
//            startActivity(mIntent);
        } catch (ActivityNotFoundException e) {
            showLog("Application");
        }
        return true;
    }
}
