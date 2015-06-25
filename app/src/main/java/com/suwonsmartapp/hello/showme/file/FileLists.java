package com.suwonsmartapp.hello.showme.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FileLists {

    private String mPath;
    private int mMode;
    private final int MODEall = 0;
    private final int MODEaudio = 1;
    private final int MODEimage = 2;
    private final int MODEvideo = 3;

    public ArrayList<FileInfo> getFileList(String path, int mode) {
        mPath = path;                           // requested path
        mMode = mode;                           // 0 = all, 1 = audio, 2 = image, 3 = video
        File currentPath = new File(path);
        File[] files = currentPath.listFiles();

        ArrayList<FileInfo> mFileInfo = new ArrayList<>();     // initialize info list

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (modeMatch(file, mMode)) {
                    if (matchFolder(file)) {                            // select matched directory only
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setFile(file);                         // file information
                        fileInfo.setTitle(file.getAbsolutePath());      // full pathname
                        fileInfo.setSize(file.length());                // file size
                        fileInfo.setModified(file.lastModified());      // file date
                        mFileInfo.add(fileInfo);                        // register a file list
                    }
                }
            }
            Collections.sort(mFileInfo, mAscComparator);
            Collections.sort(mFileInfo, mFolderAscComparator);
        }

    return mFileInfo;       // could be null
    }

    Comparator<FileInfo> mAscComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            String left = lhs.getTitle();
            String right = rhs.getTitle();
            return left.compareTo(right);
        }
    };

    // left, right
    // file, file = return 0 : not change
    // file, directory = return 1 : change
    // directory, file = return -1 : change
    // directory, directory = return 0 : not change

    Comparator<FileInfo> mFolderAscComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            if (!lhs.getFile().isDirectory() && rhs.getFile().isDirectory()) {
                return 1;
            } else if (lhs.getFile().isDirectory() && !rhs.getFile().isDirectory()) {
                return -1;
            }
            return 0;
        }
    };

    private boolean modeMatch(File file, int mode) {

        String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "wma"};
        String[] video = {"avi", "mkv", "mp4", "wmv", "asf", "mov", "mpg", "flv", "tp", "3gp",
                "m4v", "rmvb", "webm", "smi", "srt", "sub", "idx", "ass", "ssa"};
        String[] image = {"jpg", "jpeg", "gif", "png", "bmp", "tif", "tiff", "webp"};

        if (mode == MODEall) {
            return true;
        }

        int result = file.getName().lastIndexOf('.');
        if ((result == -1) || (result == 0)) {
            return false;
        }

        int length = file.getName().length();
        String ext = file.getName().substring(result + 1, length).toLowerCase();

        if (mode == MODEaudio) {
            for (int i = 0; i < audio.length; i++) {
                if (ext.equals(audio[i])) {
                    return true;
                }
            }
            return false;
        } else if (mode == MODEimage) {
            for (int i = 0; i < image.length; i++) {
                if (ext.equals(image[i])) {
                    return true;
                }
            }
            return false;
        } else if (mode == MODEvideo) {
            for (int i = 0; i < video.length; i++) {
                if (ext.equals(video[i])) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    // return true if current file's directory is matching with user selection,
    // return false if it is not.
    // we will include subdirectories also.
    private boolean matchFolder(File file) {
        String fullPath = file.getAbsolutePath();           // get full path name
        int index = fullPath.lastIndexOf('/');

        if (index == 0) {                                   // if root, make "/"
            index = 1;
        }

        String pathname = fullPath.substring(0, index);     // get pathname only
        if (pathname.length() < mPath.length()) {           // if current pathname is shorter than requested
            return false;                                   // we don't need to compare it
        }

        String s = pathname.substring(0, mPath.length());   // compare just we requested for subdirectory
        return s.equals(mPath);                             // see if this directory is matching ?
    }
}
