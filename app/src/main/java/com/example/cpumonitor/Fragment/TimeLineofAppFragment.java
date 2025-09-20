package com.example.cpumonitor.Fragment;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
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

import com.example.cpumonitor.Activity.TimeLineAppActivity;
import com.example.cpumonitor.Adapter.TimeLineAppAdapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;
import com.example.cpumonitor.Viewmodel.AppTimeline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TimeLineofAppFragment extends Fragment {
    private AppDetail app;
    List<AppTimeline> appTimelineList;
    TimeLineAppAdapter adapter;
    public TimeLineofAppFragment(AppDetail app){
        this.app = app;
    }
    RecyclerView rcvTimelineApp;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_time_line_app, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BindView(view);
        loadUsageEvents();
    }
    private void BindView(View view){
        rcvTimelineApp = view.findViewById(R.id.rcvTimelineApp);
        appTimelineList = new ArrayList<>();
    }
    private void loadUsageEvents() {
        if (app == null) return;

        UsageStatsManager usageStatsManager = (UsageStatsManager) requireContext()
                .getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) return;

        long now = System.currentTimeMillis();
        long startTime = now - 24 * 60 * 60 * 1000; // 24h trước

        UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, now);
        UsageEvents.Event event = new UsageEvents.Event();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        // Wrapper class lưu type và timestamp
        class MyEvent {
            int type;
            long timeStamp;

            MyEvent(int type, long timeStamp) {
                this.type = type;
                this.timeStamp = timeStamp;
            }
        }

        List<MyEvent> appEvents = new ArrayList<>();
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (app.packageName.equals(event.getPackageName())) {
                appEvents.add(new MyEvent(event.getEventType(), event.getTimeStamp()));
            }
        }

        appTimelineList.clear();

        // Duyệt appEvents để tính duration từ FOREGROUND → BACKGROUND
        for (int i = 0; i < appEvents.size(); i++) {
            MyEvent e = appEvents.get(i);
            if (e.type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                long start = e.timeStamp;
                long end = start; // mặc định nếu không có BACKGROUND

                // tìm event BACKGROUND tiếp theo
                for (int j = i + 1; j < appEvents.size(); j++) {
                    MyEvent next = appEvents.get(j);
                    if (next.type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        end = next.timeStamp;
                        break;
                    }
                }

                long duration = end - start; // thời lượng thực tế
                String timeStr = sdf.format(new Date(start));

                appTimelineList.add(new AppTimeline(
                        app.appName,
                        app.appIcon,
                        timeStr,
                        duration,
                        app.packageName
                ));

                Log.d("TimeLineFragment", "Added: " + timeStr + " duration=" + duration);
            }
        }
        // Sau khi thêm tất cả AppTimeline vào appTimelineList
        //Collections.sort(appTimelineList, (a, b) -> Long.compare(b.TimeDuration, a.TimeDuration));

        // Gán layout manager và adapter
        rcvTimelineApp.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimeLineAppAdapter(requireContext(), appTimelineList);
        rcvTimelineApp.setAdapter(adapter);

        Log.d("TimeLineFragment", "Total timeline items: " + appTimelineList.size());
    }


}
