package com.suwonsmartapp.hello.showme;

//import android.view.Menu;
//import android.view.MenuInflater;


class MediaPlayerList {
	int type;
	String name;
	long size;
	MediaPlayerList(int type, String name, long size) {
		this.type = type;
		this.name = name;
		this.size = size;
	}
	
	public int gettype() {
		return type;
	}
	
	public String getname() {
		return name;
	}
	
	public long getsize() {
		return size;
	}
}

