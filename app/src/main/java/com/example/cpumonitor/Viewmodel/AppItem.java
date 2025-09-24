package com.example.cpumonitor.Viewmodel;
import android.graphics.drawable.Drawable;

public class AppItem {
    public String appName;
    public Drawable appIcon; // Icon ứng dụng
    public String packageName; // Package ứng dụng
    public long todayUsage;       // thời gian sử dụng hôm nay (ms)
    public long installTime;      // thời gian cài đặt (timestamp)
    public AppItem() {}
    public AppItem(String appName, Drawable appIcon, String packageName) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.packageName = packageName;
    }
}
