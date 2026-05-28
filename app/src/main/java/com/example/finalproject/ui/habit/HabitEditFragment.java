package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.HabitCategory;
import com.example.finalproject.model.HabitItem;
import com.example.finalproject.model.HabitPriority;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.ui.calendar.RecurrenceFragment;
import com.example.finalproject.ui.calendar.ReminderFragment;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.util.HabitDefaults;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HabitEditFragment extends Fragment {
    private static final String ARG_HABIT_ID = "habit_id";
    private static final String ARG_DATE = "date";
    private static final String RESULT_START_DATE = "habit_start_date";
    private static final String RESULT_END_DATE = "habit_end_date";
    private static final String RESULT_RECURRENCE = "habit_recurrence";
    private static final String RESULT_REMINDER = "habit_reminder";

    private HabitRepository repository;
    private long habitId;
    private boolean initialized;

    private String title = "";
    private String description = "";
    private String selectedColor = HabitDefaults.COLOR_OPTIONS[0];
    private HabitCategory selectedCategory;
    private HabitPriority selectedPriority;
    private LocalDate startDate;
    private boolean hasEndDate;
    private LocalDate endDate;
    private RecurrenceRule recurrenceRule = RecurrenceRule.none();
    private final List<Reminder> reminders = new ArrayList<>();
    private String evaluationType = HabitItem.EVALUATION_BOOLEAN;
    private String targetOperator = "";
    private String targetValueText = "";
    private String targetUnit = "";
    private String initialSnapshot;
    private boolean suppressExitConfirmation;

    private EditText titleEdit;
    private NestedScrollView scrollView;
    private LinearLayout colorContainer;
    private ImageView categoryIcon;
    private TextView categoryText;
    private TextView startText;
    private TextView endText;
    private SwitchMaterial endSwitch;
    private View endPickerLayout;
    private View endRow;
    private TextView priorityText;
    private TextView repeatText;
    private TextView reminderText;
    private RadioGroup evaluationGroup;
    private RadioButton booleanRadio;
    private RadioButton numberRadio;
    private View numberFields;
    private View rootView;
    private View keyboardSpacer;
    private AutoCompleteTextView operatorInput;
    private LinearLayout customColorTrigger;
    private EditText targetValueEdit;
    private EditText targetUnitEdit;
    private EditText descriptionEdit;
    private Integer previousSoftInputMode;
    @Nullable
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
    @Nullable
    private Runnable pendingKeyboardScrollRunnable;
    @Nullable
    private View pendingKeyboardScrollAnchor;
    private int baseKeyboardSpacerHeight;
    private int keyboardInset;
    private boolean keyboardVisible;

    public static HabitEditFragment newInstance(long habitId, LocalDate date) {
        HabitEditFragment fragment = new HabitEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_HABIT_ID, habitId);
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener(RESULT_START_DATE, this, (requestKey, result) -> {
            LocalDate picked = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            if (picked != null) {
                startDate = picked;
                if (endDate != null && endDate.isBefore(startDate)) {
                    endDate = startDate;
                }
                bindValues();
            }
        });
        getParentFragmentManager().setFragmentResultListener(RESULT_END_DATE, this, (requestKey, result) -> {
            LocalDate picked = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            if (picked != null) {
                endDate = picked;
                hasEndDate = true;
                bindValues();
            }
        });
        getParentFragmentManager().setFragmentResultListener(RESULT_RECURRENCE, this, (requestKey, result) -> {
            RecurrenceRule picked = (RecurrenceRule) result.getSerializable(RecurrenceFragment.RESULT_RULE);
            recurrenceRule = picked == null ? RecurrenceRule.none() : picked;
            bindValues();
        });
        getParentFragmentManager().setFragmentResultListener(RESULT_REMINDER, this, (requestKey, result) -> {
            Reminder picked = (Reminder) result.getSerializable(ReminderFragment.RESULT_REMINDER);
            reminders.clear();
            if (picked != null && !picked.isNone()) {
                reminders.add(picked);
            }
            bindValues();
        });
        getParentFragmentManager().setFragmentResultListener(HabitCategoryPickerSheet.RESULT_KEY, this, (requestKey, result) -> {
            long categoryId = result.getLong(HabitCategoryPickerSheet.RESULT_CATEGORY_ID);
            selectedCategory = repository.getCategory(categoryId);
            bindValues();
        });
        getParentFragmentManager().setFragmentResultListener(HabitPrioritySheet.RESULT_KEY, this, (requestKey, result) -> {
            long priorityId = result.getLong(HabitPrioritySheet.RESULT_PRIORITY_ID);
            selectedPriority = repository.getPriority(priorityId);
            bindValues();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit_edit, container, false);
        repository = new HabitRepository(requireContext());
        if (getActivity() != null && previousSoftInputMode == null) {
            previousSoftInputMode = requireActivity().getWindow().getAttributes().softInputMode;
        }
        bind(view);
        installKeyboardScrollSupport();
        setupOperatorSpinner();
        if (!initialized) {
            initializeFields();
        }
        setupClicks(view);
        bindValues();
        if (initialSnapshot == null) {
            initialSnapshot = currentInputSnapshot();
        }
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
        rootView = view.findViewById(R.id.habit_edit_root);
        titleEdit = view.findViewById(R.id.edit_habit_title);
        scrollView = view.findViewById(R.id.habit_edit_scroll);
        keyboardSpacer = view.findViewById(R.id.habit_edit_keyboard_spacer);
        colorContainer = view.findViewById(R.id.layout_habit_colors);
        customColorTrigger = view.findViewById(R.id.custom_habit_color_trigger);
        categoryIcon = view.findViewById(R.id.img_selected_category);
        categoryText = view.findViewById(R.id.tv_selected_category);
        startText = view.findViewById(R.id.tv_habit_start);
        endText = view.findViewById(R.id.tv_habit_end);
        endSwitch = view.findViewById(R.id.switch_habit_end);
        endPickerLayout = view.findViewById(R.id.layout_habit_end_picker);
        endRow = view.findViewById(R.id.row_habit_end_toggle);
        priorityText = view.findViewById(R.id.tv_habit_priority);
        repeatText = view.findViewById(R.id.tv_habit_repeat);
        reminderText = view.findViewById(R.id.tv_habit_reminder);
        evaluationGroup = view.findViewById(R.id.group_habit_evaluation);
        booleanRadio = view.findViewById(R.id.radio_habit_boolean);
        numberRadio = view.findViewById(R.id.radio_habit_number);
        numberFields = view.findViewById(R.id.layout_habit_number_fields);
        operatorInput = view.findViewById(R.id.input_habit_operator);
        targetValueEdit = view.findViewById(R.id.edit_habit_target_value);
        targetUnitEdit = view.findViewById(R.id.edit_habit_target_unit);
        descriptionEdit = view.findViewById(R.id.edit_habit_description);
    }

    private void setupOperatorSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_habit_dropdown_option,
                new String[]{
                        getString(R.string.habit_operator_at_least),
                        getString(R.string.habit_operator_at_most),
                        getString(R.string.habit_operator_exactly)
                }
        );
        adapter.setDropDownViewResource(R.layout.item_habit_dropdown_option);
        operatorInput.setAdapter(adapter);
        operatorInput.setDropDownBackgroundResource(R.drawable.bg_habit_dropdown_menu);
        operatorInput.setInputType(InputType.TYPE_NULL);
        operatorInput.setKeyListener(null);
        operatorInput.setCursorVisible(false);
        operatorInput.setOnClickListener(v -> operatorInput.showDropDown());
        operatorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                operatorInput.showDropDown();
            }
        });
    }

    private void initializeFields() {
        habitId = requireArguments().getLong(ARG_HABIT_ID);
        startDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        endDate = startDate;
        if (habitId > 0) {
            HabitItem item = repository.getHabit(habitId);
            if (item != null) {
                titleEditIfReady(item.getTitle());
                title = item.getTitle() == null ? "" : item.getTitle();
                selectedColor = item.getColor() == null ? selectedColor : item.getColor();
                selectedCategory = item.getCategory();
                selectedPriority = item.getPriority();
                startDate = item.getStartDate();
                hasEndDate = item.hasEndDate();
                endDate = item.getEndDate() == null ? startDate : item.getEndDate();
                recurrenceRule = item.getRecurrenceRule() == null ? RecurrenceRule.none() : item.getRecurrenceRule();
                reminders.clear();
                reminders.addAll(item.getReminders());
                evaluationType = item.isNumberEvaluation() ? HabitItem.EVALUATION_NUMBER : HabitItem.EVALUATION_BOOLEAN;
                targetOperator = item.getTargetOperator() == null ? "" : item.getTargetOperator();
                targetValueText = item.getTargetValue() == null ? "" : String.valueOf(item.getTargetValue());
                targetUnit = item.getTargetUnit() == null ? "" : item.getTargetUnit();
                descriptionEditIfReady(item.getDescription());
                description = item.getDescription() == null ? "" : item.getDescription();
            }
        } else {
            selectedCategory = findDefaultCategory();
            selectedPriority = findDefaultPriority();
        }
        initialized = true;
    }

    private HabitCategory findDefaultCategory() {
        List<HabitCategory> categories = repository.getCategories();
        if (categories.isEmpty()) {
            return null;
        }
        for (HabitCategory category : categories) {
            if (getString(R.string.habit_category_other).equalsIgnoreCase(category.getName())) {
                return category;
            }
        }
        return categories.get(0);
    }

    private HabitPriority findDefaultPriority() {
        List<HabitPriority> priorities = repository.getPriorities();
        if (priorities.isEmpty()) {
            return null;
        }
        for (HabitPriority priority : priorities) {
            if (getString(R.string.habit_priority_medium).equalsIgnoreCase(priority.getName())) {
                return priority;
            }
        }
        return priorities.get(0);
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_back_habit).setOnClickListener(v -> handleBackPressed());
        view.findViewById(R.id.btn_save_habit).setOnClickListener(v -> saveHabit());
        view.findViewById(R.id.row_habit_category).setOnClickListener(v -> {
            captureInput();
            HabitCategoryPickerSheet.newInstance(selectedCategory == null ? 0 : selectedCategory.getId())
                    .show(getParentFragmentManager(), "HabitCategoryPicker");
        });
        view.findViewById(R.id.row_habit_start).setOnClickListener(v -> {
            captureInput();
            DatePickerDialogFragment.newInstance(RESULT_START_DATE, startDate)
                    .show(getParentFragmentManager(), RESULT_START_DATE);
        });
        endRow.setOnClickListener(v -> endSwitch.toggle());
        endSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            captureInput();
            hasEndDate = isChecked;
            if (hasEndDate && endDate == null) {
                endDate = startDate;
            }
            bindValues();
        });
        endPickerLayout.setOnClickListener(v -> {
            captureInput();
            if (hasEndDate) {
                DatePickerDialogFragment.newInstance(RESULT_END_DATE, endDate == null ? startDate : endDate)
                        .show(getParentFragmentManager(), RESULT_END_DATE);
            }
        });
        view.findViewById(R.id.row_habit_priority).setOnClickListener(v -> {
            captureInput();
            HabitPrioritySheet.newInstance(selectedPriority == null ? 0 : selectedPriority.getId())
                    .show(getParentFragmentManager(), "HabitPriorityPicker");
        });
        view.findViewById(R.id.row_habit_repeat).setOnClickListener(v -> openRecurrencePicker());
        view.findViewById(R.id.row_habit_reminder).setOnClickListener(v -> openReminderPicker());
        evaluationGroup.setOnCheckedChangeListener((group, checkedId) -> refreshEvaluationFields());
        attachKeyboardFieldBehavior(titleEdit, titleEdit, 60);
        attachKeyboardFieldBehavior(targetValueEdit, targetValueEdit, 90);
        attachKeyboardFieldBehavior(targetUnitEdit, targetUnitEdit, 90);
        attachKeyboardFieldBehavior(descriptionEdit, descriptionEdit, 140);
    }

    private void openRecurrencePicker() {
        captureInput();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).pushFullScreenFragment(
                    RecurrenceFragment.newInstance(RESULT_RECURRENCE, recurrenceRule, startDate),
                    "HabitRecurrence"
            );
        }
    }

    private void openReminderPicker() {
        captureInput();
        Reminder current = reminders.isEmpty() ? Reminder.none() : reminders.get(0);
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).pushFullScreenFragment(
                    ReminderFragment.newInstance(RESULT_REMINDER, current),
                    "HabitReminder"
            );
        }
    }

    private void bindValues() {
        if (titleEdit == null) {
            return;
        }
        refreshColors();
        if (!title.equals(titleEdit.getText().toString())) {
            titleEdit.setText(title);
            titleEdit.setSelection(titleEdit.getText().length());
        }
        if (!description.equals(descriptionEdit.getText().toString())) {
            descriptionEdit.setText(description);
            descriptionEdit.setSelection(descriptionEdit.getText().length());
        }
        if (selectedCategory != null) {
            categoryIcon.setImageResource(HabitDefaults.resolveIconRes(requireContext(), selectedCategory.getIconUri()));
            categoryText.setText(selectedCategory.getName());
        } else {
            categoryIcon.setImageResource(R.drawable.habit_category_other);
            categoryText.setText(R.string.habit_category_other);
        }
        startText.setText(DateTimeUtils.formatVietnameseDate(startDate));
        endSwitch.setChecked(hasEndDate);
        endPickerLayout.setVisibility(hasEndDate ? View.VISIBLE : View.GONE);
        endText.setText(DateTimeUtils.formatVietnameseDate(endDate == null ? startDate : endDate));
        if (selectedPriority != null) {
            HabitUiHelper.stylePriorityChip(priorityText, selectedPriority.getName(), selectedPriority.getColor());
        } else {
            HabitUiHelper.stylePriorityChip(priorityText, getString(R.string.none), getStringColorFallback());
        }
        repeatText.setText(recurrenceRule == null || recurrenceRule.isNone()
                ? getString(R.string.habit_repeat_none)
                : recurrenceRule.getDisplayText());
        reminderText.setText(reminders.isEmpty()
                ? getString(R.string.none)
                : reminders.get(0).getDisplayText());
        booleanRadio.setChecked(HabitItem.EVALUATION_BOOLEAN.equals(evaluationType));
        numberRadio.setChecked(HabitItem.EVALUATION_NUMBER.equals(evaluationType));
        operatorInput.setText(operatorLabel(targetOperator), false);
        if (!targetValueText.equals(targetValueEdit.getText().toString())) {
            targetValueEdit.setText(targetValueText);
            targetValueEdit.setSelection(targetValueEdit.getText().length());
        }
        if (!targetUnit.equals(targetUnitEdit.getText().toString())) {
            targetUnitEdit.setText(targetUnit);
            targetUnitEdit.setSelection(targetUnitEdit.getText().length());
        }
        refreshEvaluationFields();
    }

    private void refreshColors() {
        HabitUiHelper.populateColorDots(requireContext(), colorContainer, HabitDefaults.COLOR_OPTIONS, selectedColor, color -> {
            selectedColor = color;
            refreshColors();
        });
        HabitUiHelper.bindCustomColorTrigger(requireContext(), customColorTrigger, HabitDefaults.COLOR_OPTIONS, selectedColor, color -> {
            selectedColor = color;
            refreshColors();
        });
    }

    private void refreshEvaluationFields() {
        if (!numberRadio.isChecked() && !booleanRadio.isChecked()) {
            booleanRadio.setChecked(true);
        }
        boolean numberMode = numberRadio.isChecked();
        numberFields.setVisibility(numberMode ? View.VISIBLE : View.GONE);
    }

    private void installKeyboardScrollSupport() {
        if (rootView == null || keyboardLayoutListener != null) {
            return;
        }
        baseKeyboardSpacerHeight = keyboardSpacer != null && keyboardSpacer.getLayoutParams() != null
                ? keyboardSpacer.getLayoutParams().height
                : dpToPx(56);
        keyboardLayoutListener = () -> {
            if (!isAdded() || rootView == null) {
                return;
            }
            Rect visibleFrame = new Rect();
            rootView.getWindowVisibleDisplayFrame(visibleFrame);
            int fullHeight = rootView.getRootView().getHeight();
            int overlap = Math.max(0, fullHeight - visibleFrame.bottom);
            boolean nowVisible = overlap > dpToPx(120);
            int desiredInset = nowVisible ? overlap : 0;
            boolean visibilityChanged = nowVisible != keyboardVisible;
            if (desiredInset == keyboardInset && nowVisible == keyboardVisible) {
                return;
            }
            keyboardInset = desiredInset;
            keyboardVisible = nowVisible;
            int spacerHeight = baseKeyboardSpacerHeight
                    + (keyboardVisible ? Math.max(dpToPx(28), keyboardInset - dpToPx(20)) : 0);
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

    private void attachKeyboardFieldBehavior(EditText field, View anchor, long focusDelayMs) {
        if (field == null || anchor == null) {
            return;
        }
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                enableKeyboardInputMode();
                requestFieldVisibility(anchor, Math.max(focusDelayMs, 220L), true);
            }
        });
        field.setOnClickListener(v -> {
            enableKeyboardInputMode();
            requestFieldVisibility(anchor, 0L, false);
        });
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (field.hasFocus()) {
                    long delay = field == descriptionEdit ? 24L : 0L;
                    requestFieldVisibility(anchor, delay, false);
                    if (field == descriptionEdit) {
                        anchor.postDelayed(() -> {
                            if (field.hasFocus()) {
                                requestFieldVisibility(anchor, 0L, false);
                            }
                        }, 72L);
                    }
                }
            }
        });
    }

    private void requestFieldVisibility(View anchor, long delayMs, boolean animated) {
        if (scrollView == null || rootView == null || anchor == null || !isAdded()) {
            return;
        }
        cancelPendingKeyboardScroll();
        pendingKeyboardScrollAnchor = anchor;
        pendingKeyboardScrollRunnable = () -> {
            if (!isAdded() || !keyboardVisible || anchor != resolveFocusedAnchor()) {
                return;
            }
            Rect visibleFrame = new Rect();
            rootView.getWindowVisibleDisplayFrame(visibleFrame);

            int[] location = new int[2];
            anchor.getLocationOnScreen(location);
            int anchorTopOnScreen = location[1];
            int anchorBottomOnScreen = resolveAnchorBottomOnScreen(anchor, anchorTopOnScreen);
            int desiredBottom = visibleFrame.bottom - resolveDesiredBottomInset(anchor);
            if (anchorBottomOnScreen <= desiredBottom) {
                return;
            }
            int delta = anchorBottomOnScreen - desiredBottom;
            if (anchor == descriptionEdit && descriptionEdit != null) {
                delta += Math.max(dpToPx(24), descriptionEdit.getLineHeight());
            }
            if (delta < dpToPx(6)) {
                return;
            }
            int contentHeight = scrollView.getChildCount() > 0 ? scrollView.getChildAt(0).getHeight() : 0;
            int maxScroll = Math.max(0, contentHeight - scrollView.getHeight());
            int targetScroll = clamp(scrollView.getScrollY() + delta, 0, maxScroll);
            if (animated) {
                scrollView.smoothScrollTo(0, targetScroll);
            } else {
                scrollView.scrollTo(0, targetScroll);
            }
        };
        if (delayMs > 0L) {
            anchor.postDelayed(pendingKeyboardScrollRunnable, delayMs);
        } else {
            anchor.post(pendingKeyboardScrollRunnable);
        }
    }

    private View resolveFocusedAnchor() {
        if (isFieldFocused(descriptionEdit)) {
            return descriptionEdit;
        }
        if (isFieldFocused(targetUnitEdit)) {
            return targetUnitEdit;
        }
        if (isFieldFocused(targetValueEdit)) {
            return targetValueEdit;
        }
        if (isFieldFocused(titleEdit)) {
            return titleEdit;
        }
        return null;
    }

    private int resolveAnchorBottomOnScreen(View anchor, int anchorTopOnScreen) {
        if (!(anchor instanceof EditText)) {
            return anchorTopOnScreen + anchor.getHeight();
        }
        EditText editText = (EditText) anchor;
        if (!editText.hasFocus() || editText.getLayout() == null) {
            return anchorTopOnScreen + anchor.getHeight();
        }
        int safeSelection = Math.max(0, editText.getSelectionStart());
        int line = editText.getLayout().getLineForOffset(safeSelection);
        int cursorBottom = anchorTopOnScreen
                + editText.getTotalPaddingTop()
                + editText.getLayout().getLineBottom(line)
                - editText.getScrollY()
                + Math.max(dpToPx(12), editText.getLineHeight() / 2);
        int minimumVisibleBottom = anchorTopOnScreen + Math.min(editText.getHeight(), dpToPx(44));
        return Math.max(cursorBottom, minimumVisibleBottom);
    }

    private int resolveDesiredBottomInset(View anchor) {
        if (anchor != descriptionEdit || descriptionEdit == null) {
            return dpToPx(24);
        }
        int lineHeight = Math.max(descriptionEdit.getLineHeight(), dpToPx(20));
        return Math.max(dpToPx(144), lineHeight * 2 + descriptionEdit.getTotalPaddingBottom() + dpToPx(76));
    }

    private void cancelPendingKeyboardScroll() {
        if (pendingKeyboardScrollAnchor != null && pendingKeyboardScrollRunnable != null) {
            pendingKeyboardScrollAnchor.removeCallbacks(pendingKeyboardScrollRunnable);
        }
        pendingKeyboardScrollAnchor = null;
        pendingKeyboardScrollRunnable = null;
    }

    private void enableKeyboardInputMode() {
        if (getActivity() == null) {
            return;
        }
        int currentMode = requireActivity().getWindow().getAttributes().softInputMode;
        int stateMask = currentMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE;
        requireActivity().getWindow().setSoftInputMode(stateMask | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void restoreSoftInputMode() {
        if (getActivity() == null || previousSoftInputMode == null) {
            return;
        }
        requireActivity().getWindow().setSoftInputMode(previousSoftInputMode);
    }

    private boolean isFieldFocused(EditText field) {
        return field != null && field.hasFocus();
    }

    private void captureInput() {
        if (titleEdit == null) {
            return;
        }
        title = titleEdit.getText().toString().trim();
        description = descriptionEdit.getText().toString().trim();
        evaluationType = numberRadio.isChecked() ? HabitItem.EVALUATION_NUMBER : HabitItem.EVALUATION_BOOLEAN;
        targetOperator = selectedOperatorValue();
        targetValueText = targetValueEdit.getText().toString().trim();
        targetUnit = targetUnitEdit.getText().toString().trim();
    }

    private HabitItem buildHabitFromInput() {
        captureInput();
        HabitItem item = new HabitItem();
        item.setId(habitId);
        item.setTitle(title);
        item.setDescription(description);
        item.setColor(selectedColor);
        item.setCategory(selectedCategory);
        item.setCategoryId(selectedCategory == null ? null : selectedCategory.getId());
        item.setPriority(selectedPriority);
        item.setPriorityId(selectedPriority == null ? null : selectedPriority.getId());
        item.setStartDate(startDate);
        item.setHasEndDate(hasEndDate);
        item.setEndDate(hasEndDate ? (endDate != null && endDate.isBefore(startDate) ? startDate : endDate) : null);
        item.setRecurrenceRule(recurrenceRule == null ? RecurrenceRule.none() : recurrenceRule);
        item.setReminders(new ArrayList<>(reminders));
        item.setEvaluationType(evaluationType);
        if (HabitItem.EVALUATION_NUMBER.equals(evaluationType)) {
            item.setTargetOperator(targetOperator == null || targetOperator.isEmpty()
                    ? HabitItem.OPERATOR_AT_LEAST
                    : targetOperator);
            item.setTargetValue(parseDouble(targetValueText));
            item.setTargetUnit(targetUnit);
        }
        return item;
    }

    private void saveHabit() {
        HabitItem item = buildHabitFromInput();
        if (item.getTitle() == null || item.getTitle().isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_title_error, Toast.LENGTH_SHORT).show();
            return;
        }
        repository.saveHabit(item);
        suppressExitConfirmation = true;
        ((MainActivity) requireActivity()).finishToHome();
    }

    public void handleBackPressed() {
        if (!suppressExitConfirmation && hasUnsavedChanges()) {
            UiUtils.showConfirmDialog(
                    requireContext(),
                    getString(R.string.unsaved_changes_confirm),
                    getString(R.string.discard_changes),
                    this::exitWithoutSaving
            );
            return;
        }
        exitWithoutSaving();
    }

    private void exitWithoutSaving() {
        suppressExitConfirmation = true;
        ((MainActivity) requireActivity()).finishToHome();
    }

    private boolean hasUnsavedChanges() {
        return initialSnapshot != null && !initialSnapshot.equals(currentInputSnapshot());
    }

    private String currentInputSnapshot() {
        captureInput();
        long categoryId = selectedCategory == null ? 0 : selectedCategory.getId();
        long priorityId = selectedPriority == null ? 0 : selectedPriority.getId();
        String recurrenceText = recurrenceRule == null ? "" : recurrenceRule.getDisplayText();
        String reminderTextValue = reminders.isEmpty() ? "" : reminders.get(0).getDisplayText();
        return safe(title) + "|"
                + safe(description) + "|"
                + safe(selectedColor) + "|"
                + categoryId + "|"
                + priorityId + "|"
                + DateTimeUtils.dateToIso(startDate) + "|"
                + hasEndDate + "|"
                + DateTimeUtils.dateToIso(endDate) + "|"
                + safe(recurrenceText) + "|"
                + safe(reminderTextValue) + "|"
                + safe(evaluationType) + "|"
                + safe(targetOperator) + "|"
                + safe(targetValueText) + "|"
                + safe(targetUnit);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String selectedOperatorValue() {
        String value = operatorInput.getText().toString().trim();
        if (value.equals(getString(R.string.habit_operator_at_most))) {
            return HabitItem.OPERATOR_AT_MOST;
        }
        if (value.equals(getString(R.string.habit_operator_exactly))) {
            return HabitItem.OPERATOR_EXACTLY;
        }
        if (value.equals(getString(R.string.habit_operator_at_least))) {
            return HabitItem.OPERATOR_AT_LEAST;
        }
        return "";
    }

    private String getStringColorFallback() {
        return "#F0E2D6";
    }

    private String operatorLabel(String operator) {
        if (HabitItem.OPERATOR_AT_MOST.equals(operator)) {
            return getString(R.string.habit_operator_at_most);
        }
        if (HabitItem.OPERATOR_EXACTLY.equals(operator)) {
            return getString(R.string.habit_operator_exactly);
        }
        if (HabitItem.OPERATOR_AT_LEAST.equals(operator)) {
            return getString(R.string.habit_operator_at_least);
        }
        return "";
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void titleEditIfReady(String value) {
        if (titleEdit != null) {
            titleEdit.setText(value);
        }
    }

    private void descriptionEditIfReady(String value) {
        if (descriptionEdit != null) {
            descriptionEdit.setText(value == null ? "" : value);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
