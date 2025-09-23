package com.example.cpumonitor.Viewmodel;

import android.graphics.drawable.Drawable;

public class AppRunningItem {
    public String name;
    public Drawable icon;
    public String packageName;
    public long timeForeground = 0L;
    public AppRunningItem(String name, Drawable icon, String packageName, long timeForeground) {
        this.name = name;
        this.icon = icon;
        this.packageName = packageName;
        this.timeForeground = timeForeground;
    }
}
