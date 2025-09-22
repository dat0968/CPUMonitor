package com.example.cpumonitor.Activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static java.security.AccessController.getContext;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.Adapter.TimeLineAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppTimeline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import java.util.Date;
import java.util.HashMap;

import java.util.List;
import java.util.Locale;
import java.util.Map;


public class TimeLineAppActivity extends AppCompatActivity {

    RecyclerView rcvTimelineApp;
    List<AppTimeline> appTimelineList = new ArrayList<>();
    FrameLayout loadingOverlay;
    TimeLineAppAdapter adapter;
    private static String Range;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line_app);
        rcvTimelineApp = findViewById(R.id.rcvTimelineApp);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadUsageEvents(Range);
    }
    private void loadUsageEvents(String range) {
        SharedPreferences prefs = getSharedPreferences("RangeRank", Context.MODE_PRIVATE);
        Range = prefs.getString("range", "today");
        loadingOverlay.setVisibility(VISIBLE);
        new Thread(() -> {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return;
            PackageManager pm = getPackageManager();

            long[] times = getTimeRange(range);
            long startOfRange = times[0];
            long endOfRange = times[1];

            UsageEvents events = usm.queryEvents(startOfRange, endOfRange);
            UsageEvents.Event event = new UsageEvents.Event();

            List<AppTimeline> timelineList = new ArrayList<>();
            //SimpleDateFormat sdfHM = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdfHM = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Map<String, Long> startMap = new HashMap<>();

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (pkg == null) continue;

                try {
                    Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                    if (launchIntent == null || pkg.equals(getPackageName())) continue;

                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        startMap.put(pkg, event.getTimeStamp());
                    } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        Long startTs = startMap.remove(pkg);
                        if (startTs != null) {
                            long duration = event.getTimeStamp() - startTs;
                            if (duration > 1400) { // >1s
                                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                                String name = pm.getApplicationLabel(ai).toString();
                                Drawable icon = pm.getApplicationIcon(ai);
                                String timeStr = sdfHM.format(new Date(event.getTimeStamp()));

                                timelineList.add(new AppTimeline(
                                        name,
                                        icon,
                                        timeStr,
                                        duration,
                                        pkg
                                ));
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException ignored) {}
            }

            // Gộp timeline (tương tự code hiện tại)
            if (!timelineList.isEmpty()) {
                Collections.reverse(timelineList);
                final long THRESHOLD_MS = 4000L;
                List<AppTimeline> merged = new ArrayList<>();
                merged.add(timelineList.get(0));

                for (int i = 1; i < timelineList.size(); i++) {
                    AppTimeline prev = merged.get(merged.size() - 1);
                    AppTimeline cur = timelineList.get(i);

                    boolean samePackage = prev._package != null && prev._package.equals(cur._package);
                    boolean sameHHMM = prev.Timeline != null && prev.Timeline.equals(cur.Timeline);
                    long diffDuration = Math.abs(prev.TimeDuration - cur.TimeDuration);

                    if (samePackage && sameHHMM && diffDuration <= THRESHOLD_MS) {
                        prev.TimeDuration += cur.TimeDuration;
                    } else {
                        merged.add(cur);
                    }
                }

                runOnUiThread(() -> {
                    adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, merged);
                    rcvTimelineApp.setLayoutManager(new LinearLayoutManager(TimeLineAppActivity.this));
                    rcvTimelineApp.setAdapter(adapter);
                    loadingOverlay.setVisibility(GONE);
                });
            } else {
                runOnUiThread(() -> {
                    adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, timelineList);
                    rcvTimelineApp.setLayoutManager(new LinearLayoutManager(TimeLineAppActivity.this));
                    rcvTimelineApp.setAdapter(adapter);
                    loadingOverlay.setVisibility(GONE);
                });
            }
        }).start();
    }
    private long[] getTimeRange(String range) {
        if (range == null) range = "today";
        Calendar cal = Calendar.getInstance();
        long end = System.currentTimeMillis();
        long start;

        switch (range) {
            case "today":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                break;
            case "yesterday":
                cal.add(Calendar.DAY_OF_MONTH, -1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();

                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                end = cal.getTimeInMillis();
                break;
            case "7days":
                cal.add(Calendar.DAY_OF_MONTH, -6); // bao gồm hôm nay
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                break;
            case "14days":
                cal.add(Calendar.DAY_OF_MONTH, -13);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                break;
            case "28days":
                cal.add(Calendar.DAY_OF_MONTH, -27);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                break;
            default: // today
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                break;
        }

        return new long[]{start, end};
    }
}