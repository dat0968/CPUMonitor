package com.example.cpumonitor.Activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cpumonitor.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StatisticDetailAppActivity extends AppCompatActivity {

    TextView tv_usage_today, tv_usage_average, tv_usage_max,
            tv_views_today, tv_continuous_usage, tv_install_date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistic_detail_app);
        BindView();
        MapDataToUI();
    }

    private void MapDataToUI() {
        Intent intent = getIntent();

        String packageName      = intent.getStringExtra("packageName");
        long todayUsage         = intent.getLongExtra("todayUsage", 0);
        long avgDailyUsage      = intent.getLongExtra("avgDailyUsage", 0);
        long maxDailyUsage      = intent.getLongExtra("maxDailyUsage", 0);
        int todayLaunchCount    = intent.getIntExtra("todayLaunchCount", 0);
        long continuousUsage    = intent.getLongExtra("continuousUsage", 0);
        long installTime        = intent.getLongExtra("installTime", 0);

        // ---- Format dữ liệu ----
        String todayUsageStr    = formatDuration(todayUsage);
        String avgDailyStr      = formatDuration(avgDailyUsage);
        String maxDailyStr      = formatDuration(maxDailyUsage);
        String continuousStr    = formatDuration(continuousUsage);
        String installDateStr   = DateFormat.getDateInstance(
                        DateFormat.MEDIUM, Locale.getDefault())
                .format(new Date(installTime));

        // ---- Đổ dữ liệu ra UI ----
        tv_usage_today.setText(todayUsageStr);
        tv_usage_average.setText(avgDailyStr);
        tv_usage_max.setText(maxDailyStr);
        tv_views_today.setText(String.valueOf(todayLaunchCount));
        tv_continuous_usage.setText(continuousStr);
        tv_install_date.setText(installDateStr);
    }

    private void BindView() {
        tv_usage_today      = findViewById(R.id.tv_usage_today);
        tv_usage_average    = findViewById(R.id.tv_usage_average);
        tv_usage_max        = findViewById(R.id.tv_usage_max);
        tv_views_today      = findViewById(R.id.tv_views_today);
        tv_continuous_usage = findViewById(R.id.tv_continuous_usage);
        tv_install_date     = findViewById(R.id.tv_install_date);
    }

    // Hàm format mili giây → hh:mm:ss
    private String formatDuration(long millis) {
        long hours   = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
