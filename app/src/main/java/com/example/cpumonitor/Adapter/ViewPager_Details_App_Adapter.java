package com.example.cpumonitor.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.cpumonitor.Fragment.StatisticDetailAppFragment;
import com.example.cpumonitor.Fragment.SingleTimeLineAppFragment;
import com.example.cpumonitor.Viewmodel.AppDetail;

public class ViewPager_Details_App_Adapter extends FragmentStateAdapter {
    private AppDetail app;
    public ViewPager_Details_App_Adapter(@NonNull FragmentActivity fa, AppDetail app) {
        super(fa);
        this.app = app;
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new StatisticDetailAppFragment(app);
            case 1: return new SingleTimeLineAppFragment(app);
            default: return new StatisticDetailAppFragment(app);
        }
    }

    @Override
    public int getItemCount() {
        return 2; // số tab
    }
}
