package com.example.cpumonitor.Viewmodel;
import android.graphics.drawable.Drawable;

public class AppDetail {
    public String appName;
    public transient Drawable appIcon; // transient vì Drawable không Serializable
    public String packageName;
    public long todayUsage;       // thời gian sử dụng hôm nay (ms)
    public long avgDailyUsage;    // trung bình hằng ngày (ms)
    public long maxDailyUsage;    // tối đa hằng ngày (ms)
    public int todayLaunchCount;  // lượt mở hôm nay
    public long continuousUsage;  // thời gian sử dụng liên tục (ms)
    public long installTime;      // thời gian cài đặt (timestamp)
    public AppDetail() {}

    public AppDetail(String appName, Drawable appIcon, long todayUsage,
                     long avgDailyUsage, long maxDailyUsage, int todayLaunchCount,
                     long continuousUsage, long installTime, String packageName) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.todayUsage = todayUsage;
        this.avgDailyUsage = avgDailyUsage;
        this.maxDailyUsage = maxDailyUsage;
        this.packageName = packageName;
        this.todayLaunchCount = todayLaunchCount;
        this.continuousUsage = continuousUsage;
        this.installTime = installTime;
    }
}
