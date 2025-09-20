package com.example.cpumonitor.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppTimeline;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeLineAppAdapter extends RecyclerView.Adapter<TimeLineAppAdapter.TimeLineViewHolder> {
    private List<AppTimeline> listapp;
    private Context context;
    public TimeLineAppAdapter(Context context, List<AppTimeline> listapp) {
        this.context = context;
        this.listapp = listapp;
        // Sắp xếp giảm dần theo Timeline
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        this.listapp.sort((a, b) -> {
            try {
                Date dateA = sdf.parse(a.Timeline);
                Date dateB = sdf.parse(b.Timeline);
                return dateB.compareTo(dateA); // mới nhất lên đầu
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @NonNull
    @Override
    public TimeLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_timeline, parent, false);
        return new TimeLineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineViewHolder holder, int position) {
        AppTimeline item = listapp.get(position);
        try {
            item.icon = context.getPackageManager().getApplicationIcon(item._package);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        // Hiển thị icon và tên app
        holder.imgappIcon.setImageDrawable(item.icon);
        holder.txtappName.setText(item.appName);

        // Hiển thị duration dạng "2h 15m 30s"
        long durationMs = item.TimeDuration;
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        long hours = (durationMs / (1000 * 60 * 60));
        String durationStr = (hours > 0 ? hours + "h " : "")
                + (minutes > 0 ? minutes + "m " : "")
                + seconds + "s";
        holder.txtusageDuration.setText(durationStr);
        holder.txttimelineTime.setText(item.Timeline);
    }

    @Override
    public int getItemCount() {
        return listapp.size();
    }

    public static class TimeLineViewHolder extends RecyclerView.ViewHolder {
        ImageView imgappIcon;
        TextView txtappName, txtusageDuration, txttimelineTime;
        public TimeLineViewHolder(@NonNull View itemView) {
            super(itemView);
            imgappIcon = itemView.findViewById(R.id.imgappIcon);
            txtappName = itemView.findViewById(R.id.txtappName);
            txtusageDuration = itemView.findViewById(R.id.txtusageDuration);
            txttimelineTime = itemView.findViewById(R.id.txttimelineTime);
        }
    }
}
