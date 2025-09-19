package com.example.cpumonitor.Activity;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.Adapter.TimeLineAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppTimeline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        UsageStatsManager usm =
                (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        if (usm == null) {
            Toast.makeText(this, "UsageStatsManager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        long end = System.currentTimeMillis();
        long start = end - 1000L * 60 * 60; // Lấy dữ liệu 1 giờ trước

        UsageEvents usageEvents = usm.queryEvents(start, end);
        if (usageEvents == null) {
            Toast.makeText(this, "Chưa được cấp quyền Usage Access", Toast.LENGTH_SHORT).show();
            return;
        }

        UsageEvents.Event event = new UsageEvents.Event();
        PackageManager pm = getPackageManager();

        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(event.getPackageName(), 0);
                    String label = pm.getApplicationLabel(info).toString();
                    Drawable icon = pm.getApplicationIcon(info);
                    long ts = event.getTimeStamp();  // timestamp gốc
                    String timeStr = sdf.format(new Date(ts));

                    appTimelineList.add(new AppTimeline(label, icon, timeStr, ts));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        // Sắp xếp mới nhất lên đầu theo timestamp
        Collections.sort(appTimelineList, new Comparator<AppTimeline>() {
            @Override
            public int compare(AppTimeline a, AppTimeline b) {
                return Long.compare(b.timestamp, a.timestamp);
            }
        });

        adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, appTimelineList);
        rcvTimelineApp.setAdapter(adapter);
    }
}