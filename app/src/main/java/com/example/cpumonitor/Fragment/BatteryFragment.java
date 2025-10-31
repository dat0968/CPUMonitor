package com.example.cpumonitor.Fragment;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.cpumonitor.Adapter.AppsRunningAdapter;
import com.example.cpumonitor.Mapper.AppRunningItemMapper;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppRunningItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BatteryFragment extends Fragment {
    private TextView tvHealth, tvTemp, tvLevel, tvCapacity, tvVoltage, tvStatus, txtTemperatureAVG, txtBatteryLevelAVG, txtquantityAppRunning;
    private RecyclerView rvApps;
    private static final String PREFS_NAME = "BatteryLogs";
    private static final String LOG_KEY = "BatteryData";
    private final Handler handler = new Handler();
    private FrameLayout loadingOverlay;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        // Tải các ứng dụng đang chạy
        loadRunningApps();
        // Hiển thị thông tin chi tiết pin
        showBatteryDetail();
        //
        logBatteryOnce();
        // Update averages on app start
        new Handler().postDelayed(this::updateAverage, 100);
    }
    private void loadRunningApps() {
        List<AppRunningItem> appRunningItems = new ArrayList<>();
        if (getContext() == null) {
            displayApps(appRunningItems);
            return;
        }
        PackageManager pm = getContext().getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

        for (ResolveInfo info : resolveInfos) {
            try {
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (isSystemApp) {
                    continue;
                }
                String packageName = info.activityInfo.packageName;
                if (packageName.equals(getContext().getPackageName())) {
                    continue;
                }
                AppRunningItem item = AppRunningItemMapper.fromPackageInfo(pm, packageName);
                appRunningItems.add(item);
            } catch (Exception e) {
                Log.d("BatteryFragment", "loadRunningApps: error handling ResolveInfo", e);
            }
        }
        displayApps(appRunningItems);
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
    private void displayApps(List<AppRunningItem> appRunningItems) {
        txtquantityAppRunning.setText(appRunningItems.size() + " ứng dụng có thể tạm dừng để ngăn chặn tiêu hao pin.");
        rvApps.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 cột
        rvApps.setAdapter(new AppsRunningAdapter(getContext(), appRunningItems));
        if (rvApps == null) {
            Log.d("BatteryFragment", "displayApps: tblApps is null");
            return;
        }
        rvApps.post(() -> loadingOverlay.setVisibility(GONE));
        Log.d("BatteryFragment", "displayApps: rendered rows=" + rvApps.getChildCount());
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
                sumBattery += (float) obj.getDouble("battery");
                sumTemp += (float) obj.getDouble("temp");
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
    @SuppressLint("WrongViewCast")
    private void bindViews(View view) {
        tvHealth = view.findViewById(R.id.tvHealth);
        tvTemp = view.findViewById(R.id.tvTemp);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvCapacity = view.findViewById(R.id.tvCapacity);
        tvVoltage = view.findViewById(R.id.tvVoltage);
        tvStatus = view.findViewById(R.id.tvStatus);
        txtTemperatureAVG = view.findViewById(R.id.txtTemperatureAVG);
        txtBatteryLevelAVG = view.findViewById(R.id.txtBatteryLevelAVG);
        rvApps = view.findViewById(R.id.rvApps);
        txtquantityAppRunning = view.findViewById(R.id.txtquantityAppRunning);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        loadingOverlay.setVisibility(VISIBLE);
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