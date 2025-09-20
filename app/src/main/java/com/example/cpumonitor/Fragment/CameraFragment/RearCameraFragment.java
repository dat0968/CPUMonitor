package com.example.cpumonitor.Fragment.CameraFragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.util.Size;
import android.util.SizeF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cpumonitor.R;

public class RearCameraFragment extends Fragment {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "BehindCameraFragment";
    private TextView megaPixelsTextView, pixelArraySizeTextView, sensorSizeTextView,
            focalLengthTextView, orientationTextView;
    // Tạo layout cho Fragment.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_behind_camera, container, false);
    }
    // Tương tác với các view con đã được tạo.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Gán ID cho TextView
        megaPixelsTextView = view.findViewById(R.id.txt_mega_pixels);
        pixelArraySizeTextView = view.findViewById(R.id.txt_pixel_array_size);
        sensorSizeTextView = view.findViewById(R.id.txt_sensor_size);
        focalLengthTextView = view.findViewById(R.id.txt_focal_length);
        orientationTextView = view.findViewById(R.id.txt_orientation);

        // Kiểm tra quyền CAMERA
        checkAndRequestCameraPermission();
    }
    /*
        lần 1: onCreate -> onStart -> onResume
        lần 2: Quay lại ứng dụng sau khi tạm dừng -> onRestart -> onStart -> onResume

        Mục đích: Khởi tạo các tài nguyên mà ta cần khi ứng dụng đang ở trạng thái tương tác
    */
    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra lại quyền khi Fragment được làm mới
        checkAndRequestCameraPermission();
    }

    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Quyền CAMERA đã có, gọi fetchCameraInfo");
            fetchCameraInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Quyền CAMERA vừa được cấp, gọi fetchCameraInfo");
                if (isAdded() && getView() != null) {
                    fetchCameraInfo();
                    // Làm mới UI
                    getView().invalidate();
                }
            } else {
                Toast.makeText(requireContext(), "Cần quyền camera để lấy thông tin!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Lấy thông tin camera
    public void fetchCameraInfo() {
        try {
            Log.d(TAG, "Bắt đầu lấy thông tin camera");
            CameraManager cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    // Megapixels
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        Size[] outputSizes = map.getOutputSizes(android.graphics.SurfaceTexture.class);
                        if (outputSizes != null && outputSizes.length > 0) {
                            Size largest = outputSizes[0];
                            double megaPixels = (largest.getWidth() * largest.getHeight()) / 1_000_000.0;
                            megaPixelsTextView.setText(String.format("%.2f Mega Pixels", megaPixels));
                            Log.d(TAG, "Megapixels: " + megaPixels);
                        } else {
                            megaPixelsTextView.setText("N/A");
                            Log.d(TAG, "OutputSizes không có");
                        }
                    } else {
                        megaPixelsTextView.setText("N/A");
                        Log.d(TAG, "StreamConfigurationMap không có");
                    }

                    // Kích thước mảng điểm ảnh
                    Size pixelArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    if (pixelArraySize != null) {
                        pixelArraySizeTextView.setText(String.format("%d x %d", pixelArraySize.getWidth(), pixelArraySize.getHeight()));
                        Log.d(TAG, "PixelArraySize: " + pixelArraySize.getWidth() + "x" + pixelArraySize.getHeight());
                    } else {
                        pixelArraySizeTextView.setText("N/A");
                        Log.d(TAG, "PixelArraySize không có");
                    }

                    // Kích cỡ cảm biến
                    SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    if (sensorSize != null) {
                        sensorSizeTextView.setText(String.format("%.2f x %.2f mm", sensorSize.getWidth(), sensorSize.getHeight()));
                        Log.d(TAG, "SensorSize: " + sensorSize.getWidth() + "x" + sensorSize.getHeight());
                    } else {
                        sensorSizeTextView.setText("N/A");
                        Log.d(TAG, "SensorSize không có");
                    }

                    // Độ dài tiêu cự
                    float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    if (focalLengths != null && focalLengths.length > 0) {
                        focalLengthTextView.setText(String.format("%.2f mm", focalLengths[0]));
                        Log.d(TAG, "FocalLength: " + focalLengths[0]);
                    } else {
                        focalLengthTextView.setText("N/A");
                        Log.d(TAG, "FocalLengths không có");
                    }

                    // Hướng
                    Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    if (sensorOrientation != null) {
                        orientationTextView.setText(String.format("%d deg", sensorOrientation));
                        Log.d(TAG, "Orientation: " + sensorOrientation);
                    } else {
                        orientationTextView.setText("N/A");
                        Log.d(TAG, "Orientation không có");
                    }

                    break; // Thoát sau khi xử lý camera sau
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Lỗi lấy thông tin camera: " + e.getMessage());
            Toast.makeText(requireContext(), "Lỗi khi lấy thông tin camera", Toast.LENGTH_SHORT).show();
        }
    }
}