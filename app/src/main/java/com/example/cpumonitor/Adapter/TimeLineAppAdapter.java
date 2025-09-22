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
import com.example.cpumonitor.Viewmodel.TimelineItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeLineAppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TIMELINE = 1;
    private List<TimelineItem> listapp;
    private Context context;
    public TimeLineAppAdapter(Context context, List<TimelineItem> listapp) {
        this.context = context;
        this.listapp = listapp;
    }

    @Override
    public int getItemViewType(int position) {
        return listapp.get(position).isHeader ? TYPE_HEADER : TYPE_TIMELINE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_app_timeline, parent, false);
            return new TimeLineViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TimelineItem item = listapp.get(position);
        if (item.isHeader) {
            ((HeaderViewHolder) holder).txtDate.setText(item.date);
        }
        else {
            TimeLineViewHolder h = (TimeLineViewHolder) holder;
            AppTimeline t = item.timeline;
            try {
                t.icon = context.getPackageManager().getApplicationIcon(t._package);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            // Hiển thị icon và tên app
            h.imgappIcon.setImageDrawable(t.icon);
            h.txtappName.setText(t.appName);

            // Hiển thị duration dạng "2h 15m 30s"
            long durationMs = t.TimeDuration;
            long seconds = (durationMs / 1000) % 60;
            long minutes = (durationMs / (1000 * 60)) % 60;
            long hours = (durationMs / (1000 * 60 * 60));
            String durationStr = (hours > 0 ? hours + "h " : "")
                    + (minutes > 0 ? minutes + "m " : "")
                    + seconds + "s";
            h.txtusageDuration.setText(durationStr);

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            try {
                Date date = inputFormat.parse(t.Timeline); // parse từ String sang Date
                String timeOnly = outputFormat.format(date); // lấy giờ:phút:giây
                h.txttimelineTime.setText(timeOnly);
            } catch (ParseException e) {
                e.printStackTrace();
                h.txttimelineTime.setText(t.Timeline); // fallback nếu parse lỗi
            }
        }
    }

    @Override
    public int getItemCount() {
        return listapp.size();
    }
    // ViewHolder cho header
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate;
        HeaderViewHolder(View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtDateHeader);
        }
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
