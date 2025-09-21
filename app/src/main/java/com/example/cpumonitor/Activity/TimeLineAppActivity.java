package com.example.cpumonitor.Activity;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        new Thread(() -> {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return;
            PackageManager pm = getPackageManager();

            long now = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();

            UsageEvents events = usm.queryEvents(startOfDay, now);
            UsageEvents.Event event = new UsageEvents.Event();

            // build list of sessions in chronological order (old -> new)
            List<AppTimeline> timelineList = new ArrayList<>();
            SimpleDateFormat sdfHM = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Map<String, Long> startMap = new HashMap<>();

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (pkg == null) continue;

                try {
                    // bỏ app không có icon launcher hoặc chính app
                    Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                    if (launchIntent == null || pkg.equals(getPackageName())) continue;

                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        startMap.put(pkg, event.getTimeStamp());
                    } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        Long startTs = startMap.remove(pkg);
                        if (startTs != null) {
                            long duration = event.getTimeStamp() - startTs;
                            if (duration > 1000) { // >1s
                                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                                String name = pm.getApplicationLabel(ai).toString();
                                Drawable icon = pm.getApplicationIcon(ai);
                                String timeStr = sdfHM.format(new Date(event.getTimeStamp()));

                                // NOTE: constructor của bạn: (appName, icon, time, duration, _package)
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

            // nếu rỗng thì cập nhật UI trống luôn
            if (timelineList.isEmpty()) {
                runOnUiThread(() -> {
                    adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, timelineList);
                    rcvTimelineApp.setLayoutManager(new LinearLayoutManager(TimeLineAppActivity.this));
                    rcvTimelineApp.setAdapter(adapter);
                });
                return;
            }

            // Đảo để có thứ tự mới -> cũ (vì timelineList hiện theo thứ tự event: cũ -> mới)
            Collections.reverse(timelineList);

            // Gộp theo quy tắc: cùng package, cùng HH:mm, |TimeDuration diff| <= THRESHOLD_MS
            final long THRESHOLD_MS = 4000L; // 4 giây, đổi nếu cần
            List<AppTimeline> merged = new ArrayList<>();
            merged.add(timelineList.get(0));

            for (int i = 1; i < timelineList.size(); i++) {
                AppTimeline prev = merged.get(merged.size() - 1);
                AppTimeline cur = timelineList.get(i);

                boolean samePackage = prev._package != null && prev._package.equals(cur._package);
                boolean sameHHMM = prev.Timeline != null && prev.Timeline.equals(cur.Timeline);
                long diffDuration = Math.abs(prev.TimeDuration - cur.TimeDuration);

                if (samePackage && sameHHMM && diffDuration <= THRESHOLD_MS) {
                    // gộp: cộng duration vào prev
                    prev.TimeDuration = prev.TimeDuration + cur.TimeDuration;
                    // giữ prev.Timeline (mốc giờ của prev, tức là mới hơn)
                } else {
                    merged.add(cur);
                }
            }

            // cập nhật UI
            runOnUiThread(() -> {
                adapter = new TimeLineAppAdapter(TimeLineAppActivity.this, merged);
                rcvTimelineApp.setLayoutManager(new LinearLayoutManager(TimeLineAppActivity.this));
                rcvTimelineApp.setAdapter(adapter);
            });
        }).start();
    }
}