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
        mPath = path;                               // 요청받은 경로(path)를 저장함.
        mMode = mode;                               // 0 = all, 1 = audio, 2 = image, 3 = video
        File currentPath = new File(path);          // 현재 경로명 밑에 있는
        File[] files = currentPath.listFiles();     // 모든 파일 리스트를 검색함.

        ArrayList<FileInfo> mFileInfo = new ArrayList<>();     // 결과값을 전달할 어레이 할당.

        if (files != null) {
            for (File file : files) {
                if (modeMatch(file, mMode)) {                   // 모드가 맞는 것만 추려서
                    if (matchFolder(file)) {                    // 경로가 같으면 결과에 포함시킴.
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setFile(file);                         // 파일 정보 자체
                        fileInfo.setTitle(file.getAbsolutePath());      // 경로명
                        fileInfo.setSize(file.length());                // 파일 크기
                        fileInfo.setModified(file.lastModified());      // 최종 업데이트된 날자
                        mFileInfo.add(fileInfo);                        // 파일당 4개 정보를 저장함
                    }
                }
            }
            Collections.sort(mFileInfo, mAscComparator);            // 내림 차순으로 정렬하고
            Collections.sort(mFileInfo, mFolderAscComparator);      // 디렉토리는 맨앞으로 보냄
        }

    return mFileInfo;       // 결과를 리턴함. 결과값이 null일 수도 있음.
    }

    // 파일 정렬 방법 : 내림차순 정렬.
    Comparator<FileInfo> mAscComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            String left = lhs.getTitle();
            String right = rhs.getTitle();
            return left.compareTo(right);
        }
    };

    // 왼쪽, 오른쪽
    // file, file = 0 리턴 : 변화 없음
    // file, directory = 1 리턴 : 좌우 바꿈
    // directory, file = -1 리턴 : 좌우 바꿈
    // directory, directory = 0 리턴 : 변화 없음

    // 디렉토리인 경우 리스트의 맨 위로 정렬함.
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

    // 모드에 따른 파일만 추출하여 리턴함. (true = 모드와 동일, false = 모드와 다름)
    private boolean modeMatch(File file, int mode) {

        String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "wma"};
        String[] video = {"avi", "mkv", "mp4", "wmv", "asf", "mov", "mpg", "flv", "tp", "3gp",
                "m4v", "rmvb", "webm", "smi", "srt", "sub", "idx", "ass", "ssa"};
        String[] image = {"jpg", "jpeg", "gif", "png", "bmp", "tif", "tiff", "webp"};

        // 모든 파일이 허용되면 항상 true임.
        if (mode == MODEall) {
            return true;
        }

        // 확장자가 없으면 항상 false임.
        int result = file.getName().lastIndexOf('.');
        if ((result == -1) || (result == 0)) {
            return false;
        }

        // 파일명으로부터 확장자를 추출함.
        int length = file.getName().length();
        String ext = file.getName().substring(result + 1, length).toLowerCase();

        // 오디오 파일인지 검사함.
        if (mode == MODEaudio) {
            for (String anAudio : audio) {
                if (ext.equals(anAudio)) {
                    return true;
                }
            }
            return false;

        // 그림 파일인지 검사함.
        } else if (mode == MODEimage) {
            for (String anImage : image) {
                if (ext.equals(anImage)) {
                    return true;
                }
            }
            return false;

        // 비디오 파일인지 검사함.
        } else if (mode == MODEvideo) {
            for (String aVideo : video) {
                if (ext.equals(aVideo)) {
                    return true;
                }
            }
            return false;

        // 오디오, 그림, 비디오가 아닌 경우 all 로 간주함.
        } else {
            return true;
        }
    }

    // 지정한 파일이 요청된 디렉토리와 같은 경우만 true로 리턴함.
    private boolean matchFolder(File file) {
        String fullPath = file.getAbsolutePath();
        int index = fullPath.lastIndexOf('/');

        // 루트이면 강제로 "/"로 만들어 줌.
        if (index == 0) {
            index = 1;
        }

        // 현재 경로명이 요청된 경로명보다 짧으면 같은 디렉토리일 가능성이 없음.
        String pathname = fullPath.substring(0, index);
        if (pathname.length() < mPath.length()) {
            return false;
        }

        // 서브 디렉토리명을 검사함.
        String s = pathname.substring(0, mPath.length());
        return s.equals(mPath);     // 디렉토리명이 같은지 검사한 결과를 리턴함.
    }
}
