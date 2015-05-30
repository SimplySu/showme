package com.suwonsmartapp.hello.showme;

class VideoPlayerSmi {
	long time;
	String text;

	VideoPlayerSmi(long time, String text) {
		this.time = time;
		this.text = text;
	}
	
	public long getTime() {
		return time;
	}
	
	public String getText() {
		return text;
	}
}

