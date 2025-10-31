package com.example.cpumonitor.Mapper;

import android.app.usage.UsageEvents;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.example.cpumonitor.Viewmodel.AppTimeLineItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppTimeLineItemMapper {
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static AppTimeLineItem fromEventData(PackageManager pm, String pkgName, long startTime, long endTime) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
            String appName = pm.getApplicationLabel(ai).toString();
            Drawable icon = pm.getApplicationIcon(ai);

            long duration = endTime - startTime;
            String timeStr = sdf.format(new Date(startTime));

            return new AppTimeLineItem(appName, icon, timeStr, duration, pkgName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null; // hoặc return 1 object rỗng tùy mày
        }
    }
}
