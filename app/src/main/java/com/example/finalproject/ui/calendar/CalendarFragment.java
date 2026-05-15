package com.example.finalproject.ui.calendar;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.adapter.CalendarMonthPagerAdapter;
import com.example.finalproject.adapter.DaySelectorAdapter;
import com.example.finalproject.adapter.EventListAdapter;
import com.example.finalproject.adapter.TodoListAdapter;
import com.example.finalproject.adapter.WeekHourAdapter;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.CalendarDayCell;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.CalendarRepository;
import com.example.finalproject.repository.TodoRepository;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {
    public static final int MODE_MONTH = 0;
    public static final int MODE_WEEK = 1;
    public static final int MODE_DAY = 2;

    private CalendarRepository calendarRepository;
    private TodoRepository todoRepository;
    private View modeButton;
    private TextView modeLabel;
    private TextView periodTitle;
    private LinearLayout calendarNavRow;
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
    private DaySelectorAdapter weekDayAdapter;
    private DaySelectorAdapter dayStripAdapter;
    private WeekHourAdapter weekHourAdapter;
    private EventListAdapter dayEventAdapter;
    private TodoListAdapter dayTodoAdapter;
    private RecyclerView weekTimelineRecycler;

    private int mode = MODE_MONTH;
    private LocalDate selectedDate = LocalDate.now();
    private LocalDate visibleMonth = selectedDate.withDayOfMonth(1);
    private boolean monthDetailVisible = false;
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
        refresh();
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    private void bindViews(View view) {
        modeButton = view.findViewById(R.id.btn_mode);
        modeLabel = view.findViewById(R.id.tv_mode_label);
        periodTitle = view.findViewById(R.id.tv_period_title);
        calendarNavRow = view.findViewById(R.id.calendar_nav_row);
        monthContainer = view.findViewById(R.id.month_container);
        monthBody = view.findViewById(R.id.month_body);
        weekContainer = view.findViewById(R.id.week_container);
        dayContainer = view.findViewById(R.id.day_container);
        dayDetailPanel = view.findViewById(R.id.day_detail_panel);
        dayDetailScroll = view.findViewById(R.id.day_detail_scroll);
        selectedDateLabel = view.findViewById(R.id.tv_selected_date);
        weekTimelineRecycler = view.findViewById(R.id.week_timeline_recycler);
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
        monthPager.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        monthPager.setNestedScrollingEnabled(false);
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
        selectedEventAdapter.setShowActions(false);
        selectedTodoAdapter = new TodoListAdapter(todoActions());
        selectedTodoAdapter.setShowActions(false);
        RecyclerView selectedEventRecycler = view.findViewById(R.id.selected_event_recycler);
        selectedEventRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        selectedEventRecycler.setNestedScrollingEnabled(false);
        selectedEventRecycler.setAdapter(selectedEventAdapter);
        RecyclerView selectedTodoRecycler = view.findViewById(R.id.selected_todo_recycler);
        selectedTodoRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        selectedTodoRecycler.setNestedScrollingEnabled(false);
        selectedTodoRecycler.setAdapter(selectedTodoAdapter);

        weekDayAdapter = new DaySelectorAdapter(date -> {
            selectedDate = date;
            ((MainActivity) requireActivity()).setSelectedDate(date);
            refresh();
        });
        RecyclerView weekDayRecycler = view.findViewById(R.id.week_day_recycler);
        weekDayRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        weekDayRecycler.setAdapter(weekDayAdapter);

        weekHourAdapter = new WeekHourAdapter(event -> ((MainActivity) requireActivity()).openEventEditor(event.getId(), selectedDate));
        weekTimelineRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        weekTimelineRecycler.setAdapter(weekHourAdapter);

        dayStripAdapter = new DaySelectorAdapter(date -> {
            selectedDate = date;
            ((MainActivity) requireActivity()).setSelectedDate(date);
            refresh();
        });
        RecyclerView dayStripRecycler = view.findViewById(R.id.day_strip_recycler);
        dayStripRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        dayStripRecycler.setAdapter(dayStripAdapter);

        dayEventAdapter = new EventListAdapter(eventActions());
        dayEventAdapter.setShowActions(false);
        RecyclerView dayEventRecycler = view.findViewById(R.id.day_event_recycler);
        dayEventRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        dayEventRecycler.setAdapter(dayEventAdapter);

        dayTodoAdapter = new TodoListAdapter(todoActions());
        dayTodoAdapter.setShowActions(false);
        RecyclerView dayTodoRecycler = view.findViewById(R.id.day_todo_recycler);
        dayTodoRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        dayTodoRecycler.setAdapter(dayTodoAdapter);
    }

    private void setupClicks(View view) {
        modeButton.setOnClickListener(v -> showModeMenu());
        ImageButton previous = view.findViewById(R.id.btn_previous);
        ImageButton next = view.findViewById(R.id.btn_next);
        previous.setOnClickListener(v -> movePeriod(-1));
        next.setOnClickListener(v -> movePeriod(1));
        periodTitle.setOnClickListener(v -> showMonthPicker());
        view.findViewById(R.id.btn_close_month_detail).setOnClickListener(v -> {
            monthDetailVisible = false;
            refreshMonth();
        });
    }

    private void showModeMenu() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.popup_calendar_mode, null, false);
        PopupWindow popupWindow = new PopupWindow(
                content,
                UiUtils.dp(requireContext(), 120),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
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
        popupWindow.showAsDropDown(modeButton, 0, 0);
    }

    private void showMonthPicker() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_picker, null, false);
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView yearLabel = content.findViewById(R.id.tv_picker_year);
        GridLayout monthGrid = content.findViewById(R.id.month_picker_grid);
        YearMonth[] draft = {YearMonth.from(visibleMonth)};
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
                params.width = UiUtils.dp(requireContext(), 82);
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
        content.findViewById(R.id.btn_month_picker_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_month_picker_ok).setOnClickListener(v -> {
            visibleMonth = draft[0].atDay(1);
            selectedDate = visibleMonth.withDayOfMonth(Math.min(selectedDate.getDayOfMonth(), visibleMonth.lengthOfMonth()));
            monthDetailVisible = false;
            ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
            dialog.dismiss();
            refresh();
        });
        render[0].run();
        dialog.show();
    }

    private void movePeriod(int direction) {
        if (mode == MODE_MONTH) {
            visibleMonth = visibleMonth.plusMonths(direction);
            selectedDate = visibleMonth.withDayOfMonth(Math.min(selectedDate.getDayOfMonth(), visibleMonth.lengthOfMonth()));
            monthDetailVisible = false;
            ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
            updateHeader();
            refreshMonth();
            syncMonthPager(true);
            return;
        } else if (mode == MODE_WEEK) {
            selectedDate = selectedDate.plusWeeks(direction);
        } else {
            selectedDate = selectedDate.plusDays(direction);
        }
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        refresh();
    }

    private void refresh() {
        if (!isAdded()) {
            return;
        }
        updateHeader();
        UiUtils.visible(calendarNavRow, mode == MODE_MONTH);
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
    }

    private void refreshMonth() {
        monthPagerAdapter.setSelectedDate(selectedDate);
        monthPagerAdapter.setMonthDetailVisible(monthDetailVisible);
        monthPagerAdapter.notifyDataSetChanged();
        resizeMonthDetailPanel();
        syncMonthPager(false);
        UiUtils.visible(dayDetailPanel, monthDetailVisible);
        if (monthDetailVisible) {
            selectedDateLabel.setText(DateTimeUtils.formatDateWithDow(selectedDate));
            selectedEventAdapter.submit(calendarRepository.getEventsForDate(selectedDate));
            selectedTodoAdapter.submit(todoRepository.getTodosForDate(selectedDate));
            dayDetailScroll.post(dayDetailScroll::requestLayout);
        }
    }

    private List<CalendarDayCell> buildMonthCells(LocalDate monthStart) {
        LocalDate gridStart = getMonthGridStart(monthStart);
        LocalDate gridEnd = gridStart.plusDays(42);
        List<CalendarEvent> events = calendarRepository.getEventsBetween(gridStart.atStartOfDay(), gridEnd.atStartOfDay());
        Map<LocalDate, List<CalendarEvent>> eventsByDate = groupEventsByDate(events);
        List<CalendarDayCell> cells = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            LocalDate date = gridStart.plusDays(i);
            CalendarDayCell cell = new CalendarDayCell(date, date.getMonth().equals(monthStart.getMonth()));
            List<CalendarEvent> dayEvents = eventsByDate.get(date);
            if (dayEvents != null) {
                cell.getEvents().addAll(dayEvents);
            }
            cells.add(cell);
        }
        return cells;
    }

    private void refreshWeek() {
        LocalDate weekStart = getWeekStart(selectedDate);
        List<LocalDate> days = buildWeekDays(weekStart);
        weekDayAdapter.submit(days, selectedDate);
        List<CalendarEvent> events = calendarRepository.getEventsBetween(weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay());
        weekHourAdapter.submit(days, events);
        weekTimelineRecycler.post(() -> weekTimelineRecycler.scrollToPosition(6));
    }

    private void refreshDay() {
        LocalDate weekStart = getWeekStart(selectedDate);
        List<LocalDate> days = buildWeekDays(weekStart);
        dayStripAdapter.submit(days, selectedDate);
        dayEventAdapter.submit(calendarRepository.getEventsForDate(selectedDate));
        dayTodoAdapter.submit(todoRepository.getTodosForDate(selectedDate));
    }

    private EventListAdapter.Listener eventActions() {
        return new EventListAdapter.Listener() {
            @Override
            public void onEdit(CalendarEvent event) {
                ((MainActivity) requireActivity()).openEventEditor(event.getId(), selectedDate);
            }

            @Override
            public void onDelete(CalendarEvent event) {
                UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_event), () -> {
                    calendarRepository.deleteEvent(event.getId());
                    refresh();
                });
            }
        };
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
                UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_event), () -> {
                    todoRepository.deleteTodo(item.getId());
                    refresh();
                });
            }
        };
    }

    private void onMonthDateClicked(LocalDate date) {
        selectedDate = date;
        visibleMonth = date.withDayOfMonth(1);
        monthDetailVisible = true;
        ((MainActivity) requireActivity()).setSelectedDate(date);
        refresh();
    }

    private void syncMonthPager(boolean smooth) {
        if (monthPager == null || monthPagerAdapter == null) {
            return;
        }
        int targetPosition = monthPagerAdapter.getPositionForMonth(visibleMonth);
        if (getCurrentMonthPagerPosition() == targetPosition) {
            return;
        }
        if (smooth) {
            monthPager.smoothScrollToPosition(targetPosition);
        } else if (monthPager.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
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
        visibleMonth = newMonth;
        selectedDate = visibleMonth.withDayOfMonth(Math.min(selectedDate.getDayOfMonth(), visibleMonth.lengthOfMonth()));
        monthDetailVisible = false;
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        updateHeader();
        refreshMonth();
    }

    private void resizeMonthDetailPanel() {
        if (monthBody == null || dayDetailPanel == null || monthBody.getHeight() <= 0) {
            return;
        }
        ViewGroup.LayoutParams params = dayDetailPanel.getLayoutParams();
        int targetHeight = monthBody.getHeight() / 2;
        if (params.height != targetHeight) {
            params.height = targetHeight;
            dayDetailPanel.setLayoutParams(params);
        }
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
                if (mode == MODE_MONTH || e1 == null || e2 == null || Math.abs(velocityX) < Math.abs(velocityY)) {
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
        view.setOnTouchListener(listener);
        view.findViewById(R.id.week_timeline_recycler).setOnTouchListener(listener);
        view.findViewById(R.id.day_event_recycler).setOnTouchListener(listener);
    }
}
