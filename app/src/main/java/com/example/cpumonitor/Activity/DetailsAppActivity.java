package com.example.cpumonitor.Activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.cpumonitor.Adapter.ViewPager_Details_App_Adapter;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DetailsAppActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    long todayUsage, avgDailyUsage, maxDailyUsage, continuousUsage, installTime;
    int todayLaunchCount;
    private AppDetail app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_app);
        app = new AppDetail();
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        MappingData();



        // Adapter cho ViewPager2
        ViewPager_Details_App_Adapter adapter = new ViewPager_Details_App_Adapter(this, app);
        viewPager.setAdapter(adapter);

        // Liên kết TabLayout với ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Thống kê");
                    } else if (position == 1) {
                        tab.setText("Dòng thời gian");
                    }
                }).attach();

        // Chọn mặc định tab "Thống kê"
        viewPager.setCurrentItem(0, false);
    }
    public void MappingData(){
        Intent intent = getIntent();
        app.appName = intent.getStringExtra("appName");
        app.packageName = intent.getStringExtra("packageName");
        app.appIcon = Drawable.createFromPath(intent.getStringExtra("appIcon"));
        app.todayUsage         = intent.getLongExtra("todayUsage", 0);
        app.avgDailyUsage      = intent.getLongExtra("avgDailyUsage", 0);
        app.maxDailyUsage      = intent.getLongExtra("maxDailyUsage", 0);
        app.todayLaunchCount    = intent.getIntExtra("todayLaunchCount", 0);
        app.continuousUsage    = intent.getLongExtra("continuousUsage", 0);
        app.installTime        = intent.getLongExtra("installTime", 0);
    }
}