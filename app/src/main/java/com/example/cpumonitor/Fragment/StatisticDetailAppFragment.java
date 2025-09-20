package com.example.cpumonitor.Fragment;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
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
        String continuousStr    = formatDuration(app.continuousUsage);
        String installDateStr   = DateFormat.getDateInstance(
                        DateFormat.MEDIUM, Locale.getDefault())
                .format(new Date(app.installTime));

        // ---- Đổ dữ liệu ra UI ----
        tv_usage_today.setText(todayUsageStr);
        tv_usage_average.setText(avgDailyStr);
        tv_usage_max.setText(maxDailyStr);
        tv_views_today.setText(String.valueOf(app.todayLaunchCount));
        tv_continuous_usage.setText(continuousStr);
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
