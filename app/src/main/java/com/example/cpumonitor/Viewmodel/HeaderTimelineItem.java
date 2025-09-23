package com.example.cpumonitor.Viewmodel;

import java.util.List;

public class HeaderTimelineItem {
    public String dateStart;
    public boolean isExpanded;
    public List<AppTimeLineItem> appTimelineListItem;
    public HeaderTimelineItem(String dateStart, List<AppTimeLineItem> appTimelineListItem) {
        this.dateStart = dateStart;
        this.appTimelineListItem = appTimelineListItem;
    }
}
