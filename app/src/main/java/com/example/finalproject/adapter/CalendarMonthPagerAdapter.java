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
    private boolean monthDetailVisible;

    public CalendarMonthPagerAdapter(LocalDate baseMonth, MonthCellProvider cellProvider, OnDayClickListener dayClickListener) {
        this.baseMonth = baseMonth.withDayOfMonth(1);
        this.cellProvider = cellProvider;
        this.dayClickListener = dayClickListener;
        setHasStableIds(true);
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public void setMonthDetailVisible(boolean monthDetailVisible) {
        this.monthDetailVisible = monthDetailVisible;
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
        recyclerView.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        recyclerView.setClipToPadding(false);
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new MonthGridLayoutManager(parent.getContext(), 7));
        return new MonthViewHolder(recyclerView, dayClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        LocalDate monthStart = getMonthForPosition(position);
        holder.applyMonthDetailMode(monthDetailVisible);
        holder.adapter.submit(cellProvider.getCells(monthStart), selectedDate);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        final CalendarMonthAdapter adapter;
        final RecyclerView recyclerView;
        final MonthGridLayoutManager layoutManager;
        private int lastViewportHeight;

        MonthViewHolder(@NonNull RecyclerView recyclerView, OnDayClickListener listener) {
            super(recyclerView);
            this.recyclerView = recyclerView;
            this.layoutManager = (MonthGridLayoutManager) recyclerView.getLayoutManager();
            adapter = new CalendarMonthAdapter(listener::onDayClick);
            recyclerView.setAdapter(adapter);
            recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int height = bottom - top;
                if (height > 0 && height != lastViewportHeight) {
                    lastViewportHeight = height;
                    adapter.setViewportHeight(height);
                    recyclerView.post(adapter::notifyDataSetChanged);
                }
            });
        }

        void applyMonthDetailMode(boolean visible) {
            layoutManager.setVerticalScrollEnabled(visible);
            if (recyclerView.getHeight() > 0) {
                adapter.setViewportHeight(recyclerView.getHeight());
            }
            recyclerView.post(() -> {
                int bottomPadding = visible ? recyclerView.getHeight() / 2 : 0;
                if (recyclerView.getPaddingBottom() != bottomPadding) {
                    recyclerView.setPadding(0, 0, 0, bottomPadding);
                }
                recyclerView.scrollToPosition(0);
            });
        }
    }

    private static class MonthGridLayoutManager extends GridLayoutManager {
        private boolean verticalScrollEnabled;

        MonthGridLayoutManager(android.content.Context context, int spanCount) {
            super(context, spanCount);
        }

        void setVerticalScrollEnabled(boolean verticalScrollEnabled) {
            this.verticalScrollEnabled = verticalScrollEnabled;
        }

        @Override
        public boolean canScrollVertically() {
            return verticalScrollEnabled;
        }
    }
}
