package com.example.cpumonitor.Activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.Adapter.TimeLineAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppTimeLineItem;
import com.example.cpumonitor.Viewmodel.HeaderTimelineItem;

import java.text.ParseException;
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
    List<AppTimeLineItem> appTimelineListItem = new ArrayList<>();
    FrameLayout loadingOverlay;
    TimeLineAppAdapter adapter;
    private static String Range;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line_app);
        rcvTimelineApp = findViewById(R.id.rcvTimelineApp);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadUsageEvents();
    }
    private void loadUsageEvents() {
        Log.e("QueryUsageEvents", "UsageEvents");
        SharedPreferences prefs = getSharedPreferences("RangeRank", Context.MODE_PRIVATE);
        Range = prefs.getString("range", "today");
        loadingOverlay.setVisibility(VISIBLE);

        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;

        long[] times = getTimeRange(Range);
        long start = times[0];
        long end = times[1];

        UsageEvents events = usm.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();

        Map<String, Long> startMap = new HashMap<>();
        List<AppTimeLineItem> timelineList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        PackageManager pm = getPackageManager();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String pkg = event.getPackageName();
            if (getPackageName().equals(pkg)) continue;
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                startMap.put(pkg, event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                Long startTs = startMap.remove(pkg);
                if (startTs != null) {
                    long duration = event.getTimeStamp() - startTs;
                    if (duration > 1400) {
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                            String name = pm.getApplicationLabel(ai).toString();
                            Drawable icon = pm.getApplicationIcon(ai);
                            String timeStr = sdf.format(new Date(startTs)); // start time của session

                            timelineList.add(new AppTimeLineItem(
                                    name, icon, timeStr, duration, pkg
                            ));
                        } catch (PackageManager.NameNotFoundException ignored) {
                        }
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder("timelineList:\n");
        for (AppTimeLineItem item : timelineList) {
            long durationMs = item.TimeDuration;
            long hours = durationMs / 3600000;
            long minutes = (durationMs % 3600000) / 60000;
            long seconds = (durationMs % 60000) / 1000;

            sb.append("App=").append(item._package)
                    .append(", Timeline=").append(item.Timeline)
                    .append(", Duration=").append(hours).append("h ")
                    .append(minutes).append("m ").append(seconds).append("s")
                    .append("\n");
        }
        Log.d("timelineList", sb.toString());
        // ===== Gộp các session gần nhau =====
        final long THRESHOLD_GAP = 15_000L; // 15 giây

        List<AppTimeLineItem> merged = new ArrayList<>();
        if (!timelineList.isEmpty()) {
            merged.add(timelineList.get(0));
        }

        for (int i = 1; i < timelineList.size(); i++) {
            AppTimeLineItem prev = merged.get(merged.size() - 1);
            AppTimeLineItem cur = timelineList.get(i);

            boolean samePackage = prev._package != null
                    && prev._package.equals(cur._package);

            // Khoảng cách giữa 2 session
            long prevEndTime = 0;
            try {
                prevEndTime = sdf.parse(prev.Timeline).getTime() + prev.TimeDuration;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long curStartTime = 0;
            try {
                curStartTime = sdf.parse(cur.Timeline).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long gap = curStartTime - prevEndTime;

            if (samePackage && gap <= THRESHOLD_GAP) {
                // gộp duration vào prev
                prev.TimeDuration += cur.TimeDuration + gap; // cộng cả khoảng trống
            } else {
                merged.add(cur);
            }
        }
        Log.d("DEBUG_MERGED", "======= merged timeline =======");
        for (AppTimeLineItem item : merged) {
            Log.d("DEBUG_MERGED",
                    "App: " + item._package +
                            " | Timeline(start): " + item.Timeline +
                            " | Duration(ms): " + item.TimeDuration +
                            " | Duration(hms): " +
                            (item.TimeDuration / 3600000) + "h " +
                            ((item.TimeDuration / 60000) % 60) + "m " +
                            ((item.TimeDuration / 1000) % 60) + "s");
        }
        runOnUiThread(() -> {
            if (merged != null && !merged.isEmpty()) {
                List<HeaderTimelineItem> headerTimelineItems = new ArrayList<>();
                String lastDate = null;
                List<AppTimeLineItem> currentDayItems = null;
                for (AppTimeLineItem t : merged) {
                    String currentDate = t.Timeline.substring(0, 10); // yyyy-MM-dd

                    if (!currentDate.equals(lastDate)) {
                        // nếu đã có list cũ thì thêm vào header
                        if (currentDayItems != null) {
                            headerTimelineItems.add(new HeaderTimelineItem(lastDate, currentDayItems));
                        }
                        currentDayItems = new ArrayList<>();
                        lastDate = currentDate;
                    }
                    currentDayItems.add(t);
                }

                // thêm nhóm cuối cùng
                if (currentDayItems != null) {
                    headerTimelineItems.add(new HeaderTimelineItem(lastDate, currentDayItems));
                }
                adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, headerTimelineItems);
                rcvTimelineApp.setLayoutManager(new LinearLayoutManager(TimeLineAppActivity.this));
                rcvTimelineApp.setAdapter(adapter);
                loadingOverlay.setVisibility(GONE);
            } else {
                Log.w("TimeLineFragment", "timelineList rỗng, không set adapter");
            }
        });
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