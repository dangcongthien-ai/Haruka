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
        int cellHeight = resizeCellToGrid(holder);
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
        boolean hasMore = cell.getEvents().size() > 3;
        Sizing sizing = applyResponsiveSizing(holder, cellHeight, max, hasMore);
        for (int i = 0; i < max; i++) {
            CalendarEvent event = cell.getEvents().get(i);
            TextView chip = new TextView(holder.itemView.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    sizing.chipHeight
            );
            params.setMargins(0, sizing.chipMargin, 0, sizing.chipMargin);
            chip.setLayoutParams(params);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setIncludeFontPadding(false);
            chip.setMaxLines(1);
            chip.setText(event.getTitle());
            chip.setTextColor(Color.BLACK);
            chip.setTextSize(sizing.chipTextSize);
            int fallback = holder.itemView.getContext().getColor(R.color.event_blue);
            chip.setBackground(UiUtils.rounded(UiUtils.safeColor(event.getColor(), fallback), 4, holder.itemView.getContext()));
            holder.eventContainer.addView(chip);
        }
        holder.more.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onDayClick(cell.getDate()));
    }

    private int resizeCellToGrid(DayViewHolder holder) {
        int fallbackHeight = holder.itemView.getHeight() > 0
                ? holder.itemView.getHeight()
                : UiUtils.dp(holder.itemView.getContext(), 94);
        if (!(holder.itemView.getParent() instanceof RecyclerView)) {
            return fallbackHeight;
        }
        RecyclerView parent = (RecyclerView) holder.itemView.getParent();
        if (parent.getHeight() <= 0) {
            return fallbackHeight;
        }
        int targetHeight = parent.getHeight() / 6;
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params != null && params.height != targetHeight) {
            params.height = targetHeight;
            holder.itemView.setLayoutParams(params);
        }
        return targetHeight;
    }

    private Sizing applyResponsiveSizing(DayViewHolder holder, int cellHeight, int chipCount, boolean hasMore) {
        int dayHeight = clamp(cellHeight / 5, UiUtils.dp(holder.itemView.getContext(), 14), UiUtils.dp(holder.itemView.getContext(), 20));
        int moreHeight = hasMore ? clamp(cellHeight / 8, UiUtils.dp(holder.itemView.getContext(), 8), UiUtils.dp(holder.itemView.getContext(), 14)) : 0;
        int margin = cellHeight < UiUtils.dp(holder.itemView.getContext(), 76) ? 0 : 1;
        int usedByMargins = chipCount * margin * 2;
        int availableForChips = cellHeight
                - dayHeight
                - moreHeight
                - holder.itemView.getPaddingTop()
                - holder.itemView.getPaddingBottom()
                - usedByMargins;
        int chipHeight = chipCount == 0 ? 0 : availableForChips / chipCount;
        chipHeight = clamp(chipHeight, UiUtils.dp(holder.itemView.getContext(), 8), UiUtils.dp(holder.itemView.getContext(), 20));

        ViewGroup.LayoutParams dayParams = holder.dayNumber.getLayoutParams();
        dayParams.height = dayHeight;
        holder.dayNumber.setLayoutParams(dayParams);
        holder.dayNumber.setIncludeFontPadding(false);
        holder.dayNumber.setTextSize(dayHeight <= UiUtils.dp(holder.itemView.getContext(), 15) ? 11 : 14);

        ViewGroup.LayoutParams moreParams = holder.more.getLayoutParams();
        moreParams.height = Math.max(moreHeight, UiUtils.dp(holder.itemView.getContext(), 8));
        holder.more.setLayoutParams(moreParams);
        holder.more.setIncludeFontPadding(false);
        holder.more.setTextSize(moreHeight <= UiUtils.dp(holder.itemView.getContext(), 10) ? 8 : 11);

        float chipTextSize;
        if (chipHeight <= UiUtils.dp(holder.itemView.getContext(), 10)) {
            chipTextSize = 6.5f;
        } else if (chipHeight <= UiUtils.dp(holder.itemView.getContext(), 13)) {
            chipTextSize = 7.5f;
        } else if (chipHeight <= UiUtils.dp(holder.itemView.getContext(), 16)) {
            chipTextSize = 8.5f;
        } else {
            chipTextSize = 10f;
        }
        return new Sizing(chipHeight, margin, chipTextSize);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Sizing {
        final int chipHeight;
        final int chipMargin;
        final float chipTextSize;

        Sizing(int chipHeight, int chipMargin, float chipTextSize) {
            this.chipHeight = chipHeight;
            this.chipMargin = chipMargin;
            this.chipTextSize = chipTextSize;
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
