package com.example.finalproject.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TodoItem {
    private long id;
    private long userId;
    private Long recurrenceId;
    private String title;
    private String description;
    private LocalDate todoDate;
    private boolean completed;
    private String color = "#FFE9E9";
    private int priorityQuadrant = 3;
    private String createdAt;
    private String updatedAt;
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

    public LocalDate getTodoDate() {
        return todoDate;
    }

    public void setTodoDate(LocalDate todoDate) {
        this.todoDate = todoDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getPriorityQuadrant() {
        return priorityQuadrant;
    }

    public void setPriorityQuadrant(int priorityQuadrant) {
        this.priorityQuadrant = priorityQuadrant;
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
}
