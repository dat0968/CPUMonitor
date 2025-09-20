package com.example.cpumonitor.Activity;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.Adapter.TimeLineAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppTimeline;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TimeLineAppActivity extends AppCompatActivity {

    RecyclerView rcvTimelineApp;
    List<AppTimeline> appTimelineList = new ArrayList<>();
    TimeLineAppAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line_app);
        rcvTimelineApp = findViewById(R.id.rcvTimelineApp);
        loadUsageEvents();
    }
    private void loadUsageEvents() {
        PackageManager pm = getPackageManager();
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        if (usm == null) {
            Toast.makeText(this, "UsageStatsManager không khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Lấy danh sách app launcher
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, 0);
        Set<String> launcherPackageSet = new HashSet<>();
        for (ResolveInfo info : launcherApps) {
            launcherPackageSet.add(info.activityInfo.packageName);
        }

        // 2. Lấy UsageStats trong 24h
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 1000L * 60 * 60 * 24;
        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        if (usageStatsList == null || usageStatsList.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu sử dụng ứng dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        appTimelineList.clear();
        Set<String> addedPackages = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (UsageStats stats : usageStatsList) {
            String pkg = stats.getPackageName();

            // Bỏ app không phải launcher và app trùng lặp
            if (!launcherPackageSet.contains(pkg) || addedPackages.contains(pkg)) continue;
            if (pkg.equals(getPackageName())) continue;

            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                String label = pm.getApplicationLabel(info).toString();
                Drawable icon = pm.getApplicationIcon(info);

                long lastTimeUsed = stats.getLastTimeUsed();
                long totalTimeUsed = stats.getTotalTimeInForeground();
                String timelineStr = sdf.format(new Date(lastTimeUsed));
                if(totalTimeUsed < 1){
                    continue;
                }

                appTimelineList.add(new AppTimeline(label, icon, timelineStr, totalTimeUsed, pkg));
                addedPackages.add(pkg);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Sắp xếp theo Timeline (mới nhất lên đầu)
        Collections.sort(appTimelineList, (a, b) -> {
            try {
                Date dateA = sdf.parse(a.Timeline);
                Date dateB = sdf.parse(b.Timeline);
                return dateB.compareTo(dateA); // mới nhất lên đầu
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });

        // Gán RecyclerView
        rcvTimelineApp.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, appTimelineList);
        rcvTimelineApp.setAdapter(adapter);
    }


}