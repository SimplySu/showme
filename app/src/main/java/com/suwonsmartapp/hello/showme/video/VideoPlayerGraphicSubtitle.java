package com.suwonsmartapp.hello.showme.video;

class VideoPlayerGraphicSubtitle {
    long time;
    int filepos;

    VideoPlayerGraphicSubtitle(long time, int filepos) {
        this.time = time;
        this.filepos = filepos;
    }

    public long getTime() { return time; }

    public void setTime(long time) { this.time = time; }

    public int getFilepos() { return filepos; }

    public void setFilepos(int filepos) { this.filepos = filepos; }

}
