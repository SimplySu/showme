package com.suwonsmartapp.hello.showme.file;

public class FileManagerInfo {

    // 초기화면에 표시할 데이터 (아이콘, 이름, 경로명)
    private int iconName;
    private String folderName;
    private String folderPath;

    public FileManagerInfo() { }
    public int getIconName() { return iconName; }
    public void setIconName(int iconName) { this.iconName = iconName; }
    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

}
