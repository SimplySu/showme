package com.suwonsmartapp.hello.showme.video;

class VideoPlayerTextSubtitle {
	long time;
	String text;

	VideoPlayerTextSubtitle(long time, String text) {
		this.time = time;
		this.text = text;
	}

    // 텍스트 자막 (.smi, .srt, .ass/.ssa) 을 지원하기 위한 자막 데이터 구조.
	public long getTime() { return time; }
	public void setTime(long time) { this.time = time; }

	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
}

