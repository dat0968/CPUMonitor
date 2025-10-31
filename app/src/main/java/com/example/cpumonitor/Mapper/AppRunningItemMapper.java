package com.example.cpumonitor.Mapper;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.example.cpumonitor.Viewmodel.AppRunningItem;

public class AppRunningItemMapper {
    public static AppRunningItem fromPackageInfo(PackageManager pm, String packageName)
            throws PackageManager.NameNotFoundException {
        ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
        return new AppRunningItem() {{
            name = ai.loadLabel(pm).toString();
            icon = ai.loadIcon(pm);
            packageName = ai.packageName;
            timeForeground = 0L;
        }};
    }
}
