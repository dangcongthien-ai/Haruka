package com.example.finalproject.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DaySelectorAdapter extends RecyclerView.Adapter<DaySelectorAdapter.DayViewHolder> {
    public interface Listener {
        void onDaySelected(LocalDate date);
    }

    private final List<LocalDate> dates = new ArrayList<>();
    private final Listener listener;
    private LocalDate selectedDate;

    public DaySelectorAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<LocalDate> newDates, LocalDate selectedDate) {
        dates.clear();
        dates.addAll(newDates);
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_selector, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = dates.get(position);
        boolean selected = date.equals(selectedDate);
        boolean today = date.equals(LocalDate.now());
        holder.root.setBackground(selected
                ? UiUtils.rounded(holder.itemView.getContext().getColor(R.color.brand_orange), 8, holder.itemView.getContext())
                : UiUtils.roundedStroke(holder.itemView.getContext().getColor(R.color.surface), holder.itemView.getContext().getColor(R.color.brand_orange), 8, holder.itemView.getContext()));
        holder.name.setText(dayName(date));
        holder.number.setText(String.valueOf(date.getDayOfMonth()));
        int defaultColor = holder.itemView.getContext().getColor(today ? R.color.brand_orange : R.color.text_primary);
        holder.name.setTextColor(selected ? holder.itemView.getContext().getColor(R.color.white) : defaultColor);
        holder.number.setTextColor(selected ? holder.itemView.getContext().getColor(R.color.white) : defaultColor);
        holder.number.setTypeface(Typeface.DEFAULT_BOLD);
        holder.itemView.setOnClickListener(v -> listener.onDaySelected(date));
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
        final View root;
        final TextView name;
        final TextView number;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.day_selector_root);
            name = itemView.findViewById(R.id.tv_day_name);
            number = itemView.findViewById(R.id.tv_day_number);
        }
    }
}
