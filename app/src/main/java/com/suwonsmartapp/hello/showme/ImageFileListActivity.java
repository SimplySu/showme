package com.suwonsmartapp.hello.showme;

import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;

import java.net.URLDecoder;
import java.util.ArrayList;

public class ImageFileListActivity extends AppCompatActivity {

    private static final String TAG = ImageFileListActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent

    private ImageFileInfo imageFileInfo;                    // image file info getting by cursor
    private ArrayList<ImageFileInfo> mImageFileInfoList;    // image file media_player_icon_information list
    private Cursor mCursor;                                 // cursor for media store searching
    private static int mCurrentPosition = -1;               // -1 means we didn't specify file

    private ViewPager mViewPager;
    private MyAdapter mMyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_file_list);
        showLog("onCreate");

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

        mImageFileInfoList = new ArrayList<>();         // create audio file lists

        prepareFileToShow();               // setup files for showing

        mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mCurrentPosition = searchPictureIndex();      // search title index which was specified by user
        mViewPager.setCurrentItem(mCurrentPosition);

        mMyAdapter = new MyAdapter(getSupportFragmentManager(), mImageFileInfoList);
        mViewPager.setAdapter(mMyAdapter);
    }

    private void prepareFileToShow() {

        String[] projection = {
                MediaStore.Images.Media._ID,             // picture ID
                MediaStore.Images.Media.TITLE,           // full pathname
                MediaStore.Images.Media.DATA,            // full pathname
                MediaStore.Images.Media.DISPLAY_NAME,    // filename only
                MediaStore.Images.Media.SIZE,            // file length
                MediaStore.MediaColumns.DATA             // URI
        };

        String selection = MediaStore.Images.Media.DATA + " like ?";
        String sortOrder = MediaStore.Images.Media.DISPLAY_NAME + " ASC";

        mCursor = getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,    // The content URI of the words table
                        projection,                 // The columns to return for each row
                        selection,                  //  selection criteria
                        new String[] {requestedPathname + "/%"},        // Selection criteria
                        sortOrder);                 // The sort order for the returned rows

        showLog("query result : " + String.valueOf(mCursor));

        mImageFileInfoList = new ArrayList<>();     // initialize info list

        if (mCursor != null) {
            mCursor.moveToFirst();              // from the start of data base

            showLog("searched file count : " + String.valueOf(mCursor.getCount()));

            for (int i = 0; i < mCursor.getCount(); i++) {
                mCursor.moveToPosition(i);      // get next row of data base

                if (isDirectoryMatch()) {      // select matched directory only
                    imageFileInfo = new ImageFileInfo();
                    imageFileInfo.setId(mCursor.getLong(0));                // file ID
                    imageFileInfo.setTitle(mCursor.getString(1));           // filename
                    imageFileInfo.setData(mCursor.getString(2));            // full pathname
                    imageFileInfo.setDisplayName(mCursor.getString(3));     // filename
                    imageFileInfo.setSize(mCursor.getLong(4));              // file size
                    imageFileInfo.setUriData(mCursor.getString(5));         // URI data

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageFileInfo.getId());
                    imageFileInfo.setImageUri(contentUri);                   // get image media_player_icon_android

                    mImageFileInfoList.add(imageFileInfo);                  // register image on the play list
                }
            }
        } else {
            showToast("표시할 파일이 없습니다.");          // no image found
        }
    }

    // return true if current file's directory is matching with user selection,
    // return false if it is not.
    // we will include subdirectories also.
    private boolean isDirectoryMatch() {
        String fullPath = mCursor.getString(2);         // get full path name
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
    private int searchPictureIndex() {
        for (int i = 0; i < mImageFileInfoList.size(); i++) {
            ImageFileInfo imageFileInfo = mImageFileInfoList.get(i);    // read image file
            if (requestedFilename.equals(imageFileInfo.getDisplayName())) {
                return i;          // return matched index
            }
        }
        return 0;                  // default is the first picture
    }

    // custom adapter for displaying image file using fragment method
    public class MyAdapter extends FragmentPagerAdapter {

        private ArrayList<ImageFileInfo> mData;    // image file media_player_icon_information list

        public MyAdapter(FragmentManager fm, ArrayList<ImageFileInfo> data) {
            super(fm);
            mData = data;
        }

        @Override
        public Fragment getItem(int position) {
            return ImageFragment.getInstance(mData.get(position));
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        public void setData(ArrayList<ImageFileInfo> data) {
            mData = data;
        }
    }

    public static class ImageFragment extends Fragment {

        private ImageView mImageView;

        // Singleton Pattern : make just one instance, and can be accessed at everywhere
        public static Fragment getInstance(ImageFileInfo imageFileInfo) {
            ImageFragment fragment = new ImageFragment();

            Bundle args = new Bundle();
            args.putParcelable("imageinfo", imageFileInfo);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.image_fragment, container, false);
            mImageView = (ImageView)rootView.findViewById(R.id.iv_image);

            ImageFileInfo imageinfo = getArguments().getParcelable("imageinfo");
            mImageView.setImageURI(imageinfo.getImageUri());

            return rootView;
        }
    }
}
