package com.example.finalproject.adapter;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeekHourAdapter extends RecyclerView.Adapter<WeekHourAdapter.HourViewHolder> {
    public interface Listener {
        void onEventClick(CalendarEvent event);
    }

    private final List<LocalDate> weekDates = new ArrayList<>();
    private final Map<String, List<CalendarEvent>> eventsByDateHour = new HashMap<>();
    private final Listener listener;

    public WeekHourAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<LocalDate> dates, List<CalendarEvent> events) {
        weekDates.clear();
        weekDates.addAll(dates);
        eventsByDateHour.clear();
        for (CalendarEvent event : events) {
            int hour = event.isAllDay() ? 0 : event.getStartDateTime().getHour();
            String key = key(event.getDate(), hour);
            if (!eventsByDateHour.containsKey(key)) {
                eventsByDateHour.put(key, new ArrayList<>());
            }
            eventsByDateHour.get(key).add(event);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_hour, parent, false);
        return new HourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourViewHolder holder, int hour) {
        holder.hour.setText(String.format("%02d:00", hour));
        holder.columns.removeAllViews();
        for (LocalDate date : weekDates) {
            LinearLayout column = new LinearLayout(holder.itemView.getContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setPadding(2, 2, 2, 2);
            column.setBackground(UiUtils.roundedStroke(Color.TRANSPARENT, holder.itemView.getContext().getColor(R.color.line), 0, holder.itemView.getContext()));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            column.setLayoutParams(params);
            List<CalendarEvent> events = eventsByDateHour.get(key(date, hour));
            if (events != null) {
                for (CalendarEvent event : events) {
                    TextView eventView = new TextView(holder.itemView.getContext());
                    eventView.setGravity(Gravity.CENTER);
                    eventView.setMaxLines(3);
                    eventView.setText(event.getTitle());
                    eventView.setTextColor(Color.BLACK);
                    eventView.setTextSize(10);
                    eventView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    int fallback = holder.itemView.getContext().getColor(R.color.event_blue);
                    eventView.setBackground(UiUtils.rounded(UiUtils.safeColor(event.getColor(), fallback), 6, holder.itemView.getContext()));
                    LinearLayout.LayoutParams eventParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
                    eventParams.setMargins(1, 1, 1, 1);
                    eventView.setLayoutParams(eventParams);
                    UiUtils.setDebouncedClickListener(eventView, () -> listener.onEventClick(event));
                    column.addView(eventView);
                }
            }
            holder.columns.addView(column);
        }
    }

    @Override
    public int getItemCount() {
        return 24;
    }

    private String key(LocalDate date, int hour) {
        return date.toString() + "#" + hour;
    }

    static class HourViewHolder extends RecyclerView.ViewHolder {
        final TextView hour;
        final LinearLayout columns;

        HourViewHolder(@NonNull View itemView) {
            super(itemView);
            hour = itemView.findViewById(R.id.tv_hour);
            columns = itemView.findViewById(R.id.week_columns_container);
        }
    }
}
