package com.example.finalproject.data;

public final class DbContract {
    private DbContract() {
    }

    public static final class AppUser {
        public static final String TABLE = "APP_USER";
        public static final String USER_ID = "user_id";
        public static final String CREATED_AT = "created_at";
    }

    public static final class RecurrenceRule {
        public static final String TABLE = "RECURRENCE_RULE";
        public static final String RECURRENCE_ID = "recurrence_id";
        public static final String FREQ = "freq";
        public static final String INTERVAL_VALUE = "interval_value";
        public static final String BY_DAY = "by_day";
        public static final String BY_MONTH_DAY = "by_month_day";
        public static final String BY_MONTH = "by_month";
        public static final String BY_SET_POS = "by_set_pos";
        public static final String MONTHLY_PATTERN_TYPE = "monthly_pattern_type";
        public static final String END_TYPE = "end_type";
        public static final String END_DATE = "end_date";
        public static final String OCCURRENCE_COUNT = "occurrence_count";
    }

    public static final class CalendarEvent {
        public static final String TABLE = "CALENDAR_EVENT";
        public static final String EVENT_ID = "event_id";
        public static final String USER_ID = "user_id";
        public static final String RECURRENCE_ID = "recurrence_id";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String COLOR = "color";
        public static final String IS_ALL_DAY = "is_all_day";
        public static final String START_DATETIME = "start_datetime";
        public static final String END_DATETIME = "end_datetime";
        public static final String LOCATION = "location";
        public static final String URL = "url";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class EventReminder {
        public static final String TABLE = "EVENT_REMINDER";
        public static final String EVENT_REMINDER_ID = "event_reminder_id";
        public static final String EVENT_ID = "event_id";
        public static final String REMINDER_TYPE = "reminder_type";
        public static final String OFFSET_VALUE = "offset_value";
        public static final String OFFSET_UNIT = "offset_unit";
        public static final String TIME_OF_DAY = "time_of_day";
        public static final String IS_ENABLED = "is_enabled";
    }

    public static final class Todo {
        public static final String TABLE = "TODO";
        public static final String TODO_ID = "todo_id";
        public static final String USER_ID = "user_id";
        public static final String RECURRENCE_ID = "recurrence_id";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String TODO_DATE = "todo_date";
        public static final String IS_COMPLETED = "is_completed";
        public static final String COLOR = "color";
        public static final String PRIORITY_QUADRANT = "priority_quadrant";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class TodoReminder {
        public static final String TABLE = "TODO_REMINDER";
        public static final String TODO_REMINDER_ID = "todo_reminder_id";
        public static final String TODO_ID = "todo_id";
        public static final String REMINDER_TYPE = "reminder_type";
        public static final String OFFSET_VALUE = "offset_value";
        public static final String OFFSET_UNIT = "offset_unit";
        public static final String TIME_OF_DAY = "time_of_day";
        public static final String IS_ENABLED = "is_enabled";
    }

    public static final class HabitCategory {
        public static final String TABLE = "HABIT_CATEGORY";
        public static final String CATEGORY_ID = "category_id";
        public static final String USER_ID = "user_id";
        public static final String NAME = "name";
        public static final String ICON_URI = "icon_uri";
        public static final String COLOR = "color";
        public static final String CREATED_AT = "created_at";
    }

    public static final class HabitPriority {
        public static final String TABLE = "HABIT_PRIORITY";
        public static final String PRIORITY_ID = "priority_id";
        public static final String USER_ID = "user_id";
        public static final String NAME = "name";
        public static final String COLOR = "color";
        public static final String PRIORITY_ORDER = "priority_order";
        public static final String CREATED_AT = "created_at";
    }

    public static final class Habit {
        public static final String TABLE = "HABIT";
        public static final String HABIT_ID = "habit_id";
        public static final String USER_ID = "user_id";
        public static final String CATEGORY_ID = "category_id";
        public static final String PRIORITY_ID = "priority_id";
        public static final String RECURRENCE_ID = "recurrence_id";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String COLOR = "color";
        public static final String START_DATE = "start_date";
        public static final String HAS_END_DATE = "has_end_date";
        public static final String END_DATE = "end_date";
        public static final String STATUS = "status";
        public static final String EVALUATION_TYPE = "evaluation_type";
        public static final String TARGET_OPERATOR = "target_operator";
        public static final String TARGET_VALUE = "target_value";
        public static final String TARGET_UNIT = "target_unit";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class HabitReminder {
        public static final String TABLE = "HABIT_REMINDER";
        public static final String HABIT_REMINDER_ID = "habit_reminder_id";
        public static final String HABIT_ID = "habit_id";
        public static final String REMINDER_TYPE = "reminder_type";
        public static final String OFFSET_VALUE = "offset_value";
        public static final String OFFSET_UNIT = "offset_unit";
        public static final String TIME_OF_DAY = "time_of_day";
        public static final String IS_ENABLED = "is_enabled";
    }

    public static final class HabitLog {
        public static final String TABLE = "HABIT_LOG";
        public static final String HABIT_LOG_ID = "habit_log_id";
        public static final String HABIT_ID = "habit_id";
        public static final String LOG_DATE = "log_date";
        public static final String IS_COMPLETED = "is_completed";
        public static final String ACTUAL_VALUE = "actual_value";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class JournalLayout {
        public static final String TABLE = "JOURNAL_LAYOUT";
        public static final String LAYOUT_ID = "layout_id";
        public static final String NAME = "name";
        public static final String PREVIEW_IMAGE_URI = "preview_image_uri";
        public static final String BACKGROUND_IMAGE_URI = "background_image_uri";
        public static final String LAYOUT_CONFIG = "layout_config";
        public static final String IS_ACTIVE = "is_active";
        public static final String CREATED_AT = "created_at";
    }

    public static final class JournalEntry {
        public static final String TABLE = "JOURNAL_ENTRY";
        public static final String JOURNAL_ID = "journal_id";
        public static final String USER_ID = "user_id";
        public static final String LAYOUT_ID = "layout_id";
        public static final String JOURNAL_DATE = "journal_date";
        public static final String TITLE = "title";
        public static final String CAPTION = "caption";
        public static final String CONTENT = "content";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class JournalImage {
        public static final String TABLE = "JOURNAL_IMAGE";
        public static final String IMAGE_ID = "image_id";
        public static final String JOURNAL_ID = "journal_id";
        public static final String IMAGE_URI = "image_uri";
        public static final String POSITION_INDEX = "position_index";
    }

    public static final class Sticker {
        public static final String TABLE = "STICKER";
        public static final String STICKER_ID = "sticker_id";
        public static final String NAME = "name";
        public static final String STICKER_TYPE = "sticker_type";
        public static final String IMAGE_URI = "image_uri";
        public static final String IS_ACTIVE = "is_active";
    }

    public static final class JournalEntrySticker {
        public static final String TABLE = "JOURNAL_ENTRY_STICKER";
        public static final String JOURNAL_ID = "journal_id";
        public static final String STICKER_ID = "sticker_id";
        public static final String SELECTED_ORDER = "selected_order";
    }
}
