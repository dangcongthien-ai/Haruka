package com.example.finalproject.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HabitItem implements Serializable {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAUSED = "PAUSED";

    public static final String EVALUATION_BOOLEAN = "BOOLEAN";
    public static final String EVALUATION_NUMBER = "NUMBER";

    public static final String OPERATOR_AT_LEAST = "AT_LEAST";
    public static final String OPERATOR_AT_MOST = "AT_MOST";
    public static final String OPERATOR_EXACTLY = "EXACTLY";

    private long id;
    private long userId;
    private Long categoryId;
    private Long priorityId;
    private Long recurrenceId;
    private String title;
    private String description;
    private String color;
    private LocalDate startDate;
    private boolean hasEndDate;
    private LocalDate endDate;
    private String status = STATUS_ACTIVE;
    private String evaluationType = EVALUATION_BOOLEAN;
    private String targetOperator = OPERATOR_AT_LEAST;
    private Double targetValue;
    private String targetUnit;
    private String createdAt;
    private String updatedAt;
    private HabitCategory category;
    private HabitPriority priority;
    private RecurrenceRule recurrenceRule = RecurrenceRule.none();
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(Long priorityId) {
        this.priorityId = priorityId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public boolean hasEndDate() {
        return hasEndDate;
    }

    public void setHasEndDate(boolean hasEndDate) {
        this.hasEndDate = hasEndDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEvaluationType() {
        return evaluationType;
    }

    public void setEvaluationType(String evaluationType) {
        this.evaluationType = evaluationType;
    }

    public String getTargetOperator() {
        return targetOperator;
    }

    public void setTargetOperator(String targetOperator) {
        this.targetOperator = targetOperator;
    }

    public Double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Double targetValue) {
        this.targetValue = targetValue;
    }

    public String getTargetUnit() {
        return targetUnit;
    }

    public void setTargetUnit(String targetUnit) {
        this.targetUnit = targetUnit;
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

    public HabitCategory getCategory() {
        return category;
    }

    public void setCategory(HabitCategory category) {
        this.category = category;
    }

    public HabitPriority getPriority() {
        return priority;
    }

    public void setPriority(HabitPriority priority) {
        this.priority = priority;
    }

    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(RecurrenceRule recurrenceRule) {
        this.recurrenceRule = recurrenceRule == null ? RecurrenceRule.none() : recurrenceRule;
    }

    public List<Reminder> getReminders() {
        return reminders;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders == null ? new ArrayList<>() : reminders;
    }

    public boolean isNumberEvaluation() {
        return EVALUATION_NUMBER.equals(evaluationType);
    }
}
