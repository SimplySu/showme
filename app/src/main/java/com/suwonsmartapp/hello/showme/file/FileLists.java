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
        mPath = path;                               // ��û���� ���(path)�� ������.
        mMode = mode;                               // 0 = all, 1 = audio, 2 = image, 3 = video
        File currentPath = new File(path);          // ���� ��θ� �ؿ� �ִ�
        File[] files = currentPath.listFiles();     // ��� ���� ����Ʈ�� �˻���.

        ArrayList<FileInfo> mFileInfo = new ArrayList<>();     // ������� ������ ��� �Ҵ�.

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];                           // ��ο� �ִ� ��� ������ �˻��ؼ�
                if (modeMatch(file, mMode)) {                   // ��尡 �´� �͸� �߷���
                    if (matchFolder(file)) {                    // ��ΰ� ������ ����� ���Խ�Ŵ.
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setFile(file);                         // ���� ���� ��ü
                        fileInfo.setTitle(file.getAbsolutePath());      // ��θ�
                        fileInfo.setSize(file.length());                // ���� ũ��
                        fileInfo.setModified(file.lastModified());      // ���� ������Ʈ�� ����
                        mFileInfo.add(fileInfo);                        // ���ϴ� 4�� ������ ������
                    }
                }
            }
            Collections.sort(mFileInfo, mAscComparator);            // ���� �������� �����ϰ�
            Collections.sort(mFileInfo, mFolderAscComparator);      // ���丮�� �Ǿ����� ����
        }

    return mFileInfo;       // ����� ������. ������� null�� ���� ����.
    }

    // ���� ���� ��� : �������� ����.
    Comparator<FileInfo> mAscComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            String left = lhs.getTitle();
            String right = rhs.getTitle();
            return left.compareTo(right);
        }
    };

    // ����, ������
    // file, file = 0 ���� : ��ȭ ����
    // file, directory = 1 ���� : �¿� �ٲ�
    // directory, file = -1 ���� : �¿� �ٲ�
    // directory, directory = 0 ���� : ��ȭ ����

    // ���丮�� ��� ����Ʈ�� �� ���� ������.
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

    // ��忡 ���� ���ϸ� �����Ͽ� ������. (true = ���� ����, false = ���� �ٸ�)
    private boolean modeMatch(File file, int mode) {

        String[] audio = {"mp3", "ogg", "wav", "flac", "mid", "m4a", "wma"};
        String[] video = {"avi", "mkv", "mp4", "wmv", "asf", "mov", "mpg", "flv", "tp", "3gp",
                "m4v", "rmvb", "webm", "smi", "srt", "sub", "idx", "ass", "ssa"};
        String[] image = {"jpg", "jpeg", "gif", "png", "bmp", "tif", "tiff", "webp"};

        // ��� ������ ���Ǹ� �׻� true��.
        if (mode == MODEall) {
            return true;
        }

        // Ȯ���ڰ� ������ �׻� false��.
        int result = file.getName().lastIndexOf('.');
        if ((result == -1) || (result == 0)) {
            return false;
        }

        // ���ϸ����κ��� Ȯ���ڸ� ������.
        int length = file.getName().length();
        String ext = file.getName().substring(result + 1, length).toLowerCase();

        // ����� �������� �˻���.
        if (mode == MODEaudio) {
            for (int i = 0; i < audio.length; i++) {
                if (ext.equals(audio[i])) {
                    return true;
                }
            }
            return false;

        // �׸� �������� �˻���.
        } else if (mode == MODEimage) {
            for (int i = 0; i < image.length; i++) {
                if (ext.equals(image[i])) {
                    return true;
                }
            }
            return false;

        // ���� �������� �˻���.
        } else if (mode == MODEvideo) {
            for (int i = 0; i < video.length; i++) {
                if (ext.equals(video[i])) {
                    return true;
                }
            }
            return false;

        // �����, �׸�, ������ �ƴ� ��� all �� ������.
        } else {
            return true;
        }
    }

    // ������ ������ ��û�� ���丮�� ���� ��츸 true�� ������.
    private boolean matchFolder(File file) {
        String fullPath = file.getAbsolutePath();
        int index = fullPath.lastIndexOf('/');

        // ��Ʈ�̸� ������ "/"�� ����� ��.
        if (index == 0) {
            index = 1;
        }

        // ���� ��θ��� ��û�� ��θ��� ª���� ���� ���丮�� ���ɼ��� ����.
        String pathname = fullPath.substring(0, index);
        if (pathname.length() < mPath.length()) {
            return false;
        }

        // ���� ���丮���� �˻���.
        String s = pathname.substring(0, mPath.length());
        return s.equals(mPath);     // ���丮���� ������ �˻��� ����� ������.
    }
}
