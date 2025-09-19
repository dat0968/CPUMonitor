package com.example.cpumonitor.Adapter;

import android.content.Context;
import android.content.Intent;
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

        holder.imgappIcon.setImageDrawable(item.icon);
        holder.txtappName.setText(item.appName);
        holder.txtusageDuration.setText(item.time);

        // Chuyển timestamp sang String (ví dụ format "HH:mm:ss")
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date(item.timestamp));
        holder.txttimelineTime.setText(timeStr);
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
