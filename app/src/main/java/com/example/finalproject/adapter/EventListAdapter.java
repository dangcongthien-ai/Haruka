package com.example.finalproject.adapter;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.ui.common.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {
    public interface Listener {
        void onClick(CalendarEvent event);

        void onEdit(CalendarEvent event);

        void onDelete(CalendarEvent event);
    }

    private final List<CalendarEvent> events = new ArrayList<>();
    private final Listener listener;
    private boolean showActions = true;

    public EventListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CalendarEvent> items) {
        events.clear();
        events.addAll(items);
        notifyDataSetChanged();
    }

    public void setShowActions(boolean showActions) {
        this.showActions = showActions;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_timeline, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = events.get(position);
        if (event.isAllDay()) {
            holder.startTime.setText(holder.itemView.getContext().getString(R.string.all_day_short));
            holder.startTime.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            holder.startTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            holder.endTime.setText("");
        } else {
            holder.startTime.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            holder.startTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            holder.startTime.setText(DateTimeUtils.formatVietnameseTime(event.getStartDateTime().toLocalTime()));
            holder.endTime.setText(event.getEndDateTime() == null ? "" : DateTimeUtils.formatVietnameseTime(event.getEndDateTime().toLocalTime()));
        }
        holder.endTime.setVisibility(event.isAllDay() ? View.GONE : View.VISIBLE);
        holder.title.setText(event.getTitle());
        int fallback = holder.itemView.getContext().getColor(R.color.event_blue);
        holder.colorBar.setBackground(UiUtils.rounded(UiUtils.safeColor(event.getColor(), fallback), 4, holder.itemView.getContext()));
        holder.edit.setVisibility(showActions ? View.VISIBLE : View.GONE);
        holder.delete.setVisibility(showActions ? View.VISIBLE : View.GONE);
        UiUtils.setDebouncedClickListener(holder.itemView, () -> listener.onClick(event));
        UiUtils.setDebouncedClickListener(holder.edit, () -> listener.onEdit(event));
        UiUtils.setDebouncedClickListener(holder.delete, () -> listener.onDelete(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView startTime;
        final TextView endTime;
        final View colorBar;
        final TextView title;
        final ImageButton edit;
        final ImageButton delete;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            startTime = itemView.findViewById(R.id.tv_start_time);
            endTime = itemView.findViewById(R.id.tv_end_time);
            colorBar = itemView.findViewById(R.id.color_bar);
            title = itemView.findViewById(R.id.tv_event_title);
            edit = itemView.findViewById(R.id.btn_event_edit);
            delete = itemView.findViewById(R.id.btn_event_delete);
        }
    }
}
