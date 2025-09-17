package com.example.cpumonitor;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.cpumonitor.Fragment.BehindCameraFragment;
import com.example.cpumonitor.Fragment.InFrontOfFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private TextView tvInternal, tvUsed, tvFree, tvRam, tvCpuArch, tvCpuCores, tvCpuFreq,
            tvHealth, tvTemp, tvLevel, tvCapacity, tvVoltage, tvStatus, tvPlugged, tvTechnology, tvManufacturer,
            tvModel, tvBrand, tvAndroidID, tvAndroidVersion, tvApiLevel, tvSecurityPatch, tvRootAccess, tvUptime,
            tvResolution, tvDensity, tvSizeInches, tvRefreshRate, txtPixelCamera;
    private MaterialButtonToggleGroup toggleGroup;
    private FrameLayout frCamera;
    private FragmentManager fragmentManager;
    private final Handler handler = new Handler();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        bindViews();
        showStorageInfo();
        showRamInfo();
        showCpuInfo();
        showBatteryDetail();
        showDeviceInfo();
        showSystem();
        showScreenInfo();
        toggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    if (checkedId == R.id.btnRear) {
                        // Hiển thị thông tin / fragment cho camera sau
                        BehindCameraFragment behindCameraFragment = new BehindCameraFragment();
                        fragmentManager.beginTransaction().replace(R.id.frCamera, behindCameraFragment).commit();
                    } else if (checkedId == R.id.btnFront) {
                        // Hiển thị thông tin / fragment cho camera trước
                        InFrontOfFragment InFrontOfCameraFragment = new InFrontOfFragment();
                        fragmentManager.beginTransaction().replace(R.id.frCamera, InFrontOfCameraFragment).commit();
                    }
                }
            }
        });
        toggleGroup.setVisibility(INVISIBLE);
        txtPixelCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestCameraPermission();
            }
        });
    }
    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
           // updateInFrontOfFragment();
            txtPixelCamera.setVisibility(GONE);
            toggleGroup.setVisibility(VISIBLE);
            BehindCameraFragment behindCameraFragment = new BehindCameraFragment();
            fragmentManager.beginTransaction().replace(R.id.frCamera, behindCameraFragment).commit();
            updateCurrentFragment();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                txtPixelCamera.setVisibility(GONE);
                toggleGroup.setVisibility(VISIBLE);
                BehindCameraFragment behindCameraFragment = new BehindCameraFragment();
                fragmentManager.beginTransaction().replace(R.id.frCamera, behindCameraFragment).commit();
                updateCurrentFragment();
            } else {
                Toast.makeText(this, "Cần quyền camera để lấy thông tin!", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void updateCurrentFragment() {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frCamera);
        if (currentFragment instanceof BehindCameraFragment) {
            ((BehindCameraFragment) currentFragment).fetchCameraInfo();
        } else if (currentFragment instanceof InFrontOfFragment) {
            ((InFrontOfFragment) currentFragment).fetchCameraInfo();
        } else {
        }
    }
    private void bindViews() {
        tvHealth     = findViewById(R.id.tvHealth);
        tvTemp       = findViewById(R.id.tvTemp);
        tvLevel      = findViewById(R.id.tvLevel);
        tvCapacity   = findViewById(R.id.tvCapacity);
        tvVoltage    = findViewById(R.id.tvVoltage);
        tvStatus     = findViewById(R.id.tvStatus);
        tvPlugged    = findViewById(R.id.tvPlugged);
        tvTechnology = findViewById(R.id.tvTechnology);
        tvInternal = findViewById(R.id.tvInternal);
        tvUsed = findViewById(R.id.tvUsed);
        tvFree = findViewById(R.id.tvFree);
        tvRam = findViewById(R.id.tvRam);
        tvCpuArch  = findViewById(R.id.tvCpuArch);
        tvCpuCores = findViewById(R.id.tvCpuCores);
        tvCpuFreq  = findViewById(R.id.tvCpuFreq);
        tvManufacturer = findViewById(R.id.tvManufacturer);
        tvModel        = findViewById(R.id.tvModel);
        tvBrand        = findViewById(R.id.tvBrand);
        tvAndroidID    = findViewById(R.id.tvAndroidID);
        tvAndroidVersion  = findViewById(R.id.tvAndroidVersion);
        tvApiLevel        = findViewById(R.id.tvApiLevel);
        tvSecurityPatch   = findViewById(R.id.tvSecurityPatch);
        tvRootAccess      = findViewById(R.id.tvRootAccess);
        tvUptime          = findViewById(R.id.tvUptime);
        tvResolution  = findViewById(R.id.tvResolution);
        tvDensity     = findViewById(R.id.tvDensity);
        tvSizeInches  = findViewById(R.id.tvSizeInches);
        tvRefreshRate = findViewById(R.id.tvRefreshRate);
        toggleGroup = findViewById(R.id.cameraToggleGroup);
        frCamera = findViewById(R.id.frCamera);
        txtPixelCamera = findViewById(R.id.txtPixelCamera);
        fragmentManager = getSupportFragmentManager();
    }
    private void showRamInfo() {
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);

        long totalRam = memInfo.totalMem;          // Tổng RAM (byte)
        long availRam = memInfo.availMem;          // RAM còn trống (byte)
        long usedRam  = totalRam - availRam;       // RAM đã dùng (byte)

        String ramText = formatSize(usedRam) + "/" + formatSize(totalRam);
        tvRam.setText(ramText);
    }
    private void showStorageInfo() {
        // Đường dẫn bộ nhớ trong (internal storage)
        StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());

        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();

        long totalBytes = blockSize * totalBlocks;
        long freeBytes = blockSize * availableBlocks;
        long usedBytes = totalBytes - freeBytes;

        tvInternal.setText(formatSize(totalBytes));
        tvUsed.setText(formatSize(usedBytes));
        tvFree.setText(formatSize(freeBytes));
    }
    private void showCpuInfo() {
        // 1️⃣ Kiến trúc CPU
        String arch = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "Unknown";
        tvCpuArch.setText(arch);

        // 2️⃣ Số lõi
        int cores = Runtime.getRuntime().availableProcessors();
        tvCpuCores.setText(cores + " Cores");

        // 3️⃣ Phạm vi xung nhịp
        String freqRange = getCpuFreqRange();
        tvCpuFreq.setText(freqRange);
    }
    // Đọc dải xung nhịp CPU từ /sys/devices/system/cpu/cpu0/cpufreq
    private String getCpuFreqRange() {
        try {
            File minFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");
            File maxFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");

            if (minFile.exists() && maxFile.exists()) {
                String min = readFirstLine(minFile);
                String max = readFirstLine(maxFile);

                // Giá trị trong file là kHz -> chuyển MHz
                long minMHz = Long.parseLong(min.trim()) / 1000;
                long maxMHz = Long.parseLong(max.trim()) / 1000;

                return minMHz + "MHz - " + maxMHz + "MHz";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Không truy xuất được";
    }
    private void showBatteryDetail() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = registerReceiver(null, ifilter);
        if (battery == null) return;

        // Mức pin %
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct = (int)(level * 100f / scale);
        tvLevel.setText(pct + "%");

        // Tình trạng pin
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String statusStr;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: statusStr = "Charging"; break;
            case BatteryManager.BATTERY_STATUS_FULL:     statusStr = "Full";     break;
            default: statusStr = "Discharging";
        }
        tvStatus.setText(statusStr);

        // Tình trạng sạc (nguồn)
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String pluggedStr;
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_USB:  pluggedStr = "USB Charging"; break;
            case BatteryManager.BATTERY_PLUGGED_AC:   pluggedStr = "AC Charging";  break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: pluggedStr = "Wireless"; break;
            default: pluggedStr = "Not charging";
        }
        tvPlugged.setText(pluggedStr);

        // Tình trạng sức khỏe
        int health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        String healthStr;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: healthStr = "Good"; break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: healthStr = "Overheat"; break;
            case BatteryManager.BATTERY_HEALTH_DEAD: healthStr = "Dead"; break;
            default: healthStr = "Unknown";
        }
        tvHealth.setText(healthStr);

        // Nhiệt độ °C
        int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        tvTemp.setText((temp/10f) + "°C");

        // Điện áp mV -> V
        int voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        tvVoltage.setText(String.format("%.1fV", voltage/1000f));

        // Công nghệ pin (Li-ion,…)
        String tech = battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        tvTechnology.setText(tech != null ? tech : "-");

        // Tổng công suất

        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm == null) {
            tvCapacity.setText("N/A");
            return;
        }

        // chargeCounter: dung lượng hiện tại (µAh)
        long chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        // capacityPercent: phần trăm pin hiện tại (%)
        long capacityPercent = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        if (chargeCounter == Long.MIN_VALUE || capacityPercent <= 0) {
            // Thiết bị không hỗ trợ
            tvCapacity.setText("N/A");
            return;
        }

        // Tính tổng dung lượng mAh ước lượng
        double totalMah = chargeCounter / (capacityPercent / 100.0) / 1000.0; // µAh -> mAh
        tvCapacity.setText(String.format("%.0f mAh", totalMah));
    }
    private String readFirstLine(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        br.close();
        return line;
    }

    private void showDeviceInfo(){
        // Lấy thông tin thiết bị
        String manufacturer = Build.MANUFACTURER;     // Hãng sản xuất
        String model        = Build.MODEL;            // Tên model
        String brand        = Build.BRAND;            // Brand
        @SuppressLint("HardwareIds") String androidID    = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );                       // Android ID (một định danh duy nhất của thiết bị)

        // Gán vào TextView
        tvManufacturer.setText(manufacturer);
        tvModel.setText(model);
        tvBrand.setText(brand);
        tvAndroidID.setText(androidID);
    }
    private  void showSystem(){
        // 1. Phiên bản Android
        tvAndroidVersion.setText(Build.VERSION.RELEASE);

        // 2. API Level
        tvApiLevel.setText(String.valueOf(Build.VERSION.SDK_INT));

        // 3. Security Patch Level (yêu cầu Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvSecurityPatch.setText(Build.VERSION.SECURITY_PATCH);
        } else {
            tvSecurityPatch.setText("N/A");
        }

        // 4. Truy cập gốc (Root access) – kiểm tra đơn giản:
        boolean isRooted = checkRoot();
        tvRootAccess.setText(isRooted ? "Đã root" : "Chưa root");

        // 5. Thời gian hoạt động (kể từ khi khởi động, không tính deep sleep)
        long uptimeMillis = SystemClock.elapsedRealtime();
        tvUptime.setText(formatDuration(uptimeMillis));
    }
    // Hàm kiểm tra root cơ bản
    private boolean checkRoot() {
        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su",
                "/data/local/su"
        };
        for (String path : paths) {
            if (new java.io.File(path).exists()) return true;
        }
        return false;
    }

    private void showScreenInfo(){
        // Lấy DisplayMetrics từ màn hình hiện tại
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealMetrics(metrics); // kích thước thực (kể cả thanh điều hướng)

        // 1️⃣ Độ phân giải (pixel)
        String resolution = metrics.widthPixels + " x " + metrics.heightPixels + " Pixels";
        tvResolution.setText(resolution);

        // 2️⃣ Screen Density (dpi)
        String density = (int)metrics.densityDpi + " dpi";
        tvDensity.setText(density);

        // 3️⃣ Kích thước màn hình (inch)
        double widthInches  = metrics.widthPixels  / (double)metrics.xdpi;
        double heightInches = metrics.heightPixels / (double)metrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(widthInches,2) + Math.pow(heightInches,2));
        tvSizeInches.setText(String.format("%.2f inches", diagonalInches));

        // 4️⃣ Tốc độ làm mới (Refresh Rate)
        float refreshRate = display.getRefreshRate();
        tvRefreshRate.setText(String.format("%.0f Hz", refreshRate));
    }

    // Định dạng thời gian uptime thành giờ:phút:giây
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours   = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec     = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }
    // Chuyển byte -> dạng GB hoặc MB cho gọn
    private String formatSize(long size) {
        double kb = size / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb >= 1) {
            return String.format("%.1f GB", gb);
        } else if (mb >= 1) {
            return String.format("%.1f MB", mb);
        } else {
            return String.format("%.1f KB", kb);
        }
    }
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            showRamInfo();
            showBatteryDetail();
            showCpuInfo(); // nếu muốn cập nhật tần số CPU
            // lặp lại sau 1 giây
            handler.postDelayed(this, 1000);
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        // Runnable đã được đưa vào hàng đợi của Handler
        handler.post(updateRunnable); // bắt đầu cập nhật
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable); // ngừng khi Activity không hiển thị
    }
}