package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.adapter.HabitAdapter;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.HabitItem;
import com.example.finalproject.model.HabitListItem;
import com.example.finalproject.model.HabitLog;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.ui.common.HomeDataRefreshable;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.util.HabitScheduleUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HabitFragment extends Fragment implements HomeDataRefreshable {
    private static final String ARG_DATE = "date";
    private static final String DATE_RESULT_KEY = "habit_date_result";
    private static final int SORT_PRIORITY = 0;
    private static final int SORT_TITLE = 1;
    private static final int SORT_CATEGORY = 2;

    private HabitRepository repository;
    private HabitAdapter adapter;
    private LocalDate selectedDate;
    private boolean archivedTab;
    private int sortMode = SORT_PRIORITY;

    private TextView dateLabel;
    private TextView activeButton;
    private TextView archivedButton;
    private TextView emptyLabel;
    private TextView emptyHint;
    private TextView summaryLabel;
    private TextView summaryPercent;
    private RecyclerView recyclerView;
    private View emptyView;
    private View summaryLayout;
    private View progressTrack;
    private View progressFill;
    private ImageButton nextDayButton;

    public static HabitFragment newInstance(LocalDate date) {
        HabitFragment fragment = new HabitFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit, container, false);
        repository = new HabitRepository(requireContext());
        selectedDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (selectedDate == null) {
            selectedDate = ((MainActivity) requireActivity()).getSelectedDate();
        }
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }
        selectedDate = clampFutureDate(selectedDate);
        bind(view);
        setupAdapter();
        setupClicks(view);
        setupDateResult();
        refresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onHomeDataRefresh(LocalDate selectedDate) {
        if (selectedDate != null) {
            this.selectedDate = clampFutureDate(selectedDate);
        }
        refresh();
    }

    private void bind(View view) {
        dateLabel = view.findViewById(R.id.tv_habit_date);
        activeButton = view.findViewById(R.id.btn_habit_active);
        archivedButton = view.findViewById(R.id.btn_habit_archived);
        emptyView = view.findViewById(R.id.layout_habit_empty);
        emptyLabel = view.findViewById(R.id.tv_habit_empty);
        emptyHint = view.findViewById(R.id.tv_habit_empty_hint);
        summaryLabel = view.findViewById(R.id.tv_habit_summary_label);
        summaryPercent = view.findViewById(R.id.tv_habit_summary_percent);
        recyclerView = view.findViewById(R.id.recycler_habits);
        summaryLayout = view.findViewById(R.id.layout_habit_summary);
        progressTrack = view.findViewById(R.id.habit_summary_progress_track);
        progressFill = view.findViewById(R.id.habit_summary_progress_fill);
    }

    private void setupAdapter() {
        adapter = new HabitAdapter(new HabitAdapter.Listener() {
            @Override
            public void onClick(HabitListItem item) {
                showHabitDetail(item);
            }

            @Override
            public void onAction(HabitListItem item) {
                if (blockFutureAction()) {
                    return;
                }
                handleQuickAction(item);
            }

            @Override
            public void onEdit(HabitListItem item) {
                if (blockFutureAction()) {
                    return;
                }
                ((MainActivity) requireActivity()).openHabitEditor(item.getHabit().getId());
            }

            @Override
            public void onDelete(HabitListItem item) {
                if (blockFutureAction()) {
                    return;
                }
                deleteHabitWithConfirmation(item.getHabit().getId());
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupClicks(View view) {
        ImageButton previous = view.findViewById(R.id.btn_habit_prev_day);
        nextDayButton = view.findViewById(R.id.btn_habit_next_day);
        previous.setOnClickListener(v -> moveDay(-1));
        nextDayButton.setOnClickListener(v -> moveDay(1));
        dateLabel.setOnClickListener(v -> DatePickerDialogFragment
                .newInstance(DATE_RESULT_KEY, selectedDate)
                .show(getParentFragmentManager(), DATE_RESULT_KEY));
        activeButton.setOnClickListener(v -> {
            archivedTab = false;
            refresh();
        });
        archivedButton.setOnClickListener(v -> {
            archivedTab = true;
            refresh();
        });
        view.findViewById(R.id.btn_habit_report).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openHabitStats(selectedDate));
        view.findViewById(R.id.btn_habit_sort).setOnClickListener(this::showSortMenu);
    }

    private void setupDateResult() {
        getParentFragmentManager().setFragmentResultListener(DATE_RESULT_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            LocalDate picked = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            if (picked != null) {
                selectedDate = clampFutureDate(picked);
                ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
                refresh();
            }
        });
    }

    private void moveDay(int amount) {
        LocalDate nextDate = selectedDate.plusDays(amount);
        if (nextDate.isAfter(LocalDate.now())) {
            Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        selectedDate = nextDate;
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        refresh();
    }

    private void refresh() {
        if (!isAdded()) {
            return;
        }
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        dateLabel.setText(DateTimeUtils.formatVietnameseDate(selectedDate));
        updateDateNavigation();
        updateFilterButtons();

        List<HabitItem> habits = repository.getHabits();
        Map<Long, HabitLog> logsByHabit = logsByHabit(selectedDate);
        List<HabitListItem> allTodayItems = buildDayItems(habits, logsByHabit);
        List<HabitListItem> visibleItems = filterVisibleItems(allTodayItems);
        adapter.submit(visibleItems);

        boolean empty = visibleItems.isEmpty();
        UiUtils.visible(emptyView, empty);
        UiUtils.visible(recyclerView, !empty);
        if (archivedTab) {
            emptyLabel.setText(R.string.habit_empty_archived);
            emptyHint.setText(R.string.habit_empty_archived_hint);
        } else {
            emptyLabel.setText(R.string.habit_empty);
            emptyHint.setText(R.string.habit_empty_hint);
        }
        UiUtils.visible(summaryLayout, !archivedTab);
        updateSummary(allTodayItems, visibleItems);
    }

    private List<HabitListItem> buildDayItems(List<HabitItem> habits, Map<Long, HabitLog> logsByHabit) {
        List<HabitListItem> items = new ArrayList<>();
        for (HabitItem habit : habits) {
            boolean permanentlyArchived = HabitScheduleUtils.isArchivedOnDate(habit, selectedDate);
            if (!permanentlyArchived && !HabitScheduleUtils.isDueOnDate(habit, selectedDate)) {
                continue;
            }
            if (habit.getStartDate() != null && habit.getStartDate().isAfter(selectedDate)) {
                continue;
            }
            HabitLog log = logsByHabit.get(habit.getId());
            boolean completed = HabitScheduleUtils.evaluateCompletion(habit, log);
            boolean archivedByCompletion = completed;
            boolean archived = permanentlyArchived || archivedByCompletion;

            HabitListItem item = new HabitListItem();
            item.setHabit(habit);
            item.setLog(log);
            item.setArchived(archived);
            item.setArchivedByCompletion(archivedByCompletion);
            item.setDueOnSelectedDate(!permanentlyArchived && HabitScheduleUtils.isDueOnDate(habit, selectedDate));
            item.setCompletedOnSelectedDate(completed);
            item.setProgressLabel(progressLabelFor(habit, log, completed, archived));
            item.setActionLabel(actionLabelFor(habit, completed));
            item.setProgressFraction(HabitScheduleUtils.progressFraction(habit, log));
            items.add(item);
        }
        Collections.sort(items, habitComparator());
        return items;
    }

    private List<HabitListItem> filterVisibleItems(List<HabitListItem> items) {
        List<HabitListItem> visibleItems = new ArrayList<>();
        for (HabitListItem item : items) {
            if (archivedTab == item.isArchived()) {
                visibleItems.add(item);
            }
        }
        Collections.sort(visibleItems, habitComparator());
        return visibleItems;
    }

    private Comparator<HabitListItem> habitComparator() {
        return (left, right) -> {
            if (sortMode == SORT_TITLE) {
                return safeText(left.getHabit().getTitle()).compareToIgnoreCase(safeText(right.getHabit().getTitle()));
            }
            String leftCategory = safeText(left.getHabit().getCategory() == null ? "" : left.getHabit().getCategory().getName());
            String rightCategory = safeText(right.getHabit().getCategory() == null ? "" : right.getHabit().getCategory().getName());
            if (sortMode == SORT_CATEGORY) {
                int byCategory = leftCategory.compareToIgnoreCase(rightCategory);
                if (byCategory != 0) {
                    return byCategory;
                }
                return safeText(left.getHabit().getTitle()).compareToIgnoreCase(safeText(right.getHabit().getTitle()));
            }
            int byPriority = priorityOrder(left.getHabit()) - priorityOrder(right.getHabit());
            if (byPriority != 0) {
                return byPriority;
            }
            int byCategory = leftCategory.compareToIgnoreCase(rightCategory);
            if (byCategory != 0) {
                return byCategory;
            }
            return safeText(left.getHabit().getTitle()).compareToIgnoreCase(safeText(right.getHabit().getTitle()));
        };
    }

    private void showSortMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(
                new ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Light),
                anchor
        );
        popupMenu.getMenu().add(Menu.NONE, SORT_PRIORITY, Menu.NONE, getString(R.string.habit_sort_priority));
        popupMenu.getMenu().add(Menu.NONE, SORT_TITLE, Menu.NONE, getString(R.string.habit_sort_title));
        popupMenu.getMenu().add(Menu.NONE, SORT_CATEGORY, Menu.NONE, getString(R.string.habit_sort_category));
        MenuItem selectedItem = popupMenu.getMenu().findItem(sortMode);
        if (selectedItem != null) {
            selectedItem.setChecked(true);
        }
        popupMenu.getMenu().setGroupCheckable(Menu.NONE, true, true);
        popupMenu.setOnMenuItemClickListener(item -> {
            sortMode = item.getItemId();
            refresh();
            return true;
        });
        popupMenu.show();
    }

    private int priorityOrder(HabitItem habit) {
        return habit.getPriority() == null ? Integer.MAX_VALUE : habit.getPriority().getPriorityOrder();
    }

    private Map<Long, HabitLog> logsByHabit(LocalDate date) {
        Map<Long, HabitLog> map = new HashMap<>();
        for (HabitLog log : repository.getHabitLogsInRange(date, date)) {
            map.put(log.getHabitId(), log);
        }
        return map;
    }

    private void updateFilterButtons() {
        activeButton.setBackgroundResource(archivedTab
                ? R.drawable.bg_habit_segment
                : R.drawable.bg_habit_segment_selected);
        archivedButton.setBackgroundResource(archivedTab
                ? R.drawable.bg_habit_segment_selected
                : R.drawable.bg_habit_segment);
        activeButton.setTextColor(requireContext().getColor(archivedTab ? R.color.text_primary : R.color.brand_orange));
        archivedButton.setTextColor(requireContext().getColor(archivedTab ? R.color.brand_orange : R.color.text_primary));
    }

    private void updateSummary(List<HabitListItem> allTodayItems, List<HabitListItem> visibleItems) {
        if (archivedTab) {
            summaryLabel.setText(getString(R.string.habit_archived_count, visibleItems.size()));
            summaryPercent.setText(visibleItems.isEmpty() ? "0%" : "100%");
            updateProgressBar(visibleItems.isEmpty() ? 0 : 100);
            return;
        }
        int total = 0;
        int completed = 0;
        for (HabitListItem item : allTodayItems) {
            if (HabitScheduleUtils.isArchivedOnDate(item.getHabit(), selectedDate)) {
                continue;
            }
            total++;
            if (item.isCompletedOnSelectedDate()) {
                completed++;
            }
        }
        int percent = total == 0 ? 0 : Math.round(completed * 100f / total);
        summaryLabel.setText(getString(R.string.habit_today_progress_count, completed, total));
        summaryPercent.setText(String.format(Locale.US, "%d%%", percent));
        updateProgressBar(percent);
    }

    private void updateProgressBar(int percent) {
        progressTrack.post(() -> {
            int width = progressTrack.getWidth();
            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
            params.width = Math.round(width * (Math.max(0, Math.min(percent, 100)) / 100f));
            progressFill.setLayoutParams(params);
        });
    }

    private void handleQuickAction(HabitListItem item) {
        HabitItem habit = item.getHabit();
        if (habit.isNumberEvaluation()) {
            showTargetUpdateSheet(habit);
            return;
        }
        boolean nextCompleted = !item.isCompletedOnSelectedDate();
        if (nextCompleted) {
            repository.saveHabitLog(habit.getId(), selectedDate, true, null);
        } else {
            repository.deleteHabitLog(habit.getId(), selectedDate);
        }
        refresh();
    }

    private void showHabitDetail(HabitListItem listItem) {
        HabitItem habit = repository.getHabit(listItem.getHabit().getId());
        if (habit == null) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View detailView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_habit_detail, null, false);
        dialog.setContentView(detailView);
        HabitUiHelper.styleSheetDialog(dialog);
        bindHabitDetail(detailView, habit);
        detailView.findViewById(R.id.btn_habit_detail_edit).setOnClickListener(v -> {
            if (blockFutureAction()) {
                return;
            }
            dialog.dismiss();
            ((MainActivity) requireActivity()).openHabitEditor(habit.getId());
        });
        detailView.findViewById(R.id.btn_habit_detail_delete).setOnClickListener(v -> {
            if (blockFutureAction()) {
                return;
            }
            dialog.dismiss();
            deleteHabitWithConfirmation(habit.getId());
        });
        dialog.show();
    }

    private void bindHabitDetail(View detailView, HabitItem habit) {
        detailView.findViewById(R.id.view_habit_detail_color).setBackground(UiUtils.rounded(
                UiUtils.safeColor(habit.getColor(), requireContext().getColor(R.color.brand_orange_light)),
                7,
                requireContext()
        ));
        ((TextView) detailView.findViewById(R.id.tv_habit_detail_title)).setText(habit.getTitle());
        ((TextView) detailView.findViewById(R.id.tv_habit_detail_category)).setText(
                habit.getCategory() == null ? getString(R.string.habit_category_other) : habit.getCategory().getName()
        );
        ((TextView) detailView.findViewById(R.id.tv_habit_detail_start)).setText(DateTimeUtils.formatVietnameseDate(habit.getStartDate()));
        ((TextView) detailView.findViewById(R.id.tv_habit_detail_end)).setText(
                habit.hasEndDate() && habit.getEndDate() != null
                        ? DateTimeUtils.formatVietnameseDate(habit.getEndDate())
                        : getString(R.string.none)
        );
        TextView priority = detailView.findViewById(R.id.tv_habit_detail_priority);
        if (habit.getPriority() != null) {
            HabitUiHelper.stylePriorityChip(priority, habit.getPriority().getName(), habit.getPriority().getColor());
        } else {
            HabitUiHelper.stylePriorityChip(priority, getString(R.string.none), "#F0E2D6");
        }
        ((TextView) detailView.findViewById(R.id.tv_habit_detail_repeat)).setText(
                habit.getRecurrenceRule() == null
                        ? getString(R.string.habit_repeat_none)
                        : habit.getRecurrenceRule().getDisplayText()
        );
        ((TextView) detailView.findViewById(R.id.tv_habit_detail_evaluation)).setText(
                habit.isNumberEvaluation()
                        ? getString(R.string.habit_evaluation_number)
                        : getString(R.string.habit_evaluation_boolean)
        );

        View targetTypeRow = detailView.findViewById(R.id.layout_habit_detail_target_type);
        View targetValueRow = detailView.findViewById(R.id.layout_habit_detail_target_value);
        View targetUnitRow = detailView.findViewById(R.id.layout_habit_detail_target_unit);
        if (habit.isNumberEvaluation()) {
            targetTypeRow.setVisibility(View.VISIBLE);
            targetValueRow.setVisibility(View.VISIBLE);
            targetUnitRow.setVisibility(View.VISIBLE);
            ((TextView) detailView.findViewById(R.id.tv_habit_detail_target_type)).setText(operatorLabel(habit.getTargetOperator()));
            ((TextView) detailView.findViewById(R.id.tv_habit_detail_target_value)).setText(HabitScheduleUtils.formatNumber(habit.getTargetValue()));
            ((TextView) detailView.findViewById(R.id.tv_habit_detail_target_unit)).setText(safeText(habit.getTargetUnit()));
        } else {
            targetTypeRow.setVisibility(View.GONE);
            targetValueRow.setVisibility(View.GONE);
            targetUnitRow.setVisibility(View.GONE);
        }

        ((TextView) detailView.findViewById(R.id.tv_habit_detail_description)).setText(
                TextUtils.isEmpty(habit.getDescription())
                        ? getString(R.string.habit_description_empty)
                        : habit.getDescription()
        );
    }

    private void showTargetUpdateSheet(HabitItem habit) {
        if (!habit.isNumberEvaluation()) {
            return;
        }
        if (blockFutureAction()) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View targetView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_habit_update_target, null, false);
        dialog.setContentView(targetView);
        HabitUiHelper.styleSheetDialog(dialog);

        HabitLog existingLog = repository.getHabitLog(habit.getId(), selectedDate);
        EditText valueEdit = targetView.findViewById(R.id.edit_habit_target_actual_value);
        EditText unitEdit = targetView.findViewById(R.id.edit_habit_target_actual_unit);
        TextView progressText = targetView.findViewById(R.id.tv_habit_target_progress);
        valueEdit.setText(existingLog == null || existingLog.getActualValue() == null
                ? ""
                : HabitScheduleUtils.formatNumber(existingLog.getActualValue()));
        unitEdit.setText(safeText(habit.getTargetUnit()));
        unitEdit.setFocusable(false);
        unitEdit.setFocusableInTouchMode(false);
        unitEdit.setInputType(InputType.TYPE_NULL);

        ((TextView) targetView.findViewById(R.id.tv_habit_target_operator)).setText(operatorLabel(habit.getTargetOperator()));
        ((TextView) targetView.findViewById(R.id.tv_habit_target_goal)).setText(targetDisplay(habit));
        updateTargetProgress(progressText, habit, parseDouble(valueEdit.getText().toString().trim()));
        valueEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateTargetProgress(progressText, habit, parseDouble(s.toString().trim()));
            }
        });

        targetView.findViewById(R.id.btn_habit_target_close).setOnClickListener(v -> dialog.dismiss());
        targetView.findViewById(R.id.btn_habit_target_save).setOnClickListener(v -> {
            Double actualValue = parseDouble(valueEdit.getText().toString().trim());
            if (actualValue == null) {
                valueEdit.setError(getString(R.string.habit_target_save_error));
                return;
            }
            HabitLog log = new HabitLog();
            log.setActualValue(actualValue);
            boolean completed = HabitScheduleUtils.evaluateCompletion(habit, log);
            repository.saveHabitLog(habit.getId(), selectedDate, completed, actualValue);
            dialog.dismiss();
            refresh();
        });
        dialog.show();
    }

    private void deleteHabitWithConfirmation(long habitId) {
        UiUtils.showDeleteDialog(requireContext(), getString(R.string.habit_delete_confirm), () -> {
            repository.deleteHabit(habitId);
            refresh();
        });
    }

    private String progressLabelFor(HabitItem habit, HabitLog log, boolean completed, boolean archived) {
        if (archived) {
            if (habit.hasEndDate() && habit.getEndDate() != null) {
                return getString(R.string.habit_archived_on, DateTimeUtils.formatVietnameseDate(habit.getEndDate()));
            }
            return getString(R.string.habit_archived_status);
        }
        if (!habit.isNumberEvaluation()) {
            return getString(completed ? R.string.habit_status_done : R.string.habit_status_pending);
        }
        if (log != null && log.getActualValue() != null) {
            return getString(
                    R.string.habit_progress_number_today,
                    HabitScheduleUtils.formatNumber(log.getActualValue()),
                    targetDisplay(habit),
                    operatorLabel(habit.getTargetOperator())
            );
        }
        return getString(R.string.habit_progress_target_only, targetDisplay(habit));
    }

    private String actionLabelFor(HabitItem habit, boolean completed) {
        if (habit.isNumberEvaluation()) {
            return getString(R.string.habit_action_update_target);
        }
        return getString(completed ? R.string.habit_action_unmark_done : R.string.habit_action_mark_done);
    }

    private String targetDisplay(HabitItem habit) {
        String targetValue = HabitScheduleUtils.formatNumber(habit.getTargetValue());
        String unit = safeText(habit.getTargetUnit());
        if (targetValue.isEmpty()) {
            return unit;
        }
        if (unit.isEmpty()) {
            return targetValue;
        }
        return targetValue + " " + unit;
    }

    private String operatorLabel(String operator) {
        if (HabitItem.OPERATOR_AT_MOST.equals(operator)) {
            return getString(R.string.habit_operator_at_most);
        }
        if (HabitItem.OPERATOR_EXACTLY.equals(operator)) {
            return getString(R.string.habit_operator_exactly);
        }
        return getString(R.string.habit_operator_at_least);
    }

    private void updateTargetProgress(TextView progressText, HabitItem habit, @Nullable Double actualValue) {
        if (progressText == null) {
            return;
        }
        String actual = HabitScheduleUtils.formatNumber(actualValue == null ? 0d : actualValue);
        String target = HabitScheduleUtils.formatNumber(habit == null ? null : habit.getTargetValue());
        progressText.setText(actual + "/" + (TextUtils.isEmpty(target) ? "0" : target));
    }

    private Double parseDouble(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private LocalDate clampFutureDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
            return today;
        }
        return date;
    }

    private void updateDateNavigation() {
        if (nextDayButton == null) {
            return;
        }
        boolean canMoveForward = selectedDate.isBefore(LocalDate.now());
        nextDayButton.setEnabled(canMoveForward);
        nextDayButton.setAlpha(canMoveForward ? 1f : 0.35f);
    }

    private boolean blockFutureAction() {
        if (!selectedDate.isAfter(LocalDate.now())) {
            return false;
        }
        Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
        return true;
    }
}
