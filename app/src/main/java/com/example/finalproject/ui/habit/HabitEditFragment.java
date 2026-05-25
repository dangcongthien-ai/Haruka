package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.text.InputType;
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
    private View descriptionSection;
    private TextView priorityText;
    private TextView repeatText;
    private TextView reminderText;
    private RadioGroup evaluationGroup;
    private RadioButton booleanRadio;
    private RadioButton numberRadio;
    private View numberFields;
    private AutoCompleteTextView operatorInput;
    private EditText targetValueEdit;
    private EditText targetUnitEdit;
    private EditText descriptionEdit;
    private Integer previousSoftInputMode;

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
        setupOperatorSpinner();
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
        restoreSoftInputMode();
    }

    @Override
    public void onPause() {
        restoreSoftInputMode();
        super.onPause();
    }

    private void bind(View view) {
        titleEdit = view.findViewById(R.id.edit_habit_title);
        scrollView = view.findViewById(R.id.habit_edit_scroll);
        colorContainer = view.findViewById(R.id.layout_habit_colors);
        categoryIcon = view.findViewById(R.id.img_selected_category);
        categoryText = view.findViewById(R.id.tv_selected_category);
        startText = view.findViewById(R.id.tv_habit_start);
        endText = view.findViewById(R.id.tv_habit_end);
        endSwitch = view.findViewById(R.id.switch_habit_end);
        endPickerLayout = view.findViewById(R.id.layout_habit_end_picker);
        endRow = view.findViewById(R.id.row_habit_end_toggle);
        descriptionSection = view.findViewById(R.id.layout_habit_description_section);
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
        view.findViewById(R.id.btn_back_habit).setOnClickListener(v -> ((MainActivity) requireActivity()).finishToHome());
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
        attachKeyboardFieldBehavior(descriptionEdit, descriptionSection == null ? descriptionEdit : descriptionSection, 140);
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
        titleEdit.setText(title);
        descriptionEdit.setText(description);
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
        targetValueEdit.setText(targetValueText);
        targetUnitEdit.setText(targetUnit);
        refreshEvaluationFields();
    }

    private void refreshColors() {
        HabitUiHelper.populateColorDots(requireContext(), colorContainer, HabitDefaults.COLOR_OPTIONS, selectedColor, color -> {
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

    private void scrollFieldIntoView(View anchor, long delayMs, boolean animated) {
        if (scrollView == null || anchor == null) {
            return;
        }
        anchor.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            int margin = dpToPx(16);
            int extraTop = anchor == descriptionSection ? dpToPx(12) : 0;
            int targetScroll = Math.max(0, anchor.getTop() - margin - extraTop);
            if (animated) {
                scrollView.smoothScrollTo(0, targetScroll);
            } else {
                scrollView.scrollTo(0, targetScroll);
            }
        }, delayMs);
    }

    private void attachKeyboardFieldBehavior(EditText field, View anchor, long focusDelayMs) {
        if (field == null || anchor == null) {
            return;
        }
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                enableKeyboardInputMode();
                scrollFieldIntoView(anchor, focusDelayMs, true);
            } else {
                v.post(this::restoreSoftInputModeIfNeeded);
            }
        });
        field.setOnClickListener(v -> {
            enableKeyboardInputMode();
            scrollFieldIntoView(anchor, 0, false);
        });
    }

    private void enableKeyboardInputMode() {
        if (getActivity() == null) {
            return;
        }
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    private void restoreSoftInputMode() {
        if (getActivity() == null || previousSoftInputMode == null) {
            return;
        }
        requireActivity().getWindow().setSoftInputMode(previousSoftInputMode);
    }

    private void restoreSoftInputModeIfNeeded() {
        if (!isAnyKeyboardFieldFocused()) {
            restoreSoftInputMode();
        }
    }

    private boolean isAnyKeyboardFieldFocused() {
        return isFieldFocused(titleEdit)
                || isFieldFocused(targetValueEdit)
                || isFieldFocused(targetUnitEdit)
                || isFieldFocused(descriptionEdit);
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
        ((MainActivity) requireActivity()).finishToHome();
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
}
