package com.example.finalproject.ui.calendar;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalproject.R;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WeekTopStripLayout extends ViewGroup {
    public interface Listener {
        void onEventClick(CalendarEvent event);
    }

    private static final int DAY_COUNT = 7;
    private static final int BASE_STRIP_HEIGHT_DP = 45;
    private static final int STRIP_ROW_HEIGHT_DP = 22;
    private static final int STRIP_ROW_GAP_DP = 4;
    private static final int STRIP_ITEM_HORIZONTAL_INSET_DP = 2;
    private static final int STRIP_TEXT_HORIZONTAL_PADDING_DP = 6;
    private static final int STRIP_TEXT_VERTICAL_PADDING_DP = 3;

    private final Paint linePaint = new Paint();
    private final List<LocalDate> weekDates = new ArrayList<>();
    private final List<WeekTimelineLayout.WeekStripItem> items = new ArrayList<>();
    private final List<StripPlacement> placements = new ArrayList<>();
    private Listener listener;
    private int rowCount;

    public WeekTopStripLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public WeekTopStripLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeekTopStripLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void submit(@Nullable List<LocalDate> dates, @Nullable List<WeekTimelineLayout.WeekStripItem> stripItems) {
        weekDates.clear();
        items.clear();
        placements.clear();
        rowCount = 0;
        if (dates != null) {
            weekDates.addAll(dates);
        }
        if (stripItems != null) {
            items.addAll(stripItems);
        }
        buildPlacements();
        rebuildViews();
        requestLayout();
        invalidate();
    }

    public boolean hasItems() {
        return !placements.isEmpty();
    }

    private void init() {
        setWillNotDraw(false);
        linePaint.setColor(getContext().getColor(R.color.line));
        linePaint.setStrokeWidth(1f);
        linePaint.setAntiAlias(false);
    }

    private void buildPlacements() {
        if (items.isEmpty()) {
            return;
        }
        List<WeekTimelineLayout.WeekStripItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator
                .comparingInt(WeekTimelineLayout.WeekStripItem::getType)
                .thenComparingInt(WeekTimelineLayout.WeekStripItem::getStartDayIndex)
                .thenComparingInt(item -> -(item.getEndDayExclusiveIndex() - item.getStartDayIndex()))
                .thenComparing(item -> item.getLabel() == null ? "" : item.getLabel()));
        List<Integer> rowEnds = new ArrayList<>();
        for (WeekTimelineLayout.WeekStripItem item : sorted) {
            int targetRow = -1;
            for (int row = 0; row < rowEnds.size(); row++) {
                if (rowEnds.get(row) <= item.getStartDayIndex()) {
                    targetRow = row;
                    break;
                }
            }
            if (targetRow < 0) {
                targetRow = rowEnds.size();
                rowEnds.add(item.getEndDayExclusiveIndex());
            } else {
                rowEnds.set(targetRow, item.getEndDayExclusiveIndex());
            }
            placements.add(new StripPlacement(item, targetRow));
        }
        rowCount = rowEnds.size();
    }

    private void rebuildViews() {
        removeAllViews();
        for (StripPlacement placement : placements) {
            addView(createStripView(placement));
        }
    }

    private View createStripView(StripPlacement placement) {
        TextView view = new TextView(getContext());
        view.setText(placement.item.getLabel());
        view.setTextColor(getContext().getColor(R.color.text_primary));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        int horizontalPadding = UiUtils.dp(getContext(), STRIP_TEXT_HORIZONTAL_PADDING_DP);
        int verticalPadding = UiUtils.dp(getContext(), STRIP_TEXT_VERTICAL_PADDING_DP);
        view.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        int fallback = getContext().getColor(R.color.event_blue);
        int fill = UiUtils.safeColor(placement.item.getEvent().getColor(), fallback);
        view.setBackground(UiUtils.rounded(fill, 9, getContext()));
        view.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(placement.item.getEvent());
            }
        });
        return view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int baseHeight = UiUtils.dp(getContext(), BASE_STRIP_HEIGHT_DP);
        int rowHeight = UiUtils.dp(getContext(), STRIP_ROW_HEIGHT_DP);
        int rowGap = UiUtils.dp(getContext(), STRIP_ROW_GAP_DP);
        int contentHeight = rowCount == 0 ? 0 : (rowCount * rowHeight) + ((rowCount - 1) * rowGap);
        int measuredHeight = Math.max(baseHeight, contentHeight) + 1;
        setMeasuredDimension(resolveSize(measuredWidth, widthMeasureSpec), resolveSize(measuredHeight, heightMeasureSpec));
        for (int i = 0; i < getChildCount() && i < placements.size(); i++) {
            Rect bounds = computeBounds(placements.get(i), getMeasuredWidth(), measuredHeight);
            placements.get(i).bounds.set(bounds);
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(bounds.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(bounds.height(), MeasureSpec.EXACTLY)
            );
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount() && i < placements.size(); i++) {
            Rect bounds = placements.get(i).bounds;
            getChildAt(i).layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams params) {
        return new LayoutParams(params);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams params) {
        return params != null;
    }

    @Override
    protected void onDraw(@NonNull android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        float dayWidth = Math.max(1f, getWidth() / (float) DAY_COUNT);
        for (int day = 0; day <= DAY_COUNT; day++) {
            float x = crisp(day * dayWidth);
            canvas.drawLine(x, 0, x, getHeight(), linePaint);
        }
        int baseHeight = UiUtils.dp(getContext(), BASE_STRIP_HEIGHT_DP);
        int rowHeight = UiUtils.dp(getContext(), STRIP_ROW_HEIGHT_DP);
        int rowGap = UiUtils.dp(getContext(), STRIP_ROW_GAP_DP);
        int contentHeight = rowCount == 0 ? 0 : (rowCount * rowHeight) + ((rowCount - 1) * rowGap);
        int topOffset = rowCount == 0 ? 0 : Math.max(0, (Math.max(baseHeight, contentHeight) - contentHeight) / 2);
        int visibleRows = Math.max(1, rowCount);
        for (int row = 0; row <= visibleRows; row++) {
            float y;
            if (rowCount == 0) {
                y = row == 0 ? 0f : getHeight() - 1f;
            } else if (row == visibleRows) {
                y = Math.min(getHeight() - 1f, topOffset + contentHeight);
            } else {
                y = topOffset + (row * (rowHeight + rowGap)) - (row == 0 ? 0 : rowGap / 2f);
            }
            canvas.drawLine(0, crisp(y), getWidth(), crisp(y), linePaint);
        }
        canvas.drawLine(0, crisp(getHeight() - 1f), getWidth(), crisp(getHeight() - 1f), linePaint);
    }

    private Rect computeBounds(StripPlacement placement, int width, int measuredHeight) {
        float dayWidth = Math.max(1f, width / (float) DAY_COUNT);
        int inset = UiUtils.dp(getContext(), STRIP_ITEM_HORIZONTAL_INSET_DP);
        int rowHeight = UiUtils.dp(getContext(), STRIP_ROW_HEIGHT_DP);
        int rowGap = UiUtils.dp(getContext(), STRIP_ROW_GAP_DP);
        int baseHeight = UiUtils.dp(getContext(), BASE_STRIP_HEIGHT_DP);
        int contentHeight = rowCount == 0 ? 0 : (rowCount * rowHeight) + ((rowCount - 1) * rowGap);
        int topOffset = rowCount == 0 ? 0 : Math.max(0, (Math.max(baseHeight, contentHeight) - contentHeight) / 2);
        int left = Math.round((placement.item.getStartDayIndex() * dayWidth)) + inset;
        int right = Math.round((placement.item.getEndDayExclusiveIndex() * dayWidth)) - inset;
        int top = topOffset + (placement.rowIndex * (rowHeight + rowGap));
        int bottom = top + rowHeight;
        if (right <= left) {
            right = left + UiUtils.dp(getContext(), 18);
        }
        return new Rect(left, top, right, bottom);
    }

    private float crisp(float value) {
        return (float) Math.floor(value) + 0.5f;
    }

    private static class StripPlacement {
        final WeekTimelineLayout.WeekStripItem item;
        final int rowIndex;
        final Rect bounds = new Rect();

        StripPlacement(WeekTimelineLayout.WeekStripItem item, int rowIndex) {
            this.item = item;
            this.rowIndex = rowIndex;
        }
    }
}
