package com.example.finalproject.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarDayCell {
    private final LocalDate date;
    private final boolean currentMonth;
    private final List<CalendarEvent> events = new ArrayList<>();
    private String journalDayMoodName;

    public CalendarDayCell(LocalDate date, boolean currentMonth) {
        this.date = date;
        this.currentMonth = currentMonth;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isCurrentMonth() {
        return currentMonth;
    }

    public List<CalendarEvent> getEvents() {
        return events;
    }

    public String getJournalDayMoodName() {
        return journalDayMoodName;
    }

    public void setJournalDayMoodName(String journalDayMoodName) {
        this.journalDayMoodName = journalDayMoodName;
    }
}
