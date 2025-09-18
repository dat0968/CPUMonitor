package com.example.cpumonitor.Fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cpumonitor.MainActivity;
import com.example.cpumonitor.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CPUFragment extends Fragment {

    private TextView txtModel, txtLoi, txtClock, txtpercentCPU;
    private GridLayout gridCpu;
    private TableLayout tblApps;
    private final Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cpu, container, false);
        BindView(view);
        // Load thông tin CPU
        loadCpuInfo();
        getCpuUsagePercent();
        return view;
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            // Kiểm tra fragment còn được attach không
            if (isAdded() && getActivity() != null) {
                loadCpuInfo();
                getCpuUsagePercent();
                // lặp lại sau 1 giây
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Runnable đã được đưa vào hàng đợi của Handler
        handler.post(updateRunnable); // bắt đầu cập nhật
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cleanup handler khi fragment bị destroy
        handler.removeCallbacks(updateRunnable);
    }

    private void BindView(View view) {
        // Ánh xạ view từ fragment view
        txtModel = view.findViewById(R.id.txtModel);
        txtLoi = view.findViewById(R.id.txtLoi);
        txtClock = view.findViewById(R.id.txtClock);
        gridCpu = view.findViewById(R.id.gridCpu);
        txtpercentCPU = view.findViewById(R.id.txtpercentCPU);
    }

    private void getCpuUsagePercent() {
        int cores = Runtime.getRuntime().availableProcessors();
        double sumPercent = 0;
        int validCores = 0;

        for (int i = 0; i < cores; i++) {
            long cur = readCpuCurFreqValue(i);
            long max = readCpuMaxFreqValue(i);

            if (cur > 0 && max > 0) {
                double percent = (cur * 1.0 / max) * 100.0;
                sumPercent += percent;
                validCores++;
            }
        }

        if (validCores > 0) {
            double avg = sumPercent / validCores;
            txtpercentCPU.setText(String.format("Loading: %.1f%%", avg));
        } else {
            txtpercentCPU.setText("Loading: N/A");
        }
    }

    // Đọc current freq (kHz, long)
    private long readCpuCurFreqValue(int core) {
        String path = "/sys/devices/system/cpu/cpu" + core + "/cpufreq/scaling_cur_freq";
        return readFreqValue(path);
    }

    // Đọc max freq (kHz, long)
    private long readCpuMaxFreqValue(int core) {
        String path = "/sys/devices/system/cpu/cpu" + core + "/cpufreq/cpuinfo_max_freq";
        return readFreqValue(path);
    }

    // Hàm đọc giá trị tần số kHz
    private long readFreqValue(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            if (line != null) {
                return Long.parseLong(line.trim());
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void loadCpuInfo() {
        // Kiểm tra fragment còn được attach không trước khi update UI
        if (!isAdded() || getActivity() == null) {
            return;
        }

        // Model CPU
        String model = Build.HARDWARE; // hoặc Build.BOARD
        txtModel.setText(model);

        // Số lõi
        int cores = Runtime.getRuntime().availableProcessors();
        txtLoi.setText(String.valueOf(cores));

        // Tần số CPU đầu tiên (MHz)
        String freq = readCpuMaxFreqMHz(0);
        txtClock.setText(freq);

        // Hiển thị chi tiết từng core trong gridCpu
        gridCpu.removeAllViews(); // xóa trước khi vẽ
        for (int i = 0; i < cores; i++) {
            long curVal = readCpuCurFreqValue(i); // kHz
            long maxVal = readCpuMaxFreqValue(i); // kHz

            String curFreq = formatMHz(curVal);
            String maxFreq = formatMHz(maxVal);

            String percent = "N/A";
            if (curVal > 0 && maxVal > 0) {
                double p = (curVal * 1.0 / maxVal) * 100.0;
                percent = String.format("%.1f%%", p);
            }

            TextView tv = new TextView(getContext());
            tv.setText(
                    "CPU " + i + "\n" +
                            "Cur: " + curFreq + "\n" +
                            "Max: " + maxFreq + "\n" +
                            "Use: " + percent
            );
            tv.setBackgroundResource(R.drawable.bg_rounded); // bo góc (tự bạn định nghĩa drawable)
            tv.setPadding(16, 16, 16, 16);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(16, 16, 16, 16);
            tv.setLayoutParams(params);

            gridCpu.addView(tv);
        }
    }

    // Format kHz → MHz
    private String formatMHz(long khz) {
        if (khz <= 0) return "N/A";
        double mhz = khz / 1000.0;
        return String.format("%.0f MHz", mhz);
    }

    // Dùng cho txtClock
    private String readCpuMaxFreqMHz(int core) {
        long val = readCpuMaxFreqValue(core);
        return formatMHz(val);
    }
}