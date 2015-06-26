package com.suwonsmartapp.hello.showme.file;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.suwonsmartapp.hello.R;

import java.util.ArrayList;

public class FileManagerAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<FileManagerInfo> mTitleList;

    public FileManagerAdapter(Context context, ArrayList<FileManagerInfo> list) {
        mContext = context;
        mTitleList = list;
    }

    @Override
    public int getCount() {
        return mTitleList.size();
    }

    @Override
    public Object getItem(int position) {
        return mTitleList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // 처음으로 뷰와 데이터를 로드할 경우
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.file_manager_adapter, null);
            ImageView icon = (ImageView) convertView.findViewById(R.id.file_manager_icon);
            TextView foldername = (TextView) convertView.findViewById(R.id.file_manager_foldername);
            TextView folderpath = (TextView) convertView.findViewById(R.id.file_manager_folderpath);

            holder = new ViewHolder();
            holder.vhIcon = icon;
            holder.vhName = foldername;
            holder.vhPath = folderpath;

            convertView.setTag(holder);
        } else {
            // reuse View, and Data
            holder = (ViewHolder) convertView.getTag();
        }

        // 아이콘 표시
        FileManagerInfo icon = (FileManagerInfo) getItem(position);
        holder.vhIcon.setImageResource(icon.getIconName());

        // 이름 표시
        FileManagerInfo name = (FileManagerInfo) getItem(position);
        holder.vhName.setText(name.getFolderName());

        // 경로명 표시
        FileManagerInfo path = (FileManagerInfo) getItem(position);
        holder.vhPath.setText(path.getFolderPath());

        // 완성된 뷰를 리턴함.
        return convertView;
    }

    // ViewHolder 데이터 패턴
    static class ViewHolder {
        ImageView vhIcon;
        TextView vhName;
        TextView vhPath;
    }
}
