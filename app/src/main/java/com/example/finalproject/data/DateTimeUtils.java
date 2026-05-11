package com.example.finalproject.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class DateTimeUtils {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);

    private DateTimeUtils() {
    }

    public static String nowIso() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }

    public static String dateToIso(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMAT);
    }

    public static LocalDate isoToDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized, DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            LocalDateTime dateTime = isoToDateTime(normalized);
            return dateTime == null ? null : dateTime.toLocalDate();
        }
    }

    public static String dateTimeToIso(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_FORMAT);
    }

    public static LocalDateTime isoToDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() == 10) {
            return LocalDate.parse(normalized, DATE_FORMAT).atStartOfDay();
        }
        String[] candidates = {
                normalized,
                normalized.replace(' ', 'T')
        };
        DateTimeFormatter[] formatters = {
                DATETIME_FORMAT,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        };
        for (String candidate : candidates) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(candidate, formatter);
                } catch (DateTimeParseException ignored) {
                    // Try the next supported manual-entry format.
                }
            }
        }
        throw new DateTimeParseException("Unsupported datetime format", value, 0);
    }

    public static String timeToIso(LocalTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }

    public static LocalTime isoToTime(String value) {
        return value == null || value.isEmpty() ? null : LocalTime.parse(value, TIME_FORMAT);
    }

    public static String formatVietnameseDate(LocalDate date) {
        return date == null ? "" : date.format(VI_DATE);
    }

    public static String formatVietnameseTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        int hour = time.getHour();
        int displayHour = hour % 12;
        if (displayHour == 0) {
            displayHour = 12;
        }
        String period = hour < 12 ? "SA" : "CH";
        return String.format(Locale.US, "%02d:%02d %s", displayHour, time.getMinute(), period);
    }

    public static String formatDateWithDow(LocalDate date) {
        if (date == null) {
            return "";
        }
        String[] names = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        return date.format(VI_DATE) + "(" + names[date.getDayOfWeek().getValue() % 7] + ")";
    }
}
