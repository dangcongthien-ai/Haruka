package com.example.finalproject.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CalendarEvent {
    private long id;
    private long userId;
    private Long recurrenceId;
    private String title;
    private String description;
    private String color = "#90CAF9";
    private boolean allDay;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location;
    private String url;
    private String createdAt;
    private String updatedAt;
    private RecurrenceRule recurrenceRule;
    private Reminder reminder;
    private List<Reminder> reminders = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getRecurrenceId() {
        return recurrenceId;
    }

    public void setRecurrenceId(Long recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(RecurrenceRule recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public Reminder getReminder() {
        if (reminder != null && !reminder.isNone()) {
            return reminder;
        }
        return reminders.isEmpty() ? Reminder.none() : reminders.get(0);
    }

    public void setReminder(Reminder reminder) {
        this.reminder = reminder;
        this.reminders.clear();
        if (reminder != null && !reminder.isNone()) {
            this.reminders.add(reminder);
        }
    }

    public List<Reminder> getReminders() {
        return reminders;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders == null ? new ArrayList<>() : new ArrayList<>(reminders);
        this.reminder = this.reminders.isEmpty() ? Reminder.none() : this.reminders.get(0);
    }

    public LocalDate getDate() {
        return startDateTime == null ? null : startDateTime.toLocalDate();
    }
}
