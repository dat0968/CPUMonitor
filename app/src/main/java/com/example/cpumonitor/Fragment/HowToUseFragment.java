package com.example.cpumonitor.Fragment;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.cpumonitor.Adapter.TimetoUseAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.viewmodel.AppDetail;
import com.example.cpumonitor.viewmodel.AppItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HowToUseFragment extends Fragment {
    private TextView txtTimetouse;
    private RecyclerView rvAppToUse;
    private Handler handler = new Handler();
    private TimetoUseAppAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_how_to_use, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Kiểm tra & xin quyền Usage Access
        if (!hasUsageStatsPermission(requireContext())) {
            // Mở màn hình cấp quyền cho người dùng
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        bindViews(view);
        loadAppUsageList(getContext());
        // Get thời gian sử dụng màn hình
        getScreenOnTime(getContext());
        handleTimetoUse();
    }

    /**
     * Danh sách thời gian từng app
     */

    // Lấy thông tin chi tiết ứng dụng
    private void loadAppUsageList(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager pm = context.getPackageManager();
        long now = System.currentTimeMillis();

        // Bắt đầu ngày hôm nay
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        List<UsageStats> statsToday = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now);
        List<AppDetail> appDetails = new ArrayList<>();
        Map<String, AppDetail> map = new HashMap<>();
        Map<String, Long> lastBackgroundTimeMap = new HashMap<>();

        if (statsToday != null) {
            UsageEvents events = usm.queryEvents(startOfDay, now);
            UsageEvents.Event event = new UsageEvents.Event();

            for (UsageStats u : statsToday) {
                try {
                    String packageName = u.getPackageName();

                    // Bỏ app hiện tại và app hệ thống
                    if (packageName.equals(context.getPackageName())) continue;

                    ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                    boolean isSystemApp = ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                            || ((ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
                    if (isSystemApp) continue;

                    AppDetail detail = map.get(packageName);
                    if (detail == null) {
                        String name = pm.getApplicationLabel(ai).toString();
                        Drawable icon = pm.getApplicationIcon(ai);
                        long installTime = pm.getPackageInfo(packageName, 0).firstInstallTime;

                        detail = new AppDetail();
                        detail.appName = name;
                        detail.appIcon = icon;
                        detail.packageName = packageName;
                        detail.installTime = installTime;
                        detail.todayUsage = 0;
                        detail.todayLaunchCount = 0;
                        detail.continuousUsage = 0;
                        detail.avgDailyUsage = 0;
                        detail.maxDailyUsage = 0;

                        map.put(packageName, detail);
                    }

                    // Cộng dồn thời gian sử dụng hôm nay
                    detail.todayUsage += u.getTotalTimeInForeground();

                    // Tính avgDailyUsage & maxDailyUsage 7 ngày gần nhất
                    long totalUsage = 0;
                    long maxUsage = 0;
                    for (int i = 0; i < 7; i++) {
                        long start = now - i * 24L * 60 * 60 * 1000;
                        long end = start + 24L * 60 * 60 * 1000;
                        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
                        if (stats != null) {
                            for (UsageStats s : stats) {
                                if (s.getPackageName().equals(packageName)) {
                                    long time = s.getTotalTimeInForeground();
                                    totalUsage += time;
                                    if (time > maxUsage) maxUsage = time;
                                }
                            }
                        }
                    }
                    detail.avgDailyUsage = totalUsage / 7;
                    detail.maxDailyUsage = maxUsage;

                } catch (PackageManager.NameNotFoundException ignored) {}
            }

            // Xử lý todayLaunchCount & continuousUsage từ UsageEvents
            String lastForegroundPackage = ""; // biến tạm lưu package foreground trước đó

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                AppDetail detail = map.get(event.getPackageName());
                if (detail == null) continue;

                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    // Chỉ đếm khi app foreground khác app trước đó
                    if (!event.getPackageName().equals(lastForegroundPackage)) {
                        detail.todayLaunchCount++;
                    }
                    detail.lastForegroundStart = event.getTimeStamp();
                    lastForegroundPackage = event.getPackageName(); // cập nhật foreground package
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    long duration = event.getTimeStamp() - detail.lastForegroundStart;
                    if (duration > detail.continuousUsage) detail.continuousUsage = duration;
                }
            }

        }

        // Chuyển map -> list và sắp xếp theo todayUsage giảm dần
        appDetails.addAll(map.values());
        appDetails.sort((a, b) -> Long.compare(b.todayUsage, a.todayUsage));

        // Debug log
        for (AppDetail d : appDetails) {
            Log.d("AppDetailList",
                    "appName=" + d.appName +
                            ", packageName=" + d.packageName +
                            ", todayUsage(ms)=" + d.todayUsage +
                            ", avgDailyUsage(ms)=" + d.avgDailyUsage +
                            ", maxDailyUsage(ms)=" + d.maxDailyUsage +
                            ", todayLaunchCount=" + d.todayLaunchCount +
                            ", continuousUsage(ms)=" + d.continuousUsage +
                            ", installTime=" + d.installTime);
        }

        // Set adapter
        adapter = new TimetoUseAppAdapter(getContext(), appDetails);
        rvAppToUse.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAppToUse.setAdapter(adapter);
    }







    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void getScreenOnTime(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        long start = end - 24 * 60 * 60 * 1000; // 24h qua

        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        long totalForeground = 0;
        if (stats != null) {
            for (UsageStats u : stats) {
                totalForeground += u.getTotalTimeInForeground();
            }
        }
        // ---- Chuyển mili-giây sang giờ : phút : giây ----
        long seconds = totalForeground / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        txtTimetouse.setText("Tổng số sử dụng: " + time);
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && getActivity() != null) {
                getScreenOnTime(getContext());
                loadAppUsageList(getContext());
                handler.postDelayed(this, 300000);
            }
        }
    };

    public void onResume() {
        super.onResume();
        handleTimetoUse();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void handleTimetoUse() {
        handler.post(updateRunnable);
    }

    private void bindViews(View view) {
        txtTimetouse = view.findViewById(R.id.txtTimetouse);
        rvAppToUse = view.findViewById(R.id.rvAppToUse);
    }
}