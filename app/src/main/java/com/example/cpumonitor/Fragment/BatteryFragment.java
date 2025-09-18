package com.example.cpumonitor.Fragment;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cpumonitor.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BatteryFragment extends Fragment {
    // Views
    private TextView tvHealth, tvTemp, tvLevel, tvCapacity, tvVoltage, tvStatus, txtTemperatureAVG, txtBatteryLevelAVG;
    private TableLayout tblApps;

    private static final String PREFS_NAME = "BatteryLogs";
    private static final String LOG_KEY = "BatteryData";
    private final Handler handler = new Handler();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        if (hasUsageStatsPermission(getContext())) {
            loadRunningApps();
        } else {
            requestUsageStatsPermission();
        }
        showBatteryDetail();
        logBatteryOnce();
        // Update averages on app start
        new Handler().postDelayed(this::updateAverage, 100);

    }
    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsageStatsPermission() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        Toast.makeText(getContext(), "Hãy cấp quyền truy cập để xem app đang chạy", Toast.LENGTH_LONG).show();
    }
    private void loadRunningApps() {
        List<AppItem> appItems = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER); PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        // Sắp xếp theo tên app cho gọn
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));
        for (ResolveInfo info : resolveInfos) {
            // Tên ứng dụng
            String appName = info.loadLabel(pm).toString();
            Drawable appIcon = info.loadIcon(pm);
            String packageName = info.activityInfo.packageName;
            appItems.add(new AppItem(appName, appIcon, packageName));
        }
        displayApps(appItems);
    }
    private void logBatteryOnce() {
        // Lấy thông tin pin
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = requireContext().registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            // Lấy % pin
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = 0f;
            if (level >= 0 && scale > 0) {
                batteryPct = level * 100f / scale;
            }

            // Lấy nhiệt độ pin
            int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            float batteryTempC = temp >= 0 ? temp / 10f : 0f;
            // Lưu dữ liệu vào SharedPreferences
            saveData(System.currentTimeMillis(), batteryPct, batteryTempC);
        } else {
            Log.w("BatteryFragment", "Battery status intent is null!");
        }
    }
    private void displayApps(List<AppItem> appItems) {
        if (tblApps == null) return;
        tblApps.removeAllViews();

        int appsPerRow = 3;
        TableRow tableRow = null;
        int margin = 16;

        for (int i = 0; i < appItems.size(); i++) {
            if (i % appsPerRow == 0) {
                tableRow = new TableRow(getContext());
                tableRow.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT
                ));
                tblApps.addView(tableRow);
            }

            LinearLayout appLayout = new LinearLayout(getContext());
            appLayout.setOrientation(LinearLayout.VERTICAL);
            appLayout.setGravity(Gravity.CENTER);

            TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(margin, margin, margin, margin);
            appLayout.setLayoutParams(params);

            ImageView ivIcon = new ImageView(getContext());
            ivIcon.setImageDrawable(appItems.get(i).icon);
            ivIcon.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            appLayout.addView(ivIcon);

            TextView tvName = new TextView(getContext());
            tvName.setText(appItems.get(i).name);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextColor(Color.BLACK);
            tvName.setPadding(0, 8, 0, 8);
            appLayout.addView(tvName);

            Button btnStop = new Button(getContext());
            btnStop.setText("Tạm dừng");
            btnStop.setBackgroundColor(Color.parseColor("#4CAF50"));
            btnStop.setTextColor(Color.WHITE);
            appLayout.addView(btnStop);

            final String pkg = appItems.get(i).packageName;
            btnStop.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                startActivity(intent);
            });

            tableRow.addView(appLayout);
        }
    }


    private static class AppItem {
        String name;
        Drawable icon;
        String packageName;
        AppItem(String name, Drawable icon, String packageName) {
            this.name = name;
            this.icon = icon;
            this.packageName = packageName;
        }
    }
    private void saveData(long timestamp, float battery, float temp) {
        // Lấy SharedPreferences của Fragment
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(LOG_KEY, "[]"); // nếu chưa có dữ liệu thì tạo mảng rỗng

        try {
            JSONArray arr = new JSONArray(jsonStr);

            // Tạo JSONObject mới cho bản ghi hiện tại
            JSONObject obj = new JSONObject();
            obj.put("timestamp", timestamp);
            obj.put("battery", battery);
            obj.put("temp", temp);
            arr.put(obj);

            // Lọc dữ liệu cũ > 24h
            long cutoff = timestamp - 24 * 60 * 60 * 1000L; // 24 giờ trước
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getLong("timestamp") >= cutoff) {
                    newArr.put(o);
                }
            }

            // Lưu lại SharedPreferences
            prefs.edit().putString(LOG_KEY, newArr.toString()).apply();

            // Debug log
            Log.d("BatteryFragment", "Saved battery: " + battery + "%, temp: " + temp + "°C");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void updateAverage() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(LOG_KEY, "[]");

        try {
            JSONArray arr = new JSONArray(jsonStr);
            float sumBattery = 0;
            float sumTemp = 0;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                sumBattery += obj.getDouble("battery");
                sumTemp += obj.getDouble("temp");
            }

            int count = arr.length();
            float avgBattery = count > 0 ? sumBattery / count : 0;
            float avgTemp = count > 0 ? sumTemp / count : 0;

            txtBatteryLevelAVG.setText(String.format("%.2f", avgBattery) + "%");
            txtTemperatureAVG.setText(String.format("%.2f", avgTemp) + "°C");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void bindViews(View view) {
        tvHealth = view.findViewById(R.id.tvHealth);
        tvTemp = view.findViewById(R.id.tvTemp);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvCapacity = view.findViewById(R.id.tvCapacity);
        tvVoltage = view.findViewById(R.id.tvVoltage);
        tvStatus = view.findViewById(R.id.tvStatus);
        txtTemperatureAVG = view.findViewById(R.id.txtTemperatureAVG);
        txtBatteryLevelAVG = view.findViewById(R.id.txtBatteryLevelAVG);
        tblApps = view.findViewById(R.id.tblApps);
    }
    public void showBatteryDetail(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = requireContext().registerReceiver(null, ifilter);
        if (battery == null) return;

        // Mức pin %
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct = (int) (level * 100f / scale);
        tvLevel.setText(pct + "%");

        // Tình trạng pin
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String statusStr;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusStr = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusStr = "Full";
                break;
            default:
                statusStr = "Discharging";
        }
        tvStatus.setText(statusStr);


        // Tình trạng sức khỏe
        int health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        String healthStr;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthStr = "Good";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthStr = "Overheat";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthStr = "Dead";
                break;
            default:
                healthStr = "Unknown";
        }
        tvHealth.setText(healthStr);

        // Nhiệt độ °C
        int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        tvTemp.setText((temp / 10f) + "°C");

        // Điện áp mV -> V
        int voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        tvVoltage.setText(String.format("%.1fV", voltage / 1000f));


        // Tổng công suất
        BatteryManager bm = (BatteryManager) requireContext().getSystemService(Context.BATTERY_SERVICE);
        if (bm == null) {
            tvCapacity.setText("N/A");
            return;
        }

        long chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        long capacityPercent = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        if (chargeCounter == Long.MIN_VALUE || capacityPercent <= 0) {
            tvCapacity.setText("N/A");
            return;
        }

        double totalMah = chargeCounter / (capacityPercent / 100.0) / 1000.0;
        tvCapacity.setText(String.format("%.0f mAh", totalMah));
    }
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && getActivity() != null) {
                showBatteryDetail();
                handler.postDelayed(this, 1000);
            }
        }
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_battery, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(updateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateRunnable);
    }
}