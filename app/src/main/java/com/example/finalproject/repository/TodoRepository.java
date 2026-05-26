package com.example.finalproject.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.finalproject.data.CaliaryDbHelper;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.data.DbContract;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.model.TodoItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TodoRepository {
    public static final int FILTER_ALL = 0;
    public static final int FILTER_ACTIVE = 1;
    public static final int FILTER_DONE = 2;

    private final CaliaryDbHelper dbHelper;

    public TodoRepository(Context context) {
        dbHelper = new CaliaryDbHelper(context.getApplicationContext());
    }

    public TodoItem getTodo(long todoId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.Todo.TABLE,
                null,
                DbContract.Todo.TODO_ID + " = ?",
                new String[]{String.valueOf(todoId)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            TodoItem item = mapTodo(cursor);
            item.setReminders(getTodoReminders(item.getId()));
            return item;
        }
    }

    public List<TodoItem> getTodosForDate(LocalDate date) {
        List<TodoItem> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.Todo.TABLE,
                null,
                DbContract.Todo.TODO_DATE + " = ?",
                new String[]{DateTimeUtils.dateToIso(date)},
                null,
                null,
                stableTodoOrder()
        )) {
            while (cursor.moveToNext()) {
                result.add(mapTodo(cursor));
            }
        }
        return result;
    }

    public List<TodoItem> getTodos(LocalDate date, int filter) {
        List<TodoItem> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DbContract.Todo.TODO_DATE + " = ?";
        List<String> args = new ArrayList<>();
        args.add(DateTimeUtils.dateToIso(date));
        if (filter == FILTER_ACTIVE) {
            selection += " AND " + DbContract.Todo.IS_COMPLETED + " = 0";
        } else if (filter == FILTER_DONE) {
            selection += " AND " + DbContract.Todo.IS_COMPLETED + " = 1";
        }
        try (Cursor cursor = db.query(
                DbContract.Todo.TABLE,
                null,
                selection,
                args.toArray(new String[0]),
                null,
                null,
                stableTodoOrder()
        )) {
            while (cursor.moveToNext()) {
                result.add(mapTodo(cursor));
            }
        }
        return result;
    }

    public long saveTodo(TodoItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            item.setUserId(dbHelper.getDefaultUserId());
            long todoId;
            String now = DateTimeUtils.nowIso();
            ContentValues values = toTodoValues(item);
            values.put(DbContract.Todo.UPDATED_AT, now);
            if (item.getId() <= 0) {
                values.put(DbContract.Todo.CREATED_AT, now);
                todoId = db.insertOrThrow(DbContract.Todo.TABLE, null, values);
                item.setId(todoId);
            } else {
                todoId = item.getId();
                db.update(
                        DbContract.Todo.TABLE,
                        values,
                        DbContract.Todo.TODO_ID + " = ?",
                        new String[]{String.valueOf(todoId)}
                );
            }
            saveTodoReminders(db, todoId, item.getReminders());
            db.setTransactionSuccessful();
            return todoId;
        } finally {
            db.endTransaction();
        }
    }

    public void setCompleted(long todoId, boolean completed) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Todo.IS_COMPLETED, completed ? 1 : 0);
        values.put(DbContract.Todo.UPDATED_AT, DateTimeUtils.nowIso());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(
                DbContract.Todo.TABLE,
                values,
                DbContract.Todo.TODO_ID + " = ?",
                new String[]{String.valueOf(todoId)}
        );
    }

    public void deleteTodo(long todoId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DbContract.Todo.TABLE,
                DbContract.Todo.TODO_ID + " = ?",
                new String[]{String.valueOf(todoId)}
        );
    }

    public Reminder getTodoReminder(long todoId) {
        List<Reminder> reminders = getTodoReminders(todoId);
        return reminders.isEmpty() ? Reminder.none() : reminders.get(0);
    }

    public List<Reminder> getTodoReminders(long todoId) {
        List<Reminder> reminders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.TodoReminder.TABLE,
                null,
                DbContract.TodoReminder.TODO_ID + " = ?",
                new String[]{String.valueOf(todoId)},
                null,
                null,
                DbContract.TodoReminder.TODO_REMINDER_ID + " ASC"
        )) {
            while (cursor.moveToNext()) {
                reminders.add(mapTodoReminder(cursor));
            }
        }
        return reminders;
    }

    private void saveTodoReminders(SQLiteDatabase db, long todoId, List<Reminder> reminders) {
        db.delete(
                DbContract.TodoReminder.TABLE,
                DbContract.TodoReminder.TODO_ID + " = ?",
                new String[]{String.valueOf(todoId)}
        );
        if (reminders == null || reminders.isEmpty()) {
            return;
        }
        for (Reminder reminder : reminders) {
            if (reminder == null || reminder.isNone()) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(DbContract.TodoReminder.TODO_ID, todoId);
            values.put(DbContract.TodoReminder.REMINDER_TYPE, reminder.getReminderType());
            putInteger(values, DbContract.TodoReminder.OFFSET_VALUE, reminder.getOffsetValue());
            values.put(DbContract.TodoReminder.OFFSET_UNIT, reminder.getOffsetUnit());
            values.put(DbContract.TodoReminder.TIME_OF_DAY, DateTimeUtils.timeToIso(reminder.getTimeOfDay()));
            values.put(DbContract.TodoReminder.IS_ENABLED, reminder.isEnabled() ? 1 : 0);
            db.insertOrThrow(DbContract.TodoReminder.TABLE, null, values);
        }
    }

    private ContentValues toTodoValues(TodoItem item) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Todo.USER_ID, item.getUserId());
        if (item.getRecurrenceId() == null) {
            values.putNull(DbContract.Todo.RECURRENCE_ID);
        } else {
            values.put(DbContract.Todo.RECURRENCE_ID, item.getRecurrenceId());
        }
        values.put(DbContract.Todo.TITLE, item.getTitle());
        values.put(DbContract.Todo.DESCRIPTION, item.getDescription());
        values.put(DbContract.Todo.TODO_DATE, DateTimeUtils.dateToIso(item.getTodoDate()));
        values.put(DbContract.Todo.IS_COMPLETED, item.isCompleted() ? 1 : 0);
        values.put(DbContract.Todo.COLOR, item.getColor());
        values.put(DbContract.Todo.PRIORITY_QUADRANT, item.getPriorityQuadrant());
        return values;
    }

    private TodoItem mapTodo(Cursor cursor) {
        TodoItem item = new TodoItem();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.Todo.TODO_ID)));
        item.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.Todo.USER_ID)));
        int recurrenceIndex = cursor.getColumnIndexOrThrow(DbContract.Todo.RECURRENCE_ID);
        if (!cursor.isNull(recurrenceIndex)) {
            item.setRecurrenceId(cursor.getLong(recurrenceIndex));
        }
        item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Todo.TITLE)));
        item.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Todo.DESCRIPTION)));
        item.setTodoDate(DateTimeUtils.isoToDate(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Todo.TODO_DATE))));
        item.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.Todo.IS_COMPLETED)) == 1);
        item.setColor(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Todo.COLOR)));
        item.setPriorityQuadrant(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.Todo.PRIORITY_QUADRANT)));
        item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Todo.CREATED_AT)));
        item.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Todo.UPDATED_AT)));
        return item;
    }

    private String stableTodoOrder() {
        return DbContract.Todo.CREATED_AT + " ASC, " + DbContract.Todo.TODO_ID + " ASC";
    }

    private Reminder mapTodoReminder(Cursor cursor) {
        Reminder reminder = new Reminder();
        reminder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.TodoReminder.TODO_REMINDER_ID)));
        reminder.setReminderType(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.TodoReminder.REMINDER_TYPE)));
        reminder.setOffsetValue(getNullableInt(cursor, DbContract.TodoReminder.OFFSET_VALUE));
        reminder.setOffsetUnit(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.TodoReminder.OFFSET_UNIT)));
        reminder.setTimeOfDay(DateTimeUtils.isoToTime(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.TodoReminder.TIME_OF_DAY))));
        reminder.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.TodoReminder.IS_ENABLED)) == 1);
        return reminder;
    }

    private Integer getNullableInt(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getInt(index);
    }

    private void putInteger(ContentValues values, String key, Integer value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }
}
