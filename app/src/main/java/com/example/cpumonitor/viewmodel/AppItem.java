package com.example.cpumonitor.viewmodel;

import android.graphics.drawable.Drawable;

public class AppItem {
    public String name;
    public Drawable icon;
    public String packageName;
    public long timeForeground = 0L;
    public AppItem() {}
    public AppItem(String name, Drawable icon, String packageName, long timeForeground) {
        this.name = name;
        this.icon = icon;
        this.packageName = packageName;
        this.timeForeground = timeForeground;
    }
}
