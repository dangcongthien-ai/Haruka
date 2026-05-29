package com.example.finalproject.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.finalproject.data.HarukaDbHelper;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.data.DbContract;
import com.example.finalproject.model.HabitCategory;
import com.example.finalproject.model.HabitItem;
import com.example.finalproject.model.HabitLog;
import com.example.finalproject.model.HabitPriority;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.util.HabitDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HabitRepository {
    private final Context appContext;
    private final HarukaDbHelper dbHelper;

    public HabitRepository(Context context) {
        appContext = context.getApplicationContext();
        dbHelper = new HarukaDbHelper(appContext);
    }

    public List<HabitItem> getHabits() {
        SQLiteDatabase seedDb = dbHelper.getWritableDatabase();
        ensureSeedData(seedDb);

        List<HabitItem> habits = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.Habit.TABLE,
                null,
                DbContract.Habit.USER_ID + " = ?",
                new String[]{String.valueOf(dbHelper.getDefaultUserId())},
                null,
                null,
                DbContract.Habit.CREATED_AT + " DESC"
        )) {
            while (cursor.moveToNext()) {
                habits.add(loadHabitDetails(mapHabit(cursor)));
            }
        }
        return habits;
    }

    public HabitItem getHabit(long habitId) {
        SQLiteDatabase seedDb = dbHelper.getWritableDatabase();
        ensureSeedData(seedDb);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.Habit.TABLE,
                null,
                DbContract.Habit.HABIT_ID + " = ?",
                new String[]{String.valueOf(habitId)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return loadHabitDetails(mapHabit(cursor));
        }
    }

    public HabitLog getHabitLog(long habitId, LocalDate date) {
        if (date == null) {
            return null;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.HabitLog.TABLE,
                null,
                DbContract.HabitLog.HABIT_ID + " = ? AND " + DbContract.HabitLog.LOG_DATE + " = ?",
                new String[]{String.valueOf(habitId), DateTimeUtils.dateToIso(date)},
                null,
                null,
                DbContract.HabitLog.HABIT_LOG_ID + " DESC"
        )) {
            return cursor.moveToFirst() ? mapHabitLog(cursor) : null;
        }
    }

    public List<HabitLog> getHabitLogsInRange(LocalDate startDate, LocalDate endDate) {
        List<HabitLog> logs = new ArrayList<>();
        if (startDate == null || endDate == null) {
            return logs;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.HabitLog.TABLE,
                null,
                DbContract.HabitLog.LOG_DATE + " >= ? AND " + DbContract.HabitLog.LOG_DATE + " <= ?",
                new String[]{DateTimeUtils.dateToIso(startDate), DateTimeUtils.dateToIso(endDate)},
                null,
                null,
                DbContract.HabitLog.LOG_DATE + " ASC, " + DbContract.HabitLog.HABIT_ID + " ASC"
        )) {
            while (cursor.moveToNext()) {
                logs.add(mapHabitLog(cursor));
            }
        }
        return logs;
    }

    public void saveHabitLog(long habitId, LocalDate date, boolean completed, Double actualValue) {
        if (date == null) {
            return;
        }
        if (!completed && actualValue == null) {
            deleteHabitLog(habitId, date);
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        HabitLog existing = getHabitLog(habitId, date);
        String now = DateTimeUtils.nowIso();
        ContentValues values = new ContentValues();
        values.put(DbContract.HabitLog.HABIT_ID, habitId);
        values.put(DbContract.HabitLog.LOG_DATE, DateTimeUtils.dateToIso(date));
        values.put(DbContract.HabitLog.IS_COMPLETED, completed ? 1 : 0);
        putDouble(values, DbContract.HabitLog.ACTUAL_VALUE, actualValue);
        values.put(DbContract.HabitLog.UPDATED_AT, now);
        if (existing == null) {
            values.put(DbContract.HabitLog.CREATED_AT, now);
            db.insertOrThrow(DbContract.HabitLog.TABLE, null, values);
            return;
        }
        db.update(
                DbContract.HabitLog.TABLE,
                values,
                DbContract.HabitLog.HABIT_LOG_ID + " = ?",
                new String[]{String.valueOf(existing.getId())}
        );
    }

    public void deleteHabitLog(long habitId, LocalDate date) {
        if (date == null) {
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DbContract.HabitLog.TABLE,
                DbContract.HabitLog.HABIT_ID + " = ? AND " + DbContract.HabitLog.LOG_DATE + " = ?",
                new String[]{String.valueOf(habitId), DateTimeUtils.dateToIso(date)}
        );
    }

    public long saveHabit(HabitItem habit) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ensureSeedData(db);
        db.beginTransaction();
        try {
            habit.setUserId(dbHelper.getDefaultUserId());
            RecurrenceRule recurrenceRule = habit.getRecurrenceRule();
            if (recurrenceRule != null && !recurrenceRule.isNone()) {
                habit.setRecurrenceId(saveRecurrence(db, recurrenceRule));
            } else {
                habit.setRecurrenceId(null);
            }

            long habitId;
            String now = DateTimeUtils.nowIso();
            ContentValues values = toHabitValues(habit);
            values.put(DbContract.Habit.UPDATED_AT, now);
            if (habit.getId() <= 0) {
                values.put(DbContract.Habit.CREATED_AT, now);
                habitId = db.insertOrThrow(DbContract.Habit.TABLE, null, values);
                habit.setId(habitId);
            } else {
                habitId = habit.getId();
                db.update(
                        DbContract.Habit.TABLE,
                        values,
                        DbContract.Habit.HABIT_ID + " = ?",
                        new String[]{String.valueOf(habitId)}
                );
            }
            saveHabitReminders(db, habitId, habit.getReminders());
            db.setTransactionSuccessful();
            return habitId;
        } finally {
            db.endTransaction();
        }
    }

    public void deleteHabit(long habitId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DbContract.Habit.TABLE,
                DbContract.Habit.HABIT_ID + " = ?",
                new String[]{String.valueOf(habitId)}
        );
    }

    public List<HabitCategory> getCategories() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ensureSeedData(db);

        List<HabitCategory> categories = new ArrayList<>();
        try (Cursor cursor = db.query(
                DbContract.HabitCategory.TABLE,
                null,
                DbContract.HabitCategory.USER_ID + " = ?",
                new String[]{String.valueOf(dbHelper.getDefaultUserId())},
                null,
                null,
                DbContract.HabitCategory.CREATED_AT + " ASC"
        )) {
            while (cursor.moveToNext()) {
                categories.add(mapCategory(cursor));
            }
        }
        return categories;
    }

    public long saveCategory(HabitCategory category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ensureSeedData(db);
        category.setUserId(dbHelper.getDefaultUserId());
        ContentValues values = toCategoryValues(category);
        long categoryId;
        if (category.getId() <= 0) {
            values.put(DbContract.HabitCategory.CREATED_AT, DateTimeUtils.nowIso());
            categoryId = db.insertOrThrow(DbContract.HabitCategory.TABLE, null, values);
            category.setId(categoryId);
        } else {
            categoryId = category.getId();
            db.update(
                    DbContract.HabitCategory.TABLE,
                    values,
                    DbContract.HabitCategory.CATEGORY_ID + " = ?",
                    new String[]{String.valueOf(categoryId)}
            );
        }
        return categoryId;
    }

    public HabitCategory getCategory(long categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.HabitCategory.TABLE,
                null,
                DbContract.HabitCategory.CATEGORY_ID + " = ?",
                new String[]{String.valueOf(categoryId)},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() ? mapCategory(cursor) : null;
        }
    }

    public void deleteCategory(long categoryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DbContract.HabitCategory.TABLE,
                DbContract.HabitCategory.CATEGORY_ID + " = ?",
                new String[]{String.valueOf(categoryId)}
        );
    }

    public List<HabitPriority> getPriorities() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ensureSeedData(db);

        List<HabitPriority> priorities = new ArrayList<>();
        try (Cursor cursor = db.query(
                DbContract.HabitPriority.TABLE,
                null,
                DbContract.HabitPriority.USER_ID + " = ?",
                new String[]{String.valueOf(dbHelper.getDefaultUserId())},
                null,
                null,
                DbContract.HabitPriority.PRIORITY_ORDER + " ASC, " + DbContract.HabitPriority.CREATED_AT + " ASC"
        )) {
            while (cursor.moveToNext()) {
                priorities.add(mapPriority(cursor));
            }
        }
        return priorities;
    }

    public long savePriority(HabitPriority priority) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ensureSeedData(db);
        priority.setUserId(dbHelper.getDefaultUserId());
        if (priority.getPriorityOrder() <= 0) {
            priority.setPriorityOrder(nextPriorityOrder(db, priority.getUserId()));
        }

        ContentValues values = toPriorityValues(priority);
        long priorityId;
        if (priority.getId() <= 0) {
            values.put(DbContract.HabitPriority.CREATED_AT, DateTimeUtils.nowIso());
            priorityId = db.insertOrThrow(DbContract.HabitPriority.TABLE, null, values);
            priority.setId(priorityId);
        } else {
            priorityId = priority.getId();
            db.update(
                    DbContract.HabitPriority.TABLE,
                    values,
                    DbContract.HabitPriority.PRIORITY_ID + " = ?",
                    new String[]{String.valueOf(priorityId)}
            );
        }
        return priorityId;
    }

    public void deletePriority(long priorityId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DbContract.HabitPriority.TABLE,
                DbContract.HabitPriority.PRIORITY_ID + " = ?",
                new String[]{String.valueOf(priorityId)}
        );
    }

    public HabitPriority getPriority(long priorityId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.HabitPriority.TABLE,
                null,
                DbContract.HabitPriority.PRIORITY_ID + " = ?",
                new String[]{String.valueOf(priorityId)},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() ? mapPriority(cursor) : null;
        }
    }

    private HabitItem loadHabitDetails(HabitItem habit) {
        if (habit.getCategoryId() != null) {
            habit.setCategory(getCategory(habit.getCategoryId()));
        }
        if (habit.getPriorityId() != null) {
            habit.setPriority(getPriority(habit.getPriorityId()));
        }
        if (habit.getRecurrenceId() != null) {
            habit.setRecurrenceRule(getRecurrence(habit.getRecurrenceId()));
        }
        habit.setReminders(getHabitReminders(habit.getId()));
        return habit;
    }

    private void ensureSeedData(SQLiteDatabase db) {
        long userId = dbHelper.getDefaultUserId();
        if (countRows(db, DbContract.HabitCategory.TABLE, DbContract.HabitCategory.USER_ID + " = ?", userId) == 0) {
            for (HabitCategory category : HabitDefaults.defaultCategories(appContext)) {
                category.setUserId(userId);
                ContentValues values = toCategoryValues(category);
                values.put(DbContract.HabitCategory.CREATED_AT, DateTimeUtils.nowIso());
                db.insertOrThrow(DbContract.HabitCategory.TABLE, null, values);
            }
        }
        if (countRows(db, DbContract.HabitPriority.TABLE, DbContract.HabitPriority.USER_ID + " = ?", userId) == 0) {
            for (HabitPriority priority : HabitDefaults.defaultPriorities(appContext)) {
                priority.setUserId(userId);
                ContentValues values = toPriorityValues(priority);
                values.put(DbContract.HabitPriority.CREATED_AT, DateTimeUtils.nowIso());
                db.insertOrThrow(DbContract.HabitPriority.TABLE, null, values);
            }
        }
    }

    private long countRows(SQLiteDatabase db, String table, String selection, long userId) {
        try (Cursor cursor = db.query(
                table,
                new String[]{"COUNT(*)"},
                selection,
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    private int nextPriorityOrder(SQLiteDatabase db, long userId) {
        try (Cursor cursor = db.query(
                DbContract.HabitPriority.TABLE,
                new String[]{"MAX(" + DbContract.HabitPriority.PRIORITY_ORDER + ")"},
                DbContract.HabitPriority.USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() ? cursor.getInt(0) + 1 : 1;
        }
    }

    private RecurrenceRule getRecurrence(Long recurrenceId) {
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

    private List<Reminder> getHabitReminders(long habitId) {
        List<Reminder> reminders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.HabitReminder.TABLE,
                null,
                DbContract.HabitReminder.HABIT_ID + " = ?",
                new String[]{String.valueOf(habitId)},
                null,
                null,
                DbContract.HabitReminder.HABIT_REMINDER_ID + " ASC"
        )) {
            while (cursor.moveToNext()) {
                reminders.add(mapHabitReminder(cursor));
            }
        }
        return reminders;
    }

    private long saveRecurrence(SQLiteDatabase db, RecurrenceRule rule) {
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

    private void saveHabitReminders(SQLiteDatabase db, long habitId, List<Reminder> reminders) {
        db.delete(
                DbContract.HabitReminder.TABLE,
                DbContract.HabitReminder.HABIT_ID + " = ?",
                new String[]{String.valueOf(habitId)}
        );
        if (reminders == null || reminders.isEmpty()) {
            return;
        }
        for (Reminder reminder : reminders) {
            if (reminder == null || reminder.isNone()) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(DbContract.HabitReminder.HABIT_ID, habitId);
            values.put(DbContract.HabitReminder.REMINDER_TYPE, reminder.getReminderType());
            putInteger(values, DbContract.HabitReminder.OFFSET_VALUE, reminder.getOffsetValue());
            values.put(DbContract.HabitReminder.OFFSET_UNIT, reminder.getOffsetUnit());
            values.put(DbContract.HabitReminder.TIME_OF_DAY, DateTimeUtils.timeToIso(reminder.getTimeOfDay()));
            values.put(DbContract.HabitReminder.IS_ENABLED, reminder.isEnabled() ? 1 : 0);
            db.insertOrThrow(DbContract.HabitReminder.TABLE, null, values);
        }
    }

    private ContentValues toHabitValues(HabitItem habit) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Habit.USER_ID, habit.getUserId());
        putLong(values, DbContract.Habit.CATEGORY_ID, habit.getCategoryId());
        putLong(values, DbContract.Habit.PRIORITY_ID, habit.getPriorityId());
        putLong(values, DbContract.Habit.RECURRENCE_ID, habit.getRecurrenceId());
        values.put(DbContract.Habit.TITLE, habit.getTitle());
        values.put(DbContract.Habit.DESCRIPTION, habit.getDescription());
        values.put(DbContract.Habit.COLOR, habit.getColor());
        values.put(DbContract.Habit.START_DATE, DateTimeUtils.dateToIso(habit.getStartDate()));
        values.put(DbContract.Habit.HAS_END_DATE, habit.hasEndDate() ? 1 : 0);
        values.put(DbContract.Habit.END_DATE, habit.hasEndDate() ? DateTimeUtils.dateToIso(habit.getEndDate()) : null);
        values.put(DbContract.Habit.STATUS, habit.getStatus());
        values.put(DbContract.Habit.EVALUATION_TYPE, habit.getEvaluationType());
        if (HabitItem.EVALUATION_NUMBER.equals(habit.getEvaluationType())) {
            values.put(DbContract.Habit.TARGET_OPERATOR, habit.getTargetOperator());
            putDouble(values, DbContract.Habit.TARGET_VALUE, habit.getTargetValue());
            values.put(DbContract.Habit.TARGET_UNIT, habit.getTargetUnit());
        } else {
            values.putNull(DbContract.Habit.TARGET_OPERATOR);
            values.putNull(DbContract.Habit.TARGET_VALUE);
            values.putNull(DbContract.Habit.TARGET_UNIT);
        }
        return values;
    }

    private ContentValues toCategoryValues(HabitCategory category) {
        ContentValues values = new ContentValues();
        values.put(DbContract.HabitCategory.USER_ID, category.getUserId());
        values.put(DbContract.HabitCategory.NAME, category.getName());
        values.put(DbContract.HabitCategory.ICON_URI, category.getIconUri());
        values.put(DbContract.HabitCategory.COLOR, category.getColor());
        return values;
    }

    private ContentValues toPriorityValues(HabitPriority priority) {
        ContentValues values = new ContentValues();
        values.put(DbContract.HabitPriority.USER_ID, priority.getUserId());
        values.put(DbContract.HabitPriority.NAME, priority.getName());
        values.put(DbContract.HabitPriority.COLOR, priority.getColor());
        values.put(DbContract.HabitPriority.PRIORITY_ORDER, priority.getPriorityOrder());
        return values;
    }

    private HabitItem mapHabit(Cursor cursor) {
        HabitItem habit = new HabitItem();
        habit.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.Habit.HABIT_ID)));
        habit.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.Habit.USER_ID)));
        habit.setCategoryId(getNullableLong(cursor, DbContract.Habit.CATEGORY_ID));
        habit.setPriorityId(getNullableLong(cursor, DbContract.Habit.PRIORITY_ID));
        habit.setRecurrenceId(getNullableLong(cursor, DbContract.Habit.RECURRENCE_ID));
        habit.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.TITLE)));
        habit.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.DESCRIPTION)));
        habit.setColor(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.COLOR)));
        habit.setStartDate(DateTimeUtils.isoToDate(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.START_DATE))));
        habit.setHasEndDate(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.Habit.HAS_END_DATE)) == 1);
        habit.setEndDate(DateTimeUtils.isoToDate(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.END_DATE))));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.STATUS));
        habit.setStatus(status == null ? HabitItem.STATUS_ACTIVE : status);
        String evaluationType = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.EVALUATION_TYPE));
        habit.setEvaluationType(evaluationType == null ? HabitItem.EVALUATION_BOOLEAN : evaluationType);
        String targetOperator = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.TARGET_OPERATOR));
        habit.setTargetOperator(targetOperator == null ? HabitItem.OPERATOR_AT_LEAST : targetOperator);
        habit.setTargetValue(getNullableDouble(cursor, DbContract.Habit.TARGET_VALUE));
        habit.setTargetUnit(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.TARGET_UNIT)));
        habit.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.CREATED_AT)));
        habit.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Habit.UPDATED_AT)));
        return habit;
    }

    private HabitCategory mapCategory(Cursor cursor) {
        HabitCategory category = new HabitCategory();
        category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.HabitCategory.CATEGORY_ID)));
        category.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.HabitCategory.USER_ID)));
        category.setName(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitCategory.NAME)));
        category.setIconUri(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitCategory.ICON_URI)));
        category.setColor(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitCategory.COLOR)));
        category.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitCategory.CREATED_AT)));
        return category;
    }

    private HabitPriority mapPriority(Cursor cursor) {
        HabitPriority priority = new HabitPriority();
        priority.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.HabitPriority.PRIORITY_ID)));
        priority.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.HabitPriority.USER_ID)));
        priority.setName(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitPriority.NAME)));
        priority.setColor(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitPriority.COLOR)));
        priority.setPriorityOrder(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.HabitPriority.PRIORITY_ORDER)));
        priority.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitPriority.CREATED_AT)));
        return priority;
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

    private Reminder mapHabitReminder(Cursor cursor) {
        Reminder reminder = new Reminder();
        reminder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.HabitReminder.HABIT_REMINDER_ID)));
        reminder.setReminderType(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitReminder.REMINDER_TYPE)));
        reminder.setOffsetValue(getNullableInt(cursor, DbContract.HabitReminder.OFFSET_VALUE));
        reminder.setOffsetUnit(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitReminder.OFFSET_UNIT)));
        reminder.setTimeOfDay(DateTimeUtils.isoToTime(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitReminder.TIME_OF_DAY))));
        reminder.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.HabitReminder.IS_ENABLED)) == 1);
        return reminder;
    }

    private HabitLog mapHabitLog(Cursor cursor) {
        HabitLog log = new HabitLog();
        log.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.HabitLog.HABIT_LOG_ID)));
        log.setHabitId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.HabitLog.HABIT_ID)));
        log.setLogDate(DateTimeUtils.isoToDate(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitLog.LOG_DATE))));
        log.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.HabitLog.IS_COMPLETED)) == 1);
        log.setActualValue(getNullableDouble(cursor, DbContract.HabitLog.ACTUAL_VALUE));
        log.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitLog.CREATED_AT)));
        log.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.HabitLog.UPDATED_AT)));
        return log;
    }

    private Long getNullableLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getLong(index);
    }

    private Integer getNullableInt(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getInt(index);
    }

    private Double getNullableDouble(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getDouble(index);
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

    private void putDouble(ContentValues values, String key, Double value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }
}
