package com.example.finalproject.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.finalproject.data.CaliaryDbHelper;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.data.DbContract;
import com.example.finalproject.model.JournalEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalRepository {
    private static final int MAX_JOURNAL_IMAGES = 3;
    private static final int MAX_JOURNAL_MOODS = 3;
    private static final String STICKER_TYPE_JOURNAL_MOOD = "journal_mood";

    private final CaliaryDbHelper dbHelper;

    public JournalRepository(Context context) {
        dbHelper = new CaliaryDbHelper(context.getApplicationContext());
    }

    public JournalEntry getJournalEntry(long journalId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.JournalEntry.TABLE,
                null,
                DbContract.JournalEntry.JOURNAL_ID + " = ?",
                new String[]{String.valueOf(journalId)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            JournalEntry entry = mapJournalEntry(cursor);
            entry.setImageUris(getJournalImages(db, entry.getId()));
            entry.setMoodResourceNames(getJournalMoods(db, entry.getId()));
            return entry;
        }
    }

    public List<JournalEntry> getEntriesForDate(LocalDate date) {
        List<JournalEntry> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.JournalEntry.TABLE,
                null,
                DbContract.JournalEntry.JOURNAL_DATE + " = ?",
                new String[]{DateTimeUtils.dateToIso(date)},
                null,
                null,
                DbContract.JournalEntry.UPDATED_AT + " DESC, " + DbContract.JournalEntry.CREATED_AT + " DESC"
        )) {
            while (cursor.moveToNext()) {
                JournalEntry entry = mapJournalEntry(cursor);
                entry.setImageUris(getJournalImages(db, entry.getId()));
                entry.setMoodResourceNames(getJournalMoods(db, entry.getId()));
                result.add(entry);
            }
        }
        return result;
    }

    public List<JournalEntry> getEntriesForMonth(LocalDate monthDate) {
        List<JournalEntry> result = new ArrayList<>();
        if (monthDate == null) {
            return result;
        }
        LocalDate monthStart = monthDate.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.JournalEntry.TABLE,
                null,
                DbContract.JournalEntry.JOURNAL_DATE + " >= ? AND " + DbContract.JournalEntry.JOURNAL_DATE + " < ?",
                new String[]{DateTimeUtils.dateToIso(monthStart), DateTimeUtils.dateToIso(nextMonthStart)},
                null,
                null,
                DbContract.JournalEntry.JOURNAL_DATE + " ASC, " + DbContract.JournalEntry.UPDATED_AT + " DESC"
        )) {
            while (cursor.moveToNext()) {
                JournalEntry entry = mapJournalEntry(cursor);
                entry.setImageUris(getJournalImages(db, entry.getId()));
                entry.setMoodResourceNames(getJournalMoods(db, entry.getId()));
                result.add(entry);
            }
        }
        return result;
    }

    public List<JournalEntry> getEntriesForYear(LocalDate yearDate) {
        List<JournalEntry> result = new ArrayList<>();
        if (yearDate == null) {
            return result;
        }
        LocalDate yearStart = LocalDate.of(yearDate.getYear(), 1, 1);
        LocalDate nextYearStart = yearStart.plusYears(1);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.JournalEntry.TABLE,
                null,
                DbContract.JournalEntry.JOURNAL_DATE + " >= ? AND " + DbContract.JournalEntry.JOURNAL_DATE + " < ?",
                new String[]{DateTimeUtils.dateToIso(yearStart), DateTimeUtils.dateToIso(nextYearStart)},
                null,
                null,
                DbContract.JournalEntry.JOURNAL_DATE + " ASC, " + DbContract.JournalEntry.UPDATED_AT + " DESC"
        )) {
            while (cursor.moveToNext()) {
                JournalEntry entry = mapJournalEntry(cursor);
                entry.setImageUris(getJournalImages(db, entry.getId()));
                entry.setMoodResourceNames(getJournalMoods(db, entry.getId()));
                result.add(entry);
            }
        }
        return result;
    }

    public long saveJournalEntry(JournalEntry entry) {
        entry.setUserId(dbHelper.getDefaultUserId());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.ensureDefaultJournalLayouts(db);
        String now = DateTimeUtils.nowIso();
        ContentValues values = toJournalValues(entry);
        values.put(DbContract.JournalEntry.UPDATED_AT, now);
        db.beginTransaction();
        try {
            long journalId;
            if (entry.getId() <= 0) {
                values.put(DbContract.JournalEntry.CREATED_AT, now);
                journalId = db.insertOrThrow(DbContract.JournalEntry.TABLE, null, values);
                entry.setId(journalId);
            } else {
                db.update(
                        DbContract.JournalEntry.TABLE,
                        values,
                        DbContract.JournalEntry.JOURNAL_ID + " = ?",
                        new String[]{String.valueOf(entry.getId())}
                );
                journalId = entry.getId();
            }
            replaceJournalImages(db, journalId, entry.getImageUris());
            replaceJournalMoods(db, journalId, entry.getMoodResourceNames());
            db.setTransactionSuccessful();
            return journalId;
        } finally {
            db.endTransaction();
        }
    }

    public void deleteJournalEntry(long journalId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DbContract.JournalEntry.TABLE,
                DbContract.JournalEntry.JOURNAL_ID + " = ?",
                new String[]{String.valueOf(journalId)}
        );
    }

    private ContentValues toJournalValues(JournalEntry entry) {
        ContentValues values = new ContentValues();
        values.put(DbContract.JournalEntry.USER_ID, entry.getUserId());
        if (entry.getLayoutId() == null) {
            values.putNull(DbContract.JournalEntry.LAYOUT_ID);
        } else {
            values.put(DbContract.JournalEntry.LAYOUT_ID, entry.getLayoutId());
        }
        values.put(DbContract.JournalEntry.JOURNAL_DATE, DateTimeUtils.dateToIso(entry.getJournalDate()));
        values.put(DbContract.JournalEntry.TITLE, entry.getTitle());
        values.put(DbContract.JournalEntry.CAPTION, entry.getCaption());
        values.put(DbContract.JournalEntry.CONTENT, entry.getContent());
        return values;
    }

    private List<String> getJournalImages(SQLiteDatabase db, long journalId) {
        List<String> imageUris = new ArrayList<>();
        for (int index = 0; index < MAX_JOURNAL_IMAGES; index++) {
            imageUris.add("");
        }
        try (Cursor cursor = db.query(
                DbContract.JournalImage.TABLE,
                new String[]{DbContract.JournalImage.IMAGE_URI, DbContract.JournalImage.POSITION_INDEX},
                DbContract.JournalImage.JOURNAL_ID + " = ?",
                new String[]{String.valueOf(journalId)},
                null,
                null,
                DbContract.JournalImage.POSITION_INDEX + " ASC"
        )) {
            while (cursor.moveToNext()) {
                int position = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.JournalImage.POSITION_INDEX));
                if (position >= 0 && position < MAX_JOURNAL_IMAGES) {
                    imageUris.set(position, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.JournalImage.IMAGE_URI)));
                }
            }
        }
        return imageUris;
    }

    private void replaceJournalImages(SQLiteDatabase db, long journalId, List<String> imageUris) {
        db.delete(
                DbContract.JournalImage.TABLE,
                DbContract.JournalImage.JOURNAL_ID + " = ?",
                new String[]{String.valueOf(journalId)}
        );
        if (imageUris == null) {
            return;
        }
        int count = Math.min(imageUris.size(), MAX_JOURNAL_IMAGES);
        for (int index = 0; index < count; index++) {
            String imageUri = imageUris.get(index);
            if (imageUri == null || imageUri.trim().isEmpty()) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(DbContract.JournalImage.JOURNAL_ID, journalId);
            values.put(DbContract.JournalImage.IMAGE_URI, imageUri);
            values.put(DbContract.JournalImage.POSITION_INDEX, index);
            db.insertOrThrow(DbContract.JournalImage.TABLE, null, values);
        }
    }

    private List<String> getJournalMoods(SQLiteDatabase db, long journalId) {
        List<String> moodNames = new ArrayList<>();
        String sql = "SELECT s." + DbContract.Sticker.IMAGE_URI
                + " FROM " + DbContract.JournalEntrySticker.TABLE + " jes"
                + " INNER JOIN " + DbContract.Sticker.TABLE + " s"
                + " ON jes." + DbContract.JournalEntrySticker.STICKER_ID
                + " = s." + DbContract.Sticker.STICKER_ID
                + " WHERE jes." + DbContract.JournalEntrySticker.JOURNAL_ID + " = ?"
                + " AND s." + DbContract.Sticker.STICKER_TYPE + " = ?"
                + " ORDER BY jes." + DbContract.JournalEntrySticker.SELECTED_ORDER + " ASC";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(journalId), STICKER_TYPE_JOURNAL_MOOD})) {
            while (cursor.moveToNext() && moodNames.size() < MAX_JOURNAL_MOODS) {
                moodNames.add(cursor.getString(0));
            }
        }
        return moodNames;
    }

    private void replaceJournalMoods(SQLiteDatabase db, long journalId, List<String> moodResourceNames) {
        db.delete(
                DbContract.JournalEntrySticker.TABLE,
                DbContract.JournalEntrySticker.JOURNAL_ID + " = ?",
                new String[]{String.valueOf(journalId)}
        );
        if (moodResourceNames == null) {
            return;
        }
        int count = Math.min(moodResourceNames.size(), MAX_JOURNAL_MOODS);
        for (int index = 0; index < count; index++) {
            String moodName = moodResourceNames.get(index);
            if (moodName == null || moodName.trim().isEmpty()) {
                continue;
            }
            long stickerId = ensureMoodSticker(db, moodName.trim());
            ContentValues values = new ContentValues();
            values.put(DbContract.JournalEntrySticker.JOURNAL_ID, journalId);
            values.put(DbContract.JournalEntrySticker.STICKER_ID, stickerId);
            values.put(DbContract.JournalEntrySticker.SELECTED_ORDER, index);
            db.insertOrThrow(DbContract.JournalEntrySticker.TABLE, null, values);
        }
    }

    private long ensureMoodSticker(SQLiteDatabase db, String moodName) {
        try (Cursor cursor = db.query(
                DbContract.Sticker.TABLE,
                new String[]{DbContract.Sticker.STICKER_ID},
                DbContract.Sticker.STICKER_TYPE + " = ? AND " + DbContract.Sticker.IMAGE_URI + " = ?",
                new String[]{STICKER_TYPE_JOURNAL_MOOD, moodName},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        ContentValues values = new ContentValues();
        values.put(DbContract.Sticker.NAME, moodName);
        values.put(DbContract.Sticker.STICKER_TYPE, STICKER_TYPE_JOURNAL_MOOD);
        values.put(DbContract.Sticker.IMAGE_URI, moodName);
        values.put(DbContract.Sticker.IS_ACTIVE, 1);
        return db.insertOrThrow(DbContract.Sticker.TABLE, null, values);
    }

    private JournalEntry mapJournalEntry(Cursor cursor) {
        JournalEntry entry = new JournalEntry();
        entry.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.JOURNAL_ID)));
        entry.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.USER_ID)));
        int layoutIndex = cursor.getColumnIndexOrThrow(DbContract.JournalEntry.LAYOUT_ID);
        if (!cursor.isNull(layoutIndex)) {
            entry.setLayoutId(cursor.getLong(layoutIndex));
        }
        entry.setJournalDate(DateTimeUtils.isoToDate(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.JOURNAL_DATE))));
        entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.TITLE)));
        entry.setCaption(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.CAPTION)));
        entry.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.CONTENT)));
        entry.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.CREATED_AT)));
        entry.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.JournalEntry.UPDATED_AT)));
        return entry;
    }
}
