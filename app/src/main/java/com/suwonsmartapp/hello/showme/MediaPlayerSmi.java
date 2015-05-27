package com.suwonsmartapp.hello.showme;

class MediaPlayerSmi {
	long time;
	String text;
	MediaPlayerSmi(long time, String text) {
		this.time = time;
		this.text = text;
	}
	
	public long gettime() {
		return time;
	}
	
	public String gettext() {
		return text;
	}
}

