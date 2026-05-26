package com.example.finalproject.model;

public class HabitListItem {
    private HabitItem habit;
    private HabitLog log;
    private boolean dueOnSelectedDate;
    private boolean completedOnSelectedDate;
    private boolean archived;
    private boolean archivedByCompletion;
    private String progressLabel;
    private String actionLabel;
    private float progressFraction;

    public HabitItem getHabit() {
        return habit;
    }

    public void setHabit(HabitItem habit) {
        this.habit = habit;
    }

    public HabitLog getLog() {
        return log;
    }

    public void setLog(HabitLog log) {
        this.log = log;
    }

    public boolean isDueOnSelectedDate() {
        return dueOnSelectedDate;
    }

    public void setDueOnSelectedDate(boolean dueOnSelectedDate) {
        this.dueOnSelectedDate = dueOnSelectedDate;
    }

    public boolean isCompletedOnSelectedDate() {
        return completedOnSelectedDate;
    }

    public void setCompletedOnSelectedDate(boolean completedOnSelectedDate) {
        this.completedOnSelectedDate = completedOnSelectedDate;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isArchivedByCompletion() {
        return archivedByCompletion;
    }

    public void setArchivedByCompletion(boolean archivedByCompletion) {
        this.archivedByCompletion = archivedByCompletion;
    }

    public String getProgressLabel() {
        return progressLabel;
    }

    public void setProgressLabel(String progressLabel) {
        this.progressLabel = progressLabel;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public float getProgressFraction() {
        return progressFraction;
    }

    public void setProgressFraction(float progressFraction) {
        this.progressFraction = progressFraction;
    }
}
