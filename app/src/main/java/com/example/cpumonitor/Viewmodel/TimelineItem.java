package com.example.cpumonitor.Viewmodel;

public class TimelineItem {
    private boolean isHeader;
    private String date;
    private AppTimeline timeline;
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
