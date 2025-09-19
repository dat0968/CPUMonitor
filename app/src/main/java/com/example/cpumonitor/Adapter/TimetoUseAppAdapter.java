package com.example.cpumonitor.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.Activity.StatisticDetailAppActivity;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;

import java.util.List;

public class TimetoUseAppAdapter extends RecyclerView.Adapter<TimetoUseAppAdapter.TimeToUseAppViewHolder> {
    private  Context context;
    private List<AppDetail> listAppItem;
    public TimetoUseAppAdapter(Context context, List<AppDetail> listAppItem) {
        this.context = context;
        this.listAppItem = listAppItem;
    }

    @NonNull
    @Override
    public TimeToUseAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timetouseapp, parent, false);
        return new TimeToUseAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeToUseAppViewHolder holder, int position) {
        AppDetail app = listAppItem.get(position);
        holder.txtapp_name.setText(app.appName);
        holder.img_app_icon.setImageDrawable(app.appIcon);
        long sec = app.todayUsage / 1000;
        long h   = sec / 3600;
        long m   = (sec % 3600) / 60;
        long s   = sec % 60;
        String formatted = String.format("%02d:%02d:%02d", h, m, s);
        holder.txt_usage_time.setText(formatted + "");
        Log.d("AppDetailListapp",
                "appName=" + app.appName +
                        ", packageName=" + app.packageName +
                        ", todayUsage(ms)=" + app.todayUsage +
                        ", avgDailyUsage(ms)=" + app.avgDailyUsage +
                        ", maxDailyUsage(ms)=" + app.maxDailyUsage +
                        ", todayLaunchCount=" + app.todayLaunchCount +
                        ", continuousUsage(ms)=" + app.continuousUsage +
                        ", installTime=" + app.installTime);
        holder.lnl_itemTimetoUseApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, StatisticDetailAppActivity.class);
                intent.putExtra("appName", app.appName);
                intent.putExtra("packageName", app.packageName); // để load icon
                intent.putExtra("todayUsage", app.todayUsage);
                intent.putExtra("avgDailyUsage", app.avgDailyUsage);
                intent.putExtra("maxDailyUsage", app.maxDailyUsage);
                intent.putExtra("todayLaunchCount", app.todayLaunchCount);
                intent.putExtra("continuousUsage", app.continuousUsage);
                intent.putExtra("installTime", app.installTime);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listAppItem.size();
    }

    static class TimeToUseAppViewHolder extends RecyclerView.ViewHolder{
        TextView txtapp_name, txt_usage_time;
        SeekBar skb_usage_seekbar;
        ImageView img_app_icon;

        LinearLayout lnl_itemTimetoUseApp;
        public TimeToUseAppViewHolder(@NonNull View itemView) {
            super(itemView);
            txtapp_name = itemView.findViewById(R.id.txtapp_name);
            txt_usage_time = itemView.findViewById(R.id.txt_usage_time);
            skb_usage_seekbar = itemView.findViewById(R.id.skb_usage_seekbar);
            skb_usage_seekbar.setEnabled(false);
            img_app_icon = itemView.findViewById(R.id.img_app_icon);
            lnl_itemTimetoUseApp = itemView.findViewById(R.id.lnl_itemTimetoUseApp);
        }
    }
}
