package com.example.cpumonitor.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cpumonitor.R;
import com.example.cpumonitor.Viewmodel.HeaderTimelineItem;

import java.util.List;

public class TimeLineAppAdapter extends RecyclerView.Adapter<TimeLineAppAdapter.TimeLineAppViewHolder> {
    private Context context;
    private List<HeaderTimelineItem> headerTimelineItemList;
    public TimeLineAppAdapter(Context context, List<HeaderTimelineItem> headerTimelineItemList){
        this.context = context;
        this.headerTimelineItemList = headerTimelineItemList;
    }
    @NonNull
    @Override
    public TimeLineAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_header, parent, false);
        return new TimeLineAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineAppViewHolder holder, int position) {
        HeaderTimelineItem item =  headerTimelineItemList.get(position);
        holder.txtDateHeader.setText(item.dateStart);

        holder.rcv_Item_TimelineApp.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        TimeLineApp_ItemAdapter adapter = new TimeLineApp_ItemAdapter(context, item.appTimelineListItem);
        if (!headerTimelineItemList.isEmpty()) {
            headerTimelineItemList.get(0).isExpanded = true;
        }
        holder.rcv_Item_TimelineApp.setAdapter(adapter);
        holder.rcv_Item_TimelineApp.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);
        holder.txtDateHeader.setOnClickListener(v -> {
            item.isExpanded = !item.isExpanded;
            //notifyItemChanged(position);
            holder.rcv_Item_TimelineApp.setVisibility(
                    holder.rcv_Item_TimelineApp.getVisibility() == View.VISIBLE
                            ? View.GONE : View.VISIBLE
            );
        });
    }

    @Override
    public int getItemCount() {
        return headerTimelineItemList.size();
    }
    static class TimeLineAppViewHolder extends  RecyclerView.ViewHolder{
        RecyclerView rcv_Item_TimelineApp;
        TextView txtDateHeader;
        public TimeLineAppViewHolder(@NonNull View itemView) {
            super(itemView);
            this.rcv_Item_TimelineApp = itemView.findViewById((R.id.rcv_Item_TimelineApp));
            this.txtDateHeader = itemView.findViewById(R.id.txtDateHeader);
        }
    }
}
