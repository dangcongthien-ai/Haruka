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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WeekTimelineLayout extends ViewGroup {
    public interface Listener {
        void onEventClick(CalendarEvent event);
    }

    private static final int DAY_COUNT = 7;
    private static final int HOUR_COUNT = 24;
    private static final int TIME_COLUMN_WIDTH_DP = 45;
    private static final int HOUR_ROW_HEIGHT_DP = 45;
    private static final int MAX_VISIBLE_OVERLAP = 2;
    private static final int EVENT_HORIZONTAL_INSET_DP = 3;
    private static final int EVENT_TOP_INSET_DP = 1;
    private static final int EVENT_BOTTOM_INSET_DP = 3;
    private static final int EVENT_TEXT_HORIZONTAL_PADDING_DP = 5;
    private static final int EVENT_TEXT_VERTICAL_PADDING_DP = 4;
    private static final int EVENT_MIN_HEIGHT_DP = 34;

    private final Paint linePaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<LocalDate> weekDates = new ArrayList<>();
    private final List<EventPlacement> placements = new ArrayList<>();
    private final List<WeekStripItem> stripItems = new ArrayList<>();
    private Listener listener;

    public WeekTimelineLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public WeekTimelineLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeekTimelineLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void submit(@Nullable List<LocalDate> dates, @Nullable List<CalendarEvent> events) {
        weekDates.clear();
        placements.clear();
        stripItems.clear();
        if (dates != null) {
            weekDates.addAll(dates);
        }
        if (events != null && !weekDates.isEmpty()) {
            buildLayoutData(events);
        }
        rebuildEventViews();
        requestLayout();
        invalidate();
    }

    @NonNull
    public List<WeekStripItem> getStripItems() {
        return new ArrayList<>(stripItems);
    }

    public int getScrollYForHour(int hour) {
        int clampedHour = Math.max(0, Math.min(HOUR_COUNT - 1, hour));
        return clampedHour * dp(HOUR_ROW_HEIGHT_DP);
    }

    private void init() {
        setWillNotDraw(false);
        linePaint.setColor(getContext().getColor(R.color.line));
        linePaint.setStrokeWidth(1f);
        linePaint.setAntiAlias(false);
        textPaint.setColor(getContext().getColor(R.color.text_secondary));
        textPaint.setTextSize(sp(11));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    private void buildLayoutData(List<CalendarEvent> events) {
        addSpanningStripItems(events);
        for (int dayIndex = 0; dayIndex < weekDates.size(); dayIndex++) {
            LocalDate date = weekDates.get(dayIndex);
            List<EventPlacement> dayPlacements = new ArrayList<>();
            for (CalendarEvent event : events) {
                if (event.getStartDateTime() == null || !date.equals(event.getDate()) || shouldShowInTopStrip(event)) {
                    continue;
                }
                float startHour = resolveStartHour(event);
                float endHour = resolveEndHour(event, startHour);
                dayPlacements.add(new EventPlacement(event, dayIndex, startHour, endHour));
            }
            assignLanes(dayPlacements);
            for (EventPlacement placement : dayPlacements) {
                if (placement.hidden) {
                    stripItems.add(new WeekStripItem(
                            placement.event,
                            placement.dayIndex,
                            placement.dayIndex + 1,
                            buildOverflowLabel(placement.event),
                            WeekStripItem.TYPE_OVERFLOW
                    ));
                } else {
                    placements.add(placement);
                }
            }
        }
    }

    private void addSpanningStripItems(List<CalendarEvent> events) {
        if (weekDates.isEmpty()) {
            return;
        }
        LocalDate weekStart = weekDates.get(0);
        LocalDate weekEndExclusive = weekDates.get(weekDates.size() - 1).plusDays(1);
        for (CalendarEvent event : events) {
            if (event.getStartDateTime() == null || !shouldShowInTopStrip(event)) {
                continue;
            }
            int startDay = resolveWeekStartIndex(event, weekStart);
            int endDayExclusive = resolveWeekEndExclusiveIndex(event, weekStart, weekEndExclusive);
            if (endDayExclusive <= startDay) {
                continue;
            }
            stripItems.add(new WeekStripItem(
                    event,
                    startDay,
                    endDayExclusive,
                    event.getTitle(),
                    event.isAllDay() ? WeekStripItem.TYPE_ALL_DAY : WeekStripItem.TYPE_MULTI_DAY
            ));
        }
    }

    private boolean shouldShowInTopStrip(CalendarEvent event) {
        if (event == null || event.getStartDateTime() == null) {
            return false;
        }
        if (event.isAllDay()) {
            return true;
        }
        LocalDateTime end = event.getEndDateTime();
        return end != null && !end.minusNanos(1).toLocalDate().equals(event.getStartDateTime().toLocalDate());
    }

    private int resolveWeekStartIndex(CalendarEvent event, LocalDate weekStart) {
        LocalDate eventStart = event.getStartDateTime().toLocalDate();
        return Math.max(0, (int) java.time.temporal.ChronoUnit.DAYS.between(weekStart, eventStart));
    }

    private int resolveWeekEndExclusiveIndex(CalendarEvent event, LocalDate weekStart, LocalDate weekEndExclusive) {
        LocalDateTime endDateTime = event.getEndDateTime();
        LocalDate inclusiveEnd = endDateTime == null
                ? event.getStartDateTime().toLocalDate()
                : endDateTime.minusNanos(1).toLocalDate();
        LocalDate clippedEndExclusive = inclusiveEnd.plusDays(1).isAfter(weekEndExclusive)
                ? weekEndExclusive
                : inclusiveEnd.plusDays(1);
        return Math.min(DAY_COUNT, (int) java.time.temporal.ChronoUnit.DAYS.between(weekStart, clippedEndExclusive));
    }

    private String buildOverflowLabel(CalendarEvent event) {
        return event.getTitle();
    }

    private float resolveStartHour(CalendarEvent event) {
        if (event.isAllDay()) {
            return 0f;
        }
        LocalDateTime start = event.getStartDateTime();
        float value = start.getHour() + (start.getMinute() / 60f);
        return Math.max(0f, Math.min(HOUR_COUNT - 0.25f, value));
    }

    private float resolveEndHour(CalendarEvent event, float startHour) {
        if (event.isAllDay()) {
            return HOUR_COUNT;
        }
        LocalDateTime end = event.getEndDateTime();
        if (end == null) {
            return Math.min(HOUR_COUNT, startHour + 1f);
        }
        float value = end.toLocalDate().isAfter(event.getDate())
                ? HOUR_COUNT
                : end.getHour() + (end.getMinute() / 60f);
        if (value <= startHour) {
            value = startHour + 1f;
        }
        return Math.max(startHour + 0.25f, Math.min(HOUR_COUNT, value));
    }

    private void assignLanes(List<EventPlacement> dayPlacements) {
        dayPlacements.sort(Comparator
                .comparingDouble((EventPlacement placement) -> placement.startHour)
                .thenComparingDouble(placement -> placement.endHour));
        List<EventPlacement> cluster = new ArrayList<>();
        float clusterEnd = -1f;
        for (EventPlacement placement : dayPlacements) {
            if (cluster.isEmpty() || placement.startHour < clusterEnd - 0.001f) {
                cluster.add(placement);
                clusterEnd = Math.max(clusterEnd, placement.endHour);
            } else {
                resolveCluster(cluster);
                cluster.clear();
                cluster.add(placement);
                clusterEnd = placement.endHour;
            }
        }
        if (!cluster.isEmpty()) {
            resolveCluster(cluster);
        }
    }

    private void resolveCluster(List<EventPlacement> cluster) {
        List<EventPlacement> active = new ArrayList<>();
        int maxLaneCount = 0;
        for (EventPlacement placement : cluster) {
            active.removeIf(activePlacement -> activePlacement.endHour <= placement.startHour + 0.001f);
            boolean[] used = new boolean[MAX_VISIBLE_OVERLAP];
            for (EventPlacement activePlacement : active) {
                if (!activePlacement.hidden && activePlacement.laneIndex >= 0 && activePlacement.laneIndex < MAX_VISIBLE_OVERLAP) {
                    used[activePlacement.laneIndex] = true;
                }
            }
            int laneIndex = 0;
            while (laneIndex < used.length && used[laneIndex]) {
                laneIndex++;
            }
            if (laneIndex >= MAX_VISIBLE_OVERLAP) {
                placement.hidden = true;
                placement.laneIndex = -1;
                continue;
            }
            placement.laneIndex = laneIndex;
            placement.hidden = false;
            active.add(placement);
            maxLaneCount = Math.max(maxLaneCount, active.size());
        }
        for (EventPlacement placement : cluster) {
            if (!placement.hidden) {
                placement.laneCount = Math.max(1, Math.min(MAX_VISIBLE_OVERLAP, maxLaneCount));
            }
        }
    }

    private void rebuildEventViews() {
        removeAllViews();
        for (EventPlacement placement : placements) {
            addView(createEventView(placement));
        }
    }

    private View createEventView(EventPlacement placement) {
        TextView view = new TextView(getContext());
        view.setText(placement.event.getTitle());
        int fallback = getContext().getColor(R.color.event_blue);
        int fill = UiUtils.safeColor(placement.event.getColor(), fallback);
        view.setTextColor(UiUtils.readableTextColor(fill, getContext()));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, placement.laneCount > 1 ? 9f : 10f);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setGravity(Gravity.START | Gravity.TOP);
        int horizontalPadding = dp(EVENT_TEXT_HORIZONTAL_PADDING_DP);
        int verticalPadding = dp(EVENT_TEXT_VERTICAL_PADDING_DP);
        view.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1f);
        view.setMaxLines(4);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setBackground(UiUtils.adaptiveEventBackground(fill, 10, getContext()));
        view.setElevation(dp(1));
        view.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(placement.event);
            }
        });
        return view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int contentHeight = (HOUR_COUNT * dp(HOUR_ROW_HEIGHT_DP)) + 1;
        setMeasuredDimension(resolveSize(measuredWidth, widthMeasureSpec), contentHeight);
        for (int i = 0; i < getChildCount() && i < placements.size(); i++) {
            EventPlacement placement = placements.get(i);
            Rect bounds = computeBounds(placement, getMeasuredWidth(), getMeasuredHeight());
            placement.bounds.set(bounds);
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
        int timeColumnWidth = dp(TIME_COLUMN_WIDTH_DP);
        int rowHeight = dp(HOUR_ROW_HEIGHT_DP);
        float dayColumnWidth = Math.max(1f, (getWidth() - timeColumnWidth) / (float) DAY_COUNT);
        for (int day = 0; day <= DAY_COUNT; day++) {
            float x = crisp(timeColumnWidth + (day * dayColumnWidth));
            canvas.drawLine(x, 0, x, getHeight(), linePaint);
        }
        for (int hour = 0; hour <= HOUR_COUNT; hour++) {
            float y = crisp(Math.min(hour * rowHeight, getHeight() - 1f));
            canvas.drawLine(0, y, getWidth(), y, linePaint);
        }
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        for (int hour = 0; hour < HOUR_COUNT; hour++) {
            float baseline = (hour * rowHeight) + (rowHeight / 2f) - ((fontMetrics.ascent + fontMetrics.descent) / 2f);
            canvas.drawText(formatHour(hour), dp(7), baseline, textPaint);
        }
    }

    private Rect computeBounds(EventPlacement placement, int width, int height) {
        int timeColumnWidth = dp(TIME_COLUMN_WIDTH_DP);
        int columnInset = dp(EVENT_HORIZONTAL_INSET_DP);
        int topInset = dp(EVENT_TOP_INSET_DP);
        int bottomInset = dp(EVENT_BOTTOM_INSET_DP);
        int minHeight = dp(EVENT_MIN_HEIGHT_DP);
        float dayColumnWidth = Math.max(1f, (width - timeColumnWidth) / (float) DAY_COUNT);
        float laneWidth = dayColumnWidth / placement.laneCount;
        int left = Math.round(timeColumnWidth + (placement.dayIndex * dayColumnWidth) + (placement.laneIndex * laneWidth) + columnInset);
        int right = Math.round(timeColumnWidth + (placement.dayIndex * dayColumnWidth) + ((placement.laneIndex + 1) * laneWidth) - columnInset);
        int top = Math.round(placement.startHour * dp(HOUR_ROW_HEIGHT_DP)) + topInset;
        int bottom = Math.round(placement.endHour * dp(HOUR_ROW_HEIGHT_DP)) - bottomInset;
        if (bottom - top < minHeight) {
            bottom = top + minHeight;
        }
        int minWidth = dp(18);
        if (right - left < minWidth) {
            right = left + minWidth;
        }
        bottom = Math.min(height - bottomInset, bottom);
        return new Rect(left, top, right, bottom);
    }

    private float crisp(float value) {
        return (float) Math.floor(value) + 0.5f;
    }

    private String formatHour(int hour) {
        return String.format("%02d:00", hour);
    }

    private int dp(int value) {
        return UiUtils.dp(getContext(), value);
    }

    private float sp(int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private static class EventPlacement {
        final CalendarEvent event;
        final int dayIndex;
        final float startHour;
        final float endHour;
        final Rect bounds = new Rect();
        int laneIndex;
        int laneCount = 1;
        boolean hidden;

        EventPlacement(CalendarEvent event, int dayIndex, float startHour, float endHour) {
            this.event = event;
            this.dayIndex = dayIndex;
            this.startHour = startHour;
            this.endHour = endHour;
        }
    }

    public static class WeekStripItem {
        public static final int TYPE_ALL_DAY = 0;
        public static final int TYPE_MULTI_DAY = 1;
        public static final int TYPE_OVERFLOW = 2;

        private final CalendarEvent event;
        private final int startDayIndex;
        private final int endDayExclusiveIndex;
        private final String label;
        private final int type;

        public WeekStripItem(CalendarEvent event, int startDayIndex, int endDayExclusiveIndex, String label, int type) {
            this.event = event;
            this.startDayIndex = startDayIndex;
            this.endDayExclusiveIndex = endDayExclusiveIndex;
            this.label = label;
            this.type = type;
        }

        public CalendarEvent getEvent() {
            return event;
        }

        public int getStartDayIndex() {
            return startDayIndex;
        }

        public int getEndDayExclusiveIndex() {
            return endDayExclusiveIndex;
        }

        public String getLabel() {
            return label;
        }

        public int getType() {
            return type;
        }
    }
}
