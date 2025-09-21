package com.example.cpumonitor.Fragment;

import static android.content.Context.USAGE_STATS_SERVICE;
import static android.content.Intent.getIntent;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
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
import com.example.cpumonitor.Viewmodel.AppTimeline;

import java.text.DateFormat;
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
    public StatisticDetailAppFragment(AppDetail app){
        this.app = app;
    }
    TextView tv_usage_today, tv_usage_average, tv_usage_max,
            tv_views_today, tv_continuous_usage, tv_install_date;
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

        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        UsageEvents events = usm.queryEvents(startOfDay, now);
        UsageEvents.Event event = new UsageEvents.Event();

        Map<String, Long> startMap = new HashMap<>();
        List<AppTimeline> timelineList = new ArrayList<>();
        SimpleDateFormat sdfHM = new SimpleDateFormat("HH:mm", Locale.getDefault());
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
                    if (duration > 90_000) { // >1s
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
                            String name = pm.getApplicationLabel(ai).toString();
                            Drawable icon = pm.getApplicationIcon(ai);
                            String timeStr = sdfHM.format(new Date(event.getTimeStamp()));

                            // Giống hệt cấu trúc timeline ở Activity tổng
                            timelineList.add(new AppTimeline(
                                    name,
                                    icon,
                                    timeStr,
                                    duration,
                                    pkgName
                            ));
                        } catch (PackageManager.NameNotFoundException ignored) {}
                    }
                }
            }
        }

        // --- Gộp giống y hệt ở Activity tổng ---
        Collections.reverse(timelineList); // đảo: mới -> cũ
        final long THRESHOLD_MS = 4000L;    // 4 giây

        List<AppTimeline> merged = new ArrayList<>();
        if(!timelineList.isEmpty()){
            merged.add(timelineList.get(0));
        }


        for (int i = 1; i < timelineList.size(); i++) {
            AppTimeline prev = merged.get(merged.size() - 1);
            AppTimeline cur = timelineList.get(i);

            boolean samePackage = prev._package != null && prev._package.equals(cur._package);
            boolean sameHHMM    = prev.Timeline != null && prev.Timeline.equals(cur.Timeline);
            long diffDuration   = Math.abs(prev.TimeDuration - cur.TimeDuration);

            if (samePackage && sameHHMM && diffDuration <= THRESHOLD_MS) {
                // gộp: cộng duration vào prev
                prev.TimeDuration += cur.TimeDuration;
            } else {
                merged.add(cur);
            }
        }

        int todayLaunchCount = merged.size();
        tv_views_today.setText(String.valueOf(todayLaunchCount));
    }

    // Tính toán thời gian sử dụng liên tục
    private void HandleContinuousUsage() {
        // Lấy packageName từ Intent (truyền từ Activity cha)
        String pkgName = requireActivity().getIntent().getStringExtra("packageName");
        if (pkgName == null) return;

        UsageStatsManager usm =
                (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;

        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        UsageEvents events = usm.queryEvents(startOfDay, now);
        UsageEvents.Event event = new UsageEvents.Event();

        long lastForegroundStart = -1L;
        long continuousUsage = 0L;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (!pkgName.equals(event.getPackageName())) continue;

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundStart = event.getTimeStamp();
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (lastForegroundStart > 0) {
                    long duration = event.getTimeStamp() - lastForegroundStart;
                    if (duration > continuousUsage) {
                        continuousUsage = duration; // lưu lần dùng dài nhất
                    }
                }
            }
        }

        // Cập nhật UI hoặc trả về kết quả
        tv_continuous_usage.setText(formatDuration(continuousUsage));
    }

    // Tính toán thời gian trung bình hằng ngày
    private void HandleUsageAvg(){
        tv_usage_average.setText(formatDuration(app.todayUsage));
    }

    // Tính toán thời gian sử dụng tối đa hằng ngày
    private void handleMaxUsage(){
        tv_usage_max.setText(formatDuration(app.todayUsage));
    }
    private void BindView(View view) {
        tv_usage_today      = view.findViewById(R.id.tv_usage_today);
        tv_usage_average    = view.findViewById(R.id.tv_usage_average);
        tv_usage_max        = view.findViewById(R.id.tv_usage_max);
        tv_views_today      = view.findViewById(R.id.tv_views_today);
        tv_continuous_usage = view.findViewById(R.id.tv_continuous_usage);
        tv_install_date     = view.findViewById(R.id.tv_install_date);
    }

    private void MapDataToUI() {
        // ---- Format dữ liệu ----
        String todayUsageStr    = formatDuration(app.todayUsage);
        String avgDailyStr      = formatDuration(app.avgDailyUsage);
        String maxDailyStr      = formatDuration(app.maxDailyUsage);
        String installDateStr   = DateFormat.getDateInstance(
                        DateFormat.MEDIUM, Locale.getDefault())
                .format(new Date(app.installTime));

        // ---- Đổ dữ liệu ra UI ----
        tv_usage_today.setText(todayUsageStr);
        //tv_usage_average.setText(avgDailyStr);
        //tv_usage_max.setText(maxDailyStr);
        tv_install_date.setText(installDateStr);
    }
    // Hàm format mili giây → hh:mm:ss
    private String formatDuration(long millis) {
        long hours   = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

}
