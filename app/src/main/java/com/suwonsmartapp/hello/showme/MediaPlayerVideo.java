package com.suwonsmartapp.hello.showme;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.KeyEvent;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.suwonsmartapp.hello.R;

public class MediaPlayerVideo extends Activity {
	private VideoView videoView;
	private TextView subtitle;
	private ArrayList<MediaPlayerSmi> parsedSmi;
	private boolean useSmi;
	private boolean useWeb;
	private int countSmi;
	private String path; 
	private Uri iPath;
	private File smiFile;
	private URL iSmiPath;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoview);
        
        Intent intent = getIntent();
        if(intent.hasExtra("FilePath")) {
        	path = intent.getStringExtra("FilePath");
        	useWeb = false;
        } else {
        	if(intent.getData().getScheme().equals("http")) {
        		iPath = intent.getData();
        		useWeb = true;
        	} else {
        		path = intent.getData().getPath();
        		useWeb = false;
        	}
        }

        videoView = (VideoView)findViewById(R.id.videoView1);
        subtitle = (TextView)findViewById(R.id.subtitle);
        
        if(useWeb == false) {
	        String smiPath = path.substring(0,path.lastIndexOf(".")) + ".smi";
	        smiFile = new File(smiPath);
	        
	        if(smiFile.isFile() && smiFile.canRead()) {
	        	useSmi = true;
	        } else {
	        	useSmi = false;
	        }
        } else {
        	String temp = iPath.toString().substring(0,iPath.toString().lastIndexOf(".")) + ".smi";
        	try {
				iSmiPath = new URL(temp);
				useSmi = true;
			} catch (MalformedURLException e) {
				useSmi = false;
			}
        }
        
        parsedSmi = new ArrayList<MediaPlayerSmi>();
	    try {
	    	BufferedReader in;
	    	if(useWeb == false) {
	    		in = new BufferedReader(new InputStreamReader(
	    			new FileInputStream(new File(smiFile.toString())),"MS949"));
	    	} else {
	    		in = new BufferedReader(new InputStreamReader(iSmiPath.openStream(),"MS949"));
	    	}
	
    		String s;
    	    long time = -1;
    	    String text = null;
    	    boolean smistart = false;
    	    
    	    while ((s = in.readLine()) != null) {
    	    	if(s.contains("<SYNC")) {
    	    		smistart = true;
    	    		if(time != -1) {
    	    			parsedSmi.add(new MediaPlayerSmi(time, text));
    	    		}
    	    		time = Integer.parseInt(s.substring(s.indexOf("=")+1, s.indexOf(">")));
    	    		text = s.substring(s.indexOf(">")+1, s.length());
    	    		text = text.substring(text.indexOf(">")+1, text.length());
    	    	} else {
    	    		if(smistart == true) {
    	    			text += s;
    	    		}
    	    	}
    	    }
    	    
    	    if(smistart == false) {
    	    	useSmi = false;
    	    }
    	    in.close();
    	} catch (IOException e) {
    	    System.err.println(e);
    	    System.exit(1);
    	}

        subtitle.setText("");
        
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        videoView.setMediaController(mediaController);
        if(useWeb == false) {
        	videoView.setVideoPath(path);
        } else {
        	videoView.setVideoURI(iPath);
        }
        videoView.requestFocus();
        videoView.start();
        
        if(useSmi == true) {
        	new Thread(new Runnable() 
            {
            	public void run() 
            	{
            		try {
            			while(true) {
            				Thread.sleep(300);
            				myHandler.sendMessage(myHandler.obtainMessage());
            			}
            		} catch (Throwable t) {
            			// Exit Thread
            		}
            	}
            }).start();
        }
    }
    
    Handler myHandler = new Handler()
    {
    	public void handleMessage(Message msg)
    	{
    		countSmi = getSyncIndex(videoView.getCurrentPosition());
    		subtitle.setText(Html.fromHtml(parsedSmi.get(countSmi).gettext()));
    	}
    };
    
    public int getSyncIndex(long playTime) {
    	int l=0,m,h=parsedSmi.size();
    	
    	while(l <= h) {
    		m = (l+h)/2;
    		if(parsedSmi.get(m).gettime() <= playTime && playTime < parsedSmi.get(m+1).gettime()) {
    			return m;
    		}
    		if(playTime > parsedSmi.get(m+1).gettime()) {
    			l=m+1;
    		} else {
    			h=m-1;
    		}
    	}
    	return 0;
    }
    
    @Override    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		videoView.stopPlayback();
    		videoView.clearFocus();
    		finish();
    	}
    	return false;
    }
}