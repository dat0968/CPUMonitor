package com.example.cpumonitor.Fragment;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.view.View.VISIBLE;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.cpumonitor.Activity.TimeLineAppActivity;
import com.example.cpumonitor.Adapter.AppUsageAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppUsageFragment extends Fragment {
    private TextView txtTimetouse;
    private RecyclerView rvAppToUse;
    private AppUsageAdapter adapter;
    private FrameLayout loadingFragmentHowToUse;
    private Button btnGeneralDetails;
    private ImageButton btnSelectDateRange;
    private static String range;
    //static List<AppDetail> appDetails;
    static Boolean ReadyappDetails = true;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_usage_app, container, false);
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
        SharedPreferences prefs = getContext().getSharedPreferences("RangeRank", Context.MODE_PRIVATE);
        range = prefs.getString("range", "today");
        btnGeneralDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myintent = new Intent(getContext(), TimeLineAppActivity.class);
                myintent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(myintent);
            }
        });
        btnSelectDateRange.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_date_range, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.today) range = "today";
                else if (id == R.id.yesterday) range = "yesterday";
                else if (id == R.id.last_7_days) range = "7days";
                else if (id == R.id.last_14_days) range = "14days";
                else if (id == R.id.last_28_days) range = "28days";
                loadingFragmentHowToUse.setVisibility(VISIBLE);
                prefs.edit().putString("range", range).commit();
                loadAppUsageList(getContext(), range);
                getScreenOnTime(getContext(), range);
                return true;
            });
            popup.show();
        });
        loadAppUsageList(getContext(), range);
        // Get thời gian sử dụng màn hình
        getScreenOnTime(getContext(), range);
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
    private void loadAppUsageList(Context context, String range) {
        // Lấy thời gian bắt đầu và kết thúc (milim giây)
        long[] times = getTimeRange(range);
        long startTime = times[0];
        long endTime = times[1];

        new Thread(() -> {
            // Lấy UsageStatsManager để truy xuất lịch sử sử dụng app
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            // Lấy PackageManager để truy xuất thông tin ứng dụng (tên, icon, thời gian cài đặt...)
            PackageManager pm = context.getPackageManager();
            // Danh sách thống kê usage stats thô từ hệ thống trong khoảng thời gian chỉ định
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
            // Map lưu trữ thông tin ứng dụng theo pkg
            Map<String, AppDetail> map = new HashMap<>();

            if (stats != null) {
                for (UsageStats u : stats) {
                    // Lấy tên package của ứng dụng
                    String packageName = u.getPackageName();
                    // Nếu trùng tên với ứng dụng hiện tại thì bỏ qua
                    if (packageName.equals(context.getPackageName())) continue;
                    try {
                        // Lấy thông tin chi tiết của ứng dụng
                        ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                        // Nếu ứng dụng chưa có trong map thì thêm vô
                        if (!map.containsKey(packageName)) {
                            AppDetail detail = new AppDetail();
                            detail.packageName = packageName;
                            detail.appName = pm.getApplicationLabel(ai).toString();
                            detail.appIcon = pm.getApplicationIcon(ai);
                            detail.installTime = pm.getPackageInfo(packageName, 0).firstInstallTime;
                            detail.todayUsage = 0;
                            map.put(packageName, detail);
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {}
                }
            }
            /* Tính toán tổng thời gian sử dụng ứng dụng*/
            // Truy xuất tất cả sự kiện ứng dunng trong khoảng thời gian
            UsageEvents events = usm.queryEvents(startTime, endTime);
            // Đối tượng tạm thời lưu từng event
            UsageEvents.Event event = new UsageEvents.Event();
            // Lưu thời điểm app được mở theo package
            Map<String, Long> startMap = new HashMap<>();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                // Lấy tên các package trong khoảng thời gian này
                String pkg = event.getPackageName();
                // Nó mà không có trong map thì loại bỏ
                if (!map.containsKey(pkg)) continue;

                switch (event.getEventType()) {
                    case UsageEvents.Event.MOVE_TO_FOREGROUND:
                        if (!startMap.containsKey(pkg)) startMap.put(pkg, event.getTimeStamp());
                        break;
                    case UsageEvents.Event.MOVE_TO_BACKGROUND:
                        Long startTs = startMap.remove(pkg);
                        if (startTs != null) {
                            long duration = event.getTimeStamp() - startTs;
                            if (duration > 1400) {
                                map.get(pkg).todayUsage += duration;
                            }
                        }
                        break;
                }
            }

            List<AppDetail> appDetails = new ArrayList<>(map.values());
            appDetails.removeIf(d -> d.todayUsage == 0);
            appDetails.sort((a, b) -> Long.compare(b.todayUsage, a.todayUsage));

            getActivity().runOnUiThread(() -> {
                adapter = new AppUsageAdapter(getContext(), appDetails);
                rvAppToUse.setLayoutManager(new LinearLayoutManager(getContext()));
                rvAppToUse.setAdapter(adapter);
                loadingFragmentHowToUse.setVisibility(View.GONE);
            });

            ReadyappDetails = false;
        }).start();
    }


    private long[] getTimeRange(String range) {
        if (range == null) range = "today";
        Calendar cal = Calendar.getInstance();
        long end = System.currentTimeMillis(); // <- đây chính là now
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

    private void getScreenOnTime(Context context, String range) {
        // Lấy mốc thời gian (mili giây) bắt đầu và kết thúc theo khoảng range (today, yesterday, 7days,…)
        long[] times = getTimeRange(range);
        long start = times[0];
        long end = times[1];
        // Lấy UsageStatsManager để truy xuất lịch sử sử dụng app
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        // Truy vấn tất cả sự kiện sử dụng ứng dụng trong khoảng thời gian
        UsageEvents events = usm.queryEvents(start, end);
        // đối tượng tạm thời để lưu từng event
        UsageEvents.Event event = new UsageEvents.Event();

        // lưu thời điểm app được mở (foreground) theo package
        Map<String, Long> startMap = new HashMap<>();
        long totalForeground = 0;

        while (events.hasNextEvent()) {
            // Lấy sự kiện tiếp theo
            events.getNextEvent(event);
            // Package của ứng dụng
            String pkg = event.getPackageName();

            switch (event.getEventType()) {
                case UsageEvents.Event.MOVE_TO_FOREGROUND:
                    /*startMap lưu thời điểm mở app theo pkb, app chưa có trong startMap
                    thì lưu vào bằng getTimeStamp()*/
                    if (!startMap.containsKey(pkg)) startMap.put(pkg, event.getTimeStamp());
                    break;
                case UsageEvents.Event.MOVE_TO_BACKGROUND:
                    // Lấy thời điểm mở app trước đó và xóa ra khỏi map
                    Long startTs = startMap.remove(pkg);
                    if (startTs != null) {
                        // Lấy thời gian đóng trừ cho thời gian mở để lấy duration
                        long duration = event.getTimeStamp() - startTs;
                        if (duration > 1400) {
                            totalForeground += duration;
                        }
                    }
                    break;
            }
        }

        // Chuyển tổng thời gian từ ms sang giờ:phút:giây
        long seconds = totalForeground / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        txtTimetouse.setText("Tổng số sử dụng(" + range + "):\n" + time);
    }



    public void onResume() {
        super.onResume();
        if (!hasUsageStatsPermission(requireContext())) {
            // Nếu vẫn chưa cấp quyền -> mở lại màn hình cấp quyền
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            if(ReadyappDetails){
                return;
            }
            loadingFragmentHowToUse.setVisibility(VISIBLE);
            // Đã có quyền -> load dữ liệu
            loadAppUsageList(requireContext(), range);
            getScreenOnTime(requireContext(), range);
        }
    }
    private void bindViews(View view) {
        txtTimetouse = view.findViewById(R.id.txtTimetouse);
        rvAppToUse = view.findViewById(R.id.rvAppToUse);
        btnGeneralDetails = view.findViewById(R.id.btnGeneralDetails);
        loadingFragmentHowToUse = view.findViewById(R.id.loadingFragmentHowToUse);
        btnSelectDateRange = view.findViewById(R.id.btnSelectDateRange);
    }
}