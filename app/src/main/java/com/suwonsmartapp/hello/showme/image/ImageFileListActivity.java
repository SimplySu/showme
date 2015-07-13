package com.suwonsmartapp.hello.showme.image;

import android.content.Intent;
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

    // 인텐트를 통해 받은 경로명과 파일명.
    private String requestedPathname = "";
    private String requestedFilename = "";

    // -1은 파일이 특정되지 않았음을 나타냄. (초기값)
    private static int mCurrentPosition = -1;

    private ViewPager mViewPager;
    private MyAdapter mMyAdapter;

    // 파일 매니저를 통해 건네받은 파일명.
    private String value;

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
        // 타이틀바를 지우고 전체 스크린을 사용함.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);     // AppCompatActivity의 경우 순서가 중요함.

        setContentView(R.layout.image_file_list);

        // 인텐트를 통해 경로명과 파일명을 읽음.
        readIntent();

        // 표시할 그림 파일만을 추출함.
        prepareFileToShow(requestedPathname);

        mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setOffscreenPageLimit(1);

        // 특정 파일을 지정한 경우 여기부터 표시함.
        mCurrentPosition = searchPictureIndex(requestedFilename);

        // 표시할 파일 리스트를 작성하여 어댑터에 전달함.
        mMyAdapter = new MyAdapter(getSupportFragmentManager(), imageList);
        mViewPager.setAdapter(mMyAdapter);

        // 지정한 위치를 세팅함.
        mViewPager.setCurrentItem(mCurrentPosition);
    }

    // 인텐트를 통해 경로명과 파일명을 읽음.
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

    // 표시 가능한 그림 파일만 리스트로 만듬.
    private void prepareFileToShow(String rp) {
        imageList = new FileLists().getFileList(rp, MODEimage);
        if (imageList == null) {
            showToast(getString(R.string.msg_no_image));          // 표시할 파일이 없음.
        }
    }

    // 지정한 파일이 표시 가능한지 검사함.
    private int searchPictureIndex(String rf) {
        for (int i = 0; i < imageList.size(); i++) {
            FileInfo fileInfo = imageList.get(i);
            File f = fileInfo.getFile();
            if (rf.equals(f.getName())) {
                return i;          // 일치하는 인덱스를 리턴함.
            }
        }
        return 0;                  // 일치하는 파일이 없으면 처음부터 표시함.
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

    // 플래그먼트를 사용하여 그림 파일을 표시하는 어댑터
    public class MyAdapter extends FragmentPagerAdapter {
        private ArrayList<FileInfo> mData;
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
            ((Fragment) object).getFragmentManager()
                    .beginTransaction().remove((Fragment)object).commit();
        }
    }

    public static class ImageFragment extends Fragment {
        PhotoViewAttacher mAttacher;
        private ImageView mImageView;

        // 싱글톤(Singleton) 패턴 : 하나의 인스턴스를 생성하여 어디서든 엑세스하게 함.
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

            // 핀치 줌, 탭을 사용하게 하기 위해 Attacher에 한 번 건네줌. (photoview)
            mAttacher = new PhotoViewAttacher(mImageView);
            mAttacher.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mAttacher.update();

            FileInfo imageinfo = getArguments().getParcelable("imageinfo");
            mImageView.setImageBitmap(BitmapFactory.decodeFile(imageinfo.getTitle()));
            return rootView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            ((BitmapDrawable) mImageView.getDrawable()).getBitmap().recycle();
        }
    }
}
