package com.example.finalproject.ui.calendar;

import android.app.Dialog;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
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
import android.view.WindowManager;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
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
            R.color.palette_blue_1,
            R.color.palette_sky_2,
            R.color.palette_green_1,
            R.color.palette_orange_1,
            R.color.palette_yellow_1,
            R.color.brand_orange,
            R.color.palette_green_2,
            R.color.palette_blue_2
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
    private ViewGroup rootView;
    private ScrollView contentScroll;
    private View titleRow;
    private View locationRow;
    private View urlRow;
    private View notesRow;
    private View keyboardSpacer;
    private View startTimeContainer;
    private View endTimeContainer;
    private LinearLayout colorPalette;
    private MaterialButton eventTypeButton;
    private MaterialButton todoTypeButton;
    private boolean timePickerShowing;
    private Integer previousSoftInputMode;
    private ValueAnimator startTimeAnimator;
    private ValueAnimator endTimeAnimator;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
    private int baseKeyboardSpacerHeight;
    private int keyboardInset;
    private boolean keyboardVisible;
    private Runnable pendingKeyboardScrollRunnable;
    private View pendingKeyboardScrollAnchor;

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
        restoreSoftInputMode();
    }

    @Override
    public void onPause() {
        restoreSoftInputMode();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        detachKeyboardScrollSupport();
        super.onDestroyView();
    }

    private void bind(View view) {
        rootView = view.findViewById(R.id.event_edit_root);
        contentScroll = view.findViewById(R.id.event_edit_scroll);
        titleRow = view.findViewById(R.id.title_row);
        locationRow = view.findViewById(R.id.location_row);
        urlRow = view.findViewById(R.id.url_row);
        notesRow = view.findViewById(R.id.notes_row);
        keyboardSpacer = view.findViewById(R.id.event_edit_keyboard_spacer);
        titleEdit = view.findViewById(R.id.edit_event_title);
        locationEdit = view.findViewById(R.id.edit_location);
        urlEdit = view.findViewById(R.id.edit_url);
        notesEdit = view.findViewById(R.id.edit_notes);
        allDaySwitch = view.findViewById(R.id.switch_all_day);
        startDateText = view.findViewById(R.id.tv_start_date);
        endDateText = view.findViewById(R.id.tv_end_date);
        startTimeText = view.findViewById(R.id.tv_start_time);
        endTimeText = view.findViewById(R.id.tv_end_time);
        startTimeContainer = view.findViewById(R.id.start_time_container);
        endTimeContainer = view.findViewById(R.id.end_time_container);
        recurrenceText = view.findViewById(R.id.tv_repeat_value);
        reminderValueText = view.findViewById(R.id.tv_reminder_value);
        addReminderButton = view.findViewById(R.id.btn_add_reminder);
        reminderList = view.findViewById(R.id.reminder_list);
        typeSegment = view.findViewById(R.id.type_segment);
        colorPalette = view.findViewById(R.id.color_palette);
        eventTypeButton = view.findViewById(R.id.btn_event_type);
        todoTypeButton = view.findViewById(R.id.btn_todo_type);
    }

    private void installKeyboardScrollSupport() {
        if (rootView == null || keyboardLayoutListener != null) {
            return;
        }
        baseKeyboardSpacerHeight = keyboardSpacer != null && keyboardSpacer.getLayoutParams() != null
                ? keyboardSpacer.getLayoutParams().height
                : dp(28);
        keyboardLayoutListener = () -> {
            if (!isAdded() || rootView == null) {
                return;
            }
            Rect visibleFrame = new Rect();
            rootView.getWindowVisibleDisplayFrame(visibleFrame);
            int fullHeight = rootView.getRootView().getHeight();
            int overlap = Math.max(0, fullHeight - visibleFrame.bottom);
            boolean nowVisible = overlap > dp(120);
            int desiredInset = nowVisible ? overlap : 0;
            boolean visibilityChanged = nowVisible != keyboardVisible;
            if (desiredInset == keyboardInset && nowVisible == keyboardVisible) {
                return;
            }
            keyboardInset = desiredInset;
            keyboardVisible = nowVisible;
            int spacerHeight = baseKeyboardSpacerHeight
                    + (keyboardVisible ? Math.max(dp(36), keyboardInset - dp(56)) : 0);
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

    private View resolveFocusedAnchor() {
        if (isFieldFocused(notesEdit)) {
            return notesEdit;
        }
        if (isFieldFocused(urlEdit)) {
            return urlEdit;
        }
        if (isFieldFocused(locationEdit)) {
            return locationEdit;
        }
        if (isFieldFocused(titleEdit)) {
            return titleEdit;
        }
        return null;
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
        attachKeyboardFieldBehavior(titleEdit, titleEdit, 60L);
        attachKeyboardFieldBehavior(locationEdit, locationEdit, 90L);
        attachKeyboardFieldBehavior(urlEdit, urlEdit, 90L);
        attachKeyboardFieldBehavior(notesEdit, notesEdit, 140L);
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
        setTextIfChanged(titleEdit, title);
        setTextIfChanged(locationEdit, location);
        setTextIfChanged(urlEdit, url);
        setTextIfChanged(notesEdit, notes);
        allDaySwitch.setOnCheckedChangeListener(null);
        allDaySwitch.setChecked(allDay);
        allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            captureInput();
            allDay = isChecked;
            updateTimeVisibility(true);
        });
        startDateText.setText(DateTimeUtils.formatDateWithDow(startDate));
        endDateText.setText(DateTimeUtils.formatDateWithDow(endDate));
        startTimeText.setText(DateTimeUtils.formatVietnameseTime(startTime));
        endTimeText.setText(DateTimeUtils.formatVietnameseTime(endTime));
        updateTimeVisibility(false);

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
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER);
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
            dot.setLayoutParams(new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
            dot.setBackground(circleDrawable(Color.parseColor(normalizeColor(selectedColor)), requireContext().getColor(R.color.line), 1));
            frame.addView(dot);
        } else {
            View ring = new View(requireContext());
            ring.setLayoutParams(new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
            ring.setBackground(circleDrawable(requireContext().getColor(R.color.surface), requireContext().getColor(R.color.line), 1));
            frame.addView(ring);
        }
        frame.setOnClickListener(v -> showCustomColorDialog());
        return frame;
    }

    private FrameLayout buildSwatchFrame(boolean selected) {
        FrameLayout frame = new FrameLayout(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(28), dp(28));
        params.setMargins(dp(5), 0, dp(5), 0);
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
            dialog.getWindow().setLayout(dp(336), ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();
            attributes.dimAmount = 0.28f;
            dialog.getWindow().setAttributes(attributes);
        }

        View preview = content.findViewById(R.id.view_custom_color_preview);
        EditText hexInput = content.findViewById(R.id.edit_custom_color_hex);
        SeekBar redSeek = content.findViewById(R.id.seek_custom_color_red);
        SeekBar greenSeek = content.findViewById(R.id.seek_custom_color_green);
        SeekBar blueSeek = content.findViewById(R.id.seek_custom_color_blue);
        TextView redValue = content.findViewById(R.id.tv_custom_color_red_value);
        TextView greenValue = content.findViewById(R.id.tv_custom_color_green_value);
        TextView blueValue = content.findViewById(R.id.tv_custom_color_blue_value);
        String initial = normalizeColor(selectedColor);
        final boolean[] syncing = {false};
        int initialColor = Color.parseColor(initial);
        bindCustomColorInputs(
                preview,
                hexInput,
                redSeek,
                greenSeek,
                blueSeek,
                redValue,
                greenValue,
                blueValue,
                initialColor,
                syncing
        );
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (syncing[0]) {
                    return;
                }
                String candidate = tryNormalizeColor(s == null ? "" : s.toString().trim());
                if (candidate != null) {
                    bindCustomColorInputs(
                            preview,
                            hexInput,
                            redSeek,
                            greenSeek,
                            blueSeek,
                            redValue,
                            greenValue,
                            blueValue,
                            Color.parseColor(candidate),
                            syncing
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        SeekBar.OnSeekBarChangeListener sliderListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (syncing[0]) {
                    return;
                }
                int color = Color.rgb(redSeek.getProgress(), greenSeek.getProgress(), blueSeek.getProgress());
                bindCustomColorInputs(
                        preview,
                        hexInput,
                        redSeek,
                        greenSeek,
                        blueSeek,
                        redValue,
                        greenValue,
                        blueValue,
                        color,
                        syncing
                );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        redSeek.setOnSeekBarChangeListener(sliderListener);
        greenSeek.setOnSeekBarChangeListener(sliderListener);
        blueSeek.setOnSeekBarChangeListener(sliderListener);
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

    private void bindCustomColorInputs(View preview,
                                       EditText hexInput,
                                       SeekBar redSeek,
                                       SeekBar greenSeek,
                                       SeekBar blueSeek,
                                       TextView redValue,
                                       TextView greenValue,
                                       TextView blueValue,
                                       int color,
                                       boolean[] syncing) {
        syncing[0] = true;
        String normalized = colorIntToHex(color);
        updateColorPreview(preview, normalized);
        if (!normalized.equals(hexInput.getText().toString())) {
            hexInput.setText(normalized);
            hexInput.setSelection(normalized.length());
        }
        updateChannel(redSeek, redValue, Color.red(color));
        updateChannel(greenSeek, greenValue, Color.green(color));
        updateChannel(blueSeek, blueValue, Color.blue(color));
        syncing[0] = false;
    }

    private void updateChannel(SeekBar seekBar, TextView valueView, int value) {
        if (seekBar.getProgress() != value) {
            seekBar.setProgress(value);
        }
        valueView.setText(String.valueOf(value));
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
                hairline()
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
        if (getParentFragmentManager().findFragmentByTag(resultKey) != null) {
            return;
        }
        DatePickerDialogFragment
                .newInstance(resultKey, date)
                .show(getParentFragmentManager(), resultKey);
    }

    private void openTimePicker(boolean start) {
        if (timePickerShowing) {
            return;
        }
        String tag = start ? "start_time_picker" : "end_time_picker";
        if (getParentFragmentManager().findFragmentByTag(tag) != null) {
            return;
        }
        LocalTime initial = start ? startTime : endTime;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(initial.getHour())
                .setMinute(initial.getMinute())
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTitleText(R.string.set_time)
                .build();
        timePickerShowing = true;
        picker.addOnPositiveButtonClickListener(v -> {
            LocalTime pickedTime = LocalTime.of(picker.getHour(), picker.getMinute());
            if (start) {
                startTime = pickedTime;
            } else {
                endTime = pickedTime;
            }
            bindValues();
        });
        picker.addOnDismissListener(dialog -> timePickerShowing = false);
        picker.show(getParentFragmentManager(), tag);
    }

    private void updateTimeVisibility(boolean animate) {
        if (startTimeContainer == null || endTimeContainer == null) {
            return;
        }
        if (animate) {
            animateTimeContainer(startTimeContainer, !allDay, true);
            animateTimeContainer(endTimeContainer, !allDay, false);
        } else {
            applyTimeContainerState(startTimeContainer, !allDay);
            applyTimeContainerState(endTimeContainer, !allDay);
        }
    }

    private void animateTimeContainer(View container, boolean show, boolean startContainer) {
        if (container == null) {
            return;
        }
        int expandedWidth = dp(72);
        ViewGroup.LayoutParams params = container.getLayoutParams();
        if (params == null) {
            return;
        }
        ValueAnimator runningAnimator = startContainer ? startTimeAnimator : endTimeAnimator;
        if (runningAnimator != null) {
            runningAnimator.cancel();
        }

        int currentWidth = params.width > 0 ? params.width : (container.getVisibility() == View.VISIBLE ? expandedWidth : 0);
        int targetWidth = show ? expandedWidth : 0;
        if (currentWidth == targetWidth && ((show && container.getVisibility() == View.VISIBLE) || (!show && container.getVisibility() != View.VISIBLE))) {
            return;
        }
        if (show) {
            container.setVisibility(View.VISIBLE);
            container.setAlpha(container.getAlpha() > 0f ? container.getAlpha() : 0f);
        }

        ValueAnimator animator = ValueAnimator.ofInt(currentWidth, targetWidth);
        animator.setDuration(260L);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int width = (int) animation.getAnimatedValue();
            float progress = expandedWidth == 0 ? 1f : Math.min(1f, width / (float) expandedWidth);
            ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
            layoutParams.width = width;
            container.setLayoutParams(layoutParams);
            container.setAlpha(show ? progress : Math.max(0f, 1f - progress));
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
                layoutParams.width = targetWidth;
                container.setLayoutParams(layoutParams);
                container.setAlpha(show ? 1f : 0f);
                container.setVisibility(show ? View.VISIBLE : View.GONE);
                if (startContainer) {
                    startTimeAnimator = null;
                } else {
                    endTimeAnimator = null;
                }
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                if (startContainer) {
                    startTimeAnimator = null;
                } else {
                    endTimeAnimator = null;
                }
            }
        });
        if (startContainer) {
            startTimeAnimator = animator;
        } else {
            endTimeAnimator = animator;
        }
        animator.start();
    }

    private void applyTimeContainerState(View container, boolean visible) {
        if (container == null || container.getLayoutParams() == null) {
            return;
        }
        ViewGroup.LayoutParams params = container.getLayoutParams();
        params.width = visible ? dp(72) : 0;
        container.setLayoutParams(params);
        container.setAlpha(visible ? 1f : 0f);
        container.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void attachKeyboardFieldBehavior(EditText field, View anchor, long focusDelayMs) {
        if (field == null || anchor == null) {
            return;
        }
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                enableKeyboardInputMode();
                if (keyboardVisible) {
                    requestFieldVisibility(anchor, focusDelayMs, true);
                }
            } else {
                v.post(this::restoreSoftInputModeIfNeeded);
            }
        });
        field.setOnClickListener(v -> {
            enableKeyboardInputMode();
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
                if (field.hasFocus() && keyboardVisible) {
                    requestFieldVisibility(anchor, 0L, false);
                }
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
            Rect visibleFrame = new Rect();
            rootView.getWindowVisibleDisplayFrame(visibleFrame);

            int[] location = new int[2];
            anchor.getLocationOnScreen(location);
            int anchorTopOnScreen = location[1];
            int anchorBottomOnScreen = anchorTopOnScreen + anchor.getHeight();
            int desiredBottom = visibleFrame.bottom - dp(18);

            if (anchorBottomOnScreen <= desiredBottom) {
                return;
            }
            int delta = anchorBottomOnScreen - desiredBottom;
            if (delta < dp(6)) {
                return;
            }

            int contentHeight = contentScroll.getChildCount() > 0 ? contentScroll.getChildAt(0).getHeight() : 0;
            int maxScroll = Math.max(0, contentHeight - contentScroll.getHeight());
            int targetScroll = clamp(contentScroll.getScrollY() + delta, 0, maxScroll);
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

    private void restoreSoftInputModeIfNeeded() {
        if (!isAnyKeyboardFieldFocused()) {
            restoreSoftInputMode();
        }
    }

    private boolean isAnyKeyboardFieldFocused() {
        return isFieldFocused(titleEdit)
                || isFieldFocused(locationEdit)
                || isFieldFocused(urlEdit)
                || isFieldFocused(notesEdit);
    }

    private boolean isFieldFocused(EditText field) {
        return field != null && field.hasFocus();
    }

    private void setTextIfChanged(EditText editText, String value) {
        String safe = value == null ? "" : value;
        String current = editText.getText() == null ? "" : editText.getText().toString();
        if (!safe.equals(current)) {
            editText.setText(safe);
            editText.setSelection(editText.getText().length());
        }
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

    private int hairline() {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.max(1, Math.round(0.5f * density));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
