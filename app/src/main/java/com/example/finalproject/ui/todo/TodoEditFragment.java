package com.example.finalproject.ui.todo;

import android.os.Bundle;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private String initialStateSignature = "";
    private String title = "";
    private String description = "";
    private LocalDate todoDate;
    private final List<Reminder> reminders = new ArrayList<>();
    private boolean completed;
    private int priority = 3;

    private EditText titleEdit;
    private EditText descriptionEdit;
    private View rootView;
    private ScrollView contentScroll;
    private View titleRow;
    private View descriptionRow;
    private View keyboardSpacer;
    private TextView dateText;
    private TextView reminderValueText;
    private LinearLayout reminderList;
    private View typeSegment;
    private MaterialButton eventTypeButton;
    private MaterialButton todoTypeButton;
    private MaterialButton deleteButton;
    private TextView addReminderButton;
    private TodoPriorityMatrixController priorityMatrixController;
    private Integer previousSoftInputMode;
    private int baseKeyboardSpacerHeight;
    private int keyboardInset;
    private boolean keyboardVisible;
    @Nullable
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
    @Nullable
    private Runnable pendingKeyboardScrollRunnable;
    @Nullable
    private View pendingKeyboardScrollAnchor;

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
            if (reminder == null || reminder.isNone()) {
                reminders.clear();
            } else {
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
        if (getActivity() != null && previousSoftInputMode == null) {
            previousSoftInputMode = requireActivity().getWindow().getAttributes().softInputMode;
        }
        bind(view);
        installKeyboardScrollSupport();
        if (!initialized) {
            initializeFields();
        }
        setupClicks(view);
        bindValues();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        enableKeyboardInputMode();
    }

    @Override
    public void onPause() {
        restoreSoftInputMode();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        detachKeyboardScrollSupport();
        restoreSoftInputMode();
        super.onDestroyView();
    }

    private void bind(View view) {
        rootView = view.findViewById(R.id.todo_edit_root);
        contentScroll = view.findViewById(R.id.todo_edit_scroll);
        titleRow = view.findViewById(R.id.title_row);
        descriptionRow = view.findViewById(R.id.description_row);
        keyboardSpacer = view.findViewById(R.id.todo_edit_keyboard_spacer);
        titleEdit = view.findViewById(R.id.edit_todo_title);
        descriptionEdit = view.findViewById(R.id.edit_todo_description);
        dateText = view.findViewById(R.id.tv_todo_date);
        reminderValueText = view.findViewById(R.id.tv_reminder_value);
        reminderList = view.findViewById(R.id.reminder_list);
        typeSegment = view.findViewById(R.id.type_segment);
        eventTypeButton = view.findViewById(R.id.btn_event_type);
        todoTypeButton = view.findViewById(R.id.btn_todo_type);
        deleteButton = view.findViewById(R.id.btn_delete_todo);
        addReminderButton = view.findViewById(R.id.btn_add_reminder);
        priorityMatrixController = new TodoPriorityMatrixController(view.findViewById(R.id.priority_matrix));
        priorityMatrixController.setOnPrioritySelectedListener(this::setPriority);
    }

    private void installKeyboardScrollSupport() {
        if (rootView == null || keyboardLayoutListener != null) {
            return;
        }
        baseKeyboardSpacerHeight = keyboardSpacer != null && keyboardSpacer.getLayoutParams() != null
                ? keyboardSpacer.getLayoutParams().height
                : UiUtils.dp(requireContext(), 28);
        keyboardLayoutListener = () -> {
            if (!isAdded() || rootView == null) {
                return;
            }
            Rect visibleFrame = new Rect();
            rootView.getWindowVisibleDisplayFrame(visibleFrame);
            int fullHeight = rootView.getRootView().getHeight();
            int overlap = Math.max(0, fullHeight - visibleFrame.bottom);
            boolean nowVisible = overlap > UiUtils.dp(requireContext(), 120);
            int desiredInset = nowVisible ? overlap : 0;
            boolean visibilityChanged = nowVisible != keyboardVisible;
            if (desiredInset == keyboardInset && nowVisible == keyboardVisible) {
                return;
            }
            keyboardInset = desiredInset;
            keyboardVisible = nowVisible;
            int spacerHeight = baseKeyboardSpacerHeight
                    + (keyboardVisible ? Math.max(UiUtils.dp(requireContext(), 12), keyboardInset - UiUtils.dp(requireContext(), 28)) : 0);
            updateKeyboardSpacerHeight(spacerHeight);
            if (keyboardVisible) {
                View focusedAnchor = resolveFocusedAnchor();
                if (focusedAnchor != null && visibilityChanged) {
                    requestFieldVisibility(focusedAnchor, 0L, true);
                }
            } else {
                cancelPendingKeyboardScroll();
            }
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    private void detachKeyboardScrollSupport() {
        if (rootView == null || keyboardLayoutListener == null) {
            return;
        }
        ViewTreeObserver observer = rootView.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnGlobalLayoutListener(keyboardLayoutListener);
        }
        cancelPendingKeyboardScroll();
        keyboardLayoutListener = null;
    }

    private void enableKeyboardInputMode() {
        if (getActivity() == null) {
            return;
        }
        requireActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );
    }

    private void restoreSoftInputMode() {
        if (getActivity() == null || previousSoftInputMode == null) {
            return;
        }
        requireActivity().getWindow().setSoftInputMode(previousSoftInputMode);
    }

    private void updateKeyboardSpacerHeight(int height) {
        if (keyboardSpacer == null) {
            return;
        }
        ViewGroup.LayoutParams params = keyboardSpacer.getLayoutParams();
        if (params == null || params.height == height) {
            return;
        }
        params.height = height;
        keyboardSpacer.setLayoutParams(params);
    }

    private View resolveFocusedAnchor() {
        if (isFieldFocused(descriptionEdit)) {
            return descriptionEdit;
        }
        if (isFieldFocused(titleEdit)) {
            return titleEdit;
        }
        return null;
    }

    private boolean isFieldFocused(@Nullable EditText field) {
        return field != null && field.hasFocus();
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
        initialStateSignature = buildStateSignature();
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
        view.findViewById(R.id.reminder_header_row).setOnClickListener(v -> openReminderPicker());
        addReminderButton.setOnClickListener(v -> openReminderPicker());
        eventTypeButton.setOnClickListener(v -> {
            captureInput();
            ((MainActivity) requireActivity()).switchFullScreen(EventEditFragment.newInstance(0, todoDate));
        });
        attachKeyboardFieldBehavior(titleEdit, titleEdit, 60L);
        attachKeyboardFieldBehavior(descriptionEdit, descriptionEdit, 100L);
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
        captureInput();
        if (!hasUnsavedChanges()) {
            ((MainActivity) requireActivity()).finishFullScreenOrHome();
            return true;
        }
        UiUtils.showConfirmationDialog(
                requireContext(),
                R.drawable.ic_close_centered,
                getString(R.string.discard_changes_title),
                getString(R.string.discard_changes_message),
                getString(R.string.discard),
                () -> ((MainActivity) requireActivity()).finishFullScreenOrHome()
        );
        return true;
    }

    private void openReminderPicker() {
        captureInput();
        ((MainActivity) requireActivity()).pushFullScreenFragment(
                ReminderFragment.newInstance(RESULT_REMINDER, Reminder.none()),
                "Reminder"
        );
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
        priorityMatrixController.setInteractive(true);
        priorityMatrixController.setPriority(priority);
    }

    private void renderReminders() {
        reminderList.removeAllViews();
        if (reminders.isEmpty()) {
            reminderList.setVisibility(View.GONE);
            addReminderButton.setVisibility(View.GONE);
            reminderValueText.setText(R.string.none);
            reminderValueText.setTextColor(requireContext().getColor(R.color.text_muted));
            return;
        }
        reminderList.setVisibility(View.VISIBLE);
        addReminderButton.setVisibility(View.VISIBLE);
        reminderValueText.setText(getString(R.string.reminder_count_value, reminders.size()));
        reminderValueText.setTextColor(requireContext().getColor(R.color.text_secondary));
        for (int index = 0; index < reminders.size(); index++) {
            reminderList.addView(createReminderItem(reminders.get(index), index));
            if (index < reminders.size() - 1) {
                reminderList.addView(createReminderDivider());
            }
        }
    }

    private View createReminderItem(Reminder reminder, int index) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(UiUtils.dp(requireContext(), 40));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView label = new TextView(requireContext());
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        label.setText(reminder.getDisplayText());
        label.setTextColor(requireContext().getColor(R.color.text_primary));
        label.setTextSize(14f);
        label.setSingleLine(true);

        ImageButton remove = new ImageButton(requireContext());
        remove.setLayoutParams(new LinearLayout.LayoutParams(UiUtils.dp(requireContext(), 32), UiUtils.dp(requireContext(), 32)));
        remove.setBackgroundResource(android.R.color.transparent);
        remove.setContentDescription(getString(R.string.remove_reminder));
        remove.setImageResource(R.drawable.ic_close);
        remove.setColorFilter(requireContext().getColor(R.color.text_secondary));
        remove.setOnClickListener(v -> {
            reminders.remove(index);
            bindValues();
        });

        row.addView(label);
        row.addView(remove);
        return row;
    }

    private View createReminderDivider() {
        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        ));
        divider.setBackgroundColor(requireContext().getColor(R.color.line));
        return divider;
    }

    private void captureInput() {
        title = titleEdit.getText().toString().trim();
        description = descriptionEdit.getText().toString().trim();
    }

    private boolean hasUnsavedChanges() {
        return !buildStateSignature().equals(initialStateSignature);
    }

    private String buildStateSignature() {
        StringBuilder builder = new StringBuilder();
        builder.append(title == null ? "" : title).append('|')
                .append(description == null ? "" : description).append('|')
                .append(DateTimeUtils.dateToIso(todoDate)).append('|')
                .append(priority).append('|')
                .append(completed).append('|');
        for (Reminder reminder : reminders) {
            if (reminder == null) {
                continue;
            }
            builder.append(reminder.getDisplayText()).append(';');
        }
        return builder.toString();
    }

    private void setPriority(int priority) {
        this.priority = priority;
        if (priorityMatrixController != null && priorityMatrixController.getPriority() != priority) {
            priorityMatrixController.setPriority(priority);
        }
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
        initialStateSignature = buildStateSignature();
        ((MainActivity) requireActivity()).setSelectedDate(todoDate);
        ((MainActivity) requireActivity()).finishFullScreenOrHome();
    }

    private String colorForPriority(int priority) {
        if (priority == 1) {
            return "#FFE3E1";
        }
        if (priority == 2) {
            return "#FFEBD7";
        }
        if (priority == 3) {
            return "#E2F0FF";
        }
        return "#F0F0F0";
    }

    private void attachKeyboardFieldBehavior(EditText field, View anchor, long focusDelayMs) {
        if (field == null || anchor == null) {
            return;
        }
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                return;
            }
            if (keyboardVisible) {
                requestFieldVisibility(anchor, focusDelayMs, true);
            }
        });
        field.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (keyboardVisible && field.hasFocus()) {
                    requestFieldVisibility(anchor, 0L, false);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    private void requestFieldVisibility(View anchor, long delayMs, boolean animated) {
        if (contentScroll == null || anchor == null || !isAdded()) {
            return;
        }
        cancelPendingKeyboardScroll();
        pendingKeyboardScrollAnchor = anchor;
        pendingKeyboardScrollRunnable = () -> {
            if (!isAdded() || !keyboardVisible || anchor != resolveFocusedAnchor()) {
                return;
            }
            int[] location = new int[2];
            anchor.getLocationOnScreen(location);
            int anchorBottomOnScreen = resolveAnchorBottomOnScreen(anchor, location[1]);
            int desiredBottom = rootView.getHeight() - Math.max(UiUtils.dp(requireContext(), 18), keyboardInset - UiUtils.dp(requireContext(), 8));
            if (anchorBottomOnScreen <= desiredBottom) {
                return;
            }
            int delta = anchorBottomOnScreen - desiredBottom;
            if (anchor == descriptionEdit && descriptionEdit != null) {
                delta += Math.max(UiUtils.dp(requireContext(), 20), descriptionEdit.getLineHeight() * 2);
            }
            int targetScroll = Math.max(0, contentScroll.getScrollY() + delta);
            if (animated) {
                contentScroll.smoothScrollTo(0, targetScroll);
            } else {
                contentScroll.scrollTo(0, targetScroll);
            }
        };
        if (delayMs > 0L) {
            anchor.postDelayed(pendingKeyboardScrollRunnable, delayMs);
        } else {
            anchor.post(pendingKeyboardScrollRunnable);
        }
    }

    private int resolveAnchorBottomOnScreen(View anchor, int anchorTopOnScreen) {
        if (!(anchor instanceof EditText)) {
            return anchorTopOnScreen + anchor.getHeight();
        }
        EditText editText = (EditText) anchor;
        android.text.Layout layout = editText.getLayout();
        if (layout == null) {
            return anchorTopOnScreen + anchor.getHeight();
        }
        int line = Math.max(0, editText.getSelectionEnd() >= 0 ? layout.getLineForOffset(editText.getSelectionEnd()) : layout.getLineCount() - 1);
        int cursorBottom = anchorTopOnScreen
                + editText.getTotalPaddingTop()
                + layout.getLineBottom(line)
                + editText.getTotalPaddingBottom();
        int minimumVisibleBottom = anchorTopOnScreen + Math.min(editText.getHeight(), UiUtils.dp(requireContext(), 44));
        return Math.max(cursorBottom, minimumVisibleBottom);
    }

    private void cancelPendingKeyboardScroll() {
        if (pendingKeyboardScrollAnchor != null && pendingKeyboardScrollRunnable != null) {
            pendingKeyboardScrollAnchor.removeCallbacks(pendingKeyboardScrollRunnable);
        }
        pendingKeyboardScrollAnchor = null;
        pendingKeyboardScrollRunnable = null;
    }
}
