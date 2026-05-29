package com.example.finalproject.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.finalproject.data.HarukaDbHelper;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.data.DbContract;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.model.Reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CalendarRepository {
    private final HarukaDbHelper dbHelper;

    public CalendarRepository(Context context) {
        dbHelper = new HarukaDbHelper(context.getApplicationContext());
    }

    public CalendarEvent getEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.CalendarEvent.TABLE,
                null,
                DbContract.CalendarEvent.EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            CalendarEvent event = mapEvent(cursor);
            if (event.getRecurrenceId() != null) {
                event.setRecurrenceRule(getRecurrence(event.getRecurrenceId()));
            }
            event.setReminders(getEventReminders(event.getId()));
            return event;
        }
    }

    public List<CalendarEvent> getEventsForDate(LocalDate date) {
        return getEventsBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public List<CalendarEvent> getEventsForMonth(LocalDate monthDate) {
        LocalDate start = monthDate.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return getEventsBetween(start.atStartOfDay(), end.atStartOfDay());
    }

    public List<CalendarEvent> getEventsBetween(LocalDateTime start, LocalDateTime endExclusive) {
        List<CalendarEvent> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DbContract.CalendarEvent.START_DATETIME + " < ? AND ("
                + DbContract.CalendarEvent.END_DATETIME + " IS NULL OR "
                + DbContract.CalendarEvent.END_DATETIME + " >= ?)";
        String[] args = {
                DateTimeUtils.dateTimeToIso(endExclusive),
                DateTimeUtils.dateTimeToIso(start)
        };
        try (Cursor cursor = db.query(
                DbContract.CalendarEvent.TABLE,
                null,
                selection,
                args,
                null,
                null,
                DbContract.CalendarEvent.START_DATETIME + " ASC, "
                        + DbContract.CalendarEvent.CREATED_AT + " ASC, "
                        + DbContract.CalendarEvent.EVENT_ID + " ASC"
        )) {
            while (cursor.moveToNext()) {
                result.add(mapEvent(cursor));
            }
        }
        return result;
    }

    public long saveEvent(CalendarEvent event) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            event.setUserId(dbHelper.getDefaultUserId());
            RecurrenceRule recurrenceRule = event.getRecurrenceRule();
            if (recurrenceRule != null && !recurrenceRule.isNone()) {
                event.setRecurrenceId(saveRecurrence(db, recurrenceRule));
            } else {
                event.setRecurrenceId(null);
            }

            long eventId;
            String now = DateTimeUtils.nowIso();
            ContentValues values = toEventValues(event);
            values.put(DbContract.CalendarEvent.UPDATED_AT, now);
            if (event.getId() <= 0) {
                values.put(DbContract.CalendarEvent.CREATED_AT, now);
                eventId = db.insertOrThrow(DbContract.CalendarEvent.TABLE, null, values);
                event.setId(eventId);
            } else {
                eventId = event.getId();
                db.update(
                        DbContract.CalendarEvent.TABLE,
                        values,
                        DbContract.CalendarEvent.EVENT_ID + " = ?",
                        new String[]{String.valueOf(eventId)}
                );
            }

            saveEventReminders(db, eventId, event.getReminders());
            db.setTransactionSuccessful();
            return eventId;
        } finally {
            db.endTransaction();
        }
    }

    public void deleteEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DbContract.CalendarEvent.TABLE,
                DbContract.CalendarEvent.EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)}
        );
    }

    public RecurrenceRule getRecurrence(Long recurrenceId) {
        if (recurrenceId == null) {
            return RecurrenceRule.none();
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.RecurrenceRule.TABLE,
                null,
                DbContract.RecurrenceRule.RECURRENCE_ID + " = ?",
                new String[]{String.valueOf(recurrenceId)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return RecurrenceRule.none();
            }
            return mapRecurrence(cursor);
        }
    }

    public Reminder getEventReminder(long eventId) {
        List<Reminder> reminders = getEventReminders(eventId);
        return reminders.isEmpty() ? Reminder.none() : reminders.get(0);
    }

    public List<Reminder> getEventReminders(long eventId) {
        List<Reminder> reminders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.EventReminder.TABLE,
                null,
                DbContract.EventReminder.EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)},
                null,
                null,
                DbContract.EventReminder.EVENT_REMINDER_ID + " ASC"
        )) {
            while (cursor.moveToNext()) {
                reminders.add(mapEventReminder(cursor));
            }
        }
        return reminders;
    }

    private long saveRecurrence(SQLiteDatabase db, RecurrenceRule rule) {
        ContentValues values = toRecurrenceValues(rule);
        if (rule.getId() > 0) {
            db.update(
                    DbContract.RecurrenceRule.TABLE,
                    values,
                    DbContract.RecurrenceRule.RECURRENCE_ID + " = ?",
                    new String[]{String.valueOf(rule.getId())}
            );
            return rule.getId();
        }
        long id = db.insertOrThrow(DbContract.RecurrenceRule.TABLE, null, values);
        rule.setId(id);
        return id;
    }

    private void saveEventReminders(SQLiteDatabase db, long eventId, List<Reminder> reminders) {
        db.delete(
                DbContract.EventReminder.TABLE,
                DbContract.EventReminder.EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)}
        );
        if (reminders == null || reminders.isEmpty()) {
            return;
        }
        for (Reminder reminder : reminders) {
            if (reminder == null || reminder.isNone()) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(DbContract.EventReminder.EVENT_ID, eventId);
            values.put(DbContract.EventReminder.REMINDER_TYPE, reminder.getReminderType());
            putInteger(values, DbContract.EventReminder.OFFSET_VALUE, reminder.getOffsetValue());
            values.put(DbContract.EventReminder.OFFSET_UNIT, reminder.getOffsetUnit());
            values.put(DbContract.EventReminder.TIME_OF_DAY, DateTimeUtils.timeToIso(reminder.getTimeOfDay()));
            values.put(DbContract.EventReminder.IS_ENABLED, reminder.isEnabled() ? 1 : 0);
            db.insertOrThrow(DbContract.EventReminder.TABLE, null, values);
        }
    }

    private ContentValues toEventValues(CalendarEvent event) {
        ContentValues values = new ContentValues();
        values.put(DbContract.CalendarEvent.USER_ID, event.getUserId());
        putLong(values, DbContract.CalendarEvent.RECURRENCE_ID, event.getRecurrenceId());
        values.put(DbContract.CalendarEvent.TITLE, event.getTitle());
        values.put(DbContract.CalendarEvent.DESCRIPTION, event.getDescription());
        values.put(DbContract.CalendarEvent.COLOR, event.getColor());
        values.put(DbContract.CalendarEvent.IS_ALL_DAY, event.isAllDay() ? 1 : 0);
        values.put(DbContract.CalendarEvent.START_DATETIME, DateTimeUtils.dateTimeToIso(event.getStartDateTime()));
        values.put(DbContract.CalendarEvent.END_DATETIME, DateTimeUtils.dateTimeToIso(event.getEndDateTime()));
        values.put(DbContract.CalendarEvent.LOCATION, event.getLocation());
        values.put(DbContract.CalendarEvent.URL, event.getUrl());
        return values;
    }

    private ContentValues toRecurrenceValues(RecurrenceRule rule) {
        ContentValues values = new ContentValues();
        values.put(DbContract.RecurrenceRule.FREQ, rule.getFreq());
        values.put(DbContract.RecurrenceRule.INTERVAL_VALUE, rule.getIntervalValue());
        values.put(DbContract.RecurrenceRule.BY_DAY, rule.getByDay());
        putInteger(values, DbContract.RecurrenceRule.BY_MONTH_DAY, rule.getByMonthDay());
        putInteger(values, DbContract.RecurrenceRule.BY_MONTH, rule.getByMonth());
        putInteger(values, DbContract.RecurrenceRule.BY_SET_POS, rule.getBySetPos());
        values.put(DbContract.RecurrenceRule.MONTHLY_PATTERN_TYPE, rule.getMonthlyPatternType());
        values.put(DbContract.RecurrenceRule.END_TYPE, rule.getEndType());
        values.put(DbContract.RecurrenceRule.END_DATE, DateTimeUtils.dateToIso(rule.getEndDate()));
        putInteger(values, DbContract.RecurrenceRule.OCCURRENCE_COUNT, rule.getOccurrenceCount());
        return values;
    }

    private CalendarEvent mapEvent(Cursor cursor) {
        CalendarEvent event = new CalendarEvent();
        event.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.EVENT_ID)));
        event.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.USER_ID)));
        int recurrenceIndex = cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.RECURRENCE_ID);
        if (!cursor.isNull(recurrenceIndex)) {
            event.setRecurrenceId(cursor.getLong(recurrenceIndex));
        }
        event.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.TITLE)));
        event.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.DESCRIPTION)));
        event.setColor(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.COLOR)));
        event.setAllDay(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.IS_ALL_DAY)) == 1);
        event.setStartDateTime(DateTimeUtils.isoToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.START_DATETIME))));
        event.setEndDateTime(DateTimeUtils.isoToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.END_DATETIME))));
        event.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.LOCATION)));
        event.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.URL)));
        event.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.CREATED_AT)));
        event.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.CalendarEvent.UPDATED_AT)));
        return event;
    }

    private RecurrenceRule mapRecurrence(Cursor cursor) {
        RecurrenceRule rule = new RecurrenceRule();
        rule.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.RecurrenceRule.RECURRENCE_ID)));
        rule.setFreq(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.RecurrenceRule.FREQ)));
        rule.setIntervalValue(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.RecurrenceRule.INTERVAL_VALUE)));
        rule.setByDay(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.RecurrenceRule.BY_DAY)));
        rule.setByMonthDay(getNullableInt(cursor, DbContract.RecurrenceRule.BY_MONTH_DAY));
        rule.setByMonth(getNullableInt(cursor, DbContract.RecurrenceRule.BY_MONTH));
        rule.setBySetPos(getNullableInt(cursor, DbContract.RecurrenceRule.BY_SET_POS));
        rule.setMonthlyPatternType(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.RecurrenceRule.MONTHLY_PATTERN_TYPE)));
        rule.setEndType(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.RecurrenceRule.END_TYPE)));
        rule.setEndDate(DateTimeUtils.isoToDate(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.RecurrenceRule.END_DATE))));
        rule.setOccurrenceCount(getNullableInt(cursor, DbContract.RecurrenceRule.OCCURRENCE_COUNT));
        return rule;
    }

    private Reminder mapEventReminder(Cursor cursor) {
        Reminder reminder = new Reminder();
        reminder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.EventReminder.EVENT_REMINDER_ID)));
        reminder.setReminderType(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.EventReminder.REMINDER_TYPE)));
        reminder.setOffsetValue(getNullableInt(cursor, DbContract.EventReminder.OFFSET_VALUE));
        reminder.setOffsetUnit(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.EventReminder.OFFSET_UNIT)));
        reminder.setTimeOfDay(DateTimeUtils.isoToTime(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.EventReminder.TIME_OF_DAY))));
        reminder.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.EventReminder.IS_ENABLED)) == 1);
        return reminder;
    }

    private Integer getNullableInt(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getInt(index);
    }

    private void putLong(ContentValues values, String key, Long value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private void putInteger(ContentValues values, String key, Integer value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }
}
