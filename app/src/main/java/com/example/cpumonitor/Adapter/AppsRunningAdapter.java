package com.example.cpumonitor.Adapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppItem;
import java.util.List;

public class AppsRunningAdapter extends RecyclerView.Adapter<AppsRunningAdapter.AppRunningViewHolder> {
    private final List<AppItem> AppItems;
    private final Context context;
    public AppsRunningAdapter(Context context, List<AppItem> AppItems) {
        this.context = context;
        this.AppItems = AppItems;
    }

    @NonNull
    @Override
    public AppRunningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tạo ra itemLayout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_runningapp, parent, false);
        return new AppRunningViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppRunningViewHolder holder, int position) {
        AppItem item = AppItems.get(position);
        holder.ivIcon.setImageDrawable(item.appIcon);
        holder.tvName.setText(item.appName);
        // Chuyển hướng tới trang thông tin ứng dụng trong cài đặt
        holder.btnStop.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + item.packageName));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return AppItems.size();
    }

    static class AppRunningViewHolder extends RecyclerView.ViewHolder{
        ImageView ivIcon;
        TextView tvName;
        Button btnStop;
        public AppRunningViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            btnStop = itemView.findViewById(R.id.btnStop);
        }
    }

}