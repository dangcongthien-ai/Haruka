package com.example.finalproject.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.model.CalendarDayCell;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.DayViewHolder> {
    public interface OnDayClickListener {
        void onDayClick(LocalDate date);
    }

    private final List<CalendarDayCell> cells = new ArrayList<>();
    private final OnDayClickListener listener;
    private LocalDate selectedDate;

    public CalendarMonthAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<CalendarDayCell> newCells, LocalDate selectedDate) {
        cells.clear();
        cells.addAll(newCells);
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDayCell cell = cells.get(position);
        resizeCellToGrid(holder);
        boolean selected = cell.getDate().equals(selectedDate);
        int fill = selected ? holder.itemView.getContext().getColor(R.color.brand_orange_light) : holder.itemView.getContext().getColor(R.color.surface);
        int stroke = selected ? holder.itemView.getContext().getColor(R.color.brand_orange) : holder.itemView.getContext().getColor(R.color.line);
        holder.itemView.setBackground(UiUtils.roundedStroke(fill, stroke, 0, holder.itemView.getContext()));

        holder.dayNumber.setText(String.valueOf(cell.getDate().getDayOfMonth()));
        holder.dayNumber.setTextColor(cell.isCurrentMonth()
                ? holder.itemView.getContext().getColor(R.color.text_primary)
                : holder.itemView.getContext().getColor(R.color.text_muted));
        holder.dayNumber.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

        holder.eventContainer.removeAllViews();
        int max = Math.min(3, cell.getEvents().size());
        for (int i = 0; i < max; i++) {
            CalendarEvent event = cell.getEvents().get(i);
            TextView chip = new TextView(holder.itemView.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    UiUtils.dp(holder.itemView.getContext(), 20)
            );
            params.setMargins(0, 1, 0, 1);
            chip.setLayoutParams(params);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setMaxLines(1);
            chip.setText(event.getTitle());
            chip.setTextColor(Color.BLACK);
            chip.setTextSize(10);
            int fallback = holder.itemView.getContext().getColor(R.color.event_blue);
            chip.setBackground(UiUtils.rounded(UiUtils.safeColor(event.getColor(), fallback), 4, holder.itemView.getContext()));
            holder.eventContainer.addView(chip);
        }
        holder.more.setVisibility(cell.getEvents().size() > 3 ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onDayClick(cell.getDate()));
    }

    private void resizeCellToGrid(DayViewHolder holder) {
        if (!(holder.itemView.getParent() instanceof RecyclerView)) {
            return;
        }
        RecyclerView parent = (RecyclerView) holder.itemView.getParent();
        if (parent.getHeight() <= 0) {
            return;
        }
        int targetHeight = parent.getHeight() / 6;
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params != null && params.height != targetHeight) {
            params.height = targetHeight;
            holder.itemView.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayNumber;
        final LinearLayout eventContainer;
        final TextView more;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.tv_day_number);
            eventContainer = itemView.findViewById(R.id.event_chip_container);
            more = itemView.findViewById(R.id.tv_more);
        }
    }
}
