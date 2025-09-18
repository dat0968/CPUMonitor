package com.example.cpumonitor;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.cpumonitor.Fragment.BatteryFragment;
import com.example.cpumonitor.Fragment.CPUFragment;
import com.example.cpumonitor.Fragment.MainFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter adapter;

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
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(this);
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
                    }
                }).attach();

        // Thêm listener để reset SystemFragment khi click vào tab System
//        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//                if (tab.getPosition() == 0) { // Tab System
//                    Fragment fragment = getSupportFragmentManager()
//                            .findFragmentByTag("f" + 0); // ViewPager2 tag pattern
//                    if (fragment instanceof MainFragment) {
//                        ((MainFragment) fragment).resetToSystemView();
//                    }
//                }
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) {}
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) {
//                // Cũng reset khi click lại tab System đang active
//                if (tab.getPosition() == 0) {
//                    Fragment fragment = getSupportFragmentManager()
//                            .findFragmentByTag("f" + 0);
//                    if (fragment instanceof MainFragment) {
//                        ((MainFragment) fragment).resetToSystemView();
//                    }
//                }
//            }
//        });
    }

    // Method để chuyển đến CPU tab từ SystemFragment
    public void switchToCpuTab() {
        viewPager.setCurrentItem(1, true);
    }

    // Method để chuyển đến Battery tab từ SystemFragment hoặc fragment khác
    public void switchToBatteryTab() {
        viewPager.setCurrentItem(2, true);
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new MainFragment();
                case 1:
                    return new CPUFragment();
                case 2:
                    return new BatteryFragment();
                default:
                    return new MainFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // System, CPU, và Battery
        }
    }
}