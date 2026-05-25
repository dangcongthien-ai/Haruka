package com.example.finalproject.ui.calendar;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.ScreenBackHandler;
import com.example.finalproject.ui.common.UiUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecurrenceFragment extends Fragment implements ScreenBackHandler {
    public static final String ARG_RESULT_KEY = "result_key";
    public static final String ARG_RULE = "rule";
    public static final String ARG_BASE_DATE = "base_date";
    public static final String RESULT_RULE = "rule";
    private static final String END_DATE_RESULT = "recurrence_end_date";

    private RecurrenceRule rule;
    private LocalDate baseDate;
    private String resultKey;
    private View customPanel;
    private EditText intervalEdit;
    private AutoCompleteTextView unitInput;
    private TextView repeatOnLabel;
    private LinearLayout weeklyDays;
    private AutoCompleteTextView monthlyPatternInput;
    private TextView endDateText;
    private TextView endCountText;
    private boolean customMode;
    private final List<TextView> mainChecks = new ArrayList<>();
    private final List<TextView> endChecks = new ArrayList<>();
    private final List<String> selectedWeekDays = new ArrayList<>();
    private int monthlyPatternSelection;

    public static RecurrenceFragment newInstance(String resultKey, RecurrenceRule rule, LocalDate baseDate) {
        RecurrenceFragment fragment = new RecurrenceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESULT_KEY, resultKey);
        args.putSerializable(ARG_RULE, rule == null ? RecurrenceRule.none() : rule);
        args.putString(ARG_BASE_DATE, DateTimeUtils.dateToIso(baseDate));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recurrence, container, false);
        resultKey = requireArguments().getString(ARG_RESULT_KEY);
        rule = (RecurrenceRule) requireArguments().getSerializable(ARG_RULE);
        if (rule == null) {
            rule = RecurrenceRule.none();
        }
        baseDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_BASE_DATE));
        if (baseDate == null) {
            baseDate = LocalDate.now();
        }
        customMode = !rule.isNone()
                && (rule.getIntervalValue() > 1
                || rule.getByDay() != null
                || rule.getMonthlyPatternType() != null
                || RecurrenceRule.END_DATE.equals(rule.getEndType())
                || RecurrenceRule.END_COUNT.equals(rule.getEndType()));
        bind(view);
        setupDropdowns();
        setupWeekDays();
        setupClicks(view);
        setupDateResult();
        refresh();
        return view;
    }

    private void bind(View view) {
        customPanel = view.findViewById(R.id.custom_panel);
        intervalEdit = view.findViewById(R.id.edit_interval);
        unitInput = view.findViewById(R.id.input_freq_unit);
        repeatOnLabel = view.findViewById(R.id.tv_repeat_on_label);
        weeklyDays = view.findViewById(R.id.weekly_days);
        monthlyPatternInput = view.findViewById(R.id.input_monthly_pattern);
        endDateText = view.findViewById(R.id.tv_end_date_option);
        endCountText = view.findViewById(R.id.tv_end_count_option);
        mainChecks.add(view.findViewById(R.id.check_none));
        mainChecks.add(view.findViewById(R.id.check_daily));
        mainChecks.add(view.findViewById(R.id.check_weekly));
        mainChecks.add(view.findViewById(R.id.check_monthly));
        mainChecks.add(view.findViewById(R.id.check_yearly));
        mainChecks.add(view.findViewById(R.id.check_custom));
        endChecks.add(view.findViewById(R.id.check_end_none));
        endChecks.add(view.findViewById(R.id.check_end_date));
        endChecks.add(view.findViewById(R.id.check_end_count));
    }

    private void setupDropdowns() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.recurrence_units,
                R.layout.item_recurrence_dropdown_option
        );
        adapter.setDropDownViewResource(R.layout.item_recurrence_dropdown_option);
        unitInput.setAdapter(adapter);
        unitInput.setDropDownBackgroundResource(R.drawable.bg_recurrence_dropdown_menu);
        unitInput.setInputType(InputType.TYPE_NULL);
        unitInput.setKeyListener(null);
        unitInput.setCursorVisible(false);
        unitInput.setOnClickListener(v -> unitInput.showDropDown());
        unitInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                unitInput.showDropDown();
            }
        });
        unitInput.setOnItemClickListener((parent, view, position, id) -> {
            if (!customMode) {
                return;
            }
            String[] freqs = {
                    RecurrenceRule.FREQ_DAILY,
                    RecurrenceRule.FREQ_WEEKLY,
                    RecurrenceRule.FREQ_MONTHLY,
                    RecurrenceRule.FREQ_YEARLY
            };
            rule.setFreq(freqs[position]);
            refreshCustomControls();
        });

        monthlyPatternInput.setInputType(InputType.TYPE_NULL);
        monthlyPatternInput.setKeyListener(null);
        monthlyPatternInput.setCursorVisible(false);
        monthlyPatternInput.setDropDownBackgroundResource(R.drawable.bg_recurrence_dropdown_menu);
        monthlyPatternInput.setOnClickListener(v -> monthlyPatternInput.showDropDown());
        monthlyPatternInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                monthlyPatternInput.showDropDown();
            }
        });
        monthlyPatternInput.setOnItemClickListener((parent, view, position, id) -> {
            monthlyPatternSelection = position;
            applyMonthlyPatternSelection(position);
            monthlyPatternInput.dismissDropDown();
            refreshCustomControls();
        });
    }

    private void setupWeekDays() {
        if (rule.getByDay() != null && !rule.getByDay().isEmpty()) {
            String[] days = rule.getByDay().split(",");
            for (String day : days) {
                selectedWeekDays.add(day.trim());
            }
        }
        String[] labels = {
                getString(R.string.weekday_monday),
                getString(R.string.weekday_tuesday),
                getString(R.string.weekday_wednesday),
                getString(R.string.weekday_thursday),
                getString(R.string.weekday_friday),
                getString(R.string.weekday_saturday),
                getString(R.string.weekday_sunday)
        };
        String[] values = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
        for (int i = 0; i < labels.length; i++) {
            TextView chip = new TextView(requireContext());
            chip.setText(labels[i]);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextSize(12);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    UiUtils.dp(requireContext(), 40),
                    UiUtils.dp(requireContext(), 40)
            );
            params.setMargins(UiUtils.dp(requireContext(), 4), 0, UiUtils.dp(requireContext(), 4), 0);
            chip.setLayoutParams(params);
            int index = i;
            chip.setOnClickListener(v -> {
                if (selectedWeekDays.contains(values[index])) {
                    selectedWeekDays.remove(values[index]);
                } else {
                    selectedWeekDays.add(values[index]);
                }
                updateWeekChips();
            });
            weeklyDays.addView(chip);
        }
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> ((MainActivity) requireActivity()).handleActivityBackPressed());
        view.findViewById(R.id.option_none).setOnClickListener(v -> {
            setSimple(RecurrenceRule.FREQ_NONE);
            saveResult();
        });
        view.findViewById(R.id.option_daily).setOnClickListener(v -> {
            setSimple(RecurrenceRule.FREQ_DAILY);
            saveResult();
        });
        view.findViewById(R.id.option_weekly).setOnClickListener(v -> {
            setSimple(RecurrenceRule.FREQ_WEEKLY);
            saveResult();
        });
        view.findViewById(R.id.option_monthly).setOnClickListener(v -> {
            setSimple(RecurrenceRule.FREQ_MONTHLY);
            saveResult();
        });
        view.findViewById(R.id.option_yearly).setOnClickListener(v -> {
            setSimple(RecurrenceRule.FREQ_YEARLY);
            saveResult();
        });
        view.findViewById(R.id.option_custom).setOnClickListener(v -> {
            customMode = true;
            if (rule.isNone()) {
                rule.setFreq(RecurrenceRule.FREQ_DAILY);
                rule.setIntervalValue(1);
            }
            refresh();
        });
        view.findViewById(R.id.end_none_row).setOnClickListener(v -> {
            rule.setEndType(RecurrenceRule.END_NONE);
            refresh();
        });
        view.findViewById(R.id.end_date_row).setOnClickListener(v -> DatePickerDialogFragment
                .newInstance(END_DATE_RESULT, rule.getEndDate() == null ? baseDate : rule.getEndDate())
                .show(getParentFragmentManager(), END_DATE_RESULT));
        view.findViewById(R.id.end_count_row).setOnClickListener(v -> showCountDialog());
    }

    @Override
    public boolean onHandleBackPressed() {
        if (customMode) {
            saveResult();
        } else {
            ((MainActivity) requireActivity()).finishFullScreen();
        }
        return true;
    }

    private void setupDateResult() {
        getParentFragmentManager().setFragmentResultListener(END_DATE_RESULT, getViewLifecycleOwner(), (requestKey, result) -> {
            LocalDate date = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            rule.setEndDate(date);
            rule.setEndType(RecurrenceRule.END_DATE);
            refresh();
        });
    }

    private void setSimple(String freq) {
        customMode = false;
        rule.setFreq(freq);
        rule.setIntervalValue(1);
        rule.setByDay(null);
        rule.setByMonthDay(null);
        rule.setMonthlyPatternType(null);
        rule.setEndType(RecurrenceRule.END_NONE);
        rule.setEndDate(null);
        rule.setOccurrenceCount(null);
        refresh();
    }

    private void saveResult() {
        if (customMode) {
            rule.setIntervalValue(parseInterval());
            if (RecurrenceRule.FREQ_WEEKLY.equals(rule.getFreq())) {
                rule.setByDay(selectedWeekDays.isEmpty() ? dayToRule(baseDate) : String.join(",", selectedWeekDays));
                rule.setByMonthDay(null);
                rule.setMonthlyPatternType(null);
                rule.setBySetPos(null);
            } else if (RecurrenceRule.FREQ_MONTHLY.equals(rule.getFreq())) {
                applyMonthlyPatternSelection(monthlyPatternSelection);
            } else {
                rule.setByDay(null);
                rule.setByMonthDay(null);
                rule.setMonthlyPatternType(null);
                rule.setBySetPos(null);
            }
        }
        Bundle result = new Bundle();
        result.putSerializable(RESULT_RULE, rule);
        getParentFragmentManager().setFragmentResult(resultKey, result);
        ((MainActivity) requireActivity()).finishFullScreen();
    }

    private void refresh() {
        customPanel.setVisibility(customMode ? View.VISIBLE : View.GONE);
        UiUtils.setCheck(mainChecks.get(0), rule.isNone());
        UiUtils.setCheck(mainChecks.get(1), RecurrenceRule.FREQ_DAILY.equals(rule.getFreq()) && !customMode);
        UiUtils.setCheck(mainChecks.get(2), RecurrenceRule.FREQ_WEEKLY.equals(rule.getFreq()) && !customMode);
        UiUtils.setCheck(mainChecks.get(3), RecurrenceRule.FREQ_MONTHLY.equals(rule.getFreq()) && !customMode);
        UiUtils.setCheck(mainChecks.get(4), RecurrenceRule.FREQ_YEARLY.equals(rule.getFreq()) && !customMode);
        UiUtils.setCheck(mainChecks.get(5), customMode);
        intervalEdit.setText(String.valueOf(Math.max(1, rule.getIntervalValue())));
        updateUnitInput();
        refreshCustomControls();
        refreshEndOptions();
    }

    private void refreshCustomControls() {
        boolean weekly = RecurrenceRule.FREQ_WEEKLY.equals(rule.getFreq());
        boolean monthly = RecurrenceRule.FREQ_MONTHLY.equals(rule.getFreq());
        repeatOnLabel.setVisibility(weekly ? View.VISIBLE : View.GONE);
        weeklyDays.setVisibility(weekly ? View.VISIBLE : View.GONE);
        monthlyPatternInput.setVisibility(monthly ? View.VISIBLE : View.GONE);
        String[] monthlyOptions = buildMonthlyPatternOptions();
        monthlyPatternSelection = resolveMonthlyPatternSelection();
        monthlyPatternInput.setText(monthlyOptions[monthlyPatternSelection], false);
        ArrayAdapter<String> monthlyAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_recurrence_dropdown_option,
                monthlyOptions
        );
        monthlyAdapter.setDropDownViewResource(R.layout.item_recurrence_dropdown_option);
        monthlyPatternInput.setAdapter(monthlyAdapter);
        updateWeekChips();
    }

    private void refreshEndOptions() {
        endDateText.setText(rule.getEndDate() == null ? DateTimeUtils.formatDateWithDow(baseDate) : DateTimeUtils.formatDateWithDow(rule.getEndDate()));
        endCountText.setText(rule.getOccurrenceCount() == null
                ? getString(R.string.end_after_once)
                : getString(R.string.repeat_count_times, rule.getOccurrenceCount()));
        UiUtils.setCheck(endChecks.get(0), RecurrenceRule.END_NONE.equals(rule.getEndType()));
        UiUtils.setCheck(endChecks.get(1), RecurrenceRule.END_DATE.equals(rule.getEndType()));
        UiUtils.setCheck(endChecks.get(2), RecurrenceRule.END_COUNT.equals(rule.getEndType()));
    }

    private void updateUnitInput() {
        String[] units = getResources().getStringArray(R.array.recurrence_units);
        int index = 0;
        if (RecurrenceRule.FREQ_WEEKLY.equals(rule.getFreq())) {
            index = 1;
        } else if (RecurrenceRule.FREQ_MONTHLY.equals(rule.getFreq())) {
            index = 2;
        } else if (RecurrenceRule.FREQ_YEARLY.equals(rule.getFreq())) {
            index = 3;
        }
        unitInput.setText(units[index], false);
    }

    private void updateWeekChips() {
        String defaultDay = dayToRule(baseDate);
        if (selectedWeekDays.isEmpty()) {
            selectedWeekDays.add(defaultDay);
        }
        String[] values = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
        for (int i = 0; i < weeklyDays.getChildCount(); i++) {
            TextView chip = (TextView) weeklyDays.getChildAt(i);
            boolean selected = selectedWeekDays.contains(values[i]);
            chip.setBackground(selected
                    ? UiUtils.rounded(requireContext().getColor(R.color.brand_orange), 20, requireContext())
                    : UiUtils.roundedStroke(requireContext().getColor(R.color.surface), requireContext().getColor(R.color.line), 20, requireContext()));
            chip.setTextColor(selected ? requireContext().getColor(R.color.white) : requireContext().getColor(R.color.text_primary));
        }
    }

    private void showCountDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_count_picker, null, false);
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();
            attributes.dimAmount = 0.28f;
            dialog.getWindow().setAttributes(attributes);
        }
        NumberPicker picker = content.findViewById(R.id.picker_repeat_count);
        picker.setMinValue(1);
        picker.setMaxValue(100);
        picker.setValue(rule.getOccurrenceCount() == null ? 1 : rule.getOccurrenceCount());
        styleNumberPicker(picker);
        content.findViewById(R.id.btn_count_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_count_ok).setOnClickListener(v -> {
            rule.setOccurrenceCount(picker.getValue());
            rule.setEndType(RecurrenceRule.END_COUNT);
            refresh();
            dialog.dismiss();
        });
        dialog.show();
    }

    private int parseInterval() {
        try {
            return Math.max(1, Integer.parseInt(intervalEdit.getText().toString()));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private String dayToRule(LocalDate date) {
        String[] values = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
        return values[date.getDayOfWeek().getValue() - 1];
    }

    private String[] buildMonthlyPatternOptions() {
        return new String[]{
                getString(R.string.monthly_on_day, baseDate.getDayOfMonth()),
                getString(R.string.monthly_on_weekday_ordinal, weekdayLabel(baseDate), ordinalLabel(baseDate))
        };
    }

    private int resolveMonthlyPatternSelection() {
        return "DAY_OF_WEEK".equals(rule.getMonthlyPatternType()) ? 1 : 0;
    }

    private void applyMonthlyPatternSelection(int position) {
        monthlyPatternSelection = position;
        if (position == 1) {
            rule.setMonthlyPatternType("DAY_OF_WEEK");
            rule.setByMonthDay(null);
            rule.setByDay(dayToRule(baseDate));
            rule.setBySetPos(resolveWeekOrdinal(baseDate));
        } else {
            rule.setMonthlyPatternType("DAY_OF_MONTH");
            rule.setByMonthDay(baseDate.getDayOfMonth());
            rule.setByDay(null);
            rule.setBySetPos(null);
        }
    }

    private int resolveWeekOrdinal(LocalDate date) {
        LocalDate plusWeek = date.plusWeeks(1);
        if (plusWeek.getMonth() != date.getMonth()) {
            return -1;
        }
        return ((date.getDayOfMonth() - 1) / 7) + 1;
    }

    private String ordinalLabel(LocalDate date) {
        int ordinal = resolveWeekOrdinal(date);
        if (ordinal == -1) {
            return getString(R.string.ordinal_last);
        }
        return String.format(Locale.getDefault(), getString(R.string.ordinal_number), ordinal);
    }

    private String weekdayLabel(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
                return getString(R.string.weekday_monday);
            case TUESDAY:
                return getString(R.string.weekday_tuesday);
            case WEDNESDAY:
                return getString(R.string.weekday_wednesday);
            case THURSDAY:
                return getString(R.string.weekday_thursday);
            case FRIDAY:
                return getString(R.string.weekday_friday);
            case SATURDAY:
                return getString(R.string.weekday_saturday);
            default:
                return getString(R.string.weekday_sunday);
        }
    }

    private void styleNumberPicker(NumberPicker picker) {
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        try {
            Field dividerField = NumberPicker.class.getDeclaredField("mSelectionDivider");
            dividerField.setAccessible(true);
            dividerField.set(picker, new ColorDrawable(requireContext().getColor(R.color.brand_orange)));

            Field heightField = NumberPicker.class.getDeclaredField("mSelectionDividerHeight");
            heightField.setAccessible(true);
            heightField.setInt(picker, UiUtils.dp(requireContext(), 1));

            Field paintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            paintField.setAccessible(true);
            Paint paint = (Paint) paintField.get(picker);
            paint.setColor(requireContext().getColor(R.color.text_primary));
            picker.invalidate();
        } catch (Exception ignored) {
        }
    }
}
