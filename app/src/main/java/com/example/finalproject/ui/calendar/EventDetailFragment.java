package com.example.finalproject.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.example.finalproject.ui.common.ScreenBackHandler;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EventDetailFragment extends Fragment implements ScreenBackHandler {
    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_DATE = "date";

    private CalendarRepository repository;
    private long eventId;
    private LocalDate anchorDate;
    private CalendarEvent event;

    private TextView titleView;
    private TextView summaryView;
    private TextView notesView;
    private View notesSection;
    private TextView startValueView;
    private TextView endValueView;
    private TextView repeatValueView;
    private TextView reminderValueView;
    private TextView locationValueView;
    private TextView urlValueView;

    public static EventDetailFragment newInstance(long eventId, LocalDate date) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_detail, container, false);
        repository = new CalendarRepository(requireContext());
        eventId = requireArguments().getLong(ARG_EVENT_ID);
        anchorDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (anchorDate == null) {
            anchorDate = LocalDate.now();
        }
        bind(view);
        setupClicks(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContent();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshContent();
        }
    }

    private void refreshContent() {
        if (!isAdded() || getView() == null) {
            return;
        }
        loadEvent();
        render();
    }

    private void bind(View view) {
        titleView = view.findViewById(R.id.tv_event_detail_title);
        summaryView = view.findViewById(R.id.tv_event_detail_summary);
        notesView = view.findViewById(R.id.tv_event_detail_notes);
        notesSection = view.findViewById(R.id.event_detail_notes_section);
        startValueView = view.findViewById(R.id.tv_event_detail_start);
        endValueView = view.findViewById(R.id.tv_event_detail_end);
        repeatValueView = view.findViewById(R.id.tv_event_detail_repeat);
        reminderValueView = view.findViewById(R.id.tv_event_detail_reminder);
        locationValueView = view.findViewById(R.id.tv_event_detail_location);
        urlValueView = view.findViewById(R.id.tv_event_detail_url);
    }

    private void setupClicks(View view) {
        UiUtils.setDebouncedClickListener(view.findViewById(R.id.btn_back), () -> ((MainActivity) requireActivity()).handleActivityBackPressed());
        UiUtils.setDebouncedClickListener(view.findViewById(R.id.btn_edit_event), () -> {
            if (event != null) {
                ((MainActivity) requireActivity()).pushFullScreenFragment(
                        EventEditFragment.newInstance(event.getId(), event.getDate()),
                        EventEditFragment.class.getSimpleName(),
                        false
                );
            }
        });
        UiUtils.setDebouncedClickListener(view.findViewById(R.id.btn_delete_event), () -> {
            if (event == null) {
                return;
            }
            UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_event), () -> {
                repository.deleteEvent(event.getId());
                ((MainActivity) requireActivity()).finishFullScreenOrHome();
            });
        });
    }

    private void loadEvent() {
        event = repository.getEvent(eventId);
        if (event == null && isAdded()) {
            ((MainActivity) requireActivity()).finishFullScreenOrHome();
        }
    }

    private void render() {
        if (!isAdded() || event == null) {
            return;
        }
        titleView.setText(event.getTitle());
        summaryView.setText(buildSummary(event));
        notesView.setText(emptyToPlaceholder(event.getDescription()));
        UiUtils.visible(notesSection, !isEmpty(event.getDescription()));
        startValueView.setText(formatDateTimeValue(event.getStartDateTime(), event.isAllDay()));
        endValueView.setText(formatDateTimeValue(event.getEndDateTime(), event.isAllDay()));

        RecurrenceRule recurrenceRule = event.getRecurrenceRule() == null ? RecurrenceRule.none() : event.getRecurrenceRule();
        repeatValueView.setText(recurrenceRule.getDisplayText());
        repeatValueView.setTextColor(requireContext().getColor(recurrenceRule.isNone() ? R.color.text_muted : R.color.text_primary));

        List<Reminder> reminders = event.getReminders();
        Reminder firstReminder = reminders == null || reminders.isEmpty() ? Reminder.none() : reminders.get(0);
        if (firstReminder.isNone()) {
            reminderValueView.setText(R.string.none);
            reminderValueView.setTextColor(requireContext().getColor(R.color.text_muted));
        } else if (reminders.size() == 1) {
            reminderValueView.setText(firstReminder.getDisplayText());
            reminderValueView.setTextColor(requireContext().getColor(R.color.text_primary));
        } else {
            reminderValueView.setText(getString(R.string.reminder_count_compact, reminders.size(), firstReminder.getDisplayText()));
            reminderValueView.setTextColor(requireContext().getColor(R.color.text_primary));
        }

        locationValueView.setText(valueOrNone(event.getLocation()));
        locationValueView.setTextColor(requireContext().getColor(isEmpty(event.getLocation()) ? R.color.text_muted : R.color.text_primary));
        urlValueView.setText(valueOrNone(event.getUrl()));
        urlValueView.setTextColor(requireContext().getColor(isEmpty(event.getUrl()) ? R.color.text_muted : R.color.text_primary));
    }

    private String buildSummary(CalendarEvent item) {
        if (item.isAllDay()) {
            if (item.getEndDateTime() != null && !item.getEndDateTime().toLocalDate().equals(item.getStartDateTime().toLocalDate())) {
                return DateTimeUtils.formatDateWithDow(item.getStartDateTime().toLocalDate())
                        + " - "
                        + DateTimeUtils.formatDateWithDow(item.getEndDateTime().toLocalDate())
                        + "  ·  "
                        + getString(R.string.all_day_short);
            }
            return DateTimeUtils.formatDateWithDow(item.getStartDateTime().toLocalDate())
                    + "  ·  "
                    + getString(R.string.all_day_short);
        }
        LocalDateTime start = item.getStartDateTime();
        LocalDateTime end = item.getEndDateTime();
        if (start == null) {
            return "";
        }
        if (end != null && !end.toLocalDate().equals(start.toLocalDate())) {
            return DateTimeUtils.formatDateWithDow(start.toLocalDate())
                    + " "
                    + DateTimeUtils.formatVietnameseTime(start.toLocalTime())
                    + " - "
                    + DateTimeUtils.formatDateWithDow(end.toLocalDate())
                    + " "
                    + DateTimeUtils.formatVietnameseTime(end.toLocalTime());
        }
        return DateTimeUtils.formatDateWithDow(start.toLocalDate())
                + "  ·  "
                + DateTimeUtils.formatVietnameseTime(start.toLocalTime())
                + " - "
                + DateTimeUtils.formatVietnameseTime((end == null ? start.plusHours(1) : end).toLocalTime());
    }

    private String formatDateTimeValue(LocalDateTime dateTime, boolean allDay) {
        if (dateTime == null) {
            return getString(R.string.none);
        }
        return allDay
                ? DateTimeUtils.formatDateWithDow(dateTime.toLocalDate())
                : DateTimeUtils.formatDateWithDow(dateTime.toLocalDate()) + "  " + DateTimeUtils.formatVietnameseTime(dateTime.toLocalTime());
    }

    private String emptyToPlaceholder(String value) {
        return isEmpty(value) ? getString(R.string.none) : value;
    }

    private String valueOrNone(String value) {
        return isEmpty(value) ? getString(R.string.none) : value;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean onHandleBackPressed() {
        ((MainActivity) requireActivity()).finishFullScreenOrHome();
        return true;
    }
}
