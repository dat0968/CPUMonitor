package com.example.cpumonitor.Fragment;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;
import com.example.cpumonitor.Viewmodel.AppTimeLineItem;

import java.text.DateFormat;
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
import java.util.concurrent.TimeUnit;

public class StatisticDetailAppFragment extends Fragment {
    private AppDetail app;

    public StatisticDetailAppFragment(AppDetail app) {
        this.app = app;
    }

    TextView tv_usage_today, tv_usage_average, tv_usage_max,
            tv_views_today, tv_continuous_usage, tv_install_date;
    static private String Range;
    static int days = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistic_detail_app, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BindView(view);
        MapDataToUI();
        // Tính toán số cần truy cập
        HandleCount();
        // Tính toán thời gian sử dụng liên tục
        HandleContinuousUsage();
        // Tính toán thời gian sử dụng trung bình
        HandleUsageAvg();
        handleMaxUsage();
    }

    // Tính toán số lần truy cập
    private void HandleCount() {
        String pkgName = requireActivity().getIntent().getStringExtra("packageName");
        if (pkgName == null) return;

        UsageStatsManager usm =
                (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;

        long[] times = getTimeRange(Range);
        long start = times[0];
        long end = times[1];

        UsageEvents events = usm.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();

        Map<String, Long> startMap = new HashMap<>();
        List<AppTimeLineItem> timelineList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        PackageManager pm = requireContext().getPackageManager();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (!pkgName.equals(event.getPackageName())) continue;

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                startMap.put(pkgName, event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                Long startTs = startMap.remove(pkgName);
                if (startTs != null) {
                    long duration = event.getTimeStamp() - startTs;
                    if (duration > 1400) {
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
                            String name = pm.getApplicationLabel(ai).toString();
                            Drawable icon = pm.getApplicationIcon(ai);
                            String timeStr = sdf.format(new Date(startTs)); // start time của session

                            timelineList.add(new AppTimeLineItem(
                                    name, icon, timeStr, duration, pkgName
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
        int todayLaunchCount = merged.size();
        tv_views_today.setText(String.valueOf(todayLaunchCount));
    }

    // Tính toán thời gian sử dụng liên tục
    private void HandleContinuousUsage() {
        days = 1;
        switch (Range) {
            case "today":
                days = 1;
                break;
            case "yesterday":
                days = 2;
                break;
            case "7days":
                days = 7;
                break;
            case "14days":
                days = 14;
                break;
            case "28days":
                days = 28;
                break;
        }

        //String formatted = String.format(Locale.getDefault(), "%d days %s", days, formatDuration(maxContinuousUsage));
        tv_continuous_usage.setText(days + " days");
    }

    // Tính toán thời gian trung bình hằng ngày
    private void HandleUsageAvg() {
        tv_usage_average.setText(formatDuration(app.todayUsage / days));
    }

    // Tính toán thời gian sử dụng tối đa hằng ngày
    private void handleMaxUsage() {
        tv_usage_max.setText(formatDuration(app.todayUsage));
    }

    private void BindView(View view) {
        tv_usage_today = view.findViewById(R.id.tv_usage_today);
        tv_usage_average = view.findViewById(R.id.tv_usage_average);
        tv_usage_max = view.findViewById(R.id.tv_usage_max);
        tv_views_today = view.findViewById(R.id.tv_views_today);
        tv_continuous_usage = view.findViewById(R.id.tv_continuous_usage);
        tv_install_date = view.findViewById(R.id.tv_install_date);
        SharedPreferences prefs = getContext().getSharedPreferences("RangeRank", Context.MODE_PRIVATE);
        Range = prefs.getString("range", "today");
    }

    private void MapDataToUI() {
        // ---- Format dữ liệu ----
        String todayUsageStr = formatDuration(app.todayUsage);
        String installDateStr = DateFormat.getDateInstance(
                        DateFormat.MEDIUM, Locale.getDefault())
                .format(new Date(app.installTime));

        // ---- Đổ dữ liệu ra UI ----
        tv_usage_today.setText(todayUsageStr);
        tv_install_date.setText(installDateStr);
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
                cal.add(Calendar.DAY_OF_MONTH, -6);
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
            default:
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                break;
        }

        return new long[]{start, end};
    }

    // Hàm format mili giây → hh:mm:ss
    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

}
