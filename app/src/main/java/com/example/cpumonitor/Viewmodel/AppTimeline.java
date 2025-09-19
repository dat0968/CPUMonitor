package com.example.cpumonitor.Viewmodel;

import android.graphics.drawable.Drawable;

public class AppTimeline {
    public String appName;
    public Drawable icon;
    public String time;
    public long timestamp;

    public AppTimeline(String appName, Drawable icon, String time, long timestamp) {
        this.appName = appName;
        this.icon = icon;
        this.time = time;
        this.timestamp = timestamp;
    }
}
