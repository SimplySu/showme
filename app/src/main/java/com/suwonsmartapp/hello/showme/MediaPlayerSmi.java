package com.suwonsmartapp.hello.showme;

class MediaPlayerSmi {
	long time;
	String text;

	MediaPlayerSmi(long time, String text) {
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

