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

import com.example.cpumonitor.Activity.DetailsAppActivity;
import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.AppDetail;

import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.TimeToUseAppViewHolder> {
    private  Context context;
    private List<AppDetail> listAppItem;
    private long maxUsage = 0;
    private static final int ONE_DAY_MS = 24 * 60 * 60 * 1000; // 86_400_000

    public AppUsageAdapter(Context context, List<AppDetail> listAppItem) {
        this.context = context;
        this.listAppItem = listAppItem;
    }

    @NonNull
    @Override
    public TimeToUseAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_usage, parent, false);
        return new TimeToUseAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeToUseAppViewHolder holder, int position) {
        AppDetail app = listAppItem.get(position);
        holder.txtapp_name.setText(app.appName);
        holder.img_app_icon.setImageDrawable(app.appIcon);
        // Ngắn kéo sự kiện seekbar
        holder.skb_usage_seekbar.setEnabled(true);
        holder.skb_usage_seekbar.setOnTouchListener((v, event) -> true);
        // Quy đổi kiểu long từ ms → h:m:s
        long sec = app.todayUsage / 1000;
        long h   = sec / 3600;
        long m   = (sec % 3600) / 60;
        long s   = sec % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        sb.append(m).append("m ").append(s).append("s");
        String formatted = sb.toString().trim();

        holder.txt_usage_time.setText(formatted);

        // set SeekBar: 1 ngày = 100%
        holder.skb_usage_seekbar.setMax(ONE_DAY_MS);

        // progress phải là int và không vượt quá ONE_DAY_MS
        int progress = (int) Math.min(app.todayUsage, (long) ONE_DAY_MS);
        holder.skb_usage_seekbar.setProgress(progress);



        Log.d("AppUsageAdapter",
                "App: " + app.appName
                        + "  todayUsage=" + app.todayUsage
                        + "  maxUsage=" + maxUsage
                        + "  progress=" + holder.skb_usage_seekbar.getProgress());

        holder.lnl_itemTimetoUseApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, DetailsAppActivity.class);
                intent.putExtra("appName", app.appName);
                // để load icon
                intent.putExtra("packageName", app.packageName);
                intent.putExtra("todayUsage", app.todayUsage);
                /*intent.putExtra("avgDailyUsage", app.avgDailyUsage);
                intent.putExtra("maxDailyUsage", app.maxDailyUsage);
                intent.putExtra("todayLaunchCount", app.todayLaunchCount);
                intent.putExtra("continuousUsage", app.continuousUsage);*/
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

            img_app_icon = itemView.findViewById(R.id.img_app_icon);
            lnl_itemTimetoUseApp = itemView.findViewById(R.id.lnl_itemTimetoUseApp);
        }
    }
}
