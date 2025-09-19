package com.example.cpumonitor.Activity;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.Adapter.TimeLineAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppTimeline;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_time_line_app);
        rcvTimelineApp = findViewById(R.id.rcvTimelineApp);
        loadUsageEvents();
    }
    private void loadUsageEvents() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        if (usm == null) {
            Toast.makeText(this, "UsageStatsManager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        long end = System.currentTimeMillis();
        long start = end - 1000L * 60 * 60; // 1 giờ trước

        UsageEvents usageEvents = usm.queryEvents(start, end);
        if (usageEvents == null) {
            Toast.makeText(this, "Chưa được cấp quyền Usage Access", Toast.LENGTH_SHORT).show();
            return;
        }

        UsageEvents.Event event = new UsageEvents.Event();
        PackageManager pm = getPackageManager();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        // set để loại trừ trùng app
        Set<String> addedPackages = new HashSet<>();
        appTimelineList.clear();

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            String pkg = event.getPackageName();

            if (pkg.equals(getPackageName())) continue; // bỏ app hiện tại
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                // Nếu chỉ muốn mỗi app 1 lần, dùng addedPackages
                if (addedPackages.contains(pkg)) continue;

                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    String label = pm.getApplicationLabel(info).toString();
                    Drawable icon = pm.getApplicationIcon(info);
                    long ts = event.getTimeStamp();
                    String timeStr = sdf.format(new Date(ts));

                    appTimelineList.add(new AppTimeline(label, icon, timeStr, ts));
                    addedPackages.add(pkg);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        // Sắp xếp mới nhất lên đầu theo timestamp
        Collections.sort(appTimelineList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        for (int i = 0; i < Math.min(appTimelineList.size(), 10); i++) {
            AppTimeline t = appTimelineList.get(i);
            Log.d("AppTimeline", "App: " + t.appName + ", time: " + t.timestamp);
        }

        rcvTimelineApp.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, appTimelineList);
        rcvTimelineApp.setAdapter(adapter);
    }

}