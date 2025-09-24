package com.example.cpumonitor.Adapter;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.cpumonitor.Fragment.AppUsageFragment;
import com.example.cpumonitor.Fragment.BatteryFragment;
import com.example.cpumonitor.Fragment.CPUFragment;
import com.example.cpumonitor.Fragment.SystemFragment;

public class ViewPager_MainActivity_Adapter extends FragmentStateAdapter {
    public ViewPager_MainActivity_Adapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> new SystemFragment();
            case 1 -> new CPUFragment();
            case 2 -> new BatteryFragment();
            case 3 -> new AppUsageFragment();
            default -> new SystemFragment();
        };
    }

    @Override
    public int getItemCount() {
        return 4; // System, CPU, và Battery
    }
}
