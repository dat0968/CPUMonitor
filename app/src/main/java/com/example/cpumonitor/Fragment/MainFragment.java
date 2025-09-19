package com.example.cpumonitor.Fragment;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.cpumonitor.Fragment.BehindCameraFragment;
import com.example.cpumonitor.Fragment.InFrontOfFragment;
import com.example.cpumonitor.MainActivity;
import com.example.cpumonitor.R;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainFragment extends Fragment {

    // Views
    private TextView tvInternal, tvUsed, tvFree, tvRam, tvCpuArch, tvCpuCores, tvCpuFreq,
            tvHealth, tvTemp, tvLevel, tvCapacity, tvVoltage, tvStatus, tvPlugged, tvTechnology,
            tvManufacturer, tvModel, tvBrand, tvAndroidID, tvAndroidVersion, tvApiLevel,
            tvSecurityPatch, tvRootAccess, tvUptime, tvResolution, tvDensity, tvSizeInches,
            tvRefreshRate, txtPixelCamera;
    private Button btnCheckCPU, btnCheckBattery;
    private MaterialButtonToggleGroup toggleGroup;
    private FrameLayout frCamera;
    private FragmentManager fragmentManager;

    private final Handler handler = new Handler();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        bindViews(view);
        setupClickListeners();
        loadSystemInfo();
        return view;
    }

    private void bindViews(View view) {
        tvHealth = view.findViewById(R.id.tvHealth);
        tvTemp = view.findViewById(R.id.tvTemp);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvCapacity = view.findViewById(R.id.tvCapacity);
        tvVoltage = view.findViewById(R.id.tvVoltage);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvPlugged = view.findViewById(R.id.tvPlugged);
        tvTechnology = view.findViewById(R.id.tvTechnology);
        tvInternal = view.findViewById(R.id.tvInternal);
        tvUsed = view.findViewById(R.id.tvUsed);
        tvFree = view.findViewById(R.id.tvFree);
        tvRam = view.findViewById(R.id.tvRam);
        tvCpuArch = view.findViewById(R.id.tvCpuArch);
        tvCpuCores = view.findViewById(R.id.tvCpuCores);
        tvCpuFreq = view.findViewById(R.id.tvCpuFreq);
        tvManufacturer = view.findViewById(R.id.tvManufacturer);
        tvModel = view.findViewById(R.id.tvModel);
        tvBrand = view.findViewById(R.id.tvBrand);
        tvAndroidID = view.findViewById(R.id.tvAndroidID);
        tvAndroidVersion = view.findViewById(R.id.tvAndroidVersion);
        tvApiLevel = view.findViewById(R.id.tvApiLevel);
        tvSecurityPatch = view.findViewById(R.id.tvSecurityPatch);
        tvRootAccess = view.findViewById(R.id.tvRootAccess);
        tvUptime = view.findViewById(R.id.tvUptime);
        tvResolution = view.findViewById(R.id.tvResolution);
        tvDensity = view.findViewById(R.id.tvDensity);
        tvSizeInches = view.findViewById(R.id.tvSizeInches);
        tvRefreshRate = view.findViewById(R.id.tvRefreshRate);
        toggleGroup = view.findViewById(R.id.cameraToggleGroup);
        frCamera = view.findViewById(R.id.frCamera);
        txtPixelCamera = view.findViewById(R.id.txtPixelCamera);
        btnCheckCPU = view.findViewById(R.id.btnCheckCPU);
        btnCheckBattery = view.findViewById(R.id.btnCheckBattery);
        fragmentManager = getChildFragmentManager();
    }

    private void setupClickListeners() {
        btnCheckCPU.setOnClickListener(v -> {
            // Navigate to CPU tab
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToCpuTab();
            }
        });
        btnCheckBattery.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToBatteryTab();
            }
        });
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnRear) {
                    BehindCameraFragment behindCameraFragment = new BehindCameraFragment();
                    fragmentManager.beginTransaction().replace(R.id.frCamera, behindCameraFragment).commit();
                } else if (checkedId == R.id.btnFront) {
                    InFrontOfFragment inFrontOfCameraFragment = new InFrontOfFragment();
                    fragmentManager.beginTransaction().replace(R.id.frCamera, inFrontOfCameraFragment).commit();
                }
            }
        });

        toggleGroup.setVisibility(INVISIBLE);
        txtPixelCamera.setOnClickListener(v -> checkAndRequestCameraPermission());
    }

    private void loadSystemInfo() {
        // Hiển thị bộ nhớ
        showStorageInfo();
        //  Hiển thị Ram
        showRamInfo();
        // Hiển thị CPU
        showCpuInfo();
        // Hiển thị thông tin pin
        showBatteryDetail();
        // Hiển thị thông tin thiết bị
        showDeviceInfo();
        // Hiển thị thông tin hệ thống
        showSystem();
        // Hiển thị thông tin màn hình
        showScreenInfo();

        startPeriodicUpdates();
    }

    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            txtPixelCamera.setVisibility(GONE);
            toggleGroup.setVisibility(VISIBLE);
            BehindCameraFragment behindCameraFragment = new BehindCameraFragment();
            fragmentManager.beginTransaction().replace(R.id.frCamera, behindCameraFragment).commit();
            updateCurrentFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                txtPixelCamera.setVisibility(GONE);
                toggleGroup.setVisibility(VISIBLE);
                BehindCameraFragment behindCameraFragment = new BehindCameraFragment();
                fragmentManager.beginTransaction().replace(R.id.frCamera, behindCameraFragment).commit();
                updateCurrentFragment();
            } else {
                Toast.makeText(getContext(), "Cần quyền camera để lấy thông tin!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateCurrentFragment() {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frCamera);
        if (currentFragment instanceof BehindCameraFragment) {
            ((BehindCameraFragment) currentFragment).fetchCameraInfo();
        } else if (currentFragment instanceof InFrontOfFragment) {
            ((InFrontOfFragment) currentFragment).fetchCameraInfo();
        }
    }

    private void showRamInfo() {
        ActivityManager activityManager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);

        long totalRam = memInfo.totalMem;
        long availRam = memInfo.availMem;
        long usedRam = totalRam - availRam;

        String ramText = formatSize(usedRam) + "/" + formatSize(totalRam);
        tvRam.setText(ramText);
    }

    private void showStorageInfo() {
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
        String arch = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "Unknown";
        tvCpuArch.setText(arch);

        int cores = Runtime.getRuntime().availableProcessors();
        tvCpuCores.setText(cores + " Cores");

        String freqRange = getCpuFreqRange();
        tvCpuFreq.setText(freqRange);
    }

    private String getCpuFreqRange() {
        try {
            File minFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");
            File maxFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");

            if (minFile.exists() && maxFile.exists()) {
                String min = readFirstLine(minFile);
                String max = readFirstLine(maxFile);

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

        // Tình trạng sạc (nguồn)
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String pluggedStr;
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_USB:
                pluggedStr = "USB Charging";
                break;
            case BatteryManager.BATTERY_PLUGGED_AC:
                pluggedStr = "AC Charging";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                pluggedStr = "Wireless";
                break;
            default:
                pluggedStr = "Not charging";
        }
        tvPlugged.setText(pluggedStr);

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

        // Công nghệ pin (Li-ion,…)
        String tech = battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        tvTechnology.setText(tech != null ? tech : "-");

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

    private String readFirstLine(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        br.close();
        return line;
    }

    @SuppressLint("HardwareIds")
    private void showDeviceInfo() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String brand = Build.BRAND;
        String androidID = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        tvManufacturer.setText(manufacturer);
        tvModel.setText(model);
        tvBrand.setText(brand);
        tvAndroidID.setText(androidID);
    }

    private void showSystem() {
        tvAndroidVersion.setText(Build.VERSION.RELEASE);
        tvApiLevel.setText(String.valueOf(Build.VERSION.SDK_INT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvSecurityPatch.setText(Build.VERSION.SECURITY_PATCH);
        } else {
            tvSecurityPatch.setText("N/A");
        }

        boolean isRooted = checkRoot();
        tvRootAccess.setText(isRooted ? "Đã root" : "Chưa root");

        long uptimeMillis = SystemClock.elapsedRealtime();
        tvUptime.setText(formatDuration(uptimeMillis));
    }

    private boolean checkRoot() {
        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su",
                "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private void showScreenInfo() {
        WindowManager wm = (WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

        String resolution = metrics.widthPixels + " x " + metrics.heightPixels + " Pixels";
        tvResolution.setText(resolution);

        String density = (int) metrics.densityDpi + " dpi";
        tvDensity.setText(density);

        double widthInches = metrics.widthPixels / (double) metrics.xdpi;
        double heightInches = metrics.heightPixels / (double) metrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));
        tvSizeInches.setText(String.format("%.2f inches", diagonalInches));

        float refreshRate = display.getRefreshRate();
        tvRefreshRate.setText(String.format("%.0f Hz", refreshRate));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }

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
    //
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && getActivity() != null) {
                showRamInfo();
                showBatteryDetail();
                showCpuInfo();

                // Update uptime
                long uptimeMillis = SystemClock.elapsedRealtime();
                tvUptime.setText(formatDuration(uptimeMillis));

                handler.postDelayed(this, 1000);
            }
        }
    };
    private void startPeriodicUpdates() {
        handler.post(updateRunnable);
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