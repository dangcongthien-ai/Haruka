package com.example.finalproject.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HarukaDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "haruka.db";
    public static final int DATABASE_VERSION = 2;

    public HarukaDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createAppUser());
        db.execSQL(createRecurrenceRule());
        db.execSQL(createCalendarEvent());
        db.execSQL(createEventReminder());
        db.execSQL(createTodo());
        db.execSQL(createTodoReminder());
        db.execSQL(createHabitCategory());
        db.execSQL(createHabitPriority());
        db.execSQL(createHabit());
        db.execSQL(createHabitReminder());
        db.execSQL(createHabitLog());
        db.execSQL(createJournalLayout());
        db.execSQL(createJournalEntry());
        db.execSQL(createJournalImage());
        db.execSQL(createSticker());
        db.execSQL(createJournalEntrySticker());
        ensureDefaultUser(db);
        ensureDefaultJournalLayouts(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.JournalEntrySticker.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Sticker.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.JournalImage.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.JournalEntry.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.JournalLayout.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.HabitLog.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.HabitReminder.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Habit.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.HabitPriority.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.HabitCategory.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.TodoReminder.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Todo.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.EventReminder.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.CalendarEvent.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.RecurrenceRule.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.AppUser.TABLE);
        onCreate(db);
    }

    public long getDefaultUserId() {
        SQLiteDatabase db = getWritableDatabase();
        return ensureDefaultUser(db);
    }

    public void ensureDefaultJournalLayouts(SQLiteDatabase db) {
        ensureJournalLayout(db, 1L, "Gingham");
        ensureJournalLayout(db, 2L, "Denim Star");
        ensureJournalLayout(db, 3L, "Plaid");
    }

    private long ensureDefaultUser(SQLiteDatabase db) {
        try (Cursor cursor = db.query(
                DbContract.AppUser.TABLE,
                new String[]{DbContract.AppUser.USER_ID},
                null,
                null,
                null,
                null,
                DbContract.AppUser.USER_ID + " ASC",
                "1"
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }

        ContentValues values = new ContentValues();
        values.put(DbContract.AppUser.CREATED_AT, DateTimeUtils.nowIso());
        return db.insertOrThrow(DbContract.AppUser.TABLE, null, values);
    }

    private void ensureJournalLayout(SQLiteDatabase db, long layoutId, String name) {
        ContentValues values = new ContentValues();
        values.put(DbContract.JournalLayout.LAYOUT_ID, layoutId);
        values.put(DbContract.JournalLayout.NAME, name);
        values.put(DbContract.JournalLayout.CREATED_AT, DateTimeUtils.nowIso());
        db.insertWithOnConflict(
                DbContract.JournalLayout.TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    private String createAppUser() {
        return "CREATE TABLE " + DbContract.AppUser.TABLE + " ("
                + DbContract.AppUser.USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.AppUser.CREATED_AT + " TEXT NOT NULL"
                + ")";
    }

    private String createRecurrenceRule() {
        return "CREATE TABLE " + DbContract.RecurrenceRule.TABLE + " ("
                + DbContract.RecurrenceRule.RECURRENCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.RecurrenceRule.FREQ + " TEXT NOT NULL DEFAULT 'NONE', "
                + DbContract.RecurrenceRule.INTERVAL_VALUE + " INTEGER NOT NULL DEFAULT 1, "
                + DbContract.RecurrenceRule.BY_DAY + " TEXT, "
                + DbContract.RecurrenceRule.BY_MONTH_DAY + " INTEGER, "
                + DbContract.RecurrenceRule.BY_MONTH + " INTEGER, "
                + DbContract.RecurrenceRule.BY_SET_POS + " INTEGER, "
                + DbContract.RecurrenceRule.MONTHLY_PATTERN_TYPE + " TEXT, "
                + DbContract.RecurrenceRule.END_TYPE + " TEXT NOT NULL DEFAULT 'NONE', "
                + DbContract.RecurrenceRule.END_DATE + " TEXT, "
                + DbContract.RecurrenceRule.OCCURRENCE_COUNT + " INTEGER"
                + ")";
    }

    private String createCalendarEvent() {
        return "CREATE TABLE " + DbContract.CalendarEvent.TABLE + " ("
                + DbContract.CalendarEvent.EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.CalendarEvent.USER_ID + " INTEGER NOT NULL, "
                + DbContract.CalendarEvent.RECURRENCE_ID + " INTEGER, "
                + DbContract.CalendarEvent.TITLE + " TEXT NOT NULL, "
                + DbContract.CalendarEvent.DESCRIPTION + " TEXT, "
                + DbContract.CalendarEvent.COLOR + " TEXT, "
                + DbContract.CalendarEvent.IS_ALL_DAY + " INTEGER NOT NULL DEFAULT 0, "
                + DbContract.CalendarEvent.START_DATETIME + " TEXT NOT NULL, "
                + DbContract.CalendarEvent.END_DATETIME + " TEXT, "
                + DbContract.CalendarEvent.LOCATION + " TEXT, "
                + DbContract.CalendarEvent.URL + " TEXT, "
                + DbContract.CalendarEvent.CREATED_AT + " TEXT NOT NULL, "
                + DbContract.CalendarEvent.UPDATED_AT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + DbContract.CalendarEvent.USER_ID + ") REFERENCES " + DbContract.AppUser.TABLE + "(" + DbContract.AppUser.USER_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + DbContract.CalendarEvent.RECURRENCE_ID + ") REFERENCES " + DbContract.RecurrenceRule.TABLE + "(" + DbContract.RecurrenceRule.RECURRENCE_ID + ") ON DELETE SET NULL"
                + ")";
    }

    private String createEventReminder() {
        return "CREATE TABLE " + DbContract.EventReminder.TABLE + " ("
                + DbContract.EventReminder.EVENT_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.EventReminder.EVENT_ID + " INTEGER NOT NULL, "
                + DbContract.EventReminder.REMINDER_TYPE + " TEXT NOT NULL, "
                + DbContract.EventReminder.OFFSET_VALUE + " INTEGER, "
                + DbContract.EventReminder.OFFSET_UNIT + " TEXT, "
                + DbContract.EventReminder.TIME_OF_DAY + " TEXT, "
                + DbContract.EventReminder.IS_ENABLED + " INTEGER NOT NULL DEFAULT 1, "
                + "FOREIGN KEY(" + DbContract.EventReminder.EVENT_ID + ") REFERENCES " + DbContract.CalendarEvent.TABLE + "(" + DbContract.CalendarEvent.EVENT_ID + ") ON DELETE CASCADE"
                + ")";
    }

    private String createTodo() {
        return "CREATE TABLE " + DbContract.Todo.TABLE + " ("
                + DbContract.Todo.TODO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Todo.USER_ID + " INTEGER NOT NULL, "
                + DbContract.Todo.RECURRENCE_ID + " INTEGER, "
                + DbContract.Todo.TITLE + " TEXT NOT NULL, "
                + DbContract.Todo.DESCRIPTION + " TEXT, "
                + DbContract.Todo.TODO_DATE + " TEXT NOT NULL, "
                + DbContract.Todo.IS_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
                + DbContract.Todo.COLOR + " TEXT, "
                + DbContract.Todo.PRIORITY_QUADRANT + " INTEGER NOT NULL DEFAULT 3, "
                + DbContract.Todo.CREATED_AT + " TEXT NOT NULL, "
                + DbContract.Todo.UPDATED_AT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + DbContract.Todo.USER_ID + ") REFERENCES " + DbContract.AppUser.TABLE + "(" + DbContract.AppUser.USER_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + DbContract.Todo.RECURRENCE_ID + ") REFERENCES " + DbContract.RecurrenceRule.TABLE + "(" + DbContract.RecurrenceRule.RECURRENCE_ID + ") ON DELETE SET NULL"
                + ")";
    }

    private String createTodoReminder() {
        return "CREATE TABLE " + DbContract.TodoReminder.TABLE + " ("
                + DbContract.TodoReminder.TODO_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.TodoReminder.TODO_ID + " INTEGER NOT NULL, "
                + DbContract.TodoReminder.REMINDER_TYPE + " TEXT NOT NULL, "
                + DbContract.TodoReminder.OFFSET_VALUE + " INTEGER, "
                + DbContract.TodoReminder.OFFSET_UNIT + " TEXT, "
                + DbContract.TodoReminder.TIME_OF_DAY + " TEXT, "
                + DbContract.TodoReminder.IS_ENABLED + " INTEGER NOT NULL DEFAULT 1, "
                + "FOREIGN KEY(" + DbContract.TodoReminder.TODO_ID + ") REFERENCES " + DbContract.Todo.TABLE + "(" + DbContract.Todo.TODO_ID + ") ON DELETE CASCADE"
                + ")";
    }

    private String createHabitCategory() {
        return "CREATE TABLE " + DbContract.HabitCategory.TABLE + " ("
                + DbContract.HabitCategory.CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.HabitCategory.USER_ID + " INTEGER NOT NULL, "
                + DbContract.HabitCategory.NAME + " TEXT NOT NULL, "
                + DbContract.HabitCategory.ICON_URI + " TEXT, "
                + DbContract.HabitCategory.COLOR + " TEXT, "
                + DbContract.HabitCategory.CREATED_AT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + DbContract.HabitCategory.USER_ID + ") REFERENCES " + DbContract.AppUser.TABLE + "(" + DbContract.AppUser.USER_ID + ") ON DELETE CASCADE"
                + ")";
    }

    private String createHabitPriority() {
        return "CREATE TABLE " + DbContract.HabitPriority.TABLE + " ("
                + DbContract.HabitPriority.PRIORITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.HabitPriority.USER_ID + " INTEGER NOT NULL, "
                + DbContract.HabitPriority.NAME + " TEXT NOT NULL, "
                + DbContract.HabitPriority.COLOR + " TEXT, "
                + DbContract.HabitPriority.PRIORITY_ORDER + " INTEGER NOT NULL DEFAULT 0, "
                + DbContract.HabitPriority.CREATED_AT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + DbContract.HabitPriority.USER_ID + ") REFERENCES " + DbContract.AppUser.TABLE + "(" + DbContract.AppUser.USER_ID + ") ON DELETE CASCADE"
                + ")";
    }

    private String createHabit() {
        return "CREATE TABLE " + DbContract.Habit.TABLE + " ("
                + DbContract.Habit.HABIT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Habit.USER_ID + " INTEGER NOT NULL, "
                + DbContract.Habit.CATEGORY_ID + " INTEGER, "
                + DbContract.Habit.PRIORITY_ID + " INTEGER, "
                + DbContract.Habit.RECURRENCE_ID + " INTEGER, "
                + DbContract.Habit.TITLE + " TEXT NOT NULL, "
                + DbContract.Habit.DESCRIPTION + " TEXT, "
                + DbContract.Habit.COLOR + " TEXT, "
                + DbContract.Habit.START_DATE + " TEXT NOT NULL, "
                + DbContract.Habit.HAS_END_DATE + " INTEGER NOT NULL DEFAULT 0, "
                + DbContract.Habit.END_DATE + " TEXT, "
                + DbContract.Habit.STATUS + " TEXT, "
                + DbContract.Habit.EVALUATION_TYPE + " TEXT, "
                + DbContract.Habit.TARGET_OPERATOR + " TEXT, "
                + DbContract.Habit.TARGET_VALUE + " REAL, "
                + DbContract.Habit.TARGET_UNIT + " TEXT, "
                + DbContract.Habit.CREATED_AT + " TEXT NOT NULL, "
                + DbContract.Habit.UPDATED_AT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + DbContract.Habit.USER_ID + ") REFERENCES " + DbContract.AppUser.TABLE + "(" + DbContract.AppUser.USER_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + DbContract.Habit.CATEGORY_ID + ") REFERENCES " + DbContract.HabitCategory.TABLE + "(" + DbContract.HabitCategory.CATEGORY_ID + ") ON DELETE SET NULL, "
                + "FOREIGN KEY(" + DbContract.Habit.PRIORITY_ID + ") REFERENCES " + DbContract.HabitPriority.TABLE + "(" + DbContract.HabitPriority.PRIORITY_ID + ") ON DELETE SET NULL, "
                + "FOREIGN KEY(" + DbContract.Habit.RECURRENCE_ID + ") REFERENCES " + DbContract.RecurrenceRule.TABLE + "(" + DbContract.RecurrenceRule.RECURRENCE_ID + ") ON DELETE SET NULL"
                + ")";
    }

    private String createHabitReminder() {
        return "CREATE TABLE " + DbContract.HabitReminder.TABLE + " ("
                + DbContract.HabitReminder.HABIT_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.HabitReminder.HABIT_ID + " INTEGER NOT NULL, "
                + DbContract.HabitReminder.REMINDER_TYPE + " TEXT NOT NULL, "
                + DbContract.HabitReminder.OFFSET_VALUE + " INTEGER, "
                + DbContract.HabitReminder.OFFSET_UNIT + " TEXT, "
                + DbContract.HabitReminder.TIME_OF_DAY + " TEXT, "
                + DbContract.HabitReminder.IS_ENABLED + " INTEGER NOT NULL DEFAULT 1, "
                + "FOREIGN KEY(" + DbContract.HabitReminder.HABIT_ID + ") REFERENCES " + DbContract.Habit.TABLE + "(" + DbContract.Habit.HABIT_ID + ") ON DELETE CASCADE"
                + ")";
    }

    private String createHabitLog() {
        return "CREATE TABLE " + DbContract.HabitLog.TABLE + " ("
                + DbContract.HabitLog.HABIT_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.HabitLog.HABIT_ID + " INTEGER NOT NULL, "
                + DbContract.HabitLog.LOG_DATE + " TEXT NOT NULL, "
                + DbContract.HabitLog.IS_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
                + DbContract.HabitLog.ACTUAL_VALUE + " REAL, "
                + DbContract.HabitLog.CREATED_AT + " TEXT NOT NULL, "
                + DbContract.HabitLog.UPDATED_AT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + DbContract.HabitLog.HABIT_ID + ") REFERENCES " + DbContract.Habit.TABLE + "(" + DbContract.Habit.HABIT_ID + ") ON DELETE CASCADE"
                + ")";
    }

    private String createJournalLayout() {
        return "CREATE TABLE " + DbContract.JournalLayout.TABLE + " ("
                + DbContract.JournalLayout.LAYOUT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.JournalLayout.NAME + " TEXT NOT NULL, "
                + DbContract.JournalLayout.PREVIEW_IMAGE_URI + " TEXT, "
                + DbContract.JournalLayout.BACKGROUND_IMAGE_URI + " TEXT, "
                + DbContract.JournalLayout.LAYOUT_CONFIG + " TEXT, "
                + DbContract.JournalLayout.IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1, "
                + DbContract.JournalLayout.CREATED_AT + " TEXT NOT NULL"
                + ")";
    }

    private String createJournalEntry() {
        return "CREATE TABLE " + DbContract.JournalEntry.TABLE + " ("
                + DbContract.JournalEntry.JOURNAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.JournalEntry.USER_ID + " INTEGER NOT NULL, "
                + DbContract.JournalEntry.LAYOUT_ID + " INTEGER, "
                + DbContract.JournalEntry.JOURNAL_DATE + " TEXT NOT NULL, "
                + DbContract.JournalEntry.TITLE + " TEXT, "
                + DbContract.JournalEntry.CAPTION + " TEXT, "
                + DbContract.JournalEntry.CONTENT + " TEXT, "
                + DbContract.JournalEntry.CREATED_AT + " TEXT NOT NULL, "
                + DbContract.JournalEntry.UPDATED_AT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + DbContract.JournalEntry.USER_ID + ") REFERENCES " + DbContract.AppUser.TABLE + "(" + DbContract.AppUser.USER_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + DbContract.JournalEntry.LAYOUT_ID + ") REFERENCES " + DbContract.JournalLayout.TABLE + "(" + DbContract.JournalLayout.LAYOUT_ID + ") ON DELETE SET NULL"
                + ")";
    }

    private String createJournalImage() {
        return "CREATE TABLE " + DbContract.JournalImage.TABLE + " ("
                + DbContract.JournalImage.IMAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.JournalImage.JOURNAL_ID + " INTEGER NOT NULL, "
                + DbContract.JournalImage.IMAGE_URI + " TEXT NOT NULL, "
                + DbContract.JournalImage.POSITION_INDEX + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY(" + DbContract.JournalImage.JOURNAL_ID + ") REFERENCES " + DbContract.JournalEntry.TABLE + "(" + DbContract.JournalEntry.JOURNAL_ID + ") ON DELETE CASCADE"
                + ")";
    }

    private String createSticker() {
        return "CREATE TABLE " + DbContract.Sticker.TABLE + " ("
                + DbContract.Sticker.STICKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Sticker.NAME + " TEXT NOT NULL, "
                + DbContract.Sticker.STICKER_TYPE + " TEXT, "
                + DbContract.Sticker.IMAGE_URI + " TEXT, "
                + DbContract.Sticker.IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1"
                + ")";
    }

    private String createJournalEntrySticker() {
        return "CREATE TABLE " + DbContract.JournalEntrySticker.TABLE + " ("
                + DbContract.JournalEntrySticker.JOURNAL_ID + " INTEGER NOT NULL, "
                + DbContract.JournalEntrySticker.STICKER_ID + " INTEGER NOT NULL, "
                + DbContract.JournalEntrySticker.SELECTED_ORDER + " INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY("
                + DbContract.JournalEntrySticker.JOURNAL_ID + ", "
                + DbContract.JournalEntrySticker.STICKER_ID + ", "
                + DbContract.JournalEntrySticker.SELECTED_ORDER + "), "
                + "FOREIGN KEY(" + DbContract.JournalEntrySticker.JOURNAL_ID + ") REFERENCES " + DbContract.JournalEntry.TABLE + "(" + DbContract.JournalEntry.JOURNAL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + DbContract.JournalEntrySticker.STICKER_ID + ") REFERENCES " + DbContract.Sticker.TABLE + "(" + DbContract.Sticker.STICKER_ID + ") ON DELETE CASCADE"
                + ")";
    }
}
