package com.example.cpumonitor.Fragment;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.cpumonitor.Activity.TimeLineAppActivity;
import com.example.cpumonitor.Adapter.AppUsageAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppUsageFragment extends Fragment {
    private TextView txtTimetouse;
    private RecyclerView rvAppToUse;
    private AppUsageAdapter adapter;
    private FrameLayout loadingFragmentHowToUse;
    private Button btnGeneralDetails;
    //static List<AppDetail> appDetails;
    static Boolean ReadyappDetails = true;
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
        loadingFragmentHowToUse.setVisibility(VISIBLE);
        btnGeneralDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myintent = new Intent(getContext(), TimeLineAppActivity.class);
                myintent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(myintent);
            }
        });
        loadAppUsageList(getContext());
        // Get thời gian sử dụng màn hình
        getScreenOnTime(getContext());
    }
    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    // Lấy thông tin chi tiết ứng dụng
    private void loadAppUsageList(Context context) {
        new Thread(() -> {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            PackageManager pm = context.getPackageManager();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            long now        = System.currentTimeMillis();


            List<UsageStats> statsToday = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now);
            List<AppDetail> appDetails = new ArrayList<>();
            Map<String, AppDetail> map = new HashMap<>();
            if (statsToday != null) {
                for (UsageStats u : statsToday) {
                    try {
                        String packageName = u.getPackageName();
                        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                        if (launchIntent == null) continue;
                        // Bỏ app hiện tại và app hệ thống
                        if (packageName.equals(context.getPackageName())) continue;

                        ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
//                        boolean isSystemApp = ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
//                                || ((ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
//                        if (isSystemApp && !packageName.equals("com.android.chrome") &&
//                                !packageName.startsWith("com.google.")) {
//                            // Bỏ qua system app, trừ Chrome
//                            continue;
//                        }

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
                            detail.continuousUsage = 0;
                            detail.avgDailyUsage = 0;
                            detail.maxDailyUsage = 0;

                            map.put(packageName, detail);
                        }

                        // Cộng dồn thời gian sử dụng hôm nay
                        detail.todayUsage += u.getTotalTimeInForeground();

                        // Tính avgDailyUsage & maxDailyUsage 7 ngày gần nhất
//                        long totalUsage = 0;
//                        long maxUsage = 0;
//                        for (int i = 0; i < 7; i++) {
//                            long start = now - i * 24L * 60 * 60 * 1000;
//                            long end = start + 24L * 60 * 60 * 1000;
//                            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
//                            if (stats != null) {
//                                for (UsageStats s : stats) {
//                                    if (s.getPackageName().equals(packageName)) {
//                                        long time = s.getTotalTimeInForeground();
//                                        totalUsage += time;
//                                        if (time > maxUsage) maxUsage = time;
//                                    }
//                                }
//                            }
//                        }
//                        detail.avgDailyUsage = totalUsage / 7;
//                        detail.maxDailyUsage = maxUsage;

                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                }
            }

            // Chuyển map -> list và sắp xếp theo todayUsage giảm dần
            appDetails.addAll(map.values());
            // Lọc bỏ các app có todayUsage = 0;
            Iterator<AppDetail> iterator = appDetails.iterator();
            while (iterator.hasNext()) {
                AppDetail d = iterator.next();
                if (d.todayUsage == 0) {
                    iterator.remove();
                }
            }
            appDetails.sort((a, b) -> Long.compare(b.todayUsage, a.todayUsage));

            // Set adapter
            getActivity().runOnUiThread(() -> {
                adapter = new AppUsageAdapter(getContext(), appDetails);
                rvAppToUse.setLayoutManager(new LinearLayoutManager(getContext()));
                rvAppToUse.setAdapter(adapter);
                rvAppToUse.post(() -> loadingFragmentHowToUse.setVisibility(GONE));
            });
            ReadyappDetails = false;
        }).start();
    }

    private void getScreenOnTime(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        // Lấy 0 giờ hôm nay
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();   // mốc 0h hôm nay
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        long totalForeground = 0;
        if (stats != null) {
            for (UsageStats u : stats) {
                if (u.getTotalTimeInForeground() < 30_000) continue; // < 30 giây bỏ qua
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

    public void onResume() {
        super.onResume();
        if (!hasUsageStatsPermission(requireContext())) {
            // Nếu vẫn chưa cấp quyền -> mở lại màn hình cấp quyền
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            if(!ReadyappDetails){
                return;
            }
            loadingFragmentHowToUse.setVisibility(VISIBLE);
            // Đã có quyền -> load dữ liệu
            loadAppUsageList(requireContext());
            getScreenOnTime(requireContext());
        }
    }


    private void bindViews(View view) {
        txtTimetouse = view.findViewById(R.id.txtTimetouse);
        rvAppToUse = view.findViewById(R.id.rvAppToUse);
        btnGeneralDetails = view.findViewById(R.id.btnGeneralDetails);
        loadingFragmentHowToUse = view.findViewById(R.id.loadingFragmentHowToUse);
    }
}