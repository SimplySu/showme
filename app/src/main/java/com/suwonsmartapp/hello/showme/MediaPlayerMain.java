package com.suwonsmartapp.hello.showme;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.suwonsmartapp.hello.R;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;

public class MediaPlayerMain extends Activity {
    private TextView dirText;
    private ArrayList<MediaPlayerList> arrayList;
    private ListView fileList;
    private GroupAdapter adapter;
    private String nPath;
    private ArrayList<String> PathList_prev;
    private ArrayList<String> PathList_next;
    private View nextBtn;
    private View prevBtn;
    private View upBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_player_main);

        nPath = null;
        PathList_prev = new ArrayList<>();
        PathList_next = new ArrayList<>();

        makeFileList();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	setContentView(R.layout.media_player_main);

    	makeFileList();
    }

    public void makeFileList() {
        fileList = (ListView)findViewById(R.id.listView1);
        dirText = (TextView)findViewById(R.id.dirText);

		final Builder virsionDlg = new Builder(this)
        .setTitle("패치노트")
        .setMessage(
        		"[v0.2]\n" +
        		"- smi파일 색상 태그 적용.\n" +
        		"- 동영상 탐색시, 자막도 함께 이동.\n" +
        		"\n" +
        		"[v0.1]\n" +
        		"- 동영상 읽어와서 재생하기\n" +
        		"- smi 파일 읽어와서 파싱하기\n" +
        		"- 파싱한 smi를 동영상 위에 오버랩하기\n" +
        		"- 파일 브라우저 표시하기\n" +
        		"\n" +
        		"[패치예정]\n" +
        		"- 북마크&이어보기 기능 추가.\n" +
        		"- 웹에 있는 파일 스트리밍.\n" +
        		"- 웹에 있는 파일 스트리밍 할 때 같은 주소, 같은 이름의 smi파일을 불러오기.\n" +
        		"- 플레이어 세부 설정 메뉴 만들기\n" +
        		"- 앱스토어에 등록하기")
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

        View virsionBtn = findViewById(R.id.imageButton6);
        virsionBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				virsionDlg.show();
			}
        });

		final Builder infoDlg = new Builder(this)
        .setTitle("정보")
        .setMessage(
        		"[GNCPlayer]\n" +
        		"- 제작 : LlIiEe(GsNC).\n" +
        		"- 메일 : nohhplus@nate.com\n" +
        		"- 블로그 : http://gsroom.tistory.com")
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

        View infoBtn = findViewById(R.id.imageButton7);
        infoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				infoDlg.show();
			}
        });

		String path = "";
		String ext = Environment.getExternalStorageState();
		if(ext.equals(Environment.MEDIA_MOUNTED)) {
	    	path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
	    } else {
	    	path = Environment.MEDIA_UNMOUNTED;
	    }
		File dir = new File(path + "/GNCPlayer/");
        if(!dir.isDirectory()) {
        	dir.mkdirs();
    		virsionDlg.show();
        }

		final Builder errorDlg = new Builder(this)
        .setTitle("에러")
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
        View homeBtn = findViewById(R.id.imageButton1);			// monitor
        homeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PathList_prev.add(nPath);
				updateFileList(null);
			}
        });
        
        prevBtn = findViewById(R.id.imageButton2);				// left arrow
        prevBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(PathList_prev.size() > 0) {
					PathList_next.add(nPath);
					nPath = PathList_prev.get(PathList_prev.size()-1);
					PathList_prev.remove(PathList_prev.size()-1);
					updateFileList(nPath);
				}
			}
        });
        
        nextBtn = findViewById(R.id.imageButton4);				// right arrow
        nextBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(PathList_next.size() > 0) {
					PathList_prev.add(nPath);
					nPath = PathList_next.get(PathList_next.size()-1);
					PathList_next.remove(PathList_next.size()-1);
					updateFileList(nPath);
				}
			}
        });
        
        upBtn = findViewById(R.id.imageButton3);				// up arrow
        upBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!nPath.equals("/")) {
					File dir = new File(nPath);
					dir = new File(dir.getParent());
					if(dir.isDirectory() && dir.canRead()) {
						nPath = dir.getAbsolutePath().toString();
						PathList_prev.add(nPath);
						PathList_next.clear();
						if(!nPath.endsWith("/")) {
							nPath += "/";
						}
						updateFileList(nPath);
					} else {
						errorDlg.setMessage("폴더가 없거나 읽을 수 없습니다.").show();
					}
				}
			}
        });
        
        updateFileList(nPath);
        
        fileList.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		MediaPlayerList item = arrayList.get(position);
        		if(item.gettype() == 0) {
        			File dir = new File(nPath + item.getname() + "/");
    				if(dir.isDirectory() && dir.canRead()) {
    					PathList_prev.add(nPath);
    					PathList_next.clear();
    					nPath = dir.getAbsolutePath().toString();
    					if(!nPath.endsWith("/")) {
    						nPath += "/";
    					}
    					updateFileList(nPath);
    				} else {
    					errorDlg.setMessage("폴더가 없거나 읽을 수 없습니다.").show();
    				}
        		} else if(item.gettype() == 1) {
        			File file = new File(nPath + item.getname());
        			if(file.isFile() && file.canRead()) {
 						Intent intent = new Intent(MediaPlayerMain.this, MediaPlayerVideo.class);
 						intent.putExtra("FilePath", file.toString());
 					    startActivity(intent);
 					} else {
    					errorDlg.setMessage("폴더가 없거나 읽을 수 없습니다.").show();
    				}
        		}
        	}
        });
    }
    
	public void updateFileList(String sPath) {
	    String ext = Environment.getExternalStorageState();
	    String path = null;
	    MediaPlayerList temp;
	    int type;
	    long size = 0;
	    String name;
	    
	    if(sPath == null) {
		    if(ext.equals(Environment.MEDIA_MOUNTED)) {
		    	path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
		    } else {
		    	path = Environment.MEDIA_UNMOUNTED;
		    }
	    } else {
	    	path = sPath;
	    }
	    
	    dirText.setText(path);
	    nPath = path;
	
	    File files = new File(path);
	    arrayList = new ArrayList<>();
	
	    if(files.listFiles().length > 0) {
		    for(File file : files.listFiles()) {
		    	name = file.getName();
		    	if(file.isDirectory()) {
		    		type = 0;
		    	} else if(file.getName().endsWith(".mpg")
		    			||file.getName().endsWith(".avi")
		    			||file.getName().endsWith(".wmv")
		    			||file.getName().endsWith(".asf")
		    			||file.getName().endsWith(".mp4")
		    			||file.getName().endsWith(".mkv")
		    			||file.getName().endsWith(".m4v")
		    			||file.getName().endsWith(".3gp")) {
		    		type = 1;
		    	} else if(file.getName().endsWith(".apk")) {
		    		type = 2;
		    	} else {
		    		type = 3;
		    	}
		    	
		    	if(type != 0) {
		    		size = file.length();
		    	}
		    	temp = new MediaPlayerList(type, name, size);
		    	arrayList.add(temp);
		    }
	    }
	    
	    adapter = new GroupAdapter(this,R.layout.media_player_listview,arrayList);
        fileList.setAdapter(adapter);
        
        if(PathList_prev.size() > 0) {
        	prevBtn.setEnabled(true);
        } else {
        	prevBtn.setEnabled(false);
        }
        
        if(PathList_next.size() > 0) {
        	nextBtn.setEnabled(true);
        } else {
        	nextBtn.setEnabled(false);
        }
        
        if(!nPath.equals("/")) {
        	upBtn.setEnabled(true);
		} else {
			upBtn.setEnabled(false);
		}
    }
   
    private class GroupAdapter extends ArrayAdapter<Object> {
    	private ArrayList<MediaPlayerList> item;
    	private MediaPlayerList temp;
    	
    	@SuppressWarnings({ "unchecked", "rawtypes" })
		public GroupAdapter(Context ctx, int resourceID, ArrayList item) {
    		super(ctx, resourceID, item);
    		
    		this.item = item;
    	}
    	
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View v = convertView;
    		if(v == null) {
    			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			v = vi.inflate(R.layout.media_player_listview, null);
    		}
    		temp = item.get(position);
    		
    		if(temp != null) {
    			ImageView icon = (ImageView)v.findViewById(R.id.imageView1);
    			TextView name = (TextView)v.findViewById(R.id.textView1);
    			TextView size = (TextView)v.findViewById(R.id.textView2);
    			switch(temp.gettype()) {
    			case 0:
    				icon.setImageResource(R.drawable.media_player_icon_folder_horizontal);
    				break;
    			case 1:
    				icon.setImageResource(R.drawable.media_player_icon_film);
    				break;
    			case 2:
    				icon.setImageResource(R.drawable.media_player_icon_application_blue);
    				break;
    			default:
    				icon.setImageResource(R.drawable.media_player_icon_document);
    				break;
    			}
    			name.setText(temp.getname());
    			if(temp.gettype() == 0) {
    				size.setText("(폴더)");
    			} else {
    				size.setText("(" + byteTranslater(temp.getsize()) + ")");
    			}
    		}
    		return v;
    	}
    }
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.media_player_main, menu);
    	return true;
    }*/
    
    public static String byteTranslater(long size) {
      	NumberFormat nf = NumberFormat.getIntegerInstance();
      	java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
      	int intSize;
      	int kbyteSize = 1024;
      	double doubleSize;
      	String returnSize;

      	if(size >= (1000 * 1024 * 1024)) {
      		intSize = new Long(size / (1000 * 1024 * 1024)).intValue();
      		return nf.format(intSize) + "GB";
      	} else if (size > (kbyteSize * 1024)) {
      		intSize = (int) (((double) size) / ((double) (kbyteSize * 1024)) * 100);
      		doubleSize = ((double) intSize) / 100;
      		returnSize = df.format(doubleSize);
	      	if (returnSize.lastIndexOf(".") != -1) {
		      	if ("00".equals(returnSize.substring(returnSize.length() - 2, returnSize.length()))) {
			      	returnSize = returnSize.substring(0, returnSize.lastIndexOf("."));
		      	}
	      	}
	      	return returnSize + "MB";
      	} else if (size > kbyteSize) {
      		intSize = new Long(size / kbyteSize).intValue();
      		return nf.format(intSize) + "KB";
      	} else {
      		return "1KB";
      	}
    }
}