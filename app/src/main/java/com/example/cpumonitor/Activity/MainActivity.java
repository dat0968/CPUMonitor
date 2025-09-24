package com.example.cpumonitor.Activity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.cpumonitor.Adapter.ViewPager_MainActivity_Adapter;
import com.example.cpumonitor.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPager_MainActivity_Adapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupViewPager();
        setupTabLayout();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager.setUserInputEnabled(false);
    }

    private void setupViewPager() {
        adapter = new ViewPager_MainActivity_Adapter(this);
        viewPager.setAdapter(adapter);
    }

    private void setupTabLayout() {
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("System");
                            break;
                        case 1:
                            tab.setText("CPU");
                            break;
                        case 2:
                            tab.setText("Battery");
                            break;
                        case 3:
                            tab.setText("HowToUse");
                            break;
                    }
                }).attach();
    }

    // Method để chuyển đến CPU tab từ SystemFragment
    public void switchToCpuTab() {
        viewPager.setCurrentItem(1, true);
    }

    // Method để chuyển đến Battery tab từ SystemFragment hoặc fragment khác
    public void switchToBatteryTab() {
        viewPager.setCurrentItem(2, true);
    }
    // Method để chuyển đến Battery tab từ SystemFragment hoặc fragment khác
    public void switchToHowToUseTab() {
        viewPager.setCurrentItem(3, true);
    }
}