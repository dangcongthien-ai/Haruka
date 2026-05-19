package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.model.CalendarDayCell;
import com.example.finalproject.ui.calendar.MonthGridView;

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
    private static final int REVEAL_PANEL_GAP_DP = 36;
    private static final int DETAIL_BOTTOM_SCROLL_BUFFER_DP = 96;
    private static final int MONTH_CELL_HEIGHT_DP = 82;
    private static final int MONTH_GRID_BOTTOM_PADDING_DP = 18;
    private static final int REVEAL_FINAL_CORRECTION_DELAY_MS = 220;

    private final LocalDate baseMonth;
    private final MonthCellProvider cellProvider;
    private final OnDayClickListener dayClickListener;
    private LocalDate selectedDate;
    private boolean monthDetailVisible;
    private int monthDetailPanelHeight;
    private int monthDetailPanelTop = RecyclerView.NO_POSITION;
    private int pendingScrollResetPosition = RecyclerView.NO_POSITION;

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

    public void setMonthDetailPanelHeight(int monthDetailPanelHeight) {
        this.monthDetailPanelHeight = Math.max(0, monthDetailPanelHeight);
    }

    public void setMonthDetailPanelTop(int monthDetailPanelTop) {
        this.monthDetailPanelTop = monthDetailPanelTop;
    }

    public void resetScrollForPosition(RecyclerView pager, int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        RecyclerView.ViewHolder holder = pager.findViewHolderForAdapterPosition(position);
        if (holder instanceof MonthViewHolder) {
            ((MonthViewHolder) holder).resetScrollToTop();
            pendingScrollResetPosition = RecyclerView.NO_POSITION;
            return;
        }
        pendingScrollResetPosition = position;
        notifyItemChanged(position);
    }

    public void closeMonthDetailInstantly(RecyclerView pager, int position, LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        this.monthDetailVisible = false;
        this.monthDetailPanelTop = RecyclerView.NO_POSITION;
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        RecyclerView.ViewHolder holder = pager.findViewHolderForAdapterPosition(position);
        if (holder instanceof MonthViewHolder) {
            ((MonthViewHolder) holder).closeMonthDetailInstantly(selectedDate);
            pendingScrollResetPosition = RecyclerView.NO_POSITION;
            return;
        }
        pendingScrollResetPosition = position;
        notifyItemChanged(position);
    }

    public void updateVisibleMonthSelection(RecyclerView pager, int position, LocalDate selectedDate, boolean monthDetailVisible) {
        this.selectedDate = selectedDate;
        this.monthDetailVisible = monthDetailVisible;
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        RecyclerView.ViewHolder holder = pager.findViewHolderForAdapterPosition(position);
        if (holder instanceof MonthViewHolder) {
            MonthViewHolder monthHolder = (MonthViewHolder) holder;
            monthHolder.setMonthDetailPanelHeight(monthDetailPanelHeight);
            monthHolder.setMonthDetailPanelTop(monthDetailPanelTop);
            monthHolder.applyMonthDetailMode(monthDetailVisible);
            monthHolder.adapter.updateSelectedDate(selectedDate);
            monthHolder.syncGridSoon();
            return;
        }
        notifyItemChanged(position);
    }

    public void revealDateAbovePanel(RecyclerView pager, int position, LocalDate date) {
        if (position == RecyclerView.NO_POSITION || date == null) {
            return;
        }
        RecyclerView.ViewHolder holder = pager.findViewHolderForAdapterPosition(position);
        if (holder instanceof MonthViewHolder) {
            MonthViewHolder monthHolder = (MonthViewHolder) holder;
            monthHolder.setMonthDetailPanelHeight(monthDetailPanelHeight);
            monthHolder.setMonthDetailPanelTop(monthDetailPanelTop);
            monthHolder.revealDateAbovePanel(date);
        }
    }

    public int getDateCellBottomInPage(RecyclerView pager, int position, LocalDate date) {
        if (position == RecyclerView.NO_POSITION || date == null) {
            return RecyclerView.NO_POSITION;
        }
        RecyclerView.ViewHolder holder = pager.findViewHolderForAdapterPosition(position);
        if (holder instanceof MonthViewHolder) {
            return ((MonthViewHolder) holder).getDateCellBottomInPage(date);
        }
        return RecyclerView.NO_POSITION;
    }

    public void smoothResetScrollForPosition(RecyclerView pager, int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        RecyclerView.ViewHolder holder = pager.findViewHolderForAdapterPosition(position);
        if (holder instanceof MonthViewHolder) {
            ((MonthViewHolder) holder).smoothResetScrollToTop();
        }
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_month_page, parent, false);
        view.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        RecyclerView recyclerView = view.findViewById(R.id.month_grid_recycler);
        recyclerView.setClipToPadding(false);
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(43);
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutManager(new MonthGridLayoutManager(parent.getContext(), 7));
        return new MonthViewHolder(view, recyclerView, dayClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        LocalDate monthStart = getMonthForPosition(position);
        holder.setMonthDetailPanelHeight(monthDetailPanelHeight);
        holder.setMonthDetailPanelTop(monthDetailPanelTop);
        holder.adapter.submit(cellProvider.getCells(monthStart), selectedDate);
        holder.applyMonthDetailMode(monthDetailVisible);
        if (pendingScrollResetPosition == position) {
            holder.resetScrollToTop();
            pendingScrollResetPosition = RecyclerView.NO_POSITION;
        }
        holder.syncGridSoon();
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
        final MonthGridView gridView;
        final MonthGridLayoutManager layoutManager;
        private int lastViewportHeight;
        private int monthDetailPanelHeight;
        private int monthDetailPanelTop = RecyclerView.NO_POSITION;
        private boolean detailVisible;
        private int layoutGeneration;

        MonthViewHolder(@NonNull View itemView, @NonNull RecyclerView recyclerView, OnDayClickListener listener) {
            super(itemView);
            this.recyclerView = recyclerView;
            this.gridView = itemView.findViewById(R.id.month_grid_view);
            this.layoutManager = (MonthGridLayoutManager) recyclerView.getLayoutManager();
            adapter = new CalendarMonthAdapter(listener::onDayClick);
            recyclerView.setAdapter(adapter);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return position >= adapter.getCellCount() ? 7 : 1;
                }
            });
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    syncGridMetrics();
                }
            });
            recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int height = bottom - top;
                if (height > 0 && height != lastViewportHeight) {
                    lastViewportHeight = height;
                    recyclerView.post(this::syncGridMetrics);
                }
            });
        }

        void setMonthDetailPanelHeight(int monthDetailPanelHeight) {
            this.monthDetailPanelHeight = Math.max(0, monthDetailPanelHeight);
        }

        void setMonthDetailPanelTop(int monthDetailPanelTop) {
            this.monthDetailPanelTop = monthDetailPanelTop;
        }

        void applyMonthDetailMode(boolean visible) {
            int generation = ++layoutGeneration;
            detailVisible = visible;
            layoutManager.setVerticalScrollEnabled(true);
            recyclerView.post(() -> {
                if (generation != layoutGeneration || detailVisible != visible) {
                    return;
                }
                int bottomPadding = visible ? 0 : dp(MONTH_GRID_BOTTOM_PADDING_DP);
                adapter.setBottomSpacerHeight(visible ? getDetailModeBottomSpacerHeight() : 0);
                if (recyclerView.getPaddingBottom() != bottomPadding) {
                    recyclerView.setPadding(0, 0, 0, bottomPadding);
                }
                gridView.setBottomInset(visible ? 0 : bottomPadding);
                recyclerView.post(() -> {
                    if (generation == layoutGeneration && detailVisible == visible) {
                        syncGridMetrics();
                    }
                });
            });
        }

        void resetScrollToTop() {
            recyclerView.stopScroll();
            layoutManager.scrollToPositionWithOffset(0, 0);
            gridView.setScrollOffset(0);
            recyclerView.post(() -> {
                layoutManager.scrollToPositionWithOffset(0, 0);
                gridView.setScrollOffset(0);
                syncGridMetrics();
            });
        }

        void closeMonthDetailInstantly(LocalDate selectedDate) {
            layoutGeneration++;
            detailVisible = false;
            recyclerView.stopScroll();
            layoutManager.setVerticalScrollEnabled(true);
            adapter.setSelectedDateSilently(selectedDate);
            adapter.setBottomSpacerHeight(0);

            int bottomPadding = dp(MONTH_GRID_BOTTOM_PADDING_DP);
            if (recyclerView.getPaddingBottom() != bottomPadding) {
                recyclerView.setPadding(0, 0, 0, bottomPadding);
            }
            gridView.setBottomInset(bottomPadding);
            syncGridMetrics();
            recyclerView.post(this::syncGridMetrics);
        }

        void smoothResetScrollToTop() {
            recyclerView.stopScroll();
            int offset = recyclerView.computeVerticalScrollOffset();
            if (offset > 0) {
                recyclerView.smoothScrollBy(0, -offset);
            }
        }

        void revealDateAbovePanel(LocalDate date) {
            if (!detailVisible || date == null) {
                return;
            }
            recyclerView.post(() -> {
                revealDateAbovePanelNow(date, true);
                recyclerView.postDelayed(() -> revealDateAbovePanelNow(date, false), 80);
                recyclerView.postDelayed(() -> revealDateAbovePanelNow(date, false), REVEAL_FINAL_CORRECTION_DELAY_MS);
            });
        }

        private void revealDateAbovePanelNow(LocalDate date, boolean smooth) {
            if (!detailVisible || date == null) {
                return;
            }
            int position = adapter.getPositionForDate(date);
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            View selectedCell = layoutManager.findViewByPosition(position);
            if (selectedCell == null) {
                return;
            }
            int panelTopInRecycler = getPanelTop() - recyclerView.getTop();
            int desiredBottom = panelTopInRecycler - dp(REVEAL_PANEL_GAP_DP);
            int overflow = selectedCell.getBottom() - desiredBottom;
            if (overflow > 0) {
                if (smooth) {
                    recyclerView.smoothScrollBy(0, overflow);
                } else {
                    recyclerView.stopScroll();
                    recyclerView.scrollBy(0, overflow);
                    syncGridMetrics();
                }
            }
        }

        int getDateCellBottomInPage(LocalDate date) {
            int position = adapter.getPositionForDate(date);
            if (position == RecyclerView.NO_POSITION) {
                return RecyclerView.NO_POSITION;
            }
            View selectedCell = layoutManager.findViewByPosition(position);
            if (selectedCell == null) {
                return RecyclerView.NO_POSITION;
            }
            return recyclerView.getTop() + selectedCell.getBottom();
        }

        private int dp(int value) {
            return Math.round(value * recyclerView.getResources().getDisplayMetrics().density);
        }

        private int getDetailModeBottomSpacerHeight() {
            int visibleGridHeight = getVisibleGridHeightAbovePanel(recyclerView.getHeight());
            int hiddenGridHeight = Math.max(0, recyclerView.getHeight() - visibleGridHeight);
            return hiddenGridHeight + dp(DETAIL_BOTTOM_SCROLL_BUFFER_DP);
        }

        private int getTargetGridContentHeight() {
            return dp(MONTH_CELL_HEIGHT_DP) * 6;
        }

        private int getVisibleGridHeightAbovePanel(int viewportHeight) {
            int recyclerTopInPage = recyclerView.getTop();
            int visibleGridHeight = Math.max(0, getPanelTop() - recyclerTopInPage);
            return Math.min(viewportHeight, visibleGridHeight);
        }

        private int getPanelTop() {
            int itemHeight = itemView.getHeight();
            if (!detailVisible || itemHeight <= 0) {
                return itemHeight;
            }
            if (monthDetailPanelTop != RecyclerView.NO_POSITION && monthDetailPanelTop > 0) {
                return Math.min(itemHeight, monthDetailPanelTop);
            }
            int panelHeight = monthDetailPanelHeight > 0 ? monthDetailPanelHeight : Math.round(itemHeight * 0.44f);
            return Math.max(0, itemHeight - panelHeight);
        }

        private void syncGridSoon() {
            recyclerView.post(this::syncGridMetrics);
        }

        private void syncGridMetrics() {
            gridView.setScrollOffset(recyclerView.computeVerticalScrollOffset());
            float[] edges = new float[8];
            boolean[] hasEdge = new boolean[8];
            float gridTop = 0f;
            float rowHeight = 0f;
            boolean hasVerticalMetric = false;
            int childCount = recyclerView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = recyclerView.getChildAt(i);
                int position = recyclerView.getChildAdapterPosition(child);
                if (position == RecyclerView.NO_POSITION || position >= adapter.getCellCount() || child.getHeight() <= 0) {
                    continue;
                }
                int column = position % 7;
                int row = position / 7;
                edges[column] = child.getLeft();
                edges[column + 1] = child.getRight();
                hasEdge[column] = true;
                hasEdge[column + 1] = true;
                if (!hasVerticalMetric) {
                    rowHeight = child.getHeight();
                    gridTop = child.getTop() - row * rowHeight;
                    hasVerticalMetric = true;
                }
            }
            if (!hasVerticalMetric) {
                return;
            }
            for (boolean edgePresent : hasEdge) {
                if (!edgePresent) {
                    return;
                }
            }
            gridView.setGridMetrics(edges, gridTop, rowHeight);
        }
    }

    private static class MonthGridLayoutManager extends GridLayoutManager {
        private boolean verticalScrollEnabled;

        MonthGridLayoutManager(android.content.Context context, int spanCount) {
            super(context, spanCount);
            verticalScrollEnabled = true;
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
