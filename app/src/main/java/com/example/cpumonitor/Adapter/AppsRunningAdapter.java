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
import com.example.cpumonitor.Viewmodel.AppRunningItem;
import java.util.List;

public class AppsRunningAdapter extends RecyclerView.Adapter<AppsRunningAdapter.AppRunningViewHolder> {
    private final List<AppRunningItem> appRunningItems;
    private final Context context;
    public AppsRunningAdapter(Context context, List<AppRunningItem> appRunningItems) {
        this.context = context;
        this.appRunningItems = appRunningItems;
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
        AppRunningItem item = appRunningItems.get(position);

        holder.ivIcon.setImageDrawable(item.icon);
        holder.tvName.setText(item.name);

        holder.btnStop.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + item.packageName));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return appRunningItems.size();
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