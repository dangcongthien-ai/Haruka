package com.example.finalproject.ui.todo;

import android.os.Bundle;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.TodoRepository;
import com.example.finalproject.ui.calendar.EventEditFragment;
import com.example.finalproject.ui.calendar.ReminderFragment;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.ScreenBackHandler;
import com.example.finalproject.ui.common.UiUtils;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TodoEditFragment extends Fragment implements ScreenBackHandler {
    private static final String ARG_TODO_ID = "todo_id";
    private static final String ARG_DATE = "date";
    private static final String RESULT_REMINDER = "todo_reminder";
    private static final String RESULT_DATE = "todo_date";

    private TodoRepository repository;
    private long todoId;
    private boolean initialized;
    private String title = "";
    private String description = "";
    private LocalDate todoDate;
    private final List<Reminder> reminders = new ArrayList<>();
    private boolean completed;
    private int priority = 3;

    private EditText titleEdit;
    private EditText descriptionEdit;
    private TextView dateText;
    private LinearLayout reminderList;
    private View typeSegment;
    private MaterialButton eventTypeButton;
    private MaterialButton todoTypeButton;
    private MaterialButton deleteButton;
    private TextView priority1;
    private TextView priority2;
    private TextView priority3;
    private TextView priority4;

    public static TodoEditFragment newInstance(long todoId, LocalDate date) {
        TodoEditFragment fragment = new TodoEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TODO_ID, todoId);
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener(RESULT_REMINDER, this, (requestKey, result) -> {
            Reminder reminder = (Reminder) result.getSerializable(ReminderFragment.RESULT_REMINDER);
            if (reminder != null && !reminder.isNone()) {
                reminders.add(reminder);
            }
            bindValues();
        });
        getParentFragmentManager().setFragmentResultListener(RESULT_DATE, this, (requestKey, result) -> {
            LocalDate picked = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            if (picked != null) {
                todoDate = picked;
                bindValues();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo_edit, container, false);
        repository = new TodoRepository(requireContext());
        bind(view);
        if (!initialized) {
            initializeFields();
        }
        setupClicks(view);
        bindValues();
        return view;
    }

    private void bind(View view) {
        titleEdit = view.findViewById(R.id.edit_todo_title);
        descriptionEdit = view.findViewById(R.id.edit_todo_description);
        dateText = view.findViewById(R.id.tv_todo_date);
        reminderList = view.findViewById(R.id.reminder_list);
        typeSegment = view.findViewById(R.id.type_segment);
        eventTypeButton = view.findViewById(R.id.btn_event_type);
        todoTypeButton = view.findViewById(R.id.btn_todo_type);
        deleteButton = view.findViewById(R.id.btn_delete_todo);
        priority1 = view.findViewById(R.id.priority_1);
        priority2 = view.findViewById(R.id.priority_2);
        priority3 = view.findViewById(R.id.priority_3);
        priority4 = view.findViewById(R.id.priority_4);
    }

    private void initializeFields() {
        todoId = requireArguments().getLong(ARG_TODO_ID);
        todoDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (todoDate == null) {
            todoDate = LocalDate.now();
        }
        if (todoId > 0) {
            TodoItem item = repository.getTodo(todoId);
            if (item != null) {
                title = item.getTitle();
                description = item.getDescription() == null ? "" : item.getDescription();
                todoDate = item.getTodoDate();
                reminders.clear();
                reminders.addAll(item.getReminders());
                completed = item.isCompleted();
                priority = item.getPriorityQuadrant();
            }
        }
        initialized = true;
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> ((MainActivity) requireActivity()).handleActivityBackPressed());
        view.findViewById(R.id.btn_save_todo).setOnClickListener(v -> save());
        view.findViewById(R.id.todo_date_row).setOnClickListener(v -> {
            captureInput();
            DatePickerDialogFragment
                    .newInstance(RESULT_DATE, todoDate)
                    .show(getParentFragmentManager(), RESULT_DATE);
        });
        view.findViewById(R.id.reminder_section).setOnClickListener(v -> openReminderPicker());
        view.findViewById(R.id.btn_add_reminder).setOnClickListener(v -> openReminderPicker());
        eventTypeButton.setOnClickListener(v -> {
            captureInput();
            ((MainActivity) requireActivity()).switchFullScreen(EventEditFragment.newInstance(0, todoDate));
        });
        priority1.setOnClickListener(v -> setPriority(1));
        priority2.setOnClickListener(v -> setPriority(2));
        priority3.setOnClickListener(v -> setPriority(3));
        priority4.setOnClickListener(v -> setPriority(4));
        deleteButton.setOnClickListener(v -> {
            UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_todo), () -> {
                if (todoId > 0) {
                    repository.deleteTodo(todoId);
                }
                ((MainActivity) requireActivity()).finishFullScreenOrHome();
            });
        });
    }

    @Override
    public boolean onHandleBackPressed() {
        ((MainActivity) requireActivity()).finishFullScreenOrHome();
        return true;
    }

    private void openReminderPicker() {
        captureInput();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ReminderFragment.newInstance(RESULT_REMINDER, Reminder.none()))
                .addToBackStack("Reminder")
                .commit();
    }

    private void bindValues() {
        if (titleEdit == null) {
            return;
        }
        titleEdit.setText(title);
        descriptionEdit.setText(description);
        dateText.setText(DateTimeUtils.formatDateWithDow(todoDate));
        renderReminders();
        typeSegment.setVisibility(todoId > 0 ? View.GONE : View.VISIBLE);
        deleteButton.setVisibility(todoId > 0 ? View.VISIBLE : View.GONE);
        UiUtils.selectSegment(requireContext(), todoTypeButton, eventTypeButton);
        refreshPriority();
    }

    private void renderReminders() {
        reminderList.removeAllViews();
        TextView row = reminderRow(reminderSummary());
        row.setTextColor(requireContext().getColor(reminders.isEmpty() ? R.color.text_muted : R.color.text_primary));
        reminderList.addView(row);
    }

    private String reminderSummary() {
        if (reminders.isEmpty()) {
            return getString(R.string.none);
        }
        if (reminders.size() == 1) {
            return reminders.get(0).getDisplayText();
        }
        return reminders.get(0).getDisplayText() + " +" + (reminders.size() - 1);
    }

    private TextView reminderRow(String text) {
        TextView row = new TextView(requireContext());
        row.setText(text);
        row.setTextSize(14);
        row.setTextColor(requireContext().getColor(R.color.text_primary));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
        row.setSingleLine(true);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return row;
    }

    private void captureInput() {
        title = titleEdit.getText().toString().trim();
        description = descriptionEdit.getText().toString().trim();
    }

    private void setPriority(int priority) {
        this.priority = priority;
        refreshPriority();
    }

    private void refreshPriority() {
        stylePriority(priority1, priority == 1);
        stylePriority(priority2, priority == 2);
        stylePriority(priority3, priority == 3);
        stylePriority(priority4, priority == 4);
    }

    private void stylePriority(TextView view, boolean selected) {
        view.setBackground(selected
                ? UiUtils.roundedStroke(Color.TRANSPARENT, requireContext().getColor(R.color.brand_orange), 12, requireContext())
                : null);
    }

    private void save() {
        captureInput();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_title_error, Toast.LENGTH_SHORT).show();
            return;
        }
        TodoItem item = new TodoItem();
        item.setId(todoId);
        item.setTitle(title);
        item.setDescription(description);
        item.setTodoDate(todoDate);
        item.setCompleted(completed);
        item.setPriorityQuadrant(priority);
        item.setColor(colorForPriority(priority));
        item.setReminders(reminders);
        repository.saveTodo(item);
        ((MainActivity) requireActivity()).setSelectedDate(todoDate);
        ((MainActivity) requireActivity()).finishFullScreenOrHome();
    }

    private String colorForPriority(int priority) {
        if (priority == 1) {
            return "#FF0404";
        }
        if (priority == 2) {
            return "#EB9B54";
        }
        if (priority == 3) {
            return "#0D42B3";
        }
        return "#B3B3B3";
    }
}
