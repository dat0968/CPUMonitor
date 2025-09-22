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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.Adapter.TimeLineAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;
import com.example.cpumonitor.Viewmodel.AppTimeline;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SingleTimeLineAppFragment extends Fragment {
    private AppDetail app;
    List<AppTimeline> appTimelineList;
    TimeLineAppAdapter adapter;
    public SingleTimeLineAppFragment(AppDetail app){
        this.app = app;
    }
    RecyclerView rcvTimelineApp;
    private static String Range;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_time_line_app, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BindView(view);
        loadSingleAppUsageEvents(app.packageName);
    }
    private void BindView(View view){
        rcvTimelineApp = view.findViewById(R.id.rcvTimelineApp);
        appTimelineList = new ArrayList<>();
    }

    private void loadSingleAppUsageEvents(String targetPkg) {
        SharedPreferences prefs = getContext().getSharedPreferences("RangeRank", Context.MODE_PRIVATE);
        Range = prefs.getString("range", "today");
        long[] times = getTimeRange(Range);
        long startTime = times[0];
        long endTime = times[1];
        new Thread(() -> {
            UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return;

            PackageManager pm = requireContext().getPackageManager();

            UsageEvents events = usm.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();

            List<AppTimeline> timelineList = new ArrayList<>();
            SimpleDateFormat sdfHMS = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            Map<String, Long> startMap = new HashMap<>(); // Lưu thời gian mở app

            while (events.hasNextEvent()) {
                events.getNextEvent(event);

                if (!targetPkg.equals(event.getPackageName())) continue;

                try {
                    switch (event.getEventType()) {
                        case UsageEvents.Event.MOVE_TO_FOREGROUND:
                            // Chỉ lưu lần mở app đầu tiên nếu chưa có session đang mở
                            if (!startMap.containsKey(targetPkg)) {
                                startMap.put(targetPkg, event.getTimeStamp());
                            }
                            break;

                        case UsageEvents.Event.MOVE_TO_BACKGROUND:
                            Long startTs = startMap.remove(targetPkg);
                            if (startTs != null) {
                                long duration = event.getTimeStamp() - startTs;
                                if (duration > 1400) {
                                    ApplicationInfo ai = pm.getApplicationInfo(targetPkg, 0);
                                    String name = pm.getApplicationLabel(ai).toString();
                                    Drawable icon = pm.getApplicationIcon(ai);
                                    String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(startTs));

                                    timelineList.add(0, new AppTimeline(
                                            name,
                                            icon,
                                            timeStr,
                                            duration,
                                            targetPkg
                                    ));
                                }
                            }
                            break;
                    }
                } catch (PackageManager.NameNotFoundException ignored) {}
            }

            // Cập nhật RecyclerView trên UI thread
            requireActivity().runOnUiThread(() -> {
                if (timelineList != null && !timelineList.isEmpty()) {
                    adapter = new TimeLineAppAdapter(getContext(), timelineList);
                    rcvTimelineApp.setLayoutManager(new LinearLayoutManager(getContext()));
                    rcvTimelineApp.setAdapter(adapter);
                } else {
                    Log.w("TimeLineFragment", "timelineList rỗng, không set adapter");
                    // Nếu muốn, hiển thị TextView "Không có dữ liệu"
                }
            });

            Log.d("TimeLineFragment", "Total timeline items: " + timelineList.size());
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
}
