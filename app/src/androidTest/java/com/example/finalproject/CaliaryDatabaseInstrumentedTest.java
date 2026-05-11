package com.example.finalproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.finalproject.data.CaliaryDbHelper;
import com.example.finalproject.data.DbContract;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.CalendarRepository;
import com.example.finalproject.repository.TodoRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CaliaryDatabaseInstrumentedTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(CaliaryDbHelper.DATABASE_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(CaliaryDbHelper.DATABASE_NAME);
    }

    @Test
    public void createsAllTablesAndDefaultUser() {
        CaliaryDbHelper helper = new CaliaryDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        Set<String> tables = new HashSet<>();
        try (Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)) {
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }
        }

        assertTrue(tables.contains(DbContract.AppUser.TABLE));
        assertTrue(tables.contains(DbContract.RecurrenceRule.TABLE));
        assertTrue(tables.contains(DbContract.CalendarEvent.TABLE));
        assertTrue(tables.contains(DbContract.EventReminder.TABLE));
        assertTrue(tables.contains(DbContract.Todo.TABLE));
        assertTrue(tables.contains(DbContract.TodoReminder.TABLE));
        assertTrue(tables.contains(DbContract.HabitCategory.TABLE));
        assertTrue(tables.contains(DbContract.HabitPriority.TABLE));
        assertTrue(tables.contains(DbContract.Habit.TABLE));
        assertTrue(tables.contains(DbContract.HabitReminder.TABLE));
        assertTrue(tables.contains(DbContract.HabitLog.TABLE));
        assertTrue(tables.contains(DbContract.JournalLayout.TABLE));
        assertTrue(tables.contains(DbContract.JournalEntry.TABLE));
        assertTrue(tables.contains(DbContract.JournalImage.TABLE));
        assertTrue(tables.contains(DbContract.Sticker.TABLE));
        assertTrue(tables.contains(DbContract.JournalEntrySticker.TABLE));
        assertTrue(helper.getDefaultUserId() > 0);
    }

    @Test
    public void eventCrudPersistsRecurrenceReminderAndCascadesReminder() {
        CalendarRepository repository = new CalendarRepository(context);

        RecurrenceRule rule = new RecurrenceRule();
        rule.setFreq(RecurrenceRule.FREQ_WEEKLY);
        rule.setIntervalValue(1);
        rule.setByDay("MO,WE");

        Reminder reminder = new Reminder();
        reminder.setEnabled(true);
        reminder.setReminderType(Reminder.TYPE_BEFORE);
        reminder.setOffsetValue(15);
        reminder.setOffsetUnit("MINUTE");

        CalendarEvent event = new CalendarEvent();
        event.setTitle("Event test");
        event.setDescription("Description");
        event.setColor("#90CAF9");
        event.setStartDateTime(LocalDateTime.of(2026, 4, 26, 7, 15));
        event.setEndDateTime(LocalDateTime.of(2026, 4, 26, 11, 40));
        event.setRecurrenceRule(rule);
        event.setReminder(reminder);

        long eventId = repository.saveEvent(event);
        CalendarEvent saved = repository.getEvent(eventId);
        assertNotNull(saved);
        assertEquals("Event test", saved.getTitle());
        assertNotNull(saved.getRecurrenceRule());
        assertEquals(RecurrenceRule.FREQ_WEEKLY, saved.getRecurrenceRule().getFreq());
        assertEquals("Trước 15 phút", saved.getReminder().getDisplayText());

        saved.setTitle("Updated event");
        repository.saveEvent(saved);
        assertEquals("Updated event", repository.getEvent(eventId).getTitle());

        repository.deleteEvent(eventId);
        assertEquals(0, countRows(DbContract.EventReminder.TABLE));
    }

    @Test
    public void todoCrudPersistsReminderAndCascadesReminder() {
        TodoRepository repository = new TodoRepository(context);

        Reminder reminder = new Reminder();
        reminder.setEnabled(true);
        reminder.setReminderType(Reminder.TYPE_BEFORE);
        reminder.setOffsetValue(1);
        reminder.setOffsetUnit("DAY");

        TodoItem item = new TodoItem();
        item.setTitle("Todo test");
        item.setDescription("Todo description");
        item.setTodoDate(LocalDate.of(2026, 4, 28));
        item.setPriorityQuadrant(1);
        item.setColor("#FFE3E1");
        item.setReminder(reminder);

        long todoId = repository.saveTodo(item);
        TodoItem saved = repository.getTodo(todoId);
        assertNotNull(saved);
        assertEquals("Todo test", saved.getTitle());
        assertEquals(1, saved.getPriorityQuadrant());
        assertEquals("Trước 1 ngày", saved.getReminder().getDisplayText());

        repository.setCompleted(todoId, true);
        assertTrue(repository.getTodo(todoId).isCompleted());
        assertFalse(repository.getTodos(LocalDate.of(2026, 4, 28), TodoRepository.FILTER_DONE).isEmpty());

        repository.deleteTodo(todoId);
        assertEquals(0, countRows(DbContract.TodoReminder.TABLE));
    }

    private int countRows(String table) {
        CaliaryDbHelper helper = new CaliaryDbHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }
}
