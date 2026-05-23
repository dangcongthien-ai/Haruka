package com.example.finalproject.ui.calendar;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.repository.CalendarRepository;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.ui.todo.TodoEditFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventEditFragment extends Fragment {
    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_DATE = "date";
    private static final String RESULT_RECURRENCE = "event_recurrence";
    private static final String RESULT_REMINDER = "event_reminder";
    private static final String RESULT_START_DATE = "event_start_date";
    private static final String RESULT_END_DATE = "event_end_date";
    private static final int[] PRESET_COLOR_RES_IDS = {
            R.color.palette_pink_1,
            R.color.palette_pink_2,
            R.color.palette_lilac_1,
            R.color.palette_lilac_2,
            R.color.palette_blue_1,
            R.color.palette_sky_1,
            R.color.palette_sky_2,
            R.color.palette_cyan_1,
            R.color.palette_teal_1,
            R.color.palette_green_1,
            R.color.palette_lime_1,
            R.color.palette_lime_2,
            R.color.palette_yellow_1,
            R.color.palette_orange_1,
            R.color.palette_red_1,
            R.color.palette_magenta_1,
            R.color.palette_magenta_2,
            R.color.palette_purple_1,
            R.color.palette_purple_2,
            R.color.palette_blue_2,
            R.color.palette_blue_3,
            R.color.palette_green_2,
            R.color.palette_amber_1,
            R.color.palette_orange_2
    };

    private CalendarRepository repository;
    private long eventId;
    private boolean initialized;
    private String title = "";
    private String notes = "";
    private String location = "";
    private String url = "";
    private String selectedColor = "#AFC0EA";
    private boolean allDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime = LocalTime.of(7, 15);
    private LocalTime endTime = LocalTime.of(11, 40);
    private RecurrenceRule recurrenceRule = RecurrenceRule.none();
    private final List<Reminder> reminders = new ArrayList<>();

    private EditText titleEdit;
    private EditText locationEdit;
    private EditText urlEdit;
    private EditText notesEdit;
    private SwitchMaterial allDaySwitch;
    private TextView startDateText;
    private TextView endDateText;
    private TextView startTimeText;
    private TextView endTimeText;
    private TextView recurrenceText;
    private TextView reminderValueText;
    private TextView addReminderButton;
    private LinearLayout reminderList;
    private View typeSegment;
    private LinearLayout colorPalette;
    private MaterialButton eventTypeButton;
    private MaterialButton todoTypeButton;

    public static EventEditFragment newInstance(long eventId, LocalDate date) {
        EventEditFragment fragment = new EventEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener(RESULT_RECURRENCE, this, (requestKey, result) -> {
            RecurrenceRule picked = (RecurrenceRule) result.getSerializable(RecurrenceFragment.RESULT_RULE);
            recurrenceRule = picked == null ? RecurrenceRule.none() : picked;
            bindValues();
        });
        getParentFragmentManager().setFragmentResultListener(RESULT_REMINDER, this, (requestKey, result) -> {
            Reminder reminder = (Reminder) result.getSerializable(ReminderFragment.RESULT_REMINDER);
            if (reminder != null && !reminder.isNone()) {
                reminders.add(reminder);
            }
            bindValues();
        });
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
                endDate = picked.isBefore(startDate) ? startDate : picked;
                bindValues();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_edit, container, false);
        repository = new CalendarRepository(requireContext());
        bind(view);
        if (!initialized) {
            initializeFields();
        }
        setupClicks(view);
        bindValues();
        return view;
    }

    private void bind(View view) {
        titleEdit = view.findViewById(R.id.edit_event_title);
        locationEdit = view.findViewById(R.id.edit_location);
        urlEdit = view.findViewById(R.id.edit_url);
        notesEdit = view.findViewById(R.id.edit_notes);
        allDaySwitch = view.findViewById(R.id.switch_all_day);
        startDateText = view.findViewById(R.id.tv_start_date);
        endDateText = view.findViewById(R.id.tv_end_date);
        startTimeText = view.findViewById(R.id.tv_start_time);
        endTimeText = view.findViewById(R.id.tv_end_time);
        recurrenceText = view.findViewById(R.id.tv_repeat_value);
        reminderValueText = view.findViewById(R.id.tv_reminder_value);
        addReminderButton = view.findViewById(R.id.btn_add_reminder);
        reminderList = view.findViewById(R.id.reminder_list);
        typeSegment = view.findViewById(R.id.type_segment);
        colorPalette = view.findViewById(R.id.color_palette);
        eventTypeButton = view.findViewById(R.id.btn_event_type);
        todoTypeButton = view.findViewById(R.id.btn_todo_type);
    }

    private void initializeFields() {
        eventId = requireArguments().getLong(ARG_EVENT_ID);
        startDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        endDate = startDate;
        if (eventId > 0) {
            CalendarEvent event = repository.getEvent(eventId);
            if (event != null) {
                title = event.getTitle();
                notes = event.getDescription() == null ? "" : event.getDescription();
                location = event.getLocation() == null ? "" : event.getLocation();
                url = event.getUrl() == null ? "" : event.getUrl();
                selectedColor = normalizeColor(event.getColor());
                allDay = event.isAllDay();
                startDate = event.getStartDateTime().toLocalDate();
                endDate = event.getEndDateTime() == null ? startDate : event.getEndDateTime().toLocalDate();
                startTime = event.getStartDateTime().toLocalTime();
                endTime = event.getEndDateTime() == null ? startTime.plusHours(1) : event.getEndDateTime().toLocalTime();
                recurrenceRule = event.getRecurrenceRule() == null ? RecurrenceRule.none() : event.getRecurrenceRule();
                reminders.clear();
                reminders.addAll(event.getReminders());
            }
        }
        initialized = true;
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> ((MainActivity) requireActivity()).finishFullScreenOrHome());
        view.findViewById(R.id.btn_save_event).setOnClickListener(v -> save());
        startDateText.setOnClickListener(v -> openDatePicker(RESULT_START_DATE, startDate));
        endDateText.setOnClickListener(v -> openDatePicker(RESULT_END_DATE, endDate));
        startTimeText.setOnClickListener(v -> openTimePicker(true));
        endTimeText.setOnClickListener(v -> openTimePicker(false));
        view.findViewById(R.id.repeat_row).setOnClickListener(v -> {
            captureInput();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, RecurrenceFragment.newInstance(RESULT_RECURRENCE, recurrenceRule, startDate))
                    .addToBackStack("Recurrence")
                    .commit();
        });
        view.findViewById(R.id.reminder_header_row).setOnClickListener(v -> openReminderPicker());
        addReminderButton.setOnClickListener(v -> openReminderPicker());
        todoTypeButton.setOnClickListener(v -> {
            captureInput();
            ((MainActivity) requireActivity()).switchFullScreen(TodoEditFragment.newInstance(0, startDate));
        });
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
        locationEdit.setText(location);
        urlEdit.setText(url);
        notesEdit.setText(notes);
        allDaySwitch.setOnCheckedChangeListener(null);
        allDaySwitch.setChecked(allDay);
        allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            captureInput();
            allDay = isChecked;
            bindValues();
        });
        startDateText.setText(DateTimeUtils.formatDateWithDow(startDate));
        endDateText.setText(DateTimeUtils.formatDateWithDow(endDate));
        startTimeText.setText(DateTimeUtils.formatVietnameseTime(startTime));
        endTimeText.setText(DateTimeUtils.formatVietnameseTime(endTime));
        startTimeText.setVisibility(allDay ? View.GONE : View.VISIBLE);
        endTimeText.setVisibility(allDay ? View.GONE : View.VISIBLE);

        RecurrenceRule currentRule = recurrenceRule == null ? RecurrenceRule.none() : recurrenceRule;
        recurrenceText.setText(currentRule.getDisplayText());
        recurrenceText.setTextColor(requireContext().getColor(currentRule.isNone() ? R.color.text_muted : R.color.text_primary));

        typeSegment.setVisibility(eventId > 0 ? View.GONE : View.VISIBLE);
        UiUtils.selectSegment(requireContext(), eventTypeButton, todoTypeButton);
        buildColorPalette();
        renderReminders();
    }

    private void buildColorPalette() {
        colorPalette.removeAllViews();
        for (int resId : PRESET_COLOR_RES_IDS) {
            int colorInt = requireContext().getColor(resId);
            String hex = colorIntToHex(colorInt);
            colorPalette.addView(createPresetSwatch(colorInt, normalizeColor(selectedColor).equals(hex)));
        }
        colorPalette.addView(createCustomSwatch());
    }

    private View createPresetSwatch(int colorInt, boolean selected) {
        FrameLayout frame = buildSwatchFrame(selected);
        View dot = new View(requireContext());
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
        dot.setLayoutParams(dotParams);
        dot.setBackground(circleDrawable(colorInt, 0, 0));
        frame.addView(dot);
        frame.setOnClickListener(v -> {
            selectedColor = colorIntToHex(colorInt);
            buildColorPalette();
        });
        return frame;
    }

    private View createCustomSwatch() {
        boolean customSelected = !isPresetColor(selectedColor);
        FrameLayout frame = buildSwatchFrame(customSelected);
        if (customSelected) {
            View dot = new View(requireContext());
            dot.setLayoutParams(new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
            dot.setBackground(circleDrawable(Color.parseColor(normalizeColor(selectedColor)), requireContext().getColor(R.color.line), 1));
            frame.addView(dot);
        } else {
            View ring = new View(requireContext());
            ring.setLayoutParams(new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
            ring.setBackground(circleDrawable(requireContext().getColor(R.color.surface), requireContext().getColor(R.color.line), 1));
            frame.addView(ring);
        }
        frame.setOnClickListener(v -> showCustomColorDialog());
        return frame;
    }

    private FrameLayout buildSwatchFrame(boolean selected) {
        FrameLayout frame = new FrameLayout(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(32), dp(32));
        params.setMargins(dp(4), 0, dp(4), 0);
        frame.setLayoutParams(params);
        frame.setBackground(circleDrawable(
                Color.TRANSPARENT,
                selected ? requireContext().getColor(R.color.brand_orange) : Color.TRANSPARENT,
                selected ? 2 : 0
        ));
        return frame;
    }

    private void showCustomColorDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_color, null, false);
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View preview = content.findViewById(R.id.view_custom_color_preview);
        EditText hexInput = content.findViewById(R.id.edit_custom_color_hex);
        String initial = normalizeColor(selectedColor);
        hexInput.setText(initial);
        updateColorPreview(preview, initial);
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String candidate = tryNormalizeColor(s == null ? "" : s.toString().trim());
                if (candidate != null) {
                    updateColorPreview(preview, candidate);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        content.findViewById(R.id.btn_custom_color_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_custom_color_apply).setOnClickListener(v -> {
            String normalized = tryNormalizeColor(hexInput.getText().toString().trim());
            if (normalized == null) {
                Toast.makeText(requireContext(), R.string.custom_color_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            selectedColor = normalized;
            buildColorPalette();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void updateColorPreview(View preview, String hexColor) {
        int color = Color.parseColor(normalizeColor(hexColor));
        preview.setBackground(circleDrawable(color, requireContext().getColor(R.color.line), 1));
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
        for (int i = 0; i < reminders.size(); i++) {
            reminderList.addView(createReminderItem(reminders.get(i), i));
            if (i < reminders.size() - 1) {
                reminderList.addView(createReminderDivider());
            }
        }
    }

    private View createReminderItem(Reminder reminder, int index) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(40));
        row.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);

        TextView label = new TextView(requireContext());
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        label.setText(reminder.getDisplayText());
        label.setTextColor(requireContext().getColor(R.color.text_primary));
        label.setTextSize(14f);
        label.setSingleLine(true);

        ImageButton remove = new ImageButton(requireContext());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        remove.setLayoutParams(buttonParams);
        remove.setBackgroundResource(android.R.color.transparent);
        remove.setContentDescription(getString(R.string.remove_reminder));
        remove.setImageResource(R.drawable.ic_close);
        remove.setColorFilter(requireContext().getColor(R.color.text_secondary));
        remove.setOnClickListener(v -> {
            reminders.remove(index);
            renderReminders();
        });

        row.addView(label);
        row.addView(remove);
        return row;
    }

    private View createReminderDivider() {
        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        ));
        divider.setBackgroundColor(requireContext().getColor(R.color.line));
        return divider;
    }

    private void captureInput() {
        title = titleEdit.getText().toString().trim();
        location = locationEdit.getText().toString().trim();
        url = urlEdit.getText().toString().trim();
        notes = notesEdit.getText().toString().trim();
        allDay = allDaySwitch.isChecked();
    }

    private void save() {
        captureInput();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_title_error, Toast.LENGTH_SHORT).show();
            return;
        }
        LocalDateTime start = allDay ? startDate.atStartOfDay() : LocalDateTime.of(startDate, startTime);
        LocalDateTime end = allDay ? endDate.atTime(23, 59) : LocalDateTime.of(endDate, endTime);
        if (!end.isAfter(start)) {
            end = start.plusHours(1);
        }
        CalendarEvent event = new CalendarEvent();
        event.setId(eventId);
        event.setTitle(title);
        event.setDescription(notes);
        event.setColor(normalizeColor(selectedColor));
        event.setAllDay(allDay);
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setLocation(location);
        event.setUrl(url);
        event.setRecurrenceRule(recurrenceRule);
        event.setReminders(reminders);
        repository.saveEvent(event);
        ((MainActivity) requireActivity()).setSelectedDate(startDate);
        ((MainActivity) requireActivity()).finishFullScreenOrHome();
    }

    private void openDatePicker(String resultKey, LocalDate date) {
        captureInput();
        DatePickerDialogFragment
                .newInstance(resultKey, date)
                .show(getParentFragmentManager(), resultKey);
    }

    private void openTimePicker(boolean start) {
        LocalTime initial = start ? startTime : endTime;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(initial.getHour())
                .setMinute(initial.getMinute())
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTitleText(R.string.set_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            LocalTime pickedTime = LocalTime.of(picker.getHour(), picker.getMinute());
            if (start) {
                startTime = pickedTime;
            } else {
                endTime = pickedTime;
            }
            bindValues();
        });
        picker.show(getParentFragmentManager(), start ? "start_time_picker" : "end_time_picker");
    }

    private boolean isPresetColor(String hexColor) {
        String normalized = normalizeColor(hexColor);
        for (int resId : PRESET_COLOR_RES_IDS) {
            if (normalized.equals(colorIntToHex(requireContext().getColor(resId)))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeColor(String color) {
        String normalized = tryNormalizeColor(color);
        return normalized == null ? colorIntToHex(requireContext().getColor(R.color.palette_blue_1)) : normalized;
    }

    private String tryNormalizeColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return null;
        }
        String candidate = color.trim();
        if (!candidate.startsWith("#")) {
            candidate = "#" + candidate;
        }
        try {
            int parsed = Color.parseColor(candidate);
            return colorIntToHex(parsed);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String colorIntToHex(int color) {
        return String.format(Locale.US, "#%06X", (0xFFFFFF & color));
    }

    private GradientDrawable circleDrawable(int fillColor, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return UiUtils.dp(requireContext(), value);
    }
}
