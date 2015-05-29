package com.suwonsmartapp.hello.showme;

class MediaPlayerList {
	int type;
	String name;
	long size;

	MediaPlayerList(int type, String name, long size) {
		this.type = type;
		this.name = name;
		this.size = size;
	}
	
	public int getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public long getSize() {
		return size;
	}
}

