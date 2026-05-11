package com.example.finalproject.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.model.CalendarDayCell;

import java.time.LocalDate;
import java.util.List;

public class CalendarMonthPagerAdapter extends RecyclerView.Adapter<CalendarMonthPagerAdapter.MonthViewHolder> {
    public interface MonthCellProvider {
        List<CalendarDayCell> getCells(LocalDate monthStart);
    }

    public interface OnDayClickListener {
        void onDayClick(LocalDate date);
    }

    private static final int PAGE_COUNT = 2401;
    private static final int CENTER_POSITION = PAGE_COUNT / 2;

    private final LocalDate baseMonth;
    private final MonthCellProvider cellProvider;
    private final OnDayClickListener dayClickListener;
    private LocalDate selectedDate;

    public CalendarMonthPagerAdapter(LocalDate baseMonth, MonthCellProvider cellProvider, OnDayClickListener dayClickListener) {
        this.baseMonth = baseMonth.withDayOfMonth(1);
        this.cellProvider = cellProvider;
        this.dayClickListener = dayClickListener;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public int getPositionForMonth(LocalDate monthStart) {
        int diff = (monthStart.getYear() - baseMonth.getYear()) * 12
                + monthStart.getMonthValue() - baseMonth.getMonthValue();
        int position = CENTER_POSITION + diff;
        return Math.max(0, Math.min(PAGE_COUNT - 1, position));
    }

    public LocalDate getMonthForPosition(int position) {
        return baseMonth.plusMonths(position - CENTER_POSITION);
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView recyclerView = new RecyclerView(parent.getContext());
        int width = parent.getWidth() > 0
                ? parent.getWidth()
                : parent.getResources().getDisplayMetrics().widthPixels;
        recyclerView.setLayoutParams(new RecyclerView.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new NoScrollGridLayoutManager(parent.getContext(), 7));
        return new MonthViewHolder(recyclerView, dayClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        LocalDate monthStart = getMonthForPosition(position);
        holder.adapter.submit(cellProvider.getCells(monthStart), selectedDate);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        final CalendarMonthAdapter adapter;

        MonthViewHolder(@NonNull RecyclerView recyclerView, OnDayClickListener listener) {
            super(recyclerView);
            adapter = new CalendarMonthAdapter(listener::onDayClick);
            recyclerView.setAdapter(adapter);
        }
    }

    private static class NoScrollGridLayoutManager extends GridLayoutManager {
        NoScrollGridLayoutManager(android.content.Context context, int spanCount) {
            super(context, spanCount);
        }

        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }
}
