package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WeekDayHeaderAdapter extends RecyclerView.Adapter<WeekDayHeaderAdapter.DayViewHolder> {
    private final List<LocalDate> dates = new ArrayList<>();

    public WeekDayHeaderAdapter() {
    }

    public void submit(List<LocalDate> newDates) {
        dates.clear();
        dates.addAll(newDates);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_day_header, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = dates.get(position);
        holder.dayName.setText(dayName(date));
        holder.dayNumber.setText(String.valueOf(date.getDayOfMonth()));
        holder.dayName.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
        holder.dayNumber.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
        holder.dayNumberContainer.setBackground(null);
        holder.itemView.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    private String dayName(LocalDate date) {
        String[] names = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        return names[date.getDayOfWeek().getValue() % 7];
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayName;
        final FrameLayout dayNumberContainer;
        final TextView dayNumber;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayName = itemView.findViewById(R.id.tv_week_day_name);
            dayNumberContainer = itemView.findViewById(R.id.week_day_number_container);
            dayNumber = itemView.findViewById(R.id.tv_week_day_number);
        }
    }
}
