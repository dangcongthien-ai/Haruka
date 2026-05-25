package com.example.finalproject.ui.calendar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.adapter.CalendarMonthPagerAdapter;
import com.example.finalproject.adapter.DayPagerAdapter;
import com.example.finalproject.adapter.DaySelectorAdapter;
import com.example.finalproject.adapter.EventListAdapter;
import com.example.finalproject.adapter.TodoListAdapter;
import com.example.finalproject.adapter.WeekDayHeaderAdapter;
import com.example.finalproject.adapter.WeekJournalAdapter;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.CalendarDayCell;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.CalendarRepository;
import com.example.finalproject.repository.TodoRepository;
import com.example.finalproject.ui.common.ScreenBackHandler;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment implements ScreenBackHandler {
    public static final int MODE_MONTH = 0;
    public static final int MODE_WEEK = 1;
    public static final int MODE_DAY = 2;
    private static final float MONTH_DETAIL_PANEL_RATIO = 0.45f;
    private static final long MONTH_PANEL_OPEN_DURATION_MS = 220L;
    private static final long MONTH_PANEL_CLOSE_DURATION_MS = 180L;
    private static final long PERIOD_SWIPE_OUT_DURATION_MS = 140L;
    private static final long PERIOD_SWIPE_IN_DURATION_MS = 220L;

    private CalendarRepository calendarRepository;
    private TodoRepository todoRepository;
    private View modeButton;
    private TextView modeLabel;
    private ImageView modeArrow;
    private TextView periodTitle;
    private View calendarNavRow;
    private TextView todayButton;
    private LinearLayout monthContainer;
    private View monthBody;
    private LinearLayout weekContainer;
    private LinearLayout dayContainer;
    private LinearLayout dayDetailPanel;
    private View dayDetailScroll;
    private TextView selectedDateLabel;
    private RecyclerView monthPager;
    private CalendarMonthPagerAdapter monthPagerAdapter;
    private PagerSnapHelper monthSnapHelper;
    private EventListAdapter selectedEventAdapter;
    private TodoListAdapter selectedTodoAdapter;
    private WeekDayHeaderAdapter weekDayAdapter;
    private WeekJournalAdapter weekJournalAdapter;
    private DaySelectorAdapter dayStripAdapter;
    private RecyclerView dayStripRecycler;
    private ViewPager2 dayPager;
    private DayPagerAdapter dayPagerAdapter;
    private View weekTopEventRow;
    private TextView weekMonthBadge;
    private TextView dayMonthBadge;
    private NestedScrollView weekTimelineScroll;
    private WeekTimelineLayout weekTimelineView;
    private WeekTopStripLayout weekTopStripView;

    private int mode = MODE_MONTH;
    private LocalDate selectedDate = LocalDate.now();
    private LocalDate visibleMonth = selectedDate.withDayOfMonth(1);
    private LocalDate lastWeekTimelineStart;
    private boolean monthDetailVisible = false;
    private boolean periodTransitionRunning = false;
    private boolean dayPagerRecentering = false;
    private int monthPanelAnimationGeneration = 0;
    private int monthDetailPanelTop = RecyclerView.NO_POSITION;
    private PopupWindow modePopupWindow;
    private final Map<YearMonth, List<CalendarDayCell>> monthCellCache = new HashMap<>();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("'Tháng' MM/yyyy", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarRepository = new CalendarRepository(requireContext());
        todoRepository = new TodoRepository(requireContext());
        bindViews(view);
        setupAdapters(view);
        setupClicks(view);
        attachSwipe(view);
        refresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateMonthCache();
        refresh();
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    private void bindViews(View view) {
        modeButton = view.findViewById(R.id.btn_mode);
        modeLabel = view.findViewById(R.id.tv_mode_label);
        modeArrow = view.findViewById(R.id.iv_mode_arrow);
        periodTitle = view.findViewById(R.id.tv_period_title);
        calendarNavRow = view.findViewById(R.id.calendar_nav_row);
        todayButton = view.findViewById(R.id.btn_today);
        monthContainer = view.findViewById(R.id.month_container);
        monthBody = view.findViewById(R.id.month_body);
        weekContainer = view.findViewById(R.id.week_container);
        dayContainer = view.findViewById(R.id.day_container);
        dayDetailPanel = view.findViewById(R.id.day_detail_panel);
        dayDetailScroll = view.findViewById(R.id.day_detail_scroll);
        selectedDateLabel = view.findViewById(R.id.tv_selected_date);
        weekTopEventRow = view.findViewById(R.id.week_top_event_row);
        weekMonthBadge = view.findViewById(R.id.tv_week_month_badge);
        dayMonthBadge = view.findViewById(R.id.tv_day_month_badge);
        dayStripRecycler = view.findViewById(R.id.day_strip_recycler);
        weekTimelineScroll = view.findViewById(R.id.week_timeline_scroll);
        dayPager = view.findViewById(R.id.day_pager);
        weekTimelineView = view.findViewById(R.id.week_timeline_view);
        weekTopStripView = view.findViewById(R.id.week_top_event_strip);
        monthBody.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> resizeMonthDetailPanel());
        dayDetailScroll.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
    }

    private void setupAdapters(View view) {
        monthPager = view.findViewById(R.id.month_recycler);
        monthPager.setLayoutManager(new SmoothMonthLayoutManager(requireContext()));
        monthPager.setNestedScrollingEnabled(false);
        monthPager.setHasFixedSize(true);
        monthPager.setItemViewCacheSize(5);
        monthPager.setItemAnimator(null);
        monthPagerAdapter = new CalendarMonthPagerAdapter(visibleMonth, this::buildMonthCells, this::onMonthDateClicked);
        monthPagerAdapter.setSelectedDate(selectedDate);
        monthPager.setAdapter(monthPagerAdapter);
        monthSnapHelper = new PagerSnapHelper();
        monthSnapHelper.attachToRecyclerView(monthPager);
        monthPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateMonthFromPager();
                }
            }
        });
        monthPager.post(() -> syncMonthPager(false));

        selectedEventAdapter = new EventListAdapter(eventActions());
        selectedEventAdapter.setShowActions(true);
        selectedTodoAdapter = new TodoListAdapter(todoActions());
        selectedTodoAdapter.setShowActions(true);
        RecyclerView selectedEventRecycler = view.findViewById(R.id.selected_event_recycler);
        selectedEventRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        selectedEventRecycler.setNestedScrollingEnabled(false);
        selectedEventRecycler.setAdapter(selectedEventAdapter);
        RecyclerView selectedTodoRecycler = view.findViewById(R.id.selected_todo_recycler);
        selectedTodoRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        selectedTodoRecycler.setNestedScrollingEnabled(false);
        selectedTodoRecycler.setAdapter(selectedTodoAdapter);

        weekDayAdapter = new WeekDayHeaderAdapter();
        RecyclerView weekDayRecycler = view.findViewById(R.id.week_day_recycler);
        weekDayRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        weekDayRecycler.setNestedScrollingEnabled(false);
        weekDayRecycler.setHasFixedSize(true);
        weekDayRecycler.setItemAnimator(null);
        weekDayRecycler.setAdapter(weekDayAdapter);

        weekJournalAdapter = new WeekJournalAdapter();
        RecyclerView weekJournalRecycler = view.findViewById(R.id.week_journal_recycler);
        weekJournalRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        weekJournalRecycler.setNestedScrollingEnabled(false);
        weekJournalRecycler.setHasFixedSize(true);
        weekJournalRecycler.setItemAnimator(null);
        weekJournalRecycler.setAdapter(weekJournalAdapter);

        weekTimelineView.setListener(event -> {
            if (event.getDate() != null) {
                selectedDate = event.getDate();
                ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
            }
            ((MainActivity) requireActivity()).openEventDetail(event.getId(), selectedDate);
        });
        weekTopStripView.setListener(event -> {
            if (event.getDate() != null) {
                selectedDate = event.getDate();
                ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
            }
            ((MainActivity) requireActivity()).openEventDetail(event.getId(), selectedDate);
        });

        dayStripAdapter = new DaySelectorAdapter(date -> {
            selectedDate = date;
            ((MainActivity) requireActivity()).setSelectedDate(date);
            updateHeader();
            refreshDay();
        });
        dayStripRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        dayStripRecycler.setNestedScrollingEnabled(false);
        dayStripRecycler.setHasFixedSize(true);
        dayStripRecycler.setItemAnimator(null);
        dayStripRecycler.setAdapter(dayStripAdapter);

        dayPagerAdapter = new DayPagerAdapter(
                calendarRepository,
                todoRepository,
                eventActions(),
                todoActions()
        );
        dayPager.setAdapter(dayPagerAdapter);
        dayPager.setOffscreenPageLimit(1);
        dayPager.setCurrentItem(1, false);
        dayPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (dayPagerRecentering || state != ViewPager2.SCROLL_STATE_IDLE) {
                    return;
                }
                int position = dayPager.getCurrentItem();
                if (position == 1) {
                    return;
                }
                dayPagerRecentering = true;
                selectedDate = dayPagerAdapter.getDateForPosition(position);
                ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
                updateHeader();
                refreshDay();
                dayPagerRecentering = false;
            }
        });
    }

    private void setupClicks(View view) {
        modeButton.setOnClickListener(v -> showModeMenu());
        ImageButton previous = view.findViewById(R.id.btn_previous);
        ImageButton next = view.findViewById(R.id.btn_next);
        previous.setOnClickListener(v -> movePeriod(-1));
        next.setOnClickListener(v -> movePeriod(1));
        todayButton.setOnClickListener(v -> jumpToToday());
        periodTitle.setOnClickListener(v -> showMonthPicker());
        weekMonthBadge.setOnClickListener(v -> showMonthPicker());
        dayMonthBadge.setOnClickListener(v -> showMonthPicker());
        view.findViewById(R.id.btn_close_month_detail).setOnClickListener(v -> closeMonthDetailPanel());
    }

    private void closeMonthDetailPanel() {
        if (!monthDetailVisible || dayDetailPanel == null || monthPagerAdapter == null) {
            return;
        }
        int currentPosition = monthPagerAdapter.getPositionForMonth(visibleMonth);
        int animationGeneration = ++monthPanelAnimationGeneration;
        dayDetailPanel.animate().cancel();
        monthDetailVisible = false;
        float closeDistance = Math.max(dayDetailPanel.getHeight(), monthBody.getHeight() - getMonthDetailPanelTop());
        dayDetailPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        dayDetailPanel.animate()
                .translationY(closeDistance)
                .alpha(0f)
                .setDuration(MONTH_PANEL_CLOSE_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator(1.4f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (animationGeneration != monthPanelAnimationGeneration || monthDetailVisible) {
                            return;
                        }
                        UiUtils.visible(dayDetailPanel, false);
                        dayDetailPanel.setTranslationY(0f);
                        dayDetailPanel.setAlpha(1f);
                        dayDetailPanel.setLayerType(View.LAYER_TYPE_NONE, null);
                        monthDetailPanelTop = RecyclerView.NO_POSITION;
                        monthPagerAdapter.closeMonthDetailInstantly(monthPager, currentPosition, selectedDate);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        dayDetailPanel.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                })
                .start();
    }

    private void showModeMenu() {
        if (modePopupWindow != null && modePopupWindow.isShowing()) {
            modePopupWindow.dismiss();
            return;
        }
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.popup_calendar_mode, null, false);
        PopupWindow popupWindow = new PopupWindow(
                content,
                UiUtils.dp(requireContext(), 120),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        modePopupWindow = popupWindow;
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(R.style.Caliary_PopupWindowAnimation);
        popupWindow.setOnDismissListener(() -> {
            modePopupWindow = null;
            animateModeArrow(false);
        });
        content.findViewById(R.id.popup_mode_month).setOnClickListener(v -> {
            mode = MODE_MONTH;
            popupWindow.dismiss();
            refresh();
        });
        content.findViewById(R.id.popup_mode_week).setOnClickListener(v -> {
            mode = MODE_WEEK;
            popupWindow.dismiss();
            refresh();
        });
        content.findViewById(R.id.popup_mode_day).setOnClickListener(v -> {
            mode = MODE_DAY;
            popupWindow.dismiss();
            refresh();
        });
        animateModeArrow(true);
        popupWindow.showAsDropDown(modeButton, 0, 0);
    }

    private void showMonthPicker() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_picker, null, false);
        dialog.setContentView(content);
        UiUtils.styleDialogWindow(dialog, UiUtils.dp(requireContext(), 320), ViewGroup.LayoutParams.WRAP_CONTENT, 0.28f);

        TextView yearLabel = content.findViewById(R.id.tv_picker_year);
        GridLayout monthGrid = content.findViewById(R.id.month_picker_grid);
        LocalDate anchorDate = mode == MODE_MONTH ? visibleMonth : selectedDate;
        YearMonth[] draft = {YearMonth.from(anchorDate)};
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            yearLabel.setText(String.valueOf(draft[0].getYear()));
            monthGrid.removeAllViews();
            for (int month = 1; month <= 12; month++) {
                TextView chip = new TextView(requireContext());
                chip.setText("Tháng " + month);
                chip.setGravity(android.view.Gravity.CENTER);
                chip.setSingleLine(true);
                chip.setTextSize(14);
                boolean selected = draft[0].getMonthValue() == month;
                chip.setTextColor(requireContext().getColor(selected ? R.color.white : R.color.text_primary));
                chip.setBackground(selected
                        ? UiUtils.rounded(requireContext().getColor(R.color.brand_orange), 8, requireContext())
                        : UiUtils.roundedStroke(requireContext().getColor(R.color.surface), requireContext().getColor(R.color.line), 8, requireContext()));
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec((month - 1) / 3),
                        GridLayout.spec((month - 1) % 3)
                );
                params.width = UiUtils.dp(requireContext(), 88);
                params.height = UiUtils.dp(requireContext(), 40);
                int margin = UiUtils.dp(requireContext(), 3);
                params.setMargins(margin, UiUtils.dp(requireContext(), 4), margin, UiUtils.dp(requireContext(), 4));
                chip.setLayoutParams(params);
                int pickedMonth = month;
                chip.setOnClickListener(v -> {
                    draft[0] = YearMonth.of(draft[0].getYear(), pickedMonth);
                    render[0].run();
                });
                monthGrid.addView(chip);
            }
        };

        content.findViewById(R.id.btn_picker_prev_year).setOnClickListener(v -> {
            draft[0] = draft[0].minusYears(1);
            render[0].run();
        });
        content.findViewById(R.id.btn_picker_next_year).setOnClickListener(v -> {
            draft[0] = draft[0].plusYears(1);
            render[0].run();
        });
        yearLabel.setOnClickListener(v -> showYearPicker(draft[0].getYear(), year -> {
            draft[0] = YearMonth.of(year, draft[0].getMonthValue());
            render[0].run();
        }));
        content.findViewById(R.id.btn_month_picker_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_month_picker_ok).setOnClickListener(v -> {
            visibleMonth = draft[0].atDay(1);
            selectedDate = visibleMonth.withDayOfMonth(Math.min(selectedDate.getDayOfMonth(), draft[0].lengthOfMonth()));
            monthDetailVisible = false;
            ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
            dialog.dismiss();
            refresh();
        });
        render[0].run();
        dialog.show();
    }

    private void showYearPicker(int selectedYear, YearPickerListener listener) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_year_picker, null, false);
        dialog.setContentView(content);
        UiUtils.styleDialogWindow(dialog, UiUtils.dp(requireContext(), 344), ViewGroup.LayoutParams.WRAP_CONTENT, 0.28f);

        NumberPicker picker = content.findViewById(R.id.picker_year);
        int minYear = Math.max(1900, selectedYear - 100);
        int maxYear = Math.min(2200, selectedYear + 100);
        picker.setMinValue(minYear);
        picker.setMaxValue(maxYear);
        picker.setValue(selectedYear);
        picker.setWrapSelectorWheel(false);
        UiUtils.styleNumberPicker(picker, requireContext());

        content.findViewById(R.id.btn_year_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_year_ok).setOnClickListener(v -> {
            listener.onYearPicked(picker.getValue());
            dialog.dismiss();
        });
        dialog.show();
    }

    private void animateModeArrow(boolean expanded) {
        if (modeArrow == null) {
            return;
        }
        modeArrow.animate()
                .rotation(expanded ? 180f : 0f)
                .setDuration(220L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private interface YearPickerListener {
        void onYearPicked(int year);
    }

    private void movePeriod(int direction) {
        if (mode == MODE_MONTH) {
            LocalDate previousMonth = visibleMonth;
            visibleMonth = visibleMonth.plusMonths(direction);
            selectedDate = visibleMonth.withDayOfMonth(Math.min(selectedDate.getDayOfMonth(), visibleMonth.lengthOfMonth()));
            monthDetailVisible = false;
            monthDetailPanelTop = RecyclerView.NO_POSITION;
            ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
            updateHeader();
            resizeMonthDetailPanel();
            UiUtils.visible(dayDetailPanel, false);
            preloadMonthWindow(visibleMonth);
            monthPagerAdapter.setSelectedDate(selectedDate);
            monthPagerAdapter.setMonthDetailVisible(false);
            notifyMonthPages(previousMonth, visibleMonth);
            syncMonthPager(true);
            return;
        }
        if (mode == MODE_DAY) {
            if (dayPager != null) {
                dayPager.setCurrentItem(dayPager.getCurrentItem() + direction, true);
            }
            return;
        }
        animatePeriodSwipe(direction);
    }

    private void animatePeriodSwipe(int direction) {
        if (periodTransitionRunning) {
            return;
        }
        View container = weekContainer;
        View[] targets = new View[]{container};
        View measurementTarget = container;
        if (measurementTarget == null || measurementTarget.getWidth() <= 0) {
            applyPeriodShift(direction);
            refresh();
            return;
        }
        periodTransitionRunning = true;
        float distance = Math.max(measurementTarget.getWidth() * 0.28f, UiUtils.dp(requireContext(), 92));
        float exitTranslation = direction > 0 ? -distance : distance;
        float enterTranslation = -exitTranslation;
        for (View target : targets) {
            if (target == null) {
                continue;
            }
            target.animate().cancel();
            target.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            target.animate()
                    .translationX(exitTranslation)
                    .alpha(0.8f)
                    .setDuration(170L)
                    .setInterpolator(new AccelerateInterpolator(1.15f))
                    .setListener(null)
                    .start();
        }
        measurementTarget.postDelayed(() -> {
            if (!isAdded()) {
                periodTransitionRunning = false;
                clearSwipeTargets(targets);
                return;
            }
            applyPeriodShift(direction);
            refresh();
            for (View target : targets) {
                if (target == null) {
                    continue;
                }
                target.animate().cancel();
                target.setTranslationX(enterTranslation);
                target.setAlpha(0.84f);
                target.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(250L)
                        .setInterpolator(new DecelerateInterpolator(1.55f))
                        .setListener(null)
                        .start();
            }
            measurementTarget.postDelayed(() -> {
                periodTransitionRunning = false;
                clearSwipeTargets(targets);
            }, 250L);
        }, 170L);
    }

    private void clearSwipeTargets(View[] targets) {
        for (View target : targets) {
            if (target == null) {
                continue;
            }
            target.setLayerType(View.LAYER_TYPE_NONE, null);
            target.setTranslationX(0f);
            target.setAlpha(1f);
        }
    }

    private void applyPeriodShift(int direction) {
        if (mode == MODE_WEEK) {
            selectedDate = selectedDate.plusWeeks(direction);
        } else {
            selectedDate = selectedDate.plusDays(direction);
        }
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
    }

    private void refresh() {
        if (!isAdded()) {
            return;
        }
        updateHeader();
        UiUtils.visible(calendarNavRow, mode == MODE_MONTH || mode == MODE_DAY);
        UiUtils.visible(monthContainer, mode == MODE_MONTH);
        UiUtils.visible(weekContainer, mode == MODE_WEEK);
        UiUtils.visible(dayContainer, mode == MODE_DAY);
        if (mode == MODE_MONTH) {
            refreshMonth();
        } else if (mode == MODE_WEEK) {
            refreshWeek();
        } else {
            refreshDay();
        }
    }

    private void updateHeader() {
        if (mode == MODE_MONTH) {
            modeLabel.setText(R.string.month);
            periodTitle.setText(visibleMonth.format(monthFormatter));
        } else if (mode == MODE_WEEK) {
            modeLabel.setText(R.string.week);
            periodTitle.setText(selectedDate.format(monthFormatter));
        } else {
            modeLabel.setText(R.string.day);
            periodTitle.setText(DateTimeUtils.formatDateWithDow(selectedDate));
        }
        updateTodayButton();
    }

    private void updateTodayButton() {
        if (todayButton == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        boolean visible = false;
        if (mode == MODE_MONTH) {
            visible = !visibleMonth.equals(today.withDayOfMonth(1)) || !selectedDate.equals(today);
        } else if (mode == MODE_DAY) {
            visible = !selectedDate.equals(today);
        }
        UiUtils.visible(todayButton, visible);
    }

    private void refreshMonth() {
        preloadMonthWindow(visibleMonth);
        monthPagerAdapter.setSelectedDate(selectedDate);
        monthPagerAdapter.setMonthDetailVisible(monthDetailVisible);
        monthPagerAdapter.notifyDataSetChanged();
        resizeMonthDetailPanel();
        syncMonthPager(false);
        UiUtils.visible(dayDetailPanel, monthDetailVisible);
        if (monthDetailVisible) {
            updateMonthDetailContent(false);
        }
    }

    private void updateMonthDetailContent(boolean resetScroll) {
        selectedDateLabel.setText(DateTimeUtils.formatDateWithDow(selectedDate));
        selectedEventAdapter.submit(calendarRepository.getEventsForDate(selectedDate));
        selectedTodoAdapter.submit(todoRepository.getTodosForDate(selectedDate));
        dayDetailScroll.post(() -> {
            if (resetScroll) {
                dayDetailScroll.scrollTo(0, 0);
            }
            dayDetailScroll.requestLayout();
        });
    }

    private List<CalendarDayCell> buildMonthCells(LocalDate monthStart) {
        YearMonth cacheKey = YearMonth.from(monthStart);
        List<CalendarDayCell> cachedCells = monthCellCache.get(cacheKey);
        if (cachedCells != null) {
            return cachedCells;
        }
        LocalDate monthFirstDay = cacheKey.atDay(1);
        LocalDate gridStart = getMonthGridStart(monthFirstDay);
        LocalDate gridEnd = gridStart.plusDays(42);
        List<CalendarEvent> events = calendarRepository.getEventsBetween(gridStart.atStartOfDay(), gridEnd.atStartOfDay());
        Map<LocalDate, List<CalendarEvent>> eventsByDate = groupEventsByDate(events);
        List<CalendarDayCell> cells = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            LocalDate date = gridStart.plusDays(i);
            CalendarDayCell cell = new CalendarDayCell(date, YearMonth.from(date).equals(cacheKey));
            List<CalendarEvent> dayEvents = eventsByDate.get(date);
            if (dayEvents != null) {
                cell.getEvents().addAll(dayEvents);
            }
            cells.add(cell);
        }
        monthCellCache.put(cacheKey, cells);
        return cells;
    }

    private void refreshWeek() {
        LocalDate weekStart = getWeekStart(selectedDate);
        List<LocalDate> days = buildWeekDays(weekStart);
        weekMonthBadge.setText(getString(R.string.week_month_year_badge, selectedDate.getMonthValue(), selectedDate.getYear()));
        weekDayAdapter.submit(days);
        weekJournalAdapter.submit(days);
        List<CalendarEvent> events = calendarRepository.getEventsBetween(weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay());
        weekTimelineView.submit(days, events);
        weekTopStripView.submit(days, weekTimelineView.getStripItems());
        UiUtils.visible(weekTopEventRow, true);
        if (lastWeekTimelineStart == null || !lastWeekTimelineStart.equals(weekStart)) {
            lastWeekTimelineStart = weekStart;
            weekTimelineScroll.post(() -> weekTimelineScroll.scrollTo(0, weekTimelineView.getScrollYForHour(6)));
        }
    }

    private void jumpToToday() {
        LocalDate today = LocalDate.now();
        LocalDate previousMonth = visibleMonth;
        selectedDate = today;
        visibleMonth = today.withDayOfMonth(1);
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        if (mode == MODE_MONTH) {
            monthDetailVisible = false;
            monthDetailPanelTop = RecyclerView.NO_POSITION;
            updateHeader();
            resizeMonthDetailPanel();
            UiUtils.visible(dayDetailPanel, false);
            preloadMonthWindow(visibleMonth);
            monthPagerAdapter.setSelectedDate(selectedDate);
            monthPagerAdapter.setMonthDetailVisible(false);
            notifyMonthPages(previousMonth, visibleMonth);
            syncMonthPager(true);
            return;
        }
        refresh();
    }

    private void refreshDay() {
        dayMonthBadge.setText(getString(R.string.day_month_year_badge, selectedDate.getMonthValue(), selectedDate.getYear()));
        dayStripAdapter.submit(buildWeekDays(getWeekStart(selectedDate)), selectedDate);
        if (dayPagerAdapter != null) {
            dayPagerAdapter.setCenterDate(selectedDate);
        }
        if (dayPager != null && dayPager.getCurrentItem() != 1) {
            dayPagerRecentering = true;
            dayPager.setCurrentItem(1, false);
            dayPagerRecentering = false;
        }
    }

    private EventListAdapter.Listener eventActions() {
        return new EventListAdapter.Listener() {
            @Override
            public void onClick(CalendarEvent event) {
                ((MainActivity) requireActivity()).openEventDetail(event.getId(), event.getDate());
            }

            @Override
            public void onEdit(CalendarEvent event) {
                ((MainActivity) requireActivity()).openEventEditor(event.getId(), selectedDate);
            }

            @Override
            public void onDelete(CalendarEvent event) {
                UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_event), () -> {
                    calendarRepository.deleteEvent(event.getId());
                    invalidateMonthCache();
                    refresh();
                });
            }
        };
    }

    @Override
    public boolean onHandleBackPressed() {
        if (mode == MODE_MONTH && monthDetailVisible) {
            closeMonthDetailPanel();
            return true;
        }
        return false;
    }

    private TodoListAdapter.Listener todoActions() {
        return new TodoListAdapter.Listener() {
            @Override
            public void onClick(TodoItem item) {
                ((MainActivity) requireActivity()).openTodoDetail(item.getId());
            }

            @Override
            public void onToggle(TodoItem item, boolean completed) {
                todoRepository.setCompleted(item.getId(), completed);
                refresh();
            }

            @Override
            public void onEdit(TodoItem item) {
                ((MainActivity) requireActivity()).openTodoEditor(item.getId(), item.getTodoDate());
            }

            @Override
            public void onDelete(TodoItem item) {
                UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_todo), () -> {
                    todoRepository.deleteTodo(item.getId());
                    refresh();
                });
            }
        };
    }

    private void onMonthDateClicked(LocalDate date) {
        boolean wasDetailVisible = monthDetailVisible;
        LocalDate previousMonth = visibleMonth;
        selectedDate = date;
        visibleMonth = date.withDayOfMonth(1);
        monthDetailVisible = true;
        ((MainActivity) requireActivity()).setSelectedDate(date);
        updateHeader();
        resizeMonthDetailPanel();
        dayDetailPanel.animate().cancel();
        updateMonthDetailContent(!wasDetailVisible);
        monthPagerAdapter.setSelectedDate(selectedDate);
        monthPagerAdapter.setMonthDetailVisible(true);
        preloadMonthWindow(visibleMonth);
        int targetPosition = monthPagerAdapter.getPositionForMonth(visibleMonth);
        anchorMonthDetailPanelToBottom(targetPosition);
        showMonthDetailPanel(wasDetailVisible);
        if (previousMonth.equals(visibleMonth)) {
            monthPagerAdapter.updateVisibleMonthSelection(monthPager, targetPosition, selectedDate, true);
            revealSelectedDateAbovePanel(targetPosition);
        } else {
            syncMonthPager(false);
            monthPagerAdapter.notifyItemChanged(targetPosition);
            monthPager.post(() -> revealSelectedDateAbovePanel(targetPosition));
        }
    }

    private void showMonthDetailPanel(boolean wasDetailVisible) {
        int animationGeneration = ++monthPanelAnimationGeneration;
        UiUtils.visible(dayDetailPanel, true);
        dayDetailPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (wasDetailVisible && dayDetailPanel.getAlpha() > 0f) {
            dayDetailPanel.setTranslationY(0f);
            dayDetailPanel.setAlpha(1f);
            dayDetailPanel.setLayerType(View.LAYER_TYPE_NONE, null);
            return;
        }
        dayDetailPanel.post(() -> {
            if (animationGeneration != monthPanelAnimationGeneration || !monthDetailVisible) {
                return;
            }
            float startTranslation = Math.max(dayDetailPanel.getHeight(), monthBody.getHeight() - getMonthDetailPanelTop());
            dayDetailPanel.setTranslationY(startTranslation);
            dayDetailPanel.setAlpha(0.96f);
            dayDetailPanel.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(MONTH_PANEL_OPEN_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (animationGeneration == monthPanelAnimationGeneration) {
                                dayDetailPanel.setLayerType(View.LAYER_TYPE_NONE, null);
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            dayDetailPanel.setLayerType(View.LAYER_TYPE_NONE, null);
                        }
                    })
                    .start();
        });
    }

    private void revealSelectedDateAbovePanel(int targetPosition) {
        if (monthPager == null || monthPagerAdapter == null) {
            return;
        }
        dayDetailPanel.post(() -> {
            anchorMonthDetailPanelToBottom(targetPosition);
            monthPagerAdapter.setMonthDetailPanelHeight(dayDetailPanel.getHeight());
            monthPagerAdapter.setMonthDetailPanelTop(getMonthDetailPanelTop());
            monthPagerAdapter.revealDateAbovePanel(monthPager, targetPosition, selectedDate);
            monthPager.postDelayed(() -> {
                anchorMonthDetailPanelToBottom(targetPosition);
                monthPagerAdapter.setMonthDetailPanelHeight(dayDetailPanel.getHeight());
                monthPagerAdapter.setMonthDetailPanelTop(getMonthDetailPanelTop());
                monthPagerAdapter.revealDateAbovePanel(monthPager, targetPosition, selectedDate);
            }, 32);
            monthPager.postDelayed(() -> {
                anchorMonthDetailPanelToBottom(targetPosition);
                monthPagerAdapter.setMonthDetailPanelHeight(dayDetailPanel.getHeight());
                monthPagerAdapter.setMonthDetailPanelTop(getMonthDetailPanelTop());
                monthPagerAdapter.revealDateAbovePanel(monthPager, targetPosition, selectedDate);
            }, 260);
        });
    }

    private void syncMonthPager(boolean smooth) {
        if (monthPager == null || monthPagerAdapter == null) {
            return;
        }
        int targetPosition = monthPagerAdapter.getPositionForMonth(visibleMonth);
        int currentPosition = getCurrentMonthPagerPosition();
        if (currentPosition == targetPosition) {
            return;
        }
        if (smooth && currentPosition != RecyclerView.NO_POSITION && Math.abs(targetPosition - currentPosition) <= 2) {
            monthPager.smoothScrollToPosition(targetPosition);
        } else if (monthPager.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
            monthPager.stopScroll();
            monthPager.scrollToPosition(targetPosition);
        }
    }

    private int getCurrentMonthPagerPosition() {
        if (monthPager == null) {
            return RecyclerView.NO_POSITION;
        }
        RecyclerView.LayoutManager layoutManager = monthPager.getLayoutManager();
        View snapView = monthSnapHelper == null ? null : monthSnapHelper.findSnapView(layoutManager);
        if (snapView != null) {
            return monthPager.getChildAdapterPosition(snapView);
        }
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }
        return RecyclerView.NO_POSITION;
    }

    private void updateMonthFromPager() {
        int position = getCurrentMonthPagerPosition();
        if (position == RecyclerView.NO_POSITION || monthPagerAdapter == null) {
            return;
        }
        LocalDate newMonth = monthPagerAdapter.getMonthForPosition(position);
        if (newMonth.equals(visibleMonth)) {
            return;
        }
        LocalDate previousMonth = visibleMonth;
        visibleMonth = newMonth;
        selectedDate = visibleMonth.withDayOfMonth(Math.min(selectedDate.getDayOfMonth(), visibleMonth.lengthOfMonth()));
        monthDetailVisible = false;
        monthDetailPanelTop = RecyclerView.NO_POSITION;
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        updateHeader();
        resizeMonthDetailPanel();
        UiUtils.visible(dayDetailPanel, false);
        preloadMonthWindow(visibleMonth);
        monthPagerAdapter.setSelectedDate(selectedDate);
        monthPagerAdapter.setMonthDetailVisible(false);
        notifyMonthPages(previousMonth, visibleMonth);
    }

    private void notifyMonthPages(LocalDate previousMonth, LocalDate nextMonth) {
        if (monthPagerAdapter == null) {
            return;
        }
        int previousPosition = previousMonth == null ? RecyclerView.NO_POSITION : monthPagerAdapter.getPositionForMonth(previousMonth);
        int nextPosition = nextMonth == null ? RecyclerView.NO_POSITION : monthPagerAdapter.getPositionForMonth(nextMonth);
        if (previousPosition != RecyclerView.NO_POSITION) {
            monthPagerAdapter.notifyItemChanged(previousPosition);
        }
        if (nextPosition != RecyclerView.NO_POSITION && nextPosition != previousPosition) {
            monthPagerAdapter.notifyItemChanged(nextPosition);
        }
    }

    private void resizeMonthDetailPanel() {
        if (monthBody == null || dayDetailPanel == null || monthBody.getHeight() <= 0) {
            return;
        }
        int targetHeight = Math.round(monthBody.getHeight() * MONTH_DETAIL_PANEL_RATIO);
        int maxTop = Math.max(0, monthBody.getHeight() - targetHeight);
        setMonthDetailPanelFrame(maxTop, targetHeight);
    }

    private int anchorMonthDetailPanelToBottom(int targetPosition) {
        if (monthBody == null || dayDetailPanel == null || monthBody.getHeight() <= 0) {
            return RecyclerView.NO_POSITION;
        }
        int panelHeight = Math.round(monthBody.getHeight() * MONTH_DETAIL_PANEL_RATIO);
        int maxTop = Math.max(0, monthBody.getHeight() - panelHeight);
        setMonthDetailPanelFrame(maxTop, panelHeight);
        if (monthPagerAdapter != null) {
            monthPagerAdapter.updateVisibleMonthSelection(monthPager, targetPosition, selectedDate, monthDetailVisible);
        }
        return maxTop;
    }

    private void setMonthDetailPanelFrame(int top, int height) {
        ViewGroup.LayoutParams rawParams = dayDetailPanel.getLayoutParams();
        if (!(rawParams instanceof FrameLayout.LayoutParams)) {
            rawParams.height = height;
            dayDetailPanel.setLayoutParams(rawParams);
            monthDetailPanelTop = top;
            return;
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rawParams;
        boolean changed = params.height != height || params.topMargin != 0 || params.gravity != Gravity.BOTTOM;
        params.height = height;
        params.topMargin = 0;
        params.gravity = Gravity.BOTTOM;
        if (changed) {
            dayDetailPanel.setLayoutParams(params);
        }
        monthDetailPanelTop = top;
        if (monthPagerAdapter != null) {
            monthPagerAdapter.setMonthDetailPanelHeight(height);
            monthPagerAdapter.setMonthDetailPanelTop(top);
        }
    }

    private int getMonthDetailPanelTop() {
        if (monthDetailPanelTop != RecyclerView.NO_POSITION) {
            return monthDetailPanelTop;
        }
        if (dayDetailPanel != null && dayDetailPanel.getTop() > 0) {
            return dayDetailPanel.getTop();
        }
        if (monthBody != null && dayDetailPanel != null && monthBody.getHeight() > 0) {
            int panelHeight = dayDetailPanel.getHeight() > 0
                    ? dayDetailPanel.getHeight()
                    : Math.round(monthBody.getHeight() * MONTH_DETAIL_PANEL_RATIO);
            return Math.max(0, monthBody.getHeight() - panelHeight);
        }
        return 0;
    }

    private void preloadMonthWindow(LocalDate monthStart) {
        if (monthStart == null) {
            return;
        }
        LocalDate normalizedMonth = monthStart.withDayOfMonth(1);
        for (int offset = -1; offset <= 1; offset++) {
            buildMonthCells(normalizedMonth.plusMonths(offset));
        }
    }

    private void invalidateMonthCache() {
        monthCellCache.clear();
    }

    private Map<LocalDate, List<CalendarEvent>> groupEventsByDate(List<CalendarEvent> events) {
        Map<LocalDate, List<CalendarEvent>> result = new HashMap<>();
        for (CalendarEvent event : events) {
            if (event.getDate() == null) {
                continue;
            }
            if (!result.containsKey(event.getDate())) {
                result.put(event.getDate(), new ArrayList<>());
            }
            result.get(event.getDate()).add(event);
        }
        return result;
    }

    private LocalDate getMonthGridStart(LocalDate monthDate) {
        LocalDate first = YearMonth.from(monthDate).atDay(1);
        return first.minusDays(first.getDayOfWeek().getValue() - 1L);
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    private List<LocalDate> buildWeekDays(LocalDate weekStart) {
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(weekStart.plusDays(i));
        }
        return days;
    }

    private void attachSwipe(View view) {
        GestureDetector detector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (mode != MODE_WEEK || periodTransitionRunning || e1 == null || e2 == null || Math.abs(velocityX) < Math.abs(velocityY)) {
                    return false;
                }
                if (Math.abs(e2.getX() - e1.getX()) > UiUtils.dp(requireContext(), 80)) {
                    movePeriod(e2.getX() > e1.getX() ? -1 : 1);
                    return true;
                }
                return false;
            }
        });
        View.OnTouchListener listener = (v, event) -> {
            detector.onTouchEvent(event);
            return false;
        };
        view.findViewById(R.id.week_day_recycler).setOnTouchListener(listener);
        view.findViewById(R.id.week_journal_recycler).setOnTouchListener(listener);
        view.findViewById(R.id.week_top_event_strip).setOnTouchListener(listener);
        view.findViewById(R.id.week_timeline_scroll).setOnTouchListener(listener);
    }

    private static class SmoothMonthLayoutManager extends LinearLayoutManager {
        SmoothMonthLayoutManager(Context context) {
            super(context, LinearLayoutManager.HORIZONTAL, false);
            setInitialPrefetchItemCount(3);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
            LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return 90f / displayMetrics.densityDpi;
                }

                @Override
                protected int calculateTimeForScrolling(int dx) {
                    return Math.min(360, Math.max(180, super.calculateTimeForScrolling(dx) * 4));
                }
            };
            scroller.setTargetPosition(position);
            startSmoothScroll(scroller);
        }
    }
}
