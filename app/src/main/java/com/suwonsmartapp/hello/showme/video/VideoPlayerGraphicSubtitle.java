package com.suwonsmartapp.hello.showme.video;

class VideoPlayerGraphicSubtitle {
    long time;
    long filepos;

    VideoPlayerGraphicSubtitle(long time, long filepos) {
        this.time = time;
        this.filepos = filepos;
    }

    public long getTime() { return time; }

    public void setTime(long time) { this.time = time; }

    public long getFilepos() { return filepos; }

    public void setFilepos(long filepos) { this.filepos = filepos; }

}
