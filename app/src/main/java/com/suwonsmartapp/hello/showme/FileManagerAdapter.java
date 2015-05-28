package com.suwonsmartapp.hello.showme;

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
            // View 를 처음 로딩할 때, Data 를 처음 셋팅할 때
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.file_manager_main, null);
            ImageView icon = (ImageView) convertView.findViewById(R.id.file_manager_icon);
            TextView foldername = (TextView) convertView.findViewById(R.id.file_manager_foldername);
            TextView folderpath = (TextView) convertView.findViewById(R.id.file_manager_folderpath);

            holder = new ViewHolder();
            holder.vhIcon = icon;
            holder.vhName = foldername;
            holder.vhPath = folderpath;

            convertView.setTag(holder);
        } else {
            // View, Data 재사용
            holder = (ViewHolder) convertView.getTag();
        }

        FileManagerInfo icon = (FileManagerInfo) getItem(position);
        holder.vhIcon.setImageResource(icon.getIconName());

        FileManagerInfo name = (FileManagerInfo) getItem(position);
        holder.vhName.setText(name.getFolderName());

        FileManagerInfo path = (FileManagerInfo) getItem(position);
        holder.vhPath.setText(path.getFolderPath());

        return convertView;
    }

    // ViewHolder 패턴
    static class ViewHolder {
        ImageView vhIcon;
        TextView vhName;
        TextView vhPath;
    }
}
