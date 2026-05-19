package com.example.finalproject.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

    private static final int MONTH_CELL_HEIGHT_DP = 82;

    private final List<CalendarDayCell> cells = new ArrayList<>();
    private final OnDayClickListener listener;
    private LocalDate selectedDate;
    private List<CalendarDayCell> submittedSource;
    private int bottomSpacerHeight;

    public CalendarMonthAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<CalendarDayCell> newCells, LocalDate selectedDate) {
        LocalDate previousDate = this.selectedDate;
        if (newCells == submittedSource) {
            this.selectedDate = selectedDate;
            if (!isSameDate(previousDate, selectedDate)) {
                notifyDateChanged(previousDate);
                notifyDateChanged(selectedDate);
            }
            return;
        }
        submittedSource = newCells;
        cells.clear();
        cells.addAll(newCells);
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    void updateSelectedDate(LocalDate selectedDate) {
        LocalDate previousDate = this.selectedDate;
        if (isSameDate(previousDate, selectedDate)) {
            return;
        }
        this.selectedDate = selectedDate;
        notifyDateChanged(previousDate);
        notifyDateChanged(selectedDate);
    }

    void setSelectedDateSilently(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    boolean setViewportHeight(int viewportHeight) {
        return false;
    }

    void setBottomSpacerHeight(int bottomSpacerHeight) {
        int normalizedHeight = Math.max(0, bottomSpacerHeight);
        if (this.bottomSpacerHeight == normalizedHeight) {
            return;
        }
        boolean hadSpacer = this.bottomSpacerHeight > 0;
        this.bottomSpacerHeight = normalizedHeight;
        boolean hasSpacer = this.bottomSpacerHeight > 0;
        int spacerPosition = cells.size();
        if (spacerPosition == 0) {
            notifyDataSetChanged();
        } else if (!hadSpacer && hasSpacer) {
            notifyItemInserted(spacerPosition);
        } else if (hadSpacer && !hasSpacer) {
            notifyItemRemoved(spacerPosition);
        } else {
            notifyItemChanged(spacerPosition);
        }
    }

    void setBottomSpacerHeightSilently(int bottomSpacerHeight) {
        this.bottomSpacerHeight = Math.max(0, bottomSpacerHeight);
    }

    int getCellCount() {
        return cells.size();
    }

    int getPositionForDate(LocalDate date) {
        return findCellIndex(date);
    }

    private void notifyDateChanged(LocalDate date) {
        int index = findCellIndex(date);
        if (index != RecyclerView.NO_POSITION) {
            notifyItemChanged(index);
        }
    }

    private int findCellIndex(LocalDate date) {
        if (date == null) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < cells.size(); i++) {
            if (date.equals(cells.get(i).getDate())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private boolean isSameDate(LocalDate first, LocalDate second) {
        return first == null ? second == null : first.equals(second);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        if (position >= cells.size()) {
            bindBottomSpacer(holder);
            return;
        }
        CalendarDayCell cell = cells.get(position);
        int cellHeight = resizeCellToGrid(holder);
        boolean selected = cell.getDate().equals(selectedDate);
        holder.itemView.setSelected(selected);
        int fill = selected ? holder.itemView.getContext().getColor(R.color.brand_orange_light) : holder.itemView.getContext().getColor(R.color.surface);
        int stroke = selected ? holder.itemView.getContext().getColor(R.color.brand_orange) : holder.itemView.getContext().getColor(R.color.line);
        holder.itemView.setBackground(createCellBackground(holder, fill, stroke, selected));

        holder.dayNumber.setText(String.valueOf(cell.getDate().getDayOfMonth()));
        holder.dayNumber.setTextColor(cell.isCurrentMonth()
                ? holder.itemView.getContext().getColor(R.color.text_primary)
                : holder.itemView.getContext().getColor(R.color.text_muted));
        holder.dayNumber.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

        int max = Math.min(3, cell.getEvents().size());
        boolean hasMore = cell.getEvents().size() > 3;
        Sizing sizing = applyResponsiveSizing(holder, cellHeight, max, hasMore);
        bindEventChips(holder, cell, max, sizing);
        bindMoreIndicator(holder, hasMore, sizing);
        holder.itemView.setOnClickListener(v -> listener.onDayClick(cell.getDate()));
    }

    private void bindBottomSpacer(DayViewHolder holder) {
        holder.itemView.setSelected(false);
        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        holder.itemView.setOnClickListener(null);
        holder.dayNumber.setText("");
        hideEventContent(holder);
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params != null && params.height != bottomSpacerHeight) {
            params.height = bottomSpacerHeight;
            holder.itemView.setLayoutParams(params);
        }
    }

    private GradientDrawable createCellBackground(DayViewHolder holder, int fill, int stroke, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(selected ? fill : Color.TRANSPARENT);
        if (selected) {
            drawable.setStroke(UiUtils.dp(holder.itemView.getContext(), 1), stroke);
        }
        return drawable;
    }

    private void bindEventChips(DayViewHolder holder, CalendarDayCell cell, int max, Sizing sizing) {
        int fallback = holder.itemView.getContext().getColor(R.color.event_blue);
        for (int i = 0; i < holder.eventChips.length; i++) {
            TextView chip = holder.eventChips[i];
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) chip.getLayoutParams();
            params.height = sizing.chipHeight;
            params.setMargins(0, sizing.chipMargin, 0, sizing.chipMargin);
            chip.setLayoutParams(params);
            if (i < max) {
                CalendarEvent event = cell.getEvents().get(i);
                chip.setText(event.getTitle());
                chip.setTextColor(Color.BLACK);
                chip.setTextSize(sizing.chipTextSize);
                chip.setBackground(UiUtils.rounded(UiUtils.safeColor(event.getColor(), fallback), 4, holder.itemView.getContext()));
                chip.setVisibility(View.VISIBLE);
            } else {
                chip.setText("");
                chip.setVisibility(View.GONE);
            }
        }
    }

    private void bindMoreIndicator(DayViewHolder holder, boolean hasMore, Sizing sizing) {
        if (!hasMore) {
            holder.moreTopSpacer.setVisibility(View.GONE);
            holder.more.setVisibility(View.GONE);
            holder.moreBottomSpacer.setVisibility(View.GONE);
            return;
        }
        holder.more.setTextSize(sizing.moreTextSize);
        holder.more.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams moreParams = (LinearLayout.LayoutParams) holder.more.getLayoutParams();
        moreParams.height = sizing.moreHeight;
        holder.more.setLayoutParams(moreParams);
        holder.moreTopSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                0.65f
        ));
        holder.moreBottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.35f
        ));
        holder.moreTopSpacer.setVisibility(View.VISIBLE);
        holder.more.setVisibility(View.VISIBLE);
        holder.moreBottomSpacer.setVisibility(View.VISIBLE);
    }

    private void hideEventContent(DayViewHolder holder) {
        for (TextView chip : holder.eventChips) {
            chip.setText("");
            chip.setVisibility(View.GONE);
        }
        holder.moreTopSpacer.setVisibility(View.GONE);
        holder.more.setVisibility(View.GONE);
        holder.moreBottomSpacer.setVisibility(View.GONE);
    }

    private int resizeCellToGrid(DayViewHolder holder) {
        int targetHeight = UiUtils.dp(holder.itemView.getContext(), MONTH_CELL_HEIGHT_DP);
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params != null && params.height != targetHeight) {
            params.height = targetHeight;
            holder.itemView.setLayoutParams(params);
        }
        return targetHeight;
    }

    private Sizing applyResponsiveSizing(DayViewHolder holder, int cellHeight, int chipCount, boolean hasMore) {
        int dayHeight = clamp(cellHeight / 5, UiUtils.dp(holder.itemView.getContext(), 14), UiUtils.dp(holder.itemView.getContext(), 19));
        int margin = cellHeight < UiUtils.dp(holder.itemView.getContext(), 58)
                ? 1
                : UiUtils.dp(holder.itemView.getContext(), 1);
        int moreHeight = hasMore
                ? clamp(cellHeight / 7, UiUtils.dp(holder.itemView.getContext(), 12), UiUtils.dp(holder.itemView.getContext(), 16))
                : 0;
        int usedByMargins = chipCount * margin * 2;
        int availableForChips = cellHeight
                - dayHeight
                - moreHeight
                - holder.itemView.getPaddingTop()
                - holder.itemView.getPaddingBottom()
                - usedByMargins;
        int chipHeight = chipCount == 0 ? 0 : availableForChips / chipCount;
        chipHeight = clamp(chipHeight, UiUtils.dp(holder.itemView.getContext(), 8), UiUtils.dp(holder.itemView.getContext(), 18));

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
        float moreTextSize = moreHeight <= UiUtils.dp(holder.itemView.getContext(), 13) ? 10 : 13;
        return new Sizing(chipHeight, margin, chipTextSize, moreHeight, moreTextSize);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Sizing {
        final int chipHeight;
        final int chipMargin;
        final float chipTextSize;
        final int moreHeight;
        final float moreTextSize;

        Sizing(int chipHeight, int chipMargin, float chipTextSize, int moreHeight, float moreTextSize) {
            this.chipHeight = chipHeight;
            this.chipMargin = chipMargin;
            this.chipTextSize = chipTextSize;
            this.moreHeight = moreHeight;
            this.moreTextSize = moreTextSize;
        }
    }

    @Override
    public int getItemCount() {
        return cells.size() + (bottomSpacerHeight > 0 ? 1 : 0);
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayNumber;
        final TextView[] eventChips;
        final View moreTopSpacer;
        final TextView more;
        final View moreBottomSpacer;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.tv_day_number);
            eventChips = new TextView[]{
                    itemView.findViewById(R.id.tv_event_chip_1),
                    itemView.findViewById(R.id.tv_event_chip_2),
                    itemView.findViewById(R.id.tv_event_chip_3)
            };
            moreTopSpacer = itemView.findViewById(R.id.more_top_spacer);
            more = itemView.findViewById(R.id.tv_more);
            moreBottomSpacer = itemView.findViewById(R.id.more_bottom_spacer);
        }
    }
}
