package com.example.cpumonitor.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cpumonitor.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CPUFragment extends Fragment {

    private TextView txtModel, txtLoi, txtClock, txtpercentCPU;
    private GridLayout gridCpu;
    private TableLayout tblApps;
    private final Handler handler = new Handler();
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cpu, container, false);
        BindView(view);
        // Load thông tin CPU
        loadCpuInfo();
        // % CPU đang hoạt động
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
        handler.removeCallbacks(updateRunnable);
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
        txtModel = view.findViewById(R.id.txtModel);
        txtLoi = view.findViewById(R.id.txtLoi);
        txtClock = view.findViewById(R.id.txtClock);
        gridCpu = view.findViewById(R.id.gridCpu);
        txtpercentCPU = view.findViewById(R.id.txtpercentCPU);
        prefs = getContext().getSharedPreferences("System", Context.MODE_PRIVATE);
    }

    private void getCpuUsagePercent() {
        int cores = Runtime.getRuntime().availableProcessors();
        double sumPercent = 0;
        int validCores = 0;

        for (int i = 0; i < cores; i++) {
            long cur = readCpuCurFreqValue(i);
            String max = prefs.getString("maxVal_" + i, "0");
            if(max.equals("0")){
                max = readCpuMaxFreqValue(i) + "";
                prefs.edit().putString("maxVal_" + i, max).commit();
            }

            if (cur > 0 && Long.parseLong(max) > 0) {
                double percent = (cur * 1.0 / Long.parseLong(max)) * 100.0;
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
    private void loadCpuInfo() {
        // Kiểm tra fragment còn được attach không trước khi update UI
        if (!isAdded() || getActivity() == null) {
            return;
        }
        // Model CPU
        String model = prefs.getString("Model", "0");
        if(model.equals("0")){
            model = Build.HARDWARE;
            prefs.edit().putString("Model", model).commit();
        }
        txtModel.setText(model);

        // Số lõi
        String cores = prefs.getString("Cores", "0");
        if(cores.equals("0")){
            cores = Runtime.getRuntime().availableProcessors() + "";
            prefs.edit().putString("Cores", cores).commit();
        }
        txtLoi.setText(String.valueOf(cores));

        // Tần số CPU đầu tiên (MHz)
        String freq = prefs.getString("freqRange", "0");
        if(freq.equals("0")){
            freq = readCpuMaxFreqMHz(0);
            prefs.edit().putString("freqRange", freq).commit();
        }
        txtClock.setText(freq);

        // Hiển thị chi tiết từng core trong gridCpu
        gridCpu.removeAllViews(); // xóa trước khi vẽ
        for (int i = 0; i < Integer.parseInt(cores); i++) {
            long curVal = readCpuCurFreqValue(i); // kHz
            String maxVal = prefs.getString("maxVal_" + i, "0");
            if(maxVal.equals("0")){
                maxVal = readCpuMaxFreqValue(i) + ""; // kHz
                prefs.edit().putString("maxVal_" + i, maxVal);
            }

            String curFreq = formatMHz(curVal);
            String maxFreq = formatMHz(Long.parseLong(maxVal));

            String percent = "N/A";
            if (curVal > 0 && Long.parseLong(maxVal) > 0) {
                double p = (curVal * 1.0 / Long.parseLong(maxVal)) * 100.0;
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
            tv.setTextColor(Color.parseColor("#666161"));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(16, 16, 16, 16);
            tv.setLayoutParams(params);
            gridCpu.addView(tv);
        }
    }
    // Đọc tốc độ xử lý hiện tại với đơn vị Khz (kHz, long)
    private long readCpuCurFreqValue(int core) {
        String path = "/sys/devices/system/cpu/cpu" + core + "/cpufreq/scaling_cur_freq";
        return readFreqValue(path);
    }

    // Đọc tốc độ xử lý tối đa với đơn vị Khz (kHz, long)
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

    // Format kHz → MHz
    private String formatMHz(long khz) {
        if (khz <= 0) return "N/A";
        double mhz = khz / 1000.0;
        return String.format("%.0f MHz", mhz);
    }

    // Dùng cho txtClock
    private String readCpuMaxFreqMHz(int core) {
        long val = readCpuMaxFreqValue(core);
        // Đổi KHz sang MHz
        return formatMHz(val);
    }
}