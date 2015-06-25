package com.suwonsmartapp.hello.showme.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import com.suwonsmartapp.hello.showme.file.FileInfo;
import com.suwonsmartapp.hello.showme.file.FileLists;
import com.suwonsmartapp.hello.showme.photoview.PhotoViewAttacher;

import java.io.File;
import java.util.ArrayList;

public class ImageFileListActivity extends AppCompatActivity {

    private static final String TAG = ImageFileListActivity.class.getSimpleName();
    private void showLog(String msg) { Log.d(TAG, msg); }
    private void showToast(String toast_msg) { Toast.makeText(this, toast_msg, Toast.LENGTH_LONG).show(); }

    private ArrayList<FileInfo> imageList;
    private final int MODEall = 0;
    private final int MODEaudio = 1;
    private final int MODEimage = 2;
    private final int MODEvideo = 3;

    private String requestedPathname = "";          // specified pathname by user from intent
    private String requestedFilename = "";          // specified filename by user from intent

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

        prepareFileToShow();               // setup files for showing

        mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setOffscreenPageLimit(1);
        mCurrentPosition = searchPictureIndex();      // search picture index which was specified by user

        mMyAdapter = new MyAdapter(getSupportFragmentManager(), imageList);
        mViewPager.setAdapter(mMyAdapter);
        mViewPager.setCurrentItem(mCurrentPosition);        // setup position after adapter established
    }

    private void readIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("FilePath")) {
                value = intent.getStringExtra("FilePath");
            } else {
                showToast(getString(R.string.msg_wrong_file));
                finish();
            }
            requestedPathname = value.substring(0, value.lastIndexOf('/'));
            requestedFilename = value.substring(value.lastIndexOf('/') + 1, value.length());
        }
    }

    private void prepareFileToShow() {

        imageList = new FileLists().getFileList(requestedPathname, MODEimage);

        if (imageList == null) {
            showToast(getString(R.string.msg_no_image));          // no image found
        }
    }

    // search matched title with specified by user
    private int searchPictureIndex() {
        for (int i = 0; i < imageList.size(); i++) {
            FileInfo fileInfo = imageList.get(i);    // read image file
            File f = fileInfo.getFile();
            if (requestedFilename.equals(f.getName())) {
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
        private ArrayList<FileInfo> mData;    // image file media_player_icon_information list
        public MyAdapter(FragmentManager fm, ArrayList<FileInfo> data) {
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
        public void setData(ArrayList<FileInfo> data) {
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
        public static Fragment getInstance(FileInfo fileInfo) {
            ImageFragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putParcelable("imageinfo", fileInfo);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.image_fragment, container, false);
            mImageView = (ImageView)rootView.findViewById(R.id.iv_image);

            mAttacher = new PhotoViewAttacher(mImageView);
            mAttacher.setScaleType(ImageView.ScaleType.FIT_CENTER); // if we change it to MATRIX, system will die.
            mAttacher.update();

            FileInfo imageinfo = getArguments().getParcelable("imageinfo");
            Bitmap bitmap = BitmapFactory.decodeFile(imageinfo.getTitle());
            mImageView.setImageBitmap(bitmap);
            return rootView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            ((BitmapDrawable) mImageView.getDrawable()).getBitmap().recycle();
        }
    }
}
