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
import com.example.cpumonitor.Mapper.AppTimeLineItemMapper;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;
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

public class SingleTimeLineAppFragment extends Fragment {
    private AppDetail app;
    List<AppTimeLineItem> appTimelineListItem;
    TimeLineAppAdapter adapter;
    public SingleTimeLineAppFragment(AppDetail app){
        this.app = app;
    }
    RecyclerView rcvTimelineApp;
    private static String Range;
    SharedPreferences prefs;
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
        prefs = getContext().getSharedPreferences("RangeRank", Context.MODE_PRIVATE);
        rcvTimelineApp = view.findViewById(R.id.rcvTimelineApp);
        appTimelineListItem = new ArrayList<>();
    }
    private void loadSingleAppUsageEvents(String pkgName){
        if (pkgName == null) return;
        Range = prefs.getString("range", "today");
        UsageStatsManager usm =
                (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;

        long[] times = getTimeRange(Range);
        long start = times[0];
        long end = times[1];

        UsageEvents events = usm.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();

        List<AppTimeLineItem> timelineList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        PackageManager pm = requireContext().getPackageManager();
        long startTime = -1;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (!pkgName.equals(event.getPackageName())) continue;

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                startTime = event.getTimeStamp();
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                long endTime = event.getTimeStamp();
                timelineList.add(AppTimeLineItemMapper.fromEventData(pm, pkgName, startTime, endTime));
            }
        }
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
                Log.e("TimeLineParser", "Lỗi parse thời gian prev.Timeline = " + prev.Timeline, e);
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


        requireActivity().runOnUiThread(() -> {
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
                adapter = new TimeLineAppAdapter(getContext(), headerTimelineItems);
                rcvTimelineApp.setLayoutManager(new LinearLayoutManager(getContext()));
                rcvTimelineApp.setAdapter(adapter);
            } else {
                Log.w("TimeLineFragment", "timelineList rỗng, không set adapter");
            }
        });

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
