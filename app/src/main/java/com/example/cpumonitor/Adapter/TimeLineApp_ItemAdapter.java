package com.example.cpumonitor.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppTimeLineItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeLineApp_ItemAdapter extends RecyclerView.Adapter<TimeLineApp_ItemAdapter.TimeLineApp_ItemViewHolder> {
    private List<AppTimeLineItem> listapp;
    private Context context;
    public TimeLineApp_ItemAdapter(Context context, List<AppTimeLineItem> listapp) {
        this.context = context;
        this.listapp = listapp;
    }

    @NonNull
    @Override
    public TimeLineApp_ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_timeline, parent, false);
        return new TimeLineApp_ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineApp_ItemAdapter.TimeLineApp_ItemViewHolder holder, int position) {
        AppTimeLineItem item = listapp.get(position);
        holder.imgappIcon.setImageDrawable(item.icon);
        holder.txtappName.setText(item.appName);
        long durationMs = item.TimeDuration;
        long h = durationMs / 3600000;
        long m = (durationMs % 3600000) / 60000;
        long s = (durationMs % 60000) / 1000;

        String durationStr = (h > 0 ? h + "h " : "") + m + "m " + s + "s";
        holder.txtusageDuration.setText(durationStr);

        SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date date = sdfFull.parse(item.Timeline);
            SimpleDateFormat sdfTimeOnly = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String timeOnly = sdfTimeOnly.format(date);
            holder.txttimelineTime.setText(timeOnly);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public int getItemCount() {
        return listapp.size();
    }
    public class TimeLineApp_ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imgappIcon;
        TextView txtappName, txtusageDuration, txttimelineTime;
        public TimeLineApp_ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgappIcon = itemView.findViewById(R.id.img_appIcon);
            txtappName = itemView.findViewById(R.id.txt_appName);
            txtusageDuration = itemView.findViewById(R.id.txt_appNameusageDuration);
            txttimelineTime = itemView.findViewById(R.id.txt_timelineTime);
        }
    }
}
