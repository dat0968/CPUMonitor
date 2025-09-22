package com.example.cpumonitor.Viewmodel;

public class TimelineItem {
    public boolean isHeader;
    public String date;
    public AppTimeline timeline;
    // Constructor cho header
    public TimelineItem(String date) {
        this.isHeader = true;
        this.date = date;
    }

    // Constructor cho timeline
    public TimelineItem(AppTimeline timeline) {
        this.isHeader = false;
        this.timeline = timeline;
    }
}
