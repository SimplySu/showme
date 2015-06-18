package com.suwonsmartapp.hello.showme.image;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.suwonsmartapp.hello.R;
import com.suwonsmartapp.hello.showme.photoview.PhotoViewAttacher;

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

    private String value;                                   // filename passed by file manager

    public static final int RESULT_OK = 0x0fff;
    public static final int REQUEST_CODE_AUDIO = 0x0001;
    public static final int REQUEST_CODE_AUDIO_PLAYER = 0x0002;
    public static final int REQUEST_CODE_VIDEO = 0x0010;
    public static final int REQUEST_CODE_VIDEO_PLAYER = 0x0020;
    public static final int REQUEST_CODE_IMAGE = 0x0100;
    public static final int REQUEST_CODE_IMAGE_PLAYER = 0x0200;
    private Bundle imageFileListService;
    private Intent intentImageService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // delete title bar and use full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);     // for AppCompatActivity

        setContentView(R.layout.image_file_list);
//        showLog("onCreate");

        // fix the screen for portrait
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        readIntent();                       // get pathname and filename

        mImageFileInfoList = new ArrayList<>();         // create image file lists

        prepareFileToShow();               // setup files for showing

        mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setOffscreenPageLimit(1);
        mCurrentPosition = searchPictureIndex();      // search picture index which was specified by user
//        showLog("returned index : " + mCurrentPosition);

        mMyAdapter = new MyAdapter(getSupportFragmentManager(), mImageFileInfoList);
        mViewPager.setAdapter(mMyAdapter);
        mViewPager.setCurrentItem(mCurrentPosition);        // setup position after adapter established
    }

    private void readIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("FilePath")) {
                value = intent.getStringExtra("FilePath");
//                showLog(value);
            } else {
                showToast(getString(R.string.msg_wrong_file));
                finish();
            }
            requestedPathname = value.substring(0, value.lastIndexOf('/'));
            requestedFilename = value.substring(value.lastIndexOf('/') + 1, value.length());
        }
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

//        showLog("query result : " + String.valueOf(mCursor));

        mImageFileInfoList = new ArrayList<>();     // initialize info list

        if (mCursor != null) {
            mCursor.moveToFirst();              // from the start of data base

//            showLog("searched file count : " + String.valueOf(mCursor.getCount()));

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
            showToast(getString(R.string.msg_no_image));          // no image found
        }
    }

    // return true if current file's directory is matching with user selection,
    // return false if it is not.
    // we will include subdirectories also.
    private boolean isDirectoryMatch() {
        String fullPath = mCursor.getString(2);         // get full path name
        String pathname = fullPath.substring(0, fullPath.lastIndexOf('/'));     // get pathname only
        String filename = fullPath.substring(fullPath.lastIndexOf('/') + 1, fullPath.length()); // get filename only

//        showLog(filename);

        if (pathname.length() < requestedPathname.length()) {        // if current pathname is shorter than requested
            return false;                               // we don't need to compare it
        }

        String s = pathname.substring(0, requestedPathname.length()); // compare just we requested for subdirectory
        return s.equals(requestedPathname);             // see if this directory is matching ?
    }

    // search matched title with specified by user
    private int searchPictureIndex() {
//        showLog("Picture count : " + mImageFileInfoList.size());
        for (int i = 0; i < mImageFileInfoList.size(); i++) {
            ImageFileInfo imageFileInfo = mImageFileInfoList.get(i);    // read image file
//            showLog("requested filename : " + requestedFilename);
//            showLog("we found : " + imageFileInfo.getDisplayName());
//            showLog("current index : " + i);
            if (requestedFilename.equals(imageFileInfo.getDisplayName())) {
                return i;          // return matched index
            }
        }
        return 0;                  // default is the first picture
    }

    @Override
    protected void onDestroy() {
        imageFileListService = new Bundle();
        intentImageService = new Intent();
        imageFileListService.putInt("CurrentPosition", mCurrentPosition);
        intentImageService.putExtras(imageFileListService);
        this.setResult(RESULT_OK, intentImageService);

        super.onDestroy();
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

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            ((Fragment) object).getFragmentManager().beginTransaction().remove((Fragment)object).commit();
        }
    }

    public static class ImageFragment extends Fragment {
        PhotoViewAttacher mAttacher;
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

            mAttacher = new PhotoViewAttacher(mImageView);
            mAttacher.setScaleType(ImageView.ScaleType.FIT_CENTER);        // FIT_XY will expand picture

            ImageFileInfo imageinfo = getArguments().getParcelable("imageinfo");
            mImageView.setImageURI(imageinfo.getImageUri());
            return rootView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            ((BitmapDrawable) mImageView.getDrawable()).getBitmap().recycle();
        }
    }
}
