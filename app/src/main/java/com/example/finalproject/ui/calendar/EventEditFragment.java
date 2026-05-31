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
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.repository.CalendarRepository;
import com.example.finalproject.ui.common.AlphaSliderView;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.ScreenBackHandler;
import com.example.finalproject.ui.common.HueSliderView;
import com.example.finalproject.ui.common.SpectrumColorView;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.ui.common.WheelPickerView;
import com.example.finalproject.ui.todo.TodoEditFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EventEditFragment extends Fragment implements ScreenBackHandler {
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
            R.color.brand_orange,
            R.color.palette_neutral_1,
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
    private LocalTime startTime = LocalTime.of(8, 0);
    private LocalTime endTime = LocalTime.of(17, 0);
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
    private View customColorTrigger;
    private View customColorTriggerPreview;
    private View customColorTriggerCheck;
    private TextView customColorTriggerLabel;
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
    private String initialStateSignature;

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
        customColorTrigger = view.findViewById(R.id.custom_color_trigger);
        customColorTriggerPreview = view.findViewById(R.id.view_custom_color_trigger_preview);
        customColorTriggerCheck = view.findViewById(R.id.iv_custom_color_trigger_check);
        customColorTriggerLabel = view.findViewById(R.id.tv_custom_color_trigger_label);
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
                    + (keyboardVisible ? Math.max(dp(28), keyboardInset - dp(20)) : 0);
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
        initialStateSignature = buildStateSignature();
        initialized = true;
    }

    private void setupClicks(View view) {
        UiUtils.setDebouncedClickListener(view.findViewById(R.id.btn_back), () -> ((MainActivity) requireActivity()).handleActivityBackPressed());
        UiUtils.setDebouncedClickListener(view.findViewById(R.id.btn_save_event), this::save);
        UiUtils.setDebouncedClickListener(startDateText, () -> openDatePicker(RESULT_START_DATE, startDate));
        UiUtils.setDebouncedClickListener(endDateText, () -> openDatePicker(RESULT_END_DATE, endDate));
        UiUtils.setDebouncedClickListener(startTimeText, () -> openTimePicker(true));
        UiUtils.setDebouncedClickListener(endTimeText, () -> openTimePicker(false));
        UiUtils.setDebouncedClickListener(view.findViewById(R.id.repeat_row), () -> {
            captureInput();
            ((MainActivity) requireActivity()).pushFullScreenFragment(
                    RecurrenceFragment.newInstance(RESULT_RECURRENCE, recurrenceRule, startDate),
                    "Recurrence"
            );
        });
        UiUtils.setDebouncedClickListener(view.findViewById(R.id.reminder_header_row), this::openReminderPicker);
        UiUtils.setDebouncedClickListener(addReminderButton, this::openReminderPicker);
        UiUtils.setDebouncedClickListener(todoTypeButton, () -> {
            captureInput();
            ((MainActivity) requireActivity()).switchFullScreen(TodoEditFragment.newInstance(0, startDate));
        });
        UiUtils.setDebouncedClickListener(customColorTrigger, this::showCustomColorDialog);
        attachKeyboardFieldBehavior(titleEdit, titleEdit, 60L);
        attachKeyboardFieldBehavior(locationEdit, locationEdit, 90L);
        attachKeyboardFieldBehavior(urlEdit, urlEdit, 90L);
        attachKeyboardFieldBehavior(notesEdit, notesEdit, 140L);
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
        updateCustomColorTrigger();
    }

    private View createPresetSwatch(int colorInt, boolean selected) {
        FrameLayout frame = buildSwatchFrame(selected);
        View dot = new View(requireContext());
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
        dot.setLayoutParams(dotParams);
        dot.setBackground(circleDrawable(colorInt, UiUtils.adaptiveStrokeColor(colorInt, requireContext()), 1));
        frame.addView(dot);
        frame.setOnClickListener(v -> {
            selectedColor = colorIntToHex(colorInt);
            buildColorPalette();
        });
        return frame;
    }

    private FrameLayout buildSwatchFrame(boolean selected) {
        FrameLayout frame = new FrameLayout(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(26), dp(26));
        params.setMargins(dp(3), 0, dp(3), 0);
        frame.setLayoutParams(params);
        frame.setBackground(circleDrawable(
                Color.TRANSPARENT,
                selected ? requireContext().getColor(R.color.brand_orange) : Color.TRANSPARENT,
                selected ? 2 : 0
        ));
        return frame;
    }

    private void updateCustomColorTrigger() {
        boolean customSelected = !isPresetColor(selectedColor);
        int fillColor = Color.parseColor(normalizeColor(selectedColor));
        int surface = requireContext().getColor(R.color.surface);
        int line = requireContext().getColor(R.color.line);
        customColorTrigger.setBackground(customSelected
                ? UiUtils.roundedStroke(requireContext().getColor(R.color.brand_orange_light), requireContext().getColor(R.color.brand_orange), 18, requireContext())
                : UiUtils.roundedStroke(surface, line, 18, requireContext()));
        customColorTriggerPreview.setBackground(circleDrawable(
                customSelected ? fillColor : surface,
                customSelected ? UiUtils.adaptiveStrokeColor(fillColor, requireContext()) : line,
                1
        ));
        customColorTriggerLabel.setTextColor(requireContext().getColor(customSelected ? R.color.brand_orange_dark : R.color.text_secondary));
        customColorTriggerLabel.setText(R.string.custom);
        customColorTriggerCheck.setVisibility(customSelected ? View.VISIBLE : View.GONE);
    }

    private void showCustomColorDialog() {
        Dialog dialog = new Dialog(new android.view.ContextThemeWrapper(requireContext(), R.style.Haruka_LightDialog));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_color, null, false);
        dialog.setContentView(content);
        UiUtils.styleDialogWindow(dialog, resolveCustomColorDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, 0.28f);

        View preview = content.findViewById(R.id.view_custom_color_preview);
        View inlinePreview = content.findViewById(R.id.view_custom_color_inline_preview);
        SpectrumColorView spectrumView = content.findViewById(R.id.view_custom_color_spectrum);
        HueSliderView hueSliderView = content.findViewById(R.id.view_custom_color_hue);
        AlphaSliderView alphaSliderView = content.findViewById(R.id.view_custom_color_alpha);
        EditText hexEdit = content.findViewById(R.id.edit_custom_color_hex);
        TextView opacityValue = content.findViewById(R.id.tv_custom_color_opacity);
        String initial = normalizeColor(selectedColor);
        int initialColor = Color.parseColor(initial);
        final int[] alphaHolder = {Color.alpha(initialColor)};
        final int[] rgbHolder = {Color.rgb(Color.red(initialColor), Color.green(initialColor), Color.blue(initialColor))};
        final boolean[] syncingHex = {false};

        Runnable syncViews = () -> {
            int combined = Color.argb(alphaHolder[0], Color.red(rgbHolder[0]), Color.green(rgbHolder[0]), Color.blue(rgbHolder[0]));
            updateColorPreview(preview, combined);
            updateColorPreview(inlinePreview, combined);
            alphaSliderView.setBaseColor(rgbHolder[0]);
            alphaSliderView.setAlphaValue(alphaHolder[0]);
            opacityValue.setText(getString(R.string.custom_color_opacity_value, Math.round(alphaHolder[0] * 100f / 255f)));
            String rgbHex = String.format(Locale.US, "#%06X", (0xFFFFFF & rgbHolder[0]));
            String currentText = hexEdit.getText() == null ? "" : hexEdit.getText().toString();
            if (!rgbHex.equalsIgnoreCase(currentText)) {
                syncingHex[0] = true;
                hexEdit.setText(rgbHex);
                hexEdit.setSelection(rgbHex.length());
                syncingHex[0] = false;
            }
        };

        spectrumView.setOnColorSelectedListener(color -> {
            rgbHolder[0] = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
            syncViews.run();
        });
        hueSliderView.setOnHueChangeListener(hue -> {
            spectrumView.setHue(hue);
            int updated = spectrumView.getSelectedColor();
            rgbHolder[0] = Color.rgb(Color.red(updated), Color.green(updated), Color.blue(updated));
            syncViews.run();
        });
        alphaSliderView.setOnAlphaChangeListener(alpha -> {
            alphaHolder[0] = alpha;
            syncViews.run();
        });
        hexEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (syncingHex[0]) {
                    return;
                }
                String normalized = normalizeRgbHex(s == null ? null : s.toString());
                if (normalized == null) {
                    return;
                }
                int parsedColor = Color.parseColor(normalized);
                rgbHolder[0] = Color.rgb(Color.red(parsedColor), Color.green(parsedColor), Color.blue(parsedColor));
                float[] hsv = new float[3];
                Color.colorToHSV(rgbHolder[0], hsv);
                hueSliderView.setHue(hsv[0]);
                spectrumView.setSelectedColor(rgbHolder[0]);
                syncViews.run();
            }
        });
        spectrumView.post(() -> {
            float[] hsv = new float[3];
            Color.colorToHSV(rgbHolder[0], hsv);
            hueSliderView.setHue(hsv[0]);
            spectrumView.setSelectedColor(rgbHolder[0]);
            alphaSliderView.setBaseColor(rgbHolder[0]);
            alphaSliderView.setAlphaValue(alphaHolder[0]);
            syncViews.run();
        });
        UiUtils.setDebouncedClickListener(content.findViewById(R.id.btn_custom_color_cancel), dialog::dismiss);
        UiUtils.setDebouncedClickListener(content.findViewById(R.id.btn_custom_color_apply), () -> {
            selectedColor = colorIntToHex(Color.argb(alphaHolder[0], Color.red(rgbHolder[0]), Color.green(rgbHolder[0]), Color.blue(rgbHolder[0])));
            buildColorPalette();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void updateColorPreview(View preview, String hexColor) {
        int color = Color.parseColor(normalizeColor(hexColor));
        updateColorPreview(preview, color);
    }

    private void updateColorPreview(View preview, int color) {
        if (preview instanceof TextView) {
            TextView chip = (TextView) preview;
            chip.setTextColor(UiUtils.readableTextColor(color, requireContext()));
            chip.setBackground(UiUtils.adaptiveEventBackground(color, 16, requireContext()));
            return;
        }
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
        UiUtils.setDebouncedClickListener(remove, () -> {
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

    @Override
    public boolean onHandleBackPressed() {
        if (!isAdded()) {
            return true;
        }
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

    private boolean hasUnsavedChanges() {
        return !buildStateSignature().equals(initialStateSignature == null ? "" : initialStateSignature);
    }

    private String buildStateSignature() {
        StringBuilder builder = new StringBuilder();
        builder.append(valueOrEmpty(title)).append('|')
                .append(valueOrEmpty(location)).append('|')
                .append(valueOrEmpty(url)).append('|')
                .append(valueOrEmpty(notes)).append('|')
                .append(normalizeColor(selectedColor)).append('|')
                .append(allDay).append('|')
                .append(valueOrEmpty(startDate)).append('|')
                .append(valueOrEmpty(endDate)).append('|')
                .append(valueOrEmpty(startTime)).append('|')
                .append(valueOrEmpty(endTime)).append('|')
                .append(recurrenceSignature(recurrenceRule)).append('|')
                .append(reminderSignature(reminders));
        return builder.toString();
    }

    private String recurrenceSignature(RecurrenceRule rule) {
        RecurrenceRule currentRule = rule == null ? RecurrenceRule.none() : rule;
        return valueOrEmpty(currentRule.getFreq()) + '|'
                + currentRule.getIntervalValue() + '|'
                + valueOrEmpty(currentRule.getByDay()) + '|'
                + valueOrEmpty(currentRule.getByMonthDay()) + '|'
                + valueOrEmpty(currentRule.getByMonth()) + '|'
                + valueOrEmpty(currentRule.getBySetPos()) + '|'
                + valueOrEmpty(currentRule.getMonthlyPatternType()) + '|'
                + valueOrEmpty(currentRule.getEndType()) + '|'
                + valueOrEmpty(currentRule.getEndDate()) + '|'
                + valueOrEmpty(currentRule.getOccurrenceCount());
    }

    private String reminderSignature(List<Reminder> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Reminder item : items) {
            builder.append(valueOrEmpty(item == null ? null : item.getReminderType())).append(':')
                    .append(valueOrEmpty(item == null ? null : item.getOffsetValue())).append(':')
                    .append(valueOrEmpty(item == null ? null : item.getOffsetUnit())).append(':')
                    .append(valueOrEmpty(item == null ? null : item.getTimeOfDay())).append(':')
                    .append(item != null && item.isEnabled())
                    .append(';');
        }
        return builder.toString();
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
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
        LocalTime initial = start ? startTime : endTime;
        Dialog dialog = new Dialog(new android.view.ContextThemeWrapper(requireContext(), R.style.Haruka_LightDialog));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_picker, null, false);
        UiUtils.disableForceDark(content);
        WheelPickerView hourPicker = content.findViewById(R.id.picker_hour);
        WheelPickerView minutePicker = content.findViewById(R.id.picker_minute);
        WheelPickerView ampmPicker = content.findViewById(R.id.picker_ampm);

        List<String> hourItems = new ArrayList<>();
        for (int hour = 1; hour <= 12; hour++) {
            hourItems.add(String.valueOf(hour));
        }
        List<String> minuteItems = new ArrayList<>();
        for (int minute = 0; minute < 60; minute++) {
            minuteItems.add(String.format(Locale.US, "%02d", minute));
        }

        int initialHourIndex = toDisplayHour(initial.getHour()) - 1;
        int initialMinuteIndex = initial.getMinute();
        int initialAmPmIndex = initial.getHour() < 12 ? 0 : 1;
        hourPicker.setItems(hourItems);
        hourPicker.setSelectedIndex(initialHourIndex);
        minutePicker.setItems(minuteItems);
        minutePicker.setSelectedIndex(initialMinuteIndex);
        ampmPicker.setItems(Arrays.asList("SA", "CH"));
        ampmPicker.setSelectedIndex(initialAmPmIndex);

        timePickerShowing = true;
        dialog.setOnDismissListener(d -> timePickerShowing = false);
        UiUtils.setDebouncedClickListener(content.findViewById(R.id.btn_time_cancel), dialog::dismiss);
        UiUtils.setDebouncedClickListener(content.findViewById(R.id.btn_time_ok), () -> {
            LocalTime pickedTime = LocalTime.of(
                    fromDisplayHour(hourPicker.getSelectedIndex() + 1, ampmPicker.getSelectedIndex() == 1),
                    minutePicker.getSelectedIndex()
            );
            if (start) {
                startTime = pickedTime;
            } else {
                endTime = pickedTime;
            }
            bindValues();
            dialog.dismiss();
        });
        dialog.setContentView(content);
        UiUtils.styleDialogWindow(dialog, UiUtils.dp(requireContext(), 312), ViewGroup.LayoutParams.WRAP_CONTENT, 0.28f);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        content.post(() -> {
            hourPicker.setSelectedIndex(initialHourIndex);
            minutePicker.setSelectedIndex(initialMinuteIndex);
            ampmPicker.setSelectedIndex(initialAmPmIndex);
        });
    }

    private int toDisplayHour(int hour24) {
        int hour12 = hour24 % 12;
        return hour12 == 0 ? 12 : hour12;
    }

    private int fromDisplayHour(int hour12, boolean pm) {
        int normalized = hour12 % 12;
        return pm ? normalized + 12 : normalized;
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
        int expandedWidth = dp(96);
        float slideDistance = dp(8);
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
            params.width = currentWidth;
            container.setLayoutParams(params);
            container.setVisibility(View.VISIBLE);
            container.setAlpha(0f);
            container.setTranslationX(slideDistance);
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(260L);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
            if (show) {
                float widthPhase = Math.min(1f, progress / 0.55f);
                int width = lerp(currentWidth, targetWidth, widthPhase);
                float alphaPhase = progress <= 0.55f ? 0f : (progress - 0.55f) / 0.45f;
                layoutParams.width = width;
                container.setAlpha(Math.min(1f, alphaPhase));
                container.setTranslationX(slideDistance * (1f - Math.min(1f, alphaPhase)));
            } else {
                float fadePhase = Math.min(1f, progress / 0.45f);
                float widthPhase = progress <= 0.45f ? 0f : (progress - 0.45f) / 0.55f;
                int width = progress <= 0.45f ? currentWidth : lerp(currentWidth, targetWidth, widthPhase);
                layoutParams.width = width;
                container.setAlpha(1f - fadePhase);
                container.setTranslationX(slideDistance * fadePhase);
            }
            container.setLayoutParams(layoutParams);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
                layoutParams.width = targetWidth;
                container.setLayoutParams(layoutParams);
                container.setAlpha(show ? 1f : 0f);
                container.setTranslationX(0f);
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
        params.width = visible ? dp(96) : 0;
        container.setLayoutParams(params);
        container.setAlpha(visible ? 1f : 0f);
        container.setTranslationX(0f);
        container.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private int lerp(int start, int end, float fraction) {
        return Math.round(start + (end - start) * Math.max(0f, Math.min(1f, fraction)));
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
            }
        });
        field.setOnClickListener(v -> {
            enableKeyboardInputMode();
            if (keyboardVisible && field.hasFocus()) {
                requestFieldVisibility(anchor, 0L, false);
            }
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
                    long delay = field == notesEdit ? 24L : 0L;
                    requestFieldVisibility(anchor, delay, false);
                    if (field == notesEdit) {
                        anchor.postDelayed(() -> {
                            if (field.hasFocus() && keyboardVisible) {
                                requestFieldVisibility(anchor, 0L, false);
                            }
                        }, 72L);
                    }
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
            int anchorBottomOnScreen = resolveAnchorBottomOnScreen(anchor, anchorTopOnScreen);
            int desiredBottomInset = resolveDesiredBottomInset(anchor);
            int desiredBottom = visibleFrame.bottom - desiredBottomInset;

            if (anchorBottomOnScreen <= desiredBottom) {
                return;
            }
            int delta = anchorBottomOnScreen - desiredBottom;
            if (anchor == notesEdit && notesEdit != null) {
                delta += Math.max(dp(24), notesEdit.getLineHeight());
            }
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
                + Math.max(dp(12), editText.getLineHeight() / 2);
        int minimumVisibleBottom = anchorTopOnScreen + Math.min(editText.getHeight(), dp(44));
        return Math.max(cursorBottom, minimumVisibleBottom);
    }

    private int resolveDesiredBottomInset(View anchor) {
        if (anchor != notesEdit || notesEdit == null) {
            return dp(24);
        }
        int lineHeight = Math.max(notesEdit.getLineHeight(), dp(20));
        int noteSafeInset = (lineHeight * 2) + notesEdit.getTotalPaddingBottom() + dp(76);
        return Math.max(dp(144), noteSafeInset);
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

    private String normalizeRgbHex(String color) {
        if (color == null || color.trim().isEmpty()) {
            return null;
        }
        String candidate = color.trim().toUpperCase(Locale.US);
        if (!candidate.startsWith("#")) {
            candidate = "#" + candidate;
        }
        if (candidate.length() != 7) {
            return null;
        }
        try {
            Color.parseColor(candidate);
            return candidate;
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
        return Color.alpha(color) < 255
                ? String.format(Locale.US, "#%08X", color)
                : String.format(Locale.US, "#%06X", (0xFFFFFF & color));
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

    private int resolveCustomColorDialogWidth() {
        int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        return Math.min(dp(372), screenWidth - dp(24));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
