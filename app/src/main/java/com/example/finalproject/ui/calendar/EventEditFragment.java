package com.example.finalproject.ui.calendar;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class EventEditFragment extends Fragment {
    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_DATE = "date";
    private static final String RESULT_RECURRENCE = "event_recurrence";
    private static final String RESULT_REMINDER = "event_reminder";
    private static final String RESULT_START_DATE = "event_start_date";
    private static final String RESULT_END_DATE = "event_end_date";

    private final String[] colors = {
            "#F7A8D7", "#F48FB1", "#CE93D8", "#90CAF9", "#4DD0E1",
            "#7EE081", "#FFB74D", "#FFD54F", "#F4A261", "#00C853", "#3366CC"
    };

    private CalendarRepository repository;
    private long eventId;
    private boolean initialized;
    private String title = "";
    private String notes = "";
    private String location = "";
    private String url = "";
    private String selectedColor = "#90CAF9";
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
            recurrenceRule = (RecurrenceRule) result.getSerializable(RecurrenceFragment.RESULT_RULE);
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
                if (endDate.isBefore(startDate)) {
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
        buildColorPalette();
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
                selectedColor = event.getColor();
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
        view.findViewById(R.id.btn_back).setOnClickListener(v -> ((MainActivity) requireActivity()).finishToHome());
        view.findViewById(R.id.btn_save_event).setOnClickListener(v -> save());
        startDateText.setOnClickListener(v -> openDatePicker(RESULT_START_DATE, startDate));
        endDateText.setOnClickListener(v -> openDatePicker(RESULT_END_DATE, endDate));
        startTimeText.setOnClickListener(v -> openTimePicker(true));
        endTimeText.setOnClickListener(v -> openTimePicker(false));
        allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            captureInput();
            allDay = isChecked;
            bindValues();
        });
        view.findViewById(R.id.repeat_row).setOnClickListener(v -> {
            captureInput();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, RecurrenceFragment.newInstance(RESULT_RECURRENCE, recurrenceRule, startDate))
                    .addToBackStack("Recurrence")
                    .commit();
        });
        view.findViewById(R.id.reminder_section).setOnClickListener(v -> openReminderPicker());
        view.findViewById(R.id.btn_add_reminder).setOnClickListener(v -> openReminderPicker());
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

    private void buildColorPalette() {
        colorPalette.removeAllViews();
        for (String color : colors) {
            TextView dot = new TextView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(UiUtils.dp(requireContext(), 26), UiUtils.dp(requireContext(), 26));
            params.setMargins(5, 0, 5, 0);
            dot.setLayoutParams(params);
            dot.setOnClickListener(v -> {
                selectedColor = color;
                buildColorPalette();
            });
            int fill = UiUtils.safeColor(color, requireContext().getColor(R.color.event_blue));
            int stroke = color.equals(selectedColor) ? requireContext().getColor(R.color.black) : fill;
            dot.setBackground(UiUtils.roundedStroke(fill, stroke, 13, requireContext()));
            colorPalette.addView(dot);
        }
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
        recurrenceText.setText((recurrenceRule == null ? RecurrenceRule.none() : recurrenceRule).getDisplayText() + " ›");
        renderReminders();
        typeSegment.setVisibility(eventId > 0 ? View.GONE : View.VISIBLE);
        UiUtils.selectSegment(requireContext(), eventTypeButton, todoTypeButton);
        buildColorPalette();
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
        event.setColor(selectedColor);
        event.setAllDay(allDay);
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setLocation(location);
        event.setUrl(url);
        event.setRecurrenceRule(recurrenceRule);
        event.setReminders(reminders);
        repository.saveEvent(event);
        ((MainActivity) requireActivity()).setSelectedDate(startDate);
        ((MainActivity) requireActivity()).finishToHome();
    }

    private void openDatePicker(String resultKey, LocalDate date) {
        captureInput();
        DatePickerDialogFragment
                .newInstance(resultKey, date)
                .show(getParentFragmentManager(), resultKey);
    }

    private void openTimePicker(boolean start) {
        LocalTime initial = start ? startTime : endTime;
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_picker, null, false);
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        NumberPicker hourPicker = content.findViewById(R.id.picker_hour);
        NumberPicker minutePicker = content.findViewById(R.id.picker_minute);
        NumberPicker amPmPicker = content.findViewById(R.id.picker_ampm);
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(value -> String.format("%02d", value));
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"SA", "CH"});
        int hour = initial.getHour();
        amPmPicker.setValue(hour >= 12 ? 1 : 0);
        int hour12 = hour % 12;
        hourPicker.setValue(hour12 == 0 ? 12 : hour12);
        minutePicker.setValue(initial.getMinute());
        content.findViewById(R.id.btn_time_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_time_ok).setOnClickListener(v -> {
            int pickedHour = hourPicker.getValue() % 12;
            if (amPmPicker.getValue() == 1) {
                pickedHour += 12;
            }
            if (start) {
                startTime = LocalTime.of(pickedHour, minutePicker.getValue());
            } else {
                endTime = LocalTime.of(pickedHour, minutePicker.getValue());
            }
            bindValues();
            dialog.dismiss();
        });
        dialog.show();
    }
}
