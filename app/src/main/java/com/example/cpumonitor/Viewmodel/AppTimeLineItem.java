package com.example.cpumonitor.Viewmodel;

import android.graphics.drawable.Drawable;

public class AppTimeLineItem {
    public String appName;
    public Drawable icon;
    public String Timeline;
    public long TimeDuration;

    public String _package;

    public AppTimeLineItem(String appName, Drawable icon, String time, long timestamp, String _package) {
        this.appName = appName;
        this.icon = icon;
        this.Timeline = time;
        this.TimeDuration = timestamp;
        this._package = _package;
    }
}
